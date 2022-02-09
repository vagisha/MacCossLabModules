package org.labkey.panoramapublic.proteomexchange.validator;

import org.jetbrains.annotations.NotNull;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.GenericValidationStatus;
import org.labkey.panoramapublic.model.validation.Modification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidatorStatus extends GenericValidationStatus<ValidatorSkylineDoc, ValidatorSpecLib>
{
    private final List<ValidatorSkylineDoc> _skylineDocs;
    private final List<Modification> _modifications;
    private final List<ValidatorSpecLib> _spectrumLibraries;

    public ValidatorStatus(DataValidation validation)
    {
        setValidation(validation);
        _skylineDocs = new ArrayList<>();
        _modifications = new ArrayList<>();
        _spectrumLibraries = new ArrayList<>();
    }

    public void addSkylineDoc(ValidatorSkylineDoc skylineDocValidation)
    {
        _skylineDocs.add(skylineDocValidation);
    }

    public void addModification(Modification modification)
    {
        _modifications.add(modification);
    }

    public void addLibrary(ValidatorSpecLib specLib)
    {
        _spectrumLibraries.add(specLib);
    }

    @Override
    public @NotNull List<ValidatorSpecLib> getSpectralLibraries()
    {
        return Collections.unmodifiableList(_spectrumLibraries);
    }

    @Override
    public @NotNull List<ValidatorSkylineDoc> getSkylineDocs()
    {
        return Collections.unmodifiableList(_skylineDocs);
    }

    @Override
    public @NotNull List<Modification> getModifications()
    {
        return Collections.unmodifiableList(_modifications);
    }
}
