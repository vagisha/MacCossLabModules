package org.labkey.cromwell;

import java.util.regex.Pattern;

public class CromwellInput
{
    private static String INPUT_WEBDAV_DIR = "url_webdav_dir_";
    private static String INPUT_WEBDAV_OUT_DIR = "url_webdav_output_dir_";
    private static String INPUT_WEBDAV_FILE = "url_webdav_file_";
    private static String INPUT_WEBDAV_OUT_FILE = "url_webdav_output_file_";
    private static String INPUT_URL_LABKEY = "url_labkey_";
    public static String INPUT_API_KEY = "panorama_apikey";
    public static Pattern apiKeyPattern = Pattern.compile("^apikey\\|[a-z0-9]{32}$");
    private static String[] INPUT_PREFIXES = new String[] {INPUT_WEBDAV_OUT_DIR, INPUT_WEBDAV_DIR, INPUT_WEBDAV_FILE, INPUT_WEBDAV_OUT_FILE, INPUT_URL_LABKEY};

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
        for(String prefix: INPUT_PREFIXES)
        {
            if(_displayName.startsWith(prefix))
            {
                _displayName = _displayName.substring(prefix.length());
                break;
            }
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

    public boolean isWebdav()
    {
        return isWebdavFileUrl() || isWebdavDirUrl();
    }

    public boolean isWebdavOutputDir()
    {
        return _name.startsWith(INPUT_WEBDAV_OUT_DIR);
    }

    public boolean isWebdavDirUrl()
    {
        return _name.startsWith(INPUT_WEBDAV_DIR) || _name.startsWith(INPUT_WEBDAV_OUT_DIR);
    }

    public boolean isWebdavFileUrl()
    {
        return _name.startsWith(INPUT_WEBDAV_FILE);
    }

    public boolean isWebdavOutputFileUrl()
    {
        return _name.startsWith(INPUT_WEBDAV_OUT_FILE);
    }

    public boolean isApiKey()
    {
        return _name.equalsIgnoreCase(INPUT_API_KEY);
    }

    public boolean isLabkeyUrl()
    {
        return _name.startsWith(INPUT_URL_LABKEY);
    }
}
