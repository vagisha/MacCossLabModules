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

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.util.FileUtil;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.APITestHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Catalog helpers shared by the tool store tests.
 */
public class ToolStoreTestHelper
{
    private ToolStoreTestHelper()
    {
    }

    /** The catalog. Global by design, so the given container path does not matter. */
    public static JSONArray toolsFromApi(String containerPath)
    {
        try (CloseableHttpClient client = WebTestHelper.getHttpClient())
        {
            HttpGet request = new HttpGet(WebTestHelper.buildURL("skyts", containerPath, "getToolsApi"));
            APITestHelper.injectCookies(request);
            String body = client.execute(request, r -> EntityUtils.toString(r.getEntity())).trim();
            // Empty for an empty store, a bare object for one tool, an array otherwise.
            if (body.isEmpty())
                return new JSONArray();
            return body.startsWith("[") ? new JSONArray(body) : new JSONArray().put(new JSONObject(body));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to read getToolsApi", e);
        }
    }

    public static Set<String> catalogIdentifiers(String containerPath)
    {
        JSONArray tools = toolsFromApi(containerPath);
        Set<String> identifiers = new HashSet<>();
        for (int i = 0; i < tools.length(); i++)
            identifiers.add(tools.getJSONObject(i).optString("Identifier"));
        return identifiers;
    }

    /** DownloadUrl looks like /&lt;store container&gt;/skyts-downloadTool.view?id=N */
    public static int rowId(JSONObject tool)
    {
        String url = tool.getString("DownloadUrl");
        int idx = url.indexOf("id=");
        assertTrue("DownloadUrl should carry an id parameter: " + url, idx >= 0);
        String tail = url.substring(idx + 3);
        int amp = tail.indexOf('&');
        return Integer.parseInt(amp >= 0 ? tail.substring(0, amp) : tail);
    }

    public static String storeContainerOf(JSONObject tool)
    {
        String url = tool.getString("DownloadUrl");
        String path = url.substring(0, url.indexOf("/skyts-"));

        // Remove context path if it is part of the URL.
        String contextPath = WebTestHelper.getContextPath();
        if (!contextPath.isEmpty() && (path.equals(contextPath) || path.startsWith(contextPath + "/")))
            path = path.substring(contextPath.length());

        return StringUtils.strip(path, "/");
    }

    /** Mirrors SkylineToolsStoreController.toolFolderName, which cuts the tool name at its last dot. */
    public static String toolFolderName(String toolName, String version)
    {
        int lastDot = toolName.lastIndexOf('.');
        return "_tool_" + (lastDot >= 0 ? toolName.substring(0, lastDot) : toolName) + "_" + version;
    }

    /**
     * A tool zip holding nothing but tool-inf/info.properties. Identifiers are server wide, so each
     * caller needs its own name and an identifier under TEST_IDENTIFIER_PREFIX.
     */
    public static File writeMinimalToolZip(String name, String identifier, String version)
    {
        return writeToolZip(name, identifier, version, "");
    }

    /**
     * A tool zip carrying extra info.properties lines beyond the three required ones. For a caller
     * that needs the details page to have something to render.
     */
    public static File writeToolZip(String name, String identifier, String version, String extraProperties)
    {
        try
        {
            // Short prefix - ZipName is 50 characters and createTempFile appends up to 19 digits.
            File zip = newFixtureZip("ts-" + version + "-");
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip)))
            {
                out.putNextEntry(new ZipEntry("tool-inf/info.properties"));
                out.write(("Name = " + name + "\n" +
                           "Version = " + version + "\n" +
                           "Identifier = " + identifier + "\n" +
                           extraProperties).getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            return zip;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not build the test tool zip", e);
        }
    }

    /**
     * Builds a tool zip truncated in the middle of its only entry. The entry header is intact, so
     * the zip opens and the failure comes while reading the entry. Random bytes would be rejected
     * before any read.
     */
    public static File writeTruncatedToolZip(String name, String identifier, String version)
    {
        try
        {
            byte[] full = Files.readAllBytes(writeMinimalToolZip(name, identifier, version).toPath());
            // A local header is 30 bytes plus the entry name and extra field, whose lengths are held
            // at offsets 26 and 28. Keeping a little past it leaves the compressed data unfinished.
            int nameLength = (full[26] & 0xFF) | ((full[27] & 0xFF) << 8);
            int extraLength = (full[28] & 0xFF) | ((full[29] & 0xFF) << 8);
            int keep = Math.min(30 + nameLength + extraLength + 8, full.length);

            File zip = newFixtureZip("ts-cut-" + version + "-");
            Files.write(zip.toPath(), Arrays.copyOf(full, keep));
            return zip;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not build the truncated test tool zip", e);
        }
    }

    /**
     * Builds a tool zip whose tool-inf icon is not a real image. The controller picks the icon by
     * extension and stores it without decoding, so the upload fails in writeIconToFile after the
     * version folder exists.
     */
    public static File writeToolZipWithUnreadableIcon(String name, String identifier, String version)
    {
        try
        {
            File zip = newFixtureZip("ts-badicon-" + version + "-");
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip)))
            {
                out.putNextEntry(new ZipEntry("tool-inf/info.properties"));
                out.write(("Name = " + name + "\n" +
                           "Version = " + version + "\n" +
                           "Identifier = " + identifier + "\n").getBytes(StandardCharsets.UTF_8));
                out.closeEntry();

                out.putNextEntry(new ZipEntry("tool-inf/icon.png"));
                out.write("This is not an image.".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            return zip;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not build the test tool zip", e);
        }
    }

    /** Reads the Identifier out of tool-inf/info.properties inside a tool zip. */
    public static String identifierOf(File zip)
    {
        try (ZipFile zf = new ZipFile(zip))
        {
            ZipEntry entry = zf.getEntry("tool-inf/info.properties");
            assertNotNull("No tool-inf/info.properties in " + zip.getName(), entry);
            Properties props = new Properties();
            try (InputStream in = zf.getInputStream(entry))
            {
                props.load(in);
            }
            String identifier = props.getProperty("Identifier");
            assertNotNull("No Identifier in " + zip.getName(), identifier);
            return identifier.trim();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not read the identifier from " + zip, e);
        }
    }

    /**
     * Fixture zips go under build/testTemp, which the build cleans and a failed run leaves in
     * place. FileUtil.createTempFile rather than File.createTempFile, per its own note.
     */
    private static File newFixtureZip(String prefix) throws IOException
    {
        return FileUtil.createTempFile(prefix, ".zip",
                TestFileUtils.ensureTestTempDir("skylinetoolsstore"));
    }

    /** The LSID namespace every sample tool zip in this module uses. */
    private static final String TEST_IDENTIFIER_PREFIX = "URN:LSID:toolstore.test:";

    /**
     * Deletes tools in the given project whose identifier matches one of the given zips, so a test
     * starts from a known state. Runs as the current user, who must be a site admin.
     */
    public static void removeToolsFromCatalog(String containerPath, File... zips)
    {
        // getToolsApi is deliberately not container scoped, so the loop below sees every tool on the
        // server, and DeleteAction removes the container of every version of whatever it matches.
        // The only thing separating a fixture from a real tool is the identifier, so refuse to run
        // at all against a zip from outside the test namespace rather than deleting someone's tool.
        // Do not try to tell them apart by folder name - a real store folder can be called anything.
        Set<String> toRemove = new HashSet<>();
        for (File zip : zips)
        {
            String identifier = identifierOf(zip);
            assertTrue("Refusing to clean up " + zip.getName() + ". Its identifier " + identifier +
                            " is outside " + TEST_IDENTIFIER_PREFIX + ". Since this method deletes every " +
                            "version folder of the tool, wherever it lives, it must only run on test fixtures.",
                    identifier.startsWith(TEST_IDENTIFIER_PREFIX));
            toRemove.add(identifier);
        }

        JSONArray tools = toolsFromApi(containerPath);
        for (int i = 0; i < tools.length(); i++)
        {
            JSONObject tool = tools.getJSONObject(i);
            if (!toRemove.contains(tool.optString("Identifier")))
                continue;

            // requireToolAddressableFrom throws NotFoundException unless the request is addressed to
            // the tool's own folder or to the store folder above it.
            HttpPost request = new HttpPost(
                    WebTestHelper.buildURL("skyts", storeContainerOf(tool), "delete"));
            request.setEntity(MultipartEntityBuilder.create()
                    .addTextBody("toolId", String.valueOf(rowId(tool)))
                    .build());
            APITestHelper.injectCookies(request);
            int status;
            try (CloseableHttpClient client = WebTestHelper.getHttpClient())
            {
                status = client.execute(request, response -> {
                    EntityUtils.consumeQuietly(response.getEntity());
                    return response.getCode();
                });
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to remove leftover tool " + tool.optString("Name"), e);
            }
            // DeleteAction is a MutatingApiAction, so a refusal returns an error status.
            assertEquals("Deleting leftover tool " + tool.optString("Name") + " returned HTTP " + status,
                    200, status);
        }

        // Confirm that the tools are really removed.
        Set<String> stillPresent = new HashSet<>(catalogIdentifiers(containerPath));
        stillPresent.retainAll(toRemove);
        assertTrue("Tools still in the catalog after cleanup: " + stillPresent, stillPresent.isEmpty());
    }
}
