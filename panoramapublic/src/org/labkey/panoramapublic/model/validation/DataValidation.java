package org.labkey.panoramapublic.model.validation;

import org.labkey.api.data.Container;
import org.labkey.panoramapublic.model.DbEntity;

public class DataValidation extends DbEntity
{
    private Container _container;
    private int _experimentAnnotationsId;
    private int _jobId;
    private Boolean _valid;

    public DataValidation() {}

    public DataValidation (int experimentAnnotationsId, Container container, int jobId)
    {
        _experimentAnnotationsId = experimentAnnotationsId;
        _container = container;
        _jobId = jobId;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public int getExperimentAnnotationsId()
    {
        return _experimentAnnotationsId;
    }

    public void setExperimentAnnotationsId(int experimentAnnotationsId)
    {
        _experimentAnnotationsId = experimentAnnotationsId;
    }

    public int getJobId()
    {
        return _jobId;
    }

    public void setJobId(int jobId)
    {
        _jobId = jobId;
    }

    public Boolean getValid()
    {
        return _valid;
    }

    public void setValid(Boolean valid)
    {
        _valid = valid;
    }
}
