package org.labkey.panoramapublic.proteomexchange;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.BlibSourceFile;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.validation.DataValidationStatus;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.Modification.ModType;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;
import org.labkey.panoramapublic.model.validation.SkylineDocSpecLib;
import org.labkey.panoramapublic.model.validation.SkylineDoc;
import org.labkey.panoramapublic.model.validation.SpecLib;
import org.labkey.panoramapublic.query.DataValidationManager;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DataValidator
{
    private final ExperimentAnnotations _expAnnotations;
    private final Logger _log;
    private final int _jobId;

    public DataValidator(@NotNull ExperimentAnnotations expAnnotations, int jobId, @NotNull Logger log)
    {
        _expAnnotations = expAnnotations;
        _log = log;
        _jobId = jobId;
    }

    public DataValidationStatus validateExperiment(User user)
    {
        DataValidationStatus validationStatus = initValidationStatus();
        DataValidationManager.saveStatus(validationStatus, user);

        TargetedMSService svc = TargetedMSService.get();
        validate(validationStatus, svc, user);
        return validationStatus;
    }

    private void validate(DataValidationStatus validationStatus, TargetedMSService svc, User user)
    {
        validateSampleFiles(validationStatus, svc, user);
        // validateLibraries(validationStatus, svc, user);
        validateModifications(validationStatus, user);
    }

    private void validateModifications(DataValidationStatus validationStatus, User user)
    {
        List<ExperimentModificationGetter.PxModification> mods = ExperimentModificationGetter.getModifications(_expAnnotations);
        for (ExperimentModificationGetter.PxModification pxMod: mods)
        {
            Modification mod = new Modification(pxMod.getSkylineName(),
                    pxMod.getUnimodIdInt(),
                    pxMod.getName(),
                    pxMod.isIsotopicMod() ? ModType.ISOTOPIC : ModType.STRUCTURAL);
            validationStatus.addModification(mod);
            DataValidationManager.addModifications(validationStatus, user);

            Set<String> skylineDocsWithMod = pxMod.getSkylineDocs();
            validationStatus.getSkylineDocs().stream().forEach(doc ->
            {
                if (skylineDocsWithMod.contains(doc.getName()))
                {
                    doc.addModification(mod);
                }
            });
        }
        DataValidationManager.addSkylineDocModifications(validationStatus.getSkylineDocs(), user);
    }

    private void validateSampleFiles(DataValidationStatus validationStatus, TargetedMSService svc, User user)
    {
        for (SkylineDoc skyDoc: validationStatus.getSkylineDocs())
        {
            List<ISampleFile> sampleFiles = skyDoc.getSampleFiles().stream().map(s -> s.getSampleFile()).collect(Collectors.toList());
            Map<String, Path> paths = svc.getSampleFilesPaths(sampleFiles, skyDoc.getContainer(), false);
            for (SkylineDocSampleFile sampleFile : skyDoc.getSampleFiles())
            {
                Path path = paths.get(sampleFile.getName());
                sampleFile.setPath(path != null ? path.toString() : "NOT_FOUND");
            }

            DataValidationManager.updateSampleFileStatus(skyDoc, user);
        }
    }

    private DataValidationStatus initValidationStatus()
    {
        DataValidationStatus status = new DataValidationStatus(_expAnnotations, _jobId);
        TargetedMSService targetedMsSvc = TargetedMSService.get();
        addSkylineDocs(status, targetedMsSvc);
        // addSpectralLibraries(status, targetedMsSvc);
        return status;
    }

    private void addSkylineDocs(DataValidationStatus status, TargetedMSService targetedMsSvc)
    {
        // Get a list of Skyline documents associated with this experiment
        List<ITargetedMSRun> runs = ExperimentAnnotationsManager.getTargetedMSRuns(_expAnnotations);

        for(ITargetedMSRun run: runs)
        {
            SkylineDoc skylineDocValidation = new SkylineDoc();
            skylineDocValidation.setName(run.getFileName());
            skylineDocValidation.setRunId(run.getId());
            skylineDocValidation.setContainer(run.getContainer());
            status.addSkylineDoc(skylineDocValidation);

            addSampleFiles(skylineDocValidation, targetedMsSvc);
            addSpectralLibraries(skylineDocValidation, run, targetedMsSvc);
        }
    }

    private void addSpectralLibraries(SkylineDoc skylineDoc, ITargetedMSRun run, TargetedMSService targetedMsSvc)
    {
        List<? extends ISpectrumLibrary> allSpecLibs = targetedMsSvc.getLibraries(run);

        for(Map.Entry<String, List<BlibSourceFile>> entry : targetedMsSvc.getBlibSourceFiles(run).entrySet())
        {
            SkylineDocSpecLib docLib = new SkylineDocSpecLib();
            docLib.setName(entry.getKey());
            skylineDoc.addSpecLib(docLib);

            SpecLib specLib = new SpecLib();
            specLib.setName(entry.getKey());
            // specLib.set
        }
    }

    private void addSampleFiles(SkylineDoc skylineDoc, TargetedMSService svc)
    {
        List<? extends ISampleFile> sampleFiles = svc.getSampleFiles(skylineDoc.getRunId());

        for(ISampleFile s: sampleFiles)
        {
            SkylineDocSampleFile skylineDocSampleFile = new SkylineDocSampleFile(new SampleFile(s.getFileName(), s.getInstrumentId()));
            skylineDoc.addSampleFile(skylineDocSampleFile);
            if(isSciexWiff(s.getFileName()))
            {
                // If this is a SCIEX .wiff file check for the presence of the corresponding .wiff.scan file
                SkylineDocSampleFile wiffScanFile = new SkylineDocSampleFile(new SampleFile(s.getFileName() + ".scan", s.getInstrumentId()));
                skylineDoc.addSampleFile(wiffScanFile);
            }
        }
    }

    private boolean isSciexWiff(String fileName)
    {
        return fileName.toLowerCase().endsWith(".wiff");
    }

    public static class SampleFile implements ISampleFile
    {
        private final String _fileName;
        private final Long _instrumentId;

        public SampleFile(String fileName, Long instrumentId)
        {
            _fileName = fileName;
            _instrumentId = instrumentId;
        }

        @Override
        public String getFileName()
        {
            return _fileName;
        }

        @Override
        public Long getInstrumentId()
        {
            return _instrumentId;
        }

        @Override
        public String getFilePath()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSampleName()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getAcquiredTime()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getModifiedTime()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSkylineId()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Double getTicArea()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getInstrumentSerialNumber()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSampleId()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Double getExplicitGlobalStandardArea()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getIonMobilityType()
        {
            throw new UnsupportedOperationException();
        }
    }
}
