package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class Status extends GenericValidationStatus <SkylineDoc, SpecLib>
{
    private List<SkylineDoc> _skylineDocs;
    private List<Modification> _modifications;
    private List<SpecLib> _specLibs;

    @Override
    public @NotNull List<SkylineDoc> getSkylineDocs()
    {
        return _skylineDocs != null ? Collections.unmodifiableList(_skylineDocs) : Collections.emptyList();
    }

    public void setSkylineDocs(List<SkylineDoc> skylineDocs)
    {
        _skylineDocs = skylineDocs;
    }

    @Override
    public @NotNull List<Modification> getModifications()
    {
        return _modifications != null ? Collections.unmodifiableList(_modifications) : Collections.emptyList();
    }

    public void setModifications(List<Modification> modifications)
    {
        _modifications = modifications;
    }

    @Override
    public @NotNull List<SpecLib> getSpectralLibraries()
    {
        return _specLibs != null ? Collections.unmodifiableList(_specLibs) : Collections.emptyList();
    }

    public void setSpecLibs(List<SpecLib> specLibs)
    {
        _specLibs = specLibs;
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("validation", getValidation().toJSON());
        jsonObject.put("skylineDocuments", getSkylineDocsJSON());
        jsonObject.put("modifications", getModificationsJSON());
        jsonObject.put("spectrumLibraries", getSpectralLibrariesJSON());
        return jsonObject;
    }

    private JSONArray getSkylineDocsJSON()
    {
        JSONArray result = new JSONArray();
        getSkylineDocs().stream().map(SkylineDoc::toJSON).forEach(result::put);
        return result;
    }

    private JSONArray getModificationsJSON()
    {
        JSONArray result = new JSONArray();
        for (Modification modification: getModifications())
        {
            JSONObject json = modification.toJSON();
            JSONArray docsJson = new JSONArray();
            for (SkylineDoc doc: getSkylineDocs())
            {
                if(doc.hasModification(modification))
                {
                    JSONObject docMod = new JSONObject();
                    docMod.put("runId", doc.getRunId());
                    docMod.put("container", doc.getContainer().getRowId());
                    docMod.put("name", doc.getName());
                    docsJson.put(docMod);
                }
            }
            json.put("documents", docsJson);
            result.put(json);
        }
        return result;
    }

    private JSONArray getSpectralLibrariesJSON()
    {
        JSONArray result = new JSONArray();
        for (SpecLib specLib: getSpectralLibraries())
        {
            JSONObject json = specLib.toJSON();
            JSONArray docsJson = new JSONArray();
            for (SkylineDoc doc: getSkylineDocs())
            {
                if(doc.hasLibrary(specLib))
                {
                    JSONObject docLib = new JSONObject();
                    docLib.put("runId", doc.getRunId());
                    docLib.put("container", doc.getContainer().getRowId());
                    docLib.put("name", doc.getName());
                    docsJson.put(docLib);
                }
            }
            json.put("documents", docsJson);
            result.put(json);
        }
        return result;
    }
}
