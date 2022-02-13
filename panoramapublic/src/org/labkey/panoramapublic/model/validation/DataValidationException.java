package org.labkey.panoramapublic.model.validation;

public class DataValidationException extends Exception
{
    public DataValidationException(String message, Exception e)
    {
        super(message, e);
    }

    public DataValidationException(String message)
    {
        super(message);
    }
}
