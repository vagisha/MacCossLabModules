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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * The Delete and Delete latest version confirmations.
 *
 * Both are jQuery UI dialogs, so their Ok and Cancel buttons are not inside the div that carries
 * the id - jQuery UI moves that div into a wrapper and puts the buttons in a sibling button pane.
 * Every dialog on the page has a wrapper of its own, and the ones that are closed are still in the
 * document, so the buttons have to be reached through the wrapper that holds this dialog's id.
 * Matching on the button text alone finds the hidden dialogs' buttons too.
 *
 * These post over AJAX rather than navigating, so confirm() waits for the dialog to go away rather
 * than for a page load.
 */
public class ConfirmDeleteDialog extends WebDriverComponent<ConfirmDeleteDialog.ElementCache>
{
    public static final String DELETE_TOOL_DIALOG_ID = "delToolAllDlg";
    public static final String DELETE_LATEST_DIALOG_ID = "delToolLatestDlg";

    private final WebElement _el;
    private final WebDriver _driver;
    private final String _dialogId;

    private ConfirmDeleteDialog(WebDriver driver, String dialogId)
    {
        _driver = driver;
        _dialogId = dialogId;
        _el = Locator.id(dialogId).waitForElement(driver, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        WebDriverWrapper.waitFor(_el::isDisplayed, "The " + dialogId + " dialog did not open",
                WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public static ConfirmDeleteDialog deleteTool(WebDriver driver)
    {
        return new ConfirmDeleteDialog(driver, DELETE_TOOL_DIALOG_ID);
    }

    public static ConfirmDeleteDialog deleteLatestVersion(WebDriver driver)
    {
        return new ConfirmDeleteDialog(driver, DELETE_LATEST_DIALOG_ID);
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

    public String getMessage()
    {
        return _el.getText();
    }

    /** Confirms, then waits for the dialog to close. */
    public void confirm()
    {
        clickOk();
        WebDriverWrapper.waitFor(() -> !_el.isDisplayed(),
                "The delete confirmation stayed open. It shows the reason when the server refuses.",
                WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Confirms and expects the delete to be refused, returning what the dialog then says. The
     * actions render a refusal as an error view with status 200, so the dialog reports it in place
     * rather than the browser showing an error page.
     */
    public String confirmExpectingRefusal()
    {
        clickOk();
        // On a refusal the handler hides Ok and puts the reason in the dialog. Locator.button only
        // matches what is on show, so the button going missing is the condition to wait for - asking
        // a hidden one whether it is displayed would throw instead.
        WebDriverWrapper.waitFor(() -> Locators.dialogButton(_dialogId, "Ok")
                        .findOptionalElement(_driver).isEmpty(),
                "The delete was not refused - the dialog's Ok button is still on offer",
                WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return _el.getText().trim();
    }

    public void cancel()
    {
        button("Cancel").click();
    }

    /** The dialog fades in, so its buttons are on the page before they can be clicked. */
    private void clickOk()
    {
        WebElement ok = button("Ok");
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(ok));
        ok.click();
    }

    private WebElement button(String label)
    {
        return Locators.dialogButton(_dialogId, label).findElement(_driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
    }

    private static abstract class Locators
    {
        static Locator.XPathLocator dialogButton(String dialogId, String label)
        {
            return Locator.tagWithClassContaining("div", "ui-dialog")
                    .withDescendant(Locator.id(dialogId))
                    .descendant(Locator.button(label));
        }
    }
}
