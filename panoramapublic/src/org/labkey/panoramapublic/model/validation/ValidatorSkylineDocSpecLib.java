package org.labkey.panoramapublic.model.validation;

import org.labkey.api.targetedms.ISpectrumLibrary;

public class ValidatorSkylineDocSpecLib extends SkylineDocSpecLib
{
    private ISpectrumLibrary _library;

    public ValidatorSkylineDocSpecLib() {}

    public ValidatorSkylineDocSpecLib(ISpectrumLibrary library)
    {
        _library = library;
    }

    public ISpectrumLibrary getLibrary()
    {
        return _library;
    }
}
