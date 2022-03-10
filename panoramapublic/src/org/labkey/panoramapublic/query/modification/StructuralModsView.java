package org.labkey.panoramapublic.query.modification;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryUrls;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ViewContext;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

public class StructuralModsView extends QueryView
{
    public static final String NAME = "Structural Modifications";
    public static final String QUERY_NAME = "StructuralModifications";

    private final ExperimentAnnotations _exptAnnotations;

    public StructuralModsView(ViewContext portalCtx)
    {
        this(portalCtx, null);
    }

    public StructuralModsView(ViewContext portalCtx, @Nullable ExperimentAnnotations exptAnnotations)
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
        setAllowableContainerFilterTypes(ContainerFilter.Type.Current, ContainerFilter.Type.CurrentAndSubfolders);
        setFrame(FrameType.PORTAL);
    }

    private QuerySettings createQuerySettings(ViewContext portalCtx)
    {
        QuerySettings settings = getSchema().getSettings(portalCtx, NAME, QUERY_NAME, null);
        if(_exptAnnotations != null)
        {
            settings.setContainerFilterName(_exptAnnotations.isIncludeSubfolders() ?
                    ContainerFilter.Type.CurrentAndSubfolders.name() : ContainerFilter.Type.Current.name());
            settings.setAllowChooseView(false);
            settings.setViewName("ExperimentStructuralModInfo");
        }

        setTitleHref(PageFlowUtil.urlProvider(QueryUrls.class).urlExecuteQuery(portalCtx.getContainer(), PanoramaPublicSchema.SCHEMA_NAME, QUERY_NAME));

        // Allow only folder admins to customize the view
        settings.setAllowCustomizeView(portalCtx.getContainer().hasPermission(portalCtx.getUser(), AdminPermission.class));

        return settings;
    }
}
