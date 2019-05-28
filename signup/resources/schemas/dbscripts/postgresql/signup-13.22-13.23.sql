/*
 * Copyright (c) 2011 LabKey Corporation
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

-- Create schema, tables, indexes, and constraints used for SignUp module here
-- All SQL VIEW definitions should be created in signup-create.sql and dropped in signup-drop.sql


ALTER TABLE signup.temporaryuser RENAME COLUMN containerId TO Container;
ALTER TABLE signup.temporaryuser ADD labkeyUserId USERID;
UPDATE signup.temporaryuser AS tu SET labkeyUserId = (SELECT userId FROM core.principals AS p WHERE tu.email = p.name) WHERE tu.labkeyUserId IS NULL;

