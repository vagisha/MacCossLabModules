package org.labkey.cromwell;

import java.util.regex.Pattern;

public class CromwellInput
{
    private static String INPUT_WEBDAV_DIR = "url_webdav_dir_";
    private static String INPUT_WEBDAV_FILE = "url_webdav_file_";
    public static String INPUT_API_KEY = "panorama_apikey";
    public static Pattern apiKeyPattern = Pattern.compile("^apikey\\|[a-z0-9]{32}$");

    private String _name;
    private String _displayName;
    private String _value;
    private String _workflowName;

    public CromwellInput() {}

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
        _displayName = name;
        if(_displayName.startsWith(INPUT_WEBDAV_DIR))
        {
            _displayName = _displayName.substring(INPUT_WEBDAV_DIR.length());
        }
        else if(_displayName.startsWith(INPUT_WEBDAV_FILE))
        {
            _displayName = _displayName.substring(INPUT_WEBDAV_FILE.length());
        }
        _displayName = _displayName.replaceAll("_", " ");
    }

    public String getDisplayName()
    {
        return _displayName;
    }

    public String getValue()
    {
        return _value;
    }

    public void setValue(String value)
    {
        _value = value;
    }

    public String getWorkflowName()
    {
        return _workflowName;
    }

    public void setWorkflowName(String workflowName)
    {
        _workflowName = workflowName;
    }

    public boolean isWebDavDirUrl()
    {
        return _name.startsWith(INPUT_WEBDAV_DIR);
    }
    public boolean isWebdavFileUrl()
    {
        return _name.startsWith(INPUT_WEBDAV_FILE);
    }
    public boolean isApiKey()
    {
        return _name.equalsIgnoreCase(INPUT_API_KEY);
    }
}
