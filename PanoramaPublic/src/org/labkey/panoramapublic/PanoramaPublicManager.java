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

package org.labkey.panoramapublic;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.IModification;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;

import java.util.List;

public class PanoramaPublicManager
{
    private static final PanoramaPublicManager _instance = new PanoramaPublicManager();

    private static Logger _log = Logger.getLogger(PanoramaPublicManager.class);
    private PanoramaPublicManager()
    {
        // prevent external construction with a private default constructor
    }

    public static PanoramaPublicManager get()
    {
        return _instance;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(PanoramaPublicSchema.SCHEMA_NAME);
    }

    public static TableInfo getTableInfoExperimentAnnotations()
    {
        return getSchema().getTable(PanoramaPublicSchema.TABLE_EXPERIMENT_ANNOTATIONS);
    }

    public static TableInfo getTableInfoJournal()
    {
        return getSchema().getTable(PanoramaPublicSchema.TABLE_JOURNAL);
    }

    public static TableInfo getTableInfoJournalExperiment()
    {
        return getSchema().getTable(PanoramaPublicSchema.TABLE_JOURNAL_EXPERIMENT);
    }

    public static ITargetedMSRun getRunByLsid(String lsid, Container container)
    {
        return TargetedMSService.get().getRunByLsid(lsid, container);
    }

    public static String getRawFilesDir()
    {
        return TargetedMSService.get().getRawFilesDir();
    }

    public static ActionURL getRawDataTabUrl(Container container)
    {
        // urlProvider(ProjectUrls.class).getBeginURL(getContainer(), TargetedMSController.FolderSetupAction.RAW_FILES_TAB);
        return PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(container, getRawDataTabName());
    }

    public static String getRawDataTabName()
    {
        return "Raw Data"; // RAW_FILES_TAB
    }

    public static TableInfo getTableInfoRuns()
    {
        return TargetedMSService.get().getTableInfoRuns();
    }

    public static void makePanoramaExperimentalDataFolder(Container container, User user)
    {
        Module targetedMSModule = ModuleLoader.getInstance().getModule(TargetedMSService.get().getModuleName());
        ModuleProperty moduleProperty = targetedMSModule.getModuleProperties().get(TargetedMSService.get().getFolderTypePropertyName());
        if(container.getActiveModules().contains(targetedMSModule))
        {
            moduleProperty.saveValue(user, container, TargetedMSService.FolderType.Experiment.toString());
        }
    }

    public static List<String> getSampleFilePathsForRun(int runId)
    {
        return TargetedMSService.get().getSampleFilePaths(runId);
    }

    public static List<? extends IModification.IStructuralModification> getStructuralModificationsUsedInRun(int runId)
    {
        return TargetedMSService.get().getStructuralModificationsUsedInRun(runId);
    }

    public static List<? extends IModification.IIsotopeModification> getIsotopeModificationsUsedInRun(int runId)
    {
        return TargetedMSService.get().getIsotopeModificationsUsedInRun(runId);
    }
}