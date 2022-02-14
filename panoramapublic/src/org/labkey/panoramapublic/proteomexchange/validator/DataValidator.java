package org.labkey.panoramapublic.proteomexchange.validator;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.files.FileContentService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.model.SampleFilePath;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.labkey.panoramapublic.model.validation.SpecLibSourceFile.LibrarySourceFileType.*;

public class DataValidator
{
    private final ExperimentAnnotations _expAnnotations;
    private final DataValidation _validation;
    private final DataValidatorListener _listener;

    private static final String DOT_WIFF = ".wiff";
    private static final String DOT_WIFF2 = ".wiff2";
    private static final String DOT_SCAN = ".scan";

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
        validateLibraries(status, user);
        status.getValidation().setStatus(status.getPxStatus());
        DataValidationManager.updateValidationStatus(status.getValidation(), user);
    }

    private void validateLibraries(ValidatorStatus status, User user) throws DataValidationException
    {
        _listener.validatingSpectralLibraries();
        // sleep();
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
            List<LibSourceFile> libSources = null;
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
                        // Library file exists but there was an error reading the library.
                        _listener.warning(e.getMessage());
                    }
                }

                if (libSources == null)
                {
                    libSources = docLibSources;
                }
                else if(!areSameSources(libSources, docLibSources))
                {
                    throw new DataValidationException(String.format("Expected library sources to match in all documents with the library '%s'. "
                            + ". But they did not match for the library in the document '%s'.", specLib.getKey(), docLib.getDocument().getName()));
                }

                ValidatorSkylineDocSpecLib docLibV = new ValidatorSkylineDocSpecLib(docLib.getLibrary());
                docLibV.setSpeclibValidationId(specLib.getId());
                docLibV.setSkylineDocValidationId(docLib.getDocument().getId());
                docLibV.setIncluded(specLib.getSize() != null);
                // docLib.getDocument().addSpecLib(docLibV);
                DataValidationManager.saveDocSpectrumLibrary(docLibV, user);
            }

            // library sources will be null if the library is not supported, or e.g. the required table was not found in the .blib
            if (libSources != null)
            {
                validateLibrarySources(specLib, libSources, user, fcs);
            }
        }
    }

    private void validateLibrarySources(ValidatorSpecLib specLib, List<LibSourceFile> sources, User user, FileContentService fcs) throws DataValidationException
    {
        Set<String> checkedFiles = new HashSet<>();
        Set<Container> containers = new HashSet<>(specLib.getDocumentLibraries().stream().map(dl -> dl.getDocument().getContainer()).collect(Collectors.toSet()));
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

    private Path getPath(String name, Set<Container> containers, boolean isMaxquant, FileContentService fcs) throws DataValidationException
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
        try (Stream<Path> list = Files.walk(rawFilesDirPath).filter(Files::isDirectory))
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

    private static boolean areSameSources(List<LibSourceFile> sources, List<LibSourceFile> docLibSources)
    {
        if (sources != null)
        {
            sources.sort(Comparator.comparing(LibSourceFile::getSpectrumSourceFile)
                    .thenComparing(LibSourceFile::getIdFile));
        }
        if (docLibSources != null)
        {
            docLibSources.sort(Comparator.comparing(LibSourceFile::getSpectrumSourceFile)
                    .thenComparing(LibSourceFile::getIdFile));
        }
        return Objects.equals(sources, docLibSources);
    }

    private void validateModifications(ValidatorStatus status, User user)
    {
        _listener.validatingModifications();
        // sleep();
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
            // sleep();
            try (DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
            {
                List<ISampleFile> sampleFiles = skyDoc.getSampleFiles().stream().map(ValidatorSampleFile::getSampleFile).collect(Collectors.toList());
                List<SampleFilePath> paths = svc.getSampleFilePaths(sampleFiles, skyDoc.getContainer(), false);
                Map<String, Path> pathMap = new HashMap<>();
                paths.forEach(p -> pathMap.put(p.getSampleFile().getFileName(), p.getPath()));

                Set<String> duplicateSkylineSampleFileNames = getDuplicateSkylineSampleFileNames(skyDoc.getSampleFiles());
                for (SkylineDocSampleFile sampleFile : skyDoc.getSampleFiles())
                {
                    if (duplicateSkylineSampleFileNames.contains(sampleFile.getName()))
                    {
                        // We do not allow sample files with the same name but different paths imported into separate
                        // replicates. Skyline allows this but it can get confusing even for the user.
                        sampleFile.setPath(DataFile.AMBIGUOUS);
                    }
                    else
                    {
                        Path path = pathMap.get(sampleFile.getName());
                        sampleFile.setPath(path != null ? path.toString() : DataFile.NOT_FOUND);
                    }
                }

                DataValidationManager.updateSampleFileStatus(skyDoc, user);
                transaction.commit();
            }

            _listener.sampleFilesValidated(skyDoc, status);
        }
    }

    private static Set<String> getDuplicateSkylineSampleFileNames(List<ValidatorSampleFile> sampleFiles)
    {
        Map<String, Integer> counts = sampleFiles.stream().collect(Collectors.toMap(ValidatorSampleFile::getName, value -> 1, Integer::sum));
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
                spectrumLibraries.get(sLib.getKey()).addDocumentLibrary(doc, lib);
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
        List<ValidatorSampleFile> docSampleFile = getDocSampleFiles(svc.getSampleFiles(skylineDoc.getRunId()));
        docSampleFile.forEach(skylineDoc::addSampleFile);
    }

    private static List<ValidatorSampleFile> getDocSampleFiles(List<? extends ISampleFile> sampleFiles)
    {
        Set<String> pathsImported = new HashSet<>();
        Set<String> sciexWiffFileNames = new HashSet<>();

        List<ValidatorSampleFile> docSampleFiles = new ArrayList<>();

        for (ISampleFile s: sampleFiles)
        {
            ValidatorSampleFile sampleFile = new ValidatorSampleFile(s);
            if (!isSciexWiff(s.getFileName()))
            {
                if (!pathsImported.contains(s.getFilePath()))
                {
                    docSampleFiles.add(sampleFile);
                    pathsImported.add(s.getFilePath());
                }
            }
            else
            {
                String sciexSampleFilePath = getSciexSampleFilePath(s);
                if (sciexWiffFileNames.contains(sampleFile.getName()) && pathsImported.contains(sciexSampleFilePath))
                {
                    // Multi-injection SCIEX wiff files will have the same file name but different file_path attribute in the .sky XML.
                    // For <sample_file id="_6ProtMix_QC_03_f0" file_path="D:\Data\CPTAC_Study9s\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_03|2"
                    // ISampleFile.getFileName() -> Site52_041009_Study9S_Phase-I.wiff
                    // ISampleFile.getFilePath() -> D:\Data\CPTAC_Study9s\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_03|2
                    // We don't want to add this again to the list of document sample files if this is from a multi-injection wiff
                    continue;
                }

                sciexWiffFileNames.add(sampleFile.getName());
                pathsImported.add(sciexSampleFilePath);
                docSampleFiles.add(sampleFile);

                // For a SCIEX wiff file we will also add a corresponding .wiff.scan file
                ValidatorSampleFile wiffScanFile = new ValidatorSampleFile(new AbstractSampleFile()
                {
                    @Override
                    public String getFileName()
                    {
                        return s.getFileName() + DOT_SCAN;
                    }

                    @Override
                    public String getFilePath()
                    {
                        return addDotScanToPath(s);
                    }

                    @Override
                    public Long getInstrumentId()
                    {
                        return s.getInstrumentId();
                    }
                });
                docSampleFiles.add(wiffScanFile);
            }
        }
        return docSampleFiles;
    }

    private static boolean isSciexWiff(String fileName)
    {
        return fileName.toLowerCase().endsWith(DOT_WIFF) || fileName.toLowerCase().endsWith(DOT_WIFF2);
    }

    // D:\Data\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_03|2 -> D:\Data\Site52_041009_Study9S_Phase-I.wiff
    private static String getSciexSampleFilePath(ISampleFile file)
    {
        String filePath = file.getFilePath();
        int idx = filePath.indexOf(file.getFileName());
        if (idx != -1)
        {
            // Example: D:\Data\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_03|2
            // We want D:\Data\Site52_041009_Study9S_Phase-I.wiff
            return filePath.substring(0, idx + file.getFileName().length());
        }
        return filePath;
    }

    private static String addDotScanToPath(ISampleFile sampleFile)
    {
        String ext = sampleFile.getFileName().toLowerCase().endsWith(DOT_WIFF) ? DOT_WIFF : DOT_WIFF2;
        String filePath = sampleFile.getFilePath();
        int idx = filePath.toLowerCase().indexOf(ext);
        if (idx != -1)
        {
            // Path may be for a multi-injection wiff file.
            // Example: D:\Data\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_03|2
            return filePath.substring(0, idx + ext.length())
                    + DOT_SCAN
                    + filePath.substring(idx + ext.length());
        }
        return filePath;
    }

    public abstract static class AbstractSampleFile implements ISampleFile
    {
        @Override
        public String getSampleName()
        {
            return null;
        }

        @Override
        public Date getAcquiredTime()
        {
            return null;
        }

        @Override
        public Date getModifiedTime()
        {
            return null;
        }

        @Override
        public String getSkylineId()
        {
            return null;
        }

        @Override
        public Double getTicArea()
        {
            return null;
        }

        @Override
        public String getInstrumentSerialNumber()
        {
            return null;
        }

        @Override
        public String getSampleId()
        {
            return null;
        }

        @Override
        public Double getExplicitGlobalStandardArea()
        {
            return null;
        }

        @Override
        public String getIonMobilityType()
        {
            return null;
        }

        @Override
        public String getFileName()
        {
            return getFileName(getFilePath());
        }

        // Copied from SampleFile in the targetedms module.
        // TODO: Move this to the TargetedMS API
        static String getFileName(String path)
        {
            if(path != null)
            {
                // If the file path has a '?' part remove it
                // Example: 2017_July_10_bivalves_292.raw?centroid_ms2=true.
                int idx = path.indexOf('?');
                path = (idx == -1) ? path : path.substring(0, idx);

                // If the file path has a '|' part for sample name from multi-injection wiff files remove it.
                // Example: D:\Data\CPTAC_Study9s\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_07|6
                idx = path.indexOf('|');
                path =  (idx == -1) ? path : path.substring(0, idx);

                return FilenameUtils.getName(path);
            }
            return null;
        }
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testCompareLibSources()
        {
            List<LibSourceFile> source1 = new ArrayList<>();
            List<LibSourceFile> source2 = new ArrayList<>();
            assertTrue(areSameSources(source1, source2));

            LibSourceFile f1 = new LibSourceFile("C:\\LibrarySource\\file1.raw", null, null);
            LibSourceFile f2 = new LibSourceFile("C:\\LibrarySource\\file2.raw", null, null);
            LibSourceFile f3 = new LibSourceFile("C:\\LibrarySource\\file3.raw", "peptides1.pep.xml", null);
            LibSourceFile f4 = new LibSourceFile("C:\\LibrarySource\\file4.raw", "peptides2.pep.xml", null);
            LibSourceFile f5 = new LibSourceFile("C:\\LibrarySource\\file5.raw", "peptides3.pep.xml", null);
            LibSourceFile f6_same_as_f4 = new LibSourceFile("C:\\LibrarySource\\file4.raw", "peptides2.pep.xml", null);

            assertTrue(areSameSources(source1, source2));
            source1.addAll(List.of(f1, f2, f3, f4));
            source2.addAll(List.of(f3, f1, f4, f2));
            assertTrue(areSameSources(source1, source2));
            source1.clear();
            source1.addAll(List.of(f1, f2, f3, f5));
            assertFalse(areSameSources(source1, source2));
            source1.clear();
            source1.addAll(List.of(f1, f2, f3, f6_same_as_f4));
            assertTrue(areSameSources(source1, source2));
        }

        @Test
        public void testAddDotScanToPath()
        {
            testAddDotScan("Site52_041009_Study9S_Phase-I.wiff", "", true);
            testAddDotScan("Site52_041009_Study9S_Phase-I.wiff2", "", true);
            // Multi-injection wiff file
            testAddDotScan("Site52_041009_Study9S_Phase-I.wiff", "|Site52_STUDY9S_PHASEI_6ProtMix_QC_03|2", true);
            testAddDotScan("Site52_041009_Study9S_Phase-I.WIFF2", "|Site52_STUDY9S_PHASEI_6ProtMix_QC_03|2", true);

            testAddDotScan("Site52_041009_Study9S_Phase-I.RAW", "", false);
        }

        private void testAddDotScan(String fileName, String injection, boolean isSciex)
        {
            ISampleFile file = createFile("D:\\Data\\" + fileName + injection);
            assertEquals("D:\\Data\\" + fileName + (isSciex ? DOT_SCAN : "") + injection, addDotScanToPath(file));
        }

        @Test
        public void testGetDocSampleFiles()
        {
            // file1 and file1_dup are the same file. The file may have been imported into two different replicate in the document. Add it only once
            // Count 1
            ISampleFile file1 = createFile("D:\\Data\\Thermo1.raw");
            ISampleFile file1_dup = createFile("D:\\Data\\Thermo1.raw");
            // Sample file has the same file name but different path from file1 and file1_dup. This should get added
            // Count 2
            ISampleFile file1_dup_diff_path = createFile("D:\\Data\\Subfolder\\Thermo1.raw");
            // Count 3
            ISampleFile file2 = createFile("D:\\Data\\Thermo2.raw");
            // wiff1 and wiff1_dup are the same file. The file may have been imported into two different replicate in the document. Add it only once
            // A wiff.scan will also be added
            // Count 5
            ISampleFile wiff1 = createFile("D:\\Data\\Sciex1.wiff");
            ISampleFile wiff1_dup = createFile("D:\\Data\\Sciex1.wiff");
            // Sample file has the same file name but different path from wiff1 and wiff1_dup. This should get added + a wiff.scan
            // Count 7
            ISampleFile wiff1_dup_diff_path = createFile("D:\\Data\\Subfolder\\Sciex1.wiff");
            // Count 9 (wiff + wiff.scan)
            ISampleFile wiff2 = createFile("D:\\Data\\Sciex2.wiff");
            // Multi inject wiff files. Will get added only once + wiff.scan
            // Count 11
            ISampleFile multiInjectWiff1_0 = createFile("D:\\Data\\Sciex_multi_1.wiff|injection01|0");
            ISampleFile multiInjectWiff1_1 = createFile("D:\\Data\\Sciex_multi_1.wiff|injection02|1");
            ISampleFile multiInjectWiff1_2 = createFile("D:\\Data\\Sciex_multi_1.wiff|injection03|2");
            // Sample file has the same file name but different path from the multi injection files above. This should get dded (+ wiff.scan)
            // Count 13
            ISampleFile multiInjectWiff1_0_diff_path = createFile("D:\\Data\\Subfolder\\Sciex_multi_1.wiff|injection01|0");
            // file3 and file3_dup are the same file. The file may have been imported into two different replicate in the document. Add it only once
            // Count 14
            ISampleFile file3 = createFile("D:\\Data\\Thermo3.raw");
            ISampleFile file3_dup = createFile("D:\\Data\\Thermo3.raw");

            List<ValidatorSampleFile> docSampleFiles = getDocSampleFiles(List.of(file1, file1_dup, file1_dup_diff_path,
                    file2,
                    file3, file3_dup,
                    wiff1, wiff1_dup, wiff1_dup_diff_path,
                    wiff2,
                    multiInjectWiff1_0, multiInjectWiff1_1, multiInjectWiff1_2, multiInjectWiff1_0_diff_path));

            List<ISampleFile> expected = List.of(file1, file1_dup_diff_path,
                    file2,
                    file3,
                    wiff1, wiff1_dup_diff_path,
                    wiff2,
                    multiInjectWiff1_0, multiInjectWiff1_0_diff_path);

            assertEquals("Unexpected sample file count", expected.size() + 5, docSampleFiles.size());

            // Non-Sciex files
            for (int i = 0; i < 4; i++)
            {
                assertEquals("Unexpected sample file name", expected.get(i).getFileName(), docSampleFiles.get(i).getName());
                assertEquals("Unexpected sample file path", expected.get(i).getFilePath(), docSampleFiles.get(i).getFilePathImported());
            }
            // Sciex files
            int j = 4;
            for (int i = 4; i < docSampleFiles.size();)
            {
                assertEquals("Unexpected wiff file name", expected.get(j).getFileName(), docSampleFiles.get(i).getName());
                assertEquals("Unexpected wiff file path", expected.get(j).getFilePath(), docSampleFiles.get(i).getFilePathImported());
                String wiffScanName = expected.get(j).getFileName() + DOT_SCAN;
                String wiffScanPath = addDotScanToPath(expected.get(j));
                assertEquals("Unexpected wiff.scan file name", wiffScanName, docSampleFiles.get(++i).getName());
                assertEquals("Unexpected wiff.scan file path", wiffScanPath, docSampleFiles.get(i).getFilePathImported());
                i++; j++;
            }

            Set<String> duplicateNames = getDuplicateSkylineSampleFileNames(docSampleFiles);
            Set<String> expectedDuplicates = Set.of(file1.getFileName(),
                    wiff1.getFileName(), wiff1.getFileName() + DOT_SCAN,
                    multiInjectWiff1_0.getFileName(), multiInjectWiff1_0.getFileName() + DOT_SCAN);
            assertEquals(expectedDuplicates.size(), duplicateNames.size());
            assertTrue(duplicateNames.containsAll(expectedDuplicates));
        }

        private ISampleFile createFile(String path)
        {
            return new AbstractSampleFile()
            {
                @Override
                public String getFilePath()
                {
                    return path;
                }

                @Override
                public Long getInstrumentId()
                {
                    return null;
                }
            };
        }
    }

//    private void sleep()
//    {
//        try
//        {
//            Thread.sleep(1*1000);
//        }
//        catch (InterruptedException e)
//        {
//            e.printStackTrace();
//        }
//    }
}
