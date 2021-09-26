package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.targetedms.ITargetedMSRun;

import java.util.ArrayList;
import java.util.List;

public class SkylineDocValidating extends GenericSkylineDoc<SampleFileValidating, SkylineDocSpecLibValidating>
{
    private List<SampleFileValidating> _sampleFiles;
    private List<SkylineDocSpecLibValidating> _specLibraries;
    private final ITargetedMSRun _run;

    public SkylineDocValidating(@NotNull ITargetedMSRun run)
    {
        _run = run;
        _sampleFiles = new ArrayList<>();
        _specLibraries = new ArrayList<>();
    }

    public void addSampleFile(SampleFileValidating sampleFile)
    {
        _sampleFiles.add(sampleFile);
    }

    public void addSpecLib(SkylineDocSpecLibValidating specLib)
    {
        _specLibraries.add(specLib);
    }

    public void addModification(Modification mod)
    {
        if (_modifications == null)
        {
            _modifications = new ArrayList<>();
        }
        _modifications.add(new SkylineDocModification(getId(), mod.getId()));
    }

    public ITargetedMSRun getRun()
    {
        return _run;
    }

    @Override
    public List<SampleFileValidating> getSampleFiles()
    {
        return _sampleFiles;
    }

    @Override
    public List<SkylineDocSpecLibValidating> getSpecLibraries()
    {
        return _specLibraries;
    }
}
