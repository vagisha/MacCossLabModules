package org.labkey.cromwell;

public class Workflow
{
    private int _id;
    private String _name;
    private int _version;
    private String _wdl;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public int getVersion()
    {
        return _version;
    }

    public void setVersion(int version)
    {
        _version = version;
    }

    public String getWdl()
    {
        return _wdl;
    }

    public void setWdl(String wdl)
    {
        _wdl = wdl;
    }
}
