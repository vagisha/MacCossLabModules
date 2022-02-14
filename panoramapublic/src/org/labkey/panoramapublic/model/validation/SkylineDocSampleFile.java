package org.labkey.panoramapublic.model.validation;

import org.apache.commons.io.FilenameUtils;

// For table panoramapublic.skylinedocsamplefile
public class SkylineDocSampleFile extends DataFile
{
    private int _skylineDocValidationId;
    private String _filePathImported; // path of the file imported into the Skyline document

    public SkylineDocSampleFile() {}

    public int getSkylineDocValidationId()
    {
        return _skylineDocValidationId;
    }

    public void setSkylineDocValidationId(int skylineDocValidationId)
    {
        _skylineDocValidationId = skylineDocValidationId;
    }

    public String getFilePathImported()
    {
        return _filePathImported;
    }

    public void setFilePathImported(String filePathImported)
    {
        _filePathImported = filePathImported;
    }
}
