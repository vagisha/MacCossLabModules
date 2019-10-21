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
INSERT INTO panoramapublic.experimentannotations SELECT * FROM targetedms.experimentannotations;
-- Update the PrimaryKey sequence
SELECT SETVAL('panoramapublic.experimentannotations_id_seq', (SELECT MAX(id)+1 FROM panoramapublic.experimentannotations), false);

INSERT INTO panoramapublic.journal SELECT * FROM targetedms.journal;
-- Update the PrimaryKey sequence
SELECT SETVAL('panoramapublic.journal_id_seq', (SELECT MAX(id)+1 FROM panoramapublic.journal), false);

INSERT INTO panoramapublic.journalexperiment SELECT * FROM targetedms.journalExperiment;

