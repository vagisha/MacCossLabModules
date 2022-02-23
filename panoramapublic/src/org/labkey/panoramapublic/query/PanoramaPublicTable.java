package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.PanoramaPublicSchema.InnerJoinClause;

import java.util.List;
import java.util.stream.Collectors;

public class PanoramaPublicTable extends FilteredTable<PanoramaPublicSchema>
{
    private final SQLFragment _joinSql;
    private final SQLFragment _containerSql;
    private final FieldKey _containerFieldKey;

    private static final String CONTAINER = "Container";

    public PanoramaPublicTable(TableInfo table, PanoramaPublicSchema schema, ContainerFilter cf, @NotNull List<InnerJoinClause> joinList)
    {
        super(table, schema, cf);
        _joinSql = getJoinSql(joinList);
        _containerSql = getContainerSql(joinList);
        _containerFieldKey = getFieldKeyForContainer(joinList);
        wrapAllColumns(true);
        addQueryFKs();
    }

    private SQLFragment getJoinSql(List<InnerJoinClause> joinList)
    {
        SQLFragment sql = new SQLFragment();
        for (var innerJoin: joinList)
        {
            sql.append(innerJoin.toSql());
        }
        return sql;
    }

    private SQLFragment getContainerSql(List<InnerJoinClause> joinList)
    {
        if (joinList.size() > 0)
        {
            // We expect the last table in the join sequence to have the container column
            return new SQLFragment(joinList.get(joinList.size() - 1).getJoinTableAlias()).append(".").append(CONTAINER);
        }
        return new SQLFragment(CONTAINER);
    }

    private FieldKey getFieldKeyForContainer(List<InnerJoinClause> joinList)
    {
        if (joinList.size() == 0)
        {
            return super.getContainerFieldKey();
        }
        var parts = joinList.stream().map(InnerJoinClause::getJoinCol).collect(Collectors.toList());
        parts.add(CONTAINER);
        return FieldKey.fromParts(parts);
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        // Don't apply the container filter normally, let us apply it in our wrapper around the normally generated SQL
    }

    @Override
    @NotNull
    public SQLFragment getFromSQL(String alias)
    {
        SQLFragment sql = new SQLFragment("(SELECT X.* FROM ");
        sql.append(super.getFromSQL("X"));
        sql.append(" ");

        if (getContainerFilter() != ContainerFilter.EVERYTHING)
        {
            sql.append(_joinSql.getSQL());
            sql.append(" WHERE ");
            sql.append(getContainerFilter().getSQLFragment(getSchema(), _containerSql));
        }
        sql.append(") ");
        sql.append(alias);

        return sql;
    }

    @Override
    public FieldKey getContainerFieldKey()
    {
        return _containerFieldKey;
    }

    private void addQueryFKs()
    {
        for (var columnInfo : getMutableColumns())
        {
            // Add lookups to user schema tables (exposed through the query schema browser) so that we get any extra columns added to those tables.
            // If we don't add these here then the lookups in the schema browser will show null schema values: e.g. null.ExperimentAnnotations.Id
            // See Issue 40229: targetedms lookups target DB schema TableInfo instead of UserSchema version
            ForeignKey fk = columnInfo.getFk();
            if (fk != null && PanoramaPublicSchema.SCHEMA_NAME.equalsIgnoreCase(fk.getLookupSchemaName()))
            {
                columnInfo.setFk(new QueryForeignKey(getUserSchema(), getContainerFilter(), getUserSchema(), null,
                        fk.getLookupTableName(), fk.getLookupColumnName(), fk.getLookupDisplayName()));
            }
            else
            {
                String name = columnInfo.getName();
                if ("Container".equalsIgnoreCase(name))
                {
                    columnInfo.setFk(new ContainerForeignKey(getUserSchema()));
                }
                if ("CreatedBy".equalsIgnoreCase(columnInfo.getName()) || "ModifiedBy".equalsIgnoreCase(columnInfo.getName()))
                {
                    columnInfo.setFk(new UserIdQueryForeignKey(getUserSchema(), true));
                }
            }
        }
    }
}
