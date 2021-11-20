package org.labkey.panoramapublic.model.speclib;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.panoramapublic.speclib.LibraryType;

public class SpectrumLibrary implements ISpectrumLibrary
{
    private long _id;
    private long _runId;
    private String _name;
    private String _fileNameHint;
    private String _skylineLibraryId;  // lsid in <bibliospec_lite_library> element, id in others
    private String _revision;
    private String _libraryType;

    public SpectrumLibrary() {}

    public SpectrumLibrary(@NotNull ISpectrumLibrary library)
    {
        _id = library.getId();
        _runId = library.getRunId();
        _libraryType = library.getLibraryType();
        _name = library.getName();
        _fileNameHint = library.getFileNameHint();
        _skylineLibraryId = library.getSkylineLibraryId();
        _revision = library.getRevision();
    }

    @Override
    public long getId()
    {
        return _id;
    }

    public void setId(long id)
    {
        _id = id;
    }

    @Override
    public long getRunId()
    {
        return _runId;
    }

    public void setRunId(long runId)
    {
        _runId = runId;
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
    public String getRevision()
    {
        return _revision;
    }

    public void setRevision(String revision)
    {
        _revision = revision;
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

    public SpecLibKey getKey()
    {
        return new SpecLibKey(getName(), getFileNameHint(), getSkylineLibraryId(), getLibraryType());
    }

    public LibraryType getType()
    {
        return getLibraryType() != null ? LibraryType.getType(getLibraryType()) : LibraryType.unknown;
    }

    public boolean isSupported()
    {
        return getType().isSupported();
    }
}
