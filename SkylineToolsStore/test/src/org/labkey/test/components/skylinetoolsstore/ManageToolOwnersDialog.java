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
import org.labkey.test.pages.skylinetoolsstore.ManageToolOwnersPage;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * The "Manage tool owners" modal. Rendered for a site admin only, so a test that expects it must
 * be signed in as one.
 *
 * The owners field carries the store's type-ahead. Picking from it appends to the comma separated
 * list rather than replacing it. setOwners works around that.
 */
public class ManageToolOwnersDialog extends ModalDialog
{
    public static final String DIALOG_ID = "manageOwnersPop";
    private static final String TITLE = "Manage tool owners";

    public ManageToolOwnersDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle(TITLE));
    }

    public String getOwners()
    {
        return getWrapper().getFormElement(ownersField());
    }

    /**
     * Replaces the whole list. The field's type-ahead opens on input, so the menu is dismissed
     * afterwards to stop it covering the footer buttons.
     */
    public ManageToolOwnersDialog setOwners(String owners)
    {
        getWrapper().setFormElement(ownersField(), owners);
        dismissTypeAhead();
        return this;
    }

    /**
     * Types a partial address and leaves the type-ahead open. Use this to drive the menu itself rather
     * than to fill the field.
     */
    public ManageToolOwnersDialog typeOwner(String term)
    {
        getWrapper().setFormElement(ownersField(), term);
        return this;
    }

    /**
     * Types onto the end of the field rather than replacing it, the way a user carries on after
     * choose has left a separator.
     */
    public ManageToolOwnersDialog appendOwner(String term)
    {
        ownersField().findElement(getDriver()).sendKeys(term);
        return this;
    }

    /**
     * Picks an address the type-ahead is offering. The menu is drawn hidden until it has something
     * to show, so this waits for the option to be visible rather than merely present.
     */
    public ManageToolOwnersDialog clickTypeAheadOption(String address)
    {
        Locator.XPathLocator option = typeAheadOption(address).notHidden();
        getWrapper().waitForElement(option);
        getWrapper().click(option);
        return this;
    }

    /**
     * Moves the highlight down the suggestion list and opens it first if it is closed. One press
     * highlights an entry either way.
     */
    public ManageToolOwnersDialog pressDown()
    {
        ownersField().findElement(getDriver()).sendKeys(Keys.ARROW_DOWN);
        return this;
    }

    /** Tab completes the highlighted address rather than moving focus, as jQuery UI did. */
    public ManageToolOwnersDialog pressTab()
    {
        ownersField().findElement(getDriver()).sendKeys(Keys.TAB);
        return this;
    }

    /** Moves the highlight up, opening a closed list first. One press highlights the last entry. */
    public ManageToolOwnersDialog pressUp()
    {
        ownersField().findElement(getDriver()).sendKeys(Keys.ARROW_UP);
        return this;
    }

    /** Shift+Tab leaves the field backwards and must never complete an address. */
    public ManageToolOwnersDialog pressShiftTab()
    {
        ownersField().findElement(getDriver()).sendKeys(Keys.chord(Keys.SHIFT, Keys.TAB));
        return this;
    }

    /** Which address the suggestion list is currently highlighting, or null for none. */
    public String getHighlightedOption()
    {
        WebElement active = Locator.css("#" + DIALOG_ID + " ul.autocompleteMenu li.active a")
                .findElementOrNull(getDriver());
        return active == null ? null : active.getText();
    }

    /**
     * Sends Escape to the owners field, where the type-ahead listens. One press closes the suggestion
     * list. A second closes the dialog.
     */
    public ManageToolOwnersDialog pressEscape()
    {
        ownersField().findElement(getDriver()).sendKeys(Keys.ESCAPE);
        return this;
    }

    /** The dialog itself, as opposed to the type-ahead inside it. */
    public boolean isOpen()
    {
        WebElement dialog = Locator.id(DIALOG_ID).findElementOrNull(getDriver());
        return dialog != null && dialog.isDisplayed();
    }

    /** Opens on typing and on either arrow key. Focusing the field alone does not open it. */
    public boolean isTypeAheadShowing()
    {
        WebElement menu = Locator.css("#" + DIALOG_ID + " ul.autocompleteMenu")
                .findElementOrNull(getDriver());
        return menu != null && menu.isDisplayed();
    }

    /** Addresses the type-ahead is currently offering. */
    public List<String> getTypeAheadOptions()
    {
        return getWrapper().getTexts(Locator.css("#" + DIALOG_ID + " ul.autocompleteMenu li a")
                .findElements(getDriver()));
    }

    public void clickUpdate()
    {
        getWrapper().clickAndWait(submitButton());
    }

    /**
     * SetOwnersAction is a FormViewAction, so an address it does not recognise re-renders its own
     * page with the error rather than returning to the dialog.
     */
    public ManageToolOwnersPage clickUpdateExpectingError()
    {
        getWrapper().clickAndWait(submitButton());
        return new ManageToolOwnersPage(getDriver());
    }

    private Locator.CssLocator ownersField()
    {
        return Locator.css("#" + DIALOG_ID + " [name='toolOwners']");
    }

    /** Scoped to this dialog. The web part attaches one to the Add New Tool dialog on the same page. */
    private Locator.XPathLocator typeAheadOption(String address)
    {
        return Locator.id(DIALOG_ID)
                .append(Locator.tagWithClass("ul", "autocompleteMenu"))
                .child("li").child(Locator.linkWithText(address));
    }

    private Locator.CssLocator submitButton()
    {
        return Locator.css("#" + DIALOG_ID + " button[type='submit']");
    }

    private void dismissTypeAhead()
    {
        if (!isTypeAheadShowing())
            return;
        getWrapper().executeScript("document.body.click();");
        WebDriverWrapper.waitFor(() -> !isTypeAheadShowing(),
                "The suggestion list stayed open over the dialog footer", 5_000);
    }
}
