package org.labkey.panoramapublic.model.validation;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.panoramapublic.proteomexchange.UnimodModification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Modification
{
    private int _id;
    private int _validationId;
    private String _skylineModName;
    private long _dbModId;
    private Integer _unimodId;
    private String _unimodName;
    private String _modType;
    private String _unimodMatches;

    public enum ModType {STRUCTURAL, ISOTOPIC};

    public Modification() {}

    public Modification(@NotNull String skylineModName, long dbModId, @Nullable Integer unimodId, @Nullable String unimodName, @NotNull ModType modType)
    {
        _skylineModName = skylineModName;
        _dbModId = dbModId;
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

    public long getDbModId()
    {
        return _dbModId;
    }

    public void setDbModId(long dbModId)
    {
        _dbModId = dbModId;
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
        return getNameString() + ": " + getUnimodIdStr();
    }

    public String getUnimodIdStr()
    {
        return isValid() ? "UNIMOD: " + _unimodId : "No UNIMOD Id";
    }

    public String getNameString()
    {
        return !StringUtils.isBlank(_unimodName) ? _unimodName + (!_unimodName.equals(_skylineModName) ? " [ " + _skylineModName + " ]" : "") : _skylineModName;
    }

    public String getUnimodMatches()
    {
        return _unimodMatches;
    }

    public void setUnimodMatches(String unimodMatches)
    {
        _unimodMatches = unimodMatches;
    }

    public void setPossibleUnimodMatches(List<UnimodModification> uModsList)
    {
        _unimodMatches = StringUtils.join(uModsList.stream().map(m -> m.getId() + ":::" + m.getName() + ":::" + m.getNormalizedFormula() + ":::" + m.getModSites())
                .collect(Collectors.toList()), "&&");
    }

    private List<List<String>> getPossibleUnimodMatches()
    {
        if (_unimodMatches == null)
        {
            return Collections.emptyList();
        }
        String[] matches = StringUtils.splitByWholeSeparator(_unimodMatches, "&&");
        List<List<String>> matchList = new ArrayList<>();
        for (String match: matches)
        {
            String[] parts = StringUtils.splitByWholeSeparator(match, ":::");
            if (parts.length == 4)
            {
                matchList.add(List.of(parts));
            }
        }
        return matchList;
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("unimodId", getUnimodId());
        jsonObject.put("name", getNameString());
        jsonObject.put("valid", isValid());
        jsonObject.put("modType", getModType());
        jsonObject.put("dbModId", getDbModId());
        if (getUnimodId() == null)
        {
            List<List<String>> unimodMatches = getPossibleUnimodMatches();
            if (unimodMatches.size() > 0)
            {
                jsonObject.put("possibleUnimodMatches", getPossibleUnimodMatchesJSON(unimodMatches));
            }
        }
        return jsonObject;
    }

    private JSONArray getPossibleUnimodMatchesJSON(List<List<String>> unimodMatches)
    {
        JSONArray possibleUnimods = new JSONArray();
        for (List<String> match: unimodMatches)
        {
            JSONObject possibleUnimod = new JSONObject();
            possibleUnimod.put("unimodId", match.get(0));
            possibleUnimod.put("name", match.get(1));
            possibleUnimod.put("formula", match.get(2));
            possibleUnimod.put("sites", match.get(3));
            possibleUnimods.put(possibleUnimod);
        }
        return possibleUnimods;
    }
}
