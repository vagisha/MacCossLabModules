package org.labkey.cromwell;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.UserIdForeignKey;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.SiteAdminPermission;
import org.labkey.api.util.Link;
import org.labkey.api.view.ActionURL;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WorkflowTable extends FilteredTable<CromwellSchema>
{
    public WorkflowTable(CromwellSchema schema, ContainerFilter cf)
    {
        super(CromwellManager.getTableInfoWorkflow(), schema, cf);
        wrapAllColumns(true);
        setDetailsURL(new DetailsURL(new ActionURL(CromwellController.WorkflowDetailsAction.class, getContainer()), Collections.singletonMap("workflowId", "id")));

        var createdByCol = getMutableColumn(FieldKey.fromParts("CreatedBy"));
        UserIdForeignKey.initColumn(createdByCol);

        var newJobCol =  wrapColumn("NewJob", getRealTable().getColumn(FieldKey.fromParts("Id")));
        addColumn(newJobCol);
        newJobCol.setDisplayColumnFactory(colInfo -> new DataColumn(colInfo){
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Integer workflowId = ctx.get(FieldKey.fromParts("Id"), Integer.class);
                ActionURL newJobUrl = new ActionURL(CromwellController.SubmitCromwellJobAction.class, getContainer()).addParameter("workflowId", workflowId);
                out.write(new Link.LinkBuilder("Submit New Job").href(newJobUrl).clearClasses().toString());
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                keys.add(FieldKey.fromParts("Id"));
                super.addQueryFieldKeys(keys);
            }

            @Override
            public boolean isSortable()
            {
                return false;
            }

            @Override
            public boolean isFilterable()
            {
                return false;
            }
        });

        List<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("Name"));
        visibleColumns.add(FieldKey.fromParts("Version"));
        visibleColumns.add(FieldKey.fromParts("CreatedBy"));
        visibleColumns.add(FieldKey.fromParts("Created"));
        visibleColumns.add(FieldKey.fromParts("NewJob"));

        setDefaultVisibleColumns(visibleColumns);
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        // Only site admins should be able to edit workflows
        return getContainer().hasPermission(user, SiteAdminPermission.class);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }
}
