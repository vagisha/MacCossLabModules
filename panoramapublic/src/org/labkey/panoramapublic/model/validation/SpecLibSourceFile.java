package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.model.validation.DataFile;

public class SpecLibSourceFile extends DataFile
{
    private int _id;
    private int _specLibValidationId;
    private String _sourceType; // SPECTRUM | SEARCH | OTHER ?? (csv, tsv)

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
}
