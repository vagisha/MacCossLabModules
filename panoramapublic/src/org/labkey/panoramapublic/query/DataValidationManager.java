package org.labkey.panoramapublic.query;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.GenericSkylineDoc;
import org.labkey.panoramapublic.model.validation.GenericValidationStatus;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.SkylineDocModification;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;
import org.labkey.panoramapublic.model.validation.SkylineDocSpecLib;
import org.labkey.panoramapublic.model.validation.SkylineDocValidating;
import org.labkey.panoramapublic.model.validation.SpecLib;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile;
import org.labkey.panoramapublic.model.validation.StatusValidating;

import java.util.Collections;
import java.util.List;

public class DataValidationManager
{
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

    public static List<SkylineDocSampleFile> getSkyDocSampleFiles(int skyDocValidationId)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), new SimpleFilter(FieldKey.fromParts("SkylineDocValidationId"), skyDocValidationId),
                null).getArrayList(SkylineDocSampleFile.class);
    }

    public static <S extends SkylineDocSampleFile, L extends SkylineDocSpecLib> void updateSampleFileStatus(GenericSkylineDoc<S, L> skyDoc, User user)
    {
        for (S sampleFile: skyDoc.getSampleFiles())
        {
            Table.update(user, PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), sampleFile, sampleFile.getId());
        }
    }

    public static <S extends GenericSkylineDoc, L extends SpecLib> void saveModifications(GenericValidationStatus<S, L> status, User user)
    {
        for (Modification mod : status.getModifications())
        {
            mod.setValidationId(status.getValidation().getId());
            Table.insert(user, PanoramaPublicManager.getTableInfoModificationValidation(), mod);
        }
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

    public static List<SpecLibSourceFile> getSpectrumSourceFiles(int id)
    {
        return Collections.emptyList();
    }

    public static List<SpecLibSourceFile> getIdSourceFiles(int id)
    {
        return Collections.emptyList();
    }

    public static List<SkylineDocSpecLib> getSkylineDocSpecLibs(int skylineDocValidationId)
    {
        return Collections.emptyList();
    }

//    public static List<SkylineDocSpecLib> getSkylineDocSpecLibs(int skyDocValidationId)
//    {
//        return new TableSelector(PanoramaPublicManager.gettable(), new SimpleFilter(FieldKey.fromParts("SkylineDocValidationId"), skyDocValidationId),
//                null).getArrayList(SkylineDocSampleFile.class);
//    }

//    public static List<SpecLibSourceFile> getSpecLibSourceFiles(int specLibValidationId)
//    {
//        return new TableSelector(PanoramaPublicManager.getTabl(), new SimpleFilter(FieldKey.fromParts("SkylineDocValidationId"), skyDocValidationId),
//                null).getArrayList(SpecLibSourceFile.class);
//    }


}
