package org.labkey.cromwell.pipeline;

public class CromwellException extends Exception
{
    public CromwellException(String message)
    {
        super(message);
    }

    public CromwellException(String message, Throwable t)
    {
        super(message, t);
    }
}
