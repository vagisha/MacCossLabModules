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
import org.labkey.test.pages.skylinetoolsstore.ManageToolOwnersPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Replaces the set of users holding Editor on a tool's folder.
 *
 * The dialog prefills with the current owners, so getOwners is how a test reads them back without
 * going to the security API.
 */
public class ManageToolOwnersDialog extends WebDriverComponent<ManageToolOwnersDialog.ElementCache>
{
    public static final String DIALOG_ID = "manageOwnersPop";

    private final WebElement _el;
    private final WebDriver _driver;

    public ManageToolOwnersDialog(WebDriver driver)
    {
        _driver = driver;
        _el = Locator.id(DIALOG_ID).waitForElement(driver, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        WebDriverWrapper.waitFor(_el::isDisplayed, "The manage tool owners dialog did not open",
                WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
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

    /** What the dialog prefilled, which is the tool's current owners. */
    public String getOwners()
    {
        return elementCache().owners.getAttribute("value");
    }

    public ManageToolOwnersDialog setOwners(String owners)
    {
        getWrapper().setFormElement(elementCache().owners, owners);
        return this;
    }

    public void clickUpdate()
    {
        getWrapper().clickAndWait(elementCache().submit);
    }

    /**
     * Submits expecting the action to refuse, which redraws the standalone form rather than
     * returning to where the dialog was opened from.
     */
    public ManageToolOwnersPage clickUpdateExpectingError()
    {
        getWrapper().clickAndWait(elementCache().submit);
        return new ManageToolOwnersPage(_driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement owners = Locator.id("toolOwnersManage").findWhenNeeded(this);
        final WebElement submit = Locator.tagWithAttribute("input", "type", "submit").findWhenNeeded(this);
    }
}
