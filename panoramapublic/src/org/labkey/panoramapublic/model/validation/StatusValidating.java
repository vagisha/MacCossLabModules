package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatusValidating extends GenericValidationStatus <SkylineDocValidating, SpecLibValidating>
{
    private final List<SkylineDocValidating> _skylineDocs;
    private final List<Modification> _modifications;
    private final List<SpecLibValidating> _spectrumLibraries;

    public StatusValidating(ExperimentAnnotations expAnnotations, int jobId)
    {
        setValidation(new DataValidation(expAnnotations.getId(), expAnnotations.getContainer(), jobId));
        _skylineDocs = new ArrayList<>();
        _modifications = new ArrayList<>();
        _spectrumLibraries = new ArrayList<>();
    }

    public void addSkylineDoc(SkylineDocValidating skylineDocValidation)
    {
        _skylineDocs.add(skylineDocValidation);
    }

    public void addModification(Modification modification)
    {
        _modifications.add(modification);
    }

    public void addLibrary(SpecLibValidating specLib)
    {
        _spectrumLibraries.add(specLib);
    }

    @Override
    public @NotNull List<SpecLibValidating> getSpectralLibraries()
    {
        return Collections.unmodifiableList(_spectrumLibraries);
    }

    @Override
    public @NotNull List<SkylineDocValidating> getSkylineDocs()
    {
        return Collections.unmodifiableList(_skylineDocs);
    }

    @Override
    public @NotNull List<Modification> getModifications()
    {
        return Collections.unmodifiableList(_modifications);
    }
}
