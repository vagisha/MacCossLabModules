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
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

/**
 * The zip upload dialog, which serves both "Add New Tool" and a tool's "Upload new version".
 *
 * One dialog in the page does both jobs. Which action it posts to, and whether the tool owners
 * field is on show, is set by the menu handler that opened it, so a test that wants to prove the
 * form posts what the action binds has to go through here rather than build the post itself.
 */
public class ToolUploadDialog extends WebDriverComponent<ToolUploadDialog.ElementCache>
{
    public static final String DIALOG_ID = "uploadPop";

    private final WebElement _el;
    private final WebDriver _driver;
    private final boolean _newTool;

    private ToolUploadDialog(WebDriver driver, boolean newTool)
    {
        _driver = driver;
        _newTool = newTool;
        _el = Locator.id(DIALOG_ID).waitForElement(driver, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        WebDriverWrapper.waitFor(_el::isDisplayed, "The tool upload dialog did not open",
                WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public static ToolUploadDialog forNewTool(WebDriver driver)
    {
        return new ToolUploadDialog(driver, true);
    }

    public static ToolUploadDialog forNewVersion(WebDriver driver)
    {
        return new ToolUploadDialog(driver, false);
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

    public ToolUploadDialog setToolZip(File toolZip)
    {
        // The dialog fades in, so it reports itself displayed before its fields can be typed into.
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().toolZip));
        getWrapper().setFormElement(elementCache().toolZip, toolZip);
        return this;
    }

    /**
     * @param owners comma separated, or null to leave the field alone. Only offered when adding a
     *               new tool, and then only to a store admin - a new version inherits its owners.
     */
    public ToolUploadDialog setOwners(String owners)
    {
        if (owners != null)
            getWrapper().setFormElement(elementCache().owners, owners);
        return this;
    }

    /** True when the tool owners field is on show, which is the store admin's view of a new tool. */
    public boolean hasOwnersField()
    {
        return _newTool && Locators.owners.findOptionalElement(this)
                .map(WebElement::isDisplayed).orElse(false);
    }

    /**
     * Submits and waits for the page that follows.
     *
     * Both actions redirect to the details page of the version just stored, so that is what comes
     * back even when the upload started from the store listing.
     */
    public SkylineToolDetailsPage clickUpload()
    {
        getWrapper().clickAndWait(elementCache().submit);
        return new SkylineToolDetailsPage(_driver);
    }

    /**
     * Submits expecting the action to refuse, and returns what it said.
     *
     * A refused upload redraws the form with the reason in a labkey-error block rather than
     * redirecting, so there is no details page to come back to.
     */
    public String clickUploadExpectingError()
    {
        getWrapper().clickAndWait(elementCache().submit);
        return Locator.byClass("labkey-error")
                .waitForElement(_driver, WebDriverWrapper.WAIT_FOR_PAGE)
                .getText().trim();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement toolZip = Locators.toolZip.findWhenNeeded(this);
        final WebElement owners = Locators.owners.findWhenNeeded(this);
        final WebElement submit = Locators.submit.findWhenNeeded(this);
    }

    private static abstract class Locators
    {
        static final Locator.XPathLocator toolZip = Locator.tagWithName("input", "toolZip");
        static final Locator.IdLocator owners = Locator.id("toolOwnersNew");
        static final Locator.XPathLocator submit = Locator.tagWithAttribute("input", "type", "submit");
    }
}
