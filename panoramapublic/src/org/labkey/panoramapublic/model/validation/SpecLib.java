package org.labkey.panoramapublic.model.validation;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.panoramapublic.model.speclib.SpecLibInfo;

import java.util.Collections;
import java.util.List;

// For table panoramapublic.speclibvalidation
public class SpecLib extends SpecLibValidation<SkylineDocSpecLib>
{
    private List<SkylineDocSpecLib> _docsWithLibrary;
    private SpecLibInfo _specLibInfo;

    public SpecLib() {}

    @Override
    public @NotNull List<SkylineDocSpecLib> getDocsWithLibrary()
    {
        return _docsWithLibrary != null ? Collections.unmodifiableList(_docsWithLibrary) : Collections.emptyList();
    }

    public void setDocsWithLibrary(@NotNull List<SkylineDocSpecLib> docsWithLibrary)
    {
        _docsWithLibrary = docsWithLibrary;
    }

    public void setSpecLibInfo(SpecLibInfo specLibInfo)
    {
        _specLibInfo = specLibInfo;
    }

    @Override
    public SpecLibInfo getSpecLibInfo()
    {
        return _specLibInfo;
    }

    @NotNull
    public JSONObject toJSON(Container expContainer)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("libName", getLibName());
        jsonObject.put("fileName", getFileName());
        jsonObject.put("libType", getLibType());
        jsonObject.put("size", getSize() != null ? FileUtils.byteCountToDisplaySize(getSize()) : "-");
        jsonObject.put("valid", isValid());
        jsonObject.put("status", getStatusString());
        if (getSpecLibInfo() != null)
        {
            jsonObject.put("specLibInfo", getSpecLibInfo().getInfo());
            jsonObject.put("specLibInfoId", getSpecLibInfo().getId());
        }
        jsonObject.put("spectrumFiles", getSourceFilesJSON(getSpectrumFiles(), expContainer));
        jsonObject.put("idFiles", getSourceFilesJSON(getIdFiles(), expContainer));
        return jsonObject;
    }

    @NotNull
    private JSONArray getSourceFilesJSON(List<SpecLibSourceFile> sourceFiles, Container expContainer)
    {
        JSONArray result = new JSONArray();
        for (SpecLibSourceFile sourceFile: sourceFiles)
        {
            result.put(sourceFile.toJSON(expContainer));
        }
        return result;
    }
}
