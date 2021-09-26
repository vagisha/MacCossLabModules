package org.labkey.panoramapublic.model.validation;

import java.io.IOException;

public class DataValidationException extends Exception
{

    public DataValidationException(String message, Exception e)
    {
        super(message, e);
    }
}
