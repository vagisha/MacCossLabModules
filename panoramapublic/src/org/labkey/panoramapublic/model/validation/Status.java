package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.ArrayList;
import java.util.List;

public class Status extends GenericValidationStatus <SkylineDoc, SpecLib>
{
    public Status(ExperimentAnnotations expAnnotations, int jobId)
    {
        super(expAnnotations, jobId);
    }

    @Override
    public List<SpecLib> getSpectrumLibraries()
    {
        return null; // TODO: query from database
    }
}
