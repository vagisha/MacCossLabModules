package org.labkey.panoramapublic.model.validation;

import org.apache.commons.io.FilenameUtils;
import org.labkey.api.targetedms.ISampleFile;

public class SampleFileValidating extends SkylineDocSampleFile
{
    private ISampleFile _sampleFile;

    public SampleFileValidating() {}

    public SampleFileValidating(ISampleFile sampleFile)
    {
        setName(sampleFile.getFileName());
        setSkylineName(FilenameUtils.getName(sampleFile.getFilePath()));
        _sampleFile = sampleFile;
    }

    public ISampleFile getSampleFile()
    {
        return _sampleFile;
    }
}
