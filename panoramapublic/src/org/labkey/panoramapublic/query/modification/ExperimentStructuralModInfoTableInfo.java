package org.labkey.panoramapublic.query.modification;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.RowIdQueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.query.ContainerJoin;
import org.labkey.panoramapublic.query.PanoramaPublicTable;

public class ExperimentStructuralModInfoTableInfo extends PanoramaPublicTable
{
    public ExperimentStructuralModInfoTableInfo(PanoramaPublicSchema schema, ContainerFilter cf)
    {
        super(PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), schema, cf, ContainerJoin.ExpAnnotJoin);

        var unimodCol = addColumn(wrapColumn("Unimod", getRealTable().getColumn("UnimodId")));
        unimodCol.setDisplayColumnFactory(new UnimodIdDisplayColumnFactory(true));

        var assignedModCol = addColumn(wrapColumn("UnimodAssignment", getRealTable().getColumn("UnimodId")));
        assignedModCol.setLabel("Unimod Assignment");
        assignedModCol.setDisplayColumnFactory(new AssignedUnimodDisplayColumnFactory());
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new ExperimentModInfoQueryUpdateService(this);
    }

    public static class ExperimentModInfoQueryUpdateService extends RowIdQueryUpdateService<ExperimentStructuralModInfo>
    {
        public ExperimentModInfoQueryUpdateService(ExperimentStructuralModInfoTableInfo tableInfo)
        {
            super(tableInfo);
        }

        @Override
        protected ExperimentStructuralModInfo createNewBean()
        {
            return new ExperimentStructuralModInfo();
        }

        @Override
        public ExperimentStructuralModInfo get(User user, Container container, int key)
        {
            return new TableSelector(PanoramaPublicManager.getTableInfoExperimentStructuralModInfo()).getObject(key, ExperimentStructuralModInfo.class);
        }

        @Override
        public void delete(User user, Container container, int key)
        {
            Table.delete(PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), key);
        }

        @Override
        protected ExperimentStructuralModInfo insert(User user, Container container, ExperimentStructuralModInfo bean) throws ValidationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected ExperimentStructuralModInfo update(User user, Container container, ExperimentStructuralModInfo bean, Integer oldKey) throws ValidationException
        {
            throw new UnsupportedOperationException();
        }
    }
}
