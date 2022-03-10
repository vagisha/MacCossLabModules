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
CREATE TABLE panoramapublic.ExperimentStructuralModInfo
(
    _ts               TIMESTAMP,
    Id                SERIAL NOT NULL,
    CreatedBy         USERID,
    Created           TIMESTAMP,
    ModifiedBy        USERID,
    Modified          TIMESTAMP,

    ExperimentAnnotationsId   INT NOT NULL,
    StructuralModId           BIGINT NOT NULL,
    CombinationMod            BOOLEAN NOT NULL DEFAULT FALSE,
    UnimodId                  INT NOT NULL,
    UnimodName                VARCHAR NOT NULL,
    UnimodId2                 INT,
    UnimodName2               VARCHAR,


    CONSTRAINT PK_ExperimentStructuralModInfo PRIMARY KEY (Id),
    CONSTRAINT FK_ExperimentStructuralModInfo_ExperimentAnnotations FOREIGN KEY (experimentAnnotationsId) REFERENCES panoramapublic.ExperimentAnnotations(Id),
    CONSTRAINT FK_ExperimentStructuralModInfo_StructuralModification FOREIGN KEY (StructuralModId) REFERENCES targetedms.StructuralModification(Id),
    CONSTRAINT UQ_ExperimentStructuralModInfo UNIQUE (ExperimentAnnotationsId, StructuralModId)
);
CREATE INDEX IX_ExperimentStructuralModInfo_ExperimentAnnotationsId ON panoramapublic.ExperimentStructuralModInfo(experimentAnnotationsId);
CREATE INDEX IX_ExperimentStructuralModInfo_StructuralModId ON panoramapublic.ExperimentStructuralModInfo(StructuralModId);

CREATE TABLE panoramapublic.ExperimentIsotopicModInfo
(
    _ts               TIMESTAMP,
    Id                SERIAL NOT NULL,
    CreatedBy         USERID,
    Created           TIMESTAMP,
    ModifiedBy        USERID,
    Modified          TIMESTAMP,

    ExperimentAnnotationsId   INT NOT NULL,
    IsotopicModId             BIGINT NOT NULL,
    UnimodId                  INT NOT NULL,
    UnimodName                VARCHAR NOT NULL,

    CONSTRAINT PK_ExperimentIsotopicModInfo PRIMARY KEY (Id),
    CONSTRAINT FK_ExperimentIsotopicModInfo_ExperimentAnnotations FOREIGN KEY (experimentAnnotationsId) REFERENCES panoramapublic.ExperimentAnnotations(Id),
    CONSTRAINT FK_ExperimentIsotopicModInfo_IsotopicModId FOREIGN KEY (IsotopicModId) REFERENCES targetedms.IsotopicModification(Id),
    CONSTRAINT UQ_ExperimentIsotopicModInfo UNIQUE (ExperimentAnnotationsId, IsotopicModId)
);
CREATE INDEX IX_ExperimentIsotopicModInfo_ExperimentAnnotationsId ON panoramapublic.ExperimentIsotopicModInfo(experimentAnnotationsId);
CREATE INDEX IX_ExperimentIsotopicModInfo_IsotopicModId ON panoramapublic.ExperimentIsotopicModInfo(IsotopicModId);

