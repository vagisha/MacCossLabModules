package org.labkey.panoramapublic.query.modification;

import org.labkey.panoramapublic.model.DbEntity;

public class ExperimentIsotopeModInfo extends DbEntity
{
    private int _experimentAnnotationsId;
    private long _isotopeModId;
    private int _unimodId;
    private String _unimodName;

    public int getExperimentAnnotationsId()
    {
        return _experimentAnnotationsId;
    }

    public void setExperimentAnnotationsId(int experimentAnnotationsId)
    {
        _experimentAnnotationsId = experimentAnnotationsId;
    }

    public long getIsotopeModId()
    {
        return _isotopeModId;
    }

    public void setIsotopeModId(long isotopeModId)
    {
        _isotopeModId = isotopeModId;
    }

    public int getUnimodId()
    {
        return _unimodId;
    }

    public void setUnimodId(int unimodId)
    {
        _unimodId = unimodId;
    }

    public String getUnimodName()
    {
        return _unimodName;
    }

    public void setUnimodName(String unimodName)
    {
        _unimodName = unimodName;
    }
}
