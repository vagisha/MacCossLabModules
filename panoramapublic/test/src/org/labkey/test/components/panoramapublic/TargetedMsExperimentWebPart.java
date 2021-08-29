package org.labkey.test.components.panoramapublic;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.targetedms.CalibrationCurveWebpart;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.Assert.assertNotNull;

public class TargetedMsExperimentWebPart extends BodyWebPart <TargetedMsExperimentWebPart.ElementCache>
{
    public static final String DEFAULT_TITLE = "Targeted MS Experiment";
    private DataRegionTable _dataRegionTable;
    private BaseWebDriverTest _test;

    public TargetedMsExperimentWebPart(BaseWebDriverTest test)
    {
        this(test, 0);
    }

    public TargetedMsExperimentWebPart(BaseWebDriverTest test, int index)
    {
        super(test.getDriver(), DEFAULT_TITLE, index);
        _test = test;
    }

    public DataRegionTable getDataRegion()
    {
        if (_dataRegionTable == null)
            _dataRegionTable = DataRegionTable.DataRegion(_test.getDriver()).find(getComponentElement());
        return _dataRegionTable;
    }

    public TargetedMsExperimentInsertPage startInsert()
    {
        findElement(Locator.linkContainingText("Create New Experiment")).click();
        return new TargetedMsExperimentInsertPage(_test.getDriver());
    }

    public void clickSubmit()
    {
        getWrapper().clickAndWait(Locator.linkContainingText("Submit"));
    }

    public void clickResubmit()
    {
         Locator.XPathLocator resubmitLink = Locator.linkContainingText("Resubmit");
         assertNotNull("Expected to see a \"Resubmit\" button", resubmitLink);
         getWrapper().clickAndWait(resubmitLink);
    }

    public void clickMoreDetails()
    {
        Locator.XPathLocator moreDetailsLink = Locator.linkContainingText("More Details");
        assertNotNull("Expected to see a \"More Details\" link", moreDetailsLink);
        // WebElement test = new LazyWebElement(Locator.linkContainingText("More Details"), this);
        // test.click();
        getWrapper().clickAndWait(moreDetailsLink);
    }

    public void deleteExperiment(String title, String folderPath)
    {
//        Locator.XPathLocator deleteLink = getDataRegion().linkContainingText("Delete");
//        assertNotNull("Expected to see a \"Delete\" link", deleteLink);
//        getWrapper().clickAndWait(deleteLink);
//        getWrapper().assertTextPresent("Are you sure you want to delete the following experiment?");
//        getWrapper().assertTextPresent(title + " in " + folderPath);
//        findElement(Locator.linkContainingText("Create New Experiment")).click();
    }

    @Override
    protected TargetedMsExperimentWebPart.ElementCache newElementCache()
    {
        return new TargetedMsExperimentWebPart.ElementCache();
    }

    protected class ElementCache extends BodyWebPart.ElementCache
    {
        private final WebElement resubmitLink = Locator.linkContainingText("Resubmit").findWhenNeeded(this);
//        private final WebElement exportToPng = Locator.id("targetedmsCalibrationCurvefa-file-image-o").findWhenNeeded(this);
//        private final WebElement exportToPdf = Locator.id("targetedmsCalibrationCurvefa-file-pdf-o").findWhenNeeded(this);
//        private final WebElement svg = Locator.css("svg").findWhenNeeded(this);
//        private List<WebElement> findPoints()
//        {
//            return Locator.css("a.point").findElements(svg);
//        }
//        private List<WebElement> findLegendItems()
//        {
//            return Locator.css("g.legend > g.legend-item").findElements(svg);
//        }
    }
}
