package org.labkey.panoramapublic.pipeline;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.CancelledException;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.util.FileType;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.SkylineDocValidating;
import org.labkey.panoramapublic.model.validation.SpecLibValidating;
import org.labkey.panoramapublic.model.validation.StatusValidating;
import org.labkey.panoramapublic.proteomexchange.DataValidator;
import org.labkey.panoramapublic.proteomexchange.DataValidatorListener;
import org.labkey.panoramapublic.query.DataValidationManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Logger log = job.getLogger();
        try
        {
            ExperimentAnnotations exptAnnotations = jobSupport.getExpAnnotations();
            DataValidation validation = DataValidationManager.getValidation(jobSupport.getValidationId(), exptAnnotations.getContainer());
            if (validation == null)
            {
                throw new PipelineJobException(String.format("Could not find a data validation row for Id %d in folder '%s'.",
                        jobSupport.getValidationId(), exptAnnotations.getContainer().getPath()));
            }
            log.info("");
            log.info(String.format("Validating data for experiment Id: %d, validation Id: %d", exptAnnotations.getId(), validation.getId()));
            // SubmissionDataStatus status = SubmissionDataValidator.validateExperiment(exptAnnotations);
            Integer pipelineJobId = (PipelineService.get().getJobId(job.getUser(), job.getContainer(), job.getJobGUID()));
            ValidatorListener listener = new ValidatorListener(job);
            DataValidator validator = new DataValidator(exptAnnotations, validation, pipelineJobId, listener);
            StatusValidating status = validator.validateExperiment(job.getUser());

            log.info("");
            log.info("Data validation complete.");
        }
        catch (CancelledException e)
        {
            log.info("Data validation job was cancelled.");
            throw e;
        }
        catch (PipelineJobException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            log.fatal("");
            log.fatal("Error validating experiment data", t);
            log.fatal("Data validation FAILED");
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

    public static class ValidatorListener implements DataValidatorListener
    {
        private final Logger _log;
        private final PipelineJob _job;

        public ValidatorListener(PipelineJob job)
        {
            _log = job.getLogger();
            _job = job;
        }

        @Override
        public void started(StatusValidating status)
        {
            _job.setStatus("Starting data validation");
            _log.info("Validating data for %d Skyline documents in %d folders", status.getSkylineDocs().size(),
                    status.getSkylineDocs().stream().map(doc -> doc.getContainer()).distinct().count());
        }

        @Override
        public void validatingDocument(SkylineDocValidating document)
        {
            _job.setStatus("Validating document " + document.getName());
        }

        @Override
        public void sampleFilesValidated(SkylineDocValidating document, StatusValidating status)
        {
            _log.info("Sample file validation for Skyline document: " + document.getName());
            if (document.foundAllSampleFiles())
            {
                _log.info("  Found all sample files.");
            }
            else
            {
                _log.info("  MISSING SAMPLE FILES:");
                document.getMissingSampleFileNames().stream().forEach(name -> _log.info("    " + name));
            }
        }

        @Override
        public void validatingModifications()
        {
            _job.setStatus("Validating modifications");
        }

        @Override
        public void modificationsValidated(StatusValidating status)
        {
            _log.info("Modifications validation:");
            if (status.getModifications().size() == 0)
            {
                _log.info("No modifications were found in the submitted Skyline documents.");
            }
            else
            {
                Map<Boolean, List<Modification>> modGroups = status.getModifications().stream().collect(Collectors.partitioningBy(mod -> mod.isValid()));
                _log.info("VALID MODIFICATIONS:");
                for (Modification mod: modGroups.get(Boolean.TRUE))
                {
                    _log.info(mod.getId() + ": " + mod);
                    status.getSkylineDocs().stream().filter(doc -> doc.hasModification(mod)).forEach(doc -> _log.info("    " + doc.getName()));
                }
                _log.info("INVALID MODIFICATIONS (No Unimod ID):");
                for (Modification mod: modGroups.get(Boolean.FALSE))
                {
                    _log.info(mod.getId() + ": " + mod);
                    status.getSkylineDocs().stream().filter(doc -> doc.hasModification(mod)).forEach(doc -> _log.info("    " + doc.getName()));
                }
            }
        }

        @Override
        public void validatingSpectralLibraries()
        {
            _job.setStatus("Validating spectral libraries");
        }

        @Override
        public void spectralLibrariesValidated(StatusValidating status)
        {
            _log.info("Spectral library validation:");
            if (status.getSpectralLibraries().size() == 0)
            {
                _log.info("No spectral libraries were found in the submitted Skyline documents.");
            }
            else
            {
                for (SpecLibValidating specLib: status.getSpectralLibraries())
                {
                    _log.info(specLib.toString());
                    if (specLib.hasMissingSpectrumFiles() || specLib.hasMissingIdFiles())
                    {
                        _log.info("  MISSING FILES:");
                        for (String name: specLib.getMissingSpectrumFileNames())
                        {
                            _log.info("    Spectrum File: " + name);
                        }
                        for (String name: specLib.getMissingIdFileNames())
                        {
                            _log.info("    Peptide Id File: " + name);
                        }
                    }
                }
            }
        }
    }
}
