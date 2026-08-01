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
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * One tool in the Skyline Tool Store listing.
 *
 * The settings menu is rendered only for someone who may edit the tool, and which items it holds
 * varies by permission, so hasMenuItem is how a test asks what is on offer rather than assuming.
 */
public class ToolRow extends WebDriverComponent<ToolRow.ElementCache>
{
    /** Long enough for a dialog that is opening, short enough that a lost click is retried quickly. */
    private static final int WAIT_FOR_DIALOG = 5_000;

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

    public String getIdentifier()
    {
        return _el.getAttribute("data-toolLsid");
    }

    public int getDownloadCount()
    {
        return Integer.parseInt(_el.getAttribute("data-toolDownloads"));
    }

    public int getToolId()
    {
        return Integer.parseInt(_el.getAttribute("data-toolId"));
    }

    /** Absent for anyone who may not edit this tool. */
    public boolean hasSettingsMenu()
    {
        return Locators.settingsMenu.existsIn(this);
    }

    public boolean hasMenuItem(String item)
    {
        if (!hasSettingsMenu())
            return false;
        openSettingsMenu();
        return Locator.linkWithText(item).existsIn(elementCache().settingsMenu);
    }

    public SkylineToolDetailsPage clickToolName()
    {
        getWrapper().clickAndWait(elementCache().nameLink);
        return new SkylineToolDetailsPage(_driver);
    }

    public ToolUploadDialog clickUploadNewVersion()
    {
        clickMenuItem("Upload new version", ToolUploadDialog.DIALOG_ID);
        return ToolUploadDialog.forNewVersion(_driver);
    }

    public SupplementaryFileDialog clickUploadSupplementaryFile()
    {
        clickMenuItem("Upload supplementary file", SupplementaryFileDialog.DIALOG_ID);
        return new SupplementaryFileDialog(_driver);
    }

    public ManageToolOwnersDialog clickManageToolOwners()
    {
        clickMenuItem("Manage tool owners", ManageToolOwnersDialog.DIALOG_ID);
        return new ManageToolOwnersDialog(_driver);
    }

    public ConfirmDeleteDialog clickDelete()
    {
        clickMenuItem("Delete", ConfirmDeleteDialog.DELETE_TOOL_DIALOG_ID);
        return ConfirmDeleteDialog.deleteTool(_driver);
    }

    public ConfirmDeleteDialog clickDeleteLatestVersion()
    {
        clickMenuItem("Delete latest version", ConfirmDeleteDialog.DELETE_LATEST_DIALOG_ID);
        return ConfirmDeleteDialog.deleteLatestVersion(_driver);
    }

    /**
     * The menu toggles on click, not on hover, and slides open, so its items are in the DOM before
     * they can be clicked.
     */
    public void openSettingsMenu()
    {
        WebElement menu = elementCache().settingsMenu;
        getWrapper().scrollIntoView(menu);
        if (Locators.dropMenu.findElement(menu).isDisplayed())
            return;
        menu.click();
        getWrapper().shortWait().until(d -> Locators.dropMenu.findElement(menu).isDisplayed());
    }

    /**
     * Clicks a settings menu item and checks that the dialog it should open actually opened,
     * clicking once more if it did not. The page closes any open menu on a document click, so a
     * click can land in the gap between the item becoming clickable and the menu closing under it,
     * which leaves no dialog and no error. dialogId names what the item is meant to open.
     *
     * This is a stopgap. The real fix is to stop the menus animating, which is tracked separately.
     */
    private void clickMenuItem(String item, String dialogId)
    {
        clickMenuItemOnce(item);
        if (WebDriverWrapper.waitFor(() -> isDialogOpen(dialogId), WAIT_FOR_DIALOG))
            return;

        TestLogger.log("The '" + item + "' menu item opened no dialog. Clicking it again.");
        clickMenuItemOnce(item);
    }

    /**
     * The menu slides open, so an item is in the DOM and inside a displayed list before it has any
     * height of its own. Waiting for the item itself to be clickable is what makes this reliable.
     */
    private void clickMenuItemOnce(String item)
    {
        openSettingsMenu();
        WebElement link = Locator.linkWithText(item)
                .waitForElement(elementCache().settingsMenu, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        getWrapper().scrollIntoView(link);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(link));
        link.click();
    }

    /** Every dialog is in the page from the start, so being present is not the same as being open. */
    private boolean isDialogOpen(String dialogId)
    {
        WebElement dialog = Locator.id(dialogId).findElementOrNull(getDriver());
        return dialog != null && dialog.isDisplayed();
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
    }

    private static abstract class Locators
    {
        // The row holds a second menuMouseArea for the documentation drop-down, so the settings menu
        // has to be picked out by its own class rather than by being the first one.
        static final Locator.XPathLocator settingsMenu =
                Locator.tagWithClass("div", "menuMouseArea").withClass("sprocket");
        static final Locator.XPathLocator dropMenu = Locator.tagWithClass("ul", "dropMenu");
        static final Locator.XPathLocator nameLink =
                Locator.tagWithClass("span", "title").child("a");
    }
}
