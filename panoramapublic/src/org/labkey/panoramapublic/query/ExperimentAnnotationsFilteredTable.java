package org.labkey.panoramapublic.query;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;

/*
Base class for tables that have an experimentAnnotationsId column, and are filtered on the Container column
in the panoramapublic.experimentannotations table.
 */
public class ExperimentAnnotationsFilteredTable extends PanoramaPublicTable
{
    private static final String EXP = "exp";

    public ExperimentAnnotationsFilteredTable(TableInfo table, PanoramaPublicSchema schema, ContainerFilter cf)
    {
        super(table, schema, cf, getJoinSql(), new SQLFragment(EXP + ".Container"));
    }

    private static SQLFragment getJoinSql()
    {
        SQLFragment joinToExpAnnotSql = new SQLFragment(" INNER JOIN ");
        joinToExpAnnotSql.append(PanoramaPublicManager.getTableInfoExperimentAnnotations(), EXP);
        joinToExpAnnotSql.append(" ON (" + EXP + ".id = experimentAnnotationsId) ");
        return joinToExpAnnotSql;
    }
}
