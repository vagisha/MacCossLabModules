package org.labkey.panoramapublic.model.speclib;

import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.panoramapublic.speclib.LibraryType;

public class SpectrumLibrary implements ISpectrumLibrary
{
    private long _id;
    private String _name;
    private String _fileNameHint;
    private String _skylineLibraryId;
    private String _libraryType;
    private String _revision;

    public long getId()
    {
        return _id;
    }

    public void setId(long id)
    {
        _id = id;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    @Override
    public String getFileNameHint()
    {
        return _fileNameHint;
    }

    public void setFileNameHint(String fileNameHint)
    {
        _fileNameHint = fileNameHint;
    }

    @Override
    public String getSkylineLibraryId()
    {
        return _skylineLibraryId;
    }

    public void setSkylineLibraryId(String skylineLibraryId)
    {
        _skylineLibraryId = skylineLibraryId;
    }

    @Override
    public String getLibraryType()
    {
        return _libraryType;
    }

    public void setLibraryType(String libraryType)
    {
        _libraryType = libraryType;
    }

    @Override
    public String getRevision()
    {
        return _revision;
    }

    public void setRevision(String revision)
    {
        _revision = revision;
    }

    public SpecLibKey getKey()
    {
        return new SpecLibKey(getName(), getFileNameHint(), getSkylineLibraryId(), getLibraryType());
    }

    public LibraryType getType()
    {
        return _libraryType != null ? LibraryType.getType(getLibraryType()) : LibraryType.unknown;
    }
}
