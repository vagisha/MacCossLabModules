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
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
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
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The real Skyline Tool Store workflow, end to end.
 *
 * Outside authors cannot upload to the store. They attach a zip to a message board post via a wiki
 * page, a site admin adds the tool naming the author as an owner, and the author can then maintain
 * their own tool without further admin help.
 */
@Category({External.class, MacCossLabModules.class})
@BaseWebDriverTest.ClassTimeout(minutes = 10)
public class ToolStoreWorkflowTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "ToolStoreWorkflowTest";
    private static final String OTHER_STORE = "ToolStoreWorkflowTestOtherStore";
    // Its own store so the tools this test adds cannot disturb the single-tool assertions elsewhere.
    private static final String FORMS_STORE = "ToolStoreWorkflowTestForms";

    private static final String FORMS_TOOL_NAME = "FormBindingProbe";
    private static final String FORMS_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:formbinding";
    private static File _formsToolV1;
    private static File _formsToolV2;

    // Its own store, because this test deliberately fails an upload and then retries the same
    // version, which would disturb the single-tool assertions in the other stores.
    private static final String RETRY_STORE = "ToolStoreWorkflowTestFailedUpload";
    private static final String RETRY_TOOL_NAME = "PartialUploadProbe";
    private static final String RETRY_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:partialupload";
    private static final String RETRY_TOOL_VERSION = "1.0";

    // Its own store, because it deletes everything it adds and the other stores assert on one tool.
    private static final String WEBPART_STORE = "ToolStoreWorkflowTestWebPartDeletes";
    private static final String WEBPART_TOOL_NAME = "WebPartDeleteProbe";
    private static final String WEBPART_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:webpartdelete";

    // Its own store, because it puts a stray folder in a tool's file root and leaves it there.
    private static final String STRAY_STORE = "ToolStoreWorkflowTestStrayFolder";
    private static final String STRAY_TOOL_NAME = "StrayFolderProbe";
    private static final String STRAY_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:strayfolder";

    // Its own store, because it leaves behind a version folder that cannot be deleted.
    private static final String BLOCKED_STORE = "ToolStoreWorkflowTestBlockedDelete";
    private static final String BLOCKED_TOOL_NAME = "BlockedDeleteProbe";
    private static final String BLOCKED_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:blockeddelete";

    // Its own store, because it needs a tool with two versions and the other stores assert on one.
    private static final String GROUP_STORE = "ToolStoreWorkflowTestOwnerGroup";
    private static final String GROUP_TOOL_NAME = "OwnerGroupProbe";
    private static final String GROUP_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:ownergroup";
    private static final String OWNER_GROUP = "ToolStoreOwnerGroup";
    private static final String ICON_STORE = "ToolStoreWorkflowTestIconReplace";
    private static final String ICON_TOOL_NAME = "IconReplaceProbe";
    private static final String ICON_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:iconreplace";
    private static final String NO_EXT_STORE = "ToolStoreWorkflowTestNoExtSupplement";
    private static final String NO_EXT_TOOL_NAME = "NoExtSupplementProbe";
    private static final String NO_EXT_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:noextsupplement";
    private static final String OLDER_STORE = "ToolStoreWorkflowTestOlderVersion";
    private static final String OLDER_TOOL_NAME = "OlderVersionProbe";
    private static final String OLDER_TOOL_IDENTIFIER = "URN:LSID:toolstore.test:olderversion";

    // Its own store, because it enables the module in a tool folder, which the other stores do not.
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

    private static final String WIKI_NAME = "submit-a-tool";
    private static final String WIKI_TITLE = "Submit a Skyline Tool";
    private static final String SUBMISSION_TITLE = "Skyline Tool submission";

    private static final String TOOL_V1 = "skylinetoolsstore/user1-v1.zip";
    private static final String TOOL_V2 = "skylinetoolsstore/user1-v2.zip";
    private static final String TOOL_OTHER = "skylinetoolsstore/user2-v1.zip";
    private static final String SUPP_FILE = "skylinetoolsstore/test.pdf";
    private static final String REPLACEMENT_ICON = "skylinetoolsstore/replacement-icon.png";

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
        _userHelper.createUser(TOOL_SECOND_OWNER);

        _formsToolV1 = writeMinimalToolZip("1.0");
        _formsToolV2 = writeMinimalToolZip("2.0");

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
        Set<String> beforeAdminAdd = catalogIdentifiers();
        impersonate(TOOL_AUTHOR);
        try
        {
            uploadTool(TOOL_V1, null);
        }
        finally
        {
            stopImpersonating();
        }
        assertEquals("A submitter must not be able to add a tool to the store",
                beforeAdminAdd, catalogIdentifiers());

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

        // The supplementary file is attached BEFORE the new version is published on purpose. A new
        // version copies the previous version's supplementary files into its own folder, and that
        // copy loop only runs when the previous version has some.
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
     * Skyline asks a container that is not the store folder and only finds tools because of it.
     */
    @Test
    public void testListingIsScopedToThisStoreButApiIsNot()
    {
        _containerHelper.createProject(OTHER_STORE, "Collaboration");
        _containerHelper.enableModule(OTHER_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

        uploadToolTo(OTHER_STORE, TOOL_OTHER);

        JSONObject otherTool = onlyToolInStore(OTHER_STORE);
        String otherName = otherTool.getString("Name");

        goToProjectHome(OTHER_STORE);
        assertTextPresent(otherName);

        goToProjectHome(PROJECT_NAME);
        assertTextNotPresent(otherName);

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
        click(Locator.id("add-new-tool-btn"));
        setFormElement(Locator.css("#uploadPop input[name='toolZip']"), _formsToolV1);
        clickAndWait(Locator.css("#uploadPop input[type='submit']"));

        goToProjectHome(FORMS_STORE);
        assertTextPresent(FORMS_TOOL_NAME);
        assertEquals("The dialog should have added exactly one tool", 1, toolsInStore(FORMS_STORE));

        log("Publish a new version through the details page dialog");
        clickAndWait(Locator.linkWithText(FORMS_TOOL_NAME));
        clickSprocketMenuItem("Upload new version");
        setFormElement(Locator.css("#uploadPop input[name='toolZip']"), _formsToolV2);
        clickAndWait(Locator.css("#uploadPop input[type='submit']"));

        assertEquals("The dialog should have published 2.0",
                "2.0", onlyToolInStore(FORMS_STORE).getString("Version"));
        assertEquals("Publishing a version must not add a second tool", 1, toolsInStore(FORMS_STORE));

        log("Delete the newest version through the details page dialog");
        clickSprocketMenuItem("Delete latest version");
        clickDialogOk("delToolLatestDlg");

        // Read the version from the catalog rather than the page - the details page carries script
        // constants that a bare text search for a version number picks up.
        assertEquals("Deleting the newest version should leave 1.0 as the latest",
                "1.0", onlyToolInStore(FORMS_STORE).getString("Version"));
    }

    /**
     * The two delete items on the web part's own gear menu, which no other test reaches.
     *
     * Both post over ajax and then go to the page the action names in its reply. When a refusal came
     * back as an error view at status 200 the caller could not tell it from a success, so the store
     * page was left saying the tool had gone.
     */
    @Test
    public void testWebPartDeleteItemsRemoveTheVersionAndThenTheTool()
    {
        _containerHelper.createProject(WEBPART_STORE, "Collaboration");
        _containerHelper.enableModule(WEBPART_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

        uploadToolFileTo(WEBPART_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                WEBPART_TOOL_NAME, WEBPART_TOOL_IDENTIFIER, "1.0"));
        int v1RowId = rowId(onlyToolInStore(WEBPART_STORE));
        String v1Folder = "/" + WEBPART_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(WEBPART_TOOL_NAME, "1.0");
        uploadToolFileTo(v1Folder, ToolStoreTestHelper.writeMinimalToolZip(
                WEBPART_TOOL_NAME, WEBPART_TOOL_IDENTIFIER, "2.0"), v1RowId);

        log("Delete the newest version from the web part's menu");
        goToProjectHome(WEBPART_STORE);
        clickSprocketMenuItem("Delete latest version");
        clickDialogOk("delToolLatestDlg");

        assertEquals("Deleting the newest version should leave 1.0 as the latest",
                "1.0", onlyToolInStore(WEBPART_STORE).getString("Version"));

        log("Delete the whole tool from the web part's menu");
        goToProjectHome(WEBPART_STORE);
        clickSprocketMenuItem("Delete tool from store");
        clickDialogOk("delToolAllDlg");

        assertEquals("Deleting the tool should leave the store empty",
                0, toolsInStore(WEBPART_STORE));
    }

    /**
     * A version folder holding a folder of its own cannot be removed - ContainerManager.delete
     * returns false rather than throwing. Promoting the previous version anyway left the tool with
     * two rows flagged latest, and getToolLatestByIdentifier matches nothing when two rows match, so
     * the lsid downloads shipped Skyline clients make stopped resolving for that tool.
     */
    @Test
    public void testADeleteLatestThatCannotRemoveTheFolderChangesNothing()
    {
        _containerHelper.createProject(BLOCKED_STORE, "Collaboration");
        _containerHelper.enableModule(BLOCKED_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

        uploadToolFileTo(BLOCKED_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                BLOCKED_TOOL_NAME, BLOCKED_TOOL_IDENTIFIER, "1.0"));
        int v1RowId = rowId(onlyToolInStore(BLOCKED_STORE));
        String v1FolderName = ToolStoreTestHelper.toolFolderName(BLOCKED_TOOL_NAME, "1.0");
        uploadToolFileTo("/" + BLOCKED_STORE + "/" + v1FolderName,
                ToolStoreTestHelper.writeMinimalToolZip(BLOCKED_TOOL_NAME, BLOCKED_TOOL_IDENTIFIER, "2.0"),
                v1RowId);

        int v2RowId = rowId(onlyToolInStore(BLOCKED_STORE));
        String v2FolderName = ToolStoreTestHelper.toolFolderName(BLOCKED_TOOL_NAME, "2.0");

        // The child folder is the whole point - it is what makes the delete impossible.
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
        // Two rows flagged latest show up here as two tools, since the catalog lists every latest row.
        assertEquals("The tool should still be listed once", 1, toolsInStore(BLOCKED_STORE));
        assertEquals("2.0 should still be the latest version",
                "2.0", onlyToolInStore(BLOCKED_STORE).getString("Version"));

        log("The same refusal reaches the person who clicked the menu item");
        goToProjectHome(BLOCKED_STORE);
        clickSprocketMenuItem("Delete latest version");
        // Not clickDialogOk - this click is refused, so no page follows it. The dialog is where the
        // reason has to appear. Reading it back as a success is the whole family of defects this
        // conversion closes, so assert the reason is on screen rather than that nothing happened.
        Locator.XPathLocator ok = Locator.xpath(
                "//div[contains(@class,'ui-dialog')][.//div[@id='delToolLatestDlg']]" +
                "//div[contains(@class,'ui-dialog-buttonpane')]//button[normalize-space()='Ok']");
        waitAndClick(ok.notHidden());

        // "nothing was changed" only appears on the pre-check refusal. The post-commit one also
        // says "could not be deleted", so that phrase alone would not pin which path ran.
        waitForElement(Locator.id("delToolLatestDlg").containing("nothing was changed"));
        assertEquals("2.0 should still be the latest version after the dialog was used",
                "2.0", onlyToolInStore(BLOCKED_STORE).getString("Version"));
    }

    /**
     * Clicks Ok on one dialog and waits for the page the action sends the caller to.
     *
     * Scoped to that dialog's own wrapper. A page holds several jQuery UI dialogs and the hidden
     * ones have an Ok button too.
     */
    private void clickDialogOk(String dialogId)
    {
        Locator.XPathLocator ok = Locator.xpath(
                "//div[contains(@class,'ui-dialog')][.//div[@id='" + dialogId + "']]" +
                "//div[contains(@class,'ui-dialog-buttonpane')]//button[normalize-space()='Ok']");
        waitForElement(ok.notHidden());
        clickAndWait(ok.notHidden());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Opens a tool's gear menu and clicks one of its items. The details page and the web part draw
     * the same menu, and a store page listing one tool has one of them.
     */
    private void clickSprocketMenuItem(String item)
    {
        click(Locator.css(".menuMouseArea.sprocket"));

        // The menu slides open, so the item is in the DOM before it is visible, and once a tool has
        // more than one version the menu is long enough to run past the bottom of the window.
        Locator.XPathLocator link = Locator.linkWithText(item);
        waitForElement(link.notHidden());

        // Wait the slide out before clicking. Part way through it the item is already reported as
        // visible while the menu around it is still clipped, and the click fails as not interactable.
        waitFor(() -> Boolean.TRUE.equals(executeScript(
                        "return !window.jQuery || jQuery('.dropMenu:animated').length === 0;")),
                "The gear menu was still sliding open", WAIT_FOR_JAVASCRIPT);

        scrollIntoView(link.notHidden());
        waitAndClick(link.notHidden());
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
     * The last block covers the URL rather than the menu. Hiding an item only removes the way in
     * that the UI offers, and UpdateToolAction used to draw its upload form for any version and
     * refuse at the post, which spends the owner's whole upload before telling them no.
     */
    @Test
    public void testAnOlderVersionPageOffersOnlyWhatItCanDo()
    {
        _containerHelper.createProject(OLDER_STORE, "Collaboration");
        _containerHelper.enableModule(OLDER_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

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
                sprocketHasItem("Upload new version"));
        assertTrue("The latest version is the one Delete latest version acts on, so it belongs here",
                sprocketHasItem("Delete latest version"));

        log("The older version's page offers only the supplementary file item");
        beginAt(WebTestHelper.buildURL("skyts", OLDER_STORE, "details",
                Map.of("name", OLDER_TOOL_NAME, "version", "1.0")));
        assertFalse("Upload new version must not be offered where the action would refuse it",
                sprocketHasItem("Upload new version"));
        assertFalse("Delete latest version must not be offered where it would act on another version",
                sprocketHasItem("Delete latest version"));
        assertTrue("Upload supplementary file works on any version and must stay",
                sprocketHasItem("Upload supplementary file"));

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
     * A tool's own folder shows that tool rather than an empty store.
     *
     * The module is enabled in every tool folder on skyline.ms, and a version folder has no children,
     * so a listing there has nothing to show. The folder holds exactly one tool row, so its store
     * page stands in for that tool's details page.
     */
    @Test
    public void testAToolsOwnFolderShowsThatTool()
    {
        _containerHelper.createProject(FOLDER_STORE, "Collaboration");
        _containerHelper.enableModule(FOLDER_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

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
        _containerHelper.createProject(NESTED_STORE, "Collaboration");
        _containerHelper.enableModule(NESTED_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

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
     * A folder sitting in a tool's file root must not block the next version from being published.
     *
     * A new version carries the previous version's supplementary files forward, and that list was
     * built from everything in the folder. A folder is not a file, the copy that carries them cannot
     * copy one, and the upload was refused with a message naming nothing - permanently, for that
     * tool. Only the name "docs" was excluded, so any other folder fell through.
     */
    @Test
    public void testAStrayFolderDoesNotBlockThenextVersion()
    {
        _containerHelper.createProject(STRAY_STORE, "Collaboration");
        _containerHelper.enableModule(STRAY_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

        uploadToolFileTo(STRAY_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                STRAY_TOOL_NAME, STRAY_TOOL_IDENTIFIER, "1.0"));
        int v1RowId = rowId(onlyToolInStore(STRAY_STORE));
        String v1Folder = "/" + STRAY_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(STRAY_TOOL_NAME, "1.0");

        // Made over WebDAV, which is how one really turns up - the Files web part writes here too.
        // Named something other than docs, which the list already excluded by name.
        int status = makeWebdavFolder(v1Folder, "extra_material");
        assertTrue("Could not create the stray folder over WebDAV, got HTTP " + status, status < 400);

        uploadToolFileTo(v1Folder, ToolStoreTestHelper.writeMinimalToolZip(
                STRAY_TOOL_NAME, STRAY_TOOL_IDENTIFIER, "2.0"), v1RowId);

        // Read from the catalog. A refused publish renders at 200, so the status proves nothing.
        assertEquals("A folder in the tool's file root blocked the next version from publishing",
                "2.0", onlyToolInStore(STRAY_STORE).getString("Version"));
    }

    /** Creates a folder inside a container's file root, as the current user. */
    private int makeWebdavFolder(String containerPath, String folderName)
    {
        String url = WebTestHelper.getBaseURL() + "/_webdav" + containerPath + "/@files/" + folderName;
        HttpUriRequestBase request = new HttpUriRequestBase("MKCOL", URI.create(url));
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
            throw new RuntimeException("Could not create " + url, e);
        }
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
        _containerHelper.createProject(CORRUPT_STORE, "Collaboration");
        _containerHelper.enableModule(CORRUPT_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

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

    /** Whether the details page gear menu carries an item, without clicking it. */
    private boolean sprocketHasItem(String item)
    {
        // Gated in the JSP, so an item that does not apply is absent from the page rather than
        // hidden by CSS. A DOM check needs no hover and cannot race the menu animation.
        return Locator.tagWithClass("ul", "dropMenu").append(Locator.linkWithText(item))
                .existsIn(getDriver());
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
     * Asserts the effect rather than the status. A refusal here renders as an error view with
     * status 200, so a status alone proves nothing.
     */
    @Test
    public void testAnUploadThatFailsPartWayLeavesNoFolderBehind()
    {
        _containerHelper.createProject(RETRY_STORE, "Collaboration");
        _containerHelper.enableModule(RETRY_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");

        // resetErrorMark is server wide, so consuming this upload's errors below consumes everything
        // logged since the last mark. checkErrors() runs after every test method, so nothing should
        // be pending here - assert that rather than let the reset hide an error silently.
        assertEquals("Server errors were already pending before this test",
                0, getServerErrorCount());

        // Its icon is not a decodable image, so storing the version throws in writeIconToFile, which
        // runs after the folder has been made.
        uploadToolFileTo(RETRY_STORE, ToolStoreTestHelper.writeToolZipWithUnreadableIcon(
                RETRY_TOOL_NAME, RETRY_TOOL_IDENTIFIER, RETRY_TOOL_VERSION));

        // createVersionFolder turns the failure into a message on the form, so nothing should reach the
        // log as a server error. writeIconToFile used to let an IllegalArgumentException out, which
        // is not the IOException it declares, so no catch saw it and the upload died as a 500.
        // Read and cleared before asserting, so a failure here leaves nothing pending for the next
        // test. resetErrors is server wide.
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
     * @param containerPath  the store folder for a new tool, or the TOOL's own folder for a new
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

    /**
     * Replacing a tool's icon rewrites both the stored icon.png and the icon entry inside the zip
     * Skyline downloads.
     *
     * The zip entry name used to be the client's filename, used raw, so a crafted one could land a
     * path outside tool-inf in a zip Skyline extracts. It also had to stay findable - getToolFromZip
     * picks the icon out by extension, so a real image named .txt would never be read back as one.
     *
     * The rewrite decides what to drop by extension, and a documentation image under tool-inf/docs/
     * has the same extension as an icon, so this checks the screenshot is still there afterwards.
     */
    @Test
    public void testReplacingAnIconRewritesTheFileAndTheZipEntry() throws Exception
    {
        byte[] uploaded = java.nio.file.Files.readAllBytes(
                TestFileUtils.getSampleData(REPLACEMENT_ICON).toPath());

        // Its own store and identifier - identifiers are unique server-wide, so sharing a tool with
        // another test class makes an upload fail as TOOL_ALREADY_EXISTS.
        _containerHelper.createProject(ICON_STORE, "Collaboration");
        _containerHelper.enableModule(ICON_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");
        uploadToolFileTo(ICON_STORE, ToolStoreTestHelper.writeToolZipWithIcon(
                ICON_TOOL_NAME, ICON_TOOL_IDENTIFIER, "1.0", ToolStoreTestHelper.solidPng(16)));

        JSONObject tool = onlyToolInStore(ICON_STORE);
        int toolId = rowId(tool);
        String folder = "/" + ICON_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(ICON_TOOL_NAME, "1.0");
        String iconUrl = tool.getString("IconUrl");

        byte[] before = getBytes(iconUrl);
        assertEquals("The icon the tool shipped should be the one stored", 16, iconWidth(before));

        log("A real image under a name the store cannot read back is refused");
        assertEquals("An icon named .txt has to be refused", 400,
                postIcon(folder, toolId, "logo.txt", uploaded));
        assertArrayEquals("A refused icon must not touch the stored one", before, getBytes(iconUrl));

        log("A legal name is accepted and replaces the stored icon");
        assertEquals("A .png icon should be accepted", 200,
                postIcon(folder, toolId, "replacement-icon.png", uploaded));
        assertEquals("The stored icon should now be the one just posted", 128, iconWidth(getBytes(iconUrl)));

        log("Documentation under tool-inf/docs is not an icon and is left alone");
        assertTrue("A documentation image has to survive an icon replacement",
                zipEntryNames(tool.getString("DownloadUrl"))
                        .contains(ToolStoreTestHelper.DOC_SCREENSHOT_ENTRY));

        log("A crafted name cannot put an entry outside tool-inf");
        assertEquals("A name with separators should be legalised, not refused", 200,
                postIcon(folder, toolId, "../../evil.png", uploaded));
        for (String entry : zipEntryNames(tool.getString("DownloadUrl")))
        {
            assertFalse("No zip entry may carry a path the client chose: " + entry,
                    entry.contains("../"));
            // Documentation is the one thing that nests under tool-inf, and real tools ship folders
            // of it, so the rule that an icon sits directly under tool-inf is checked on the rest.
            if (entry.toLowerCase().startsWith("tool-inf/") &&
                    !entry.toLowerCase().startsWith("tool-inf/docs/"))
                assertFalse("An icon entry belongs directly under tool-inf: " + entry,
                        entry.substring("tool-inf/".length()).contains("/"));
        }
    }

    /**
     * A supplementary file whose name has no extension must not take the store listing down.
     *
     * getSupplementaryFiles picks each file's tile icon from its extension, and
     * FileUtil.getExtension returns null for a name with no dot. The listing resolves the
     * supplementary files of every tool it draws, so one such file removed every tool from the
     * page rather than only its own.
     */
    @Test
    public void testASupplementaryFileWithNoExtensionStillLists()
    {
        _containerHelper.createProject(NO_EXT_STORE, "Collaboration");
        _containerHelper.enableModule(NO_EXT_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");
        uploadToolFileTo(NO_EXT_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                NO_EXT_TOOL_NAME, NO_EXT_TOOL_IDENTIFIER, "1.0"));

        JSONObject tool = onlyToolInStore(NO_EXT_STORE);
        int toolId = rowId(tool);
        String folder = "/" + NO_EXT_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(NO_EXT_TOOL_NAME, "1.0");

        assertEquals("A supplementary file with no extension should be accepted", 200,
                postSupplement(folder, toolId, "README"));

        log("The store listing still draws the tool");
        beginAt(WebTestHelper.buildURL("skyts", NO_EXT_STORE, "begin"));
        assertTextNotPresent("NullPointerException");
        assertTextPresent(NO_EXT_TOOL_NAME);

        log("So does the tool's own details page, which offers the file");
        beginAt(WebTestHelper.buildURL("skyts", NO_EXT_STORE, "details") + "?id=" + toolId);
        assertTextNotPresent("NullPointerException");
        assertElementPresent(Locator.id("download-tool-btn"));
        assertTextPresent("README");
    }

    /** Uploads one supplementary file through the action the dialogs post to. */
    private int postSupplement(String folderPath, int toolId, String fileName)
    {
        HttpPost request = new HttpPost(
                WebTestHelper.buildURL("skyts", folderPath, "insertSupplement"));
        request.setEntity(MultipartEntityBuilder.create()
                .addTextBody("toolId", String.valueOf(toolId))
                .addBinaryBody("suppFile", "notes".getBytes(), ContentType.DEFAULT_BINARY, fileName)
                .build());
        return execute(request);
    }

    /** Decodes a served icon far enough to tell which image it is. */
    private int iconWidth(byte[] png) throws Exception
    {
        java.awt.image.BufferedImage image =
                javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertTrue("The served icon should decode as an image", image != null);
        return image.getWidth();
    }

    /**
     * Manage tool owners names a group that holds a tool-owner role, and says the box does not
     * control it.
     *
     * The box lists and replaces users only. P55 stopped a group being stripped by a save, which was
     * right, but left the dialog showing an incomplete picture - an admin emptied it and believed
     * access was gone while the group still had it.
     */
    @Test
    public void testManageOwnersNamesAGroupThatHoldsAccess()
    {
        _containerHelper.createProject(GROUP_STORE, "Collaboration");
        _containerHelper.enableModule(GROUP_STORE, "SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");
        uploadToolFileTo(GROUP_STORE, ToolStoreTestHelper.writeMinimalToolZip(
                GROUP_TOOL_NAME, GROUP_TOOL_IDENTIFIER, "1.0"));

        String toolFolder = "/" + GROUP_STORE + "/" +
                ToolStoreTestHelper.toolFolderName(GROUP_TOOL_NAME, "1.0");

        log("Before the group holds anything, the dialog says nothing about one");
        beginAt(WebTestHelper.buildURL("skyts", GROUP_STORE, "details",
                Map.of("name", GROUP_TOOL_NAME)));
        assertTextNotPresent("Groups with access");

        // In GROUP_STORE, not the test's own project - a project group can only hold a role inside
        // the project that owns it, and the tool folder lives here.
        _permissionsHelper.createProjectGroup(OWNER_GROUP, GROUP_STORE);
        _permissionsHelper.addMemberToRole(OWNER_GROUP, "Editor",
                PermissionsHelper.MemberType.group, toolFolder);

        log("Now it is named, and the dialog says the box does not control it");
        beginAt(WebTestHelper.buildURL("skyts", GROUP_STORE, "details",
                Map.of("name", GROUP_TOOL_NAME)));
        assertTextPresent("Groups with access", OWNER_GROUP,
                "configured through the permissions UI");
    }

    /** Posts one image to updateProperty as the icon, under the given filename. */
    private int postIcon(String folderPath, int toolId, String fileName, byte[] image)
    {
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", folderPath, "updateProperty"));
        request.setEntity(MultipartEntityBuilder.create()
                .addTextBody("toolId", String.valueOf(toolId))
                .addBinaryBody("propValue", image, ContentType.create("image/png"), fileName)
                .build());
        return execute(request);
    }

    /** Reads a URL the catalog handed us, as the current user. */
    private byte[] getBytes(String url) throws Exception
    {
        // IconUrl and DownloadUrl already carry the context path, so the host alone goes in front.
        HttpGet request = new HttpGet(WebTestHelper.getBaseUrlWithoutContextPath() + url);
        APITestHelper.injectCookies(request);
        try (CloseableHttpClient client = WebTestHelper.getHttpClient())
        {
            return client.execute(request, r -> EntityUtils.toByteArray(r.getEntity()));
        }
    }

    private java.util.List<String> zipEntryNames(String downloadUrl) throws Exception
    {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (java.util.zip.ZipInputStream in =
                     new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(getBytes(downloadUrl))))
        {
            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextEntry()) != null)
                names.add(entry.getName());
        }
        return names;
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
        _containerHelper.deleteProject(WEBPART_STORE, false);
        _containerHelper.deleteProject(BLOCKED_STORE, false);
        _containerHelper.deleteProject(STRAY_STORE, false);
        _containerHelper.deleteProject(RETRY_STORE, false);
        _containerHelper.deleteProject(OLDER_STORE, false);
        _containerHelper.deleteProject(ICON_STORE, false);
        _containerHelper.deleteProject(NO_EXT_STORE, false);
        _containerHelper.deleteProject(GROUP_STORE, false);
        _containerHelper.deleteProject(FOLDER_STORE, false);
        _containerHelper.deleteProject(NESTED_STORE, false);
        _containerHelper.deleteProject(CORRUPT_STORE, false);
        _userHelper.deleteUsers(false, TOOL_AUTHOR, TOOL_SECOND_OWNER);
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
