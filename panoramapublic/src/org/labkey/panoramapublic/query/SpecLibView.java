package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpecLibView extends QueryView
{
    public static final String NAME = "Spectral Libraries";
    public static final String QUERY_NAME = "SpectralLibraries";

    private final ExperimentAnnotations _exptAnnotations;

    public SpecLibView(ViewContext portalCtx)
    {
        this(portalCtx, null);
    }

    public SpecLibView(ViewContext portalCtx, ExperimentAnnotations exptAnnotations)
    {
        super(new PanoramaPublicSchema(portalCtx.getUser(), portalCtx.getContainer()));
        _exptAnnotations = exptAnnotations;

        setTitle(NAME);
        setSettings(createQuerySettings(portalCtx, NAME));
        setShowDetailsColumn(false);
        setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
        setShowExportButtons(false);
        setShowBorders(true);
        setShadeAlternatingRows(true);

        setAllowableContainerFilterTypes(ContainerFilter.Type.Current, ContainerFilter.Type.CurrentAndSubfolders);

        setFrame(FrameType.PORTAL);

//        TableInfo
//        List<FieldKey> cols = new ArrayList<>(table.getDefaultVisibleColumns());
//        for (ColumnInfo col : QueryService.get().getColumns(getTable(), cols).values())
//        {
//            DisplayColumn displayCol = col.getRenderer();
//            displayCols.add(displayCol);
//        }
    }

    @Override
    protected void setupDataView(DataView ret)
    {
        super.setupDataView(ret);
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx, String dataRegionName)
    {
        QuerySettings settings = getSchema().getSettings(portalCtx, dataRegionName, QUERY_NAME);
        if(settings.getContainerFilterName() == null && _exptAnnotations != null)
        {
            settings.setContainerFilterName(_exptAnnotations.isIncludeSubfolders() ?
                    ContainerFilter.Type.CurrentAndSubfolders.name() : ContainerFilter.Type.Current.name());
        }
//        portalCtx.getus
//        QueryDefinition qdef = settings.getQueryDef(getSchema());
//        QueryService.get().getColumns(getTable(), FieldKey.fromParts("RunIds"));
//        CustomView customView = qdef.createCustomView();
//        addDetailsAndUpdateColumns();
        return settings;
    }

    @Override
    public List<DisplayColumn> getDisplayColumns()
    {
        TableInfo table = getTable();
        if (table == null)
        {
            return Collections.emptyList();
        }


//        List<DisplayColumn> displayCols = new ArrayList<>();
//        for (ColumnInfo col : QueryService.get().getColumns(table, table.getDefaultVisibleColumns()).values())
//        {
//            if (col.getName().equalsIgnoreCase("RunIds"))
//            {
//                if (col.)
//                col.setDisplayColumnFactory(new LibraryDocumentsDisplayColumnFactory(_exptAnnotations));
//            }
//            DisplayColumn displayCol = col.getRenderer();
//            displayCols.add(displayCol);
//        }
//
//        Map<FieldKey, ColumnInfo> columns = QueryService.get().getColumns(getTable(), table.getDefaultVisibleColumns());


        List<DisplayColumn> displayCols = new ArrayList<>();
        for (DisplayColumn col : super.getDisplayColumns())
        {
            if (col.getName().equalsIgnoreCase("FileNameHint"))
            {
                col.setName("File Name");
                // displayCols.add(new AliasedColumn("File Name", col.getColumnInfo()).getRenderer());
            }
            else if (col.getName().equalsIgnoreCase("SkylineDocuments"))
            {
//                var skyDocsCol = new AliasedColumn("Skyline Documents", table.getColumn(FieldKey.fromParts("SkylineDocuments")));
//                skyDocsCol.setDisplayColumnFactory(new LibraryDocumentsDisplayColumnFactory(_exptAnnotations));
//                displayCols.add(skyDocsCol.getRenderer());
            }
            displayCols.add(col);
//            else
//            {
//                displayCols.add(col);
//            }
        }

//        if (_exptAnnotations != null)
//        {
//            cols.add(0, new SimpleDisplayColumn("Details")
//            {
//                @Override
//                public String renderURL(RenderContext ctx)
//                {
//                    String containerId = (String) ctx.getRow().get("Container");
//                    Long specimenId = (Long) ctx.getRow().get("RowId");
//                    ActionURL url = getHistoryLinkURL(ctx.getViewContext(), containerId).addParameter("id", specimenId.toString()).addParameter("selected", _participantVisitFiltered);
//                    return url.toString();
//                }
//            });
//        }

//        ColumnInfo fileNameCol = new AliasedColumn("File Name",table.getColumn(FieldKey.fromParts("FileNameHint")));




        List<DisplayColumn> fromQueryDef = getQueryDef().getDisplayColumns(null, table);
        // displayCols.addAll(getQueryDef().getDisplayColumns(null, table));
        //displayCols.add(table.getColumn(FieldKey.fromParts("Name")).getRenderer());
//        displayCols.add(table.getColumn(FieldKey.fromParts("Name")).getRenderer());
//        displayCols.add(fileNameCol.getRenderer());
//        displayCols.add(runIdsCol.getRenderer());
//        for (ColumnInfo col : QueryService.get().getColumns(table, cols).values())
//        {
//            DisplayColumn displayCol = col.getRenderer();
//            displayCols.add(displayCol);
//        }
        return displayCols;
    }
}
