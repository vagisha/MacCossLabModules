package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.Link;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.PanoramaPublicController;
import org.labkey.panoramapublic.proteomexchange.UnimodModification;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static org.labkey.api.util.DOM.BR;

public class UnimodIdDisplayColumnFactory implements DisplayColumnFactory
{
    private static final FieldKey MOD_ID = FieldKey.fromParts("ModId");
    private static final FieldKey EXPT_ID = FieldKey.fromParts("ExperimentAnnotationsId");
    private static final FieldKey MOD_INFO_ID = FieldKey.fromParts("ModInfoId");

    private final boolean _structural;

    public UnimodIdDisplayColumnFactory(boolean structural)
    {
        _structural = structural;
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out)
            {
                Integer unimodId = ctx.get(colInfo.getFieldKey(), Integer.class);
                Integer modInfoId = ctx.get(MOD_INFO_ID, Integer.class);

                if (unimodId != null)
                {
                    UnimodModification.getLink(unimodId).appendTo(out);
                }
                else if (modInfoId == null)
                {
                    Integer exptId = ctx.get(EXPT_ID, Integer.class);
                    Long modId = ctx.get(MOD_ID, Long.class);

                    if (modId != null && exptId != null)
                    {
                        var url = new ActionURL(PanoramaPublicController.MatchToUnimodAction.class, ctx.getContainer())
                                .addParameter("id", exptId)
                                .addParameter("modificationId", modId)
                                .addParameter("structural", _structural);
                        url.addReturnURL(ctx.getViewContext().getActionURL());
                        var findMatchLink = new Link.LinkBuilder("Find Match").href(url);
                        findMatchLink.appendTo(out);
                        if (_structural)
                        {
                            var comboModUrl = new ActionURL(PanoramaPublicController.CombinationModificationAction.class, ctx.getContainer())
                                    .addParameter("id", exptId)
                                    .addParameter("modificationId", modId);
                            comboModUrl.addReturnURL(ctx.getViewContext().getActionURL());
                            BR().appendTo(out);
                            new Link.LinkBuilder("Combination Modification").href(comboModUrl).appendTo(out);
                        }
                    }
                }
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(MOD_ID);
                keys.add(EXPT_ID);
                keys.add(MOD_INFO_ID);
            }
        };
    }

    public static class StructuralUnimodIdColumn extends UnimodIdDisplayColumnFactory
    {
        public StructuralUnimodIdColumn()
        {
            super(true);
        }
    }

    public static class IsotopicUnimodIdColumn extends UnimodIdDisplayColumnFactory
    {
        public IsotopicUnimodIdColumn()
        {
            super(false);
        }
    }
}
