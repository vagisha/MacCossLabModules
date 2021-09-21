package org.labkey.panoramapublic.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.util.FileType;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.Collections;
import java.util.List;

public class PxDataValidationTask extends PipelineJob.Task<PxDataValidationTask.Factory>
{
    private PxDataValidationTask(PxDataValidationTask.Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @Override
    @NotNull
    public RecordedActionSet run() throws PipelineJobException
    {
        PipelineJob job = getJob();
        PxDataValidationJobSupport support = job.getJobSupport(PxDataValidationJobSupport.class);
        doValidation(job, support);

        return new RecordedActionSet();
    }

    public void doValidation(PipelineJob job, PxDataValidationJobSupport jobSupport) throws PipelineJobException
    {
        try
        {
            ExperimentAnnotations exptAnnotations = jobSupport.getExpAnnotations();

            job.getLogger().info("");
            job.getLogger().info("Validating data for experiment.");
            // writeExperiment(jobSupport, exptAnnotations, job.getUser());
            job.getLogger().info("");
            job.getLogger().info("Data validation complete.");
            // TODO:  write a summary?
        }
        catch (Throwable t)
        {
            job.getLogger().fatal("");
            job.getLogger().fatal("Error validating experiment data", t);
            job.getLogger().fatal("Data validation FAILED");
            throw new PipelineJobException(t) {};
        }
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, PxDataValidationTask.Factory>
    {
        public Factory()
        {
            super(PxDataValidationTask.class);
        }

        @Override
        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new PxDataValidationTask(this, job);
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
            return "VALIDATE EXPERIMENT";
        }

        @Override
        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }
    }
}
