package org.labkey.panoramapublic.model.validation;

import org.labkey.api.targetedms.ISampleFile;

public class SampleFileValidating extends SkylineDocSampleFile
{
    private final ISampleFile _sampleFile;

    public SampleFileValidating(ISampleFile sampleFile)
    {
        setName(sampleFile.getFileName());
        _sampleFile = sampleFile;
    }

    public ISampleFile getSampleFile()
    {
        return _sampleFile;
    }
}
