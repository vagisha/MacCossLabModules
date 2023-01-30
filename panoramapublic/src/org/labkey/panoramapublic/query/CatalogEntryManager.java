package org.labkey.panoramapublic.query;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.attachments.AttachmentFile;
import org.labkey.api.attachments.AttachmentParent;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.attachments.SpringAttachmentFile;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.NumberUtilsLabKey;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.panoramapublic.CatalogImageAttachmentParent;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.CatalogEntry;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class CatalogEntryManager
{
    public static String PANORAMA_PUBLIC_CATALOG = "Panorama Public catalog";
    public static String CATALOG_ENTRY_ENABLED = "Catalog entry enabled";
    public static String CATALOG_MAX_FILE_SIZE = "Catalog entry max file size";
    public static String CATALOG_MIN_IMG_WIDTH = "Catalog image min width";
    public static String CATALOG_MIN_IMG_HEIGHT = "Catalog image min height";
    public static String CATALOG_TEXT_CHAR_LIMIT = "Catalog text character limit";

    public static CatalogEntry getEntry(int catalogEntryId)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoCatalogEntry(),null, null).getObject(catalogEntryId, CatalogEntry.class);
    }
    public static void save(CatalogEntry entry, User user)
    {
        Table.insert(user, PanoramaPublicManager.getTableInfoCatalogEntry(), entry);
    }

    public static void update(CatalogEntry entry, User user)
    {
        Table.update(user, PanoramaPublicManager.getTableInfoCatalogEntry(), entry, entry.getId());
    }

    public static void saveEntry(@NotNull CatalogEntry entry, @NotNull AttachmentFile imageFile,
                                 @NotNull ExperimentAnnotations expAnnotations, User user) throws IOException
    {
        try(DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
        {
            save(entry, user);

            saveImageAttachment(imageFile, expAnnotations, user);

            transaction.commit();
        }
    }

    public static void saveImageAttachment(@NotNull AttachmentFile imageFile, @NotNull ExperimentAnnotations expAnnotations, User user) throws IOException
    {
        AttachmentParent ap = new CatalogImageAttachmentParent(expAnnotations.getShortUrl(), expAnnotations);
        AttachmentService svc = AttachmentService.get();
        svc.deleteAttachments(ap); // If there is an existing attachment, delete it.
        svc.addAttachments(ap, Collections.singletonList(imageFile), user);
    }

    public static void updateEntry(@NotNull CatalogEntry entry, @NotNull ExperimentAnnotations expAnnotations,
                                   @Nullable AttachmentFile imageFile, User user) throws IOException
    {
        try(DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
        {
            update(entry, user);

            if (imageFile != null)
            {
                // Save the new image file if one was uploaded.
                saveImageAttachment(imageFile, expAnnotations, user);
            }

            transaction.commit();
        }
    }

    public static @Nullable CatalogEntry getEntryForShortUrl(@Nullable ShortURLRecord record)
    {
        if (record != null)
        {
            SimpleFilter filter = new SimpleFilter();
            filter.addCondition(FieldKey.fromParts("ShortUrl"), record.getEntityId());
            return new TableSelector(PanoramaPublicManager.getTableInfoCatalogEntry(), filter, null).getObject(CatalogEntry.class);
        }
        return null;
    }

    public static @Nullable CatalogEntry getEntryForExperiment(@NotNull ExperimentAnnotations expAnnotations)
    {
        return getEntryForShortUrl(expAnnotations.getShortUrl());
    }

    public static void deleteEntryForExperiment(CatalogEntry entry, @NotNull ExperimentAnnotations expAnnotations, User user)
    {
        if (entry != null)
        {
            try(DbScope.Transaction transaction = PanoramaPublicManager.getSchema().getScope().ensureTransaction())
            {
                AttachmentParent ap = new CatalogImageAttachmentParent(expAnnotations.getShortUrl(), expAnnotations);
                AttachmentService.get().deleteAttachment(ap, entry.getImageFileName(), user);

                Table.delete(PanoramaPublicManager.getTableInfoCatalogEntry(), new SimpleFilter(FieldKey.fromParts("id"), entry.getId()));

                transaction.commit();
            }
        }
    }

    public static void deleteEntryForExperiment(@NotNull ExperimentAnnotations expAnnotations, User user)
    {
        deleteEntryForExperiment(getEntryForExperiment(expAnnotations), expAnnotations, user);
    }

    public static void moveEntry(@NotNull ExperimentAnnotations previousCopy, @NotNull ExperimentAnnotations targetExperiment, User user) throws IOException
    {
        AttachmentService svc = AttachmentService.get();
        CatalogEntry entry = getEntryForShortUrl(targetExperiment.getShortUrl());
        if (entry != null)
        {
            AttachmentParent ap = new CatalogImageAttachmentParent(targetExperiment.getShortUrl(), previousCopy);
            svc.moveAttachments(targetExperiment.getContainer(), List.of(ap), user);
        }
    }

    public static List<CatalogEntry> getEntries(boolean approvedOnly)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoCatalogEntry(),
                approvedOnly ? new SimpleFilter(FieldKey.fromParts("Approved"), true) : null,
                null).getArrayList(CatalogEntry.class);
    }

    public static Integer getMaxFileSize()
    {
        return getIntProperty(CATALOG_MAX_FILE_SIZE, 5 * 1024 * 1024);
    }

    public static Integer getMinImageWidth()
    {
        return getIntProperty(CATALOG_MIN_IMG_WIDTH, 600);
    }

    public static Integer getMinImageHeight()
    {
        return getIntProperty(CATALOG_MIN_IMG_HEIGHT, 400);
    }

    public static Integer getTextCharLimit()
    {
        return getIntProperty(CATALOG_TEXT_CHAR_LIMIT, 500);
    }

    private static int getIntProperty(String propName, int defaultValue)
    {
        int ret = defaultValue;
        PropertyManager.PropertyMap map = PropertyManager.getNormalStore().getWritableProperties(PANORAMA_PUBLIC_CATALOG, false);
        if (map != null)
        {
            ret = NumberUtils.toInt(map.get(propName), defaultValue);
        }
        return ret;
    }
}
