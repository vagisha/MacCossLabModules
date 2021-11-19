package org.labkey.panoramapublic.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.view.ViewContext;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.List;

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

        if (_exptAnnotations != null && _exptAnnotations.isIncludeSubfolders())
        {
            setAllowableContainerFilterTypes(ContainerFilter.Type.Current, ContainerFilter.Type.CurrentAndSubfolders);
        }
        else
        {
            setAllowableContainerFilterTypes(ContainerFilter.Type.Current);
        }

        setFrame(FrameType.PORTAL);
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx, String dataRegionName)
    {
        String viewName = _exptAnnotations != null &&
                SpecLibInfoManager.getForExperiment(_exptAnnotations.getId(), portalCtx.getContainer()).size() > 0 ?
                "SpectralLibrariesInfo" : null;

        QuerySettings settings = getSchema().getSettings(portalCtx, dataRegionName, QUERY_NAME, viewName);

        if(settings.getContainerFilterName() == null && _exptAnnotations != null)
        {
            settings.setContainerFilterName(_exptAnnotations.isIncludeSubfolders() ?
                    ContainerFilter.Type.CurrentAndSubfolders.name() : ContainerFilter.Type.Current.name());
        }
        // Allow only folder admins to customize the view
        settings.setAllowCustomizeView(portalCtx.getContainer().hasPermission(portalCtx.getUser(), AdminPermission.class));

        return settings;
    }

    @Override
    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> displayCols = super.getDisplayColumns();
        if (_exptAnnotations != null && !_exptAnnotations.isJournalCopy())
        {
            TableInfo table = getTable();
            if (table != null)
            {
                var col = table.getColumn(FieldKey.fromParts("Details")).getRenderer();
                displayCols.add(col);
            }
        }

        return displayCols;
    }
}
