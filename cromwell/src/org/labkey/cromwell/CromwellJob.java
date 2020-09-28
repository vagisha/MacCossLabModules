package org.labkey.cromwell;

import org.labkey.api.data.Container;

public class CromwellJob
{
    private int _id;
    private int _workflowId;
    private Container _container;
    private Integer _pipelineJobId;
    private String _cromwellJobId;
    private String _cromwellJobStatus;
    private String _inputs;

    public CromwellJob() {}
    public CromwellJob(int workflowId, Container container)
    {
        _workflowId = workflowId;
        _container = container;
    }

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getWorkflowId()
    {
        return _workflowId;
    }

    public void setWorkflowId(int workflowId)
    {
        _workflowId = workflowId;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public Container getContainer()
    {
        return _container;
    }

    public String getInputs()
    {
        return _inputs;
    }

    public void setInputs(String inputs)
    {
        _inputs = inputs;
    }

    public Integer getPipelineJobId()
    {
        return _pipelineJobId;
    }

    public void setPipelineJobId(Integer pipelineJobId)
    {
        _pipelineJobId = pipelineJobId;
    }

    public String getCromwellJobId()
    {
        return _cromwellJobId;
    }

    public void setCromwellJobId(String cromwellJobId)
    {
        _cromwellJobId = cromwellJobId;
    }

    public String getCromwellJobStatus()
    {
        return _cromwellJobStatus;
    }

    public void setCromwellJobStatus(String cromwellJobStatus)
    {
        _cromwellJobStatus = cromwellJobStatus;
    }
}
