package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.query.DataValidationManager;

import java.util.List;

public class SkylineDoc extends GenericSkylineDoc<SkylineDocSampleFile, SkylineDocSpecLib>
{
    private List<SkylineDocSampleFile> _sampleFiles;
    private List<SkylineDocSpecLib> _specLibraries;

    public List<SkylineDocSampleFile> getSampleFiles()
    {
        if(_sampleFiles == null)
        {
            _sampleFiles = DataValidationManager.getSkyDocSampleFiles(getId());
        }
        return _sampleFiles;
    }

    public List<SkylineDocSpecLib> getSpecLibraries()
    {
        if(_specLibraries == null)
        {
            _specLibraries = DataValidationManager.getSkylineDocSpecLibs(getId());
        }
        return _specLibraries;
    }
}
