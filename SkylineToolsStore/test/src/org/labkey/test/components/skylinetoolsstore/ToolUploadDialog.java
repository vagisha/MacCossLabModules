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
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.skylinetoolsstore.SkylineToolDetailsPage;
import org.openqa.selenium.WebDriver;

import java.io.File;

/**
 * The "Upload tool zip file" modal. One dialog serves both Add New Tool and a per-tool Upload new
 * version, and the handler that opens it sets the form action, so the two are told apart by what
 * the caller does rather than by the markup.
 */
public class ToolUploadDialog extends ModalDialog
{
    public static final String DIALOG_ID = "uploadPop";
    private static final String TITLE = "Upload tool zip file";

    private ToolUploadDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle(TITLE));
    }

    public static ToolUploadDialog forNewTool(WebDriver driver)
    {
        return new ToolUploadDialog(driver);
    }

    public static ToolUploadDialog forNewVersion(WebDriver driver)
    {
        return new ToolUploadDialog(driver);
    }

    public ToolUploadDialog setToolZip(File toolZip)
    {
        getWrapper().setFormElement(field("toolZip"), toolZip);
        return this;
    }

    public ToolUploadDialog setOwners(String owners)
    {
        getWrapper().setFormElement(field("toolOwners"), owners);
        return this;
    }

    /** Both actions redirect to the details page of the tool they wrote. */
    public SkylineToolDetailsPage clickUpload()
    {
        getWrapper().clickAndWait(submitButton());
        return new SkylineToolDetailsPage(getDriver());
    }

    private Locator.CssLocator field(String name)
    {
        return Locator.css("#" + DIALOG_ID + " [name='" + name + "']");
    }

    private Locator.CssLocator submitButton()
    {
        return Locator.css("#" + DIALOG_ID + " button[type='submit']");
    }
}
