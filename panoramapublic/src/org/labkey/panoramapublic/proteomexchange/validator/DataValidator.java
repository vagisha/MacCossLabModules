package org.labkey.panoramapublic.proteomexchange.validator;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.files.FileContentService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.FileUtil;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.validation.DataFile;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.DataValidationException;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.Modification.ModType;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile;
import org.labkey.panoramapublic.proteomexchange.ExperimentModificationGetter;
import org.labkey.panoramapublic.query.DataValidationManager;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;
import org.labkey.panoramapublic.speclib.LibSourceFile;
import org.labkey.panoramapublic.speclib.SpecLibReader;
import org.labkey.panoramapublic.speclib.SpecLibReaderException;

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

import static org.labkey.panoramapublic.model.validation.SpecLibSourceFile.LibrarySourceFileType.*;

public class DataValidator
{
    private final ExperimentAnnotations _expAnnotations;
    private final DataValidation _validation;
    private final DataValidatorListener _listener;

    public DataValidator(@NotNull ExperimentAnnotations expAnnotations, @NotNull DataValidation validation, @NotNull DataValidatorListener listener)
    {
        _expAnnotations = expAnnotations;
        _validation = validation;
        _listener = listener;
    }

    public ValidatorStatus validateExperiment(User user) throws DataValidationException
    {
            ValidatorStatus status = initValidationStatus(_validation, user);
            _listener.started(status);

            TargetedMSService svc = TargetedMSService.get();
            validate(status, svc, user);

            return status;
    }

    private void validate(ValidatorStatus status, TargetedMSService svc, User user) throws DataValidationException
    {
        validateSampleFiles(status, svc, user);
        validateModifications(status, user);
        validateLibraries(status, svc, user);
        status.getValidation().setStatus(status.getPxStatus());
        DataValidationManager.updateValidationStatus(status.getValidation(), user);
    }

    private void sleep()
    {
        try
        {
            Thread.sleep(1*1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void validateLibraries(ValidatorStatus status, TargetedMSService svc, User user) throws DataValidationException
    {
        _listener.validatingSpectralLibraries();
        sleep(); // TODO: remove this
        FileContentService fcs = FileContentService.get();
        for (ValidatorSpecLib specLib: status.getSpectralLibraries())
        {
            try (DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
            {
                validateLibrary(specLib, status, user, fcs);
                transaction.commit();
            }
        }
        _listener.spectralLibrariesValidated(status);
    }

    private void validateLibrary(ValidatorSpecLib specLib, ValidatorStatus status, User user, FileContentService fcs) throws DataValidationException
    {
        specLib.setValidationId(status.getValidation().getId());
        DataValidationManager.saveSpectrumLibrary(specLib, user);

        if (specLib.isMissingInSkyZip())
        {
            for (ValidatorSpecLib.DocLib docLib: specLib.getDocumentLibraries())
            {
                ValidatorSkylineDocSpecLib docLibV = new ValidatorSkylineDocSpecLib(docLib.getLibrary());
                docLibV.setSpeclibValidationId(specLib.getId());
                docLibV.setSkylineDocValidationId(docLib.getDocument().getId());
                docLib.getDocument().addSpecLib(docLibV);
                DataValidationManager.saveDocSpectrumLibrary(docLibV, user);
            }
        }
        else
        {
            List<LibSourceFile> sources = null;
            for (ValidatorSpecLib.DocLib docLib: specLib.getDocumentLibraries())
            {
                ISpectrumLibrary isl = docLib.getLibrary();
                SpecLibReader libReader = SpecLibReader.getReader(isl);
                List<LibSourceFile> docLibSources = null;
                if (libReader != null)
                {
                    try
                    {
                        docLibSources = libReader.readLibSourceFiles(docLib.getDocument().getRun(), isl);
                    }
                    catch (SpecLibReaderException e)
                    {
                        // TODO: library file exists but there was an error reading the library.
                        e.printStackTrace();
                    }
                }

                if (sources == null)
                {
                    sources = docLibSources;
                }
                else if(!areSameSources(sources, docLibSources))
                {
                    specLib.removeSkylineDoc(docLib.getDocument());
                    ValidatorSpecLib newSpecLib = new ValidatorSpecLib();
                    newSpecLib.setLibName(specLib.getLibName());
                    newSpecLib.setFileName(specLib.getFileName());
                    newSpecLib.setSize(specLib.getSize());
                    newSpecLib.setLibType(specLib.getLibType());
                    newSpecLib.addDocumentLibrary(docLib.getDocument(), docLib.getLibrary());
                    validateLibrary(newSpecLib, status, user, fcs);
                    continue;
                }

                ValidatorSkylineDocSpecLib docLibV = new ValidatorSkylineDocSpecLib(docLib.getLibrary());
                docLibV.setSpeclibValidationId(specLib.getId());
                docLibV.setSkylineDocValidationId(docLib.getDocument().getId());
                docLibV.setIncluded(specLib.getSize() != null);
                docLib.getDocument().addSpecLib(docLibV);
                DataValidationManager.saveDocSpectrumLibrary(docLibV, user);
            }
            if (sources != null) // Sources were not found either because the library is not supported or e.g. the required table was not found in the .blib
            {
                validateLibrarySources(specLib, sources, user, fcs);
            }
        }
    }

    private void validateLibrarySources(ValidatorSpecLib specLib, List<LibSourceFile> sources, User user, FileContentService fcs) throws DataValidationException
    {
        Set<String> checkedFiles = new HashSet<>();
        List<Container> containers = new ArrayList<>();
        containers.addAll(specLib.getDocumentLibraries().stream().map(dl -> dl.getDocument().getContainer()).collect(Collectors.toList()));
        containers.add(_expAnnotations.getContainer());

        List<SpecLibSourceFile> spectrumFiles = new ArrayList<>();
        List<SpecLibSourceFile> idFiles = new ArrayList<>();

        for (LibSourceFile source: sources)
        {
            String ssf = source.getSpectrumSourceFile();
            if (source.hasSpectrumSourceFile() && !checkedFiles.contains(ssf))
            {
                checkedFiles.add(ssf);
                boolean isMaxquant = (source.hasIdFile() && source.getIdFile().endsWith("msms.txt")) || source.containsScoreType("MAXQUANT SCORE");
                String ssfName = FilenameUtils.getName(ssf);
                Path path = getPath(ssfName, containers, isMaxquant, fcs);
                SpecLibSourceFile sourceFile = new SpecLibSourceFile(ssfName, SPECTRUM);
                sourceFile.setSpecLibValidationId(specLib.getId());
                sourceFile.setPath(path != null ? path.toString() : DataFile.NOT_FOUND);
                DataValidationManager.saveSpecLibSourceFile(sourceFile, user);
                spectrumFiles.add(sourceFile);
            }
            String idFile = source.getIdFile();
            if (source.hasIdFile() && !checkedFiles.contains(idFile))
            {
                checkedFiles.add(idFile);
                String idFileName = FilenameUtils.getName(idFile);
                Path path = getPath(idFileName, containers, false, fcs);
                SpecLibSourceFile sourceFile = new SpecLibSourceFile(idFileName, ID);
                sourceFile.setSpecLibValidationId(specLib.getId());
                sourceFile.setPath(path != null ? path.toString() : DataFile.NOT_FOUND);
                DataValidationManager.saveSpecLibSourceFile(sourceFile, user);
                idFiles.add(sourceFile);
            }
        }
        specLib.setSpectrumFiles(spectrumFiles);
        specLib.setIdFiles(idFiles);
    }

    private Path getPath(String name, List<Container> containers, boolean isMaxquant, FileContentService fcs) throws DataValidationException
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

    private Path findInDirectoryTree(java.nio.file.Path rawFilesDirPath, String fileName, boolean allowBaseName) throws DataValidationException
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
            throw new DataValidationException("Error looking for files in " + rawFilesDirPath, e);
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
            throw new DataValidationException("Error looking for files in sub-directories of" + rawFilesDirPath, e);
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
        // TODO: Test this method
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

    private void validateModifications(ValidatorStatus status, User user)
    {
        _listener.validatingModifications();
        sleep(); // TODO: remove this
        try (DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
        {
            List<ExperimentModificationGetter.PxModification> mods = ExperimentModificationGetter.getModifications(_expAnnotations);
            for (ExperimentModificationGetter.PxModification pxMod : mods)
            {
                Modification mod = new Modification(pxMod.getSkylineName(), pxMod.getDbModId(),
                        pxMod.getUnimodIdInt(),
                        pxMod.getName(),
                        pxMod.isIsotopicMod() ? ModType.ISOTOPIC : ModType.STRUCTURAL);
                if (pxMod.hasPossibleUnimods())
                {
                    mod.setPossibleUnimodMatches(pxMod.getPossibleUnimodMatches());
                }
                mod.setValidationId(status.getValidation().getId());
                DataValidationManager.saveModification(mod, user);
                status.addModification(mod);

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

            transaction.commit();
        }
        _listener.modificationsValidated(status);
    }

    private void validateSampleFiles(ValidatorStatus status, TargetedMSService svc, User user)
    {
        for (ValidatorSkylineDoc skyDoc: status.getSkylineDocs())
        {
            _listener.validatingDocument(skyDoc);
            sleep(); // TODO remove this
            try (DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
            {
                List<ISampleFile> sampleFiles = skyDoc.getSampleFiles().stream().map(s -> s.getSampleFile()).collect(Collectors.toList());
                Map<String, Path> paths = svc.getSampleFilesPaths(sampleFiles, skyDoc.getContainer(), false);

                Set<String> duplicateSkylineSampleFileNames = getDuplicateSkylineSampleFileNames(skyDoc.getSampleFiles());
                for (SkylineDocSampleFile sampleFile : skyDoc.getSampleFiles())
                {
                    if (duplicateSkylineSampleFileNames.contains(sampleFile.getSkylineName()))
                    {
                        // Sample file names in Skyline documents submitted to Panorama Public must have unique names.
                        // We do not allow sample files with the same name but different paths imported into separate
                        // replicates, for example. Skyline allows this but it can get very confusing even for the user.
                        sampleFile.setPath(DataFile.AMBIGUOUS);
                    }
                    else
                    {
                        Path path = paths.get(sampleFile.getName());
                        sampleFile.setPath(path != null ? path.toString() : DataFile.NOT_FOUND);
                    }
                }

                DataValidationManager.updateSampleFileStatus(skyDoc, user);
                transaction.commit();
            }

            if (_listener != null)
            {
                _listener.sampleFilesValidated(skyDoc, status);
            }
        }
    }

    private Set<String> getDuplicateSkylineSampleFileNames(List<ValidatorSampleFile> sampleFiles)
    {
        Map<String, Integer> counts = sampleFiles.stream().collect(Collectors.toMap(ValidatorSampleFile::getSkylineName, value -> 1, Integer::sum));
        Set<String> duplicates = new HashSet<>();
        counts.entrySet().stream().filter(entry -> entry.getValue() > 1).forEach(entry -> duplicates.add(entry.getKey()));
        return duplicates;
    }

    private ValidatorStatus initValidationStatus(DataValidation validation, User user) throws DataValidationException
    {
        try (DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
        {
            ValidatorStatus status = new ValidatorStatus(validation);
            TargetedMSService targetedMsSvc = TargetedMSService.get();
            addSkylineDocs(status, targetedMsSvc);
            addSpectralLibraries(status, targetedMsSvc);

            DataValidationManager.saveStatus(status, user);

            transaction.commit();
            return status;
        }
    }

    private void addSkylineDocs(ValidatorStatus status, TargetedMSService targetedMsSvc)
    {
        // Get a list of Skyline documents associated with this experiment
        List<ITargetedMSRun> runs = ExperimentAnnotationsManager.getTargetedMSRuns(_expAnnotations);

        for(ITargetedMSRun run: runs)
        {
            ValidatorSkylineDoc skyDoc = new ValidatorSkylineDoc(run);
            skyDoc.setName(run.getFileName());
            skyDoc.setRunId(run.getId());
            skyDoc.setContainer(run.getContainer());
            status.addSkylineDoc(skyDoc);

            addSampleFiles(skyDoc, targetedMsSvc);
        }
    }

    private void addSpectralLibraries(ValidatorStatus status, TargetedMSService targetedMsSvc) throws DataValidationException
    {
        Map<String, ValidatorSpecLib> spectrumLibraries = new HashMap<>();

        for (ValidatorSkylineDoc doc: status.getSkylineDocs())
        {
            List<? extends ISpectrumLibrary> allSpecLibs = targetedMsSvc.getLibraries(doc.getRun());
            for (ISpectrumLibrary lib: allSpecLibs)
            {
                ValidatorSpecLib sLib = getSpectrumLibrary(targetedMsSvc, doc, lib);
                spectrumLibraries.putIfAbsent(sLib.getKey(), sLib);
                sLib.addDocumentLibrary(doc, lib);
            }
        }
        spectrumLibraries.values().forEach(status::addLibrary);
    }

    private ValidatorSpecLib getSpectrumLibrary(TargetedMSService targetedMsSvc, ValidatorSkylineDoc doc, ISpectrumLibrary lib) throws DataValidationException
    {
        ValidatorSpecLib sLib = new ValidatorSpecLib();
        sLib.setLibName(lib.getName());
        sLib.setFileName(lib.getFileNameHint());
        sLib.setLibType(lib.getLibraryType());
        Path libPath = targetedMsSvc.getLibraryFilePath(doc.getRun(), lib);
        if(libPath != null && Files.exists(libPath))
        {
            try
            {
                sLib.setSize(Files.size(libPath));
            }
            catch (IOException e)
            {
                throw new DataValidationException("Error getting size of library file '" + libPath + "'.", e);
            }
        }
        return sLib;
    }

    private void addSampleFiles(ValidatorSkylineDoc skylineDoc, TargetedMSService svc)
    {
        List<? extends ISampleFile> sampleFiles = svc.getSampleFiles(skylineDoc.getRunId());

        Set<String> sciexWiffFileNames = new HashSet<>();

        for (ISampleFile s: sampleFiles)
        {
            ValidatorSampleFile sampleFile = new ValidatorSampleFile(s);
            boolean sciexWiff = isSciexWiff(s.getFileName());
            if (sciexWiff)
            {
                if (sciexWiffFileNames.contains(sampleFile.getName()) && !sampleFile.getName().equals(sampleFile.getSkylineName()))
                {
                    // Multi-injection SCIEX wiff files will have the same file name but different skyline file name.
                    // We don't want to add this twice to the list of document sample files.
                    // Example: D:\Data\CPTAC_Study9s\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_07|6
                    // SampleFile.getName(): Site52_041009_Study9S_Phase-I.wiff
                    // SampleFile.getSkylineName(): Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_07|6
                    continue;
                }
                sciexWiffFileNames.add(sampleFile.getName());
            }

            skylineDoc.addSampleFile(sampleFile);

            if (sciexWiff)
            {
                // If this is a SCIEX .wiff file we will also look for the corresponding .wiff.scan file
                String fileName = s.getFileName() + ".scan";
                ValidatorSampleFile wiffScanFile = new ValidatorSampleFile(new ISampleFile()
                {
                    @Override
                    public String getFileName()
                    {
                        return fileName;
                    }

                    @Override
                    public String getFilePath()
                    {
                        return s.getFilePath().replace(".wiff", ".wiff.scan");
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
