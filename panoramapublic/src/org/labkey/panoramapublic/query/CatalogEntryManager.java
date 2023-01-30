package org.labkey.panoramapublic.query;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.attachments.AttachmentFile;
import org.labkey.api.attachments.AttachmentParent;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.panoramapublic.catalog.CatalogEntrySettings;
import org.labkey.panoramapublic.catalog.CatalogImageAttachmentParent;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.CatalogEntry;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

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

    public static CatalogEntrySettings getCatalogEntrySettings()
    {
        PropertyManager.PropertyMap map = PropertyManager.getNormalStore().getWritableProperties(PANORAMA_PUBLIC_CATALOG, false);
        if (map != null)
        {
            if (Boolean.parseBoolean(map.get(CATALOG_ENTRY_ENABLED)))
            {
                return new CatalogEntrySettings(
                        NumberUtils.toLong(map.get(CATALOG_MAX_FILE_SIZE), CatalogEntrySettings.MAX_FILE_SIZE),
                        NumberUtils.toInt(map.get(CATALOG_MIN_IMG_WIDTH), CatalogEntrySettings.MIN_IMG_WIDTH),
                        NumberUtils.toInt(map.get(CATALOG_MIN_IMG_HEIGHT), CatalogEntrySettings.MIN_IMG_HEIGHT),
                        NumberUtils.toInt(map.get(CATALOG_TEXT_CHAR_LIMIT), CatalogEntrySettings.MAX_TEXT_CHARS));
            }
        }
        return new CatalogEntrySettings(false);
    }

    public static void saveCatalogEntrySettings(boolean enabled, Long maxFileSize, Integer minImgWidth, Integer minImgHeight, Integer maxTextChars)
    {
        PropertyManager.PropertyMap map = PropertyManager.getNormalStore().getWritableProperties(CatalogEntryManager.PANORAMA_PUBLIC_CATALOG, true);
        map.put(CatalogEntryManager.CATALOG_ENTRY_ENABLED, Boolean.toString(enabled));
        map.put(CatalogEntryManager.CATALOG_MAX_FILE_SIZE, maxFileSize != null ? String.valueOf(maxFileSize) : null);
        map.put(CatalogEntryManager.CATALOG_MIN_IMG_WIDTH, minImgWidth !=null ? String.valueOf(minImgWidth) : null);
        map.put(CatalogEntryManager.CATALOG_MIN_IMG_HEIGHT, minImgHeight != null ? String.valueOf(minImgHeight) : null);
        map.put(CatalogEntryManager.CATALOG_TEXT_CHAR_LIMIT, maxTextChars != null ? String.valueOf(maxTextChars) : null);
        map.save();
    }

//    public static boolean catalogEntriesEnabled()
//    {
//        return Boolean.parseBoolean(getPropertyValue(CATALOG_ENTRY_ENABLED));
//    }
//
//    public static long getMaxFileSize()
//    {
//        return getLongProperty(CATALOG_MAX_FILE_SIZE, 5 * 1024 * 1024);
//    }
//
//    public static int getMinImageWidth()
//    {
//        return getIntProperty(CATALOG_MIN_IMG_WIDTH, 600);
//    }
//
//    public static int getMinImageHeight()
//    {
//        return getIntProperty(CATALOG_MIN_IMG_HEIGHT, 400);
//    }
//
//    public static int getTextCharLimit()
//    {
//        return getIntProperty(CATALOG_TEXT_CHAR_LIMIT, 500);
//    }

    private static int getIntProperty(String propName, int defaultValue)
    {
        return NumberUtils.toInt(getPropertyValue(propName), defaultValue);
    }

    private static long getLongProperty(String propName, long defaultValue)
    {
        return NumberUtils.toLong(getPropertyValue(propName), defaultValue);
    }

    private static String getPropertyValue(String propName)
    {
        PropertyManager.PropertyMap map = PropertyManager.getNormalStore().getWritableProperties(PANORAMA_PUBLIC_CATALOG, false);
        return map != null ? map.get(propName) : null;
    }
}
