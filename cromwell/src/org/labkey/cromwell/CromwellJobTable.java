package org.labkey.cromwell;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
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
import org.labkey.api.util.Link;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CromwellJobTable extends FilteredTable<CromwellSchema>
{
    public CromwellJobTable(CromwellSchema schema, ContainerFilter cf)
    {
        super(CromwellManager.getTableInfoJob(), schema, cf);
        wrapAllColumns(true);
        setDetailsURL(new DetailsURL(new ActionURL(CromwellController.CromwellJobDetailsAction.class, getContainer()), Collections.singletonMap("jobId", "id")));

        var containerCol = getMutableColumn(FieldKey.fromParts("Container"));
        ContainerForeignKey.initColumn(containerCol, schema);

        var createdByCol = getMutableColumn(FieldKey.fromParts("CreatedBy"));
        UserIdForeignKey.initColumn(createdByCol);

        var copyJobCol =  wrapColumn("CopyJob", getRealTable().getColumn(FieldKey.fromParts("Id")));
        addColumn(copyJobCol);
        copyJobCol.setDisplayColumnFactory(colInfo -> new DataColumn(colInfo){
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Integer jobId = ctx.get(FieldKey.fromParts("Id"), Integer.class);
                ActionURL copyJobUrl = new ActionURL(CromwellController.CopyCromwellJobAction.class, getContainer()).addParameter("jobId", jobId);
                out.write(new Link.LinkBuilder("Copy").href(copyJobUrl).clearClasses().toString());
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
        ActionURL cromwellMetadataUrl = new ActionURL(CromwellController.CromwellJobMetadataAction.class, getContainer());
        cromwellMetadataUrl.addParameter("jobId", "${Id}");
        getMutableColumn(FieldKey.fromParts("CromwellStatus")).setURL(StringExpressionFactory.createURL(cromwellMetadataUrl));

        List<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("WorkflowId"));
        visibleColumns.add(FieldKey.fromParts("CromwellJobId"));
        visibleColumns.add(FieldKey.fromParts("CromwellStatus"));
        visibleColumns.add(FieldKey.fromParts("PipelineJobId"));
        visibleColumns.add(FieldKey.fromParts("CreatedBy"));
        visibleColumns.add(FieldKey.fromParts("Created"));
        visibleColumns.add(FieldKey.fromParts("CopyJob"));

        setDefaultVisibleColumns(visibleColumns);
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        // return getContainer().hasPermission(user, perm);
        return (ReadPermission.class.equals(perm) || DeletePermission.class.equals(perm))
                && getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }
}
