package org.labkey.panoramapublic.query.modification;

import org.labkey.panoramapublic.model.DbEntity;

public class ExperimentStructuralModInfo extends DbEntity
{
    private int _experimentAnnotationsId;
    private long _structuralModId;
    private boolean _combinationMod;
    private int _unimodId;
    private String _unimodName;
    private int _unimodId2;
    private String _unimodName2;

    public int getExperimentAnnotationsId()
    {
        return _experimentAnnotationsId;
    }

    public void setExperimentAnnotationsId(int experimentAnnotationsId)
    {
        _experimentAnnotationsId = experimentAnnotationsId;
    }

    public long getStructuralModId()
    {
        return _structuralModId;
    }

    public void setStructuralModId(long structuralModId)
    {
        _structuralModId = structuralModId;
    }

    public boolean isCombinationMod()
    {
        return _combinationMod;
    }

    public void setCombinationMod(boolean combinationMod)
    {
        _combinationMod = combinationMod;
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

    public int getUnimodId2()
    {
        return _unimodId2;
    }

    public void setUnimodId2(int unimodId2)
    {
        _unimodId2 = unimodId2;
    }

    public String getUnimodName2()
    {
        return _unimodName2;
    }

    public void setUnimodName2(String unimodName2)
    {
        _unimodName2 = unimodName2;
    }
}
