package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ContainerFilter;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.query.ContainerJoin;
import org.labkey.panoramapublic.query.PanoramaPublicTable;

public class ExperimentIsotopicModInfoTableInfo extends PanoramaPublicTable
{
    public ExperimentIsotopicModInfoTableInfo(PanoramaPublicSchema schema, ContainerFilter cf)
    {
        super(PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), schema, cf, ContainerJoin.ExpAnnotJoin);

//        var runIdsCol = getMutableColumn("RunIds");
//        if (runIdsCol != null)
//        {
//            runIdsCol.setDisplayColumnFactory(new ModificationDocsDisplayColumnFactory(true));
//        }
//
        var unimodCol = addColumn(wrapColumn("Unimod", getRealTable().getColumn("UnimodId")));
        // guideSetCol.setFk(QueryForeignKey.from(schema, cf).to("GuideSet", "RowId", "AnalyteName"));
//        var unimodIdCol = getMutableColumn("UnimodId");
        if (unimodCol != null)
        {
            unimodCol.setTextAlign("left");
            unimodCol.setDisplayColumnFactory(new UnimodIdDisplayColumnFactory(true));
        }
    }
}
