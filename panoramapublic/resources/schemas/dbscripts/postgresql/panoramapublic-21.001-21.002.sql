/*
 * Copyright (c) 2019 LabKey Corporation
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
CREATE TABLE panoramapublic.PxDataValidation
(
    _ts TIMESTAMP,
    CreatedBy              USERID,
    Created                TIMESTAMP,
    ModifiedBy             USERID,
    Modified               TIMESTAMP,

    Id                         SERIAL NOT NULL,
    Container ENTITYID         NOT NULL,
    ExperimentAnnotationsId    INT    NOT NULL,
    JobId                      INTEGER NOT NULL,
    Data                       BYTEA,

    CONSTRAINT PK_PxDataValidation PRIMARY KEY (Id),
    CONSTRAINT FK_PxDataValidation_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT FK_PxDataValidation_ExperimentAnnotations FOREIGN KEY (ExperimentAnnotationsId) REFERENCES panoramapublic.ExperimentAnnotations(Id),
    CONSTRAINT FK_PxDataValidation_JobId FOREIGN KEY (JobId) REFERENCES pipeline.statusfiles (RowId)
);

CREATE INDEX IX_PxDataValidation_Container ON panoramapublic.PxDataValidation(Container);
CREATE INDEX IX_PxDataValidation_ExperimentAnnotations ON panoramapublic.PxDataValidation(ExperimentAnnotationsId);
CREATE INDEX IX_PxDataValidation_JobId ON panoramapublic.PxDataValidation(JobId);

