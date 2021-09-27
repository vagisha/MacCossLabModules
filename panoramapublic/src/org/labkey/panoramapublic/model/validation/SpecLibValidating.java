package org.labkey.panoramapublic.model.validation;

import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SpecLibValidating extends SpecLib
{
    private List<Pair<SkylineDocValidating, ISpectrumLibrary>> _documentLibraries;

    public SpecLibValidating()
    {
        _documentLibraries = new ArrayList<>();
        _spectrumFiles = new ArrayList<>();
        _idFiles = new ArrayList<>();
    }

    public void addDocumentLibrary(SkylineDocValidating doc, ISpectrumLibrary specLib)
    {
        _documentLibraries.add(new Pair(doc, specLib));
    }

    public List<Pair<SkylineDocValidating, ISpectrumLibrary>> getDocumentLibraries()
    {
        return _documentLibraries;
    }

    public void addSpectrumFile(SpecLibSourceFile spectrumFile)
    {
        _spectrumFiles.add(spectrumFile);
    }

    public void addIdFile(SpecLibSourceFile idFile)
    {
        _idFiles.add(idFile);
    }

    public List<SpecLibSourceFile> getSpectrumFiles()
    {
        return _spectrumFiles;
    }

    public List<SpecLibSourceFile> getIdFiles()
    {
        return _idFiles;
    }

    public String getKey()
    {
        return String.format("%s;%s;%s;%s", getLibName(), getFileName(), getLibType(), getDiskSize() == null ? "NOT_FOUND" : getDiskSize().toString());
    }

    public void removeSkylineDoc(SkylineDocValidating doc)
    {
        _documentLibraries.removeIf(pair -> pair.first.getRunId() == doc.getRunId());
    }
}
