package org.labkey.panoramapublic.proteomexchange;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.files.FileContentService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.LibSourceFile;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Pair;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.validation.DataFile;
import org.labkey.panoramapublic.model.validation.DataValidationException;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.Modification.ModType;
import org.labkey.panoramapublic.model.validation.SampleFileValidating;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;
import org.labkey.panoramapublic.model.validation.SkylineDocSpecLibValidating;
import org.labkey.panoramapublic.model.validation.SkylineDocValidating;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile;
import org.labkey.panoramapublic.model.validation.SpecLibValidating;
import org.labkey.panoramapublic.model.validation.StatusValidating;
import org.labkey.panoramapublic.query.DataValidationManager;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        FileContentService fcs = FileContentService.get();
        for (SpecLibValidating specLib: status.getSpectrumLibraries())
        {
            validateLibrary(specLib, status, svc, user, fcs);
        }
    }

    private void validateLibrary(SpecLibValidating specLib, StatusValidating status, TargetedMSService svc, User user, FileContentService fcs)
    {
        specLib.setValidationId(status.getId());
        DataValidationManager.saveSpectrumLibrary(specLib, user);

        if (specLib.isMissingInSkyDoc())
        {
            for (Pair<SkylineDocValidating, ISpectrumLibrary> docLib: specLib.getDocumentLibraries())
            {
                SkylineDocSpecLibValidating docLibV = new SkylineDocSpecLibValidating(docLib.second);
                docLibV.setSpeclibValidationId(specLib.getId());
                docLibV.setSkylineDocValidationId(docLib.first.getId());
                docLib.first.addSpecLib(docLibV);
                DataValidationManager.saveDocSpectrumLibrary(docLibV, user);
            }
        }
        else
        {
            List<LibSourceFile> sources = null;
            for (Pair<SkylineDocValidating, ISpectrumLibrary> docLib: specLib.getDocumentLibraries())
            {
                ISpectrumLibrary isl = docLib.second;
                List<LibSourceFile> docLibSources = svc.getLibrarySourceFiles(docLib.first.getRun(), isl);
                if (sources == null)
                {
                    sources = docLibSources;
                }
                else if(!areSameSources(sources, docLibSources))
                {
                    specLib.removeSkylineDoc(docLib.first);
                    SpecLibValidating newSpecLib = new SpecLibValidating();
                    newSpecLib.setLibName(specLib.getLibName());
                    newSpecLib.setFileName(specLib.getFileName());
                    newSpecLib.setDiskSize(specLib.getDiskSize());
                    newSpecLib.setLibType(specLib.getLibType());
                    newSpecLib.addDocumentLibrary(docLib.first, docLib.second);
                    validateLibrary(newSpecLib, status, svc, user, fcs);
                }
                else
                {
                    SkylineDocSpecLibValidating docLibV = new SkylineDocSpecLibValidating(docLib.second);
                    docLibV.setSpeclibValidationId(specLib.getId());
                    docLibV.setSkylineDocValidationId(docLib.first.getId());
                    docLib.first.addSpecLib(docLibV);
                    DataValidationManager.saveDocSpectrumLibrary(docLibV, user);
                }
            }
            validateLibrarySources(specLib, sources, user, fcs);
        }
    }

    private void validateLibrarySources(SpecLibValidating specLib, List<LibSourceFile> sources, User user, FileContentService fcs)
    {
        Set<String> checkedFiles = new HashSet<>();
        List<Container> containers = new ArrayList<>();
        containers.addAll(specLib.getDocumentLibraries().stream().map(pair -> pair.first.getContainer()).collect(Collectors.toList()));
        containers.add(_expAnnotations.getContainer());

        for (LibSourceFile source: sources)
        {
            String ssf = source.getSpectrumSourceFile();
            if (source.hasSpectrumSourceFile() && !checkedFiles.contains(ssf))
            {
                checkedFiles.add(ssf);
                boolean isMaxquant = (source.hasIdFile() && source.getIdFile().endsWith("msms.txt")) || source.containsScoreType("MAXQUANT SCORE");
                String ssfName = FilenameUtils.getName(ssf);
                Path path = getPath(ssfName, containers, isMaxquant, fcs);
                SpecLibSourceFile sourceFile = new SpecLibSourceFile(ssfName, SpecLibSourceFile.SPECTRUM_SOURCE);
                sourceFile.setSpecLibValidationId(specLib.getId());
                sourceFile.setPath(path != null ? path.toString() : DataFile.NOT_FOUND);
                specLib.addSpectrumFile(sourceFile);
                DataValidationManager.saveSpecLibSourceFile(sourceFile, user);
            }
            String idFile = source.getIdFile();
            if (source.hasIdFile() && !checkedFiles.contains(idFile))
            {
                checkedFiles.add(idFile);
                String idFileName = FilenameUtils.getName(idFile);
                Path path = getPath(idFileName, containers, false, fcs);
                SpecLibSourceFile sourceFile = new SpecLibSourceFile(idFileName, SpecLibSourceFile.ID_SOURCE);
                sourceFile.setSpecLibValidationId(specLib.getId());
                sourceFile.setPath(path != null ? path.toString() : DataFile.NOT_FOUND);
                specLib.addSpectrumFile(sourceFile);
                DataValidationManager.saveSpecLibSourceFile(sourceFile, user);
            }
        }
    }

    private Path getPath(String name, List<Container> containers, boolean isMaxquant, FileContentService fcs)
    {
        for (Container container: containers)
        {
            java.nio.file.Path rawFilesDir = getRawFilesDirPath(container, fcs);
            Path path = findInDirectoryTree(rawFilesDir, name, isMaxquant);
            if (path != null)
            {
                return path;
            }
        }
        return null;
    }

    private static java.nio.file.Path getRawFilesDirPath(Container c, FileContentService fcs)
    {
        if(fcs != null)
        {
            java.nio.file.Path fileRoot = fcs.getFileRootPath(c, FileContentService.ContentType.files);
            if (fileRoot != null)
            {
                return fileRoot.resolve(TargetedMSService.RAW_FILES_DIR);
            }
        }
        return null;
    }

    private Path findInDirectoryTree(java.nio.file.Path rawFilesDirPath, String fileName, boolean allowBaseName)
    {
        try
        {
            Path path = getPath(rawFilesDirPath, fileName, allowBaseName);
            if (path != null)
            {
                return path;
            }
        }
        catch (IOException e)
        {
            _log.error("Error looking for files in " + rawFilesDirPath, e);
            return null;
        }

        // Look in subdirectories
        try (Stream<Path> list = Files.walk(rawFilesDirPath).filter(p -> Files.isDirectory(p)))
        {
            for (Path subDir : list.collect(Collectors.toList()))
            {
                Path path = getPath(subDir, fileName, allowBaseName);
                if (path != null)
                {
                    return path;
                }
            }
        }
        catch (IOException e)
        {
            _log.error("Error looking for files in sub-directories of" + rawFilesDirPath, e);
            return null;
        }
        return null;
    }

    private @Nullable Path getPath(Path rawFilesDirPath, String fileName, boolean allowBaseName) throws IOException
    {
        Path filePath = rawFilesDirPath.resolve(fileName);
        if(Files.exists(filePath) || Files.isDirectory(filePath))
        {
            return filePath;
        }

        // Look for zip files
        try (Stream<Path> list = Files.list(rawFilesDirPath).filter(p -> FileUtil.getFileName(p).startsWith(fileName)))
        {
            for (Path path : list.collect(Collectors.toList()))
            {
                String name = FileUtil.getFileName(path);
                if(accept(fileName, name, allowBaseName))
                {
                    return rawFilesDirPath.resolve(name);
                }
            }
        }
        return null;
    }

    private static boolean accept(String fileName, String uploadedFileName, boolean allowBasenameOnly)
    {
        // Accept QC_10.9.17.raw OR for QC_10.9.17.raw.zip
        // 170428_DBS_cal_7a.d OR 170428_DBS_cal_7a.d.zip
        String ext = FileUtil.getExtension(uploadedFileName).toLowerCase();
        return fileName.equals(uploadedFileName)
                || ext.equals("zip") && fileName.equals(FileUtil.getBaseName(uploadedFileName))
                || (allowBasenameOnly && fileName.equals(FileUtil.getBaseName(uploadedFileName)));
    }

    private boolean areSameSources(List<LibSourceFile> sources, List<LibSourceFile> docLibSources)
    {
        if (sources.size() == docLibSources.size())
        {
            sources.sort(Comparator.comparing(LibSourceFile::getSpectrumSourceFile)
            .thenComparing(LibSourceFile::getIdFile));

            docLibSources.sort(Comparator.comparing(LibSourceFile::getSpectrumSourceFile)
                    .thenComparing(LibSourceFile::getIdFile));

            return sources.equals(docLibSources);
        }
        return false;
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
        Map<String, SpecLibValidating> spectrumLibraries = new HashMap<>();

        for (SkylineDocValidating doc: status.getSkylineDocs())
        {
            List<? extends ISpectrumLibrary> allSpecLibs = targetedMsSvc.getLibraries(doc.getRun());
            for (ISpectrumLibrary lib: allSpecLibs)
            {
                SpecLibValidating sLib = getSpectrumLibrary(targetedMsSvc, doc, lib);
                sLib = spectrumLibraries.putIfAbsent(sLib.getKey(), sLib);
                sLib.addDocumentLibrary(doc, lib);
            }
        }
        spectrumLibraries.values().forEach(status::addLibrary);
    }

    private SpecLibValidating getSpectrumLibrary(TargetedMSService targetedMsSvc, SkylineDocValidating doc, ISpectrumLibrary lib) throws DataValidationException
    {
        SpecLibValidating sLib = new SpecLibValidating();
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
        return sLib;
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
