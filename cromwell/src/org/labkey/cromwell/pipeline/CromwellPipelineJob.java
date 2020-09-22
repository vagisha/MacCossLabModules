package org.labkey.cromwell.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.labkey.api.pipeline.LocalDirectory;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.pipeline.TaskId;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.cromwell.CromwellJob;
import org.labkey.cromwell.CromwellModule;
import org.labkey.cromwell.Workflow;

public class CromwellPipelineJob extends PipelineJob implements CromwellJobSupport
{
    private Workflow _workflow;
    private CromwellJob _cromwellJob;

    @JsonCreator
    protected CromwellPipelineJob(@JsonProperty("_workflow") Workflow workflow, @JsonProperty("_cromwellJob") CromwellJob cromwellJob)
    {
        super();
        _workflow = workflow;
        _cromwellJob = cromwellJob;
    }

    public CromwellPipelineJob(ViewBackgroundInfo info, PipeRoot root, Workflow workflow, CromwellJob cromwellJob)
    {
        super(CromwellPipelineProvider.NAME, info, root);
        _workflow = workflow;
        _cromwellJob = cromwellJob;

        String baseLogFileName = FileUtil.makeFileNameWithTimestamp(workflow.getName().replace(" ", "_"));
        LocalDirectory localDirectory = LocalDirectory.create(root, CromwellModule.NAME, baseLogFileName, root.getRootPath().getAbsolutePath());
        setLocalDirectory(localDirectory);
        setLogFile(localDirectory.determineLogFile());

        header(getDescription());
    }

    @Override
    public TaskPipeline getTaskPipeline()
    {
        return PipelineJobService.get().getTaskPipeline(new TaskId(CromwellPipelineJob.class));
    }

    @Override
    public URLHelper getStatusHref()
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Cromwell job for workflow '" + _workflow.getName() + "'";
    }

    @Override
    public Workflow getWorkflow()
    {
        return _workflow;
    }

    @Override
    public CromwellJob getCromwellJob()
    {
        return _cromwellJob;
    }
}
