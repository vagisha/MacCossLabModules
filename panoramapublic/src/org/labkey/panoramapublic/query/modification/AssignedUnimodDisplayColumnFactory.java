package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.targetedms.IModification;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.DOM;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.PanoramaPublicController;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.proteomexchange.UnimodModification;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;
import org.labkey.panoramapublic.query.ModificationInfoManager;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.B;
import static org.labkey.api.util.DOM.DIV;
import static org.labkey.api.util.DOM.SPAN;
import static org.labkey.api.util.DOM.at;
import static org.labkey.api.util.DOM.cl;

public abstract class AssignedUnimodDisplayColumnFactory<T extends ExperimentModInfo> implements DisplayColumnFactory
{
    private static final FieldKey MOD_ID = FieldKey.fromParts("ModId");
    private static final FieldKey MOD_INFO_ID = FieldKey.fromParts("ModInfoId");

    abstract ActionURL getMatchToUnimodAction(RenderContext ctx);
    abstract ActionURL getDeleteAction(RenderContext ctx);
    abstract T getModInfo(int modInfoId);
    abstract IModification getModification(long dbModId);

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out)
            {
                Integer unimodId = ctx.get(colInfo.getFieldKey(), Integer.class);

                if (unimodId != null)
                {
                    Integer modInfoId = ctx.get(MOD_INFO_ID, Integer.class);
                    if (modInfoId == null)
                    {
                        // This is the Unimod Id from the Skyline document
                        UnimodModification.getLink(unimodId).appendTo(out);
                    }
                    else
                    {
                        var modInfo = getModInfo(modInfoId);
                        DIV(getUnimodDetails(modInfo)).appendTo(out);

                        int exptId = modInfo.getExperimentAnnotationsId();
                        var dbMod = getModification(modInfo.getModId());
                        ActionURL deleteUrl = getDeleteAction(ctx).addParameter("id", exptId).addParameter("modInfoId", modInfoId);
                        DIV(at(style, "margin-top:5px;"), new Link.LinkBuilder("[Delete]")
                                .href(deleteUrl)
                                .clearClasses().addClass("labkey-error")
                                .usePost("Are you sure you want to delete the saved Unimod information for modification " + dbMod.getName() + "?")
                                .build())
                                .appendTo(out);
                    }
                }
                else
                {
                    ExperimentAnnotations exptAnnotations = ExperimentAnnotationsManager.getExperimentInContainer(ctx.getContainer());
                    Integer exptId = exptAnnotations != null ? exptAnnotations.getId() : null;

                    Long modId = ctx.get(MOD_ID, Long.class);

                    if (modId != null && exptId != null)
                    {
                        var url = getMatchToUnimodAction(ctx).addParameter("id", exptId).addParameter("modificationId", modId);
                        url.addReturnURL(ctx.getViewContext().getActionURL());
                        var findMatchLink = new Link.LinkBuilder("Find Match").href(url);
                        DIV(cl("alert-warning"), findMatchLink).appendTo(out);
                    }
                }
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(MOD_ID);
                keys.add(MOD_INFO_ID);
            }
        };
    }

    protected List<DOM.Renderable> getUnimodDetails(T modInfo)
    {
        return List.of(SPAN("**"), UnimodModification.getLink(modInfo.getUnimodId(), true), HtmlString.NBSP, SPAN("(" + modInfo.getUnimodName() + ")"));
    }

    public static class AssignedStructuralUnimod extends AssignedUnimodDisplayColumnFactory<ExperimentStructuralModInfo>
    {
        @Override
        ActionURL getMatchToUnimodAction(RenderContext ctx)
        {
            return new ActionURL(PanoramaPublicController.MatchToUnimodStructuralAction.class, ctx.getContainer());
        }

        @Override
        ActionURL getDeleteAction(RenderContext ctx)
        {
            return new ActionURL(PanoramaPublicController.DeleteStructuralModInfoAction.class, ctx.getContainer());
        }

        @Override
        ExperimentStructuralModInfo getModInfo(int modInfoId)
        {
            return ModificationInfoManager.getStructuralModInfo(modInfoId);
        }

        @Override
        IModification getModification(long dbModId)
        {
            return TargetedMSService.get().getStructuralModification(dbModId);
        }

        @Override
        protected List<DOM.Renderable> getUnimodDetails(ExperimentStructuralModInfo modInfo)
        {
            List<DOM.Renderable> list = new ArrayList<>(super.getUnimodDetails(modInfo));
            if (modInfo.isCombinationMod())
            {
                list.add(SPAN(at(style, "margin:0 10px 0 10px;"), B("+")));
                list.add(UnimodModification.getLink(modInfo.getUnimodId2(), true));
                list.add(HtmlString.NBSP);
                list.add(SPAN("(" + modInfo.getUnimodName2() + ")"));
            }
            return list;
        }
    }

    public static class AssignedIsotopeUnimod extends AssignedUnimodDisplayColumnFactory<ExperimentModInfo>
    {
        @Override
        ActionURL getMatchToUnimodAction(RenderContext ctx)
        {
            return new ActionURL(PanoramaPublicController.MatchToUnimodIsotopeAction.class, ctx.getContainer());
        }

        @Override
        ActionURL getDeleteAction(RenderContext ctx)
        {
            return new ActionURL(PanoramaPublicController.DeleteIsotopeModInfoAction.class, ctx.getContainer());
        }

        @Override
        ExperimentModInfo getModInfo(int modInfoId)
        {
            return ModificationInfoManager.getIsotopeModInfo(modInfoId);
        }

        @Override
        IModification getModification(long dbModId)
        {
            return TargetedMSService.get().getStructuralModification(dbModId);
        }
    }
}
