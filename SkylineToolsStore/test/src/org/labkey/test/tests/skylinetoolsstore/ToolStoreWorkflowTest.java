/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.tests.skylinetoolsstore;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.External;
import org.labkey.test.categories.MacCossLabModules;
import org.labkey.test.components.skylinetoolsstore.SkylineToolStoreWebPart;
import org.labkey.test.components.skylinetoolsstore.ToolUploadDialog;
import org.labkey.test.pages.skylinetoolsstore.ManageToolOwnersPage;
import org.labkey.test.pages.skylinetoolsstore.SkylineToolDetailsPage;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The real Skyline Tool Store workflow, end to end.
 *
 * Outside authors cannot upload to the store. They attach a zip to a message board post via a wiki
 * page, a site admin reviews it and adds the tool naming the author as an owner, and the author can
 * then maintain their own tool without further admin help.
 */
@Category({External.class, MacCossLabModules.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class ToolStoreWorkflowTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "ToolStoreWorkflowTest";
    private static final String OTHER_STORE = "ToolStoreWorkflowTestOtherStore";
    // Its own store so the tools this test adds cannot disturb the single-tool assertions elsewhere.
    private static final String FORMS_STORE = "ToolStoreWorkflowTestForms";

    // One store per regression test below, for the same reason. Listed so doCleanup can clear them.
    private static final String FAILED_UPLOAD_STORE = "ToolStoreWorkflowTestFailedUpload";
    private static final String NO_EXTENSION_STORE = "ToolStoreWorkflowTestNoExtension";
    private static final String OWNER_PRIVACY_STORE = "ToolStoreWorkflowTestOwnerPrivacy";
    private static final String STALE_DELETE_STORE = "ToolStoreWorkflowTestStaleDelete";
    private static final String OWNER_ESCAPING_STORE = "ToolStoreWorkflowTestOwnerEscaping";
    private static final List<String> REGRESSION_STORES = List.of(FAILED_UPLOAD_STORE,
            NO_EXTENSION_STORE, OWNER_PRIVACY_STORE, STALE_DELETE_STORE, OWNER_ESCAPING_STORE);

    private static final String FORMS_TOOL_NAME = "FormBindingProbe";
    private static final String FORMS_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:formbinding";
    private static File _formsToolV1;
    private static File _formsToolV2;
    private static File _formsToolV3;

    // A site user with no stake in any tool, used to prove an owner cannot hand out Editor.
    private static final String OTHER_USER = "toolstore_bystander@toolstore.test";

    // An ordinary site user. Gets Editor on their tool's folder only after the admin names them.
    private static final String TOOL_AUTHOR = "toolstore_author@toolstore.test";

    private static final String WIKI_NAME = "submit-a-tool";
    private static final String WIKI_TITLE = "Submit a Skyline Tool";
    private static final String SUBMISSION_TITLE = "Skyline Tool submission";

    private static final String TOOL_V1 = "skylinetoolsstore/user1-v1.zip";
    private static final String TOOL_V2 = "skylinetoolsstore/user1-v2.zip";
    private static final String TOOL_OTHER = "skylinetoolsstore/user2-v1.zip";
    private static final String SUPP_FILE = "skylinetoolsstore/test.pdf";

    private final ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    public static void setupProject()
    {
        ToolStoreWorkflowTest init = getCurrentTest();
        init.doSetup();
    }

    @LogMethod
    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, "Collaboration");
        _containerHelper.enableModule("SkylineToolsStore");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Skyline Tool Store");
        portalHelper.addWebPart("Messages");
        portalHelper.addWebPart("Wiki");

        // Public store, and site users may post tool submissions to the message board. This mirrors
        // skyline.ms, where Message Board Contributor is granted to All Site Users.
        _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Reader");
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Message Board Contributor");

        _userHelper.createUser(TOOL_AUTHOR);

        _userHelper.createUser(OTHER_USER);

        _formsToolV1 = writeMinimalToolZip("1.0");
        _formsToolV2 = writeMinimalToolZip("2.0");
        _formsToolV3 = writeMinimalToolZip("3.0");

        ToolStoreTestHelper.removeToolsFromCatalog(PROJECT_NAME,
                TestFileUtils.getSampleData(TOOL_V1), TestFileUtils.getSampleData(TOOL_OTHER),
                _formsToolV1);

        // wikiVisualBody=false so the HTML goes in through the source tab. The visual editor is not
        // interactable for raw markup.
        new WikiHelper(this).createWikiPage(WIKI_NAME, "HTML", WIKI_TITLE,
                submissionFormHtml(), false, null, false);
    }

    /**
     * The submission form as it appears on skyline.ms - it posts to the announcements controller in
     * this folder, not to the tool store.
     */
    private String submissionFormHtml()
    {
        String action = WebTestHelper.buildRelativeUrl("announcements", PROJECT_NAME, "insert");
        return "<div id=\"requestUser\">\n" +
               "<form id=\"submit-tool-form\" action=\"" + action + "\" enctype=\"multipart/form-data\" method=\"post\">\n" +
               "<input type=\"hidden\" name=\"X-LABKEY-CSRF\" value=\"\" id=\"submit-tool-form-labkey-csrf\" />\n" +
               "<input value=\"" + SUBMISSION_TITLE + "\" name=\"title\" type=\"hidden\">\n" +
               "<input value=\"Hi! Here is my Skyline Tool.\" name=\"body\" type=\"hidden\">\n" +
               "<input value=\"RADEOX\" name=\"rendererType\" type=\"hidden\">\n" +
               "<input name=\"formFiles[00]\" type=\"file\"><br />\n" +
               "<input type=\"submit\" value=\"Submit\" name=\"Submit\">\n" +
               "</form>\n</div>\n" +
               "<script type=\"text/javascript\">\n" +
               "document.getElementById(\"submit-tool-form-labkey-csrf\").value = LABKEY.CSRF;\n" +
               "</script>";
    }

    /**
     * Submission through to self-service maintenance, in the order it happens in production.
     *
     * One method rather than several because each stage depends on the last, and JUnit does not
     * order test methods.
     */
    @Test
    public void testSubmitReviewAndMaintainATool()
    {
        log("The wiki page offers the submission form to a logged-in user");
        goToProjectHome(PROJECT_NAME);
        clickAndWait(Locator.linkWithText(WIKI_TITLE));
        assertElementPresent(Locator.id("submit-tool-form"));

        log("An ordinary site user submits a tool zip to the message board");
        impersonate(TOOL_AUTHOR);
        try
        {
            int status = submitToolToMessageBoard(TOOL_V1);
            assertTrue("Tool submission should be accepted, got HTTP " + status, status < 400);
        }
        finally
        {
            stopImpersonating();
        }

        log("The submission is visible on the message board with its attachment");
        goToProjectHome(PROJECT_NAME);
        assertTextPresent(SUBMISSION_TITLE);
        clickAndWait(Locator.linkWithText(SUBMISSION_TITLE));
        assertTextPresent("user1-v1.zip");

        // Two claims here, and the UI can only make the first. That the button is absent says the
        // store does not offer the author the option; it says nothing about what happens if they
        // post anyway. The hand-built post is what proves the action refuses, so it stays.
        log("The author cannot add the tool to the store themselves");
        Set<String> beforeAdminAdd = catalogIdentifiers();
        impersonate(TOOL_AUTHOR);
        try
        {
            goToProjectHome(PROJECT_NAME);
            assertFalse("A submitter must not be offered Add New Tool",
                    new SkylineToolStoreWebPart(getDriver()).canAddTool());
            uploadTool(TOOL_V1, null);
        }
        finally
        {
            stopImpersonating();
        }
        assertEquals("A submitter must not be able to add a tool to the store",
                beforeAdminAdd, catalogIdentifiers());

        log("A site admin adds the tool and names the author as an owner");
        goToProjectHome(PROJECT_NAME);
        SkylineToolStoreWebPart store = new SkylineToolStoreWebPart(getDriver());
        assertTrue("A site admin should be offered Add New Tool", store.canAddTool());
        store.addTool(TestFileUtils.getSampleData(TOOL_V1), TOOL_AUTHOR);

        JSONObject tool = onlyToolInThisStore();
        String identifier = tool.getString("Identifier");
        String toolName = tool.getString("Name");
        String v1Folder = toolFolderPath(tool);
        String v1Version = tool.getString("Version");
        assertTrue("The author should own their tool's folder", hasEditorRole(v1Folder, TOOL_AUTHOR));

        log("The store lists the tool");
        goToProjectHome(PROJECT_NAME);
        store = new SkylineToolStoreWebPart(getDriver());
        assertTrue("The store should list " + toolName, store.hasTool(toolName));
        assertEquals("The listed version should be the one that was added",
                v1Version, store.getTool(toolName).getVersion());

        log("The author can attach a supplementary file to their own tool");
        impersonate(TOOL_AUTHOR);
        try
        {
            goToProjectHome(PROJECT_NAME);
            SkylineToolDetailsPage details = new SkylineToolStoreWebPart(getDriver())
                    .getTool(toolName).clickToolName();
            assertTrue("The owner should get the tool's settings menu", details.hasSettingsMenu());
            details = details.uploadSupplementaryFile(TestFileUtils.getSampleData(SUPP_FILE));
            assertTrue("The supplementary file should be listed on the details page",
                    details.getSupplementaryFileNames().contains("test.pdf"));
        }
        finally
        {
            stopImpersonating();
        }

        // The supplementary file is attached BEFORE the new version is published on purpose. A new
        // version copies the previous version's supplementary files into its own folder, and that
        // copy loop only runs when the previous version has some.
        log("The author publishes a new version without admin help");
        impersonate(TOOL_AUTHOR);
        try
        {
            goToProjectHome(PROJECT_NAME);
            SkylineToolDetailsPage details = new SkylineToolStoreWebPart(getDriver())
                    .getTool(toolName).clickToolName();
            details = details.uploadNewVersion(TestFileUtils.getSampleData(TOOL_V2));
            assertNotEquals("The details page should be showing the version just published",
                    v1Version, details.getVersion());
        }
        finally
        {
            stopImpersonating();
        }

        JSONObject latest = onlyToolInThisStore();
        assertEquals("Still the same tool", identifier, latest.getString("Identifier"));
        assertNotEquals("The author's upload should have become the latest version",
                v1Version, latest.getString("Version"));

        log("Ownership carries forward to the new version's folder");
        String v2Folder = toolFolderPath(latest);
        int v2RowId = rowId(latest);
        assertNotEquals(v1Folder, v2Folder);
        assertTrue("The author should own the new version too", hasEditorRole(v2Folder, TOOL_AUTHOR));

        log("The supplementary file carries forward to the new version");
        goToProjectHome(PROJECT_NAME);
        SkylineToolDetailsPage latestDetails = new SkylineToolStoreWebPart(getDriver())
                .getTool(toolName).clickToolName();
        assertEquals("The details page should be showing the new version",
                latest.getString("Version"), latestDetails.getVersion());
        assertTrue("The supplementary file should have carried forward to the new version",
                latestDetails.getSupplementaryFileNames().contains("test.pdf"));

        // Hand-built posts on purpose, as above. The point is what the actions do with a request the
        // UI would never send, so going through the UI would prove nothing here.
        log("Owning one tool does not let the author add another, or reassign ownership");
        Set<String> beforeAuthorAttempts = catalogIdentifiers();
        impersonate(TOOL_AUTHOR);
        try
        {
            uploadTool(TOOL_OTHER, null);
            setOwners(v2RowId, PasswordUtilUsername());
        }
        finally
        {
            stopImpersonating();
        }
        assertEquals("A tool owner must not be able to add a different tool",
                beforeAuthorAttempts, catalogIdentifiers());
        assertTrue("Ownership must be unchanged", hasEditorRole(v2Folder, TOOL_AUTHOR));
    }

    /**
     * One store folder must not list another's tools. The JSON API stays global on purpose, because
     * Skyline asks a container that is not the store folder and only finds tools because of it.
     */
    @Test
    public void testListingIsScopedToThisStoreButApiIsNot()
    {
        _containerHelper.createProject(OTHER_STORE, "Collaboration");
        _containerHelper.enableModule(OTHER_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

        goToProjectHome(OTHER_STORE);
        new SkylineToolStoreWebPart(getDriver()).addTool(TestFileUtils.getSampleData(TOOL_OTHER), null);

        JSONObject otherTool = onlyToolInStore(OTHER_STORE);
        String otherName = otherTool.getString("Name");

        goToProjectHome(OTHER_STORE);
        assertTrue("The store it was added to should list it",
                new SkylineToolStoreWebPart(getDriver()).hasTool(otherName));

        goToProjectHome(PROJECT_NAME);
        assertFalse("One store must not list another store's tool",
                new SkylineToolStoreWebPart(getDriver()).hasTool(otherName));

        // The catalog is global, so the other store's tool is still there for Skyline.
        assertTrue("getToolsApi must keep returning tools from every container",
                catalogIdentifiers().contains(otherTool.getString("Identifier")));
    }

    /**
     * Drives the store's own dialogs instead of building the POST by hand.
     *
     * The other tests name every parameter themselves, so a field renamed in a JSP but not in its
     * form bean - or the reverse - cannot fail them. Submitting the real forms is the only way that
     * drift shows up, since a name the bean does not bind either silently stays at its default or
     * fails to convert.
     */
    @Test
    public void testStoreDialogsPostWhatTheActionsBind()
    {
        _containerHelper.createProject(FORMS_STORE, "Collaboration");
        _containerHelper.enableModule(FORMS_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

        log("Add a tool through the web part's Add New Tool dialog");
        goToProjectHome(FORMS_STORE);
        SkylineToolStoreWebPart store = new SkylineToolStoreWebPart(getDriver());
        ToolUploadDialog upload = store.clickAddNewTool();
        assertTrue("Adding a new tool should offer the owners field", upload.hasOwnersField());
        SkylineToolDetailsPage added = upload.setToolZip(_formsToolV1).clickUpload();
        assertEquals("Adding a tool should land on its details page", FORMS_TOOL_NAME, added.getToolName());

        goToProjectHome(FORMS_STORE);
        store = new SkylineToolStoreWebPart(getDriver());
        assertTrue("The dialog should have added the tool", store.hasTool(FORMS_TOOL_NAME));
        assertEquals("The dialog should have added exactly one tool", 1, store.getToolCount());

        log("Publish a new version through the details page dialog");
        SkylineToolDetailsPage details = store.getTool(FORMS_TOOL_NAME).clickToolName();
        // A new version inherits its owners, so this dialog must not offer to set them.
        ToolUploadDialog newVersion = details.clickUploadNewVersion();
        assertFalse("Publishing a version must not offer the owners field", newVersion.hasOwnersField());
        details = newVersion.setToolZip(_formsToolV2).clickUpload();
        assertEquals("The details page should show the version just published", "2.0", details.getVersion());

        goToProjectHome(FORMS_STORE);
        store = new SkylineToolStoreWebPart(getDriver());
        assertEquals("Publishing a version must not add a second tool", 1, store.getToolCount());
        assertEquals("The listing should show the new version", "2.0",
                store.getTool(FORMS_TOOL_NAME).getVersion());

        log("Delete the newest version through the store's own dialog");
        store.getTool(FORMS_TOOL_NAME).clickDeleteLatestVersion().confirm();

        goToProjectHome(FORMS_STORE);
        store = new SkylineToolStoreWebPart(getDriver());
        assertEquals("Deleting the newest version should leave 1.0 as the latest",
                "1.0", store.getTool(FORMS_TOOL_NAME).getVersion());
        JSONObject afterDelete = onlyToolInStore(FORMS_STORE);
        assertEquals("The catalog should agree with the listing",
                "1.0", afterDelete.getString("Version"));

        // UpdateToolAction shares its form with InsertToolAction, so toolOwners binds on this path
        // too, and an owner who could name owners while publishing could hand Editor on their folder
        // to anyone. Two things stop that: the action passes an empty owner list, and
        // copyContainerPermissions then replaces the new folder's policy with the previous version's.
        // The second is the one that actually holds - removing only the first still leaves the policy
        // overwritten. This asserts the outcome, so it survives either being reworked.
        log("Publishing a version cannot grant ownership");
        int status = uploadToolTo(toolFolderPath(FORMS_STORE, afterDelete), _formsToolV3,
                rowId(afterDelete), OTHER_USER);
        assertTrue("Publishing 3.0 should be accepted, got HTTP " + status, status < 400);

        JSONObject afterGrantAttempt = onlyToolInStore(FORMS_STORE);
        assertEquals("3.0", afterGrantAttempt.getString("Version"));
        assertFalse("Naming an owner while publishing must not grant Editor",
                hasEditorRole(toolFolderPath(FORMS_STORE, afterGrantAttempt), OTHER_USER));
    }

    // -------------------------------------------------------------------------
    // Regression tests for the defects the code review found
    // -------------------------------------------------------------------------

    /**
     * A version that cannot be stored must not take the tool out of the catalog.
     *
     * UpdateToolAction used to demote the previous version before the new one existed, so a failure
     * in between left no row marked latest and the tool disappeared from the store and from the
     * catalog Skyline clients read, with no way for the owner to get it back.
     *
     * A folder already using the new version's name is the reachable way to make storing fail -
     * makeContainer refuses a name that is taken, which is what a half-finished upload leaves behind.
     */
    @Test
    public void testAFailedVersionUploadLeavesTheToolInTheCatalog()
    {
        String store = FAILED_UPLOAD_STORE;
        String tool = "FailedUploadProbe";
        File v1 = writeMinimalToolZip(tool, "URN:LSID:toolstore.test:failedupload", "1.0");
        File v2 = writeMinimalToolZip(tool, "URN:LSID:toolstore.test:failedupload", "2.0");
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(v1, null);

        log("Take the folder name that publishing 2.0 would need");
        _containerHelper.createSubfolder(store, "_tool_" + tool + "_2.0");

        log("Publishing 2.0 is refused");
        goToProjectHome(store);
        SkylineToolDetailsPage details = new SkylineToolStoreWebPart(getDriver())
                .getTool(tool).clickToolName();
        String error = details.clickUploadNewVersion().setToolZip(v2).clickUploadExpectingError();
        assertTrue("The refusal should name the folder that is in the way, got: " + error,
                error.contains("_tool_" + tool + "_2.0"));

        log("The tool is still listed, still at 1.0");
        goToProjectHome(store);
        SkylineToolStoreWebPart webPart = new SkylineToolStoreWebPart(getDriver());
        assertTrue("A refused upload must not remove the tool from the store", webPart.hasTool(tool));
        assertEquals("The previous version must still be the latest one",
                "1.0", webPart.getTool(tool).getVersion());

        assertEquals("A refused upload must leave the catalog Skyline reads alone",
                "1.0", onlyToolInStore(store).getString("Version"));
    }

    /**
     * A supplementary file whose name has no extension used to throw out of the icon lookup, which
     * took out the whole store listing for every visitor rather than just that tool's page.
     */
    @Test
    public void testASupplementaryFileWithNoExtensionDoesNotBreakTheStore()
    {
        String store = NO_EXTENSION_STORE;
        String tool = "NoExtensionProbe";
        createStore(store);

        goToProjectHome(store);
        SkylineToolDetailsPage details = new SkylineToolStoreWebPart(getDriver()).addTool(
                writeMinimalToolZip(tool, "URN:LSID:toolstore.test:noextension", "1.0"), null);

        details = details.uploadSupplementaryFile(writeFileNamed("README"));
        assertTrue("The details page should list the file",
                details.getSupplementaryFileNames().contains("README"));

        log("The store listing still renders, which is what used to break");
        goToProjectHome(store);
        assertTrue("The listing must still show the tool",
                new SkylineToolStoreWebPart(getDriver()).hasTool(tool));
    }

    /**
     * Gating the owners form was not enough on its own - the addresses were still written into the
     * page for whoever loaded it, so anyone who viewed source could read them.
     */
    @Test
    public void testOwnerAddressesAreNotInThePageForOtherUsers()
    {
        String store = OWNER_PRIVACY_STORE;
        String tool = "OwnerPrivacyProbe";
        createStore(store);
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Reader");

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(
                writeMinimalToolZip(tool, "URN:LSID:toolstore.test:ownerprivacy", "1.0"), TOOL_AUTHOR);

        impersonate(OTHER_USER);
        try
        {
            goToProjectHome(store);
            new SkylineToolStoreWebPart(getDriver()).getTool(tool).clickToolName();
            assertFalse("SECURITY: a reader can read the tool's owners out of the page source",
                    getDriver().getPageSource().contains(TOOL_AUTHOR));
        }
        finally
        {
            stopImpersonating();
        }
    }

    /**
     * A delete the server refuses used to look like one that worked.
     *
     * The actions render a refusal as an error view with status 200, so the browser's .fail() never
     * runs. The handler took that for success, closed the dialog and removed the row, telling the
     * admin the tool was gone when it was the server saying no.
     */
    @Test
    public void testDeletingAToolThatIsAlreadyGoneReportsTheRefusal()
    {
        String store = STALE_DELETE_STORE;
        String tool = "StaleDeleteProbe";
        File zip = writeMinimalToolZip(tool, "URN:LSID:toolstore.test:staledelete", "1.0");
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(zip, null);

        goToProjectHome(store);
        SkylineToolStoreWebPart webPart = new SkylineToolStoreWebPart(getDriver());

        // Someone else deletes it while this page sits there, which is the state the handler got wrong.
        ToolStoreTestHelper.removeToolsFromCatalog(store, zip);

        String message = webPart.getTool(tool).clickDelete().confirmExpectingRefusal();
        assertTrue("The dialog should report what the server said, got: " + message,
                message.contains("does not exist"));
    }

    /**
     * The owners box is prefilled from a script, so the value has to be escaped for JavaScript.
     * Escaping it as HTML put the entities themselves in the box, and an admin correcting one bad
     * address had to retype the whole list.
     */
    @Test
    public void testARefusedOwnerListComesBackUnchanged()
    {
        String store = OWNER_ESCAPING_STORE;
        String tool = "OwnerEscapingProbe";
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(
                writeMinimalToolZip(tool, "URN:LSID:toolstore.test:ownerescaping", "1.0"), null);

        goToProjectHome(store);
        String submitted = "a&b@toolstore.test";
        ManageToolOwnersPage reshow = new SkylineToolStoreWebPart(getDriver())
                .getTool(tool).clickManageToolOwners()
                .setOwners(submitted)
                .clickUpdateExpectingError();

        assertTrue("An unknown address should be reported, got: " + reshow.getError(),
                reshow.getError().contains("unknown"));
        assertEquals("The address must come back exactly as it was typed",
                submitted, reshow.getOwners());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** A store folder of its own, so one test's tools cannot disturb another's counts. */
    private void createStore(String projectName)
    {
        _containerHelper.createProject(projectName, "Collaboration");
        _containerHelper.enableModule(projectName, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");
    }

    /**
     * A small file with an exact name. createTempFile always appends a suffix, and the name is the
     * whole point when the extension is what is being tested, so this puts the file in a directory
     * of its own instead.
     */
    private static File writeFileNamed(String name)
    {
        try
        {
            Path dir = Files.createTempDirectory("toolstore-supp");
            dir.toFile().deleteOnExit();
            Path file = dir.resolve(name);
            Files.writeString(file, "supplementary file for the tool store tests");
            file.toFile().deleteOnExit();
            return file.toFile();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not write a file named " + name, e);
        }
    }

    /**
     * A tool zip holding nothing but tool-inf/info.properties. Name, Version and Identifier are the
     * only required properties, and the sample zips in this module are tens of megabytes, so this
     * test builds its own rather than adding more of those to the repository.
     */
    private static File writeMinimalToolZip(String version)
    {
        return writeMinimalToolZip(FORMS_TOOL_NAME, FORMS_TOOL_IDENTIFIER, version);
    }

    /**
     * Identifiers are unique across the whole server, not per folder, so a test that adds its own
     * tool needs its own name and identifier or it collides with every other store on the server.
     */
    private static File writeMinimalToolZip(String name, String identifier, String version)
    {
        try
        {
            // ZipName is a 50 character column and createTempFile appends up to 19 random digits, so
            // the prefix has to stay short. The tool's name comes from info.properties, not from here.
            File zip = File.createTempFile("ts-" + version + "-", ".zip");
            zip.deleteOnExit();
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip)))
            {
                out.putNextEntry(new ZipEntry("tool-inf/info.properties"));
                out.write(("Name = " + name + "\n" +
                           "Version = " + version + "\n" +
                           "Identifier = " + identifier + "\n").getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            return zip;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not build the test tool zip", e);
        }
    }

    /** Posts a tool zip to the folder's message board, the way the wiki form does. */
    private int submitToolToMessageBoard(String sampleDataRelativePath)
    {
        File zip = TestFileUtils.getSampleData(sampleDataRelativePath);
        HttpPost request = new HttpPost(WebTestHelper.buildURL("announcements", PROJECT_NAME, "insert"));
        request.setEntity(MultipartEntityBuilder.create()
                .addTextBody("title", SUBMISSION_TITLE)
                .addTextBody("body", "Hi! Here is my Skyline Tool.")
                .addTextBody("rendererType", "RADEOX")
                .addBinaryBody("formFiles[00]", zip, ContentType.create("application/zip"), zip.getName())
                .build());
        return execute(request);
    }

    /** Adds a brand-new tool to this store folder. */
    private int uploadTool(String sampleDataRelativePath, String toolOwners)
    {
        return uploadToolTo(PROJECT_NAME, sampleDataRelativePath, -1, toolOwners);
    }

    /**
     * @param containerPath  the store folder for a new tool, or the TOOL's own folder for a new
     *                       version - the two actions are addressed to different containers
     * @param updateTarget   row id of the tool being updated, or -1 for a brand-new tool
     */
    @LogMethod
    private int uploadToolTo(String containerPath, String sampleDataRelativePath, int updateTarget,
                             String toolOwners)
    {
        return uploadToolTo(containerPath, TestFileUtils.getSampleData(sampleDataRelativePath),
                updateTarget, toolOwners);
    }

    private int uploadToolTo(String containerPath, File zip, int updateTarget, String toolOwners)
    {
        boolean newVersion = updateTarget >= 0;
        HttpPost request = new HttpPost(
                WebTestHelper.buildURL("skyts", containerPath, newVersion ? "updateTool" : "insertTool"));
        MultipartEntityBuilder entity = MultipartEntityBuilder.create()
                .addBinaryBody("toolZip", zip, ContentType.create("application/zip"), zip.getName());
        if (newVersion)
            entity.addTextBody("toolId", String.valueOf(updateTarget));
        if (toolOwners != null)
            entity.addTextBody("toolOwners", toolOwners);
        request.setEntity(entity.build());
        return execute(request);
    }

    private int setOwners(int toolRowId, String owner)
    {
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", PROJECT_NAME, "setOwners"));
        request.setEntity(MultipartEntityBuilder.create()
                .addTextBody("toolId", String.valueOf(toolRowId))
                .addTextBody("toolOwners", owner)
                .build());
        return execute(request);
    }

    /** Sends as the current (possibly impersonated) user, with a valid CSRF token. */
    private int execute(HttpPost request)
    {
        APITestHelper.injectCookies(request);
        try (CloseableHttpClient client = WebTestHelper.getHttpClient())
        {
            return client.execute(request, response -> {
                EntityUtils.consumeQuietly(response.getEntity());
                return response.getCode();
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException("Request failed: " + request.getRequestUri(), e);
        }
    }

    private JSONArray toolsFromApi()
    {
        return ToolStoreTestHelper.toolsFromApi(PROJECT_NAME);
    }

    private Set<String> catalogIdentifiers()
    {
        return ToolStoreTestHelper.catalogIdentifiers(PROJECT_NAME);
    }

    private JSONObject onlyToolInThisStore()
    {
        return onlyToolInStore(PROJECT_NAME);
    }

    /** Every tool whose folder sits under the given store, read from the global catalog. */
    private JSONArray toolsInStoreJson(String containerPath)
    {
        JSONArray tools = toolsFromApi();
        JSONArray inStore = new JSONArray();
        for (int i = 0; i < tools.length(); i++)
        {
            JSONObject tool = tools.getJSONObject(i);
            if (tool.optString("IconUrl").contains("/" + containerPath + "/") ||
                tool.optString("DownloadUrl").contains("/" + containerPath + "/"))
                inStore.put(tool);
        }
        return inStore;
    }

    /** The single tool whose folder sits under the given store, read from the global catalog. */
    private JSONObject onlyToolInStore(String containerPath)
    {
        JSONArray tools = toolsFromApi();
        JSONObject found = null;
        for (int i = 0; i < tools.length(); i++)
        {
            JSONObject tool = tools.getJSONObject(i);
            if (tool.optString("IconUrl").contains("/" + containerPath + "/") ||
                tool.optString("DownloadUrl").contains("/" + containerPath + "/"))
            {
                assertTrue("Expected one tool in " + containerPath, found == null);
                found = tool;
            }
        }
        assertTrue("No tool found in " + containerPath, found != null);
        return found;
    }

    private int rowId(JSONObject tool)
    {
        return ToolStoreTestHelper.rowId(tool);
    }

    private String toolFolderPath(JSONObject tool)
    {
        return toolFolderPath(PROJECT_NAME, tool);
    }

    /** A tool's version folder sits directly under the store that holds it. */
    private String toolFolderPath(String storePath, JSONObject tool)
    {
        return "/" + storePath + "/_tool_" + tool.getString("Name") + "_" + tool.getString("Version");
    }

    private boolean hasEditorRole(String containerPath, String user)
    {
        return _permissionsHelper.getUserRoles(containerPath, user).stream()
                .anyMatch(role -> role.endsWith("EditorRole"));
    }

    private String PasswordUtilUsername()
    {
        return org.labkey.test.util.PasswordUtil.getUsername();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
        _containerHelper.deleteProject(OTHER_STORE, false);
        _containerHelper.deleteProject(FORMS_STORE, false);
        // A store of its own per regression test, so each has to be cleared here too. A run that
        // fails leaves its projects behind, and createProject then throws on the next run.
        for (String store : REGRESSION_STORES)
            _containerHelper.deleteProject(store, false);
        _userHelper.deleteUsers(false, TOOL_AUTHOR);
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("SkylineToolsStore");
    }
}
