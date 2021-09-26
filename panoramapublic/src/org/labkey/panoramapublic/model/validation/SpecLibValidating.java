package org.labkey.panoramapublic.model.validation;

import org.labkey.api.targetedms.ISpectrumLibrary;

import java.util.ArrayList;
import java.util.List;

public class SpecLibValidating extends SpecLib
{
    private final ISpectrumLibrary _library;

    public SpecLibValidating(ISpectrumLibrary library)
    {
        _library = library;
        _spectrumFiles = new ArrayList<>();
        _idFiles = new ArrayList<>();
    }

    public List<SpecLibSourceFile> getSpectrumFiles()
    {
        return _spectrumFiles;
    }

    public List<SpecLibSourceFile> getIdFiles()
    {
        return _idFiles;
    }
}
