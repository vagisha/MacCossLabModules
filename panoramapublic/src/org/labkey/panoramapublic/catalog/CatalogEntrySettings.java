package org.labkey.panoramapublic.catalog;

public class CatalogEntrySettings
{
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    public static final int MIN_IMG_WIDTH = 600;
    public static final int MIN_IMG_HEIGHT = 400;
    public static final int MAX_TEXT_CHARS = 500;

    private boolean _enabled;
    private long _maxFileSize;
    private int _minImgWidth;
    private int _minImgHeight;
    private int _maxTextChars;

    public CatalogEntrySettings(boolean enabled)
    {
        _enabled = enabled;
        if (enabled)
        {
            _maxFileSize = MAX_FILE_SIZE;
            _minImgWidth = MIN_IMG_WIDTH;
            _minImgHeight = MIN_IMG_HEIGHT;
            _maxTextChars = MAX_TEXT_CHARS;
        }
    }

    public CatalogEntrySettings(long maxFileSize, int minImgWidth, int minImgHeight, int maxTextChars)
    {
        _enabled = true;
        _maxFileSize = maxFileSize;
        _minImgWidth = minImgWidth;
        _minImgHeight = minImgHeight;
        _maxTextChars = maxTextChars;
    }

    public boolean isEnabled()
    {
        return _enabled;
    }

    public long getMaxFileSize()
    {
        return _maxFileSize;
    }

    public int getMinImgWidth()
    {
        return _minImgWidth;
    }

    public int getMinImgHeight()
    {
        return _minImgHeight;
    }

    public int getMaxTextChars()
    {
        return _maxTextChars;
    }
}
