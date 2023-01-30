package org.labkey.panoramapublic.catalog;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.attachments.AttachmentParent;
import org.labkey.api.attachments.AttachmentType;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

public class CatalogImageAttachmentParent implements AttachmentParent
{
    private final String _containerId;
    private final String _entityId;

    public CatalogImageAttachmentParent(ShortURLRecord shortUrl, ExperimentAnnotations experimentAnnotations)
    {
        _containerId = experimentAnnotations.getContainer().getId();
        _entityId = shortUrl.getEntityId().toString();
    }
    @Override
    public String getEntityId()
    {
        return _entityId;
    }

    @Override
    public String getContainerId()
    {
        return _containerId;
    }

    @Override
    public @NotNull AttachmentType getAttachmentType()
    {
        return CatalogImageAttachmentType.get();
    }
}
