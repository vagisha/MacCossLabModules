package org.labkey.test.pages.panoramapublic;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class DataValidationPage extends LabKeyPage<DataValidationPage.ElementCache>
{
    public DataValidationPage(WebDriver driver)
    {
        super(driver);
        waitForElement(Locators.bodyTitle().withText("Data Validation Status"));
        assertTextNotPresent("Could not find job status for job");
        waitForTextToDisappear("This page will automatically refresh", WAIT_FOR_PAGE);
        expandValidationRows();
    }

    private void expandValidationRows()
    {
        var gridRowExapnder = Locator.XPathLocator.tagWithClass("div", "x4-grid-row-expander");
        var els = gridRowExapnder.findElements(getDriver());
        assertTrue("Expected to find grid expander elements", els.size() > 0);
        for (var el: els)
        {
            el.click(); // Expand all the rows
        }
    }

    public void checkSampleFileStatus(List<String> found, List<String> missing)
    {
        if (missing.size() > 0)
        {
            assertTextPresent("The data cannot be assigned a ProteomeXchange ID");
            assertTextPresent("Missing raw data files");
        }
        var sampleFilesTableLoc = Locator.XPathLocator.tagWithClass("table", "sample-files-status");
        assertTrue("Expected to find sample file status table", sampleFilesTableLoc.findElements(getDriver()).size() > 0);
        for (var file: missing)
        {
            checkFileStatus(sampleFilesTableLoc, file, true);
        }
        for (var file: found)
        {
            checkFileStatus(sampleFilesTableLoc, file, false);
        }
    }

    private void checkFileStatus(Locator.XPathLocator sampleFilesTableLoc, String file, boolean missing)
    {
        Locator cell = sampleFilesTableLoc.child("tbody").child("tr")
                .child(Locator.tag("td").withText(file))
                .followingSibling("td")
                .child(Locator.tag("span").withClass(missing ? "invalid" : "valid"));
        cell.findElement(getDriver());
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
    }
}
