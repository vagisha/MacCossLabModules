package org.labkey.panoramapublic.model.validation;

import org.labkey.api.data.Container;
import org.labkey.panoramapublic.model.DbEntity;
import org.labkey.panoramapublic.query.DataValidationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SkylineDoc
{
    private int _id;
    private int _validationId;
    private long _runId; // Refers to targetedms.runs.id
    private Container _container; // Container in which the Skyline document was imported
    private String _name; // Name of the .sky.zip file

    private List<SkylineDocSampleFile> _sampleFiles;
    private List<SkylineDocSpecLib> _specLibraries;
    private List<SkylineDocModification> _modifications;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

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

    public void addSampleFile(SkylineDocSampleFile sampleFile)
    {
        if (_sampleFiles == null)
        {
            _sampleFiles = new ArrayList<>();
        }
        _sampleFiles.add(sampleFile);
    }

    public void addSpecLib(SkylineDocSpecLib specLib)
    {
        if (_specLibraries == null)
        {
            _specLibraries = new ArrayList<>();
        }
        _specLibraries.add(specLib);
    }

    public boolean isValid()
    {
        return foundAllSampleFiles() && specLibrariesIncluded();
    }

    private boolean foundAllSampleFiles()
    {
        return sampleFiles().stream().allMatch(f -> !f.isPending() && f.found());
    }

    private boolean specLibrariesIncluded()
    {
        return specLibraries().stream().allMatch(SkylineDocSpecLib::isIncluded);
    }

    public boolean isPending()
    {
        return sampleFiles().stream().anyMatch(DataFile::isPending);
    }

    public List<String> getMissingSampleFileNames()
    {
        return sampleFiles().stream().filter(f -> !f.found()).map(DataFile::getName).collect(Collectors.toList());
    }

    public List<SkylineDocSampleFile> getSampleFiles()
    {
        return sampleFiles();
    }

    public List<SkylineDocSpecLib> getSpecLibraries()
    {
        return specLibraries();
    }

    private List<SkylineDocSampleFile> sampleFiles()
    {
        if(_sampleFiles == null)
        {
            _sampleFiles = DataValidationManager.getSkyDocSampleFiles(getId());
        }
        return _sampleFiles;
    }

    private List<SkylineDocSpecLib> specLibraries()
    {
//        if(_specLibraries == null)
//        {
//            _specLibraries = DataValidationManager.getSkylineDocSpecLibs(getId());
//        }
        return _specLibraries;
    }

    public void addModification(Modification mod)
    {
        if (_modifications == null)
        {
            _modifications = new ArrayList<>();
        }
        _modifications.add(new SkylineDocModification(_id, mod.getId()));
    }

    public List<SkylineDocModification> getModifications()
    {
        return _modifications == null ? Collections.emptyList() : _modifications;
    }

    public boolean hasModification(Modification modification)
    {
        return getModifications().stream().anyMatch(mod -> mod.getModificationValidationId() == modification.getId());
    }
}
