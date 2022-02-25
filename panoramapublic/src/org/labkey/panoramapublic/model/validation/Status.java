package org.labkey.panoramapublic.model.validation;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.labkey.panoramapublic.query.DataValidationManager.*;

public class Status extends GenericValidationStatus <SkylineDoc, SpecLib>
{
    private List<SkylineDoc> _skylineDocs;
    private List<Modification> _modifications;
    private List<SpecLib> _specLibs;
    private MissingMetadata _missingMetadata;

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

    public boolean foundAllSampleFiles()
    {
        return getSkylineDocs().stream().allMatch(SkylineDocValidation::foundAllSampleFiles);
    }

    public void setModifications(List<Modification> modifications)
    {
        _modifications = modifications;
    }

    public boolean allModificationsValid()
    {
        return getModifications().stream().allMatch(Modification::isValid);
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

    public boolean specLibsComplete()
    {
        return getSpectralLibraries().stream().allMatch(SpecLib::isValid);
    }

    public MissingMetadata getMissingMetadata()
    {
        return _missingMetadata;
    }

    public void setMissingMetadata(MissingMetadata missingMetadata)
    {
        _missingMetadata = missingMetadata;
    }

    public boolean hasMissingMetadata()
    {
        return _missingMetadata != null && _missingMetadata.count() > 0;
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        JSONObject validationJson = getValidation().toJSON();
        if (hasMissingMetadata())
        {
            JSONArray missingFields = new JSONArray();
            missingFields.put(getMissingMetadata().getMessages());
            validationJson.put("missingMetadata", getMissingMetadata().getMessages());
        }
        validationJson.put("modificationsValid", allModificationsValid());
        validationJson.put("sampleFilesValid", foundAllSampleFiles());
        validationJson.put("specLibsComplete", specLibsComplete());

        jsonObject.put("validation", validationJson);
        jsonObject.put("skylineDocuments", getSkylineDocsJSON());
        Map<Integer, SkylineDoc> docMap = getSkylineDocs().stream().collect(Collectors.toMap(SkylineDocValidation::getId, Function.identity()));
        jsonObject.put("modifications", getModificationsJSON(docMap));
        jsonObject.put("spectrumLibraries", getSpectralLibrariesJSON(docMap));
        return jsonObject;
    }

    private JSONArray getSkylineDocsJSON()
    {
        JSONArray result = new JSONArray();
        getSkylineDocs().stream().map(SkylineDoc::toJSON).forEach(result::put);
        return result;
    }

    private JSONArray getModificationsJSON(Map<Integer, SkylineDoc> docMap)
    {
        JSONArray result = new JSONArray();
        for (Modification modification: getModifications())
        {
            JSONObject json = modification.toJSON();
            JSONArray docsJson = new JSONArray();
            for (SkylineDocModification skyDocMod: modification.getDocsWithModification())
            {
                SkylineDoc doc = docMap.get(skyDocMod.getSkylineDocValidationId());
                if (doc != null)
                {
                    docsJson.put(getMemberDocJSON(doc));
                }
            }
            json.put("documents", docsJson);
            result.put(json);
        }
        return result;
    }

    @NotNull
    private JSONObject getMemberDocJSON(SkylineDoc doc)
    {
        JSONObject docMod = new JSONObject();
        docMod.put("runId", doc.getRunId());
        docMod.put("container", doc.getContainer().getPath());
        docMod.put("name", doc.getName());
        return docMod;
    }

    private JSONArray getSpectralLibrariesJSON(Map<Integer, SkylineDoc> docMap)
    {
        JSONArray result = new JSONArray();
        for (SpecLib specLib: getSpectralLibraries())
        {
            JSONObject json = specLib.toJSON();
            JSONArray docsJson = new JSONArray();
            for (SkylineDocSpecLib skyDocLib: specLib.getDocsWithLibrary())
            {
                SkylineDoc doc = docMap.get(skyDocLib.getSkylineDocValidationId());
                if (doc != null)
                {
                    docsJson.put(getMemberDocJSON(doc));
                }
            }
            json.put("documents", docsJson);
            result.put(json);
        }
        return result;
    }

    public JSONArray toProgressSummaryJSON()
    {
        JSONArray json = new JSONArray();
        int documentCount = getSkylineDocs().size();
        long validatedCount = getSkylineDocs().stream().filter(doc -> !doc.isPending()).count();
        if (validatedCount > 0)
        {
            boolean missingFilesFound = getSkylineDocs().stream().anyMatch(doc -> !doc.isPending() && !doc.isValid());
            json.put("Validating sample files for Skyline documents: " + validatedCount + "/" + documentCount + " completed."
            + (missingFilesFound ? " Found missing sample files." : ""));
        }
        if (getModifications().size() > 0)
        {
            boolean invalidModsFound = getModifications().stream().anyMatch(mod -> !mod.isValid());
            json.put("Modifications validation complete." + (invalidModsFound ? " Found invalid modifications." : ""));
        }
        int specLibCount = getSpectralLibraries().size();
        validatedCount = getSpectralLibraries().stream().filter(lib -> !lib.isPending()).count();
        if (validatedCount > 0)
        {
            boolean missingFilesFound = getSpectralLibraries().stream().anyMatch(lib -> !lib.isPending() && !lib.isValid());
            json.put("Validating spectral libraries: " + validatedCount + "/" + specLibCount + " completed."
                    + (missingFilesFound ? " Found invalid libraries." : ""));
        }
        return json;
    }
}
