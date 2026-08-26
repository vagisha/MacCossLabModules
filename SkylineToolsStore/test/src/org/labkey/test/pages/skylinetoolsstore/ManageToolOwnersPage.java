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
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

/**
 * The standalone Manage Tool Owners page.
 *
 * Nothing links here. SetOwnersAction is a FormViewAction, so a post carrying an address the server
 * does not recognise comes back as this page, with the submitted list still in the field. The tool id
 * rides in a hidden input rather than the url, so reloading this page fails.
 */
public class ManageToolOwnersPage extends LabKeyPage<ManageToolOwnersPage.ElementCache>
{
    public ManageToolOwnersPage(WebDriver driver)
    {
        super(driver);
    }

    public String getError()
    {
        return getText(Locator.tagWithClass("div", "labkey-error"));
    }

    /** Filled in by script from the submitted list, not by a value attribute on the input. */
    public String getOwners()
    {
        return getFormElement(Locator.id("toolOwners"));
    }

    /** The tool the resubmit will address. The page never shows it to the user. */
    public String getToolId()
    {
        return getAttribute(Locator.css("input[name='toolId']"), "value");
    }

    public void clickUpdate()
    {
        clickAndWait(Locator.lkButton("Update Tool Owners"));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
    }
}
