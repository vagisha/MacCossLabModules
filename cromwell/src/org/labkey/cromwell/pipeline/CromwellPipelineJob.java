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
import org.labkey.cromwell.CromwellModule;
import org.labkey.cromwell.Workflow;

public class CromwellPipelineJob extends PipelineJob implements CromwellJobSupport
{
    private Workflow _workflow;
    private int _cromwellJobId;
    private String _panoramaApiKey;

    @JsonCreator
    protected CromwellPipelineJob(@JsonProperty("_workflow") Workflow workflow, @JsonProperty("_cromwellJobId") int cromwellJobId,  @JsonProperty("_panoramaApiKey") String panoramaApiKey)
    {
        super();
        _workflow = workflow;
        _cromwellJobId = cromwellJobId;
        _panoramaApiKey = panoramaApiKey;
    }

    public CromwellPipelineJob(ViewBackgroundInfo info, PipeRoot root, Workflow workflow, int cromwellJobId, String panoramaApiKey)
    {
        super(CromwellPipelineProvider.NAME, info, root);
        _workflow = workflow;
        _cromwellJobId = cromwellJobId;
        _panoramaApiKey = panoramaApiKey;

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
    public int getCromwellJobId()
    {
        return _cromwellJobId;
    }

    @Override
    public String getPanoramaApiKey()
    {
        return _panoramaApiKey;
    }
}
