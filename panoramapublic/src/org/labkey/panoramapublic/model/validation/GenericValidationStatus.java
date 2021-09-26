package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.model.DbEntity;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.util.ArrayList;
import java.util.List;

public class GenericValidationStatus <D extends GenericSkylineDoc> extends DbEntity
{
    private DataValidation _validation;
    List<D> _skylineDocs;
    List<SpecLib> _specLibs;
    List<Modification> _modifications;

    public GenericValidationStatus() {}

    public GenericValidationStatus(ExperimentAnnotations expAnnotations, int jobId)
    {
        _validation = new DataValidation(expAnnotations.getId(), expAnnotations.getContainer(), jobId);
        _skylineDocs = new ArrayList<>();
        _modifications = new ArrayList<>();
    }

    public List<D> getSkylineDocs()
    {
        return _skylineDocs;
    }

    public DataValidation getValidation()
    {
        return _validation;
    }

    public void setValidation(DataValidation validation)
    {
        _validation = validation;
    }

    public List<Modification> getModifications()
    {
        return _modifications;
    }
}
