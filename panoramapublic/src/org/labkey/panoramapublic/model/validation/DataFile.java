package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.nio.file.Path;

public abstract class DataFile
{
    private int _id;
    private String _name;
    private String _path;

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String AMBIGUOUS = "AMBIGUOUS";

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

    /**
     * @return Null if the validator hasn't yet looked for this file
     *         OR the path of the file on disk if it was found
     *         OR NOT_FOUND if the file wasn't found
     *         OR AMBIGUOUS if more than one sample file in a Skyline document have the same name
     */
    public String getPath()
    {
        return _path;
    }

    public void setPath(String path)
    {
        _path = path;
    }

    public boolean isPending()
    {
        return _path == null;
    }

    public boolean found()
    {
        // Require sample file names to be unique. Users have been know to import files that have the same name
        // but are in different directories.
        return _path != null && !NOT_FOUND.equals(_path) && !isAmbiguous();
    }

    public boolean isAmbiguous()
    {
        return AMBIGUOUS.equals(getPath());
    }

    @NotNull
    public JSONObject toJSON(Path parentPath)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", getName());
        jsonObject.put("pending", isPending());
        jsonObject.put("found", found());
        jsonObject.put("ambiguous", isAmbiguous());
        if (found())
        {
            jsonObject.put("path", getDisplayPath(parentPath));
        }
        return jsonObject;
    }

    // Returns the path relative to the given parent path.  For a sample file this should be relative to the file root
    // of the run's container (e.g. RawFiles/SISpeptides.d.zip.  For a library source file this should be relative to the
    // file root of the folder containing the experiment.
    public String getDisplayPath(Path parentPath)
    {
        if (!found())
        {
            return getPath();
        }
        if (parentPath != null)
        {
            try
            {
                Path filePath = Path.of(getPath());
                return parentPath.relativize(filePath).toString();
            }
            catch (IllegalArgumentException ignored)
            {
            }
        }
        // Return empty string if we couldn't relativize the path. Don't display the full path on the server.
        return "";
    }
}
