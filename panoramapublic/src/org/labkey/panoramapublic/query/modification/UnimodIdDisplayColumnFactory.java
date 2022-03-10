package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.panoramapublic.proteomexchange.UnimodModification;

import java.io.Writer;

public class UnimodIdDisplayColumnFactory implements DisplayColumnFactory
{
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
                    UnimodModification.getLink(unimodId).appendTo(out);
                }
            }
        };
    }
}
