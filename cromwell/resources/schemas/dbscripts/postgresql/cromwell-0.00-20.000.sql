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

-- Create schema, tables, indexes, and constraints used for Cromwell module here
-- All SQL VIEW definitions should be created in cromwell-create.sql and dropped in cromwell-drop.sql
CREATE SCHEMA cromwell;

CREATE TABLE cromwell.workflow
(
    -- standard fields
    _ts TIMESTAMP,
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

    name VARCHAR NOT NULL,
    version INTEGER NOT NULL,
    wdl TEXT,

    CONSTRAINT PK_workflow PRIMARY KEY (Id)
);

CREATE TABLE cromwell.cromwelljob
(
    -- standard fields
    _ts TIMESTAMP,
    Id SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,

    workflowid INTEGER NOT NULL,
    pipelinejobid INTEGER NOT NULL,
    cromwelljobid VARCHAR,
    cromwellstatus VARCHAR,
    inputs TEXT NOT NULL,

    CONSTRAINT PK_job PRIMARY KEY (Id),
    CONSTRAINT FK_job_container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT FK_job_workflow FOREIGN KEY (workflowid) REFERENCES cromwell.workflow(Id)
    -- CONSTRAINT FK_job_statusfiles FOREIGN KEY (pipelinejobid) REFERENCES pipeline.statusfiles(rowId)
);
CREATE INDEX IX_job_container ON cromwell.cromwelljob (Container);
CREATE INDEX IX_job_workflow ON cromwell.cromwelljob (workflowid);
CREATE INDEX IX_job_statusfiles ON cromwell.cromwelljob (pipelinejobid);
