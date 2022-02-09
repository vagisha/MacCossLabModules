package org.labkey.panoramapublic.model.validation;

import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ValidatorSpecLib extends SpecLib
{
    private final List<Pair<ValidatorSkylineDoc, ISpectrumLibrary>> _documentLibraries;

    public ValidatorSpecLib()
    {
        _documentLibraries = new ArrayList<>();
        setSpectrumFiles(new ArrayList<>());
        setIdFiles(new ArrayList<>());
    }

    public void addDocumentLibrary(ValidatorSkylineDoc doc, ISpectrumLibrary specLib)
    {
        _documentLibraries.add(new Pair<>(doc, specLib));
    }

    public List<Pair<ValidatorSkylineDoc, ISpectrumLibrary>> getDocumentLibraries()
    {
        return _documentLibraries;
    }

    public void removeSkylineDoc(ValidatorSkylineDoc doc)
    {
        _documentLibraries.removeIf(pair -> pair.first.getRunId() == doc.getRunId());
    }

    public String getKey()
    {
        return String.format("%s;%s;%s;%s", getLibName(), getFileName(), getLibType(), getSize() == null ? "NOT_FOUND" : getSize().toString());
    }

    @Override
    public String toString()
    {
        return String.format("'%s' (%s) library in %d Skyline documents was built with %d raw files; %d peptide Id files. Status: %s",
                getLibName(), getFileName(), _documentLibraries.size(), getSpectrumFiles().size(), getIdFiles().size(), getStatusString());
    }
}
