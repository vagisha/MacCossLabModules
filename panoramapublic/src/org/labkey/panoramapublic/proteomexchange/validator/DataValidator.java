package org.labkey.panoramapublic.proteomexchange.validator;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.DbScope;
import org.labkey.api.files.FileContentService;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.util.UnexpectedException;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.model.validation.DataFile;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.Modification.ModType;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile;
import org.labkey.panoramapublic.proteomexchange.ExperimentModificationGetter;
import org.labkey.panoramapublic.query.DataValidationManager;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;
import org.labkey.panoramapublic.speclib.LibSourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public ValidatorStatus validateExperiment(User user)
    {
            ValidatorStatus status = initValidationStatus(_validation, user);
            _listener.started(status);

            TargetedMSService svc = TargetedMSService.get();
            validate(status, svc, user);

            return status;
    }

    private void validate(ValidatorStatus status, TargetedMSService svc, User user)
    {
        validateSampleFiles(status, svc, user);
        validateModifications(status, user);
        validateLibraries(status, user);
        status.getValidation().setStatus(status.getPxStatus());
        DataValidationManager.updateValidationStatus(status.getValidation(), user);
    }

    private void validateLibraries(ValidatorStatus status, User user)
    {
        _listener.validatingSpectralLibraries();
        // sleep();
        FileContentService fcs = FileContentService.get();
        for (ValidatorSpecLib specLib: status.getSpectralLibraries())
        {
            try (DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
            {
                specLib.setValidationId(status.getValidation().getId());
                DataValidationManager.saveSpectrumLibrary(specLib, user);

                List<String> errors = specLib.validate(fcs, _expAnnotations);
                if (!errors.isEmpty())
                {
                    _listener.error("There were unexpected errors in validating the library " + specLib.getFileName());
                    errors.forEach(_listener::error);
                }

                specLib.getDocumentLibraries().forEach(dl -> addSkylineDocSpecLib(specLib, user, dl));
                specLib.getSpectrumFiles().forEach(s -> DataValidationManager.saveSpecLibSourceFile(s, user));
                specLib.getIdFiles().forEach(s -> DataValidationManager.saveSpecLibSourceFile(s, user));

                transaction.commit();
            }
        }

        _listener.spectralLibrariesValidated(status);
    }

    private void addSkylineDocSpecLib(ValidatorSpecLib specLib, User user, ValidatorSpecLib.DocLib docLib)
    {
        ValidatorSkylineDocSpecLib docLibV = new ValidatorSkylineDocSpecLib(docLib.getLibrary());
        docLibV.setSpeclibValidationId(specLib.getId());
        docLibV.setSkylineDocValidationId(docLib.getDocument().getId());
        docLibV.setIncluded(specLib.getSize() != null);
        DataValidationManager.saveDocSpectrumLibrary(docLibV, user);
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
                        pxMod.isMatchInferred(),
                        pxMod.getName(),
                        pxMod.isIsotopicMod() ? ModType.Isotopic : ModType.Structural);
                if (pxMod.hasPossibleUnimods())
                {
                    mod.setPossibleUnimodMatches(pxMod.getPossibleUnimodMatches());
                }
                mod.setValidationId(status.getValidation().getId());
                DataValidationManager.saveModification(mod, user);
                status.addModification(mod);

                Set<String> skylineDocsWithMod = pxMod.getSkylineDocs();
                status.getSkylineDocs().forEach(doc ->
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
                skyDoc.validateSampleFiles(svc);

                DataValidationManager.updateSampleFileStatus(skyDoc, user);
                transaction.commit();
            }

            _listener.sampleFilesValidated(skyDoc, status);
        }
    }

    private ValidatorStatus initValidationStatus(DataValidation validation, User user)
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

            skyDoc.addSampleFiles(targetedMsSvc);
        }
    }

    private void addSpectralLibraries(ValidatorStatus status, TargetedMSService targetedMsSvc)
    {
        Map<String, ValidatorSpecLib> spectralLibraries = new HashMap<>();

        for (ValidatorSkylineDoc doc: status.getSkylineDocs())
        {
            List<? extends ISpectrumLibrary> allSpecLibs = targetedMsSvc.getLibraries(doc.getRun());
            for (ISpectrumLibrary lib: allSpecLibs)
            {
                ValidatorSpecLib sLib = getSpectralLibrary(targetedMsSvc, doc, lib);
                spectralLibraries.putIfAbsent(sLib.getKey(), sLib);
                spectralLibraries.get(sLib.getKey()).addDocumentLibrary(doc, lib);
            }
        }
        // A library can be used with more than one Skyline document.  Add it only once.
        spectralLibraries.values().forEach(status::addLibrary);
    }

    private ValidatorSpecLib getSpectralLibrary(TargetedMSService targetedMsSvc, ValidatorSkylineDoc doc, ISpectrumLibrary lib)
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
                throw UnexpectedException.wrap(e, "Error getting size of the library file '" + libPath + "'.");
            }
        }
        return sLib;
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
