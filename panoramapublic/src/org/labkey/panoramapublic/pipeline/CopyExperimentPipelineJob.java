/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.panoramapublic.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.panoramapublic.PanoramaPublicModule;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.Journal;

import java.io.File;
import java.io.IOException;

/**
 * User: vsharma
 * Date: 8/21/2014
 * Time: 9:31 AM
 */
public class CopyExperimentPipelineJob extends PipelineJob implements CopyExperimentJobSupport
{

    private final ExperimentAnnotations _experimentAnnotations;
    private final Journal _journal;
    private final String _description;

    private String _reviewerEmailPrefix;

    private boolean _assignPxId;
    private boolean _usePxTestDb = true;

    private boolean _assignDoi;
    private boolean _useDataCiteTestApi;

    private boolean _deletePreviousCopy;


    @JsonCreator
    protected CopyExperimentPipelineJob(@JsonProperty("_experimentAnnotations") ExperimentAnnotations experiment, @JsonProperty("_journal") Journal journal,
                                        @JsonProperty("_description") String description)
    {
        super();
        _experimentAnnotations = experiment;
        _journal = journal;
        _description = description;
    }

    public CopyExperimentPipelineJob(ViewBackgroundInfo info, PipeRoot root, ExperimentAnnotations experiment, Journal journal)
    {
        super(CopyExperimentPipelineProvider.NAME, info, root);
        _experimentAnnotations = experiment;
        _journal = journal;
        _description = "Copying experiment:  " + experiment.getTitle();

        // Ensure local directory for the target
        String baseLogFileName = "Experiment_" + experiment.getExperimentId();
        PipeRoot targetRoot = PipelineService.get().findPipelineRoot(getContainer()); // Target container is where the 'export' directory will get written
        if (null == targetRoot)
            throw new NotFoundException("Cannot find target pipeline root.");

        // CONSIDER: Add a static factory method to LocalDirectory instead of using the constructor.
        //           create(@NotNull PipeRoot root, @NotNull String moduleName, @NotNull String baseLogFileName, @NotNull String localDirPath, boolean temporary)
        LocalDirectory localDirectory = new LocalDirectory(targetRoot.getContainer(), PanoramaPublicModule.NAME, root, baseLogFileName);

        setLocalDirectory(localDirectory);
        setLogFile(localDirectory.determineLogFile());

        header("Copying experiment \"" + experiment.getTitle() + "\" from folder "
                + experiment.getContainer().getPath() + " to " + getContainer().getPath());
    }

    @Override
    public ActionURL getStatusHref()
    {
        if (getContainer() != null)
        {
            return PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(getContainer());
        }
        return null;
    }

    @Override
    public String getDescription()
    {
        return _description;
    }

    @Override
    public TaskPipeline getTaskPipeline()
    {
        return PipelineJobService.get().getTaskPipeline(new TaskId(CopyExperimentPipelineJob.class));
    }

    @Override
    protected void finallyCleanUpLocalDirectory()
   {
       // Wait until the final task is complete before deleting anything from the temp export location
       if (getActiveTaskId() == null)
       {
           super.finallyCleanUpLocalDirectory();
       }
   }

    public static File getLogFileFor(PipeRoot root, ExperimentAnnotations experimentAnnotations) throws IOException
    {
        File rootDir = root.getLogDirectory();
        if (!rootDir.exists())
        {
            throw new IOException("Pipeline root directory " + rootDir.getAbsolutePath() + " does not exist.");
        }

        String logFileName = "Experiment_" + experimentAnnotations.getExperimentId() + ".log";

        return new File(rootDir, logFileName);
    }

    @Override
    public ExperimentAnnotations getExpAnnotations()
    {
        return _experimentAnnotations;
    }

    @Override
    public Journal getJournal()
    {
        return _journal;
    }

    @Override
    public File getExportDir()
    {
        return new File(getLocalDirectory().getLocalDirectoryFile(), PipelineService.EXPORT_DIR);
    }

    @Override
    public String getReviewerEmailPrefix()
    {
        return _reviewerEmailPrefix;
    }

    @Override
    public boolean assignPxId()
    {
        return _assignPxId;
    }

    @Override
    public boolean usePxTestDb()
    {
        return _usePxTestDb;
    }

    @Override
    public boolean assignDoi()
    {
        return _assignDoi;
    }

    @Override
    public boolean useDataCiteTestApi()
    {
        return _useDataCiteTestApi;
    }

    @Override
    public boolean deletePreviousCopy()
    {
        return _deletePreviousCopy;
    }

    public void setReviewerEmailPrefix(String reviewerEmailPrefix)
    {
        _reviewerEmailPrefix = reviewerEmailPrefix;
    }

    public void setAssignPxId(boolean assignPxId)
    {
        _assignPxId = assignPxId;
    }

    public void setUsePxTestDb(boolean usePxTestDb)
    {
        _usePxTestDb = usePxTestDb;
    }

    public void setAssignDoi(boolean assignDoi)
    {
        _assignDoi = assignDoi;
    }

    public void setUseDataCiteTestApi(boolean useDataCiteTestApi)
    {
        _useDataCiteTestApi = useDataCiteTestApi;
    }

    public void setDeletePreviousCopy(boolean deletePreviousCopy)
    {
        _deletePreviousCopy = deletePreviousCopy;
    }
}
