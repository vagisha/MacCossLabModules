package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.GenericSkylineDoc;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.SkylineDoc;
import org.labkey.panoramapublic.model.validation.SkylineDocModification;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;
import org.labkey.panoramapublic.model.validation.SkylineDocSpecLib;
import org.labkey.panoramapublic.model.validation.SkylineDocValidating;
import org.labkey.panoramapublic.model.validation.SpecLib;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile;
import org.labkey.panoramapublic.model.validation.SpecLibValidating;
import org.labkey.panoramapublic.model.validation.Status;
import org.labkey.panoramapublic.model.validation.StatusValidating;

import java.util.List;

public class DataValidationManager
{
    public static @Nullable Status getStatusForJobId(int jobId, Container container)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("JobId"), jobId);
        filter.addCondition(FieldKey.fromParts("Container"), container);
        DataValidation validation = new TableSelector(PanoramaPublicManager.getTableInfoDataValidation(), filter, null).getObject(DataValidation.class);
        if (validation != null)
        {
            Status status = new Status();
            status.setValidation(validation);
            SimpleFilter validationIdFilter = new SimpleFilter(FieldKey.fromParts("ValidationId"), validation.getId());
            status.setSkylineDocs(getSkylineDocs(validationIdFilter));
            status.setModifications(getModifications(validationIdFilter));
            status.setSpecLibs(getSpectrumLibraries(validationIdFilter));
            return status;
        }
        return null;
    }

    private static List<SkylineDoc> getSkylineDocs(SimpleFilter filter)
    {
        List<SkylineDoc> docs = new TableSelector(PanoramaPublicManager.getTableInfoSkylineDocValidation(), filter, null).getArrayList(SkylineDoc.class);
        for (SkylineDoc doc: docs)
        {
            SimpleFilter skyDocFilter = new SimpleFilter(FieldKey.fromParts("SkylineDocValidationId"), doc.getId());
            doc.setSampleFiles(getSkylineDocSampleFiles(skyDocFilter));
            doc.setModifications(getSkylineDocModifications(skyDocFilter));
            doc.setSpecLibraries(getSkylineDocSpecLibs(skyDocFilter));
        }
        return docs;
    }

    private static List<SkylineDocSampleFile> getSkylineDocSampleFiles(SimpleFilter filter)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), filter, null).getArrayList(SkylineDocSampleFile.class);
    }

    private static List<SkylineDocModification> getSkylineDocModifications(SimpleFilter filter)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoSkylineDocModification(), filter, null).getArrayList(SkylineDocModification.class);
    }

    private static List<SkylineDocSpecLib> getSkylineDocSpecLibs(SimpleFilter filter)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoSkylineDocSpecLib(), filter, null).getArrayList(SkylineDocSpecLib.class);
    }

    private static List<Modification> getModifications(SimpleFilter filter)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoModificationValidation(), filter, null).getArrayList(Modification.class);
    }

    private static List<SpecLib> getSpectrumLibraries(SimpleFilter filter)
    {
        List<SpecLib> specLibs = new TableSelector(PanoramaPublicManager.getTableInfoSpecLibValidation(), filter, null).getArrayList(SpecLib.class);
        for (SpecLib lib: specLibs)
        {
            SimpleFilter specLibIdFilter = new SimpleFilter(FieldKey.fromParts("SpecLibValidationId"), lib.getId());
            lib.setSpectrumFiles(getSpectrumSourceFiles(specLibIdFilter));
            lib.setIdFiles(getIdSourceFiles(specLibIdFilter));
        }
        return specLibs;
    }

    private static List<SpecLibSourceFile> getSpectrumSourceFiles(SimpleFilter filter)
    {
        filter.addCondition(FieldKey.fromParts("SourceType"), SpecLibSourceFile.SPECTRUM_SOURCE);
        return new TableSelector(PanoramaPublicManager.getTableInfoSpecLibSourceFile(), filter, null).getArrayList(SpecLibSourceFile.class);
    }

    private static List<SpecLibSourceFile> getIdSourceFiles(SimpleFilter filter)
    {
        filter.addCondition(FieldKey.fromParts("SourceType"), SpecLibSourceFile.ID_SOURCE);
        return new TableSelector(PanoramaPublicManager.getTableInfoSpecLibSourceFile(), filter, null).getArrayList(SpecLibSourceFile.class);
    }

    public static void saveStatus(StatusValidating status, User user)
    {
        DataValidation validation = status.getValidation();
        validation = Table.insert(user, PanoramaPublicManager.getTableInfoDataValidation(), validation);
        for (SkylineDocValidating doc : status.getSkylineDocs())
        {
            doc.setValidationId(validation.getId());
            doc = Table.insert(user, PanoramaPublicManager.getTableInfoSkylineDocValidation(), doc);

            for (SkylineDocSampleFile sampleFile: doc.getSampleFiles())
            {
               sampleFile.setSkylineDocValidationId(doc.getId());
               Table.insert(user, PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), sampleFile);
            }
        }
    }

    public static <S extends SkylineDocSampleFile, L extends SkylineDocSpecLib> void updateSampleFileStatus(GenericSkylineDoc<S, L> skyDoc, User user)
    {
        for (S sampleFile: skyDoc.getSampleFiles())
        {
            Table.update(user, PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), sampleFile, sampleFile.getId());
        }
    }

    public static void saveModification(Modification modification, User user)
    {
        Table.insert(user, PanoramaPublicManager.getTableInfoModificationValidation(), modification);
    }

    public static void saveSkylineDocModifications(List<SkylineDocValidating> skylineDocs, User user)
    {
        for (SkylineDocValidating doc: skylineDocs)
        {
            for (SkylineDocModification mod: doc.getModifications())
            {
                Table.insert(user, PanoramaPublicManager.getTableInfoSkylineDocModification(), mod);
            }
        }
    }

    public static void saveSpectrumLibrary(SpecLibValidating specLib, User user)
    {
        Table.insert(user, PanoramaPublicManager.getTableInfoSpecLibValidation(), specLib);
    }

    public static void saveSpecLibSourceFile(SpecLibSourceFile sourceFile, User user)
    {
        Table.insert(user, PanoramaPublicManager.getTableInfoSpecLibSourceFile(), sourceFile);
    }

    public static void saveDocSpectrumLibrary(SkylineDocSpecLib docSpecLib, User user)
    {
        Table.insert(user, PanoramaPublicManager.getTableInfoSkylineDocSpecLib(), docSpecLib);
    }
}
