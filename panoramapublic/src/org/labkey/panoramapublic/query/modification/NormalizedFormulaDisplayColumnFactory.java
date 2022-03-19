package org.labkey.panoramapublic.query.modification;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.panoramapublic.proteomexchange.Formula;

import java.io.IOException;
import java.io.Writer;

public class NormalizedFormulaDisplayColumnFactory implements DisplayColumnFactory
{
    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String formula = ctx.get(colInfo.getFieldKey(), String.class);
                if (formula != null)
                {
                    out.write(Formula.normalizeFormula(formula));
                }
            }
        };
    }
}
