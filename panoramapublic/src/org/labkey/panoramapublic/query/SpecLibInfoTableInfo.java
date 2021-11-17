package org.labkey.panoramapublic.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;

public class SpecLibInfoTableInfo extends PanoramaPublicTable
{

    public SpecLibInfoTableInfo(TableInfo table, PanoramaPublicSchema schema, ContainerFilter cf)
    {
        super(table, schema, cf, getJoinSql(), new SQLFragment(" exp.Container "));

        getMutableColumn("DependencyType").setFk(new LookupForeignKey()
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return getUserSchema().getTable(PanoramaPublicSchema.TABLE_LIB_DEPENDENCY_TYPE, cf);
            }
        });

        getMutableColumn("SourceType").setFk(new LookupForeignKey()
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return getUserSchema().getTable(PanoramaPublicSchema.TABLE_LIB_SOURCE_TYPE, cf);
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
