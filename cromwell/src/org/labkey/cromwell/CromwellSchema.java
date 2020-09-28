/*
 * Copyright (c) 2020 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.cromwell;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;

import java.util.Set;

public class CromwellSchema extends UserSchema
{
    public static final String NAME = "cromwell";
    public static final String SCHEMA_DESCR = "Contains data for MacCoss lab's Cromwell workflows";

    public static final String TABLE_WORKFLOW = "workflow";
    public static final String TABLE_JOB = "cromwelljob";

    public CromwellSchema(User user, Container container)
    {
        super(NAME, SCHEMA_DESCR, user, container, getSchema());
    }

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new CromwellSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(NAME, DbSchemaType.Module);
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    @Nullable
    public static Workflow getWorkflow(int workflowId)
    {
        return new TableSelector(getTableInfoWorkflow(),null, null).getObject(workflowId, Workflow.class);
    }

    @Nullable
    public static CromwellJob getJob(int jobId)
    {
        return new TableSelector(getTableInfoJob(),null, null).getObject(jobId, CromwellJob.class);
    }

    public static TableInfo getTableInfoWorkflow()
    {
        return getSchema().getTable(TABLE_WORKFLOW);
    }

    public static TableInfo getTableInfoJob()
    {
        return getSchema().getTable(TABLE_JOB);
    }

    @Override
    public @Nullable TableInfo createTable(String name, ContainerFilter cf)
    {
        if (TABLE_JOB.equalsIgnoreCase(name))
        {
            return new CromwellJobTable(this, cf);
        }
        if (TABLE_WORKFLOW.equalsIgnoreCase(name))
        {
            return new WorkflowTable(this, cf);
        }
        return null;
    }

    @Override
    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_WORKFLOW);
        hs.add(TABLE_JOB);

        return hs;
    }
}
