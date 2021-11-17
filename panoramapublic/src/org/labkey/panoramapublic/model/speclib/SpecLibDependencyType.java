package org.labkey.panoramapublic.model.speclib;

import org.labkey.api.util.SafeToRenderEnum;

import java.util.Arrays;

public enum SpecLibDependencyType implements SafeToRenderEnum
{
    STATISTICALLY_DEPENDENT("Statistically dependent results"),
    TARGETS_AND_FRAGMENTS("Used for choosing targets and fragments"),
    TARGETS_ONLY("Used for choosing targets only"),
    SUPPORTING_INFO("Used only as supporting information"),
    IRRELEVANT("Irrelevant to results");

    private final String _description;

    SpecLibDependencyType(String description)
    {
        _description = description;
    }

    public String getDescription()
    {
        return _description;
    }

    public static SpecLibDependencyType get(int ordinal)
    {
        return values().length < ordinal ? values()[ordinal] : null;
    }

    public static SpecLibDependencyType get(String name)
    {
        return Arrays.stream(values()).filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }
}
