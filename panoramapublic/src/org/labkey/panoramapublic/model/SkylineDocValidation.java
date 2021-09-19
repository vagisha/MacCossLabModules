package org.labkey.panoramapublic.model;

import org.labkey.api.data.Container;
import org.labkey.panoramapublic.query.DataValidationManager;

import java.util.List;
import java.util.stream.Collectors;

public class SkylineDocValidation extends DbEntity
{
    private int _validationId;
    private long _runId; // Refers to targetedms.runs.id
    private Container _container; // Container in which the run was imported
    private String _name; // Name of the .sky.zip file

    private List<SkyDocSampleFile> _sampleFiles;

    public int getValidationId()
    {
        return _validationId;
    }

    public void setValidationId(int validationId)
    {
        _validationId = validationId;
    }

    public long getRunId()
    {
        return _runId;
    }

    public void setRunId(long runId)
    {
        _runId = runId;
    }

    public Container getContainer()
    {
        return _container;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public boolean isValid()
    {
        return sampleFiles().stream().allMatch(f -> !f.isPending() && f.found());
    }

    public boolean isPending()
    {
        return sampleFiles().stream().anyMatch(DataFile::isPending);
    }

    public List<String> getMissingSampleFileNames()
    {
        return sampleFiles().stream().filter(f -> !f.found()).map(DataFile::getName).collect(Collectors.toList());
    }

    private List<SkyDocSampleFile> sampleFiles()
    {
        if(_sampleFiles == null)
        {
            _sampleFiles = DataValidationManager.getSkyDocSampleFiles(getId(), getContainer());
        }
        return _sampleFiles;
    }
}
