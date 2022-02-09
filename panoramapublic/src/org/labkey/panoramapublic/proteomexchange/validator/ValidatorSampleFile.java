package org.labkey.panoramapublic.proteomexchange.validator;

import org.apache.commons.io.FilenameUtils;
import org.labkey.api.targetedms.ISampleFile;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;

public class ValidatorSampleFile extends SkylineDocSampleFile
{
    private ISampleFile _sampleFile;

    public ValidatorSampleFile() {}

    public ValidatorSampleFile(ISampleFile sampleFile)
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
