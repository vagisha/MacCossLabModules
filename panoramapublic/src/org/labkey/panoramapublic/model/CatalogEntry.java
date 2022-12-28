package org.labkey.panoramapublic.model;

import org.labkey.api.attachments.Attachment;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.panoramapublic.CatalogImageAttachmentParent;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;

public class CatalogEntry extends DbEntity
{
    private ShortURLRecord _shortUrlEntityId;
    private String _imageFileName;
    private String _description;
    private Boolean _approved;

    public ShortURLRecord getShortUrlEntityId()
    {
        return _shortUrlEntityId;
    }

    public void setShortUrlEntityId(ShortURLRecord shortUrlEntityId)
    {
        _shortUrlEntityId = shortUrlEntityId;
    }

    public String getImageFileName()
    {
        return _imageFileName;
    }

    public void setImageFileName(String imageFileName)
    {
        _imageFileName = imageFileName;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public Boolean isApproved()
    {
        return _approved;
    }

    public void setApproved(Boolean approved)
    {
        _approved = approved;
    }

    public Attachment getAtachment()
    {
        if (_shortUrlEntityId != null && _imageFileName != null)
        {
            ExperimentAnnotations expAnnotations = ExperimentAnnotationsManager.getExperimentForShortUrl(_shortUrlEntityId);
            return expAnnotations != null
                    ? AttachmentService.get().getAttachment(new CatalogImageAttachmentParent(_shortUrlEntityId, expAnnotations), _imageFileName)
                    : null;
        }
        return null;
    }

    public boolean isPendingApproval()
    {
        return _approved == null;
    }
}
