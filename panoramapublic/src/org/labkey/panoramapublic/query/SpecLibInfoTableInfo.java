package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.HtmlString;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;

import java.io.IOException;
import java.io.Writer;

public class SpecLibInfoTableInfo extends PanoramaPublicTable
{

    public SpecLibInfoTableInfo(PanoramaPublicSchema schema, ContainerFilter cf)
    {
        super(PanoramaPublicManager.getTableInfoSpecLibInfo(), schema, cf, getJoinSql(), new SQLFragment(" exp.Container "));

        var dependencyTypeCol = getMutableColumn("DependencyType");
        if (dependencyTypeCol != null)
        {
            dependencyTypeCol.setFk(new LookupForeignKey()
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return getUserSchema().getTable(PanoramaPublicSchema.TABLE_LIB_DEPENDENCY_TYPE, cf);
                }
            });
        }

        var sourceTypeCol = getMutableColumn("SourceType");
        if (sourceTypeCol != null)
        {
            sourceTypeCol.setFk(new LookupForeignKey()
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return getUserSchema().getTable(PanoramaPublicSchema.TABLE_LIB_SOURCE_TYPE, cf);
                }
            });
        }

        var sourcePasswordCol = getMutableColumn("SourcePassword");
        sourcePasswordCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DataColumn(colInfo)
                {
                    @Override
                    public Object getValue(RenderContext ctx)
                    {
                        if (ctx.getViewContext().getUser().isInSiteAdminGroup())
                        {
                            // Show the password only to site admins
                            return super.getValue(ctx);
                        }
                        return "********";
                    }

                    @Override
                    public Object getDisplayValue(RenderContext ctx)
                    {
                        return getValue(ctx);
                    }

                    @Override
                    public @NotNull HtmlString getFormattedHtml(RenderContext ctx)
                    {
                        return HtmlString.of(getValue(ctx));
                    }

                    @Override
                    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                    {
                        super.renderGridCellContents(ctx, out);
                    }
                };
            }
        });
    }

    private static SQLFragment getJoinSql()
    {
        SQLFragment joinToExpAnnotSql = new SQLFragment(" INNER JOIN ");
        joinToExpAnnotSql.append(PanoramaPublicManager.getTableInfoExperimentAnnotations(), "exp");
        joinToExpAnnotSql.append(" ON (exp.id = experimentannotationsid) ");
        return joinToExpAnnotSql;
    }
}
