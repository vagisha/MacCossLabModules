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
 * The "Upload supplementary file" modal. On the web part one dialog serves every row, so the
 * handler that opens it sets both the form action and the tool id.
 */
public class SupplementaryFileDialog extends ModalDialog
{
    public static final String DIALOG_ID = "uploadSuppPop";
    private static final String TITLE = "Upload supplementary file";

    public SupplementaryFileDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle(TITLE));
    }

    public SupplementaryFileDialog setFile(File file)
    {
        getWrapper().setFormElement(Locator.css("#" + DIALOG_ID + " [name='suppFile']"), file);
        return this;
    }

    public SkylineToolDetailsPage clickUpload()
    {
        getWrapper().clickAndWait(Locator.css("#" + DIALOG_ID + " button[type='submit']"));
        return new SkylineToolDetailsPage(getDriver());
    }

    /** The tool the dialog is currently addressed to, set per row when it opens. */
    public String getToolId()
    {
        return getWrapper().getFormElement(Locator.id("suppFormToolId"));
    }
}
