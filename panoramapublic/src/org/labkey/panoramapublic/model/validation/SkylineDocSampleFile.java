package org.labkey.panoramapublic.model.validation;

// For table panoramapublic.skylinedocsamplefile
public class SkylineDocSampleFile extends DataFile
{
    private int _skylineDocValidationId;
    // Example: 2017_July_10_bivalves_292.raw?centroid_ms2=true.
    // Example: D:\Data\CPTAC_Study9s\Site52_041009_Study9S_Phase-I.wiff|Site52_STUDY9S_PHASEI_6ProtMix_QC_07|6
    private String _skylineName;

    public SkylineDocSampleFile() {}

    public int getSkylineDocValidationId()
    {
        return _skylineDocValidationId;
    }

    public void setSkylineDocValidationId(int skylineDocValidationId)
    {
        _skylineDocValidationId = skylineDocValidationId;
    }

    public String getSkylineName()
    {
        return _skylineName;
    }

    public void setSkylineName(String skylineName)
    {
        _skylineName = skylineName;
    }
}
