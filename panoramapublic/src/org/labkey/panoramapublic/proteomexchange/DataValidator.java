package org.labkey.panoramapublic.proteomexchange;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.LibSourceFile;
import org.labkey.api.targetedms.LibrarySourceFiles;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.validation.DataValidationException;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.Modification.ModType;
import org.labkey.panoramapublic.model.validation.SampleFileValidating;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;
import org.labkey.panoramapublic.model.validation.SkylineDocSpecLibValidating;
import org.labkey.panoramapublic.model.validation.SkylineDocValidating;
import org.labkey.panoramapublic.model.validation.SpecLib;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile;
import org.labkey.panoramapublic.model.validation.SpecLibValidating;
import org.labkey.panoramapublic.model.validation.StatusValidating;
import org.labkey.panoramapublic.query.DataValidationManager;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
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

    public StatusValidating validateExperiment(User user) throws DataValidationException
    {
        StatusValidating status = initValidationStatus();
        DataValidationManager.saveStatus(status, user);

        TargetedMSService svc = TargetedMSService.get();
        validate(status, svc, user);
        return status;
    }

    private void validate(StatusValidating status, TargetedMSService svc, User user)
    {
        validateModifications(status, user);
        validateSampleFiles(status, svc, user);
        validateLibraries(status, svc, user);
    }

    private void validateLibraries(StatusValidating status, TargetedMSService svc, User user)
    {
        Set<SpecLib> specLibs = new HashSet<>();
        for (SkylineDocValidating doc: status.getSkylineDocs())
        {
            for (SkylineDocSpecLibValidating docSpecLib: doc.getSpecLibraries())
            {
                if (!docSpecLib.isIncluded())
                {
                    continue;
                }

                SpecLib lib = addSpecLib(doc, docSpecLib, svc, specLibs);


                SkylineDocSpecLibValidating sLib = new SkylineDocSpecLibValidating(lib);
                sLib.setLibName(lib.getName());
                sLib.setFileName(lib.getFileNameHint());
                sLib.setLibType(lib.getLibraryType());
                Path libPath = targetedMsSvc.getSpectrumLibraryPath(doc.getRun(), lib);
                if(Files.exists(libPath))
                {
                    try
                    {
                        sLib.setDiskSize(Files.size(libPath));
                    }
                    catch (IOException e)
                    {
                        throw new DataValidationException("Error getting size of library file '" + libPath + "'.", e);
                    }
                }
                doc.addSpecLib(sLib);
            }
        }
    }

    private SpecLib addSpecLib(SkylineDocValidating doc, SkylineDocSpecLibValidating docSpecLib, TargetedMSService svc, Set<SpecLib> specLibs)
    {
        ISpectrumLibrary isl = docSpecLib.getLibrary();
        List<LibSourceFile> sources = svc.getLibrarySourceFiles(doc.getRun(), isl);
        SpecLib lib = new SpecLib();
        lib.setLibName(isl.getName());
        lib.setFileName(isl.getFileNameHint());
        lib.setLibType(isl.getLibraryType());
        lib.setDiskSize(docSpecLib.getDiskSize());
        Set<String> specFileNames = new HashSet<>();
        Set<String> idFileNames = new HashSet<>();
        List<SpecLibSourceFile> spectrumFiles = new ArrayList<>();
        List<SpecLibSourceFile> idFiles = new ArrayList<>();
        for (LibSourceFile source: sources)
        {
            String spectrumFile = source.getSpectrumSourceFile();
            String idFile = source.getIdFile();
            if (source.hasSpectrumSourceFile() && !spectrumFiles.contains(spectrumFile))
            {
                spectrumFiles.add(new SpecLibSourceFile(spectrumFile, SpecLibSourceFile.SPECTRUM_SOURCE));
                specFileNames.add(spectrumFile);
            }
            if (source.hasIdFile() && !idFileNames.contains(idFile))
            {
                idFiles.add(new SpecLibSourceFile(idFile, SpecLibSourceFile.ID_SOURCE));
                idFileNames.add(idFile);
            }
        }
        lib.setSpectrumFiles(spectrumFiles);
        lib.setIdFiles(idFiles);
        if(!specLibs.contains(lib))
        {
            lib = validateSpecLib(lib);
            specLibs.add(lib);
        }
        docSpecLib.setSpeclibValidationId(lib.getId());
    }

    private void validateModifications(StatusValidating status, User user)
    {
        List<ExperimentModificationGetter.PxModification> mods = ExperimentModificationGetter.getModifications(_expAnnotations);
        for (ExperimentModificationGetter.PxModification pxMod: mods)
        {
            Modification mod = new Modification(pxMod.getSkylineName(),
                    pxMod.getUnimodIdInt(),
                    pxMod.getName(),
                    pxMod.isIsotopicMod() ? ModType.ISOTOPIC : ModType.STRUCTURAL);
            status.addModification(mod);
            DataValidationManager.saveModifications(status, user);

            Set<String> skylineDocsWithMod = pxMod.getSkylineDocs();
            status.getSkylineDocs().stream().forEach(doc ->
            {
                if (skylineDocsWithMod.contains(doc.getName()))
                {
                    doc.addModification(mod);
                }
            });
        }
        DataValidationManager.saveSkylineDocModifications(status.getSkylineDocs(), user);
    }

    private void validateSampleFiles(StatusValidating status, TargetedMSService svc, User user)
    {
        for (SkylineDocValidating skyDoc: status.getSkylineDocs())
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

    private StatusValidating initValidationStatus() throws DataValidationException
    {
        StatusValidating status = new StatusValidating(_expAnnotations, _jobId);
        TargetedMSService targetedMsSvc = TargetedMSService.get();
        addSkylineDocs(status, targetedMsSvc);
        addSpectralLibraries(status, targetedMsSvc);
        return status;
    }

    private void addSkylineDocs(StatusValidating status, TargetedMSService targetedMsSvc)
    {
        // Get a list of Skyline documents associated with this experiment
        List<ITargetedMSRun> runs = ExperimentAnnotationsManager.getTargetedMSRuns(_expAnnotations);

        for(ITargetedMSRun run: runs)
        {
            SkylineDocValidating skyDoc = new SkylineDocValidating(run);
            skyDoc.setName(run.getFileName());
            skyDoc.setRunId(run.getId());
            skyDoc.setContainer(run.getContainer());
            status.addSkylineDoc(skyDoc);

            addSampleFiles(skyDoc, targetedMsSvc);
        }
    }

    private void addSpectralLibraries(StatusValidating status, TargetedMSService targetedMsSvc) throws DataValidationException
    {
        for (SkylineDocValidating doc: status.getSkylineDocs())
        {
            List<? extends ISpectrumLibrary> allSpecLibs = targetedMsSvc.getLibraries(doc.getRun());
            for (ISpectrumLibrary lib: allSpecLibs)
            {
                SkylineDocSpecLibValidating sLib = new SkylineDocSpecLibValidating(lib);
                sLib.setLibName(lib.getName());
                sLib.setFileName(lib.getFileNameHint());
                sLib.setLibType(lib.getLibraryType());
                Path libPath = targetedMsSvc.getSpectrumLibraryPath(doc.getRun(), lib);
                if(Files.exists(libPath))
                {
                    try
                    {
                        sLib.setDiskSize(Files.size(libPath));
                    }
                    catch (IOException e)
                    {
                        throw new DataValidationException("Error getting size of library file '" + libPath + "'.", e);
                    }
                }
                doc.addSpecLib(sLib);
            }
        }
    }

    private void addSampleFiles(SkylineDocValidating skylineDoc, TargetedMSService svc)
    {
        List<? extends ISampleFile> sampleFiles = svc.getSampleFiles(skylineDoc.getRunId());

        for(ISampleFile s: sampleFiles)
        {
            SampleFileValidating sampleFile = new SampleFileValidating(s);
            skylineDoc.addSampleFile(sampleFile);
            if(isSciexWiff(s.getFileName()))
            {
                // If this is a SCIEX .wiff file check for the presence of the corresponding .wiff.scan file
                String fileName = s.getFileName() + ".scan";
                SampleFileValidating wiffScanFile = new SampleFileValidating(new ISampleFile()
                {
                    @Override
                    public String getFileName()
                    {
                        return fileName;
                    }

                    @Override
                    public Long getInstrumentId()
                    {
                        return s.getInstrumentId();
                    }
                });
                skylineDoc.addSampleFile(wiffScanFile);
            }
        }
    }

    private boolean isSciexWiff(String fileName)
    {
        return fileName.toLowerCase().endsWith(".wiff");
    }
}
