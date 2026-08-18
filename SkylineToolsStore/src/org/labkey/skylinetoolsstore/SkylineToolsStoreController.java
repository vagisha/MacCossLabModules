/*
 * Copyright (c) 2017-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.skylinetoolsstore;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.PermissionCheckable;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.LabKeyCollectors;
import org.labkey.api.data.Container;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.ServerPrimaryKeyLock;
import org.labkey.api.data.NormalContainerType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.FieldKey;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.security.ActionNames;
import org.labkey.api.security.Group;
import org.labkey.api.security.MutableSecurityPolicy;
import org.labkey.api.security.RequiresAllOf;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.RoleAssignment;
import org.labkey.api.security.SecurityPolicy;
import org.labkey.api.security.SecurityPolicyManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.roles.EditorRole;
import org.labkey.api.security.roles.FolderAdminRole;
import org.labkey.api.security.roles.ReaderRole;
import org.labkey.api.security.roles.Role;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.JavaScriptFragment;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.SafeToRender;
import org.labkey.api.util.URLHelper;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.api.webdav.WebdavService;
import org.labkey.skylinetoolsstore.model.SkylineTool;
import org.labkey.skylinetoolsstore.view.SkylineToolDetails;
import org.labkey.skylinetoolsstore.view.SkylineToolStoreUrls;
import org.labkey.skylinetoolsstore.view.SkylineToolsStoreWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SkylineToolsStoreController extends SpringActionController
{
    private static final Logger LOG = LogHelper.getLogger(SkylineToolsStoreController.class, "SkylineToolsStoreController requests");
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(SkylineToolsStoreController.class);
    private static final String[] VALID_ICON_EXTENSIONS = new String[] { "png", "jpg", "jpeg", "gif" };

    private static final String STORE_NOT_AVAILABLE = "The Skyline Tool Store is not available in this folder.";
    private static final String TOOL_ALREADY_EXISTS = "The Skyline Tool you are trying to add already exists.";
    // Enumerates the addresses it was given, so it must not reach a caller refused for permissions.
    // ToolStoreSecurityTest.testInsertDoesNotRevealWhetherAccountsExist repeats this text, because
    // that source set cannot see this class. Change the two together.
    private static final String UNKNOWN_USERS = "The following users are unknown: ";

    public SkylineToolsStoreController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public static class BeginAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            if (!getContainer().hasActiveModuleByName(SkylineToolsStoreModule.NAME))
                throw new NotFoundException(STORE_NOT_AVAILABLE);

            SkylineTool[] ownTools = SkylineToolsStoreManager.get().getTools(getContainer());
            // If this container has a tool, redirect to the tool details page.
            if (ownTools.length > 0)
                throw new RedirectException(SkylineToolStoreUrls.getToolDetailsByIdUrl(ownTools[0]));

            return new SkylineToolsStoreWebPart();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild(getToolStoreNav(getContainer()));
        }
    }

    /**
     * True when a tool can be added to this container. The module should be enabled, and it should not contain a tool.
     */
    public static boolean isStoreContainer(Container container)
    {
        return container.hasActiveModuleByName(SkylineToolsStoreModule.NAME)
                && SkylineToolsStoreManager.get().getTools(container).length == 0;
    }

    public static NavTree getToolStoreNav(Container container)
    {
        return new NavTree("Skyline Tool Store", new ActionURL(BeginAction.class, container));
    }

    /**
     * Nav trail link for an action that runs in a tool's own folder. We need to link back to The main
     * tool store folder that is the tool folder's parent.
     */
    private static NavTree getToolStoreNavFromToolFolder(Container toolContainer)
    {
        return getToolStoreNav(toolContainer.getParent());
    }

    protected SkylineTool getToolFromZip(MultipartFile zip) throws IOException
    {
        SkylineTool tool = null;
        byte[] toolIcon = null;
        try (ZipInputStream zipStream = new ZipInputStream(zip.getInputStream()))
        {
            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null &&
                    (tool == null || toolIcon == null))
            {
                String entryLower = zipEntry.getName().toLowerCase();
                if (entryLower.startsWith("tool-inf/") && !entryLower.startsWith("tool-inf/docs/"))
                {
                    String lowerBaseName = new File(zipEntry.getName()).getName().toLowerCase();

                    if (lowerBaseName.equals("info.properties"))
                    {
                        byte[] bytes = unzip(zipStream);
                        // zipEntry.closeEntry() not necessary, getNextEntry() does it automatically

                        tool = new SkylineTool(new BufferedReader(new StringReader(new String(bytes, "UTF-8"))));
                    }
                    else if (Arrays.asList(VALID_ICON_EXTENSIONS).contains(FileUtil.getExtension(lowerBaseName)))
                    {
                        toolIcon = unzip(zipStream);
                    }
                }
            }
        }

        if (tool != null)
        {
            tool.setZipName(FileUtil.makeLegalName(zip.getOriginalFilename()));
            if (toolIcon != null)
                tool.setIcon(toolIcon);
        }

        return tool;
    }

    /** Reads the current zip entry. Throws on a corrupt zip rather than returning null. */
    protected byte[] unzip(ZipInputStream stream) throws IOException
    {
        final int BUFFER_SIZE = 2048;

        byte[] bytes = new byte[BUFFER_SIZE];
        int bytesRead;
        try (ByteArrayOutputStream unzipBytes = new ByteArrayOutputStream())
        {
            while ((bytesRead = stream.read(bytes, 0, BUFFER_SIZE)) != -1)
                unzipBytes.write(bytes, 0, bytesRead);
            return unzipBytes.toByteArray();
        }
    }

    protected static boolean extractDocsFromZip(Path zipFile, Path containerDir) throws IOException
    {
        Path docsDir = containerDir.resolve("docs");
        boolean extracted = false;
        try (ZipFile zf = new ZipFile(zipFile.toFile()))
        {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.toLowerCase().startsWith("tool-inf/docs/") || entry.isDirectory())
                    continue;
                // Strip "tool-inf/docs/" prefix to get relative path within docs dir
                String relativePath = name.substring("tool-inf/docs/".length());
                if (relativePath.isEmpty())
                    continue;
                Path destPath = docsDir.resolve(relativePath).normalize();
                // Zip-slip protection
                if (!destPath.startsWith(docsDir.normalize()))
                    throw new IOException("Zip entry outside target directory: " + name);
                Files.createDirectories(destPath.getParent());
                try (InputStream in = zf.getInputStream(entry))
                {
                    Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
                extracted = true;
            }
        }
        return extracted;
    }

    public static File makeFile(Container c, String filename)
    {
        return getLocalPath(c).resolve(FileUtil.makeLegalName(filename)).toFile();
    }

    /**
     * Confirms the given container is one this tool may be acted on from - its own folder, or the store folder above it.
     *
     * For actions that act on every version folder of a tool, so requireToolInContainer cannot be used. They are
     * @RequiresSiteAdmin, and weakening that to @RequiresPermission(AdminPermission.class) would let an admin of any
     * folder act on any tool through them.
     */
    private static void requireToolAddressableFrom(SkylineTool tool, Container c)
    {
        Container toolContainer = tool.lookupContainer();
        if (toolContainer == null)
            throw new NotFoundException("Failed to look up the folder for " + tool.getName() + ".");
        if (!c.equals(toolContainer) && !c.equals(toolContainer.getParent()))
            throw new NotFoundException("This request has to be addressed to folder containing " + tool.getName() +
                    " or to the tool store containing it.");
    }

    /**
     * Resolves a tool row id and confirms the tool lives in the given container.
     */
    private static SkylineTool requireToolInContainer(int toolId, Container c)
    {
        SkylineTool tool = SkylineToolsStoreManager.get().getTool(toolId);
        if (tool == null || !c.equals(tool.lookupContainer()))
            throw new NotFoundException("Could not find tool with Id " + toolId + " in this folder.");
        return tool;
    }

    public static Path getLocalPath(Container c)
    {
        return FileContentService.get().getFileRootPath(c, FileContentService.ContentType.files);
    }

    protected Container makeContainer(Container parent, String folderName, List<User> users, Role role) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        if (!Container.isLegalName(folderName, false, sb))
            return null;

        if (parent.hasChild(folderName))
            return null;

        Container c = ContainerManager.createContainer(parent, folderName, null, null, NormalContainerType.NAME, getUser());
        boolean folderSetupComplete = false;
        try
        {
            c.setFolderType(FolderTypeManager.get().getFolderType("Collaboration"), getUser());

            Path fileRoot = FileContentService.get().getFileRootPath(c, FileContentService.ContentType.files);
            if(!Files.exists(fileRoot))
            {
                Files.createDirectories(fileRoot);
            }

            // Make folder readable by all site users and guests, so that they can access the zip file/icon
            MutableSecurityPolicy policy = new MutableSecurityPolicy(c);
            User guest = new User();
            guest.setUserId(Group.groupGuests);
            User user = new User();
            user.setUserId(Group.groupUsers);
            policy.addRoleAssignment(guest, RoleManager.getRole(ReaderRole.class));
            policy.addRoleAssignment(user, RoleManager.getRole(ReaderRole.class));

            if (users != null && !users.isEmpty() && role != null)
                for (User u : users)
                    policy.addRoleAssignment(u, role);

            SecurityPolicyManager.savePolicy(policy, User.getAdminServiceUser());

            folderSetupComplete = true;
            return c;
        }
        finally
        {
            // Discard the folder if folder setup could not be completed.
            if (!folderSetupComplete)
                discardVersionFolder(c);
        }
    }

    protected MutableSecurityPolicy copyPolicy(Container c, SecurityPolicy from)
    {
        MutableSecurityPolicy policy = new MutableSecurityPolicy(c);
        for (RoleAssignment assignment : from.getAssignments())
        {
            User u = new User();
            u.setUserId(assignment.getUserId());
            policy.addRoleAssignment(u, assignment.getRole());
        }
        return policy;
    }

    protected MutableSecurityPolicy filterPolicy(SecurityPolicy original, List<User> users, Role[] roles)
    {
        // For each role assignment where the role is in roles, only keep if the user is in users.
        // Assignments held by a group are left alone, since only users are being replaced.
        MutableSecurityPolicy policy = new MutableSecurityPolicy(ContainerManager.getForId(original.getContainerId()));
        for (RoleAssignment assignment : original.getAssignments())
        {
            if (Arrays.asList(roles).contains(assignment.getRole()) &&
                UserManager.getUser(assignment.getUserId()) != null)
            {
                boolean skip = true;
                for (User u : users)
                {
                    if (u.getUserId() == assignment.getUserId())
                    {
                        skip = false;
                        break;
                    }
                }
                if (skip)
                    continue;
            }

            User u = new User();
            u.setUserId(assignment.getUserId());
            policy.addRoleAssignment(u, assignment.getRole());
        }
        return policy;
    }

    protected void copyContainerPermissions(Container from, Container to)
    {
        if (from == null || to == null)
            return;

        SecurityPolicyManager.savePolicy(copyPolicy(to, from.getPolicy()), User.getAdminServiceUser());
    }

    /**
     * Makes the Editor and FolderAdmin role holders on one folder exactly the given owners. Runs per
     * folder, because the additions are worked out against that folder's existing assignments.
     */
    protected void setToolOwners(Container c, List<User> owners)
    {
        ArrayList<User> newToolEditors = new ArrayList<>(owners);

        for (RoleAssignment assignment : c.getPolicy().getAssignments())
        {
            if (assignment.getRole() != RoleManager.getRole(FolderAdminRole.class) &&
                assignment.getRole() != RoleManager.getRole(EditorRole.class))
                continue;
            for (int i = 0; i < newToolEditors.size(); ++i)
            {
                if (newToolEditors.get(i).getUserId() == assignment.getUserId())
                {
                    newToolEditors.remove(i);
                    break;
                }
            }
        }

        MutableSecurityPolicy policy = copyPolicy(c, c.getPolicy());
        for (User u : newToolEditors)
            policy.addRoleAssignment(u, RoleManager.getRole(EditorRole.class));
        policy = filterPolicy(policy, owners, new Role[]{RoleManager.getRole(EditorRole.class), RoleManager.getRole(FolderAdminRole.class)});
        SecurityPolicyManager.savePolicy(policy, User.getAdminServiceUser());
    }

    public static SkylineTool[] sortToolsByCreateDate(SkylineTool[] tools)
    {
        List<SkylineTool> toolList = Arrays.asList(tools);

        toolList.sort((lhs, rhs) -> rhs.getCreated().compareTo(lhs.getCreated()));

        return toolList.toArray(new SkylineTool[0]);
    }

    public static SafeToRender getUsersForAutocomplete()
    {
        JSONArray jsonArray = UserManager.getActiveUsers().stream()
            .map(User::getEmail)
            .collect(LabKeyCollectors.toJSONArray());

        return JavaScriptFragment.unsafe(jsonArray.toString());
    }

    protected static Pair<ArrayList<User>, ArrayList<String>> parseToolOwnerString(String toolOwners)
    {
        ArrayList<User> toolOwnersUsers = new ArrayList<>();
        ArrayList<String> toolOwnersInvalid = new ArrayList<>();
        if (toolOwners != null)
        {
            for (String toolOwner : toolOwners.split(","))
            {
                toolOwner = toolOwner.trim();
                if (!toolOwner.isEmpty())
                {
                    User u;
                    try
                    {
                        u = UserManager.getUser(new ValidEmail(toolOwner));
                    }
                    catch (ValidEmail.InvalidEmailException e)
                    {
                        // Reject an entry that is not a valid address.
                        u = null;
                    }
                    if (u == null)
                        toolOwnersInvalid.add(toolOwner);
                    else
                        toolOwnersUsers.add(u);
                }
            }
        }
        return new Pair<>(toolOwnersUsers,  toolOwnersInvalid);
    }

    public static ArrayList<String> getToolOwners(SkylineTool tool)
    {
        return getToolRelevantUsers(tool, new Role[]{RoleManager.getRole(EditorRole.class), RoleManager.getRole(FolderAdminRole.class)});
    }

    public static ArrayList<String> getToolRelevantUsers(SkylineTool tool, Role[] roles)
    {
        HashSet<String> users = new HashSet<>();
        for (RoleAssignment assignment : tool.lookupContainer().getPolicy().getAssignments())
            if (Arrays.asList(roles).contains(assignment.getRole()))
            {
                User user = UserManager.getUser(assignment.getUserId());
                if(user != null && user.getEmail() != null)
                {
                    users.add(user.getEmail());
                }
            }
        return new ArrayList<>(users);
    }

    public static HashMap<String, String> getSupplementaryFiles(SkylineTool tool) throws IOException
    {
        // Store supporting files in map <url, icon url>
        final String[] knownExtensions = {"pdf", "zip"};
        final String imgDir = AppProps.getInstance().getContextPath() + "/skylinetoolsstore/img/";

        HashMap<String, String> suppFiles = new HashMap<>();
        for (String suppFile : getSupplementaryFileBasenames(tool))
        {
            final String suppFileExtension = FileUtil.getExtension(suppFile).toLowerCase();
            final String suppFileIcon = (Arrays.asList(knownExtensions).contains(suppFileExtension)) ?
                imgDir + suppFileExtension + "-icon.png" : imgDir + "unknown-icon.jpg";
            suppFiles.put(tool.getFolderUrl() + suppFile, suppFileIcon);
        }

        return suppFiles;
    }

    public static HashSet<String> getSupplementaryFileBasenames(SkylineTool tool) throws IOException
    {
        HashSet<String> suppFiles = new HashSet<>();
        Path localToolDir = getLocalPath(tool.lookupContainer());
        try (var stream = Files.list(localToolDir))
        {
            // Regular files only
            stream.filter(Files::isRegularFile)
                  .map(p -> p.getFileName().toString())
                  .filter(name -> !name.startsWith(".") && !name.equals(tool.getZipName()) && !name.equals("icon.png") && !name.equals("docs"))
                  .forEach(suppFiles::add);
        }
        return suppFiles;
    }

    /**
     * Adds a brand-new tool to this store folder.
     *
     * Site admin only. Tool authors do not upload here - they attach a zip to a message board post and
     * an admin adds it.
     *
     * Split from the old combined InsertAction because adding a tool and publishing a new tool version
     * need different permissions on different containers. See UpdateToolAction.
     */
    @RequiresSiteAdmin
    public class InsertToolAction extends FormViewAction<ToolUploadForm>
    {
        private SkylineTool _tool;

        @Override
        public void validateCommand(ToolUploadForm form, Errors errors)
        {
            // This action should only be called from a tool store container
            if (!isStoreContainer(getContainer()))
                errors.reject(ERROR_MSG, STORE_NOT_AVAILABLE);
        }

        @Override
        public ModelAndView getView(ToolUploadForm form, boolean reshow, BindException errors)
        {
            if (!reshow && !isStoreContainer(getContainer()))
            {
                errors.addError(new LabKeyError(STORE_NOT_AVAILABLE));
                return new SimpleErrorView(errors);
            }

            return new JspView<>("/org/labkey/skylinetoolsstore/view/SkylineToolsStoreUpload.jsp", form, errors);
        }

        @Override
        public boolean handlePost(ToolUploadForm form, BindException errors) throws Exception
        {
            Pair<ArrayList<User>, ArrayList<String>> parsedOwners = parseToolOwnerString(form.getToolOwners());
            if (!parsedOwners.second.isEmpty())
            {
                errors.reject(ERROR_MSG, UNKNOWN_USERS + StringUtils.join(parsedOwners.second, ", "));
                return false;
            }

            SkylineTool tool = readToolFromUpload(getFileMap().get("toolZip"), errors);
            if (tool == null)
                return false;

            // Identifiers must be unique across the whole server, since Skyline keys on them. Every row
            // is scanned rather than only the ones flagged latest, because a tool whose rows are all
            // flagged not latest is invisible to getToolsLatest and its identifier would be let in a
            // second time.
            for (SkylineTool existing : SkylineToolsStoreManager.get().getAllTools())
            {
                if (tool.getIdentifier().equalsIgnoreCase(existing.getIdentifier()))
                {
                    errors.reject(ERROR_MSG, TOOL_ALREADY_EXISTS);
                    return false;
                }
            }
            for (Container child : getContainer().getChildren())
            {
                if (child.getName().equalsIgnoreCase(toolFolderName(tool)))
                {
                    errors.reject(ERROR_MSG, TOOL_ALREADY_EXISTS);
                    return false;
                }
            }

            // A Name must identify one tool. Skyline builds details.view?name= for every tool it
            // lists, and getLatestTool picks arbitrarily when two tools share a name.
            if (SkylineToolsStoreManager.get().getTools(
                    new SimpleFilter(FieldKey.fromParts("Name"), tool.getName())).length > 0)
            {
                errors.reject(ERROR_MSG, "Another tool is already published under the name " +
                        tool.getName() + ".");
                return false;
            }

            Container versionContainer = createVersionFolder(getContainer(), tool,
                    getFileMap().get("toolZip"), parsedOwners.first, null, errors);
            // createVersionFolder has already rejected with the reason.
            if (versionContainer == null)
                return false;

            boolean stored = false;
            try
            {
                tool.setLatest(true);
                _tool = SkylineToolsStoreManager.get().insertTool(versionContainer, getUser(), tool);
                stored = true;
            }
            finally
            {
                if (!stored)
                    discardVersionFolder(versionContainer);
            }
            return true;
        }

        @Override
        public URLHelper getSuccessURL(ToolUploadForm form)
        {
            return SkylineToolStoreUrls.getToolDetailsUrl(_tool);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild(getToolStoreNav(getContainer()));
            root.addChild("Upload Tool");
        }
    }

    /**
     * Publishes a new version of an existing tool.
     *
     * Should target the tool's own container, so the annotation checks the folder where the owner
     * holds Editor. This is what lets a tool author maintain their tool without an admin.
     *
     * Writes outside the folder the permissions annotation checks. createVersionFolder adds a child to the store folder
     * above this one and insertTool writes the tool row into that child. This is ok because a tool's version folders
     * all carry the same policy - see createVersionFolder, copyContainerPermissions and SetOwnersAction.
     */
    @RequiresAllOf({UpdatePermission.class, InsertPermission.class, DeletePermission.class})
    public class UpdateToolAction extends FormViewAction<ToolUploadForm>
    {
        private SkylineTool _tool;

        @Override
        public void validateCommand(ToolUploadForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(ToolUploadForm form, boolean reshow, BindException errors)
        {
            // Without a toolId the shared JSP would draw the add-a-new-tool form instead.
            SkylineTool tool = requireToolInContainer(form.getToolId(), getContainer());

            // Refuse before the form is drawn. handlePost refuses too, but only after the upload.
            if (!tool.getLatest())
            {
                errors.reject(ERROR_MSG, notLatestVersionMessage(tool));
                return new SimpleErrorView(errors);
            }

            return new JspView<>("/org/labkey/skylinetoolsstore/view/SkylineToolsStoreUpload.jsp", form, errors);
        }

        @Override
        public boolean handlePost(ToolUploadForm form, BindException errors) throws Exception
        {
            SkylineTool currentVersion = requireToolInContainer(form.getToolId(), getContainer());

            // Publishing demotes the version it supersedes. Done from an older version, that leaves
            // two rows flagged latest and the tool is listed twice.
            if (!currentVersion.getLatest())
            {
                errors.reject(ERROR_MSG, notLatestVersionMessage(currentVersion));
                return false;
            }

            SkylineTool tool = readToolFromUpload(getFileMap().get("toolZip"), errors);
            if (tool == null)
                return false;

            if (!tool.getIdentifier().equalsIgnoreCase(currentVersion.getIdentifier()))
            {
                errors.reject(ERROR_MSG, "The Skyline tool in the zip file did not have the same " +
                        "identifier as the Skyline tool being updated.");
                return false;
            }
            // Version must be newer than the existing versions.
            for (SkylineTool existing : SkylineToolsStoreManager.get()
                    .getToolsByIdentifier(currentVersion.getIdentifier()))
            {
                if (SkylineTool.compareVersions(tool.getVersion(), existing.getVersion()) <= 0)
                {
                    errors.reject(ERROR_MSG, "The Skyline Tool zip file contained version " +
                            tool.getVersion() + ", which is not newer than the stored version " +
                            existing.getVersion() + ".");
                    return false;
                }
            }

            // A version may rename its own tool, but it must not take a name another tool already
            // publishes under. Skyline builds details.view?name= for every tool it lists, and
            // getLatestTool picks arbitrarily when two tools share a name.
            for (SkylineTool sameName : SkylineToolsStoreManager.get().getTools(
                    new SimpleFilter(FieldKey.fromParts("Name"), tool.getName())))
            {
                if (!sameName.getIdentifier().equalsIgnoreCase(tool.getIdentifier()))
                {
                    errors.reject(ERROR_MSG, "Another tool is already published under the name " +
                            tool.getName() + ".");
                    return false;
                }
            }

            // Create the child folder for the tool version and store its zip, icon and docs
            Container versionContainer = createVersionFolder(getContainer().getParent(), tool,
                    getFileMap().get("toolZip"), Collections.emptyList(), currentVersion, errors);
            // createVersionFolder has already rejected with the reason.
            if (versionContainer == null)
                return false;

            // Locks the version being superseded for the length of the transaction. A second publish
            // of the same version waits here, then reads the demoted row and is refused, rather than
            // passing the check below and committing a second latest row.
            Lock currentVersionLock = new ServerPrimaryKeyLock(true,
                    SkylineToolsStoreSchema.getInstance().getTableInfoSkylineTool(),
                    currentVersion.getRowId());

            // The insert and the demotion must be in a transaction.
            boolean stored = false;
            try (DbScope.Transaction transaction = SkylineToolsStoreSchema.getInstance().getSchema()
                         .getScope().ensureTransaction(currentVersionLock))
            {
                // Read again inside the transaction. The check above ran before the upload so the row may have been
                // demoted since.
                SkylineTool current = SkylineToolsStoreManager.get().getTool(currentVersion.getRowId());
                if (current == null || !current.getLatest())
                {
                    errors.reject(ERROR_MSG, notLatestVersionMessage(currentVersion));
                    return false;
                }

                tool.setLatest(true);
                _tool = SkylineToolsStoreManager.get().insertTool(versionContainer, getUser(), tool);

                current.setLatest(false);
                SkylineToolsStoreManager.get().updateTool(getContainer(), getUser(), current);

                transaction.commit();
                stored = true;
            }
            finally
            {
                // The folder and the zip are not covered by the transaction, so a rollback would
                // otherwise leave a folder with no row, blocking a retry of this same version.
                if (!stored)
                    discardVersionFolder(versionContainer);
            }
            return true;
        }

        @Override
        public URLHelper getSuccessURL(ToolUploadForm form)
        {
            return SkylineToolStoreUrls.getToolDetailsUrl(_tool);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild(getToolStoreNavFromToolFolder(getContainer()));
            root.addChild("Upload New Version");
        }

        /** Shared, so the form and the post cannot refuse an older version with different wording. */
        private static String notLatestVersionMessage(SkylineTool tool)
        {
            return "Version " + tool.getVersion() + " is not the latest version of " + tool.getName() +
                    ". Publish a new version from the latest one.";
        }
    }

    /**
     * Shared by both upload actions. InsertToolAction ignores toolId, UpdateToolAction ignores
     * toolOwners - a new version inherits its owners from the version it supersedes.
     */
    public static class ToolUploadForm extends ReturnUrlForm
    {
        private int _toolId;
        private String _toolOwners;

        public int getToolId()
        {
            return _toolId;
        }

        public void setToolId(int toolId)
        {
            _toolId = toolId;
        }

        public String getToolOwners()
        {
            return _toolOwners;
        }

        public void setToolOwners(String toolOwners)
        {
            _toolOwners = toolOwners;
        }
    }


    /**
     * Creates the child folder for a tool version and stores its zip, icon and docs. Shared by
     * InsertToolAction and UpdateToolAction, which differ only in what they check first.
     *
     * Deliberately does not insert the row. The caller owns that, so it can put the insert in a
     * transaction with whatever else has to succeed alongside it, and can hand the folder to
     * discardVersionFolder if that transaction does not commit. None of the work here is
     * transactional, so it must not sit inside one - it creates a container and moves the zip.
     *
     * Either returns a folder holding the whole version or, in case of failure, removes its own partial
     * work before returning null.
     *
     * @param storeContainer  the tool store folder to create the version's folder under
     * @param previousVersion the version being superseded, or null for a brand-new tool. Supplies the
     *                        permissions, supplementary files and docs that carry forward, which is
     *                        how a tool's owners keep their access across versions.
     * @return the new version's folder, or null if it could not be stored, in which case the reason
     *         has been added to errors. Does not throw - an IOException on the way is reported the
     *         same way, so the caller never has to tell the two apart.
     */
    private Container createVersionFolder(Container storeContainer, SkylineTool tool, MultipartFile zip,
                                          List<User> owners, @Nullable SkylineTool previousVersion,
                                          BindException errors)
    {
        // A throw after makeContainer would strand the folder, and makeContainer refuses a name it
        // has already used. The finally removes it.
        Container c = null;
        boolean populated = false;
        try
        {
            Container previousContainer = previousVersion != null ? previousVersion.lookupContainer() : null;
            Set<String> carryForward = previousVersion != null
                    ? getSupplementaryFileBasenames(previousVersion) : Collections.emptySet();

            c = makeContainer(storeContainer, toolFolderName(tool), owners,
                    RoleManager.getRole(EditorRole.class));
            // makeContainer returns null rather than throwing when the folder name is not legal or is
            // already taken, for example by a folder left behind by an upload that failed part way.
            if (c == null)
            {
                errors.reject(ERROR_MSG, "Could not create a folder named " + toolFolderName(tool) +
                        " for this version. Check whether a folder by that name already exists.");
                return null;
            }

            copyContainerPermissions(previousContainer, c);

            File storedZip = makeFile(c, zip.getOriginalFilename());
            zip.transferTo(storedZip);
            tool.writeIconToFile(makeFile(c, "icon.png"), "png");

            // Docs come from tool-inf/docs/ in the zip, or carry forward from the previous version.
            boolean hasDocs = extractDocsFromZip(storedZip.toPath(), getLocalPath(c));
            if (!hasDocs && previousContainer != null)
            {
                Path oldDocs = getLocalPath(previousContainer).resolve("docs");
                if (Files.isDirectory(oldDocs))
                    FileUtil.copyDirectory(oldDocs, getLocalPath(c).resolve("docs"));
            }

            if (previousContainer != null)
            {
                // Resolved by the name on disk. makeFile would run it through makeLegalName first,
                // turning an existing "User's Guide.pdf" into "User_s Guide.pdf", which is not there.
                Path previousFiles = getLocalPath(previousContainer);
                Path newFiles = getLocalPath(c);
                for (String copyFile : carryForward)
                    FileUtils.copyFile(previousFiles.resolve(copyFile).toFile(),
                            newFiles.resolve(copyFile).toFile(), true);
            }

            populated = true;
            return c;
        }
        catch (IOException e)
        {
            // The message names a server path, so it goes to the log only. Warn, not error - the
            // upload was refused, not broken.
            LOG.warn("Could not store version {} of {}", tool.getVersion(), tool.getName(), e);
            errors.reject(ERROR_MSG, "The tool version could not be stored.");
            return null;
        }
        finally
        {
            if (c != null && !populated)
                discardVersionFolder(c);
        }
    }

    /** Removes a tool version folder that never got a tool row because upload failed part way.
     * Logs rather than throws if it cannot.
     */
    private void discardVersionFolder(Container versionContainer)
    {
        final String failed = "Could not remove the folder for a tool version that was never stored: {}";
        try
        {
            // delete returns false rather than throwing when the folder still has children of its own.
            if (!ContainerManager.delete(versionContainer, getUser()))
                LOG.error(failed, versionContainer.getPath());
        }
        catch (Exception e)
        {
            LOG.error(failed, versionContainer.getPath(), e);
        }
    }

    /** The child folder a tool version lives in, for example _tool_MSstats_4.0 */
    private static String toolFolderName(SkylineTool tool)
    {
        return "_tool_" + FileUtil.getBaseName(FileUtil.makeLegalName(tool.getName())) + "_" +
                FileUtil.makeLegalName(tool.getVersion());
    }

    /** Reads the uploaded zip, rejecting anything that is not a usable tool. Null if rejected. */
    private SkylineTool readToolFromUpload(MultipartFile zip, BindException errors)
    {
        if (zip == null || StringUtils.isEmpty(zip.getOriginalFilename()))
        {
            errors.reject(ERROR_MSG, "You did not submit a file.");
            return null;
        }

        SkylineTool tool;
        try
        {
            tool = getToolFromZip(zip);
        }
        catch (IOException e)
        {
            // Only a ZipException describes the file. Any other IOException names a path on the
            // server, which a tool owner should not see, so it goes to the log only.
            LOG.warn("Could not read the uploaded tool zip {}", zip.getOriginalFilename(), e);
            String detail = e instanceof ZipException ? " " + e.getMessage() : "";
            errors.reject(ERROR_MSG, "The file was not a valid Skyline tool zip file." + detail);
            return null;
        }
        if (tool == null)
        {
            errors.reject(ERROR_MSG, "The file was not a valid Skyline Tool zip file.");
            return null;
        }
        if (!tool.getMissingValues().isEmpty())
        {
            errors.reject(ERROR_MSG, "The tool was missing the following properties: " +
                    StringUtils.join(tool.getMissingValues(), ", "));
            return null;
        }
        // Skyline reads Version through System.Version.TryParse and treats anything else as no
        // version at all, so a tool stored with one would show no version in every Skyline client.
        if (SkylineTool.parseSkylineVersion(tool.getVersion()) == null)
        {
            errors.reject(ERROR_MSG, "Skyline cannot read \"" + tool.getVersion() + "\" as a version. " +
                    "Use two to four numbers separated by dots, for example 1.0 or 1.2.3.");
            return null;
        }
        return tool;
    }

    private void redirectToToolStoreContainer(SkylineTool tool, ActionURL originalUrl)
    {
        // If the container in the request URL does not match the parent of the container associated
        // with the tool, redirect to the correct URL
        Container toolContainerParent = tool.getContainerParent();
        if(toolContainerParent != null)
        {
            if(!toolContainerParent.equals(getContainer()))
            {
                ActionURL url = originalUrl.clone();
                url.setContainer(toolContainerParent);
                throw new RedirectException(url);
            }
        }
    }

    /**
     * Uploads one supplementary file for a tool. Addressed to the tool's own container.
     */
    @RequiresPermission(InsertPermission.class)
    public static class InsertSupplementAction extends FormViewAction<SupplementUploadForm>
    {
        private SkylineTool _tool;

        @Override
        public void validateCommand(SupplementUploadForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(SupplementUploadForm form, boolean reshow, BindException errors)
        {
            requireToolInContainer(form.getToolId(), getContainer());

            return new JspView<>("/org/labkey/skylinetoolsstore/view/SkylineToolSupplementUpload.jsp", form, errors);
        }

        @Override
        public boolean handlePost(SupplementUploadForm form, BindException errors) throws Exception
        {
            _tool = requireToolInContainer(form.getToolId(), getContainer());

            // Uploaded files are multipart parts, not request parameters, so they do not bind to the form.
            MultipartFile suppFile = getFileMap().get("suppFile");
            if (suppFile == null || StringUtils.isEmpty(suppFile.getOriginalFilename()))
            {
                errors.reject(ERROR_MSG, "Please submit a file.");
                return false;
            }

            File targetFile = makeFile(getContainer(), FileUtil.makeLegalName(suppFile.getOriginalFilename()));
            if (targetFile.exists())
            {
                errors.reject(ERROR_MSG, "A supplementary file with that name already exists.");
                return false;
            }

            suppFile.transferTo(targetFile);
            return true;
        }

        @Override
        public URLHelper getSuccessURL(SupplementUploadForm form)
        {
            return SkylineToolStoreUrls.getToolDetailsUrl(_tool);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild(getToolStoreNavFromToolFolder(getContainer()));
            root.addChild("Upload Supplementary File");
        }
    }

    public static class SupplementUploadForm
    {
        private int _toolId;

        public int getToolId()
        {
            return _toolId;
        }

        public void setToolId(int toolId)
        {
            _toolId = toolId;
        }
    }

    /**
     * Deletes one supplementary file from a tool.
     *
     * A MutatingApiAction because the details page calls this over ajax and hides the file's tile on
     * success.
     *
     * Addressed to the tool's own container, not the store folder, so the annotation checks the
     * folder that actually holds the file. Callers must build the URL with
     * SkylineToolStoreUrls.getToolActionUrl.
     */
    @RequiresPermission(DeletePermission.class)
    public static class DeleteSupplementAction extends MutatingApiAction<SupplementForm>
    {
        @Override
        public Object execute(SupplementForm form, BindException errors) throws Exception
        {
            final SkylineTool tool = requireToolInContainer(form.getToolId(), getContainer());

            File targetDel = makeFile(getContainer(), form.getSuppFile());

            // The tool's own zip and icon are not supplementary files, so they are not deletable here.
            if (!targetDel.isFile() ||
                targetDel.getName().equalsIgnoreCase("icon.png") ||
                targetDel.getName().equalsIgnoreCase(tool.getZipName()))
            {
                throw new NotFoundException("No supplementary file named " + form.getSuppFile() +
                        " for tool " + tool.getName());
            }

            if (!targetDel.delete())
            {
                // The message names a server path, so it goes to the log only.
                LOG.warn("Could not delete the supplementary file {}", targetDel.getAbsolutePath());
                errors.reject(ERROR_MSG, "The file " + form.getSuppFile() + " could not be deleted.");
                return null;
            }

            return new ApiSimpleResponse("success", true);
        }
    }

    public static class SupplementForm
    {
        private int _toolId;
        private String _suppFile;

        public int getToolId()
        {
            return _toolId;
        }

        public void setToolId(int toolId)
        {
            _toolId = toolId;
        }

        public String getSuppFile()
        {
            return _suppFile;
        }

        public void setSuppFile(String suppFile)
        {
            _suppFile = suppFile;
        }
    }

    /**
     * Removes a tool and every one of its versions.
     *
     * A MutatingApiAction because both callers post over ajax.
     */
    @RequiresSiteAdmin
    public static class DeleteAction extends MutatingApiAction<IdForm>
    {
        @Override
        public Object execute(IdForm idForm, BindException errors) throws Exception
        {
            final SkylineTool tool = SkylineToolsStoreManager.get().getTool(idForm.getToolId());

            if(tool == null)
            {
                errors.reject(ERROR_MSG, "Tool with id " + idForm.getToolId() + " does not exist.");
                return null;
            }

            requireToolAddressableFrom(tool, getContainer());

            // Resolved before the delete, and from the tool rather than from the request, because
            // this action may be addressed to the tool's own folder, which it is about to remove.
            Container toolStoreContainer = tool.getContainerParent() != null ? tool.getContainerParent() : getContainer();

            // Deliberately not one transaction. Deleting a container deletes its files from disk
            // inside the transaction and not through a commit task, so a rollback would put the rows
            // back with the zips, icons and documentation already gone. Every folder is checked
            // first instead, so the refusal that actually happens costs nothing.
            SkylineTool[] versions = SkylineToolsStoreManager.get().getToolsByIdentifier(tool.getIdentifier());
            List<Container> versionFolders = new ArrayList<>();
            for (SkylineTool toDelete : versions)
            {
                Container versionContainer = toDelete.lookupContainer();
                if (versionContainer == null)
                {
                    errors.reject(ERROR_MSG, "Failed to look up the folder containing " + toDelete.getName() +
                            " version " + toDelete.getVersion() + ". Nothing was deleted.");
                    return null;
                }
                // The one condition ContainerManager.delete refuses on, checked before anything goes.
                if (!versionContainer.getChildren().isEmpty())
                {
                    errors.reject(ERROR_MSG, "The folder containing " + toDelete.getName() + " version " +
                            toDelete.getVersion() + " has folders of its own, so it cannot be deleted. " +
                            "Nothing was deleted.");
                    return null;
                }
                versionFolders.add(versionContainer);
            }

            for (int i = 0; i < versions.length; i++)
            {
                // The folders resolved above are the ones deleted, rather than looking each up again.
                // This refuses only if a folder gained a child in between, and there is no undo - the
                // versions already removed stay removed, so say so.
                if (!ContainerManager.delete(versionFolders.get(i), getUser()))
                {
                    errors.reject(ERROR_MSG, "The folder containing " + versions[i].getName() + " version " +
                            versions[i].getVersion() + " could not be deleted. Versions removed before it " +
                            "are gone.");
                    return null;
                }
            }

            ApiSimpleResponse response = new ApiSimpleResponse("success", true);
            response.put("successUrl", SkylineToolStoreUrls.getToolStoreHomeUrl(toolStoreContainer, getUser()).getLocalURIString());
            return response;
        }
    }

    public static class IdForm extends ReturnUrlForm
    {
        private String _name;
        private int _toolId;

        public IdForm()
        {
        }

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public int getToolId()
        {
            return _toolId;
        }

        public void setToolId(int toolId)
        {
            _toolId = toolId;
        }
    }

    /**
     * Deletes only the newest version of a tool and promotes the previous one.
     *
     * The promotion writes to the previous version's folder, outside the one the permissions annotation
     * checks. This is ok because all of a tool's version folders carry the same policy - see UpdateToolAction.
     */
    @RequiresAllOf({UpdatePermission.class, DeletePermission.class})
    public static class DeleteLatestAction extends MutatingApiAction<DeleteLatestForm>
    {
        @Override
        public Object execute(DeleteLatestForm form, BindException errors) throws Exception
        {
            final SkylineTool tool = requireToolInContainer(form.getToolId(), getContainer());

            // The details page hides this on older versions, but a stale page can still post.
            if (!tool.getLatest())
            {
                errors.reject(ERROR_MSG, "Version " + tool.getVersion() + " is not the latest version of " +
                        tool.getName() + ".");
                return null;
            }

            ActionURL returnUrl = form.getReturnActionURL();

            // Resolve the tool store container before the tool's own container is deleted.
            Container toolStoreContainer = tool.getContainerParent() != null ? tool.getContainerParent() : getContainer();

            SkylineTool[] tools = sortToolsByCreateDate(SkylineToolsStoreManager.get().getToolsByIdentifier(tool.getIdentifier()));

            // Needs an older version to promote in its place.
            if (tools.length == 1)
            {
                errors.reject(ERROR_MSG, "Cannot delete the only version of " + tool.getName());
                return null;
            }

            if (!tools[0].getRowId().equals(tool.getRowId()))
                throw new IllegalStateException("Version " + tool.getVersion() + " of " + tool.getName() +
                        " is flagged as the latest but is not the most recently created version.");

            SkylineTool newLatest = tools[1];
            Container newLatestContainer = newLatest.lookupContainer();
            if (newLatestContainer == null)
                throw new NotFoundException("Failed to look up the folder containing " + newLatest.getName() +
                        " version " + newLatest.getVersion() + ".");

            // Refuse before anything changes. ContainerManager.delete returns false for a folder
            // with children, and the commit task below discards that.
            if (!getContainer().getChildren().isEmpty())
            {
                errors.reject(ERROR_MSG, "The folder containing " + tool.getName() + " version " +
                        tool.getVersion() + " has child folders and could not be deleted, " +
                        "so nothing was changed.");
                return null;
            }

            // addCommitTask takes a Runnable, so the delete's false return needs somewhere to go.
            AtomicBoolean deleted = new AtomicBoolean();

            // UpdateToolAction takes this same lock on the version it supersedes, so a publish on
            // top of this one cannot interleave and leave two rows flagged latest.
            Lock latestVersionLock = new ServerPrimaryKeyLock(true,
                    SkylineToolsStoreSchema.getInstance().getTableInfoSkylineTool(), tool.getRowId());

            try (DbScope.Transaction transaction = SkylineToolsStoreSchema.getInstance().getSchema()
                         .getScope().ensureTransaction(latestVersionLock))
            {
                // The tool was read before the lock was held, so a publish may have demoted it since.
                SkylineTool current = SkylineToolsStoreManager.get().getTool(tool.getRowId());
                if (current == null || !current.getLatest())
                {
                    errors.reject(ERROR_MSG, "Version " + tool.getVersion() + " is no longer the latest version of " +
                            tool.getName() + ".");
                    return null;
                }

                // Re-read rather than writing back the bean loaded above to get the updated download count.
                SkylineTool promoted = SkylineToolsStoreManager.get().getTool(newLatest.getRowId());
                if (promoted == null)
                    throw new NotFoundException("Could not find version " + newLatest.getVersion() +
                            " of " + newLatest.getName() + ".");

                promoted.setLatest(true);
                SkylineToolsStoreManager.get().updateTool(newLatestContainer, getUser(), promoted);

                // Demote here rather than letting the container delete take the row with the folder.
                // If that delete fails, this is what keeps exactly one row flagged latest.
                current.setLatest(false);
                SkylineToolsStoreManager.get().updateTool(getContainer(), getUser(), current);

                // Outside the transaction - ContainerManager.delete runs its own, and an enclosing
                // one costs it the deadlock retry.
                transaction.addCommitTask(
                        () -> deleted.set(ContainerManager.delete(getContainer(), getUser())),
                        DbScope.CommitTaskOption.POSTCOMMIT);

                transaction.commit();
            }

            // Only reached when the commit succeeded, so the promotion and the demotion are stored
            // and only the folder is left behind.
            if (!deleted.get())
            {
                errors.reject(ERROR_MSG, "Version " + tool.getVersion() + " of " + tool.getName() +
                        " is no longer the latest version, but its folder could not be deleted.");
                return null;
            }

            if (returnUrl != null)
            {
                if (!tool.getName().equals(newLatest.getName()) && returnUrl.getParameter("name") != null)
                    returnUrl.replaceParameter("name", newLatest.getName());

                if (returnUrl.getParameter("version") != null && returnUrl.getParameter("version").equals(tool.getVersion()))
                    returnUrl.deleteParameter("version");
            }

            URLHelper successUrl = returnUrl != null ? returnUrl :
                    SkylineToolStoreUrls.getToolStoreHomeUrl(toolStoreContainer, getUser());

            ApiSimpleResponse response = new ApiSimpleResponse("success", true);
            response.put("successUrl", successUrl.getLocalURIString());
            return response;
        }
    }

    /** IdForm extends ReturnUrlForm, so the page to come back to binds as returnUrl. */
    public static class DeleteLatestForm extends IdForm
    {
    }

    /**
     * Left as an AbstractController with an empty checkPermissions() on purpose, unlike the mutating
     * actions in this controller.
     *
     * This is an anonymous GET that Skyline clients call directly, and CSRF validation only
     * applies to non-GET requests, so routing it through the framework would buy no security.
     * Same reasoning for GetToolsApiAction.
     */
    @RequiresNoPermission
    public class DownloadToolAction extends AbstractController implements PermissionCheckable
    {
        public static final String DOWNLOADED_COOKIE_PREFIX = "downloadtool";

        @Override
        public ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse) throws Exception
        {
            final int id = NumberUtils.toInt(httpServletRequest.getParameter("id"), -1);
            final String toolName = httpServletRequest.getParameter("name");
            final String toolLsid = httpServletRequest.getParameter("lsid");
            SkylineTool tool = null;

            if (id > 0 && (tool = SkylineToolsStoreManager.get().getTool(id)) == null)
                throw new NotFoundException("Could not find tool with id " + id);
            else if (toolName != null &&
                     (tool = SkylineToolsStoreManager.get().getLatestTool(URLDecoder.decode(toolName.trim(), "UTF-8"))) == null)
                throw new NotFoundException("Could not find tool with name " + toolName);
            else if (toolLsid != null &&
                     (tool = SkylineToolsStoreManager.get().getToolLatestByIdentifier(toolLsid.trim())) == null)
                throw new NotFoundException("Could not find tool with LSID " + toolLsid);

            if (tool == null)
                throw new NotFoundException("Could not do tool lookup");

            if (recordDownload(httpServletRequest, tool.getRowId()))
            {
                // Cookie expires after 1 day
                final int expires = 24 * 60 * 60;

                // Download counter is an incidental write on a GET action — use ignoreSqlUpdates()
                // to avoid the dev-mode mutating SQL assertion (like auditing writes)
                try (var ignored = SpringActionController.ignoreSqlUpdates())
                {
                    SkylineToolsStoreManager.get().recordToolDownload(tool);
                }

                DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, expires);

                httpServletResponse.setHeader("Set-Cookie",
                    DOWNLOADED_COOKIE_PREFIX + tool.getRowId() + "=1; " +
                    "Expires=" + df.format(calendar.getTime()) + "; " +
                    "Max-Age=" + expires + "; " +
                    "Path=/; Domain=;");
            }

            Container toolContainer = ContainerManager.getForId(tool.getContainerId());
            if (toolContainer == null)
            {
                throw new NotFoundException("Tool container not found");
            }

            org.labkey.api.util.Path path = WebdavService.getPath().append(toolContainer.getParsedPath()).append(FileContentService.FILES_LINK).append(tool.getZipName());
            WebdavResource resource = WebdavService.get().getResolver().lookup(path);
            if (resource == null || !resource.isFile())
                throw new NotFoundException("Resource could not be found: " + path.toString());

            // Issue 49580: https://www.labkey.org/MacCoss/Issue%20Tracker/issues-details.view?issueId=49580
            // Add our own 'Content-Disposition' header so that it overwrites the one set in
            // ResponseHelper.setContentDisposition(HttpServletResponse response, ContentDispositionType type, @NotNull String filename)
            // Skyline expects the 'Content-Disposition' header value to look like this: filename="MSstatsShiny.zip".
            Map<String, String> headers = Collections.singletonMap("Content-Disposition", "filename=\""+ tool.getZipName() + "\"");
            PageFlowUtil.streamFile(getViewContext().getResponse(),
                    headers,
                    resource.getName(),
                    resource.getInputStream(getUser()),
                    true);
            return null;
        }

        protected boolean recordDownload(HttpServletRequest httpServletRequest, int toolId)
        {
            if (httpServletRequest.getParameter("noSaveCookie") != null)
            {
                // To prevent incrementing download counter when the tool is downloaded
                // from Skyline for running tests.
                return false;
            }
            else if (httpServletRequest.getCookies() == null)
            {
                return true;
            }

            for (Cookie cookie : httpServletRequest.getCookies())
                if (cookie.getName().equalsIgnoreCase("downloadtool" + toolId) && cookie.getValue().equals("1"))
                    return false;

            return true;
        }

        @Override
        public void checkPermissions() throws UnauthorizedException
        {

        }
    }

    @RequiresNoPermission
    @ActionNames("details, toolDetails")
    public class DetailsAction extends SimpleViewAction<ViewToolDetailsForm> implements PermissionCheckable
    {
        private SkylineTool _tool = null;

        @Override
        public ModelAndView getView(ViewToolDetailsForm form, BindException errors) throws Exception
        {
            Integer toolId = form.getId();

            if (toolId != null)
            {
                // Do lookup by id if we are given an id.
                _tool = SkylineToolsStoreManager.get().getTool(toolId);
                if (_tool == null)
                {
                    errors.reject(SpringActionController.ERROR_MSG, "Could not find tool " + " by Id " + toolId);
                    return new SimpleErrorView(errors);
                }
            }
            else
            {
                // Lookup tool by name and version (optional) if we were not given a toolId.
                String toolName = form.getName();
                if (StringUtils.trimToNull(toolName) == null)
                {
                    errors.reject(SpringActionController.ERROR_MSG, "No tool name found in request");
                    return new SimpleErrorView(errors);
                }

                String version = form.getVersion();
                if (StringUtils.trimToNull(version) != null)
                {
                    // Lookup by version if we were given one
                    _tool = SkylineToolsStoreManager.get().getToolByNameAndVersion(toolName, version);
                }
                else
                {
                    // Otherwise, return the latest version of this tool
                    _tool = SkylineToolsStoreManager.get().getLatestTool(toolName);
                }

                if (_tool == null)
                {
                    StringBuilder msg = new StringBuilder("Could not find tool ").append(" by name ").append(toolName);
                    if (version != null)
                        msg.append(" and version ").append(version);
                    errors.reject(SpringActionController.ERROR_MSG,  msg.toString());
                    return new SimpleErrorView(errors);
                }
            }

            if (_tool.lookupContainer() == null)
            {
                errors.reject(ERROR_MSG, "The folder containing " + _tool.getName() +
                        " no longer exists, so its details cannot be shown.");
                return new SimpleErrorView(errors);
            }

            // If the container in the request URL does not match the parent of the container associated
            // with the tool, redirect to the correct URL
            redirectToToolStoreContainer(_tool, getViewContext().getActionURL());

            return new SkylineToolDetails(_tool);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            // getContainer() should be the store container due to the redirect, except on error views.
            root.addChild(getToolStoreNav(getContainer()));
            if (_tool != null)
                root.addChild(_tool.getName());
        }
    }

    public static class ViewToolDetailsForm
    {
        private Integer _id;
        private String _name;
        private String _version;

        public Integer getId()
        {
            return _id;
        }

        public void setId(Integer id)
        {
            _id = id;
        }

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getVersion()
        {
            return _version;
        }

        public void setVersion(String version)
        {
            _version = version;
        }
    }

    /**
     * Sets the tool owners on every one of a tool's version folders.
     */
    @RequiresSiteAdmin
    public class SetOwnersAction extends FormViewAction<SetOwnersForm>
    {
        private URLHelper _successURL;

        @Override
        public void validateCommand(SetOwnersForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(SetOwnersForm form, boolean reshow, BindException errors)
        {
            SkylineTool tool = SkylineToolsStoreManager.get().getTool(form.getToolId());
            if (tool == null)
                throw new NotFoundException("Could not find tool with Id " + form.getToolId());
            requireToolAddressableFrom(tool, getContainer());
            if (!reshow)
                // Prefill the box. handlePost replaces the whole list, so a blank form strips every owner.
                form.setToolOwners(StringUtils.join(getToolOwners(tool), ", "));

            return new JspView<>("/org/labkey/skylinetoolsstore/view/SkylineToolManageOwners.jsp", form, errors);
        }

        @Override
        public boolean handlePost(SetOwnersForm form, BindException errors) throws Exception
        {
            Pair<ArrayList<User>, ArrayList<String>> parsedOwners = parseToolOwnerString(form.getToolOwners());
            ArrayList<User> toolOwnersUsers = parsedOwners.first;
            ArrayList<String> toolOwnersInvalid = parsedOwners.second;

            if (!toolOwnersInvalid.isEmpty())
            {
                errors.reject(ERROR_MSG, UNKNOWN_USERS + StringUtils.join(toolOwnersInvalid, ", "));
                return false;
            }

            final SkylineTool tool = SkylineToolsStoreManager.get().getTool(form.getToolId());
            if (tool == null)
                throw new NotFoundException("Could not find tool with Id " + form.getToolId());

            requireToolAddressableFrom(tool, getContainer());

            // Get all the version folders
            List<Container> versionFolders = new ArrayList<>();
            for (SkylineTool version : SkylineToolsStoreManager.get().getToolsByIdentifier(tool.getIdentifier()))
            {
                Container versionFolder = version.lookupContainer();
                if (versionFolder == null)
                    throw new NotFoundException("Failed to look up the folder containing " + version.getName() +
                            " version " + version.getVersion() + ".");
                versionFolders.add(versionFolder);
            }

            // Set the tool owners on every version of the tool
            try (DbScope.Transaction transaction = CoreSchema.getInstance().getSchema().getScope().ensureTransaction())
            {
                for (Container versionFolder : versionFolders)
                    setToolOwners(versionFolder, toolOwnersUsers);
                transaction.commit();
            }

            Container toolStoreContainer = tool.getContainerParent() != null ? tool.getContainerParent() : getContainer();
            _successURL = form.getReturnUrlHelper(
                    SkylineToolStoreUrls.getToolStoreHomeUrl(toolStoreContainer, getUser()));
            return true;
        }

        @Override
        public URLHelper getSuccessURL(SetOwnersForm form)
        {
            return _successURL;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            // This action runs in the store folder, not a tool folder, so the store is getContainer()
            // itself rather than its parent.
            root.addChild(getToolStoreNav(getContainer()));
            root.addChild("Manage Tool Owners");
        }
    }

    public static class SetOwnersForm extends ReturnUrlForm
    {
        private int _toolId;
        private String _toolOwners;

        public int getToolId()
        {
            return _toolId;
        }

        public void setToolId(int toolId)
        {
            _toolId = toolId;
        }

        public String getToolOwners()
        {
            return _toolOwners;
        }

        public void setToolOwners(String toolOwners)
        {
            _toolOwners = toolOwners;
        }
    }

    /**
     * Edits one property of a tool, or replaces its icon, rewriting tool-inf/info.properties inside
     * the stored zip so the file and the database row stay in step.
     *
     * A MutatingApiAction because the details page calls this over ajax and reads the result in code.
     */
    @RequiresPermission(UpdatePermission.class)
    public static class UpdatePropertyAction extends MutatingApiAction<UpdatePropertyForm>
    {
        /**
         * The properties the details page lets an owner edit. SkylineTool.setProperty also accepts
         * name, version and identifier - these cannot be edited since they identify the tool to Skyline clients.
         */
        private static final Set<String> EDITABLE_PROPERTIES =
                Set.of("author", "description", "languages", "organization", "provider");

        @Override
        public Object execute(UpdatePropertyForm form, BindException errors) throws Exception
        {
            final SkylineTool tool = requireToolInContainer(form.getToolId(), getContainer());
            final Container container = getContainer();

            final String propName = form.getPropName();
            String propValue = "";
            String iconEntryName = null;

            // An icon arrives as a file part named propValue, the same name the text edits use.
            // getFileMap is empty rather than null when the post is not multipart.
            final MultipartFile icon = getFileMap().get("propValue");

            if (icon == null)
            {
                if (propName == null)
                {
                    errors.reject(ERROR_MSG, "No property was named to edit.");
                    return null;
                }
                if (!EDITABLE_PROPERTIES.contains(propName.toLowerCase()))
                {
                    errors.reject(ERROR_MSG, "The property " + propName + " cannot be edited here.");
                    return null;
                }
                // Form binding turns an empty value into null. Blanking a property is a normal edit,
                // so treat null as an empty value rather than a missing parameter.
                String submitted = form.getPropValue() == null ? "" : form.getPropValue();
                propValue = submitted.replace("\r", "").replace("\n", "\r\n");
            }
            else
            {
                // This name goes into the downloadable tool zip, so remove illegal characters and
                // ensure it has an accepted file extension - that is how the icon is found again.
                iconEntryName = FileUtil.makeLegalName(icon.getOriginalFilename());
                if (!Arrays.asList(VALID_ICON_EXTENSIONS).contains(
                        FileUtil.getExtension(iconEntryName.toLowerCase())))
                {
                    errors.reject(ERROR_MSG, "An icon file has to be named .png, .jpg, .jpeg or .gif.");
                    return null;
                }

                tool.setIcon(icon.getBytes());
                try
                {
                    // Validate now - writeIconToFile runs after the zip is replaced, so a bad image
                    // would otherwise fail with the stored zip already changed.
                    tool.validateIcon();
                }
                catch (IOException e)
                {
                    LOG.warn("Rejected an icon that could not be decoded for tool {}", tool.getName(), e);
                    errors.reject(ERROR_MSG, "The icon file is not an image this server can read.");
                    return null;
                }
            }

            File zipFile = makeFile(container, tool.getZipName());
            File tmpFile = makeFile(container, tool.getZipName() + "~");
            try
            {
                try (ZipFile zipIn = new ZipFile(zipFile);
                     ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tmpFile)))
                {
                    for (Enumeration e = zipIn.entries(); e.hasMoreElements();)
                    {
                        ZipEntry zipEntry = (ZipEntry)e.nextElement();
                        final String lowerName = zipEntry.getName().toLowerCase();

                        if (icon == null)
                        {
                            try (InputStream in = lowerName.equals("tool-inf/info.properties")
                                    ? tool.getInfoPropertiesStream(zipIn.getInputStream(zipEntry), propName, propValue)
                                    : zipIn.getInputStream(zipEntry))
                            {
                                ZipEntry newEntry = new ZipEntry(zipEntry.getName());
                                zipOut.putNextEntry(newEntry);
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = in.read(buf)) > 0)
                                    zipOut.write(buf, 0, len);
                                zipOut.closeEntry();
                            }
                        }
                        else if (!lowerName.startsWith("tool-inf/") ||
                                 !Arrays.asList(VALID_ICON_EXTENSIONS).contains(FileUtil.getExtension(lowerName)))
                        {
                            try (InputStream in = zipIn.getInputStream(zipEntry))
                            {
                                ZipEntry newEntry = new ZipEntry(zipEntry.getName());
                                zipOut.putNextEntry(newEntry);
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = in.read(buf)) > 0)
                                    zipOut.write(buf, 0, len);
                                zipOut.closeEntry();
                            }
                        }
                    }
                    if (icon != null)
                    {
                        zipOut.putNextEntry(new ZipEntry("tool-inf/" + iconEntryName));
                        try (InputStream in = icon.getInputStream())
                        {
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0)
                                zipOut.write(buf, 0, len);
                            zipOut.closeEntry();
                        }
                    }
                }

                // One step. Deleting first and then renaming loses the zip if the rename fails.
                try
                {
                    Files.move(tmpFile.toPath(), zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                catch (IOException e)
                {
                    throw new IOException("Could not replace " + zipFile + ". The tool zip is unchanged.", e);
                }
            }
            finally
            {
                // A rewrite that throws leaves the temp zip behind, and the store lists it as a supplementary file.
                if (tmpFile.exists() && !tmpFile.delete())
                    LOG.warn("Could not delete the temporary zip {}", tmpFile.getName());
            }

            if (icon == null)
            {
                // Re-read rather than writing back the bean loaded at the top to get the updated download count.
                SkylineTool current = SkylineToolsStoreManager.get().getTool(tool.getRowId());
                if (current == null)
                    throw new NotFoundException("Could not find tool with Id " + form.getToolId() + ".");

                current.setProperty(propName, propValue);
                SkylineToolsStoreManager.get().updateTool(container, getUser(), current);
            }
            else
            {
                tool.writeIconToFile(makeFile(container, "icon.png"), "png");
            }

            return new ApiSimpleResponse("success", true);
        }
    }

    public static class UpdatePropertyForm
    {
        private int _toolId;
        private String _propName;
        private String _propValue;

        public int getToolId()
        {
            return _toolId;
        }

        public void setToolId(int toolId)
        {
            _toolId = toolId;
        }

        public String getPropName()
        {
            return _propName;
        }

        public void setPropName(String propName)
        {
            _propName = propName;
        }

        public String getPropValue()
        {
            return _propValue;
        }

        public void setPropValue(String propValue)
        {
            _propValue = propValue;
        }
    }

    @RequiresNoPermission
    public class GetToolsApiAction extends AbstractController implements PermissionCheckable
    {
        @Override
        public ModelAndView handleRequestInternal(@NotNull HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse) throws IOException
        {
            StringBuilder sb = new StringBuilder();

            String toolName = getViewContext().getRequest().getParameter("toolName");
            SkylineTool tool;
            SkylineTool[] tools =
                (toolName == null || (tool = SkylineToolsStoreManager.get().getLatestTool(toolName)) == null) ?
                SkylineToolsStoreManager.get().getToolsLatest() : new SkylineTool[]{tool};

            if (tools.length > 1)
                sb.append("[");

            for (int i = 0; i < tools.length; ++i)
            {
                tool = tools[i];
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Authors", tool.getAuthors());
                jsonObject.put("Description", tool.getDescription());
                jsonObject.put("Downloads", tool.getDownloads());
                // The container does not really matter for the download URL, but try to set the right container in the URL.
                Container toolStoreContainer = tool.getContainerParent();
                if(toolStoreContainer == null)
                {
                    toolStoreContainer = getContainer();
                }
                jsonObject.put("DownloadUrl", new ActionURL(DownloadToolAction.class, toolStoreContainer).addParameter("id", tool.getRowId()).toString());
                jsonObject.put("IconUrl", tool.getIconUrl());
                jsonObject.put("Identifier", tool.getIdentifier());
                jsonObject.put("Languages", tool.getLanguages());
                jsonObject.put("Name", tool.getName());
                jsonObject.put("Organization", tool.getOrganization());
                jsonObject.put("Provider", tool.getProvider());
                jsonObject.put("Version", tool.getVersion());
                sb.append(jsonObject);
                if (i < tools.length - 1)
                    sb.append(',');
            }

            if (tools.length > 1)
                sb.append(']');

            httpServletResponse.setContentType("application/json");
            httpServletResponse.getOutputStream().write(sb.toString().getBytes());
            return null;
        }

        @Override
        public void checkPermissions() throws UnauthorizedException
        {

        }
    }
}
