package org.labkey.panoramapublic.model.validation;

public class SkylineDocSpecLib
{
    private int _id;
    private int _skylineDocValidationId; // One doc can have multiple libraries
    private Integer _speclibValidationId;  // Null if the.sky.zip does not contain the lib file
    private String _libName;
    private String _fileName;
    private Long _diskSize;
    private String _libType; // BLIB, BLIB_PROSIT, BLIB_ASSAY_LIB, BLIB_NO_ID_FILES, ELIB, OTHER

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

    public String getLibName()
    {
        return _libName;
    }

    public void setLibName(String libName)
    {
        _libName = libName;
    }

    public String getFileName()
    {
        return _fileName;
    }

    public void setFileName(String fileName)
    {
        _fileName = fileName;
    }

    public Long getDiskSize()
    {
        return _diskSize;
    }

    public void setDiskSize(Long diskSize)
    {
        _diskSize = diskSize;
    }

    public String getLibType()
    {
        return _libType;
    }

    public void setLibType(String libType)
    {
        _libType = libType;
    }

    public boolean isIncluded()
    {
        return _diskSize != null;
    }
}
