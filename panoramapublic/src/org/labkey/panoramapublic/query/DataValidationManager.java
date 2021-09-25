package org.labkey.panoramapublic.query;

import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.DataValidationStatus;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.SkylineDoc;
import org.labkey.panoramapublic.model.validation.SkylineDocModification;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;

import java.util.List;

public class DataValidationManager
{
    public static DataValidationStatus saveStatus(DataValidationStatus status, User user)
    {
        DataValidation validation = status.getValidation();
        validation = Table.insert(user, PanoramaPublicManager.getTableInfoDataValidation(), validation);
        for (SkylineDoc doc : status.getSkylineDocs())
        {
            doc.setValidationId(validation.getId());
            doc = Table.insert(user, PanoramaPublicManager.getTableInfoSkylineDocValidation(), doc);

            for (SkylineDocSampleFile sampleFile: doc.getSampleFiles())
            {
               sampleFile.setSkylineDocValidationId(doc.getId());
               Table.insert(user, PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), sampleFile);
            }
        }
        return status;
    }

    public static List<SkylineDocSampleFile> getSkyDocSampleFiles(int skyDocValidationId)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), new SimpleFilter(FieldKey.fromParts("SkylineDocValidationId"), skyDocValidationId),
                null).getArrayList(SkylineDocSampleFile.class);
    }

    public static void updateSampleFileStatus(SkylineDoc skyDoc, User user)
    {
        for (SkylineDocSampleFile sampleFile: skyDoc.getSampleFiles())
        {
            Table.update(user, PanoramaPublicManager.getTableInfoSkylineDocSampleFile(), sampleFile, sampleFile.getId());
        }
    }

    public static void addModifications(DataValidationStatus status, User user)
    {
        for (Modification mod : status.getModifications())
        {
            mod.setValidationId(status.getValidation().getId());
            Table.insert(user, PanoramaPublicManager.getTableInfoModificationValidation(), mod);
        }
    }

    public static void addSkylineDocModifications(List<SkylineDoc> skylineDocs, User user)
    {
        for (SkylineDoc doc: skylineDocs)
        {
            for (SkylineDocModification mod: doc.getModifications())
            {
                Table.insert(user, PanoramaPublicManager.getTableInfoSkylineDocModification(), mod);
            }
        }
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
