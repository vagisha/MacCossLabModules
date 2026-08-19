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
package org.labkey.skylinetoolsstore.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.data.Entity;
import org.labkey.api.files.FileContentService;
import org.labkey.api.security.Group;
import org.labkey.api.security.RoleAssignment;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.roles.EditorRole;
import org.labkey.api.security.roles.FolderAdminRole;
import org.labkey.api.security.roles.Role;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.Pair;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.webdav.WebdavService;
import org.labkey.skylinetoolsstore.SkylineToolsStoreController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SkylineTool extends Entity
{
    private static final Logger LOG = LogHelper.getLogger(SkylineTool.class, "Skyline tool store model");

    private String _zipName;
    private String _name;
    private String _authors;
    private String _organization;
    private String _provider;
    private String _version;
    private String _languages;
    private String _description;
    private String _identifier;
    private byte[] _icon = null;
    private Integer _downloads = 0;
    private boolean _latest = false;
    private Integer _rowId;

    public SkylineTool()
    {
    }

    public SkylineTool(BufferedReader reader) throws IOException
    {
        parseProperties(reader);
    }

    public String getZipName()
    {
        return _zipName;
    }

    public void setZipName(String zipName)
    {
        _zipName = zipName;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getAuthors()
    {
        return _authors;
    }

    public void setAuthors(String authors)
    {
        _authors = authors;
    }

    public String getOrganization()
    {
        return _organization;
    }

    public void setOrganization(String organization)
    {
        _organization = organization;
    }

    public String getProvider()
    {
        return _provider;
    }

    public void setProvider(String provider)
    {
        _provider = provider;
    }

    public String getVersion()
    {
        return _version;
    }

    public void setLanguages(String languages)
    {
        _languages = languages;
    }

    public String getLanguages()
    {
        return _languages;
    }

    public void setVersion(String version)
    {
        _version = version;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getIdentifier()
    {
        return _identifier;
    }

    public void setIdentifier(String identifier)
    {
        _identifier = identifier;
    }

    public Integer getDownloads()
    {
        return _downloads;
    }

    public void setDownloads(Integer downloads)
    {
        _downloads = downloads;
    }

    public boolean getLatest()
    {
        return _latest;
    }

    public void setLatest(boolean latest)
    {
        _latest = latest;
    }

    public byte[] getIcon()
    {
        return _icon;
    }

    public void setIcon(byte[] icon)
    {
        _icon = icon;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public void setRowId(Integer rowId)
    {
        _rowId = rowId;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof SkylineTool p))
            return false;

        return Objects.equals(_name, p.getName()) &&
               Objects.equals(_authors, p.getAuthors()) &&
               Objects.equals(_provider, p.getProvider()) &&
               Objects.equals(_version, p.getVersion()) &&
               Objects.equals(_languages, p.getLanguages()) &&
               Objects.equals(_description, p.getDescription()) &&
               Objects.equals(_identifier, p.getIdentifier());
    }

    protected Pair<String, String> getNextProperty(BufferedReader reader) throws IOException
    {
        String line;
        int splitter = 0;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            if (!line.isEmpty() && line.charAt(0) != '#' && (splitter = line.indexOf('=')) != -1)
                break;
        }

        if (line == null)
            return null;

        String propName = line.substring(0, splitter).trim();
        String propValue = (splitter != line.length() - 1) ? line.substring(splitter + 1).trim() : "";

        while (propValue.endsWith("\\") && (line = reader.readLine()) != null)
        {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '#')
                continue;

            propValue = propValue.substring(0, propValue.length() - 1) + '\n' + line;
        }

        return new Pair<>(propName, propValue);
    }

    public void setProperty(String propName, String propValue)
    {
        switch (propName.toLowerCase())
        {
            case "name":
                this.setName(propValue);
                break;
            case "version":
                this.setVersion(propValue);
                break;
            case "author":
                this.setAuthors(propValue);
                break;
            case "languages":
                this.setLanguages(propValue);
                break;
            case "organization":
                this.setOrganization(propValue);
                break;
            case "description":
                // Description may have quotes around it
                if (propValue.length() > 1 && propValue.startsWith("\"") && propValue.endsWith("\""))
                    propValue = propValue.substring(1, propValue.length() - 1).trim();
                this.setDescription(propValue);
                break;
            case "provider":
                this.setProvider(propValue);
                break;
            case "identifier":
                this.setIdentifier(propValue);
                break;
        }
    }

    public void parseProperties(BufferedReader reader) throws IOException
    {
        Pair<String, String> pair;
        while ((pair = getNextProperty(reader)) != null)
            setProperty(pair.first, pair.second);
    }

    public ArrayList<String> getMissingValues()
    {
        ArrayList<String> missingValues = new ArrayList();
        if (StringUtils.trimToNull(_name) == null)
            missingValues.add("Name");
        if (StringUtils.trimToNull(_version) == null)
            missingValues.add("Version");
        if (StringUtils.trimToNull(_identifier) == null)
            missingValues.add("Identifier");
        return missingValues;
    }

    /**
     * Writes the icon beside the target and moves it into place, so any failure leaves the icon
     * already there rather than an empty file. getIconUrl only tests that the file exists, so an
     * empty one renders as a broken image until someone uploads again.
     *
     * The temp name starts with a dot, which keeps it out of the supplementary file list even if
     * it is left behind - see getSupplementaryFileBasenames.
     */
    public void writeIconToFile(File file, String format) throws IOException
    {
        if (_icon == null)
            return;

        File tmpFile = new File(file.getParentFile(), "." + file.getName() + ".tmp");
        try
        {
            try (FileOutputStream iconOutputStream = new FileOutputStream(tmpFile))
            {
                // write returns false rather than throwing when no writer handles the format.
                if (!ImageIO.write(decodeIcon(), format, iconOutputStream))
                    throw new IOException("This server has no writer for the image format " + format + ".");
            }

            Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        finally
        {
            // A move that succeeded consumed it already.
            if (tmpFile.exists() && !tmpFile.delete())
                LOG.warn("Could not delete the temporary icon {}", tmpFile.getAbsolutePath());
        }
    }

    /**
     * Throws unless the icon bytes are an image this server can decode. For callers that must
     * refuse before committing other work alongside the icon.
     */
    public void validateIcon() throws IOException
    {
        if (_icon == null)
            return;

        decodeIcon();
    }

    private BufferedImage decodeIcon() throws IOException
    {
        try (ByteArrayInputStream iconInputStream = new ByteArrayInputStream(_icon))
        {
            BufferedImage image = ImageIO.read(iconInputStream);
            // null means no reader recognised the format. Callers catch IOException, so make it one.
            if (image == null)
                throw new IOException("The tool's icon is not an image this server can read.");
            return image;
        }
    }

    public String getFolderUrl()
    {
        return AppProps.getInstance().getContextPath() + "/files" + lookupContainer().getPath() + "/";
    }

    public boolean hasDocumentation()
    {
        Path localPath = SkylineToolsStoreController.getLocalPath(lookupContainer());
        return localPath != null && Files.exists(localPath.resolve("docs/index.html"));
    }

    public String getDocsUrl()
    {
        org.labkey.api.util.Path path = WebdavService.getPath()
                .append(lookupContainer().getParsedPath())
                .append(FileContentService.FILES_LINK)
                .append(new org.labkey.api.util.Path("docs", "index.html"));
        return AppProps.getInstance().getContextPath() + path.encode();
    }

    public String getIconUrl()
    {
        return (SkylineToolsStoreController.makeFile(lookupContainer(), "icon.png").exists()) ?
            getFolderUrl() + "icon.png" :
            AppProps.getInstance().getContextPath() + "/skylinetoolsstore/img/placeholder.png";
    }

    /**
     * The users that were granted roles giving them the ability to manage this tool.
     *
     * A tool owner is whoever holds Editor or FolderAdmin role in the tool folder - see createVersionFolder,
     * which grants the owners EditorRole when it makes the folder.
     */
    public ArrayList<String> getOwners()
    {
        HashSet<String> users = new HashSet<>();
        for (RoleAssignment assignment : ownerRoleAssignments())
        {
            User user = UserManager.getUser(assignment.getUserId());
            if (user != null && user.getEmail() != null)
                users.add(user.getEmail());
        }
        // Sorted, so the owners box does not reorder itself between page loads.
        ArrayList<String> sorted = new ArrayList<>(users);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    /**
     * The groups that were granted roles giving them the ability to manage this tool.
     *
     * Manage Tool Owners only adds and removes users, not groups. A group is granted access through
     * the permissions UI, and has to be revoked there too.
     */
    public ArrayList<String> getOwnerGroups()
    {
        HashSet<String> groups = new HashSet<>();
        for (RoleAssignment assignment : ownerRoleAssignments())
        {
            // A principal that does not resolve as a user is a group.
            if (UserManager.getUser(assignment.getUserId()) != null)
                continue;
            Group group = SecurityManager.getGroup(assignment.getUserId());
            if (group != null)
                groups.add(group.getName());
        }
        ArrayList<String> sorted = new ArrayList<>(groups);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    private List<RoleAssignment> ownerRoleAssignments()
    {
        Container c = lookupContainer();
        if (c == null)
            return List.of();

        Set<Role> ownerRoles = Set.of(RoleManager.getRole(EditorRole.class),
                RoleManager.getRole(FolderAdminRole.class));
        List<RoleAssignment> owners = new ArrayList<>();
        for (RoleAssignment assignment : c.getPolicy().getAssignments())
            if (ownerRoles.contains(assignment.getRole()))
                owners.add(assignment);
        return owners;
    }

    /**
     * Whether the user may use the editing controls the store pages offer for this tool.
     * Only tool owners may edit, and createVersionFolder grants them EditorRole, which carries
     * Update, Insert and Delete.
     */
    public boolean isEditor(User user)
    {
        Container c = lookupContainer();
        return c != null && (user.hasSiteAdminPermission() || c.hasPermissions(user,
                Set.of(UpdatePermission.class, InsertPermission.class, DeletePermission.class)));
    }

    public String getPrettyCreated()
    {
        Calendar uploadCal = Calendar.getInstance();
        uploadCal.setTime(getCreated());
        return new SimpleDateFormat("MMM d, yyyy").format(uploadCal.getTime());
    }

    public InputStream getInfoPropertiesStream(InputStream in, String propName, String newValue) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        newValue = newValue.replace("\r\n", "\\\r\n");

        boolean foundProperty = false;
        StringBuilder sb = new StringBuilder();

        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            String trimmed = line.trim();
            int splitter = trimmed.indexOf('=');

            if (trimmed.isEmpty() || trimmed.charAt(0) == '#' || splitter == -1
                    || !trimmed.substring(0, splitter).trim().equalsIgnoreCase(propName))
            {
                appendLine(sb, line);

                // Carry the rest of another property's value over as it stands. Left to the loop,
                // a continuation line holding an = would be read as a property of its own.
                while (trimmed.endsWith("\\") && (line = reader.readLine()) != null)
                {
                    appendLine(sb, line);
                    trimmed = line.trim();
                }
                continue;
            }

            foundProperty = true;
            String curPropName = trimmed.substring(0, splitter).trim();

            // Skip this property's own line and every continuation of it. A line continues onto
            // the next when it ends with a backslash, so the test is on the line in hand rather
            // than on the one after it.
            while (trimmed.endsWith("\\") && (line = reader.readLine()) != null)
                trimmed = line.trim();

            // A blank value removes the property line rather than writing an empty one.
            if (!newValue.isEmpty())
                appendLine(sb, curPropName + " = " + newValue);
        }

        if (!foundProperty)
            appendLine(sb, propName + " = " + newValue);

        return new ByteArrayInputStream(sb.toString().getBytes());
    }

    /**
     * Adds one line, writing the line break before it rather than after it.
     *
     * The loop above reads the next line before it appends the one in hand, so a reader with
     * nothing left does not mean this is the last line to be written.
     */
    private static void appendLine(StringBuilder sb, String line)
    {
        if (sb.length() > 0)
            sb.append("\r\n");
        sb.append(line);
    }

    public Container getContainerParent()
    {
        Container c = lookupContainer();
        if(c != null)
        {
            return c.getParent();
        }
        return null;
    }

    /**
     * The version format Skyline prescribes. ToolInstaller.cs reads Version through
     * System.Version.TryParse and reads anything it rejects as no version at all, so a version is
     * two to four dot separated parts, each a run of digits that fits in an int.
     *
     * @return major, minor, build and revision, with -1 for a part that is not there, the way
     *         System.Version leaves it. Null when Skyline would not read the string as a version
     */
    public static int[] parseSkylineVersion(String version)
    {
        String[] parts = (version == null ? "" : version.trim()).split("\\.", -1);
        if (parts.length < 2 || parts.length > 4)
            return null;

        int[] parsed = {-1, -1, -1, -1};
        for (int i = 0; i < parts.length; i++)
        {
            if (parts[i].isEmpty() || leadingDigits(parts[i]).length() != parts[i].length())
                return null;
            try
            {
                parsed[i] = Integer.parseInt(parts[i]);
            }
            catch (NumberFormatException e)
            {
                // Longer than an int holds, which System.Version does not accept either.
                return null;
            }
        }

        return parsed;
    }

    /**
     * Orders two tool versions the way System.Version does, so 1.0.0 is newer than 1.0. Negative
     * when a is older, zero when they are the same version, positive when a is newer.
     *
     * A version Skyline cannot read sorts below one it can, and two of them are the same version.
     * readToolFromUpload refuses those on the way in, so only an older row can still be one.
     */
    public static int compareVersions(String a, String b)
    {
        int[] aParsed = parseSkylineVersion(a);
        int[] bParsed = parseSkylineVersion(b);
        if (aParsed == null || bParsed == null)
        {
            if (aParsed == null && bParsed == null)
                return 0;
            return aParsed == null ? -1 : 1;
        }

        for (int i = 0; i < aParsed.length; i++)
        {
            int result = Integer.compare(aParsed[i], bParsed[i]);
            if (result != 0)
                return result;
        }

        return 0;
    }

    private static String leadingDigits(String part)
    {
        int end = 0;
        while (end < part.length() && part.charAt(end) >= '0' && part.charAt(end) <= '9')
            end++;

        return part.substring(0, end);
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testCompareVersions()
        {
            assertTrue("2.0 is newer than 1.9", compareVersions("2.0", "1.9") > 0);
            assertTrue("1.9 is older than 2.0", compareVersions("1.9", "2.0") < 0);
            assertTrue("1.10 is newer than 1.9, which comparing as text gets backwards",
                    compareVersions("1.10", "1.9") > 0);
            assertTrue("System.Version leaves an absent part at -1, so 1.0.0 is newer than 1.0",
                    compareVersions("1.0", "1.0.0") < 0);
            assertTrue("A third part still orders", compareVersions("1.0.1", "1.0") > 0);
            assertEquals("Leading zeroes do not change the number", 0, compareVersions("1.01", "1.1"));
            assertEquals("Two versions Skyline cannot read are the same version",
                    0, compareVersions("1.0-BETA", "1.0-beta"));
            assertTrue("A version Skyline can read is newer than one it cannot",
                    compareVersions("1.0", "1.0-beta") > 0);
            assertTrue("A part that is not a number makes the whole version unreadable",
                    compareVersions("1.9", "1.beta") > 0);
            assertEquals("Surrounding space does not make a different version",
                    0, compareVersions(" 1.0 ", "1.0"));
            assertEquals("Nothing compares equal to nothing", 0, compareVersions(null, ""));
            assertTrue("A version that is not there is older than one that is",
                    compareVersions("", "0.1") < 0);
            assertTrue("A fourth part orders the way System.Version orders it",
                    compareVersions("1.0.0.1", "1.0.0") > 0);
        }

        @Test
        public void testParseSkylineVersion()
        {
            assertNotNull("Two parts is the shortest Skyline reads", parseSkylineVersion("1.0"));
            assertNotNull("Four parts is the longest", parseSkylineVersion("1.0.0.0"));
            assertNull("One part is not a version Skyline reads", parseSkylineVersion("2"));
            assertNull("Five parts is too many", parseSkylineVersion("1.0.0.0.0"));
            assertNull("A pre-release suffix is not a number", parseSkylineVersion("1.0-beta"));
            assertNull("A part that is empty is not a number", parseSkylineVersion("1..0"));
            assertNull("A trailing dot leaves an empty part", parseSkylineVersion("1.0."));
            assertNull("A part too long to hold in an int is not a version",
                    parseSkylineVersion("1.12345678901234567890"));
            assertNull("Nothing is not a version", parseSkylineVersion(null));

            assertArrayEquals("A part that is not there is -1, not 0",
                    new int[]{1, 0, -1, -1}, parseSkylineVersion("1.0"));
            assertArrayEquals("Surrounding space does not change the version",
                    new int[]{1, 0, -1, -1}, parseSkylineVersion(" 1.0 "));
        }

        @Test
        public void testEditingAPropertyKeepsTheLineAfterIt()
        {
            String edited = editProperty("Name = Foo\r\nProvider = http://old\r\n" +
                    "Identifier = URN:LSID:x\r\n", "provider", "http://new");

            assertTrue("The edited property should end where its value ends, but got\n" + edited,
                    edited.contains("Provider = http://new\r\n"));
            assertTrue("The property after it has to keep its own line, but got\n" + edited,
                    edited.contains("\r\nIdentifier = URN:LSID:x"));
        }

        @Test
        public void testEditingAPropertyLeavesTheOthersAlone()
        {
            String file = "Name = Foo\r\nProvider = http://old\r\nOrganization = Lab\r\n" +
                    "Identifier = URN:LSID:x\r\n";

            assertEquals("Only the edited property should change",
                    "Name = Foo\r\nProvider = http://new\r\nOrganization = Lab\r\n" +
                            "Identifier = URN:LSID:x",
                    editProperty(file, "provider", "http://new"));

            assertEquals("Editing the last property should work the same way",
                    "Name = Foo\r\nProvider = http://old\r\nOrganization = Lab\r\n" +
                            "Identifier = URN:LSID:y",
                    editProperty(file, "identifier", "URN:LSID:y"));
        }

        /**
         * A value spanning exactly two lines is the case the continuation scan gets wrong. One line
         * has nothing to leave behind and three or more are consumed correctly.
         */
        private static final String TWO_LINE_VALUE =
                "Name = Foo\r\nDescription = one line\\\r\ntwo line\r\nAuthor = Bob\r\n";

        @Test
        public void testEditingATwoLinePropertyLeavesNoOrphan()
        {
            String edited = editProperty(TWO_LINE_VALUE, "Description", "replaced");

            assertTrue("The property should carry its new value, but got\n" + edited,
                    edited.contains("Description = replaced"));
            assertFalse("The second line of the old value must not be left behind, but got\n" + edited,
                    edited.contains("two line"));
        }

        @Test
        public void testBlankingATwoLinePropertyRemovesTheWholeValue()
        {
            String edited = editProperty(TWO_LINE_VALUE, "Description", "");

            assertFalse("Blanking should remove the property, but got\n" + edited,
                    edited.contains("Description"));
            assertFalse("Blanking must not leave the second line behind, but got\n" + edited,
                    edited.contains("two line"));
            assertTrue("The property after it should survive, but got\n" + edited,
                    edited.contains("Author = Bob"));
        }

        /**
         * The details page turns each line of a textarea into a backslash continuation, so a value
         * whose second line reads like a property entry is something an owner can type.
         */
        @Test
        public void testAContinuationThatLooksLikeAPropertyIsLeftAlone()
        {
            String edited = editProperty(
                    "Name = Foo\r\nAuthor = Bob\\\r\nDescription = part of what Bob typed\r\n" +
                            "Description = the real one\r\nProvider = p\r\n",
                    "Description", "replaced");

            assertTrue("The line belonging to Author should be untouched, but got\n" + edited,
                    edited.contains("Description = part of what Bob typed"));
            assertTrue("The real property should carry its new value, but got\n" + edited,
                    edited.contains("Description = replaced"));
            assertEquals("Only the real property should have been replaced, but got\n" + edited,
                    1, edited.split("Description = replaced", -1).length - 1);
        }

        @Test
        public void testAThreeLinePropertyIsStillConsumedWhole()
        {
            String edited = editProperty(
                    "Name = Foo\r\nDescription = a\\\r\nb\\\r\nc\r\nAuthor = Bob\r\n",
                    "Description", "replaced");

            assertEquals("Only the edited property should change",
                    "Name = Foo\r\nDescription = replaced\r\nAuthor = Bob", edited);
        }

        private static String editProperty(String file, String propName, String newValue)
        {
            try
            {
                return new String(new SkylineTool().getInfoPropertiesStream(
                        new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)),
                        propName, newValue).readAllBytes(), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Could not rewrite the test properties", e);
            }
        }
    }
}
