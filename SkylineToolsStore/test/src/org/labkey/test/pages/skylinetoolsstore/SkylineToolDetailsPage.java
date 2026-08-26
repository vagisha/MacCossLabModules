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
package org.labkey.test.pages.skylinetoolsstore;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.skylinetoolsstore.ConfirmDeleteDialog;
import org.labkey.test.components.skylinetoolsstore.ManageToolOwnersDialog;
import org.labkey.test.components.skylinetoolsstore.SupplementaryFileDialog;
import org.labkey.test.components.skylinetoolsstore.ToolUploadDialog;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The details page for one version of one tool.
 *
 * The settings gear and the edit pencils are drawn only for someone who may edit the tool. The
 * supplementary file trash icons are drawn only for someone who may delete. The can* methods are how
 * a test asks what is on offer.
 */
public class SkylineToolDetailsPage extends LabKeyPage<SkylineToolDetailsPage.ElementCache>
{
    public static final String DESCRIPTION = "Description";
    public static final String ORGANIZATION = "Organization";
    public static final String AUTHORS = "Authors";
    public static final String LANGUAGES = "Languages";
    public static final String PROVIDER = "Provider's Website";

    public SkylineToolDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public String getToolName()
    {
        return getText(Locators.toolName);
    }

    /** The line reads "Version 1.0", followed by a View All link when the tool has more than one. */
    public String getVersion()
    {
        String line = getText(Locators.versionLine);
        Matcher version = Pattern.compile("Version\\s+(\\S+)").matcher(line);
        if (!version.find())
            throw new AssertionError("No version in the details header: " + line);
        return version.group(1);
    }

    public int getDownloadCount()
    {
        return Integer.parseInt(getText(Locators.downloadCount).trim());
    }

    /**
     * Properties are keyed by the title attribute their row carries. The edit dialog uses the same
     * string, so the constants above serve both.
     */
    public String getProperty(String property)
    {
        return getText(Locators.propertyValue(property));
    }

    public boolean hasProperty(String property)
    {
        return Locators.property(property).existsIn(getDriver());
    }

    public boolean canEditProperties()
    {
        return Locators.editPencil.existsIn(getDriver());
    }

    public List<String> getSupplementaryFileNames()
    {
        return Locators.supplementaryFileName.findElements(getDriver()).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public boolean hasOnlineDocumentationLink()
    {
        return Locator.linkWithText("Online Documentation").existsIn(getDriver());
    }

    /** The box is hidden once its last supplementary file goes. There is no page load. */
    public boolean isDocumentationBoxShowing()
    {
        WebElement box = Locators.documentationBox.findElementOrNull(getDriver());
        return box != null && box.isDisplayed();
    }

    public boolean canDeleteSupplementaryFiles()
    {
        return Locators.deleteSupplementaryFile.existsIn(getDriver());
    }

    /**
     * Deletes over ajax behind a browser confirm, then waits for the row to go. The row is removed
     * by script on any 2xx, so a caller that needs to know the file is really gone should reload.
     */
    public SkylineToolDetailsPage deleteSupplementaryFile(String fileName)
    {
        Locator.XPathLocator row = Locators.supplementaryFileRow(fileName);
        click(row.append(Locators.deleteSupplementaryFile));
        acceptAlert();
        waitForElementToDisappear(row);
        return this;
    }

    public boolean hasSettingsMenu()
    {
        return Locators.settingsToggle.existsIn(getDriver());
    }

    public boolean hasMenuItem(String item)
    {
        if (!hasSettingsMenu())
            return false;
        openSettingsMenu();
        return Locator.linkWithText(item).existsIn(Locators.settingsMenu.findElement(getDriver()));
    }

    /** Bootstrap toggles the menu with no animation, so it is usable as soon as it is open. */
    public void openSettingsMenu()
    {
        if (isMenuOpen())
            return;
        click(Locators.settingsToggle);
        WebDriverWrapper.waitFor(this::isMenuOpen, "The settings menu did not open", 5_000);
    }

    public ToolUploadDialog clickUploadNewVersion()
    {
        clickMenuItem("Upload new version");
        return ToolUploadDialog.forNewVersion(getDriver());
    }

    public SupplementaryFileDialog clickUploadSupplementaryFile()
    {
        clickMenuItem("Upload supplementary file");
        return new SupplementaryFileDialog(getDriver());
    }

    public ManageToolOwnersDialog clickManageToolOwners()
    {
        clickMenuItem("Manage tool owners");
        return new ManageToolOwnersDialog(getDriver());
    }

    public ConfirmDeleteDialog clickDelete()
    {
        clickMenuItem("Delete tool from store");
        return ConfirmDeleteDialog.deleteTool(getDriver());
    }

    public ConfirmDeleteDialog clickDeleteLatestVersion()
    {
        clickMenuItem("Delete latest version");
        return ConfirmDeleteDialog.deleteLatestVersion(getDriver());
    }

    public SkylineToolDetailsPage uploadNewVersion(File toolZip)
    {
        return clickUploadNewVersion().setToolZip(toolZip).clickUpload();
    }

    public SkylineToolDetailsPage uploadSupplementaryFile(File file)
    {
        return clickUploadSupplementaryFile().setFile(file).clickUpload();
    }

    private void clickMenuItem(String item)
    {
        openSettingsMenu();
        WebElement link = Locator.linkWithText(item)
                .waitForElement(Locators.settingsMenu.findElement(getDriver()), WAIT_FOR_JAVASCRIPT);
        scrollIntoView(link);
        link.click();
    }

    private boolean isMenuOpen()
    {
        WebElement menu = Locators.settingsMenu.findElementOrNull(getDriver());
        if (menu == null)
            return false;
        String cls = menu.getAttribute("class");
        return cls != null && cls.contains("open");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
    }

    private static abstract class Locators
    {
        static final Locator.XPathLocator toolName = Locator.tagWithClass("div", "block").child("h2");
        // The first paragraph of the header block. The ones after it are the upload date and, on an
        // older version, a link to the latest.
        static final Locator.XPathLocator versionLine = Locator.tagWithClass("div", "block").child("p");
        static final Locator.IdLocator downloadCount = Locator.id("downloadcounter");
        static final Locator.XPathLocator documentationBox = Locator.id("documentationbox");
        static final Locator.XPathLocator supplementaryFileName =
                Locator.tagWithClass("span", "suppfilename");
        static final Locator.XPathLocator deleteSupplementaryFile =
                Locator.tagWithClass("span", "deleteSuppFile");
        static final Locator.XPathLocator editPencil = Locator.id("editIcon");
        static final Locator.XPathLocator settingsMenu =
                Locator.tagWithClass("div", "dropdown").withClass("sprocket");
        static final Locator.XPathLocator settingsToggle = Locator.tagWithId("button", "toolSettingsMenu");

        static Locator.XPathLocator property(String property)
        {
            return Locator.tagWithClass("*", "toolProperty").withAttribute("title", property);
        }

        static Locator.XPathLocator propertyValue(String property)
        {
            return property(property).append(Locator.tagWithClass("span", "toolPropertyValue"));
        }

        static Locator.XPathLocator supplementaryFileRow(String fileName)
        {
            return Locator.tagWithClass("div", "suppfile")
                    .withDescendant(supplementaryFileName.withText(fileName));
        }
    }
}
