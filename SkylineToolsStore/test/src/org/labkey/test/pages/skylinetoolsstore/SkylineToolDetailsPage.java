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
import org.labkey.test.components.Component;
import org.labkey.test.components.skylinetoolsstore.ConfirmDeleteDialog;
import org.labkey.test.components.skylinetoolsstore.ManageToolOwnersDialog;
import org.labkey.test.components.skylinetoolsstore.SupplementaryFileDialog;
import org.labkey.test.components.skylinetoolsstore.ToolUploadDialog;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One tool's details page.
 *
 * Each editable property is a block carrying the property's display name in its title attribute,
 * which is also what the page's own edit script reads to label the dialog, so getProperty and the
 * page agree on what a property is called.
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
        return elementCache().toolName.getText();
    }

    public String getVersion()
    {
        return elementCache().toolVersion.getText();
    }

    public int getDownloadCount()
    {
        return Integer.parseInt(elementCache().downloadCount.getText().trim());
    }

    /**
     * @param property one of the constants on this class
     * @return the value shown, empty when the tool has none
     */
    public String getProperty(String property)
    {
        return Locators.propertyValue(property).findElement(getDriver()).getText().trim();
    }

    public boolean hasProperty(String property)
    {
        return Locators.property(property).existsIn(getDriver());
    }

    /** The pencils are only rendered for someone who may edit this tool. */
    public boolean canEditProperties()
    {
        return Locators.editPencil.existsIn(getDriver());
    }

    public List<String> getSupplementaryFileNames()
    {
        return Locators.suppFileName.findElements(getDriver()).stream()
                .map(name -> name.getText().trim())
                .collect(Collectors.toList());
    }

    public boolean hasOnlineDocumentationLink()
    {
        return Locator.linkWithText("Online Documentation").existsIn(getDriver());
    }

    public boolean isDocumentationBoxShowing()
    {
        return Locators.documentationBox.findOptionalElement(getDriver())
                .map(WebElement::isDisplayed).orElse(false);
    }

    /**
     * Deletes a supplementary file, accepting the browser confirm the page raises first.
     *
     * The row is removed by script rather than by a page load, so this waits for it to go.
     */
    public SkylineToolDetailsPage deleteSupplementaryFile(String fileName)
    {
        Locators.deleteSuppFile(fileName).findElement(getDriver()).click();
        acceptAlert();
        WebDriverWrapper.waitFor(() -> !getSupplementaryFileNames().contains(fileName),
                "The supplementary file " + fileName + " was still listed after deleting it",
                WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public boolean canDeleteSupplementaryFiles()
    {
        return Locators.anyDeleteSuppFile.existsIn(getDriver());
    }

    public boolean hasSettingsMenu()
    {
        return Locators.settingsMenu.existsIn(getDriver());
    }

    public boolean hasMenuItem(String item)
    {
        if (!hasSettingsMenu())
            return false;
        openSettingsMenu();
        return Locator.linkWithText(item).existsIn(elementCache().settingsMenu);
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
        clickMenuItem("Delete");
        return ConfirmDeleteDialog.deleteTool(getDriver());
    }

    public ConfirmDeleteDialog clickDeleteLatestVersion()
    {
        clickMenuItem("Delete latest version");
        return ConfirmDeleteDialog.deleteLatestVersion(getDriver());
    }

    /** Publishes a new version and returns the details page for it. */
    public SkylineToolDetailsPage uploadNewVersion(File toolZip)
    {
        return clickUploadNewVersion().setToolZip(toolZip).clickUpload();
    }

    public SkylineToolDetailsPage uploadSupplementaryFile(File file)
    {
        return clickUploadSupplementaryFile().setFile(file).clickUpload();
    }

    /**
     * The menu toggles on click, not on hover, and slides open, so its items are in the DOM before
     * they can be clicked.
     */
    public void openSettingsMenu()
    {
        WebElement menu = elementCache().settingsMenu;
        scrollIntoView(menu);
        if (Locators.dropMenu.findElement(menu).isDisplayed())
            return;
        menu.click();
        shortWait().until(d -> Locators.dropMenu.findElement(menu).isDisplayed());
    }

    /**
     * The menu slides open, so an item is in the DOM and inside a displayed list before it has any
     * height of its own. Waiting for the item itself to be clickable is what makes this reliable.
     */
    private void clickMenuItem(String item)
    {
        openSettingsMenu();
        WebElement link = Locator.linkWithText(item)
                .waitForElement(elementCache().settingsMenu, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        scrollIntoView(link);
        shortWait().until(ExpectedConditions.elementToBeClickable(link));
        link.click();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        final WebElement toolName = Locator.id("toolName").findWhenNeeded(getDriver());
        final WebElement toolVersion = Locator.id("toolVersion").findWhenNeeded(getDriver());
        final WebElement downloadCount = Locator.id("downloadcounter").findWhenNeeded(getDriver());
        final WebElement settingsMenu = Locators.settingsMenu.findWhenNeeded(getDriver());
    }

    private static abstract class Locators
    {
        static final Locator.XPathLocator settingsMenu =
                Locator.tagWithClass("div", "menuMouseArea").withClass("sprocket");
        static final Locator.XPathLocator dropMenu = Locator.tagWithClass("ul", "dropMenu");
        static final Locator.XPathLocator editPencil = Locator.tagWithClass("span", "editToolIcon");
        static final Locator.XPathLocator documentationBox = Locator.id("documentationbox");
        static final Locator.XPathLocator suppFileName = Locator.tagWithClass("span", "suppfilename");
        static final Locator.XPathLocator anyDeleteSuppFile = Locator.tagWithClass("span", "deleteSuppFile");

        // The description is a <p> and the information rows are <div>, so this matches on the class
        // and the title rather than on a tag.
        static Locator.XPathLocator property(String property)
        {
            return Locator.tagWithClass("*", "toolProperty").withAttribute("title", property);
        }

        static Locator.XPathLocator propertyValue(String property)
        {
            return property(property).descendant(Locator.tagWithClass("span", "toolPropertyValue"));
        }

        static Locator.XPathLocator deleteSuppFile(String fileName)
        {
            return Locator.tagWithClass("div", "suppfile")
                    .withDescendant(suppFileName.withText(fileName))
                    .descendant(anyDeleteSuppFile);
        }
    }
}
