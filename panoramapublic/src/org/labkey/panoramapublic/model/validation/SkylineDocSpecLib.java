package org.labkey.panoramapublic.model.validation;

public class SkylineDocSpecLib
{
    private int _id;
    private int _skylineDocValidationId; // One doc can have multiple libraries
    private Integer _speclibValidationId;  // Null if the.sky.zip does not contain the lib file
    private long _specLibId; // Refers to targetedms.spectrumlibrary.id
    private String _name;
    private long _diskSize;
    private String _libLsid; // If this a .blib file
    private boolean _included;

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

    public long getSpecLibId()
    {
        return _specLibId;
    }

    public void setSpecLibId(long specLibId)
    {
        _specLibId = specLibId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public long getDiskSize()
    {
        return _diskSize;
    }

    public void setDiskSize(long diskSize)
    {
        _diskSize = diskSize;
    }

    public String getLibLsid()
    {
        return _libLsid;
    }

    public void setLibLsid(String libLsid)
    {
        _libLsid = libLsid;
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
