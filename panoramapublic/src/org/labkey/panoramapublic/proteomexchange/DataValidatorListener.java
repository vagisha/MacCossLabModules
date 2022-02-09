package org.labkey.panoramapublic.proteomexchange;

import org.labkey.panoramapublic.model.validation.ValidatorSkylineDoc;
import org.labkey.panoramapublic.model.validation.ValidatorStatus;

public interface DataValidatorListener
{
    void started(ValidatorStatus status);
    void validatingDocument(ValidatorSkylineDoc document);
    void sampleFilesValidated(ValidatorSkylineDoc document, ValidatorStatus status);
    void validatingModifications();
    void modificationsValidated(ValidatorStatus status);
    void validatingSpectralLibraries();
    void spectralLibrariesValidated(ValidatorStatus status);
}
