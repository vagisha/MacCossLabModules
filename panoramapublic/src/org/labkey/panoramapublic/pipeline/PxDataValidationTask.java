package org.labkey.panoramapublic.pipeline;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.util.FileType;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.validation.SkylineDocValidating;
import org.labkey.panoramapublic.model.validation.Status;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.SkylineDoc;
import org.labkey.panoramapublic.model.validation.StatusValidating;
import org.labkey.panoramapublic.proteomexchange.DataValidator;

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
            Logger log = job.getLogger();
            log.info("");
            log.info("Validating data for experiment '" + exptAnnotations.getTitle() + "'.");
            // SubmissionDataStatus status = SubmissionDataValidator.validateExperiment(exptAnnotations);
            Integer pipelineJobId = (PipelineService.get().getJobId(job.getUser(), job.getContainer(), job.getJobGUID()));
            DataValidator validator = new DataValidator(exptAnnotations, pipelineJobId, job.getLogger());
            StatusValidating status = validator.validateExperiment(job.getUser());
            log.info("Skyline Document validation: ");
            for (SkylineDocValidating doc: status.getSkylineDocs())
            {
                log.info(doc.getId() + ": " + doc.getName() +": VALID: " + doc.isValid());
                if (!doc.isValid())
                {
                    log.info("  MISSING SAMPLE FILES:");
                    doc.getMissingSampleFileNames().stream().forEach(name -> log.info("    " + name));
                }
            }
            log.info("Modification validation:");
            if (status.getModifications().size() == 0)
            {
                log.info("No modifications were found in the submitted Skyline documents.");
            }
            else
            {
                for (Modification mod: status.getModifications())
                {
                    log.info(mod.getId() + ": " + mod);
                    status.getSkylineDocs().stream().filter(doc -> doc.hasModification(mod)).forEach(doc -> log.info("    " + doc.getName()));
                }
            }
            log.info("");
            log.info("Data validation complete.");
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
