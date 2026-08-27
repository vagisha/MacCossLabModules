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
package org.labkey.test.components.skylinetoolsstore;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.pages.skylinetoolsstore.SkylineToolDetailsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * One tool in the Skyline Tool Store listing.
 *
 * The settings menu is rendered only for someone who may edit the tool, and which items it holds
 * varies by permission.
 */
public class ToolRow extends WebDriverComponent<ToolRow.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public ToolRow(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public String getName()
    {
        return _el.getAttribute("data-toolName");
    }

    public String getVersion()
    {
        return _el.getAttribute("data-toolVersion");
    }

    public int getDownloadCount()
    {
        return Integer.parseInt(_el.getAttribute("data-toolDownloads"));
    }

    public int getToolId()
    {
        return Integer.parseInt(_el.getAttribute("data-toolId"));
    }

    public SkylineToolDetailsPage clickToolName()
    {
        getWrapper().clickAndWait(elementCache().nameLink);
        return new SkylineToolDetailsPage(_driver);
    }

    public ToolUploadDialog clickUploadNewVersion()
    {
        clickMenuItem("Upload new version");
        return ToolUploadDialog.forNewVersion(_driver);
    }

    public SupplementaryFileDialog clickUploadSupplementaryFile()
    {
        clickMenuItem("Upload supplementary file");
        return new SupplementaryFileDialog(_driver);
    }

    public ManageToolOwnersDialog clickManageToolOwners()
    {
        clickMenuItem("Manage tool owners");
        return new ManageToolOwnersDialog(_driver);
    }

    public ConfirmDeleteDialog clickDelete()
    {
        clickMenuItem("Delete tool from store");
        return ConfirmDeleteDialog.deleteTool(_driver);
    }

    public ConfirmDeleteDialog clickDeleteLatestVersion()
    {
        clickMenuItem("Delete latest version");
        return ConfirmDeleteDialog.deleteLatestVersion(_driver);
    }

    /**
     * Bootstrap toggles the menu open on click with no animation, so its items are clickable as
     * soon as the parent carries the open class.
     */
    public void openSettingsMenu()
    {
        WebElement toggle = elementCache().settingsToggle;
        getWrapper().scrollIntoView(toggle);
        if (isMenuOpen())
            return;
        toggle.click();
        WebDriverWrapper.waitFor(this::isMenuOpen, "The settings menu did not open", 5_000);
    }

    /** Bootstrap does not animate the menu, so one click on an item is enough. */
    private void clickMenuItem(String item)
    {
        openSettingsMenu();
        WebElement link = Locator.linkWithText(item)
                .waitForElement(elementCache().settingsMenu, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        getWrapper().scrollIntoView(link);
        link.click();
    }

    /** Bootstrap marks the open menu by putting the open class on the .dropdown wrapper. */
    private boolean isMenuOpen()
    {
        String cls = elementCache().settingsMenu.getAttribute("class");
        return cls != null && cls.contains("open");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement nameLink = Locators.nameLink.findWhenNeeded(this);
        final WebElement settingsMenu = Locators.settingsMenu.findWhenNeeded(this);
        final WebElement settingsToggle = Locators.settingsToggle.findWhenNeeded(this);
    }

    private static abstract class Locators
    {
        // The row holds a second dropdown for the documentation menu, so the settings menu has to be
        // picked out by its own class rather than by being the first one.
        static final Locator.XPathLocator settingsMenu =
                Locator.tagWithClass("div", "dropdown").withClass("sprocket");
        static final Locator.XPathLocator settingsToggle =
                settingsMenu.child(Locator.tagWithClass("button", "sprocketToggle"));
        static final Locator.XPathLocator nameLink =
                Locator.tagWithClass("span", "title").child("a");
    }
}
