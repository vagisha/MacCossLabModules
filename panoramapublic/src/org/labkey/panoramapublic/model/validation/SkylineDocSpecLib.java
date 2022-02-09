package org.labkey.panoramapublic.model.validation;

// For table panoramapublic.skylinedocspeclib
public class SkylineDocSpecLib
{
    private int _id;
    private int _skylineDocValidationId; // One doc can have multiple libraries
    private Integer _speclibValidationId;
    private boolean _included; // true if the library file is included in the .sky.zip

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getSkylineDocValidationId()
    {
        return _skylineDocValidationId;
    }

    public void setSkylineDocValidationId(int skylineDocValidationId)
    {
        _skylineDocValidationId = skylineDocValidationId;
    }

    public Integer getSpeclibValidationId()
    {
        return _speclibValidationId;
    }

    public void setSpeclibValidationId(Integer speclibValidationId)
    {
        _speclibValidationId = speclibValidationId;
    }

    public boolean isIncluded()
    {
        return _included;
    }

    public void setIncluded(boolean included)
    {
        _included = included;
    }
}
