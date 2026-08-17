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
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.data.Entity;
import org.labkey.api.files.FileContentService;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.Pair;
import org.labkey.api.webdav.WebdavService;
import org.labkey.skylinetoolsstore.SkylineToolsStoreController;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;
import java.util.Set;

public class SkylineTool extends Entity
{
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
        // Name, version, and identifier are required
        ArrayList<String> missingValues = new ArrayList();
        if (StringUtils.trimToNull(_name) == null)
            missingValues.add("Name");
        if (StringUtils.trimToNull(_version) == null)
            missingValues.add("Version");
        if (StringUtils.trimToNull(_identifier) == null)
            missingValues.add("Identifier");
        return missingValues;
    }

    public void writeIconToFile(File file, String format) throws IOException
    {
        if (_icon == null)
            return;

        // Decode before opening the stream. Opening a FileOutputStream truncates the file it names,
        // so decoding inside the try left icon.png at zero bytes when the bytes were not an image.
        // getIconUrl only tests that the file exists, so the tool then rendered a broken image.
        BufferedImage image = decodeIcon();
        try (FileOutputStream iconOutputStream = new FileOutputStream(file))
        {
            ImageIO.write(image, format, iconOutputStream);
        }
    }

    /**
     * Throws unless the icon bytes are an image this server can decode. For callers that commit
     * other work alongside the icon and need to refuse before that work is committed.
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
            // read returns null when nothing can decode the bytes. ImageIO.write then threw
            // IllegalArgumentException, which is unchecked, so it escaped the callers that catch
            // IOException and became a server error.
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
     * Whether the user may use the editing controls the store pages offer for this tool.
     *
     * All three permissions are required - Update to publish a version or edit a property, Insert to
     * upload a supplementary file, Delete to remove the newest version.
     * createVersionFolder grants tool owners EditorRole, which carries all three permissions.
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
        String line = reader.readLine();
        while (line != null)
        {
            String originalLine = line;
            line = line.trim();
            int splitter;
            String curPropName;
            if (!line.isEmpty() && line.charAt(0) != '#' && (splitter = line.indexOf('=')) != -1
                && (curPropName = line.substring(0, splitter).trim()).equalsIgnoreCase(propName))
            {
                foundProperty = true;
                boolean wasMultiline = false;
                while ((line = reader.readLine()) != null && line.trim().endsWith("\\"))
                    wasMultiline = true;
                if (!newValue.isEmpty())
                {
                    sb.append(curPropName).append(" = ").append(newValue).append(reader.ready() ? "\r\n" : "");
                }
                // Consume the last line of a property value that had multiple lines
                if (!wasMultiline)
                    continue;
            }
            else
                sb.append(originalLine).append(reader.ready() ? "\r\n" : "");

            line = reader.readLine();
        }

        if (!foundProperty)
            sb.append("\r\n").append(propName).append(" = ").append(newValue);

        return new ByteArrayInputStream(sb.toString().getBytes());
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
     * Orders two tool versions the way an author writes them - dot separated parts, each compared as
     * a number where both sides are numbers and as text otherwise. A part that is not there counts
     * as zero, so 1.0 and 1.0.0 are the same version. Returns a negative number when a is older than
     * b, zero when they are the same version, and a positive number when a is newer.
     *
     * A version is whatever the author put in info.properties, so nothing here may throw on one that
     * is not a number at all.
     */
    public static int compareVersions(String a, String b)
    {
        String[] aParts = (a == null ? "" : a.trim()).split("\\.");
        String[] bParts = (b == null ? "" : b.trim()).split("\\.");

        for (int i = 0; i < Math.max(aParts.length, bParts.length); i++)
        {
            String aPart = i < aParts.length ? aParts[i].trim() : "0";
            String bPart = i < bParts.length ? bParts[i].trim() : "0";
            if (aPart.equalsIgnoreCase(bPart))
                continue;

            int result = compareVersionParts(aPart, bPart);
            if (result != 0)
                return result;
        }

        return 0;
    }

    /**
     * One dot separated part. The digits it starts with are compared as a number and whatever
     * follows them as text, except that a part with nothing following outranks one that has
     * something. That is what makes 1.0 newer than 1.0-beta rather than older, which matters because
     * a version that does not sort above every stored version cannot be published at all.
     */
    private static int compareVersionParts(String a, String b)
    {
        String aDigits = leadingDigits(a);
        String bDigits = leadingDigits(b);

        int result = compareDigitRuns(aDigits, bDigits);
        if (result != 0)
            return result;

        String aRest = a.substring(aDigits.length());
        String bRest = b.substring(bDigits.length());
        if (aRest.isEmpty() != bRest.isEmpty())
            return aRest.isEmpty() ? 1 : -1;

        return aRest.compareToIgnoreCase(bRest);
    }

    private static String leadingDigits(String part)
    {
        int end = 0;
        while (end < part.length() && part.charAt(end) >= '0' && part.charAt(end) <= '9')
            end++;

        return part.substring(0, end);
    }

    /**
     * Numeric order without parsing, so a number too long to hold in an int still orders correctly.
     * A part with no digits at all sorts below one that has some.
     */
    private static int compareDigitRuns(String a, String b)
    {
        String aTrimmed = stripLeadingZeroes(a);
        String bTrimmed = stripLeadingZeroes(b);
        if (aTrimmed.length() != bTrimmed.length())
            return Integer.compare(aTrimmed.length(), bTrimmed.length());

        return aTrimmed.compareTo(bTrimmed);
    }

    private static String stripLeadingZeroes(String digits)
    {
        int start = 0;
        while (start < digits.length() - 1 && digits.charAt(start) == '0')
            start++;

        return digits.substring(start);
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
            assertEquals("A part that is not there counts as zero", 0, compareVersions("1.0", "1.0.0"));
            assertTrue("A third part still orders", compareVersions("1.0.1", "1.0") > 0);
            assertEquals("Leading zeroes do not change the number", 0, compareVersions("1.01", "1.1"));
            assertEquals("Case is ignored", 0, compareVersions("1.0-BETA", "1.0-beta"));
            assertTrue("A release is newer than the pre-release it follows",
                    compareVersions("1.0", "1.0-beta") > 0);
            assertTrue("Pre-releases of one version order among themselves as text",
                    compareVersions("1.0-beta", "1.0-alpha") > 0);
            assertTrue("A pre-release is still newer than the version before it",
                    compareVersions("2.0-beta", "1.9") > 0);
            assertTrue("A part with no number at all sorts below one that has a number",
                    compareVersions("1.9", "1.beta") > 0);
            assertEquals("Surrounding space does not make a different version",
                    0, compareVersions(" 1.0 ", "1.0"));
            assertEquals("Nothing compares equal to nothing", 0, compareVersions(null, ""));
            assertTrue("A version that is not there is older than one that is",
                    compareVersions("", "0.1") < 0);
            assertTrue("A number too long to hold in an int still orders by size",
                    compareVersions("1.12345678901234567890", "1.9") > 0);
        }
    }
}
