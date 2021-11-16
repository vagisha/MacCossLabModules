package org.labkey.panoramapublic.model;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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

    public String getKey()
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
}
