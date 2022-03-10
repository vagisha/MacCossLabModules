package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.view.ViewContext;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;
import org.labkey.panoramapublic.query.SpecLibInfoManager;

import java.util.List;

public class ExperimentStructuralModsView extends QueryView
{
    public static final String NAME = "Experiment Structural Modifications";
    public static final String QUERY_NAME = "ExperimentStructuralModifications";

    private final ExperimentAnnotations _exptAnnotations;

    public ExperimentStructuralModsView(ViewContext portalCtx)
    {
        this(portalCtx, ExperimentAnnotationsManager.getExperimentInContainer(portalCtx.getContainer()));
    }

    public ExperimentStructuralModsView(ViewContext portalCtx, ExperimentAnnotations exptAnnotations)
    {
        super(new PanoramaPublicSchema(portalCtx.getUser(), portalCtx.getContainer()));
        _exptAnnotations = exptAnnotations;
        setTitle(NAME);
        setSettings(createQuerySettings(portalCtx));
        setShowDetailsColumn(false);
        setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
        setShowExportButtons(false);
        setShowBorders(true);
        setShadeAlternatingRows(true);
        setAllowableContainerFilterTypes();
        setFrame(FrameType.PORTAL);
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx)
    {
//        String viewName = _exptAnnotations != null &&
//                SpecLibInfoManager.getForExperiment(_exptAnnotations.getId(), portalCtx.getContainer()).size() > 0 ?
//                "SpectralLibrariesInfo" : null;

        QuerySettings settings = getSchema().getSettings(portalCtx, NAME, QUERY_NAME, null);
        settings.setContainerFilterName(_exptAnnotations != null && _exptAnnotations.isIncludeSubfolders() ?
                ContainerFilter.Type.CurrentAndSubfolders.name() : ContainerFilter.Type.Current.name());

        // Allow only folder admins to customize the view
        settings.setAllowCustomizeView(portalCtx.getContainer().hasPermission(portalCtx.getUser(), AdminPermission.class));

        return settings;
    }

    @Override
    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> displayCols = super.getDisplayColumns();
        if (_exptAnnotations != null && _exptAnnotations.getContainer().hasPermission(getUser(), UpdatePermission.class))
        {
            TableInfo table = getTable();
            if (table != null)
            {
                var col = table.getColumn(FieldKey.fromParts("UnimodAssignment")).getRenderer();
                displayCols.add(col);
            }
        }
        return displayCols;
    }
}
