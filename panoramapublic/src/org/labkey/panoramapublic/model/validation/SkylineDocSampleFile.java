package org.labkey.panoramapublic.model.validation;

import org.labkey.api.targetedms.ISampleFile;

public class SkylineDocSampleFile extends DataFile
{
    private int _skylineDocValidationId;
    private int _sampleFileId; // Refers to targetedms.samplefile.id
    private ISampleFile _sampleFile;

    public SkylineDocSampleFile() {}

    public SkylineDocSampleFile(ISampleFile sampleFile)
    {
        setName(sampleFile.getFileName());
        _sampleFile = sampleFile;
    }

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

    public ISampleFile getSampleFile()
    {
        return _sampleFile;
    }
}
