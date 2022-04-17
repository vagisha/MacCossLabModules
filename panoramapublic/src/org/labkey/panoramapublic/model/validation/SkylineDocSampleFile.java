package org.labkey.panoramapublic.model.validation;

// For table panoramapublic.skylinedocsamplefile
public class SkylineDocSampleFile extends DataFile
{
    private int _skylineDocValidationId;
    private long _sampleFileId;
    private String _filePathImported;

    public SkylineDocSampleFile() {}

    public int getSkylineDocValidationId()
    {
        return _skylineDocValidationId;
    }

    public void setSkylineDocValidationId(int skylineDocValidationId)
    {
        _skylineDocValidationId = skylineDocValidationId;
    }

    public long getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(long sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }

    /**
     * Path of the sample file imported into the Skyline document
     */
    public String getFilePathImported()
    {
        return _filePathImported;
    }

    public void setFilePathImported(String filePathImported)
    {
        _filePathImported = filePathImported;
    }
}
