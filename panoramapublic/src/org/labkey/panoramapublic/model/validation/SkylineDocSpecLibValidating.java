package org.labkey.panoramapublic.model.validation;

import org.labkey.api.targetedms.ISpectrumLibrary;

public class SkylineDocSpecLibValidating extends SkylineDocSpecLib
{
    private ISpectrumLibrary _library;

    public SkylineDocSpecLibValidating() {}

    public SkylineDocSpecLibValidating(ISpectrumLibrary library)
    {
        _library = library;
    }

    public ISpectrumLibrary getLibrary()
    {
        return _library;
    }
}
