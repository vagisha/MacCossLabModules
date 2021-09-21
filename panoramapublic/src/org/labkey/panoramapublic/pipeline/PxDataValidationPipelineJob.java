package org.labkey.panoramapublic.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.portal.ProjectUrls;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.io.File;

public class PxDataValidationPipelineJob extends PipelineJob implements PxDataValidationJobSupport
{
    private final ExperimentAnnotations _experimentAnnotations;
    private final String _description;

    @JsonCreator
    protected PxDataValidationPipelineJob(@JsonProperty("_experimentAnnotations") ExperimentAnnotations experiment, @JsonProperty("_description") String description)
    {
        super();
        _experimentAnnotations = experiment;
        _description = description;
    }

    public PxDataValidationPipelineJob(ViewBackgroundInfo info, PipeRoot root, ExperimentAnnotations experiment)
    {
        super(CopyExperimentPipelineProvider.NAME, info, root);
        _experimentAnnotations = experiment;
        _description = "Validating data for experiment:  '" + experiment.getTitle() + "'";

        String baseLogFileName = FileUtil.makeFileNameWithTimestamp("Experiment_Validation_" + experiment.getExperimentId(), "log");
        setLogFile(new File(root.getLogDirectory(), baseLogFileName));

        header("Validating data in experiment '" + experiment.getTitle() + "' for a ProteomeXchange submission.");
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
        return PipelineJobService.get().getTaskPipeline(new TaskId(PxDataValidationPipelineJob.class));
    }

    @Override
    public ExperimentAnnotations getExpAnnotations()
    {
        return _experimentAnnotations;
    }
}
