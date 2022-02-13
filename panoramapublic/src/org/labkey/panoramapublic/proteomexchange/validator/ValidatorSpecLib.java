package org.labkey.panoramapublic.proteomexchange.validator;

import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.panoramapublic.model.validation.SpecLib;

import java.util.ArrayList;
import java.util.List;

public class ValidatorSpecLib extends SpecLib
{
    private final List<DocLib> _documentLibraries;

    public ValidatorSpecLib()
    {
        _documentLibraries = new ArrayList<>();
        setSpectrumFiles(new ArrayList<>());
        setIdFiles(new ArrayList<>());
    }

    public void addDocumentLibrary(ValidatorSkylineDoc doc, ISpectrumLibrary specLib)
    {
        _documentLibraries.add(new DocLib(doc, specLib));
    }

    public List<DocLib> getDocumentLibraries()
    {
        return _documentLibraries;
    }

    public void removeSkylineDoc(ValidatorSkylineDoc doc)
    {
        _documentLibraries.removeIf(dl -> dl.getDocument().getRunId() == doc.getRunId());
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

    public static class DocLib
    {
        private final ValidatorSkylineDoc _document;
        private final ISpectrumLibrary _library;

        public DocLib(ValidatorSkylineDoc document, ISpectrumLibrary library)
        {
            _document = document;
            _library = library;
        }

        public ValidatorSkylineDoc getDocument()
        {
            return _document;
        }

        public ISpectrumLibrary getLibrary()
        {
            return _library;
        }
    }
}
