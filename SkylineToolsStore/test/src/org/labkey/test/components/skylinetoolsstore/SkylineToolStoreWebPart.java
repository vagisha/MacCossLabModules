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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Skyline Tool Store web part, which lists the tools in one store folder.
 *
 * Every tool is a table carrying its row id, name, version, identifier and download count as data
 * attributes, which is what getTool matches on. Reading these from the page rather than from
 * getToolsApi is the point - the API is global and answers even when the listing is wrong.
 */
public class SkylineToolStoreWebPart extends BodyWebPart<SkylineToolStoreWebPart.ElementCache>
{
    public static final String DEFAULT_TITLE = "Skyline Tool Store";

    public SkylineToolStoreWebPart(WebDriver driver)
    {
        this(driver, 0);
    }

    public SkylineToolStoreWebPart(WebDriver driver, int index)
    {
        super(driver, DEFAULT_TITLE, index);
    }

    public List<String> getToolNames()
    {
        return Locators.toolTable.findElements(this).stream()
                .map(row -> row.getAttribute("data-toolName"))
                .collect(Collectors.toList());
    }

    public int getToolCount()
    {
        return Locators.toolTable.findElements(this).size();
    }

    public boolean hasTool(String toolName)
    {
        return Locators.tool(toolName).existsIn(this);
    }

    /**
     * @throws NoSuchElementException when the store is not listing that tool. A test that expects it
     *         to be there gets a failure naming the tool rather than a null to check for itself.
     */
    public ToolRow getTool(String toolName)
    {
        return new ToolRow(Locators.tool(toolName).findElement(this), getDriver());
    }

    /** Only a store admin gets the button, so this is also the permission check. */
    public boolean canAddTool()
    {
        return Locators.addToolButton.existsIn(getDriver());
    }

    public ToolUploadDialog clickAddNewTool()
    {
        elementCache().addToolButton.click();
        return ToolUploadDialog.forNewTool(getDriver());
    }

    /**
     * Adds a brand-new tool.
     *
     * @param owners comma separated, or null to leave the owners field alone
     * @return the new tool's details page, which is where InsertToolAction lands rather than back
     *         on the store listing
     */
    public SkylineToolDetailsPage addTool(File toolZip, String owners)
    {
        return clickAddNewTool().setToolZip(toolZip).setOwners(owners).clickUpload();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BodyWebPart<?>.ElementCache
    {
        // Outside the web part element - the button sits above it on the portal page.
        final WebElement addToolButton = Locators.addToolButton.findWhenNeeded(getDriver());
    }

    private static abstract class Locators
    {
        static final Locator.XPathLocator toolTable = Locator.tagWithClass("table", "tablewrap");
        static final Locator.IdLocator addToolButton = Locator.id("add-new-tool-btn");

        static Locator.XPathLocator tool(String toolName)
        {
            return toolTable.withAttribute("data-toolName", toolName);
        }
    }
}
