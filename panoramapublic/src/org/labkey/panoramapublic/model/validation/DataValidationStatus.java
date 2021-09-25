package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.ArrayList;
import java.util.List;

public class DataValidationStatus
{
    private DataValidation _validation;
    private List<SkylineDoc> _skylineDocs;
    private List<SpecLib> _specLibs;
    private List<Modification> _modifications;

    public DataValidationStatus(ExperimentAnnotations expAnnotations, int jobId)
    {
        _validation = new DataValidation(expAnnotations.getId(), expAnnotations.getContainer(), jobId);
        _skylineDocs = new ArrayList<>();
        _modifications = new ArrayList<>();
    }

    public DataValidation getValidation()
    {
        return _validation;
    }

    public void setValidation(DataValidation validation)
    {
        _validation = validation;
    }

    public List<SkylineDoc> getSkylineDocs()
    {
        return _skylineDocs;
    }

    public void addSkylineDoc(SkylineDoc skylineDocValidation)
    {
        _skylineDocs.add(skylineDocValidation);
    }

    public void addModification(Modification modification)
    {
        _modifications.add(modification);
    }

    public List<Modification> getModifications()
    {
        return _modifications;
    }
}
