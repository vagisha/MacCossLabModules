package org.labkey.panoramapublic.model;

public class SkylineDocSpecLib extends DbEntity
{
    private int _skylineDocValidationId; // One doc can have multiple libraries
    private Integer _speclibValidationId;  // Null if the.sky.zip does not contain the lib file
    private long _specLibId;
    private String _name;
    private long _diskSize;
    private String _libLsid;

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
}
