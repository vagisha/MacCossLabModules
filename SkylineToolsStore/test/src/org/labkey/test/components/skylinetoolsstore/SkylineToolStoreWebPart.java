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
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.pages.skylinetoolsstore.SkylineToolDetailsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Skyline Tool Store web part, which lists the latest version of every tool in the folders
 * below this one.
 *
 * Add New Tool is site admin only, so canAddTool is how a test asks rather than assuming.
 */
public class SkylineToolStoreWebPart extends BodyWebPart<SkylineToolStoreWebPart.ElementCache>
{
    public static final String DEFAULT_TITLE = "Skyline Tool Store";

    public SkylineToolStoreWebPart(WebDriver driver)
    {
        super(driver, DEFAULT_TITLE);
    }

    public SkylineToolStoreWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
    }

    public List<String> getToolNames()
    {
        return rows().stream().map(ToolRow::getName).collect(Collectors.toList());
    }

    public int getToolCount()
    {
        return rows().size();
    }

    public boolean hasTool(String toolName)
    {
        return getToolNames().contains(toolName);
    }

    public ToolRow getTool(String toolName)
    {
        return rows().stream()
                .filter(row -> toolName.equals(row.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No tool named '" + toolName +
                        "' in the store. Found " + getToolNames()));
    }

    public boolean canAddTool()
    {
        WebElement button = Locators.addNewTool.findElementOrNull(getComponentElement());
        return button != null && button.isDisplayed();
    }

    public ToolUploadDialog clickAddNewTool()
    {
        Locators.addNewTool.findElement(getComponentElement()).click();
        return ToolUploadDialog.forNewTool(getDriver());
    }

    /** Adds a tool and lands on its details page, which is where InsertToolAction redirects. */
    public SkylineToolDetailsPage addTool(File toolZip, String owners)
    {
        ToolUploadDialog dialog = clickAddNewTool().setToolZip(toolZip);
        if (owners != null)
            dialog.setOwners(owners);
        return dialog.clickUpload();
    }

    private List<ToolRow> rows()
    {
        return Locators.toolTable.findElements(getComponentElement()).stream()
                .map(el -> new ToolRow(el, getDriver()))
                .collect(Collectors.toList());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BodyWebPart<?>.ElementCache
    {
    }

    private static abstract class Locators
    {
        static final Locator.XPathLocator toolTable = Locator.tagWithClass("table", "tablewrap");
        static final Locator.IdLocator addNewTool = Locator.id("add-new-tool-btn");
    }
}
