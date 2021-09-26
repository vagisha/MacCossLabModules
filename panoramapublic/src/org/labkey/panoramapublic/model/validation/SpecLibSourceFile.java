package org.labkey.panoramapublic.model.validation;

import java.util.Objects;

public class SpecLibSourceFile extends DataFile
{
    private int _id;
    private int _specLibValidationId;
    private String _sourceType; // SPECTRUM_SOURCE | ID_SOURCE | OTHER ?? (csv, tsv)

    public static final String SPECTRUM_SOURCE = "SPECTRUM_SOURCE";
    public static final String ID_SOURCE = "ID_SOURCE";

    public SpecLibSourceFile() {}

    public SpecLibSourceFile(String name, String sourceType)
    {
        setName(name);
        _sourceType = sourceType;
    }

    @Override
    public int getId()
    {
        return _id;
    }

    @Override
    public void setId(int id)
    {
        _id = id;
    }

    public int getSpecLibValidationId()
    {
        return _specLibValidationId;
    }

    public void setSpecLibValidationId(int specLibValidationId)
    {
        _specLibValidationId = specLibValidationId;
    }

    public String getSourceType()
    {
        return _sourceType;
    }

    public void setSourceType(String sourceType)
    {
        _sourceType = sourceType;
    }

    public boolean isSpectrumFile()
    {
        return "SPECTRUM_SOURCE".equals(_sourceType);
    }

    public boolean isIdFile()
    {
        return "ID_SOURCE".equals(_sourceType);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpecLibSourceFile that = (SpecLibSourceFile) o;
        return getSourceType().equals(that.getSourceType()) && getName().equals(that.getName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getSourceType(), getName());
    }
}
