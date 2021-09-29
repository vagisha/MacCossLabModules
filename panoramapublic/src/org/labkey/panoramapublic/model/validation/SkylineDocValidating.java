package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.targetedms.ITargetedMSRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkylineDocValidating extends GenericSkylineDoc<SampleFileValidating, SkylineDocSpecLibValidating>
{
    private List<SampleFileValidating> _sampleFiles;
    private List<SkylineDocSpecLibValidating> _specLibraries;
    private List<SkylineDocModification> _modifications;
    private ITargetedMSRun _run;

    public SkylineDocValidating()
    {
        _sampleFiles = new ArrayList<>();
        _specLibraries = new ArrayList<>();
        _modifications = new ArrayList<>();
    }

    public SkylineDocValidating(@NotNull ITargetedMSRun run)
    {
        this();
        _run = run;
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
        _modifications.add(new SkylineDocModification(getId(), mod.getId()));
    }

    public ITargetedMSRun getRun()
    {
        return _run;
    }

    @Override
    public List<SampleFileValidating> getSampleFiles()
    {
        return Collections.unmodifiableList(_sampleFiles);
    }

    @Override
    public List<SkylineDocSpecLibValidating> getSpecLibraries()
    {
        return Collections.unmodifiableList(_specLibraries);
    }

    @Override
    public List<SkylineDocModification> getModifications()
    {
        return Collections.unmodifiableList(_modifications);
    }
}
