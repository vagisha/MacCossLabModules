package org.labkey.panoramapublic.proteomexchange.validator;

import org.labkey.api.targetedms.ISampleFile;
import org.labkey.panoramapublic.model.validation.SkylineDocSampleFile;

public class ValidatorSampleFile extends SkylineDocSampleFile
{
    private ISampleFile _sampleFile;
    private String _replicateName;

    public ValidatorSampleFile() {}

    public ValidatorSampleFile(ISampleFile sampleFile, String replicateName)
    {
        setName(sampleFile.getFileName());
        setSampleFileId(sampleFile.getId());
        setFilePathImported(sampleFile.getFilePath());
        _sampleFile = sampleFile;
    }

    public ISampleFile getSampleFile()
    {
        return _sampleFile;
    }

    public String getReplicateName()
    {
        return _replicateName;
    }
}
