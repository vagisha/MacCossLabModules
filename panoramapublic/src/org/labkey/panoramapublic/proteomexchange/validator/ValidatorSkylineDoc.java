package org.labkey.panoramapublic.proteomexchange.validator;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.panoramapublic.model.validation.GenericSkylineDoc;
import org.labkey.panoramapublic.model.validation.Modification;
import org.labkey.panoramapublic.model.validation.SkylineDocModification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidatorSkylineDoc extends GenericSkylineDoc<ValidatorSampleFile, ValidatorSkylineDocSpecLib>
{
    private List<ValidatorSampleFile> _sampleFiles;
    private List<ValidatorSkylineDocSpecLib> _specLibraries;
    private List<SkylineDocModification> _modifications;
    private ITargetedMSRun _run;

    public ValidatorSkylineDoc()
    {
        _sampleFiles = new ArrayList<>();
        _specLibraries = new ArrayList<>();
        _modifications = new ArrayList<>();
    }

    public ValidatorSkylineDoc(@NotNull ITargetedMSRun run)
    {
        this();
        _run = run;
    }

    public void addSampleFile(ValidatorSampleFile sampleFile)
    {
        _sampleFiles.add(sampleFile);
    }

    public void addSpecLib(ValidatorSkylineDocSpecLib specLib)
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
    public List<ValidatorSampleFile> getSampleFiles()
    {
        return Collections.unmodifiableList(_sampleFiles);
    }

    @Override
    public List<ValidatorSkylineDocSpecLib> getSpecLibraries()
    {
        return Collections.unmodifiableList(_specLibraries);
    }

    @Override
    public List<SkylineDocModification> getModifications()
    {
        return Collections.unmodifiableList(_modifications);
    }
}
