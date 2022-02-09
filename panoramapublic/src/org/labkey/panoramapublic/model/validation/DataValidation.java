package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.labkey.panoramapublic.model.DbEntity;

// For table panoramapublic.datavalidation
public class DataValidation extends DbEntity
{
    private int _experimentAnnotationsId;
    private int _jobId;
    private PxStatus _status;

    public DataValidation() {}

    public DataValidation (int experimentAnnotationsId/*, Container container*/)
    {
        _experimentAnnotationsId = experimentAnnotationsId;
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

    public PxStatus getStatus()
    {
        return _status;
    }

    public void setStatus(PxStatus status)
    {
        _status = status;
    }

    public boolean isComplete()
    {
        return getStatus() != null;
    }
    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("experimentAnnotationsId", getExperimentAnnotationsId());
        jsonObject.put("status", _status != null ? _status.getLabel() : "In Progress");
        jsonObject.put("statusId", _status != null ? _status.ordinal() : -1);
        return jsonObject;
    }
}
