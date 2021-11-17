package org.labkey.panoramapublic.model.speclib;

import org.labkey.api.util.SafeToRenderEnum;

import java.util.Arrays;

public enum SpecLibSourceType implements SafeToRenderEnum
{
    LOCAL("Uploaded to project"),
    OTHER_REPOSITORY("In another repository"),
    UNAVAILABLE("Files unavailable");

    private final String _description;

    SpecLibSourceType(String description)
    {
        _description = description;
    }

    public String getDescription()
    {
        return _description;
    }

    public static SpecLibSourceType get(int ordinal)
    {
        return values().length < ordinal ? values()[ordinal] : null;
    }

    public static SpecLibSourceType get(String name)
    {
        return Arrays.stream(values()).filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }
}
