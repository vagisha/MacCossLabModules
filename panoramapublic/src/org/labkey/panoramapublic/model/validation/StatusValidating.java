package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.model.ExperimentAnnotations;

public class StatusValidating extends GenericValidationStatus <SkylineDocValidating>
{
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

    public void addLibrary(SpecLib specLib)
    {
        _specLibs.add(specLib);
    }
}
