package org.labkey.panoramapublic.proteomexchange.validator;

import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.panoramapublic.model.validation.SkylineDocSpecLib;

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
