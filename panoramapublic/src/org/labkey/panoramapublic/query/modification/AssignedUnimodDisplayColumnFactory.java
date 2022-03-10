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

import java.io.Writer;
import java.util.Set;

import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.DIV;

public class AssignedUnimodDisplayColumnFactory implements DisplayColumnFactory
{
    private static final FieldKey MOD_ID = FieldKey.fromParts("ModId"); // Structural Mod Id
    private static final FieldKey GIVEN_UNIMOD_ID = FieldKey.fromParts("GivenUnimodId");
//    private static final FieldKey EXPT_ID = FieldKey.fromParts("ModId", "ExperimentAnnotationsId");
    private static final FieldKey UNIMOD_ID = FieldKey.fromParts("ModInfoId", "UnimodId");
    private static final FieldKey UNIMOD_NAME = FieldKey.fromParts("ModInfoId", "UnimodName");
    private static final FieldKey UNIMOD_ID2 = FieldKey.fromParts("ModInfoId", "UnimodId2");
    private static final FieldKey UNIMOD_NAME2 = FieldKey.fromParts("ModInfoId", "UnimodName2");

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

                        Long modId = ctx.get(MOD_ID, Long.class); // Structural mod Id

                        if (modId != null && exptId != null)
                        {
                            var url = new ActionURL(PanoramaPublicController.MatchToUnimodAction.class, ctx.getContainer())
                                    .addParameter("id", exptId)
                                    .addParameter("modificationId", modId)
                                    .addParameter("structural", true);
                            url.addReturnURL(ctx.getViewContext().getActionURL());
                            var findMatchLink = new Link.LinkBuilder("Find Match").href(url);
                            findMatchLink.appendTo(out);
//                        if (_structural)
//                        {
                            var comboModUrl = new ActionURL(PanoramaPublicController.CombinationModificationAction.class, ctx.getContainer())
                                    .addParameter("id", exptId)
                                    .addParameter("modificationId", modId);
                            comboModUrl.addReturnURL(ctx.getViewContext().getActionURL());
                            BR().appendTo(out);
                            new Link.LinkBuilder("Combination Modification").href(comboModUrl).appendTo(out);
//                        }
                        }
                    }
                }
                else
                {

                    Integer unimodId = ctx.get(UNIMOD_ID, Integer.class);
                    String unimodName = ctx.get(UNIMOD_NAME, String.class);
                    Integer unimodId2 = ctx.get(UNIMOD_ID2, Integer.class);
                    String unimodName2 = ctx.get(UNIMOD_NAME2, String.class);

                    if (unimodId != null)
                    {
                        if (unimodId2 != null)
                        {
                            DIV("Combination of: ").appendTo(out);
                        }
                        DIV(unimodName != null ? unimodName + ", " : HtmlString.EMPTY_STRING, UnimodModification.getLink(unimodId)).appendTo(out);
                        if (unimodId2 != null)
                        {
                            DIV("and", BR()).appendTo(out);
                            DIV(unimodName2 != null ? unimodName2 + ", " : HtmlString.EMPTY_STRING, UnimodModification.getLink(unimodId2)).appendTo(out);
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
//                keys.add(EXPT_ID);
                keys.add(UNIMOD_ID);
                keys.add(UNIMOD_NAME);
                keys.add(UNIMOD_ID2);
                keys.add(UNIMOD_NAME2);
            }
        };
    }
}
