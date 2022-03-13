package org.labkey.panoramapublic.query.modification;


public class ExperimentStructuralModInfo extends ExperimentModInfo
{
    private Integer _unimodId2;
    private String _unimodName2;

    public Integer getUnimodId2()
    {
        return _unimodId2;
    }

    public void setUnimodId2(Integer unimodId2)
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

    @Override
    public boolean isCombinationMod()
    {
        return _unimodId2 != null;
    }
}
