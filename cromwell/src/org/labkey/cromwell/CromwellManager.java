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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;

import java.util.List;

public class CromwellManager
{
    private static final CromwellManager _instance = new CromwellManager();

    private CromwellManager()
    {
        // prevent external construction with a private default constructor
    }

    public static CromwellManager get()
    {
        return _instance;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(CromwellSchema.NAME, DbSchemaType.Module);
    }

    public List<Workflow> getWorkflows()
    {
        return new TableSelector(getTableInfoWorkflow()).getArrayList(Workflow.class);
    }

    @Nullable
    public Workflow getWorkflow(int workflowId)
    {
        return new TableSelector(getTableInfoWorkflow()).getObject(workflowId, Workflow.class);
    }

    @Nullable
    public CromwellJob getCromwellJob(int id)
    {
        return new TableSelector(getTableInfoJob()).getObject(id, CromwellJob.class);
    }

    public CromwellJob saveNewJob(CromwellJob job, User user)
    {
        return Table.insert(user, getTableInfoJob(), job);
    }

    public void updateJob(CromwellJob job, User user)
    {
        Table.update(user, getTableInfoJob(), job, job.getId());
    }

    public static TableInfo getTableInfoWorkflow()
    {
        return getSchema().getTable(CromwellSchema.TABLE_WORKFLOW);
    }

    public static TableInfo getTableInfoJob()
    {
        return getSchema().getTable(CromwellSchema.TABLE_JOB);
    }

    public void deleteJob(CromwellJob cromwellJob)
    {
        Table.delete(getTableInfoJob(), new SimpleFilter(FieldKey.fromParts("Id"), cromwellJob.getId()));
    }
}