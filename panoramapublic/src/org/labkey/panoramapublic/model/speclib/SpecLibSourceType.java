package org.labkey.panoramapublic.model.speclib;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.SafeToRenderEnum;

public enum SpecLibSourceType implements SafeToRenderEnum
{
    LOCAL("Uploaded to project"),
    OTHER_REPOSITORY("In another repository"),
    UNAVAILABLE("Files unavailable");

    private final String _label;

    SpecLibSourceType(String label)
    {
        _label = label;
    }

    public String getLabel()
    {
        return _label;
    }

    public static @Nullable SpecLibSourceType getFromName(String name)
    {
        try
        {
            return name != null ? valueOf(name) : null;
        }
        catch(IllegalArgumentException e)
        {
            return null;
        }
    }
}
