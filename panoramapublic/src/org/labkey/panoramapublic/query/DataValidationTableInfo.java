package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.RowIdQueryUpdateService;
import org.labkey.api.query.UserIdQueryForeignKey;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.PanoramaPublicController;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.model.validation.DataValidation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataValidationTableInfo extends FilteredTable<PanoramaPublicSchema>
{
    public DataValidationTableInfo(@NotNull PanoramaPublicSchema userSchema, ContainerFilter cf)
    {
        super(PanoramaPublicManager.getTableInfoDataValidation(), userSchema, cf);
        wrapAllColumns(true);
        getMutableColumn("CreatedBy").setFk(new UserIdQueryForeignKey(userSchema));
        getMutableColumn("ModifiedBy").setFk(new UserIdQueryForeignKey(userSchema));
        var statusCol = getMutableColumn("Status");
        if (statusCol != null)
        {
            statusCol.setFk(QueryForeignKey.from(userSchema, cf).to(PanoramaPublicSchema.TABLE_PX_STATUS, "RowId", null));
        }
        Map<String, Object> params = new HashMap<>();
        params.put("validationId", FieldKey.fromParts("Id"));
        params.put("id", FieldKey.fromParts("ExperimentAnnotationsId"));
        statusCol.setURL(new DetailsURL(new ActionURL(PanoramaPublicController.PxValidationStatusAction.class, getContainer()), params));

        List<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("Id"));
        visibleColumns.add(FieldKey.fromParts("Created"));
        visibleColumns.add(FieldKey.fromParts("CreatedBy"));
        visibleColumns.add(FieldKey.fromParts("JobId"));
        visibleColumns.add(FieldKey.fromParts("Status"));
        setDefaultVisibleColumns(visibleColumns);
    }


    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DataValidationQueryUpdateService(this);
    }

    // Update service allows row deletion but not insert or edit
    public static class DataValidationQueryUpdateService extends RowIdQueryUpdateService<DataValidation>
    {
        public DataValidationQueryUpdateService(DataValidationTableInfo tableInfo)
        {
            super(tableInfo);
        }

        @Override
        protected DataValidation createNewBean()
        {
            return new DataValidation();
        }

        @Override
        public DataValidation get(User user, Container container, int key)
        {
            return new TableSelector(PanoramaPublicManager.getTableInfoDataValidation()).getObject(key, DataValidation.class);
        }

        @Override
        public void delete(User user, Container container, int key)
        {
            DataValidationManager.deleteValidation(key, container);
        }

        @Override
        protected DataValidation insert(User user, Container container, DataValidation bean) throws ValidationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected DataValidation update(User user, Container container, DataValidation bean, Integer oldKey) throws ValidationException
        {
            throw new UnsupportedOperationException();
        }
    }
}
