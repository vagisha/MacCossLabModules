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
    Container                  ENTITYID NOT NULL,
    ExperimentAnnotationsId    INT    NOT NULL,
    JobId                      INTEGER NOT NULL,
    Status                     INT,

    CONSTRAINT PK_PxDataValidation PRIMARY KEY (Id),
    CONSTRAINT FK_PxDataValidation_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId),
    CONSTRAINT FK_PxDataValidation_ExperimentAnnotations FOREIGN KEY (ExperimentAnnotationsId) REFERENCES panoramapublic.ExperimentAnnotations(Id),
    CONSTRAINT FK_PxDataValidation_JobId FOREIGN KEY (JobId) REFERENCES pipeline.statusfiles (RowId)
);

CREATE INDEX IX_PxDataValidation_Container ON panoramapublic.PxDataValidation(Container);
CREATE INDEX IX_PxDataValidation_ExperimentAnnotations ON panoramapublic.PxDataValidation(ExperimentAnnotationsId);
CREATE INDEX IX_PxDataValidation_JobId ON panoramapublic.PxDataValidation(JobId);

CREATE TABLE panoramapublic.SkylineDocValidation
(
    Id                         SERIAL NOT NULL,
    ValidationId               INT NOT NULL,
    RunId                      INT NOT NULL, -- targetedms.runs.Id
    Container                  ENTITYID NOT NULL,
    Name                       VARCHAR(300),

    CONSTRAINT PK_SkylineDocValidation PRIMARY KEY (Id),
    CONSTRAINT FK_SkylineDocValidation_PxDataValidation FOREIGN KEY (ValidationId) REFERENCES panoramapublic.PxDataValidation(Id),
    CONSTRAINT FK_SkylineDocValidation_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);
CREATE INDEX IX_SkylineDocValidation_ValidationId ON panoramapublic.SkylineDocValidation(ValidationId);
CREATE INDEX IX_SkylineDocValidation_Container ON panoramapublic.SkylineDocValidation(Container);
CREATE INDEX IX_SkylineDocValidation_RunId ON panoramapublic.SkylineDocValidation(runId);

CREATE TABLE panoramapublic.SkylineDocSampleFile
(
    Id                         SERIAL NOT NULL,
    SkylineDocValidationId     INT NOT NULL,
    SampleFileId               INT NOT NULL, -- targetedms.samplefile.id
    Name                       VARCHAR(300),
    Path                       TEXT,

    CONSTRAINT PK_SkylineDocSampleFile PRIMARY KEY (Id),
    CONSTRAINT FK_SkylineDocSampleFile_SkylineDocValidation FOREIGN KEY (SkylineDocValidationId) REFERENCES panoramapublic.SkylineDocValidation(Id)
);

CREATE TABLE panoramapublic.SpecLibValidation
(
    Id                         SERIAL NOT NULL,
    ValidationId               INT NOT NULL,
    Name                       VARCHAR(300),
    Size                       INT NOT NULL,
    LibLsid                    VARCHAR(100), -- LibInfo.libLSID from the .blib file
    LibType                    INT NOT NULL, -- BLIB, BLIB_PROSIT, BLIB_ASSAY, ELIB, OTHER

    CONSTRAINT PK_SpecLibValidation PRIMARY KEY (Id),
    CONSTRAINT FK_SpecLibValidation_PxDataValidation FOREIGN KEY (ValidationId) REFERENCES panoramapublic.PxDataValidation(Id)
);

CREATE TABLE panoramapublic.SpecLibSourceFile
(
    Id                         SERIAL NOT NULL,
    SpecLibValidationId        INT NOT NULL,
    Name                       VARCHAR(300),
    SourceType                 INT NOT NULL, -- Spectrum, Id File, Prosit, CSV/TSV
    Path                       TEXT,

    CONSTRAINT PK_SpecLibSourceFile PRIMARY KEY (Id),
    CONSTRAINT FK_SpecLibSourceFile_SpecLibValidation FOREIGN KEY (SpecLibValidationId) REFERENCES panoramapublic.SpecLibValidation(Id)
);

CREATE TABLE panoramapublic.SkyDocSpecLib
(
    Id                         SERIAL NOT NULL,
    SkylineDocValidationId     INT NOT NULL,
    SpecLibValidationId        INT NOT NULL,
    SpectrumLibraryId          INT NOT NULL, -- targetedms.spectrumlibrary.id
    Included                   BOOLEAN NOT NULL,

    CONSTRAINT PK_SkyDocSpecLib PRIMARY KEY (Id),
    CONSTRAINT FK_SkyDocSpecLib_SkylineDocValidation FOREIGN KEY (SkylineDocValidationId) REFERENCES panoramapublic.SkylineDocValidation(Id),
    CONSTRAINT FK_SkyDocSpecLib_SpecLibValidation FOREIGN KEY (SpecLibValidationId) REFERENCES panoramapublic.SpecLibValidation(Id)
);

CREATE TABLE panoramapublic.ModificationValidation
(
    Id                         SERIAL NOT NULL,
    ValidationId               INT NOT NULL,
    SkylineModName             VARCHAR(100) NOT NULL,
    UnimodId                   INT,
    UnimodName                 VARCHAR(100), -- LibInfo.libLSID from the .blib file
    ModType                    INT NOT NULL, -- Structural, Isotopic

    CONSTRAINT PK_ModificationValidation PRIMARY KEY (Id),
    CONSTRAINT FK_ModificationValidation_PxDataValidation FOREIGN KEY (ValidationId) REFERENCES panoramapublic.PxDataValidation(Id)
);

CREATE TABLE panoramapublic.SkylineDocModification
(
    Id                         SERIAL NOT NULL,
    SkylineDocValidationId     INT NOT NULL,
    ModificationValidationId   INT NOT NULL,

    CONSTRAINT PK_SkylineDocModification PRIMARY KEY (Id),
    CONSTRAINT FK_SkylineDocModification_SkylineDocValidation FOREIGN KEY (SkylineDocValidationId) REFERENCES panoramapublic.SkylineDocValidation(Id),
    CONSTRAINT FK_SkylineDocModification_SpecLibValidation FOREIGN KEY (ModificationValidationId) REFERENCES panoramapublic.ModificationValidation(Id)
);







