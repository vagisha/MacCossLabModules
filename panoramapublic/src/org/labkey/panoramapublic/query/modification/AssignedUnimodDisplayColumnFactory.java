package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.PanoramaPublicController;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.proteomexchange.UnimodModification;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;
import org.labkey.panoramapublic.query.ModificationInfoManager;

import java.io.Writer;
import java.util.Set;

import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.DIV;
import static org.labkey.api.util.DOM.EM;
import static org.labkey.api.util.DOM.cl;

public abstract class AssignedUnimodDisplayColumnFactory<T extends ExperimentModInfo> implements DisplayColumnFactory
{
    private static final FieldKey MOD_ID = FieldKey.fromParts("ModId");
    private static final FieldKey GIVEN_UNIMOD_ID = FieldKey.fromParts("GivenUnimodId");
    private static final FieldKey UNIMOD_ID = FieldKey.fromParts("ModInfoId", "UnimodId");
    private static final FieldKey UNIMOD_NAME = FieldKey.fromParts("ModInfoId", "UnimodName");
    private static final FieldKey UNIMOD_ID2 = FieldKey.fromParts("ModInfoId", "UnimodId2");
    private static final FieldKey UNIMOD_NAME2 = FieldKey.fromParts("ModInfoId", "UnimodName2");
    private static final FieldKey EXPT_ID = FieldKey.fromParts("ModInfoId", "ExperimentAnnotationsId");

    abstract boolean allowCombinationModification();
    abstract ActionURL getMatchToUnimodAction(RenderContext ctx);
    abstract ActionURL getDeleteAction(RenderContext ctx);
    abstract T getModInfo(int modInfoId);

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out)
            {
                Integer modInfoId = ctx.get(colInfo.getFieldKey(), Integer.class);
                if (modInfoId == null)
                {
                    Integer givenUnimodId = ctx.get(GIVEN_UNIMOD_ID, Integer.class);
                    if (givenUnimodId == null)
                    {
                        ExperimentAnnotations exptAnnotations = ExperimentAnnotationsManager.getExperimentInContainer(ctx.getContainer());
                        Integer exptId = exptAnnotations != null ? exptAnnotations.getId() : null;

                        Long modId = ctx.get(MOD_ID, Long.class);

                        if (modId != null && exptId != null)
                        {
                            var url = getMatchToUnimodAction(ctx).addParameter("id", exptId).addParameter("modificationId", modId);
                            url.addReturnURL(ctx.getViewContext().getActionURL());
                            var findMatchLink = new Link.LinkBuilder("Find Match").href(url);
                            DIV(cl("alert-info"), findMatchLink).appendTo(out);
                            if (allowCombinationModification())
                            {
                                DIV(EM("OR")).appendTo(out);
                                var comboModUrl = new ActionURL(PanoramaPublicController.DefineCombinationModificationAction.class, ctx.getContainer())
                                        .addParameter("id", exptId)
                                        .addParameter("modificationId", modId);
                                comboModUrl.addReturnURL(ctx.getViewContext().getActionURL());
                                DIV(new Link.LinkBuilder("Combination Modification").href(comboModUrl)).appendTo(out);
                            }
                        }
                    }
                }
                else
                {
//                    var modInfo = getModInfo(modInfoId);
                    Integer unimodId = ctx.get(UNIMOD_ID, Integer.class);
                    String unimodName = ctx.get(UNIMOD_NAME, String.class);
                    Integer unimodId2 = ctx.get(UNIMOD_ID2, Integer.class);
                    String unimodName2 = ctx.get(UNIMOD_NAME2, String.class);

                    if (unimodId != null)
                    {
                        if (unimodId2 != null)
                        {
                            DIV(EM("Combination of: ")).appendTo(out);
                        }
                        DIV(unimodName != null ? unimodName + ", " : HtmlString.EMPTY_STRING, UnimodModification.getLink(unimodId)).appendTo(out);
                        if (unimodId2 != null)
                        {
                            DIV(EM("and"), BR()).appendTo(out);
                            DIV(unimodName2 != null ? unimodName2 + ", " : HtmlString.EMPTY_STRING, UnimodModification.getLink(unimodId2)).appendTo(out);
                        }

                        Integer exptId = ctx.get(EXPT_ID, Integer.class);
                        if (exptId != null)
                        {
                            ActionURL deleteUrl = getDeleteAction(ctx).addParameter("id", exptId).addParameter("modInfoId", modInfoId);
                            DIV(new Link.LinkBuilder("[Delete]")
                                    .href(deleteUrl)
                                    .addClass("labkey-error")
                                    .usePost("Are you sure you want to delete the saved Unimod information for modification FILL IN THE NAME!!!!")
                                    .clearClasses().build())
                                    .appendTo(out);
                        }
                    }
                }
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(MOD_ID);
                keys.add(GIVEN_UNIMOD_ID);
                keys.add(UNIMOD_ID);
                keys.add(UNIMOD_NAME);
                keys.add(UNIMOD_ID2);
                keys.add(UNIMOD_NAME2);
                keys.add(EXPT_ID);
            }
        };
    }

    public static class AssignedStructuralUnimod extends AssignedUnimodDisplayColumnFactory<ExperimentStructuralModInfo>
    {
        @Override
        boolean allowCombinationModification()
        {
            return true;
        }

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
    }

    public static class AssignedIsotopeUnimod extends AssignedUnimodDisplayColumnFactory<ExperimentModInfo>
    {
        @Override
        boolean allowCombinationModification()
        {
            return false;
        }

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
    }
}
