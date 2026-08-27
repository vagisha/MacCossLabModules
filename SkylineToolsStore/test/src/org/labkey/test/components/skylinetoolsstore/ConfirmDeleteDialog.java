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
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * The two delete confirmations, "Delete" and "Delete latest version".
 *
 * Found by element id rather than by title, unlike the other dialogs here. ModalDialogFinder matches
 * a title by substring, and "Delete" is a substring of "Delete latest version", so a title lookup
 * can return either one.
 *
 * Both pages post over ajax, and what a confirm does next depends on how the server responded.
 *
 * <ul>
 * <li>Accepted, and the handler navigates to the successUrl the reply carries - confirmExpectingPageLoad</li>
 * <li>Refused, and the dialog stays open with the reason in its body - confirmExpectingRefusal</li>
 * </ul>
 *
 * Neither dialog can be dismissed while the post is in flight. They use a static backdrop and their
 * footer buttons are disabled for the duration.
 */
public class ConfirmDeleteDialog extends ModalDialog
{
    public static final String DELETE_TOOL_DIALOG_ID = "delToolAllDlg";
    public static final String DELETE_LATEST_DIALOG_ID = "delToolLatestDlg";

    private final String _okButtonId;

    private ConfirmDeleteDialog(WebDriver driver, String dialogId, String okButtonId)
    {
        super(waitForOpenDialog(driver, dialogId), driver);
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
     * Every dialog is in the page from the start, so waiting for the element to exist proves nothing.
     * This waits for the one with this id to be shown.
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

    /** The dialog body. It holds the confirmation question, or the error message after a refusal. */
    public String getMessage()
    {
        return getBodyText();
    }

    /**
     * Confirms a delete and waits for the successUrl page to load.
     *
     * The handler sets window.location inside .done(), so the browser leaves the page rather than the
     * dialog closing. Waiting for the dialog to go would pass at unload, which says nothing about the
     * new page having loaded.
     */
    public void confirmExpectingPageLoad()
    {
        getWrapper().clickAndWait(okButton());
    }

    /**
     * Clicks Ok on a delete the server will refuse, and returns the reason shown in the dialog.
     */
    public String confirmExpectingRefusal()
    {
        getWrapper().click(okButton());
        // If the server returns an error message, showModalError hides the Ok button and displays the refusal.
        // If the deletion was accepted, the handler navigated and the wait below will time out. The catch
        // covers that case, where the button can go stale under the wait as the page unloads.
        WebDriverWrapper.waitFor(() -> {
            try
            {
                WebElement ok = okButton().findElementOrNull(getDriver());
                return ok != null && !ok.isDisplayed();
            }
            catch (StaleElementReferenceException ignored)
            {
                return false;
            }
        }, "The delete dialog neither closed nor reported a refusal", 10_000);
        return getBodyText();
    }

    public void cancel()
    {
        dismiss("Cancel");
    }

    /**
     * Whether the Ok button is on offer. Showing a refusal hides it. Closing the dialog puts it back.
     * This is how a reopened dialog is checked.
     */
    public boolean isConfirmOffered()
    {
        WebElement ok = okButton().findElementOrNull(getDriver());
        return ok != null && ok.isDisplayed();
    }

    private Locator.XPathLocator okButton()
    {
        return Locator.tagWithId("button", _okButtonId);
    }
}
