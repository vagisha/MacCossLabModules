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

/** Uploads one supplementary file for a tool. Posts to the tool's own folder. */
public class SupplementaryFileDialog extends WebDriverComponent<SupplementaryFileDialog.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public SupplementaryFileDialog(WebDriver driver)
    {
        _driver = driver;
        _el = Locator.id("uploadSuppPop").waitForElement(driver, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        WebDriverWrapper.waitFor(_el::isDisplayed, "The supplementary file dialog did not open",
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

    public SupplementaryFileDialog setFile(File file)
    {
        // The dialog fades in, so it reports itself displayed before its fields can be typed into.
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().suppFile));
        getWrapper().setFormElement(elementCache().suppFile, file);
        return this;
    }

    /** The action redirects to the tool's details page. */
    public SkylineToolDetailsPage clickUpload()
    {
        getWrapper().clickAndWait(elementCache().submit);
        return new SkylineToolDetailsPage(_driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement suppFile = Locator.tagWithName("input", "suppFile").findWhenNeeded(this);
        final WebElement submit = Locator.tagWithAttribute("input", "type", "submit").findWhenNeeded(this);
    }
}
