/*
 * Copyright (c) 2020 LabKey Corporation
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

package org.labkey.cromwell;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.TableInfo;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusUrls;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.ApiKeyManager;
import org.labkey.api.security.RequiresAllOf;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.DOM;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.api.webdav.WebdavService;
import org.labkey.cromwell.pipeline.CromwellException;
import org.labkey.cromwell.pipeline.CromwellMetadata;
import org.labkey.cromwell.pipeline.CromwellPipelineJob;
import org.labkey.cromwell.pipeline.CromwellUtil;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.DIV;

public class CromwellController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(CromwellController.class);
    public static final String NAME = "cromwell";

    public static final String PROPS_CROMWELL = "cromwell_properties";
    public static final String PROP_CROMWELL_SERVER_URL = "cromwell_server_url";
    public static final String PROP_CROMWELL_SERVER_PORT = "cromwell_server_port";
    public static final String PROP_SCP_USER = "cromwell_scp_user";
    public static final String PROP_SCP_KEY_FILE = "cromwell_scp_key_filepath";
    public static final String PROP_SCP_PORT = "cromwell_scp_port";
    public static final String PROP_ALLOWED_PROJECTS = "cromwell_allowed_projects";
    public static final String PROPS_CROMWELL_USER = "cromwell_properties_user";
    public static final String PROP_USER_APIKEY = "panorama_apikey";

    public CromwellController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(AdminOperationsPermission.class)
    public static class CromwellSettingsAction extends FormViewAction<CromwellSettingsForm>
    {
        @Override
        public void validateCommand(CromwellSettingsForm form, Errors errors)
        {
            if (StringUtils.isBlank(form.getCromwellServer()))
            {
                errors.reject(ERROR_MSG, "Cromwell server URL cannot be blank.");
            }
            if (StringUtils.isBlank(form.getScpUser()))
            {
                errors.reject(ERROR_MSG, "SCP user name cannot be blank.");
            }
            if (StringUtils.isBlank(form.getScpKeyFilePath()))
            {
                errors.reject(ERROR_MSG, "SCP key file path cannot be blank.");
            }
            List<String> invalidProjectNames = form.invalidProjectNames();
            if(invalidProjectNames.size() > 0)
            {
                errors.reject(ERROR_MSG, "Invalid project names: " + StringUtils.join(invalidProjectNames, ", "));
            }
        }

        @Override
        public ModelAndView getView(CromwellSettingsForm form, boolean reshow, BindException errors)
        {
            if(!reshow)
            {
                PropertyManager.PropertyMap map = PropertyManager.getEncryptedStore().getWritableProperties(PROPS_CROMWELL, false);
                PropertyManager.getEncryptedStore().getWritableProperties(getUser(), getContainer(), PROPS_CROMWELL, true);
                if(map != null)
                {
                    form.setCromwellServer(map.get(PROP_CROMWELL_SERVER_URL));
                    form.setScpUser(map.get(PROP_SCP_USER));
                    form.setScpKeyFilePath(map.get(PROP_SCP_KEY_FILE));

                    String port = map.get(PROP_CROMWELL_SERVER_PORT);
                    if(port != null)
                    {
                        form.setPort(Integer.valueOf(port));
                    }
                    String scpPort = map.get(PROP_SCP_PORT);
                    if(scpPort != null)
                    {
                        form.setScpPort(Integer.valueOf(scpPort));
                    }
                    String allowedProjects = map.get(PROP_ALLOWED_PROJECTS);
                    if(allowedProjects != null)
                    {
                        form.setAllowedProjectNames(allowedProjects);
                    }

                }
            }
            JspView<CromwellSettingsForm> view = new JspView<>("/org/labkey/cromwell/view/cromwellSettings.jsp", form, errors);
            view.setFrame(WebPartView.FrameType.PORTAL);
            view.setTitle("Cromwell Settings");
            return view;
        }

        @Override
        public boolean handlePost(CromwellSettingsForm form, BindException errors)
        {
            PropertyManager.PropertyMap map = PropertyManager.getEncryptedStore().getWritableProperties(PROPS_CROMWELL, true);
            map.put(PROP_CROMWELL_SERVER_URL, form.getCromwellServer());
            map.put(PROP_CROMWELL_SERVER_PORT, form.getPort() != null ? String.valueOf(form.getPort()) : null);
            map.put(PROP_SCP_USER, form.getScpUser());
            map.put(PROP_SCP_KEY_FILE, form.getScpKeyFilePath());
            map.put(PROP_SCP_PORT, form.getScpPort() != null ? String.valueOf(form.getScpPort()) : null);
            map.put(PROP_ALLOWED_PROJECTS, form.getAllowedProjectNames());
            map.save();
            return true;
        }

        @Override
        public ModelAndView getSuccessView(CromwellSettingsForm form)
        {
            ActionURL adminConsoleUrl = PageFlowUtil.urlProvider(AdminUrls.class).getAdminConsoleURL();
            return new HtmlView(
                    DIV("Cromwell settings saved!",
                            BR(),
                            new Link.LinkBuilder("Back to Admin Console").href(adminConsoleUrl).build()));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Cromwell Settings");
        }

        @Override
        public URLHelper getSuccessURL(CromwellSettingsForm cromwellSettingsForm)
        {
            return null;
        }
    }

    public static class CromwellSettingsForm
    {
        private String _cromwellServer;
        private Integer _port;
        private String _scpUser;
        private String _scpKeyFilePath;
        private Integer _scpPort;
        private String _allowedProjectNames;

        public String getCromwellServer()
        {
            return _cromwellServer;
        }

        public void setCromwellServer(String cromwellServer)
        {
            _cromwellServer = cromwellServer;
        }

        public Integer getPort()
        {
            return _port;
        }

        public void setPort(Integer port)
        {
            _port = port;
        }

        public String getScpUser()
        {
            return _scpUser;
        }

        public void setScpUser(String scpUser)
        {
            _scpUser = scpUser;
        }

        public String getScpKeyFilePath()
        {
            return _scpKeyFilePath;
        }

        public void setScpKeyFilePath(String scpKeyFilePath)
        {
            _scpKeyFilePath = scpKeyFilePath;
        }

        public Integer getScpPort()
        {
            return _scpPort;
        }

        public void setScpPort(Integer scpPort)
        {
            _scpPort = scpPort;
        }

        public String getAllowedProjectNames()
        {
            return _allowedProjectNames;
        }

        public void setAllowedProjectNames(String allowedProjectNames)
        {
            _allowedProjectNames = allowedProjectNames;
        }

        public List<String> invalidProjectNames()
        {
            if (StringUtils.isBlank(_allowedProjectNames))
            {
                return Collections.emptyList();
            }
            String[] projNames = StringUtils.split(_allowedProjectNames, ',');
            List<String> invalidName = new ArrayList<>();
            for (String projName : projNames)
            {
                Container proj = ContainerManager.getForPath(projName.trim());
                if (proj == null)
                {
                    invalidName.add(projName);
                }
            }

            return invalidName;
        }
    }

    @RequiresAllOf({AdminPermission.class, CromwellPermission.class})
    public static class SubmitCromwellJobAction extends FormViewAction<CromwellJobForm>
    {
        @Override
        public void validateCommand(CromwellJobForm form, Errors errors)
        {
            if(!getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(CromwellModule.NAME)))
            {
                errors.reject(ERROR_MSG, "Cromwell module is not enabled in the container. Cannot submit Cromwell jobs.");
                return;
            }

            Workflow workflow = form.getWorkflow();
            if(workflow == null)
            {
                errors.reject(ERROR_MSG, "Could not find a workflow in the request.");
            }
        }

        @Override
        public ModelAndView getView(CromwellJobForm form, boolean reshow, BindException errors) throws Exception
        {
            Workflow workflow;
            var manager = CromwellManager.get();
            if(reshow)
            {
                workflow = form.getWorkflow();
                if(workflow == null)
                {
                    errors.reject(ERROR_MSG, "No workflow found in request");
                    return new SimpleErrorView(errors);
                }
                List<CromwellInput> formInputs = Workflow.createInputs(workflow);
                formInputs = Workflow.populateInputs(formInputs, getViewContext().getRequest().getParameterMap());
                form.setInputsArray(formInputs);
            }
            else
            {
                if(form.getJobId() != null)
                {
                    // We are making a copy of an existing Cromwell job.
                    CromwellJob job = manager.getCromwellJob(form.getJobId());
                    if(job == null)
                    {
                        errors.reject(ERROR_MSG, "Could not find a job with Id " + form.getJobId());
                        return new SimpleErrorView(errors);
                    }

                    workflow = manager.getWorkflow(job.getWorkflowId());
                    if(workflow == null)
                    {
                        errors.reject(ERROR_MSG, "Could not find a workflow with Id " + job.getWorkflowId());
                        return new SimpleErrorView(errors);
                    }
                    form.setWorkflow(workflow);
                    List<CromwellInput> inputList = job.getInputList();
                    for(CromwellInput input: inputList)
                    {
                        if(input.isWebdav())
                        {
                            // Webdav paths are converted to full URLs before saving in the jobs table
                            // Convert back to path
                            org.labkey.api.util.Path inputPath = getDecodedPath(input.getValue());
                            org.labkey.api.util.Path fileRootPath = getFileRootPath(getContainer());
                            if(inputPath != null && inputPath.startsWith(fileRootPath))
                            {
                                // If this is a path in the current container show the path under the file root.
                                // Otherwise, show the original value
                                org.labkey.api.util.Path folderPath = fileRootPath.relativize(inputPath);
                                input.setValue(folderPath.toString());
                            }
                        }
                    }
                    form.setInputsArray(job.getInputList());
                }
                else
                {
                    workflow = form.getWorkflow();
                    if(workflow == null)
                    {
                        errors.reject(ERROR_MSG, "Could not find a workflow with Id " + form.getWorkflowId());
                        return new SimpleErrorView(errors);
                    }
                    form.setWorkflow(workflow);
                    form.setInputsArray(Workflow.createInputs(workflow)); // Set the input fields based on the WDL definition
                }

                setInputApiKey(form.getInputsArray(), getUser());
            }
            return new JspView<>("/org/labkey/cromwell/view/createJob.jsp", form, errors);
        }

        @Override
        public boolean handlePost(CromwellJobForm form, BindException errors) throws Exception
        {
            Container container = getContainer();
            PipeRoot root = PipelineService.get().findPipelineRoot(container);
            if (root == null || !root.isValid())
            {
                errors.reject(ERROR_MSG, "No valid pipeline root found for " + container.getPath());
                return false;
            }

            Workflow workflow = form.getWorkflow();

            // We do not have form fields for the workflow inputs.  Read them from the request parameter map.
            List<CromwellInput> workflowInputs = Workflow.createInputs(workflow);
            workflowInputs = Workflow.populateInputs(workflowInputs, getViewContext().getRequest().getParameterMap());
            if(!validateInputs(workflowInputs, getUser(), errors))
            {
                return false;
            }

            CromwellManager cromwellManager = CromwellManager.get();

            ViewBackgroundInfo info = new ViewBackgroundInfo(container, getUser(), null);
            CromwellJob cromwellJob = new CromwellJob(workflow.getId(), container);
            cromwellJob.setInputList(workflowInputs);
            cromwellJob = cromwellManager.saveNewJob(cromwellJob, getUser());

            String panoramaApiKey;
            CromwellInput input = CromwellJob.getApiKeyInput(workflowInputs);
            panoramaApiKey = input != null ? input.getValue() : null;

            CromwellPipelineJob job = new CromwellPipelineJob(info, root, workflow, cromwellJob.getId(), panoramaApiKey);
            try
            {
                PipelineService.get().queueJob(job);
            }
            catch (PipelineValidationException e)
            {
                cromwellManager.deleteJob(cromwellJob);
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
            return true;
        }

        private boolean validateInputs(List<CromwellInput> workflowInputs, User user, BindException errors)
        {
            for(CromwellInput input: workflowInputs)
            {
                if(input.isApiKey())
                {
                    validateApiKey(user, errors, input);
                }
                else if(input.isWebdav())
                {
                    validateWebdavPath(user, errors, input);
                }
                else if(input.isLabkeyUrl())
                {
                    validateLabkeyUrl(user, errors, input);
                }
                else if(StringUtils.isBlank(input.getValue()))
                {
                    errors.reject(ERROR_MSG, input.getDisplayName() + ": Value cannot be blank");
                }
            }
            return !errors.hasErrors();
        }

        private void validateLabkeyUrl(User user, BindException errors, CromwellInput input)
        {
            var inputUrl = input.getValue();
            if (StringUtils.isBlank(inputUrl))
            {
                inputUrl = PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(getContainer()).getURIString();
                input.setValue(inputUrl);
            }
            try
            {
                ViewContext context = getViewContext();
                URLHelper urlHelper = new URLHelper(inputUrl);
                if(!urlHelper.isLocalUri(context))
                {
                    errors.reject(ERROR_MSG, input.getDisplayName() + ": Not a URL on host " + context.getActionURL().getHost() + " - " + inputUrl);
                    return;
                }
                ActionURL actionUrl = new ActionURL(PageFlowUtil.decode(inputUrl));
                Container c = ContainerManager.getForPath(actionUrl.getParsedPath());
                if(c == null)
                {
                    errors.reject(ERROR_MSG, input.getDisplayName() + ": Path '" + actionUrl.getParsedPath() + "' does not exist on the server");
                    return;
                }
                if(!c.hasPermission(user, InsertPermission.class))
                {
                    errors.reject(ERROR_MSG, input.getDisplayName() + ": User does not have permissions in folder - " + c.getPath());
                    return;
                }
            }
            catch (URISyntaxException e)
            {
                errors.reject(ERROR_MSG, input.getDisplayName() + ": URL could not be parsed. Error was - " + e.getMessage());
            }
            catch(IllegalArgumentException e)
            {
                errors.reject(ERROR_MSG, input.getDisplayName() + ": Invalid URL. Error was - " + e.getMessage());
            }
        }

        private void validateWebdavPath(User user, BindException errors, CromwellInput input)
        {
            var inputPath = input.getValue();
            if (StringUtils.isBlank(inputPath))
            {
                errors.reject(ERROR_MSG, input.getDisplayName() + ": Path cannot be blank");
            }
            else
            {
                org.labkey.api.util.Path labkeyPath;

                if(inputPath.startsWith("https:") || inputPath.startsWith("http:"))
                {
                    labkeyPath = getDecodedPath(inputPath);
                }
                else
                {
                    labkeyPath = getFileRootPath(getContainer()).append(org.labkey.api.util.Path.parse(inputPath));
                }

                if(labkeyPath == null)
                {
                    errors.reject(ERROR_MSG, input.getDisplayName() + ": Input path could not be resolved. Path should be on the same server.");
                    return;
                }

                WebdavResource resource = WebdavService.get().lookup(labkeyPath);

                if(resource == null)
                {
                    errors.reject(ERROR_MSG, input.getDisplayName() + ": Could not resolve to Webdav resource " + labkeyPath);
                    return;
                }

                if(input.isWebdavOutputDir())
                {
                    // This directory path will be created as a result of running the Cromwell job.  Check if the user can create it.
                    // TODO: Any other check here?
                    if(!resource.canCreate(user, false))
                    {
                        errors.reject(ERROR_MSG, input.getDisplayName() + ": User cannot create directory path: " + labkeyPath);
                    }
                }
                else {

                    Path nioPath = resource.getNioPath();
                    if(nioPath == null || !Files.exists(nioPath))
                    {
                        errors.reject(ERROR_MSG, input.getDisplayName() + ": Path does not exists on server " + labkeyPath);
                    }
                    else
                    {
                        // Set the input value as the full URL of the webdav resources.  This will be submitted to the Cromwell server.
                        input.setValue(resource.getHref(getViewContext()));
                    }
                }
            }
        }

        private void validateApiKey(User user, BindException errors, CromwellInput input)
        {
            String inputApiKey = input.getValue();
            if(!CromwellInput.apiKeyPattern.matcher(input.getValue()).matches())
            {
                errors.reject(ERROR_MSG, "Invalid API Key");
            }
            else
            {
                // Make sure the key is valid
                User apiKeyUser = ApiKeyManager.get().authenticateFromApiKey(inputApiKey);
                if(apiKeyUser == null)
                {
                    errors.reject(ERROR_MSG, "Cannot authenticate user with API Key.  The key may have expired");
                }
                else if(!apiKeyUser.equals(user))
                {
                    errors.reject(ERROR_MSG, "API key is not valid for current user " + user.getEmail());
                }
                else
                {
                    PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getWritableProperties(getUser(), ContainerManager.getRoot(), PROPS_CROMWELL_USER, true);
                    String savedApiKey = props.get(PROP_USER_APIKEY);
                    // Save the API Key if one was not saved before or is different from the one the user provided
                    if (savedApiKey == null || !inputApiKey.equals(savedApiKey))
                    {
                        props.put(PROP_USER_APIKEY, inputApiKey);
                        props.save();
                    }
                }
            }
        }

        @Override
        public URLHelper getSuccessURL(CromwellJobForm cromwellJobForm)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Create Cromwell Job");
        }
    }

    private static void setInputApiKey(List<CromwellInput> inputList, User user)
    {
        for(CromwellInput input: inputList)
        {
            if(input.isApiKey())
            {
                PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getWritableProperties(user, ContainerManager.getRoot(), PROPS_CROMWELL_USER, false);
                if (props != null && props.get(PROP_USER_APIKEY) != null)
                {
                    String apiKey = props.get(PROP_USER_APIKEY);
                    User apiKeyUser = ApiKeyManager.get().authenticateFromApiKey(apiKey);
                    if(apiKeyUser != null && apiKeyUser.equals(user))
                    {
                        input.setValue(apiKey);
                    }
                }

                break;
            }
        }
    }

    public static class CromwellJobForm extends ReturnUrlForm
    {
        private Workflow _workflow;
        private List<CromwellInput> inputList;
        private Integer _jobId;

        public void setWorkflowId(int workflowId)
        {
           setWorkflow(CromwellManager.get().getWorkflow(workflowId));
        }
        public int getWorkflowId()
        {
            return _workflow!= null ? _workflow.getId() : 0;
        }

        public String getWorkflowName()
        {
            return _workflow!= null ? _workflow.getName() : "NOT FOUND";
        }

        public void setWorkflow(Workflow workflow)
        {
            _workflow = workflow;
        }

        public Workflow getWorkflow()
        {
            return _workflow;
        }

        public Integer getJobId()
        {
            return _jobId;
        }

        public void setJobId(Integer jobId)
        {
            _jobId = jobId;
        }

        public List<CromwellInput> getInputsArray()
        {
            return inputList != null ? inputList : Collections.emptyList();
        }

        public void setInputsArray(List<CromwellInput> inputs)
        {
            inputList = inputs;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public static class CopyCromwellJobAction extends SimpleRedirectAction<CromwellJobForm>
    {

        @Override
        public URLHelper getRedirectURL(CromwellJobForm form)
        {
            return new ActionURL(SubmitCromwellJobAction.class, getContainer()).addParameter("jobId", form.getJobId());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public static class GetFileRootTreeAction extends ReadOnlyApiAction<GetFileRootTreeForm>
    {
        @Override
        public Object execute(GetFileRootTreeForm form, BindException errors) throws Exception
        {
            Path fileRootPath = FileContentService.get().getFileRootPath(getContainer(), FileContentService.ContentType.files);
            org.labkey.api.util.Path labkeyPath = WebdavService.getPath().append(getContainer().getParsedPath()).append(FileContentService.FILES_LINK);
            JSONObject root = form.isFiles() ? getFilesTreeJSON(fileRootPath, labkeyPath) : getFolderTreeJSON(fileRootPath, labkeyPath);
            root.put("expanded", true);

            HttpServletResponse resp = getViewContext().getResponse();
            resp.setContentType("application/json");
            resp.getWriter().write(root.toString());
            return null;
        }

        private JSONObject getFilesTreeJSON(Path rootPath, org.labkey.api.util.Path labkeyPath) throws IOException
        {
            JSONObject root = new JSONObject();
            JSONArray rootChildren = new JSONArray();

            for (Path path : Files.walk(rootPath, 1).sorted((p1, p2) -> {
                if(Files.isDirectory(p1) && Files.isRegularFile(p2)) return -1;
                else if(Files.isRegularFile(p1) && Files.isDirectory(p2)) return 1;
                else return p1.compareTo(p2);
            })
                    .collect(Collectors.toList()))
            {
                if(path.equals(rootPath))
                {
                    continue;
                }
                JSONObject childTree = getFilesTreeJSON(path, labkeyPath.append(path.getFileName().toString()));
                rootChildren.put(childTree);
            }

            root.put("text", rootPath.getFileName());
            root.put("expanded", false);
            if(rootChildren.length() > 0)
            {
                root.put("children", rootChildren);
            }
            else
            {
                if(Files.isDirectory(rootPath))
                {
                    root.put("iconCls", "x4-tree-icon-parent");
                }
                else
                {
                    root.put("isFile", true);
                }
                root.put("leaf", true);
            }

            return root;
        }

        private JSONObject getFolderTreeJSON(Path rootPath, org.labkey.api.util.Path labkeyPath) throws IOException
        {
            JSONObject root = new JSONObject();
            JSONArray rootChildren = new JSONArray();

            for (Path path : Files.walk(rootPath, 1).filter(Files::isDirectory).sorted(Comparator.naturalOrder()).collect(Collectors.toList()))
            {
                if(path.equals(rootPath))
                {
                    continue;
                }
                JSONObject childTree = getFolderTreeJSON(path, labkeyPath.append(path.getFileName().toString()));
                rootChildren.put(childTree);
            }

            root.put("text", rootPath.getFileName());

            // WebdavResource resource = WebdavService.get().lookup(labkeyPath);labkeyPath.toString()
            // root.put("path", resource.getHref(getViewContext()));
            root.put("expanded", false);
            root.put("iconCls", "x4-tree-icon-parent");
            if(rootChildren.length() > 0)
            {
                root.put("children", rootChildren);
            }
            else
            {
                root.put("leaf", true);
            }

            return root;
        }
    }

    public static class GetFileRootTreeForm
    {
        private boolean _files;

        public boolean isFiles()
        {
            return _files;
        }

        public void setFiles(boolean files)
        {
            _files = files;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class CromwellJobDetailsAction extends SimpleViewAction<CromwellJobForm>
    {
        @Override
        public ModelAndView getView(CromwellJobForm form, BindException errors)
        {
            Integer cromwellJobId = form.getJobId();
            CromwellJob job = CromwellManager.get().getCromwellJob(cromwellJobId);
            if(job == null)
            {
                errors.addError(new LabKeyError("Could not find a Cromwell job for Id: " + cromwellJobId));
                return new SimpleErrorView(errors);
            }

            VBox view = new VBox();
            DataRegion dr = getCromwellJobDetailsDataRegion();

            DetailsView detailsView = new DetailsView(dr, job.getId());
            view.addView(detailsView);

            if(job.getPipelineJobId() != null)
            {
                ActionURL pipelineJobUrl = PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlDetails(getContainer(), job.getPipelineJobId());
                String pipelineJobStatus = PipelineService.get().getStatusFile(job.getPipelineJobId()).getStatus();
                view.addView(new HtmlView(new Link.LinkBuilder("View Pipeline Job. Status: " + pipelineJobStatus).href(pipelineJobUrl).build()));
            }

            view.setTitle("Cromwell Job Details");
            view.setFrame(WebPartView.FrameType.PORTAL);
            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Cromwell Job Details");
        }
    }

    @NotNull
    private static DataRegion getCromwellJobDetailsDataRegion()
    {
        DataRegion dr = new DataRegion();
        TableInfo tInfo = CromwellManager.get().getTableInfoJob();
        dr.setColumns(tInfo.getColumns("Id", "Created", "CreatedBy", "Container", "WorkflowId", "CromwellJobId", "CromwellStatus", "PipelineJobId", "Inputs"));

        dr.getDisplayColumn("Inputs").setVisible(false);
        dr.getDisplayColumn("PipelineJobId").setVisible(false);

        DataColumn pipelineJobCol = (DataColumn)dr.getDisplayColumn("PipelineJobId");

        //errCol.setPreserveNewlines(true);

        SimpleDisplayColumn inputsCol = new SimpleDisplayColumn(){

            @Override
            public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String json = ctx.get(FieldKey.fromParts("Inputs"), String.class);
                if(!StringUtils.isBlank(json))
                {
                    JSONObject jsonObj = new JSONObject(json);
                    DOM.PRE(jsonObj.toString(2)).appendTo(out);
                }
                else
                {
                    super.renderDetailsCellContents(ctx, out);
                }
            }
        };
        inputsCol.setCaption("JSON Inputs:");

        // List<DisplayColumn> columns = dr.getDisplayColumns();
        dr.addDisplayColumn(inputsCol);
        return dr;
    }

    @RequiresPermission(AdminPermission.class)
    public class WorkflowDetailsAction extends SimpleViewAction<WorkflowForm>
    {
        @Override
        public ModelAndView getView(WorkflowForm form, BindException errors)
        {
            int workflowId = form.getWorkflowId();
            Workflow workflow = CromwellManager.get().getWorkflow(workflowId);
            if(workflow == null)
            {
                errors.addError(new LabKeyError("Could not find a workflow for Id: " + workflowId));
                return new SimpleErrorView(errors);
            }

            VBox view = new VBox();
            DataRegion dr = getWorkflowDetailsDataRegion();

            DetailsView detailsView = new DetailsView(dr, workflow.getId());
            view.addView(detailsView);

            view.setTitle("Cromwell Workflow Details");
            view.setFrame(WebPartView.FrameType.PORTAL);
            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Cromwell Workflow Details");
        }
    }

    public static class WorkflowForm
    {
        private int _workflowId;

        public void setWorkflowId(int workflowId)
        {
            _workflowId = workflowId;
        }
        public int getWorkflowId()
        {
            return _workflowId;
        }
    }

    @NotNull
    private static DataRegion getWorkflowDetailsDataRegion()
    {
        DataRegion dr = new DataRegion();
        TableInfo tInfo = CromwellManager.get().getTableInfoWorkflow();
        dr.setColumns(tInfo.getColumns("Id", "Created", "CreatedBy", "Name", "Version", "wdl"));

        dr.getDisplayColumn("wdl").setVisible(false);

        SimpleDisplayColumn wdlCol = new SimpleDisplayColumn(){

            @Override
            public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String wdl = ctx.get(FieldKey.fromParts("wdl"), String.class);
                if(!StringUtils.isBlank(wdl))
                {
                    DOM.PRE(wdl).appendTo(out);
                }
                else
                {
                    super.renderDetailsCellContents(ctx, out);
                }
            }
        };
        wdlCol.setCaption("WDL:");
        dr.addDisplayColumn(wdlCol);
        return dr;
    }

    @NotNull
    private static org.labkey.api.util.Path getDecodedPath(String webdavUrl)
    {
        if(!webdavUrl.startsWith(AppProps.getInstance().getBaseServerUrl()))
        {
            return null;
        }
        // Remove the base server url and context path
        webdavUrl = webdavUrl.replace(AppProps.getInstance().getBaseServerUrl() + AppProps.getInstance().getContextPath(), "");
        return org.labkey.api.util.Path.decode(webdavUrl);
    }

    private static org.labkey.api.util.Path getFileRootPath(Container container)
    {
        return WebdavService.getPath().append(container.getParsedPath()).append(FileContentService.FILES_LINK);
    }

    @RequiresPermission(AdminPermission.class)
    public class CromwellJobMetadataAction extends SimpleViewAction<CromwellJobForm>
    {
        @Override
        public ModelAndView getView(CromwellJobForm form, BindException errors)
        {
            Integer cromwellJobId = form.getJobId();
            CromwellJob job = CromwellManager.get().getCromwellJob(cromwellJobId);
            if(job == null)
            {
                errors.addError(new LabKeyError("Could not find a Cromwell job for Id: " + cromwellJobId));
                return new SimpleErrorView(errors);
            }

            if(job.getCromwellJobId() == null)
            {
                errors.addError(new LabKeyError("Could not find a Cromwell job Id"));
                return new SimpleErrorView(errors);
            }

            CromwellUtil.CromwellProperties cromwellProperties;
            try
            {
                cromwellProperties = CromwellUtil.readCromwellProperties();
            }
            catch (CromwellException e)
            {
                errors.addError(new LabKeyError("Error reading Cromwell server properties"));
                return new SimpleErrorView(errors);
            }
            URI metadataUri;
            try
            {
                metadataUri = cromwellProperties.buildMetadataUrl(job.getCromwellJobId());
            }
            catch (URISyntaxException e)
            {
                errors.addError(new LabKeyError("Error building Cromwell metadata URI"));
                return new SimpleErrorView(errors);
            }

            try
            {
                CromwellMetadata metadata = CromwellUtil.getJobMetadata(metadataUri);

                VBox view = new VBox();

                view.setTitle("Cromwell Job Metadata");
                view.setFrame(WebPartView.FrameType.PORTAL);
                return view;
            }
            catch (CromwellException e)
            {
                errors.addError(new LabKeyError("Error getting metadata for Cromwell job " + job.getCromwellJobId() + ". The error was " + e.getMessage()));
                return new SimpleErrorView(errors);
            }
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Cromwell Job Details");
        }
    }
}