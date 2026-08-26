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
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Authorization tests for the Skyline Tool Store.
 *
 * These use raw HTTP rather than the browser because the UI does not render the controls that reach
 * these actions for a user who lacks the permission.
 */
@Category({External.class, MacCossLabModules.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class ToolStoreSecurityTest extends BaseWebDriverTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "ToolStoreSecurityTest";

    // Impersonated as the non-admin caller, and named as the new owner in the setOwners refusals. It
    // has to be a real account. A made-up address is refused as an unknown user before the checks
    // these tests are about, so a hole that opened would still look green. It holds no role anywhere,
    // so finding it on a folder means an exploit.
    private static final String ATTACKER = "toolstore_attacker@toolstore.test";

    // Deliberately never created. It is only the credentials for a client whose sign-in fails, which
    // is how a request reaches the server with no session.
    private static final String NO_SESSION_USER = "toolstore_nosession@toolstore.test";

    // Holds Editor on the store folder, so it has InsertPermission there but is not a site admin.
    // It is the account that tells the site-admin rule apart from a plain InsertPermission check.
    private static final String CONTRIBUTOR = "toolstore_contributor@toolstore.test";

    // The tool this class adds to the store in doSetup.
    private static final String TOOL = "skylinetoolsstore/user1-v1.zip";
    // A different tool, so uploading it would add a new identifier to the catalog.
    private static final String TOOL_OTHER = "skylinetoolsstore/user2-v1.zip";

    // A folder with no tool in it, for posting at an action from somewhere unrelated to the tool.
    private static final String UNRELATED_FOLDER = "unrelated";
    private static final String UNRELATED_PATH = "/" + PROJECT_NAME + "/" + UNRELATED_FOLDER;

    // The shared tool's identity, read once in doSetup. Nothing here publishes a new version onto
    // it, so every test acts on the same version whatever order they run in. The catalog is server
    // wide, so the LSID is what picks our tool out of it.
    private static String _toolIdentifier;
    private static String _toolName;
    private static String _toolFolderPath;
    private static int _toolRowId;

    private final ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    public static void setupProject()
    {
        ToolStoreSecurityTest init = getCurrentTest();
        init.doSetup();
    }

    @LogMethod
    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.enableModule("SkylineToolsStore");
        new PortalHelper(this).addWebPart("Skyline Tool Store");
        _containerHelper.createSubfolder(PROJECT_NAME, UNRELATED_FOLDER);

        _userHelper.createUser(ATTACKER);
        _userHelper.createUser(CONTRIBUTOR);
        _permissionsHelper.addMemberToRole(CONTRIBUTOR, "Editor", PermissionsHelper.MemberType.user,
                "/" + PROJECT_NAME);

        // Tool identifiers are server-global and the test classes here share sample data, so clear
        // any tool left behind by another class or an earlier run.
        ToolStoreTestHelper.removeToolsFromCatalog(PROJECT_NAME,
                TestFileUtils.getSampleData(TOOL), TestFileUtils.getSampleData(TOOL_OTHER));

        Set<String> before = catalogIdentifiers();
        uploadTool(TOOL);
        Set<String> added = catalogIdentifiers();
        added.removeAll(before);
        assertEquals("Upload should have added exactly one tool identifier, got " + added,
                1, added.size());
        _toolIdentifier = added.iterator().next();

        JSONObject tool = getOurTool();
        _toolName = tool.getString("Name");
        _toolFolderPath = toolFolderPath(_toolName, tool.getString("Version"));
        _toolRowId = extractRowIdFromDownloadUrl(tool.getString("DownloadUrl"));

        // A wrong folder path would make the security assertions pass vacuously.
        assertTrue("Expected the upload to create tool folder " + _toolFolderPath,
                _containerHelper.doesContainerExist(_toolFolderPath));
        assertFalse(ATTACKER + " must start with no role on the tool folder",
                hasEditorRole(_toolFolderPath, ATTACKER));
    }

    // -------------------------------------------------------------------------
    // SetOwnersAction - only site admin can change tool owners
    // -------------------------------------------------------------------------

    /** setOwners must reject a request carrying no session and no CSRF token. */
    @Test
    public void testSetOwnersRejectsAnonymousRequest()
    {
        int status = post("setOwners", ownerParams(ATTACKER), false, false);

        assertFalse("SECURITY: an unauthenticated setOwners request granted " + ATTACKER +
                        " the Editor role on " + _toolFolderPath + " (HTTP " + status + ")",
                hasEditorRole(_toolFolderPath, ATTACKER));
        assertRefusalStatus("An anonymous setOwners", 401, status);
    }

    /** setOwners must reject a signed-in non-site admin. */
    @Test
    public void testSetOwnersRejectsNonAdminUser()
    {
        List<NameValuePair> params = ownerParams(ATTACKER);
        int status;
        impersonate(ATTACKER);
        try
        {
            status = post("setOwners", params, true, true);
        }
        finally
        {
            stopImpersonating();
        }

        assertFalse("SECURITY: a non-site admin setOwners request granted " + ATTACKER +
                        " the Editor role on " + _toolFolderPath + " (HTTP " + status + ")",
                hasEditorRole(_toolFolderPath, ATTACKER));
        assertRefusalStatus("setOwners as a signed-in non-site admin", 403, status);
    }

    /**
     * setOwners must refuse a request addressed to a folder that is not the tool's folder or the parent
     * tool store folder. Sent as site admin with a valid token, so only that check can refuse it.
     * See requireToolAddressableFrom.
     */
    @Test
    public void testSetOwnersRefusesARequestAddressedElsewhere()
    {
        Reply reply = postToForReply(UNRELATED_PATH, "setOwners", ownerParams(ATTACKER), true, true);

        assertFalse("setOwners acted on a tool the request was not addressed to (HTTP " +
                        reply.status() + ")",
                hasEditorRole(_toolFolderPath, ATTACKER));
        assertNotFoundNaming("setOwners", _toolName, reply);
    }

    private List<NameValuePair> ownerParams(String newOwner)
    {
        return List.of(new BasicNameValuePair("toolId", String.valueOf(_toolRowId)),
                new BasicNameValuePair("toolOwners", newOwner));
    }

    // -------------------------------------------------------------------------
    // InsertToolAction - new tools can be added by site admin only
    // -------------------------------------------------------------------------

    /**
     * The web part offers Add New Tool to site admins only, so the action must enforce it too.
     * CONTRIBUTOR holds Editor here, so only the site-admin rule refuses it.
     */
    @Test
    public void testInsertNewToolRejectsNonSiteAdmin()
    {
        Set<String> before = catalogIdentifiers();
        int status;
        impersonate(CONTRIBUTOR);
        try
        {
            status = uploadToolExpectingRefusal(TOOL_OTHER);
        }
        finally
        {
            stopImpersonating();
        }

        assertEquals("SECURITY: a folder Editor who is not a site admin added a tool to the store",
                before, catalogIdentifiers());
        // @RequiresSiteAdmin on InsertToolAction means the framework rejects this before the action
        // body runs, so the refusal is a status rather than a message rendered into the form.
        assertTrue("Expected the site-admin refusal, got HTTP " + status, status >= 400);
    }

    /**
     * The refusal must come before the owner list is resolved, or the "unknown users" reply tells
     * any logged-in caller whether an email is a registered account. handlePost parses the owners
     * first and only then reads the zip, so the enumeration is one line away from a caller the
     * framework does not turn back.
     *
     * The form echoes the submitted value back into its input, so the test looks for that message
     * rather than for the address.
     */
    @Test
    public void testInsertDoesNotRevealWhetherAccountsExist()
    {
        // Must match SkylineToolsStoreController.UNKNOWN_USERS, which is not on this source set's
        // classpath. The control at the end is what catches a rewording.
        final String unknownUsersMessage = "The following users are unknown";
        String unknown = "definitely_not_a_user_" + System.nanoTime() + "@toolstore.test";

        Reply asNonAdmin;
        impersonate(ATTACKER);
        try
        {
            asNonAdmin = postOwnersOnly(unknown);
        }
        finally
        {
            stopImpersonating();
        }

        assertTrue("Expected the site-admin refusal, got HTTP " + asNonAdmin.status(),
                asNonAdmin.status() >= 400);
        assertFalse("SECURITY: the reply reported which accounts are unknown, which lets any " +
                        "logged-in user test whether an address is registered",
                asNonAdmin.body().contains(unknownUsersMessage));

        // The control. Asserting only that a phrase is absent passes just as well when the phrase
        // has been reworded or the path is unreachable, so prove the same post does report it to
        // someone allowed to get that far.
        Reply asAdmin = postOwnersOnly(unknown);
        assertTrue("The unknown-users message no longer appears even for a site admin, so the"
                + " assertion above proves nothing. Body was " + asAdmin.body(),
                asAdmin.body().contains(unknownUsersMessage));
    }

    // -------------------------------------------------------------------------
    // CSRF - a mutating post without the CSRF header must be refused
    // -------------------------------------------------------------------------

    /**
     * A session cookie without the CSRF header is what a cross-site form post looks like. CSRF is a
     * double submit, so the token must arrive as a parameter or header and match the cookie.
     */
    @Test
    public void testUpdatePropertyRequiresCsrfToken()
    {
        String originalDescription = getOurTool().getString("Description");
        String forged = "forged-by-csrf-" + System.nanoTime();

        int status = postTo(_toolFolderPath, "updateProperty", descriptionParams(forged), true, false);

        assertEquals("SECURITY: updateProperty accepted a POST with no CSRF token (HTTP " + status + ")",
                originalDescription, getOurTool().getString("Description"));
        // A CSRF failure counts as unauthenticated whoever sent it, so 401 rather than 403.
        assertRefusalStatus("updateProperty with no CSRF token", 401, status);
    }

    /**
     * The positive control - with a valid token the edit must work, so CSRF enforcement cannot be
     * satisfied by breaking the feature. Also covers SkylineToolDetails.jsp, which attaches the
     * token itself.
     */
    @Test
    public void testUpdatePropertySucceedsWithCsrfToken()
    {
        String newDescription = "edited-with-token-" + System.nanoTime();

        int status = postTo(_toolFolderPath, "updateProperty", descriptionParams(newDescription), true, true);

        assertTrue("An authenticated updateProperty with a CSRF token should succeed, got HTTP " + status,
                status < 400);
        assertEquals("Description should have been updated",
                newDescription, getOurTool().getString("Description"));
    }

    private List<NameValuePair> descriptionParams(String description)
    {
        return List.of(new BasicNameValuePair("toolId", String.valueOf(_toolRowId)),
                new BasicNameValuePair("propName", "Description"),
                new BasicNameValuePair("propValue", description));
    }

    // -------------------------------------------------------------------------
    // Error hygiene - a bad request must not surface as a server fault
    // -------------------------------------------------------------------------

    /**
     * A request naming a tool that does not exist is an ordinary bad request, so each action has to
     * return 404 rather than a server error that reaches the log and mothership.
     */
    @Test
    public void testMissingToolReturnsNotFoundNotServerError()
    {
        int absentId = 99999999;

        // insertSupplement is a FormViewAction, so its 404 is an error page rather than JSON and
        // only the status is worth asserting. The three below are MutatingApiActions.
        assertEquals("insertSupplement with an unknown tool id should be a 404",
                404, post("insertSupplement",
                        List.of(new BasicNameValuePair("toolId", String.valueOf(absentId))), true, true));

        assertNotFoundNaming("deleteSupplement", String.valueOf(absentId),
                postToForReply(_toolFolderPath, "deleteSupplement",
                        List.of(new BasicNameValuePair("toolId", String.valueOf(absentId)),
                                new BasicNameValuePair("suppFile", "whatever.pdf")), true, true));

        assertNotFoundNaming("updateProperty", String.valueOf(absentId),
                postToForReply(_toolFolderPath, "updateProperty",
                        List.of(new BasicNameValuePair("toolId", String.valueOf(absentId)),
                                new BasicNameValuePair("propName", "Description"),
                                new BasicNameValuePair("propValue", "x")), true, true));

        assertNotFoundNaming("deleteLatest", String.valueOf(absentId),
                postToForReply(PROJECT_NAME, "deleteLatest",
                        List.of(new BasicNameValuePair("toolId", String.valueOf(absentId))), true, true));
    }

    /** Asking for a supplementary file that is not one must not be a server fault either. */
    @Test
    public void testDeletingSomethingThatIsNotASupplementaryFileIsNotFound()
    {
        assertNotFoundNaming("deleteSupplement", "no-such-file.pdf",
                postToForReply(_toolFolderPath, "deleteSupplement",
                        List.of(new BasicNameValuePair("toolId", String.valueOf(_toolRowId)),
                                new BasicNameValuePair("suppFile", "no-such-file.pdf")), true, true));

        assertNotFoundNaming("deleteSupplement", "icon.png",
                postToForReply(_toolFolderPath, "deleteSupplement",
                        List.of(new BasicNameValuePair("toolId", String.valueOf(_toolRowId)),
                                new BasicNameValuePair("suppFile", "icon.png")), true, true));
    }

    // -------------------------------------------------------------------------
    // DeleteLatestAction - a container delete must not be reachable by GET
    // -------------------------------------------------------------------------

    /**
     * deleteLatest deletes a container, so it must not be reachable by GET, which an img tag can
     * forge and no CSRF token can protect.
     */
    @Test
    public void testDeleteLatestRejectsGetRequest()
    {
        // Its own tool, so the second version published here does not move the shared one.
        String name = "GetDeleteLatestProbe";
        String identifier = "URN:LSID:toolstore.test:getdeletelatest";
        File v1 = ToolStoreTestHelper.writeMinimalToolZip(name, identifier, "1.0");
        File v2 = ToolStoreTestHelper.writeMinimalToolZip(name, identifier, "2.0");
        ToolStoreTestHelper.removeToolsFromCatalog(PROJECT_NAME, v1);

        uploadTool(v1);
        String v1FolderPath = toolFolderPath(name, "1.0");
        int v1RowId = extractRowIdFromDownloadUrl(toolInCatalog(identifier).getString("DownloadUrl"));
        uploadNewVersionTo(v1FolderPath, v2, v1RowId);

        JSONObject latest = toolInCatalog(identifier);
        // The upload returns 200 even on failure, so confirm the version actually moved.
        assertNotEquals("The second upload should have become the latest version",
                "1.0", latest.getString("Version"));
        int latestRowId = extractRowIdFromDownloadUrl(latest.getString("DownloadUrl"));
        String latestFolderPath = toolFolderPath(name, latest.getString("Version"));

        assertTrue("The second version folder should exist before the GET",
                _containerHelper.doesContainerExist(latestFolderPath));

        // Address the tool's own version folder with the row id the form binds, so a GET that was
        // accepted would really delete the version and the assertions below would catch it.
        String url = WebTestHelper.buildURL("skyts", latestFolderPath, "deleteLatest") + "?toolId=" + latestRowId;
        int status = execute(new HttpGet(url), true, true);

        assertEquals("A GET to deleteLatest has to be refused as a method that is not allowed",
                405, status);
        assertTrue("SECURITY: a GET to deleteLatest deleted tool folder " + latestFolderPath +
                        " (HTTP " + status + ")",
                _containerHelper.doesContainerExist(latestFolderPath));
        assertTrue("The original version must also survive",
                _containerHelper.doesContainerExist(v1FolderPath));
    }

    // -------------------------------------------------------------------------
    // UpdateToolAction - a new version must come from the latest one
    // -------------------------------------------------------------------------

    /**
     * Publishing demotes the version it supersedes, so publishing from an older version would leave
     * the real latest flagged as well. The tool would be listed twice and the new version would
     * inherit the older one's owners and files.
     */
    @Test
    public void testPublishingFromAnOldVersionIsRefused()
    {
        String name = "StaleParentPublishProbe";
        String identifier = "URN:LSID:toolstore.test:staleparent";
        File v1 = ToolStoreTestHelper.writeMinimalToolZip(name, identifier, "1.0");
        File v2 = ToolStoreTestHelper.writeMinimalToolZip(name, identifier, "2.0");
        File v3 = ToolStoreTestHelper.writeMinimalToolZip(name, identifier, "3.0");
        ToolStoreTestHelper.removeToolsFromCatalog(PROJECT_NAME, v1);

        uploadToolOwnedBy(v1, CONTRIBUTOR);
        int v1RowId = extractRowIdFromDownloadUrl(toolInCatalog(identifier).getString("DownloadUrl"));
        uploadNewVersionTo(toolFolderPath(name, "1.0"), v2, v1RowId);
        assertEquals("2.0 should be the latest before the probe",
                "2.0", toolInCatalog(identifier).getString("Version"));

        // Publish 3.0 naming 1.0 as its parent, which is what the details page of an old version does.
        HttpPost request = new HttpPost(
                WebTestHelper.buildURL("skyts", toolFolderPath(name, "1.0"), "updateTool"));
        request.setEntity(MultipartEntityBuilder.create()
                .addBinaryBody("toolZip", v3, ContentType.create("application/zip"), v3.getName())
                .addTextBody("toolId", String.valueOf(v1RowId))
                .build());
        int status = execute(request, true, true);

        // updateTool is a FormViewAction, so a refusal renders an error view at 200 and the status
        // cannot tell a refusal from a success. It can still tell either from a server error.
        assertTrue("A refused publish must not be a server error, HTTP " + status, status < 500);

        // The catalog holds one row per tool only if exactly one row is flagged latest. Two would
        // list the tool twice.
        assertEquals("Publishing from 1.0 must not add a second latest row",
                1, catalogEntriesFor(identifier));
        assertEquals("2.0 must still be the latest version",
                "2.0", toolInCatalog(identifier).getString("Version"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Adds a new tool to the store folder as the site admin, naming who owns it. */
    private void uploadToolOwnedBy(File zip, String owners)
    {
        postInsertTool(zip, owners);
    }

    /** Adds a tool this test built, rather than one from sample data. */
    private void uploadTool(File zip)
    {
        postInsertTool(zip, null);
    }

    private void postInsertTool(File zip, String owners)
    {
        MultipartEntityBuilder entity = MultipartEntityBuilder.create()
                .addBinaryBody("toolZip", zip, ContentType.create("application/zip"), zip.getName());
        if (owners != null)
            entity.addTextBody("toolOwners", owners);

        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", PROJECT_NAME, "insertTool"));
        request.setEntity(entity.build());
        int status = execute(request, true, true);
        assertTrue("Adding " + zip.getName() + " failed, HTTP " + status, status < 400);
    }

    /** Publishes a new version, which is addressed to the tool's own folder. */
    private void uploadNewVersionTo(String toolFolderPath, File zip, int toolId)
    {
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", toolFolderPath, "updateTool"));
        request.setEntity(MultipartEntityBuilder.create()
                .addBinaryBody("toolZip", zip, ContentType.create("application/zip"), zip.getName())
                .addTextBody("toolId", String.valueOf(toolId))
                .build());
        int status = execute(request, true, true);
        assertTrue("Publishing " + zip.getName() + " failed, HTTP " + status, status < 400);
    }

    /** The catalog is server wide, so a tool has to be picked out by its identifier. */
    private JSONObject toolInCatalog(String identifier)
    {
        JSONArray tools = getToolsFromApi();
        for (int i = 0; i < tools.length(); i++)
        {
            JSONObject tool = tools.getJSONObject(i);
            if (identifier.equals(tool.optString("Identifier")))
                return tool;
        }
        throw new AssertionError("No tool with identifier " + identifier + " in the catalog");
    }

    /** How many rows the catalog holds for a tool. More than one means two rows are flagged latest. */
    private int catalogEntriesFor(String identifier)
    {
        JSONArray tools = getToolsFromApi();
        int count = 0;
        for (int i = 0; i < tools.length(); i++)
        {
            if (identifier.equals(tools.getJSONObject(i).optString("Identifier")))
                count++;
        }
        return count;
    }

    /**
     * Adds a brand-new tool from sample data through insertTool as the site admin. A rejected upload
     * re-renders the form at 200, so callers verify the effect rather than the status.
     */
    @LogMethod
    private void uploadTool(String sampleDataRelativePath)
    {
        File zip = TestFileUtils.getSampleData(sampleDataRelativePath);
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", PROJECT_NAME, "insertTool"));
        request.setEntity(MultipartEntityBuilder.create()
                .addBinaryBody("toolZip", zip, ContentType.create("application/zip"), zip.getName())
                .build());

        int status = execute(request, true, true);
        assertTrue("Tool upload failed for " + zip.getName() + ", HTTP " + status, status < 400);
    }

    /** Attempts to add a new tool as the current (possibly impersonated) user, returning the status. */
    private int uploadToolExpectingRefusal(String sampleDataRelativePath)
    {
        File zip = TestFileUtils.getSampleData(sampleDataRelativePath);
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", PROJECT_NAME, "insertTool"));
        request.setEntity(MultipartEntityBuilder.create()
                .addBinaryBody("toolZip", zip, ContentType.create("application/zip"), zip.getName())
                .build());
        return execute(request, true, true);
    }

    /** Posts only a toolOwners value, with no zip, and returns the status and body. */
    private Reply postOwnersOnly(String owner)
    {
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", PROJECT_NAME, "insertTool"));
        request.setEntity(MultipartEntityBuilder.create()
                .addTextBody("toolOwners", owner)
                .build());
        return executeForReply(request, true, true);
    }

    private int post(String action, List<NameValuePair> params, boolean withSession, boolean withCsrfToken)
    {
        return postTo(PROJECT_NAME, action, params, withSession, withCsrfToken);
    }

    /**
     * Actions on a single tool are addressed to that tool's own container rather than the store, so
     * their permission annotation checks the folder holding the tool. Pass _toolFolderPath there.
     */
    private int postTo(String containerPath, String action, List<NameValuePair> params,
                       boolean withSession, boolean withCsrfToken)
    {
        return postToForReply(containerPath, action, params, withSession, withCsrfToken).status();
    }

    /** Like postTo, but keeps the reply body. Only the MutatingApiActions respond with JSON. */
    private Reply postToForReply(String containerPath, String action, List<NameValuePair> params,
                                 boolean withSession, boolean withCsrfToken)
    {
        HttpPost request = new HttpPost(WebTestHelper.buildURL("skyts", containerPath, action));
        request.setEntity(new UrlEncodedFormEntity(new ArrayList<>(params)));
        return executeForReply(request, withSession, withCsrfToken);
    }

    /**
     * @param withSession   send the site-admin session. False produces a fully anonymous request.
     * @param withCsrfToken send the X-LABKEY-CSRF header. The cookie alone is not enough, so false
     *                      simulates a cross-site post.
     */
    private int execute(HttpUriRequest request, boolean withSession, boolean withCsrfToken)
    {
        return executeForReply(request, withSession, withCsrfToken).status();
    }

    /** A reply's status and body. The body is empty for a response that carries none. */
    private record Reply(int status, String body) {}

    private Reply executeForReply(HttpUriRequest request, boolean withSession, boolean withCsrfToken)
    {
        if (withSession && withCsrfToken)
            APITestHelper.injectCookies(request);
        else if (withSession)
            injectSessionOnly(request);

        try (CloseableHttpClient client = withSession
                ? WebTestHelper.getHttpClient()
                : WebTestHelper.getHttpClientBuilder(NO_SESSION_USER, "").build())
        {
            return client.execute(request, response -> {
                HttpEntity entity = response.getEntity();
                String body = entity == null ? "" : EntityUtils.toString(entity);
                return new Reply(response.getCode(), body);
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException("Request failed: " + request.getRequestUri(), e);
        }
    }

    /** Sends the session but not the CSRF header. injectCookies sets both. */
    private void injectSessionOnly(HttpUriRequest request)
    {
        org.openqa.selenium.Cookie session =
                WebTestHelper.getCookies(org.labkey.test.util.PasswordUtil.getUsername())
                        .get(org.labkey.remoteapi.Connection.JSESSIONID);
        assertTrue("No saved session for the primary test user", session != null);
        request.setHeader(session.getName(), session.getValue());
    }

    /**
     * Asserts the status a refused request has to carry.
     *
     * These tests lead with an absence - no role granted, no value changed - and an action that no
     * longer exists satisfies every one of those. Pinning the status separates a real refusal from
     * the 404 a renamed or moved action returns.
     */
    private void assertRefusalStatus(String what, int expectedStatus, int status)
    {
        assertEquals(what + " should have been refused", expectedStatus, status);
    }

    /**
     * Asserts a 404 whose reply names what the request was about - the id it gave, the file it named,
     * the tool it meant. Matching the name rather than the sentence keeps this from passing vacuously
     * if the wording changes.
     */
    private void assertNotFoundNaming(String action, String expectedInBody, Reply reply)
    {
        assertEquals(action + " should return 404, body was " + reply.body(), 404, reply.status());
        assertTrue(action + " should name " + expectedInBody + " in its reply, got " + reply.body(),
                reply.body().contains(expectedInBody));
    }

    /** Reads the public JSON API. Returns the latest version of every tool in the store. */
    private JSONArray getToolsFromApi()
    {
        return ToolStoreTestHelper.toolsFromApi(PROJECT_NAME);
    }

    /**
     * Reads the shared tool out of the catalog. Its identity is held in fields, so this is for the
     * properties that change during the run. Description is the only one.
     */
    private JSONObject getOurTool()
    {
        JSONArray tools = getToolsFromApi();
        for (int i = 0; i < tools.length(); i++)
        {
            JSONObject tool = tools.getJSONObject(i);
            if (_toolIdentifier.equals(tool.optString("Identifier")))
                return tool;
        }
        throw new AssertionError("Tool " + _toolIdentifier + " is not in the catalog");
    }

    private Set<String> catalogIdentifiers()
    {
        return ToolStoreTestHelper.catalogIdentifiers(PROJECT_NAME);
    }

    /** DownloadUrl looks like .../skyts-downloadTool.view?id=123 */
    private int extractRowIdFromDownloadUrl(String downloadUrl)
    {
        return ToolStoreTestHelper.rowId(new JSONObject().put("DownloadUrl", downloadUrl));
    }

    private String toolFolderPath(String toolName, String version)
    {
        return "/" + PROJECT_NAME + "/" + ToolStoreTestHelper.toolFolderName(toolName, version);
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
        _userHelper.deleteUsers(false, ATTACKER, CONTRIBUTOR);
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
