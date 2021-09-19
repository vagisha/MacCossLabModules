package org.labkey.panoramapublic.model;

public abstract class DataFile
{
    private int _id;
    private String _name;
    private String _path;

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

    public String getPath()
    {
        return _path;
    }

    public void setPath(String path)
    {
        _path = path;
    }

    public boolean found()
    {
        return _path != null && !"NOT_FOUND".equals(_path);
    }

    public boolean isPending()
    {
        return _path == null;
    }
}
