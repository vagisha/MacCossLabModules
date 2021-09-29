package org.labkey.panoramapublic.proteomexchange;

import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.SkylineDocValidating;
import org.labkey.panoramapublic.model.validation.StatusValidating;

public interface DataValidatorListener
{
    void started(StatusValidating status);
    void validatingDocument(SkylineDocValidating document);
    void sampleFilesValidated(SkylineDocValidating document, StatusValidating status);
    void validatingModifications();
    void modificationsValidated(StatusValidating status);
    void validatingSpectralLibraries();
    void spectralLibrariesValidated(StatusValidating status);
}
