package org.labkey.panoramapublic.model;

import java.io.Serializable;
import java.util.List;

public class DataValidationStatus implements Serializable
{
    private DataValidation _validation;
    private List<SkylineDocValidation> _skyDocValidations;
    private List<SkylineDocSpecLib> _skyDocSpecLibs;
    private List<SpecLibValidation> _specLibValidations;

    public byte[] toByteArray()
    {
        return null;
    }

    public static DataValidationStatus fromByteArray(byte[] bytes)
    {
        return null;
    }
}
