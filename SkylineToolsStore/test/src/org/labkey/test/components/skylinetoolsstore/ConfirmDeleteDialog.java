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
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * The two delete confirmations, "Delete" and "Delete latest version".
 *
 * Found by element id rather than by title, unlike the other dialogs here. ModalDialogFinder matches
 * a title by substring, and "Delete" is a substring of "Delete latest version", so a title lookup
 * can return either one.
 *
 * Both post over ajax and rewrite their own body, so a refusal leaves the dialog open with the
 * message in it. Neither can be dismissed while the post is in flight - they use a static backdrop
 * and their footer buttons are disabled for the duration.
 */
public class ConfirmDeleteDialog extends ModalDialog
{
    public static final String DELETE_TOOL_DIALOG_ID = "delToolAllDlg";
    public static final String DELETE_LATEST_DIALOG_ID = "delToolLatestDlg";

    private final String _dialogId;
    private final String _okButtonId;

    private ConfirmDeleteDialog(WebDriver driver, String dialogId, String okButtonId)
    {
        super(waitForOpenDialog(driver, dialogId), driver);
        _dialogId = dialogId;
        _okButtonId = okButtonId;
        waitForReady();
    }

    public static ConfirmDeleteDialog deleteTool(WebDriver driver)
    {
        return new ConfirmDeleteDialog(driver, DELETE_TOOL_DIALOG_ID, "delToolAllOk");
    }

    public static ConfirmDeleteDialog deleteLatestVersion(WebDriver driver)
    {
        return new ConfirmDeleteDialog(driver, DELETE_LATEST_DIALOG_ID, "delToolLatestOk");
    }

    /**
     * Every dialog is in the page from the start, so waiting for the element to exist proves
     * nothing. This waits for the one with this id to be shown.
     */
    private static WebElement waitForOpenDialog(WebDriver driver, String dialogId)
    {
        Locator.XPathLocator inner = Locator.id(dialogId)
                .append(Locator.tagWithClassContaining("div", "modal-dialog"));
        WebDriverWrapper.waitFor(() -> {
            WebElement outer = Locator.id(dialogId).findElementOrNull(driver);
            return outer != null && outer.isDisplayed();
        }, "The " + dialogId + " dialog did not open", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return inner.findElement(driver);
    }

    /** Written per tool when the dialog opens, so it names what is about to go. */
    public String getMessage()
    {
        return getBodyText();
    }

    /** Waits for the dialog to close, which is what says the delete was accepted. */
    public void confirm()
    {
        getWrapper().click(okButton());
        WebDriverWrapper.waitFor(() -> !isOpen(), "The delete dialog stayed open", 10_000);
    }

    /**
     * A refused delete comes back as an error view with status 200, so the ajax call succeeds and
     * the page rewrites the dialog body rather than closing it. That message is the only evidence.
     */
    public String confirmExpectingRefusal()
    {
        getWrapper().click(okButton());
        WebDriverWrapper.waitFor(() -> getBodyText().toLowerCase().contains("error"),
                "The delete dialog neither closed nor reported an error", 10_000);
        return getBodyText();
    }

    public void cancel()
    {
        dismiss("Cancel");
    }

    private boolean isOpen()
    {
        WebElement dialog = Locator.id(_dialogId).findElementOrNull(getDriver());
        return dialog != null && dialog.isDisplayed();
    }

    private Locator.XPathLocator okButton()
    {
        return Locator.tagWithId("button", _okButtonId);
    }
}
