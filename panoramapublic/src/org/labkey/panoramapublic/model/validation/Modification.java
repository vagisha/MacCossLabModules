package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class Modification
{
    private int _id;
    private int _validationId;
    private String _skylineModName;
    private Integer _unimodId;
    private String _unimodName;
    private String _modType;

    public enum ModType {STRUCTURAL, ISOTOPIC};

    public Modification() {}

    public Modification(@NotNull String skylineModName, @Nullable Integer unimodId, @Nullable String unimodName, @NotNull ModType modType)
    {
        _skylineModName = skylineModName;
        _unimodId = unimodId;
        _unimodName = unimodName;
        _modType = modType.name();
    }

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getValidationId()
    {
        return _validationId;
    }

    public void setValidationId(int validationId)
    {
        _validationId = validationId;
    }

    public String getSkylineModName()
    {
        return _skylineModName;
    }

    public void setSkylineModName(String skylineModName)
    {
        _skylineModName = skylineModName;
    }

    public Integer getUnimodId()
    {
        return _unimodId;
    }

    public void setUnimodId(Integer unimodId)
    {
        _unimodId = unimodId;
    }

    public String getUnimodName()
    {
        return _unimodName;
    }

    public void setUnimodName(String unimodName)
    {
        _unimodName = unimodName;
    }

    public String getModType()
    {
        return _modType;
    }

    public void setModType(String modType)
    {
        _modType = modType;
    }

    public void setModType(ModType modType)
    {
        _modType = modType.name();
    }

    public boolean isValid()
    {
        return _unimodId != null;
    }

    public String toString()
    {
        if (isValid())
        {
            return "UNIMOD:" + _unimodId + ", " + _unimodName + "(" + _skylineModName + ")";
        }
        else
        {
            return _skylineModName + ": No UNIMOD ID";
        }
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("valid", isValid());
        jsonObject.put("modType", getModType());
        jsonObject.put("name", toString());
        return jsonObject;
    }
}
