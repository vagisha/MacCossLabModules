package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.ArrayList;
import java.util.List;

public class StatusValidating extends GenericValidationStatus <SkylineDocValidating, SpecLibValidating>
{
    private List<SpecLibValidating> _spectrumLibraries;

    public StatusValidating()
    {
        _spectrumLibraries = new ArrayList<>();
    }

    public StatusValidating(ExperimentAnnotations expAnnotations, int jobId)
    {
        super(expAnnotations, jobId);
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

    public List<SpecLibValidating> getSpectrumLibraries()
    {
        return _spectrumLibraries;
    }
}
