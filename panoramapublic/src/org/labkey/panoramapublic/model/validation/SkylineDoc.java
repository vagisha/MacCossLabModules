package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class SkylineDoc extends GenericSkylineDoc<SkylineDocSampleFile, SkylineDocSpecLib>
{
    private List<SkylineDocSampleFile> _sampleFiles;
    private List<SkylineDocSpecLib> _specLibraries;
    private List<SkylineDocModification> _modifications;

    public void setSampleFiles(List<SkylineDocSampleFile> sampleFiles)
    {
        _sampleFiles = sampleFiles;
    }

    @Override
    public @NotNull List<SkylineDocSampleFile> getSampleFiles()
    {
        return _sampleFiles != null ? Collections.unmodifiableList(_sampleFiles) : Collections.emptyList();
    }

    public void setSpecLibraries(List<SkylineDocSpecLib> specLibraries)
    {
        _specLibraries = specLibraries;
    }

    @Override
    public @NotNull List<SkylineDocSpecLib> getSpecLibraries()
    {
        return _specLibraries != null ? Collections.unmodifiableList(_specLibraries) : Collections.emptyList();
    }

    public void setModifications(List<SkylineDocModification> modifications)
    {
        _modifications = modifications;
    }

    @Override
    public @NotNull List<SkylineDocModification> getModifications()
    {
        return _modifications != null ? Collections.unmodifiableList(_modifications) : Collections.emptyList();
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("runId", getRunId());
        jsonObject.put("container", getContainer().getPath());
        jsonObject.put("name", getName());
        jsonObject.put("valid", foundAllSampleFiles());
        jsonObject.put("sampleFiles", getSampleFilesJSON());
        return jsonObject;
    }

    @NotNull
    private JSONArray getSampleFilesJSON()
    {
        JSONArray result = new JSONArray();
        for (SkylineDocSampleFile sampleFile: getSampleFiles())
        {
            result.put(sampleFile.toJSON());
        }
        return result;
    }
}
