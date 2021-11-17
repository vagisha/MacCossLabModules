package org.labkey.panoramapublic.model.speclib;

import org.jetbrains.annotations.NotNull;
import org.labkey.panoramapublic.speclib.LibraryType;

public class SpecLibKey
{
    private final String _name;
    private final String _fileNameHint;
    private final String _skylineLibraryId;
    private final String _libraryType;

    private static final String SEP = "__&&__";

    public SpecLibKey(@NotNull String name, @NotNull String fileNameHint, String skylineLibraryId, String libraryType)
    {
        _name = name;
        _fileNameHint = fileNameHint;
        _skylineLibraryId = skylineLibraryId;
        _libraryType = libraryType;
    }

    public String getKeyString()
    {
        return String.format("%s%s%s%s%s", _name, SEP, _libraryType,
                (_fileNameHint != null ? SEP + _fileNameHint : ""),
                (_skylineLibraryId != null ? SEP + _skylineLibraryId : ""));

    }

    public static SpecLibKey from(String key)
    {
        String[] parts = key.split(SEP);
        if (parts.length > 1)
        {
            String name = parts[0];
            String libraryType = parts[1];
            String fileNameHint = parts.length > 2 ? parts[2] : null;
            String skylineLibId = parts.length > 3 ? parts[3] : null;
            return new SpecLibKey(name, libraryType, fileNameHint, skylineLibId);
        }
        return null;
    }

    public String getName()
    {
        return _name;
    }

    public String getFileNameHint()
    {
        return _fileNameHint;
    }

    public String getSkylineLibraryId()
    {
        return _skylineLibraryId;
    }

    public String getLibraryType()
    {
        return _libraryType;
    }

    public LibraryType getType()
    {
        return LibraryType.getType(_libraryType);
    }
}
