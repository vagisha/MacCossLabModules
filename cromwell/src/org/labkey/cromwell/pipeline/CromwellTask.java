package org.labkey.cromwell.pipeline;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.security.User;
import org.labkey.api.util.FileType;
import org.labkey.cromwell.CromwellJob;
import org.labkey.cromwell.CromwellManager;

import java.util.Collections;
import java.util.List;

public class CromwellTask extends PipelineJob.Task<CromwellTask.Factory>
{
    public CromwellTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @Override
    public @NotNull RecordedActionSet run() throws PipelineJobException
    {
        PipelineJob job = getJob();
        CromwellJobSupport support = job.getJobSupport(CromwellJobSupport.class);

        job.getLogger().info("Submitting Cromwell job for " + support.getWorkflow().getName());

        runCromwellJob(support, job.getUser(), job.getLogger());

        job.getLogger().info("Finished running Cromwell job.");

        return new RecordedActionSet();
    }

    private void runCromwellJob(CromwellJobSupport jobSupport, User user, Logger log) throws PipelineJobException
    {
        int cromwellJobId = jobSupport.getCromwellJobId();
        CromwellManager manager = CromwellManager.get();
        CromwellJob cromwellJob = manager.getCromwellJob(cromwellJobId);
        if(cromwellJob == null)
        {
            throw new PipelineJobException("Could not find a CromwellJob for id: " + cromwellJobId);
        }

        if(cromwellJob.getPipelineJobId() == null)
        {
            Integer pipelineJobId = (PipelineService.get().getJobId(getJob().getUser(), getJob().getContainer(), getJob().getJobGUID()));
            cromwellJob.setPipelineJobId(pipelineJobId);
        }

        try
        {
            CromwellUtil.CromwellJobStatus status = CromwellUtil.submitJob(cromwellJob, log);
            if(status != null)
            {
                cromwellJob.setCromwellJobId(status.getJobId());
                cromwellJob.setCromwellStatus(status.getJobStatus());
                manager.updateJob(cromwellJob, user);

                if(!status.submitted())
                {
                    // TODO: what do we expect to see here?
                    throw new PipelineJobException("Job submission failed. Status returned from Cromwell server was " + status.getJobStatus());
                }
            }
            else
            {
                throw new PipelineJobException("Job submission failed. Could not get status from Cromwell server.");
            }
        }
        catch (CromwellException e)
        {
            throw new PipelineJobException("Job submission failed with error message " + e.getMessage(), e);
        }

        final int sleepTime = 1 * 20 * 1000;

        String lastStatus = "";
        int attempts = 5;
        while(true)
        {
            try
            {
                Thread.sleep(sleepTime);
            }
            catch (InterruptedException e)
            {
                log.error("Cancelled task.", e);
                break;
            }

            try
            {
                var pipelineJobStatus = PipelineService.get().getStatusFile(getJob().getJobGUID()).getStatus();
                if(PipelineJob.TaskStatus.cancelling.matches(pipelineJobStatus))
                {
                    break;
                }

                log.info("Checking status of job " + cromwellJob.getCromwellJobId());
                CromwellUtil.CromwellJobStatus status = CromwellUtil.getJobStatus(cromwellJob, log);
                if(status == null && attempts > 0)
                {
                    log.info("Did not get job status.  Job may not yet have started running. Trying again...");
                    attempts--;
                    continue;
                }
                if(!lastStatus.equalsIgnoreCase(status.getJobStatus()))
                {
                    log.info("Cromwell job status: " + status.getJobStatus());
                    cromwellJob.setCromwellStatus(status.getJobStatus());
                    manager.updateJob(cromwellJob, user);
                }

                if(status.success())
                {
                    log.info("Cromwell job successfully completed.");
                    break;
                }
                if(status.failed())
                {
                    log.error("Cromwell job failed.");
                    // TODO: throw an exception here so the pipeline job status is set to failed too?
                    break;
                }
                if(status.success())
                {
                    log.error("Cromwell job succeeded!");
                    break;
                }
            }
            catch (CromwellException e)
            {
                log.error("An error occurred getting Cromwell job status", e);
                // TODO: throw an exception here so the pipeline job status is set to failed too?
                break;
            }
        }
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        public Factory()
        {
            super(CromwellTask.class);
        }

        @Override
        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new CromwellTask(this, job);
        }

        @Override
        public List<FileType> getInputTypes()
        {
            return Collections.emptyList();
        }

        @Override
        public List<String> getProtocolActionNames()
        {
            return Collections.emptyList();
        }

        @Override
        public String getStatusName()
        {
            return "SUBMIT CROMWELL JOB";
        }

        @Override
        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }
    }
}
