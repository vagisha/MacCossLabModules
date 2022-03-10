package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HtmlString;
import org.labkey.panoramapublic.proteomexchange.UnimodModification;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.DIV;

public class AssignedUnimodDisplayColumnFactory implements DisplayColumnFactory
{
    private static final FieldKey UNIMOD_NAME = FieldKey.fromParts("UnimodName");
    private static final FieldKey UNIMOD_ID2 = FieldKey.fromParts("UnimodId2");
    private static final FieldKey UNIMOD_NAME2 = FieldKey.fromParts("UnimodName2");

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Integer unimodId = ctx.get(colInfo.getFieldKey(), Integer.class);
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

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(UNIMOD_NAME);
                keys.add(UNIMOD_ID2);
                keys.add(UNIMOD_NAME2);
            }
        };
    }
}
