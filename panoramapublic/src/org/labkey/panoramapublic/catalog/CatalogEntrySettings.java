package org.labkey.panoramapublic.catalog;

public class CatalogEntrySettings
{
    // Defaults
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    public static final int IMG_WIDTH = 600;
    public static final int IMG_HEIGHT = 400;
    public static final int MAX_TEXT_CHARS = 500;

    private final boolean _enabled;
    private final long _maxFileSize;
    private final int _imgWidth;
    private final int _imgHeight;
    private final int _maxTextChars;

    public static CatalogEntrySettings DISABLED = new CatalogEntrySettings(false, MAX_FILE_SIZE, IMG_WIDTH, IMG_HEIGHT, MAX_TEXT_CHARS);

    public CatalogEntrySettings(long maxFileSize, int imgWidth, int imgHeight, int maxTextChars)
    {
        this(true, maxFileSize, imgWidth, imgHeight, maxTextChars);
    }

    private CatalogEntrySettings(boolean enabled, long maxFileSize, int imgWidth, int imgHeight, int maxTextChars)
    {
        _enabled = enabled;
        _maxFileSize = maxFileSize;
        _imgWidth = imgWidth;
        _imgHeight = imgHeight;
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

    public int getImgWidth()
    {
        return _imgWidth;
    }

    public int getImgHeight()
    {
        return _imgHeight;
    }

    public int getMaxTextChars()
    {
        return _maxTextChars;
    }
}
