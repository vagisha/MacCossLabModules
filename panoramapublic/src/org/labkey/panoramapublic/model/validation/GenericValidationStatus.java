package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class GenericValidationStatus <D extends GenericSkylineDoc, L extends SpecLib>
{
    private DataValidation _validation;

    public GenericValidationStatus() {}

    public DataValidation getValidation()
    {
        return _validation;
    }

    public void setValidation(DataValidation validation)
    {
        _validation = validation;
    }

    public abstract @NotNull List<L> getSpectralLibraries();
    public abstract @NotNull List<D> getSkylineDocs();
    public abstract @NotNull List<Modification> getModifications();
}
