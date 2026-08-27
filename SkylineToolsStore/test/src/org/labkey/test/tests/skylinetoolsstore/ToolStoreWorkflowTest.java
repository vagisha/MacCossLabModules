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
import org.labkey.test.components.skylinetoolsstore.ConfirmDeleteDialog;
import org.labkey.test.components.skylinetoolsstore.ManageToolOwnersDialog;
import org.labkey.test.components.skylinetoolsstore.SkylineToolStoreWebPart;
import org.labkey.test.components.skylinetoolsstore.SupplementaryFileDialog;
import org.labkey.test.components.skylinetoolsstore.ToolRow;
import org.labkey.test.pages.skylinetoolsstore.ManageToolOwnersPage;
import org.labkey.test.pages.skylinetoolsstore.SkylineToolDetailsPage;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The real Skyline Tool Store workflow, end to end.
 *
 * Outside authors cannot upload to the store. They attach a zip to a message board post via a wiki
 * page. A site admin adds the tool and names the author as an owner. The author can then maintain
 * their own tool without further admin help.
 */
@Category({External.class, MacCossLabModules.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class ToolStoreWorkflowTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "ToolStoreWorkflowTest";
    private static final String OTHER_STORE = "ToolStoreWorkflowTestOtherStore";
    // Its own store. The tools this test adds cannot then disturb the single-tool assertions
    // elsewhere.
    private static final String FORMS_STORE = "ToolStoreWorkflowTestForms";
    // Deliberately does NOT contain the library's name. The folder name is part of every url on its
    // pages, including the favicon, so a folder named after the library defeats any url check.
    private static final String NO_JQUERY_UI_STORE = "ToolStoreWorkflowTestNoUiLib";
    // A store folder per test, so one test's tools cannot disturb another's counts.
    private static final String NO_EXTENSION_STORE = "ToolStoreWorkflowTestNoExtension";
    private static final String STALE_DELETE_STORE = "ToolStoreWorkflowTestStaleDelete";
    private static final String OWNER_ESCAPING_STORE = "ToolStoreWorkflowTestOwnerEscaping";
    private static final String ESCAPE_STORE = "ToolStoreWorkflowTestEscapeKey";
    private static final String TAB_STORE = "ToolStoreWorkflowTestTabKey";
    private static final String PROPERTIES_STORE = "ToolStoreWorkflowTestProperties";
    private static final String SHIFT_TAB_STORE = "ToolStoreWorkflowTestShiftTab";
    private static final String REOPEN_STORE = "ToolStoreWorkflowTestReopenAfterRefusal";
    private static final String PARTIAL_PERMS_STORE = "ToolStoreWorkflowTestPartialPerms";

    private static final String FORMS_TOOL_NAME = "FormBindingProbe";
    private static final String FORMS_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:formbinding";
    // A second tool in the same store. One dialog serves every row, so a single row cannot show
    // whether the handler addressed it to the right one.
    private static final String FORMS_SECOND_TOOL_NAME = "FormBindingProbeTwo";
    private static final String FORMS_SECOND_TOOL_IDENTIFIER =
            "URN:LSID:toolstore.test:formbindingtwo";
    private static File _formsToolV1;
    private static File _formsToolV2;

    // Its own store. This test deliberately fails an upload and then retries the same version, which
    // would disturb the single-tool assertions in the other stores.
    private static final String RETRY_STORE = "ToolStoreWorkflowTestFailedUpload";
    private static final String RETRY_TOOL_NAME = "PartialUploadProbe";
    private static final String RETRY_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:partialupload";
    private static final String RETRY_TOOL_VERSION = "1.0";

    // Its own store, because it deletes everything it adds and the other stores assert on one tool.
    private static final String WEBPART_STORE = "ToolStoreWorkflowTestWebPartDeletes";
    private static final String WEBPART_TOOL_NAME = "WebPartDeleteProbe";
    private static final String WEBPART_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:webpartdelete";
    // Never deleted. Both menu items act on the row they were opened from, which one tool in the
    // store cannot show.
    private static final String WEBPART_OTHER_TOOL_NAME = "WebPartDeleteBystander";
    private static final String WEBPART_OTHER_TOOL_IDENTIFIER =
            "URN:LSID:toolstore.test:webpartbystander";


    // Its own store, because it leaves behind a version folder that cannot be deleted.
    private static final String BLOCKED_STORE = "ToolStoreWorkflowTestBlockedDelete";
    private static final String BLOCKED_TOOL_NAME = "BlockedDeleteProbe";
    private static final String BLOCKED_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:blockeddelete";

    private static final String OLDER_STORE = "ToolStoreWorkflowTestOlderVersion";
    private static final String OLDER_TOOL_NAME = "OlderVersionProbe";
    private static final String OLDER_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:olderversion";

    // Its own store. It enables the module in a tool folder, which the other stores do not.
    private static final String FOLDER_STORE = "ToolStoreWorkflowTestToolFolder";
    private static final String FOLDER_TOOL_NAME = "ToolFolderProbe";
    private static final String FOLDER_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:toolfolder";
    // Its own store, because it too enables the module in a tool folder.
    private static final String NESTED_STORE = "ToolStoreWorkflowTestNestedInsert";
    private static final String NESTED_HOST_TOOL_NAME = "NestedHostProbe";
    private static final String NESTED_HOST_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:nestedhost";
    // Never expected to exist. The upload that would create it is posted at a tool's own folder.
    private static final String NESTED_TOOL_NAME = "NestedToolProbe";
    private static final String NESTED_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:nested";

    // Its own store, because the upload it makes is meant to be refused before a tool is created.
    private static final String CORRUPT_STORE = "ToolStoreWorkflowTestCorruptZip";
    private static final String CORRUPT_TOOL_NAME = "CorruptZipProbe";
    private static final String CORRUPT_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:corruptzip";

    // An ordinary site user. Gets Editor on their tool's folder only after the admin names them.
    private static final String TOOL_AUTHOR = "toolstore_author@toolstore.test";

    // A second owner, used to check that an owner change reaches every version's folder.
    private static final String TOOL_SECOND_OWNER = "toolstore_second@toolstore.test";

    // Neither an owner nor an admin. Used where a test needs a second account that holds nothing.
    private static final String OTHER_USER = "toolstore_bystander@toolstore.test";

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

        // Public store. Site users may post tool submissions to the message board. This mirrors
        // skyline.ms, where Message Board Contributor is granted to All Site Users.
        _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Reader");
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Message Board Contributor");

        _userHelper.createUser(TOOL_AUTHOR);
        _userHelper.createUser(TOOL_SECOND_OWNER);
        _userHelper.createUser(OTHER_USER);

        _formsToolV1 = writeMinimalToolZip("1.0");
        _formsToolV2 = writeMinimalToolZip("2.0");

        ToolStoreTestHelper.removeToolsFromCatalog(PROJECT_NAME,
                TestFileUtils.getSampleData(TOOL_V1), TestFileUtils.getSampleData(TOOL_OTHER),
                _formsToolV1);

        // Edit in the source tab rather than visual
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
     * Submission through to self-service maintenance, the full workflow in the order it happens in production.
     */
    @Test
    public void testSubmitPublishAndMaintainATool()
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

        log("The author cannot add the tool to the store themselves");
        Set<String> beforeToolAdd = catalogIdentifiers();
        impersonate(TOOL_AUTHOR);
        try
        {
            int status = uploadTool(TOOL_V1, null);
            assertTrue("A submitter must not be able to add a tool, got HTTP " + status, status >= 400);
        }
        finally
        {
            stopImpersonating();
        }
        assertEquals("A submitter must not be able to add a tool to the store",
                beforeToolAdd, catalogIdentifiers());

        log("A site admin adds the tool and names the author as an owner");
        uploadTool(TOOL_V1, TOOL_AUTHOR);
        JSONObject tool = onlyToolInThisStore();
        String identifier = tool.getString("Identifier");
        String v1Folder = toolFolderPath(tool);
        assertTrue("The author should own their tool's folder", hasEditorRole(v1Folder, TOOL_AUTHOR));

        log("The store lists the tool");
        goToProjectHome(PROJECT_NAME);
        assertTextPresent(tool.getString("Name"));

        log("The author can attach a supplementary file to their own tool");
        int v1RowId = rowId(tool);
        String v1Version = tool.getString("Version");
        impersonate(TOOL_AUTHOR);
        try
        {
            int status = uploadSupplementaryFile(v1Folder, v1RowId);
            assertTrue("Supplementary upload should be accepted, got HTTP " + status, status < 400);
        }
        finally
        {
            stopImpersonating();
        }
        goToProjectHome(PROJECT_NAME);
        assertTextPresent("test.pdf");

        // Attaching the supplementary file before publishing is deliberate. A new version copies the
        // previous version's supplementary files. That loop only runs when there are some.
        log("The author publishes a new version without admin help");
        impersonate(TOOL_AUTHOR);
        try
        {
            uploadNewVersion(v1Folder, TOOL_V2, v1RowId);
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
        assertTextPresent("test.pdf");

        log("The trash icon on the details page deletes a supplementary file");
        SkylineToolDetailsPage detailsPage = new SkylineToolStoreWebPart(getDriver())
                .getTool(latest.getString("Name")).clickToolName();
        assertTrue("The details page should list the supplementary file",
                detailsPage.getSupplementaryFileNames().contains("test.pdf"));
        detailsPage.deleteSupplementaryFile("test.pdf");

        // deleteSupplementaryFile waits for the script to remove the row. Reload so the assertion
        // reads the list back from the server rather than from the page the script edited.
        refresh();
        assertFalse("The supplementary file should be gone after a reload",
                new SkylineToolDetailsPage(getDriver()).getSupplementaryFileNames()
                        .contains("test.pdf"));
        assertTextNotPresent("test.pdf");

        log("Owning one tool does not let the author add another, or reassign ownership");
        Set<String> beforeAuthorAttempts = catalogIdentifiers();
        impersonate(TOOL_AUTHOR);
        try
        {
            int uploadStatus = uploadTool(TOOL_OTHER, null);
            assertTrue("Adding a different tool must be refused, got HTTP " + uploadStatus,
                    uploadStatus >= 400);
            int ownersStatus = setOwners(v2RowId, TOOL_SECOND_OWNER);
            assertTrue("Reassigning ownership must be refused, got HTTP " + ownersStatus,
                    ownersStatus >= 400);
        }
        finally
        {
            stopImpersonating();
        }
        assertEquals("A tool owner must not be able to add a different tool",
                beforeAuthorAttempts, catalogIdentifiers());
        assertTrue("Ownership must be unchanged", hasEditorRole(v2Folder, TOOL_AUTHOR));

        log("Setting owners reaches every version's folder, not just the one named");
        setOwners(v2RowId, TOOL_AUTHOR + ", " + TOOL_SECOND_OWNER);
        assertTrue("The new owner should hold Editor on the version that was named",
                hasEditorRole(v2Folder, TOOL_SECOND_OWNER));
        assertTrue("The new owner should hold Editor on the older version too",
                hasEditorRole(v1Folder, TOOL_SECOND_OWNER));

        log("Removing an owner reaches every version's folder as well");
        setOwners(v2RowId, TOOL_SECOND_OWNER);
        assertFalse("The dropped owner should lose Editor on the version that was named",
                hasEditorRole(v2Folder, TOOL_AUTHOR));
        assertFalse("The dropped owner should lose Editor on the older version too",
                hasEditorRole(v1Folder, TOOL_AUTHOR));
    }

    /**
     * One store folder must not list another's tools. The JSON API stays global on purpose, because
     * Skyline generates URLs that target /home, not the tool store folder to get a list of all external tools.
     */
    @Test
    public void testListingIsScopedToThisStoreButApiIsNot()
    {
        createStore(OTHER_STORE);

        uploadToolTo(OTHER_STORE, TOOL_OTHER);

        JSONObject otherTool = onlyToolInStore(OTHER_STORE);
        String otherName = otherTool.getString("Name");

        goToProjectHome(OTHER_STORE);
        assertTrue("The other store should list its own tool",
                new SkylineToolStoreWebPart(getDriver()).hasTool(otherName));

        goToProjectHome(PROJECT_NAME);
        assertFalse("This store must not list another store's tool",
                new SkylineToolStoreWebPart(getDriver()).hasTool(otherName));

        // getToolsApi returns the global list of tools.
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
        createStore(FORMS_STORE);

        log("Add a tool through the web part's Add New Tool dialog");
        goToProjectHome(FORMS_STORE);
        new SkylineToolStoreWebPart(getDriver()).addTool(_formsToolV1, null);

        goToProjectHome(FORMS_STORE);
        assertTrue("The dialog should have added the tool to the listing",
                new SkylineToolStoreWebPart(getDriver()).hasTool(FORMS_TOOL_NAME));
        assertEquals("The dialog should have added exactly one tool", 1, toolsInStore(FORMS_STORE));

        log("Add a second tool, so the shared dialog has a row it could be addressed to wrongly");
        new SkylineToolStoreWebPart(getDriver()).addTool(ToolStoreTestHelper.writeMinimalToolZip(
                FORMS_SECOND_TOOL_NAME, FORMS_SECOND_TOOL_IDENTIFIER, "1.0"), null);
        goToProjectHome(FORMS_STORE);
        assertEquals("The store should now hold both tools", 2, toolsInStore(FORMS_STORE));

        // The web part displays a settings gear per tool row, but one dialog serves them all and the
        // handler sets the tool id when it opens.
        assertNotEquals("The two tools must have different ids",
                new SkylineToolStoreWebPart(getDriver()).getTool(FORMS_TOOL_NAME).getToolId(),
                new SkylineToolStoreWebPart(getDriver()).getTool(FORMS_SECOND_TOOL_NAME).getToolId());

        log("Each row's gear opens the shared dialog addressed to that row's tool");
        for (String name : List.of(FORMS_TOOL_NAME, FORMS_SECOND_TOOL_NAME))
        {
            ToolRow row = new SkylineToolStoreWebPart(getDriver()).getTool(name);
            SupplementaryFileDialog rowDialog = row.clickUploadSupplementaryFile();
            assertEquals("The dialog should carry " + name + "'s tool id",
                    String.valueOf(row.getToolId()), rowDialog.getToolId());
            rowDialog.dismiss("Cancel");
        }

        SupplementaryFileDialog suppDialog = new SkylineToolStoreWebPart(getDriver())
                .getTool(FORMS_TOOL_NAME).clickUploadSupplementaryFile();

        // The modal is in the page from the start and is reused on every open, so a file chosen
        // and then cancelled would be attached to the next upload. clearFileInputsOnClose empties
        // it on hidden.bs.modal.
        suppDialog.setFile(TestFileUtils.getSampleData(SUPP_FILE));
        assertNotEquals("The file should be attached before the Cancel",
                "", suppDialog.getSelectedFile());
        suppDialog.dismiss("Cancel");

        SupplementaryFileDialog reopened = new SkylineToolStoreWebPart(getDriver())
                .getTool(FORMS_TOOL_NAME).clickUploadSupplementaryFile();
        assertEquals("Reopening must not keep the file the Cancel discarded",
                "", reopened.getSelectedFile());
        reopened.dismiss("Cancel");

        log("Publish a new version through the details page dialog");
        SkylineToolDetailsPage details = new SkylineToolStoreWebPart(getDriver())
                .getTool(FORMS_TOOL_NAME).clickToolName();
        details = details.uploadNewVersion(_formsToolV2);

        assertEquals("The dialog should have published 2.0",
                "2.0", toolInStore(FORMS_STORE, FORMS_TOOL_NAME).getString("Version"));
        assertEquals("The details page should show the version it just published",
                "2.0", details.getVersion());
        assertEquals("Publishing a version must not add a tool", 2, toolsInStore(FORMS_STORE));

        log("The owners field completes an address from its dropdown");
        ManageToolOwnersDialog ownersDialog = details.clickManageToolOwners()
                .typeOwner("toolstore_bystander")
                .clickTypeAheadOption(OTHER_USER);
        assertEquals("Picking from the dropdown should replace the term being typed and leave a " +
                        "separator ready for the next address",
                OTHER_USER + ", ", ownersDialog.getOwners());

        ownersDialog.appendOwner("toolstore_author").clickTypeAheadOption(TOOL_AUTHOR);
        assertEquals("Picking a second address should append it after the first and leave a " +
                        "separator ready",
                OTHER_USER + ", " + TOOL_AUTHOR + ", ", ownersDialog.getOwners());
        ownersDialog.dismiss("Cancel");

        log("Delete the newest version through the details page dialog");
        goToProjectHome(FORMS_STORE);
        new SkylineToolStoreWebPart(getDriver()).getTool(FORMS_TOOL_NAME).clickToolName()
                .clickDeleteLatestVersion().confirmExpectingPageLoad();

        // Read the version from the catalog rather than the page. The details page carries script
        // constants that a bare text search for a version number picks up.
        assertEquals("Deleting the newest version should leave 1.0 as the latest",
                "1.0", toolInStore(FORMS_STORE, FORMS_TOOL_NAME).getString("Version"));
    }

    /**
     * The two delete items on a tool's settings menu in the web part, which no other test reaches.
     *
     * Both post over ajax and then go to the successUrl the action returns, so a refusal has to be
     * shown in the dialog rather than read as a success.
     */
    @Test
    public void testDeletingTheLatestVersionThenTheToolFromToolSettingsMenu()
    {
        createStore(WEBPART_STORE);

        uploadToolFileTo(WEBPART_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                WEBPART_TOOL_NAME, WEBPART_TOOL_IDENTIFIER, "1.0"));
        int v1RowId = rowId(onlyToolInStore(WEBPART_STORE));
        String v1Folder = "/" + WEBPART_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(WEBPART_TOOL_NAME, "1.0");
        uploadToolFileTo(v1Folder, ToolStoreTestHelper.writeMinimalToolZip(
                WEBPART_TOOL_NAME, WEBPART_TOOL_IDENTIFIER, "2.0"), v1RowId);
        uploadToolFileTo(WEBPART_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                WEBPART_OTHER_TOOL_NAME, WEBPART_OTHER_TOOL_IDENTIFIER, "1.0"));

        goToProjectHome(WEBPART_STORE);
        SkylineToolStoreWebPart webPart = new SkylineToolStoreWebPart(getDriver());
        assertEquals("The listing should show the version just published",
                "2.0", webPart.getTool(WEBPART_TOOL_NAME).getVersion());
        assertEquals("The other tool should be listed at its own version",
                "1.0", webPart.getTool(WEBPART_OTHER_TOOL_NAME).getVersion());

        log("Delete the newest version from the web part's menu");
        new SkylineToolStoreWebPart(getDriver()).getTool(WEBPART_TOOL_NAME)
                .clickDeleteLatestVersion().confirmExpectingPageLoad();

        assertEquals("Deleting the newest version should leave 1.0 as the latest",
                "1.0", toolInStore(WEBPART_STORE, WEBPART_TOOL_NAME).getString("Version"));
        assertEquals("Deleting one tool's version must not touch the other",
                "1.0", toolInStore(WEBPART_STORE, WEBPART_OTHER_TOOL_NAME).getString("Version"));
        assertEquals("Both tools should still be listed", 2, toolsInStore(WEBPART_STORE));

        log("Delete the whole tool from the web part's menu");
        goToProjectHome(WEBPART_STORE);
        new SkylineToolStoreWebPart(getDriver()).getTool(WEBPART_TOOL_NAME)
                .clickDelete().confirmExpectingPageLoad();

        assertEquals("Only the deleted tool should go", 1, toolsInStore(WEBPART_STORE));
        assertTrue(WEBPART_OTHER_TOOL_NAME + " should still be listed",
                new SkylineToolStoreWebPart(getDriver()).hasTool(WEBPART_OTHER_TOOL_NAME));
    }

    /**
     * A tool version folder containing a subfolder cannot be removed. ContainerManager.delete returns
     * false rather than throwing. That must not leave two tool rows flagged as the latest.
     * getToolLatestByIdentifier matches nothing when more than one row carries the flag, and Skyline
     * clients then stop resolving that tool.
     */
    @Test
    public void testADeleteLatestThatCannotRemoveTheFolderChangesNothing()
    {
        createStore(BLOCKED_STORE);

        uploadToolFileTo(BLOCKED_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                BLOCKED_TOOL_NAME, BLOCKED_TOOL_IDENTIFIER, "1.0"));
        int v1RowId = rowId(onlyToolInStore(BLOCKED_STORE));
        String v1FolderName = ToolStoreTestHelper.toolFolderName(BLOCKED_TOOL_NAME, "1.0");
        uploadToolFileTo("/" + BLOCKED_STORE + "/" + v1FolderName,
                ToolStoreTestHelper.writeMinimalToolZip(BLOCKED_TOOL_NAME, BLOCKED_TOOL_IDENTIFIER, "2.0"),
                v1RowId);

        int v2RowId = rowId(onlyToolInStore(BLOCKED_STORE));
        String v2FolderName = ToolStoreTestHelper.toolFolderName(BLOCKED_TOOL_NAME, "2.0");

        // Create a child folder - this will make folder deletion fail
        _containerHelper.createSubfolder(BLOCKED_STORE, v2FolderName, "keepsTheFolderAlive",
                "Collaboration", null);

        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts",
                "/" + BLOCKED_STORE + "/" + v2FolderName, "deleteLatest"));
        request.setEntity(MultipartEntityBuilder.create()
                .addTextBody("toolId", String.valueOf(v2RowId))
                .build());
        int status = execute(request);

        assertEquals("A folder that could not be deleted has to be refused, not reported as deleted",
                400, status);
        // A failure to delete the tool folder should leave the store as it was, with only one
        // version flagged as latest.
        assertEquals("The tool should still be listed once", 1, toolsInStore(BLOCKED_STORE));
        assertEquals("2.0 should still be the latest version",
                "2.0", onlyToolInStore(BLOCKED_STORE).getString("Version"));

        log("The same refusal reaches the person who clicked the menu item");
        goToProjectHome(BLOCKED_STORE);
        // confirmExpectingRefusal, because no page is loaded here. The server refuses and the
        // reason is shown in the modal when the request returns.
        String refusal = new SkylineToolStoreWebPart(getDriver()).getTool(BLOCKED_TOOL_NAME)
                .clickDeleteLatestVersion().confirmExpectingRefusal();

        // Two different refusals both say "could not be deleted". Only the one that happens before any
        // row is changed adds "so nothing was changed". That is the path this test sets up.
        assertTrue("The dialog should say nothing was changed, got: " + refusal,
                refusal.contains("nothing was changed"));
        assertEquals("2.0 should still be the latest version after the dialog was used",
                "2.0", onlyToolInStore(BLOCKED_STORE).getString("Version"));
    }

    /**
     * The store dropped jQuery UI for Bootstrap. Nothing should pull it back, from a CDN or from the
     * module's own webapp folder. A page that quietly reloaded it would let the converted widgets
     * keep working by accident, so the removal would look finished when it was not.
     */
    @Test
    public void testNoPageLoadsJQueryUi()
    {
        String store = NO_JQUERY_UI_STORE;
        createStore(store);

        File zip = ToolStoreTestHelper.writeMinimalToolZip("JQueryUiProbe",
                "URN:LSID:toolstore.test:jqueryuiprobe", "1.0");
        uploadToolFileTo(store, zip, -1);
        JSONObject tool = onlyToolInStore(store);

        // Every page that used to carry a jQuery UI tag, in the order a user meets them.
        goToProjectHome(store);
        assertNoJQueryUi("the store web part");

        new SkylineToolStoreWebPart(getDriver()).getTool(tool.getString("Name")).clickToolName();
        assertNoJQueryUi("the tool details page");

        beginAt(WebTestHelper.buildURL("skyts", store, "insertTool"));
        assertNoJQueryUi("the add a tool page");

        beginAt(WebTestHelper.buildURL("skyts", store, "setOwners",
                Map.of("toolId", String.valueOf(rowId(tool)))));
        assertNoJQueryUi("the manage owners page");
    }

    /**
     * A delete the server refuses must be reported in the dialog rather than read as a success.
     *
     * DeleteAction responds to a refusal with an error status, so the handler's .fail() runs and
     * showModalError puts the reason in the dialog body.
     */
    @Test
    public void testDeletingAToolThatIsAlreadyGoneReportsTheRefusal()
    {
        String store = STALE_DELETE_STORE;
        String tool = "StaleDeleteProbe";
        File zip = ToolStoreTestHelper.writeMinimalToolZip(tool,
                "URN:LSID:toolstore.test:staledelete", "1.0");
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(zip, null);

        goToProjectHome(store);
        SkylineToolStoreWebPart webPart = new SkylineToolStoreWebPart(getDriver());

        // Someone else deletes it while this page sits there, so the confirm below reaches a tool
        // the server can no longer find.
        ToolStoreTestHelper.removeToolsFromCatalog(store, zip);

        String message = webPart.getTool(tool).clickDelete().confirmExpectingRefusal();
        // DeleteAction names the row it could not find. Matching that rather than the word "error" keeps
        // this from passing on any refusal at all.
        assertTrue("The dialog should report what the server said, got: " + message,
                message.contains("does not exist"));
    }

    /**
     * A refused delete on the details page leaves the dialog usable on the next open.
     *
     * A refusal replaces the body with the error message and hides the Ok button. Closing the dialog causes
     * the dialog state to be reset in restoreOnClose. Otherwise, opening the dialog a second time would show
     * the old refusal message.
     */
    @Test
    public void testADetailsPageDialogIsUsableAfterARefusal()
    {
        String store = REOPEN_STORE;
        String tool = "ReopenProbe";
        File zip = ToolStoreTestHelper.writeMinimalToolZip(tool,
                "URN:LSID:toolstore.test:reopen", "1.0");
        createStore(store);

        goToProjectHome(store);
        SkylineToolDetailsPage details = new SkylineToolStoreWebPart(getDriver()).addTool(zip, null);

        // Deletes the tool over HTTP rather than through the UI. The browser page is still left open for
        // the tool. Clicking delete will return a refusal from the server that is displayed in the dialog.
        ToolStoreTestHelper.removeToolsFromCatalog(store, zip);

        ConfirmDeleteDialog refused = details.clickDelete();
        String refusal = refused.confirmExpectingRefusal();
        assertTrue("The dialog should report what the server said, got: " + refusal,
                refusal.contains("does not exist"));

        log("Cancel, then open the same dialog again");
        refused.cancel();
        ConfirmDeleteDialog reopened = details.clickDelete();

        // Reopening the dialog should display the question that names the tool, not the refusal error message.
        String question = "Are you sure you want to completely remove " + tool + " from the store?";
        assertTrue("The dialog should ask its question again, got: " + reopened.getMessage(),
                reopened.getMessage().contains(question));
        assertTrue("The confirm button should be back", reopened.isConfirmOffered());
    }

    /**
     * Tab completes the highlighted address instead of moving focus off the field.
     *
     * Without it the field keeps the partial term, so Update Tool Owners posts something like
     * "toolst" and SetOwnersAction refuses it as an unknown user.
     */
    @Test
    public void testTabCompletesTheHighlightedAddress()
    {
        String store = TAB_STORE;
        String tool = "TabProbe";
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(
                ToolStoreTestHelper.writeMinimalToolZip(tool, "URN:LSID:toolstore.test:tabkey",
                        "1.0"), null);

        goToProjectHome(store);
        ManageToolOwnersDialog dialog = new SkylineToolStoreWebPart(getDriver())
                .getTool(tool).clickManageToolOwners();

        // Typing narrows the list to addresses containing the term, which is the case a user meets.
        dialog.typeOwner("toolstore_a");
        assertTrue("Typing should open the suggestion list", dialog.isTypeAheadShowing());

        // The candidates are every active account on the server, including ones other test classes
        // create. Which address sorts first is not this test's to assume. One Down highlights the
        // first, and that is what Tab has to produce.
        String firstOffered = dialog.getTypeAheadOptions().get(0);
        dialog.pressDown().pressTab();

        assertEquals("Tab should complete the highlighted address and leave a separator ready",
                firstOffered + ", ", dialog.getOwners());
    }

    /**
     * Two keyboard cases the first Tab fix got wrong.
     *
     * Shift+Tab is a user leaving the field backwards. Completing there put an address into the list
     * that nobody chose, and Update Tool Owners would have granted it Editor. Up on a closed list has
     * to highlight the last entry, the way Down highlights the first. It must not open the list and
     * select nothing.
     */
    @Test
    public void testShiftTabLeavesTheFieldAndUpHighlightsFromAClosedList()
    {
        String store = SHIFT_TAB_STORE;
        String tool = "ShiftTabProbe";
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(
                ToolStoreTestHelper.writeMinimalToolZip(tool, "URN:LSID:toolstore.test:shifttab",
                        "1.0"), null);

        goToProjectHome(store);
        ManageToolOwnersDialog dialog = new SkylineToolStoreWebPart(getDriver())
                .getTool(tool).clickManageToolOwners();

        dialog.typeOwner("toolstore_");
        dialog.pressDown();
        assertNotNull("Down should highlight an entry", dialog.getHighlightedOption());

        dialog.pressShiftTab();
        assertEquals("Shift+Tab must not complete anything",
                "toolstore_", dialog.getOwners());

        // Up on a closed list. One press should land on the last entry, not on nothing.
        dialog.typeOwner("toolstore_");
        dialog.pressEscape();
        assertFalse("The list should be closed", dialog.isTypeAheadShowing());
        dialog.pressUp();
        assertNotNull("One Up press on a closed list should highlight an entry",
                dialog.getHighlightedOption());
    }

    /**
     * What the details page shows for one tool, read through the page component.
     *
     * The store has no other coverage of the page rendering a tool's own fields. Everything else reads
     * the catalog over the API. That cannot tell whether the page drew what the catalog holds.
     */
    @Test
    public void testTheDetailsPageShowsTheToolsProperties()
    {
        String store = PROPERTIES_STORE;
        String tool = "PropertiesProbe";
        createStore(store);

        goToProjectHome(store);
        SkylineToolDetailsPage details = new SkylineToolStoreWebPart(getDriver())
                .addTool(ToolStoreTestHelper.writeToolZip(tool,
                        "URN:LSID:toolstore.test:properties", "1.0",
                        "Description = A tool for exercising the details page.\n" +
                        "Organization = MacCoss Lab\n"), null);

        assertEquals("The page should name the tool it was asked for", tool, details.getToolName());
        assertEquals("1.0", details.getVersion());
        assertEquals("A tool nobody has downloaded yet", 0, details.getDownloadCount());

        assertTrue("The description row should be on the page",
                details.hasProperty(SkylineToolDetailsPage.DESCRIPTION));
        assertEquals("A tool for exercising the details page.",
                details.getProperty(SkylineToolDetailsPage.DESCRIPTION));
        assertEquals("MacCoss Lab", details.getProperty(SkylineToolDetailsPage.ORGANIZATION));

        assertTrue("A site admin should be offered the edit pencils", details.canEditProperties());

        // The zip carries no documentation url and the tool has no supplementary files, so the
        // whole box is absent rather than empty.
        assertFalse("Nothing to document yet", details.isDocumentationBoxShowing());
        assertFalse(details.hasOnlineDocumentationLink());

        details = details.uploadSupplementaryFile(writeFileNamed("manual.pdf"));
        assertTrue("A supplementary file should bring the documentation box out",
                details.isDocumentationBoxShowing());
        assertTrue(details.getSupplementaryFileNames().contains("manual.pdf"));
    }

    /**
     * Escape has to reach the type-ahead without reaching the dialog around it.
     *
     * Bootstrap's modal hides on any Escape that bubbles up to it and does not check preventDefault.
     * Dismissing the suggestion list used to throw away the whole dialog along with whatever had been
     * typed into it. The old jQuery UI widget could not do this. It called preventDefault on Escape
     * and the jQuery UI dialog honoured that.
     *
     * The last two assertions carry as much weight as the first two. Stopping propagation whether or
     * not the list is open would pass the middle of this test. It would leave a dialog that Escape
     * cannot close at all.
     */
    @Test
    public void testEscapeClosesTheTypeAheadBeforeTheDialog()
    {
        String store = ESCAPE_STORE;
        String tool = "EscapeProbe";
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(
                ToolStoreTestHelper.writeMinimalToolZip(tool, "URN:LSID:toolstore.test:escape",
                        "1.0"), null);

        goToProjectHome(store);
        ManageToolOwnersDialog dialog = new SkylineToolStoreWebPart(getDriver())
                .getTool(tool).clickManageToolOwners();

        dialog.typeOwner("toolstore_");
        assertTrue("Typing should open the suggestion list", dialog.isTypeAheadShowing());

        dialog.pressEscape();
        assertFalse("Escape should close the suggestion list", dialog.isTypeAheadShowing());
        assertTrue("The Escape that closed the suggestion list must leave the dialog open",
                dialog.isOpen());

        dialog.pressEscape();
        waitFor(() -> !dialog.isOpen(),
                "Escape with no suggestion list showing should close the dialog", 5_000);
    }

    /**
     * A refused owner list comes back on the reshown page exactly as it was typed.
     *
     * SkylineToolManageOwners.jsp renders the list into the input's value attribute with h(), and the
     * browser decodes it again, so an address containing an ampersand survives the round trip. This is
     * the only test that reaches the standalone page SetOwnersAction reshows on a refusal.
     */
    @Test
    public void testARefusedOwnerListComesBackUnchanged()
    {
        String store = OWNER_ESCAPING_STORE;
        String tool = "OwnerEscapingProbe";
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(
                ToolStoreTestHelper.writeMinimalToolZip(tool,
                        "URN:LSID:toolstore.test:ownerescaping", "1.0"), null);

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
                ToolStoreTestHelper.writeMinimalToolZip(tool, "URN:LSID:toolstore.test:noextension",
                        "1.0"), null);

        details = details.uploadSupplementaryFile(writeFileNamed("README"));
        assertTrue("The details page should list the file",
                details.getSupplementaryFileNames().contains("README"));

        log("The store listing still renders, which is what used to break");
        goToProjectHome(store);
        assertTrue("The listing must still show the tool",
                new SkylineToolStoreWebPart(getDriver()).hasTool(tool));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Fails if the page currently loaded pulls jQuery UI from anywhere.
     *
     * Looks at the urls of the script and stylesheet elements, and matches the library's file name
     * rather than the string anywhere in the url. Two loose versions of this check were both wrong:
     * searching the page source matches the tool name in an ordinary link, and matching "jqueryui"
     * anywhere in a url matches the folder name, which is a segment of every url on the page.
     */
    private void assertNoJQueryUi(String pageDescription)
    {
        String loaded = executeScript(
                "return Array.from(document.querySelectorAll('script[src], link[href]'))" +
                        ".map(function(e) { return e.src || e.href; })" +
                        ".filter(function(u) { return /jquery-?ui(\\.min)?\\.(js|css)/i.test(u); })" +
                        ".join(', ');",
                String.class);
        assertEquals("jQuery UI is loaded on " + pageDescription, "", loaded);
    }

    /**
     * An older version's page must not offer to publish a new version. UpdateToolAction refuses
     * anything but the latest, so offering it there spent the owner's whole upload before saying so.
     *
     * Nor may it offer to delete the latest version, because DeleteLatestAction removes the newest
     * version whatever page it was clicked from, so from here it acts on a version other than the
     * one being viewed.
     *
     * Supplementary files are per version and the action accepts them on any version, so that item
     * has to stay. Checked here as well, to keep a later change from hiding the whole menu.
     *
     * Deleting the whole tool also stays, because DeleteAction takes every version wherever it is
     * clicked from. Its label is asserted rather than just its presence. From an older version's
     * page the tool being removed is not the one on screen, so "Delete" next to "Delete latest
     * version" would read as this version.
     *
     * The last block covers the URL rather than the menu, because hiding an item only removes the
     * way in that the UI offers. The action itself has to refuse before it draws an upload form,
     * or an owner spends a whole upload before being told no.
     */
    @Test
    public void testAnOlderVersionPageOffersOnlyWhatItCanDo()
    {
        createStore(OLDER_STORE);

        uploadToolFileTo(OLDER_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                OLDER_TOOL_NAME, OLDER_TOOL_IDENTIFIER, "1.0"));
        int v1RowId = rowId(onlyToolInStore(OLDER_STORE));
        String v1Folder = "/" + OLDER_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(OLDER_TOOL_NAME, "1.0");
        uploadToolFileTo(v1Folder, ToolStoreTestHelper.writeMinimalToolZip(
                OLDER_TOOL_NAME, OLDER_TOOL_IDENTIFIER, "2.0"), v1RowId);

        log("The latest version's page offers both items");
        beginAt(WebTestHelper.buildURL("skyts", OLDER_STORE, "details",
                Map.of("name", OLDER_TOOL_NAME)));
        assertTrue("The latest version should still offer Upload new version",
                detailsPageHasMenuItem("Upload new version"));
        assertTrue("The latest version is the one Delete latest version acts on, so it belongs here",
                detailsPageHasMenuItem("Delete latest version"));

        log("The older version's page offers only the supplementary file item");
        beginAt(WebTestHelper.buildURL("skyts", OLDER_STORE, "details",
                Map.of("name", OLDER_TOOL_NAME, "version", "1.0")));
        assertFalse("Upload new version must not be offered where the action would refuse it",
                detailsPageHasMenuItem("Upload new version"));
        assertFalse("Delete latest version must not be offered where it would act on another version",
                detailsPageHasMenuItem("Delete latest version"));
        assertTrue("Upload supplementary file works on any version and must stay",
                detailsPageHasMenuItem("Upload supplementary file"));
        assertTrue("Deleting the whole tool works from any version and must say what it removes",
                detailsPageHasMenuItem("Delete tool from store"));

        log("The URL behind the hidden item refuses before drawing the form");
        beginAt(WebTestHelper.buildURL("skyts", v1Folder, "updateTool",
                Map.of("toolId", String.valueOf(v1RowId))));
        assertElementNotPresent("The upload form must not be drawn where the post would be refused",
                Locator.css("input[name='toolZip']"));
        assertTextPresent("is not the latest version of");

        log("deleteLatest refuses an older version rather than removing the newest one");
        HttpPost deleteOlder = new HttpPost(WebTestHelper.buildURL("skyts", v1Folder, "deleteLatest"));
        deleteOlder.setEntity(MultipartEntityBuilder.create()
                .addTextBody("toolId", String.valueOf(v1RowId))
                .build());
        assertEquals("deleteLatest addressed to a version that is not the latest has to be refused",
                400, execute(deleteOlder));
        assertEquals("2.0 should still be the latest version", "2.0",
                onlyToolInStore(OLDER_STORE).getString("Version"));
    }

    /**
     * The editing controls need all three of Update, Insert and Delete, not just one of them.
     *
     * tool.isEditor requires the three together, and the settings gear, the edit pencils and the
     * supplementary file trash icons all gate on it. Author holds Insert alone, so none of them is
     * drawn, and adding Editor brings the other two so they come back.
     */
    @Test
    public void testEditingControlsNeedAllThreePermissions()
    {
        String store = PARTIAL_PERMS_STORE;
        String tool = "PartialPermsProbe";
        createStore(store);

        goToProjectHome(store);
        new SkylineToolStoreWebPart(getDriver()).addTool(
                ToolStoreTestHelper.writeMinimalToolZip(tool, "URN:LSID:toolstore.test:partialperms",
                        "1.0"), null)
                .uploadSupplementaryFile(TestFileUtils.getSampleData(SUPP_FILE));

        // Two grants, because createVersionFolder saves an explicit policy on every tool folder -
        // Guests and All Site Users as Readers, plus the owners as Editors. The folder therefore
        // inherits nothing from the store, and isEditor reads the folder rather than the store.
        // Author on the store is what makes the listing render. Author on the tool folder is the
        // subset under test, Insert without Update or Delete.
        String toolFolder = "/" + store + "/" + ToolStoreTestHelper.toolFolderName(tool, "1.0");
        _permissionsHelper.addMemberToRole(OTHER_USER, "Author", PermissionsHelper.MemberType.user,
                "/" + store);
        _permissionsHelper.addMemberToRole(OTHER_USER, "Author", PermissionsHelper.MemberType.user,
                toolFolder);

        log("Author holds Insert but neither Update nor Delete");
        impersonate(OTHER_USER);
        try
        {
            goToProjectHome(store);
            assertFalse("The web part row must not offer a settings menu",
                    new SkylineToolStoreWebPart(getDriver()).getTool(tool).hasSettingsMenu());

            SkylineToolDetailsPage page = new SkylineToolStoreWebPart(getDriver())
                    .getTool(tool).clickToolName();
            assertFalse("The details page must not offer a settings menu", page.hasSettingsMenu());
            assertFalse("The details page must not offer the edit pencils", page.canEditProperties());
            assertFalse("The details page must not offer the supplementary file trash icons",
                    page.canDeleteSupplementaryFiles());

            // Proves the details page rendered, so the three assertions above it are absences
            // rather than a page that failed to load. getTool covers the web part the same way,
            // since it throws when the row is not there.
            assertTrue("The supplementary file should still be listed for a user who cannot edit",
                    page.getSupplementaryFileNames().contains("test.pdf"));
        }
        finally
        {
            stopImpersonating();
        }

        log("Editor on the tool folder adds Update and Delete, so the controls come back");
        _permissionsHelper.addMemberToRole(OTHER_USER, "Editor", PermissionsHelper.MemberType.user,
                toolFolder);
        impersonate(OTHER_USER);
        try
        {
            goToProjectHome(store);
            assertTrue("Holding all three, the web part row should offer a settings menu",
                    new SkylineToolStoreWebPart(getDriver()).getTool(tool).hasSettingsMenu());
            assertTrue("Holding all three, the details page should offer the edit pencils",
                    new SkylineToolStoreWebPart(getDriver()).getTool(tool).clickToolName()
                            .canEditProperties());
        }
        finally
        {
            stopImpersonating();
        }
    }

    /**
     * A tool's own folder shows that tool rather than an empty store.
     *
     * The module is enabled in every tool folder on skyline.ms, and a version folder has no children,
     * so a listing there has nothing to show. The folder holds exactly one tool row, so its store
     * page stands in for that tool's details page.
     */
    @Test
    public void testAToolsOwnFolderShowsThatTool()
    {
        createStore(FOLDER_STORE);

        uploadToolFileTo(FOLDER_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                FOLDER_TOOL_NAME, FOLDER_TOOL_IDENTIFIER, "1.0"));
        String toolFolder = "/" + FOLDER_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(FOLDER_TOOL_NAME, "1.0");
        _containerHelper.enableModule(toolFolder, "SkylineToolsStore");

        assertEquals("The store folder lists its one tool", 1, toolsInStore(FOLDER_STORE));

        log("The store folder itself still lists its tools and offers Add New Tool");
        beginAt(WebTestHelper.buildURL("skyts", FOLDER_STORE, "begin"));
        assertElementPresent(Locator.id("add-new-tool-btn"));

        log("The tool's own folder lands on that tool's details page");
        beginAt(WebTestHelper.buildURL("skyts", toolFolder, "begin"));
        // download-tool-btn belongs to the details page. The listing has no element with that id,
        // so this separates a details page from a store page showing the same tool.
        assertElementPresent(Locator.id("download-tool-btn"));
        assertTextPresent(FOLDER_TOOL_NAME);

        // The redirect makes this page somewhere a user lands rather than somewhere they clicked
        // through to, so it needs its own way back. The details page carries this text only in
        // the nav trail.
        assertElementPresent("The details page needs a link back to the store",
                Locator.linkWithText("Skyline Tool Store"), 1);
    }

    /**
     * A tool's own folder refuses an upload posted straight at it.
     *
     * createVersionFolder creates the version folder under the container the action runs in, and the
     * module is enabled in every tool folder, so an upload from one would file the new tool inside
     * another tool, where the store listing cannot reach it. Separate from the test above because
     * neither the redirect nor the hidden button stops a post sent straight to the URL.
     */
    @Test
    public void testAToolCannotBeAddedFromInsideAnotherToolsFolder()
    {
        createStore(NESTED_STORE);

        uploadToolFileTo(NESTED_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                NESTED_HOST_TOOL_NAME, NESTED_HOST_TOOL_IDENTIFIER, "1.0"));
        String toolFolder = "/" + NESTED_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(NESTED_HOST_TOOL_NAME, "1.0");
        _containerHelper.enableModule(toolFolder, "SkylineToolsStore");

        // Verifying the effect rather than the status, because a refusal renders 200.
        uploadToolFileTo(toolFolder, ToolStoreTestHelper.writeMinimalToolZip(
                NESTED_TOOL_NAME, NESTED_TOOL_IDENTIFIER, "1.0"));
        assertFalse("A tool must not be added from inside another tool's folder",
                ToolStoreTestHelper.catalogIdentifiers(NESTED_STORE).contains(NESTED_TOOL_IDENTIFIER));
    }

    /**
     * A corrupt tool zip is refused with a message rather than a server error.
     *
     * unzip returned null when reading the entry threw, and getToolFromZip then dereferenced it. An
     * NPE is not an IOException, so readToolFromUpload's catch never saw it and the upload died as a
     * server error rather than naming the file.
     */
    @Test
    public void testACorruptZipIsRefusedWithoutAServerError()
    {
        createStore(CORRUPT_STORE);

        assertEquals("Server errors were already pending before this test", 0, getServerErrorCount());

        uploadToolFileTo(CORRUPT_STORE, ToolStoreTestHelper.writeTruncatedToolZip(
                CORRUPT_TOOL_NAME, CORRUPT_TOOL_IDENTIFIER, "1.0"));

        // Read and cleared before asserting, so a failure here leaves nothing pending for whichever
        // test runs next. resetErrors is server wide.
        int errorsLogged = getServerErrorCount();
        resetErrors();

        assertEquals("A corrupt zip must be refused with a message, not a server error",
                0, errorsLogged);
        assertFalse("A corrupt zip must not add a tool",
                ToolStoreTestHelper.catalogIdentifiers(CORRUPT_STORE).contains(CORRUPT_TOOL_IDENTIFIER));
    }

    /**
     * Whether the details page currently loaded offers a settings menu item.
     *
     * Items that do not apply are gated in the JSP, so an absent item is absent from the page
     * rather than hidden. hasMenuItem opens the menu, which Bootstrap does without animating.
     */
    private boolean detailsPageHasMenuItem(String item)
    {
        return new SkylineToolDetailsPage(getDriver()).hasMenuItem(item);
    }

    /** A store folder of its own, so one test's tools cannot disturb another's counts. */
    private void createStore(String projectName)
    {
        _containerHelper.createProject(projectName, "Collaboration");
        _containerHelper.enableModule(projectName, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");
    }

    /**
     * A small file carrying exactly the name given. The callers pass "manual.pdf" and "README", and
     * the extension, or its absence, is what those tests are about.
     */
    private static File writeFileNamed(String name)
    {
        try
        {
            File file = TestFileUtils.ensureTestTempFile("skylinetoolsstore", "supp", name);
            Files.writeString(file.toPath(), "supplementary file for the tool store tests");
            return file;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not write a file named " + name, e);
        }
    }

    /** Number of tools the given store folder lists. */
    private int toolsInStore(String storeContainerPath)
    {
        return toolsInStoreJson(storeContainerPath).length();
    }

    /**
     * An upload that fails after the version folder has been created must not leave the folder
     * behind. makeContainer refuses a name that is already taken, so an orphan blocks that same
     * version from then on - the owner retries with a good zip and is told the tool already exists,
     * with nothing to say the folder is the reason.
     *
     * Asserts the effect rather than the status. InsertToolAction is a FormViewAction and re-renders
     * the upload form at 200 on a refusal, so a status alone proves nothing.
     */
    @Test
    public void testAnUploadThatFailsPartWayLeavesNoFolderBehind()
    {
        createStore(RETRY_STORE);

        // resetErrors is server wide, so consuming this upload's errors below consumes everything
        // logged since the last mark. checkErrors() runs after every test method, so nothing should
        // be pending here - assert that rather than let the reset hide an error silently.
        assertEquals("Server errors were already pending before this test",
                0, getServerErrorCount());

        // Its icon is not a decodable image, so storing the version throws in writeIconToFile, which
        // runs after the folder has been made.
        uploadToolFileTo(RETRY_STORE, ToolStoreTestHelper.writeToolZipWithUnreadableIcon(
                RETRY_TOOL_NAME, RETRY_TOOL_IDENTIFIER, RETRY_TOOL_VERSION));

        // createVersionFolder turns the failure into a message on the form, so nothing should reach the
        // log as a server error. Read and cleared before asserting, so a failure here leaves nothing
        // pending for the next test. resetErrors is server wide.
        int errorsLogged = getServerErrorCount();
        resetErrors();
        assertEquals("An unreadable icon must be refused with a message, not a server error",
                0, errorsLogged);

        assertFalse("The upload was supposed to fail while storing the version",
                ToolStoreTestHelper.catalogIdentifiers(RETRY_STORE).contains(RETRY_TOOL_IDENTIFIER));

        // Same name and version, so it needs the folder name the failed upload already used.
        uploadToolFileTo(RETRY_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                RETRY_TOOL_NAME, RETRY_TOOL_IDENTIFIER, RETRY_TOOL_VERSION));
        assertTrue("A failed upload left its folder behind, blocking this version for good",
                ToolStoreTestHelper.catalogIdentifiers(RETRY_STORE).contains(RETRY_TOOL_IDENTIFIER));
    }

    /** Like uploadToolTo, for a zip this test built rather than one from sample data. */
    private int uploadToolFileTo(String containerPath, File zip)
    {
        return uploadToolFileTo(containerPath, zip, -1);
    }

    /** @param updateTarget row id of the tool getting a new version, or -1 for a brand-new tool */
    private int uploadToolFileTo(String containerPath, File zip, int updateTarget)
    {
        boolean newVersion = updateTarget >= 0;
        HttpPost request = new HttpPost(
                WebTestHelper.buildURL("skyts", containerPath, newVersion ? "updateTool" : "insertTool"));
        MultipartEntityBuilder entity = MultipartEntityBuilder.create()
                .addBinaryBody("toolZip", zip, ContentType.create("application/zip"), zip.getName());
        if (newVersion)
            entity.addTextBody("toolId", String.valueOf(updateTarget));
        request.setEntity(entity.build());
        return execute(request);
    }

    /**
     * A tool zip holding nothing but tool-inf/info.properties. Name, Version and Identifier are the
     * only required properties, and the sample zips in this module are tens of megabytes, so this
     * test builds its own rather than adding more of those to the repository.
     */
    private static File writeMinimalToolZip(String version)
    {
        return ToolStoreTestHelper.writeMinimalToolZip(FORMS_TOOL_NAME, FORMS_TOOL_IDENTIFIER, version);
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

    /** Publishes a new version, which is addressed to the tool's own folder. */
    private int uploadNewVersion(String toolContainerPath, String sampleDataRelativePath, int toolId)
    {
        return uploadToolTo(toolContainerPath, sampleDataRelativePath, toolId, null);
    }

    private void uploadToolTo(String containerPath, String sampleDataRelativePath)
    {
        uploadToolTo(containerPath, sampleDataRelativePath, -1, null);
    }

    /**
     * @param containerPath  the store folder for a new tool, or the tool's own folder for a new
     *                       version - the two actions are addressed to different containers
     * @param updateTarget   row id of the tool being updated, or -1 for a brand-new tool
     */
    @LogMethod
    private int uploadToolTo(String containerPath, String sampleDataRelativePath, int updateTarget,
                             String toolOwners)
    {
        File zip = TestFileUtils.getSampleData(sampleDataRelativePath);
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

    /**
     * insertSupplement is addressed to the tool's own container, so its permission annotation checks
     * the folder that holds the tool. The author holds Editor there and nothing on the store folder.
     */
    private int uploadSupplementaryFile(String toolContainerPath, int toolRowId)
    {
        File pdf = TestFileUtils.getSampleData(SUPP_FILE);
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", toolContainerPath, "insertSupplement"));
        request.setEntity(MultipartEntityBuilder.create()
                .addTextBody("toolId", String.valueOf(toolRowId))
                .addBinaryBody("suppFile", pdf, ContentType.create("application/pdf"), pdf.getName())
                .build());
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

    /** The named tool in the given store, for a store holding more than one. */
    private JSONObject toolInStore(String containerPath, String toolName)
    {
        JSONArray tools = toolsInStoreJson(containerPath);
        for (int i = 0; i < tools.length(); i++)
        {
            JSONObject tool = tools.getJSONObject(i);
            if (toolName.equals(tool.optString("Name")))
                return tool;
        }
        throw new AssertionError("No tool named " + toolName + " in " + containerPath);
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
                assertNull("Expected one tool in " + containerPath, found);
                found = tool;
            }
        }
        assertNotNull("No tool found in " + containerPath, found);
        return found;
    }

    private int rowId(JSONObject tool)
    {
        return ToolStoreTestHelper.rowId(tool);
    }

    private String toolFolderPath(JSONObject tool)
    {
        return "/" + PROJECT_NAME + "/" +
                ToolStoreTestHelper.toolFolderName(tool.getString("Name"), tool.getString("Version"));
    }

    private boolean hasEditorRole(String containerPath, String user)
    {
        return _permissionsHelper.getUserRoles(containerPath, user).stream()
                .anyMatch(role -> role.endsWith("EditorRole"));
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
        _containerHelper.deleteProject(OTHER_STORE, false);
        _containerHelper.deleteProject(FORMS_STORE, false);
        _containerHelper.deleteProject(WEBPART_STORE, false);
        _containerHelper.deleteProject(BLOCKED_STORE, false);
        _containerHelper.deleteProject(RETRY_STORE, false);
        _containerHelper.deleteProject(OLDER_STORE, false);
        _containerHelper.deleteProject(FOLDER_STORE, false);
        _containerHelper.deleteProject(NESTED_STORE, false);
        _containerHelper.deleteProject(CORRUPT_STORE, false);
        _containerHelper.deleteProject(NO_JQUERY_UI_STORE, false);
        _containerHelper.deleteProject(NO_EXTENSION_STORE, false);
        _containerHelper.deleteProject(STALE_DELETE_STORE, false);
        _containerHelper.deleteProject(OWNER_ESCAPING_STORE, false);
        _containerHelper.deleteProject(ESCAPE_STORE, false);
        _containerHelper.deleteProject(TAB_STORE, false);
        _containerHelper.deleteProject(PROPERTIES_STORE, false);
        _containerHelper.deleteProject(SHIFT_TAB_STORE, false);
        _containerHelper.deleteProject(REOPEN_STORE, false);
        _containerHelper.deleteProject(PARTIAL_PERMS_STORE, false);
        _userHelper.deleteUsers(false, TOOL_AUTHOR, TOOL_SECOND_OWNER, OTHER_USER);
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
