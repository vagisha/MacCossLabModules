package org.labkey.panoramapublic.model;

public class SkyDocSampleFile extends DataFile
{
    private int _skylineDocValidationId;
    private int _sampleFileId; // Refers to targetedms.samplefile.id

    public int getSkylineDocValidationId()
    {
        return _skylineDocValidationId;
    }

    public void setSkylineDocValidationId(int skylineDocValidationId)
    {
        _skylineDocValidationId = skylineDocValidationId;
    }

    public int getSampleFileId()
    {
        return _sampleFileId;
    }

    public void setSampleFileId(int sampleFileId)
    {
        _sampleFileId = sampleFileId;
    }
}
