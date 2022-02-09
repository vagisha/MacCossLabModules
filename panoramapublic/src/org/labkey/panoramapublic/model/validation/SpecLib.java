package org.labkey.panoramapublic.model.validation;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// For table panoramapublic.speclibvalidation
public class SpecLib
{
    private int _id;
    private int _validationId;
    private String _libName;
    private String _fileName;
    private Long _size;
    private String _libType; // bibliospec, bibliospec_lite, elib, hunter, midas, nist, spectrast, chromatogram

    private List<SpecLibSourceFile> _spectrumFiles;
    private List<SpecLibSourceFile> _idFiles;

    public SpecLib() {}

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

    public String getLibName()
    {
        return _libName;
    }

    public void setLibName(String libName)
    {
        _libName = libName;
    }

    public String getFileName()
    {
        return _fileName;
    }

    public void setFileName(String fileName)
    {
        _fileName = fileName;
    }

    public Long getSize()
    {
        return _size;
    }

    public void setSize(Long size)
    {
        _size = size;
    }

    public String getLibType()
    {
        return _libType;
    }

    public void setLibType(String libType)
    {
        _libType = libType;
    }

    public boolean isMissingInSkyZip()
    {
        return _size == null;
    }

    public boolean isValid()
    {
        if (isPending())
        {
            return false;
        }

        if (isMissingInSkyZip() || isAssayLibrary() || isUnsupportedLibrary() || isIncompleteBlib())
        {
            return false;
        }
        if (isPrositLibrary())
        {
            return true; // No source files for a library based on Prosit predictions
        }
        // No peptide search files needed for EncyclopeDIA libraries so we only check for spectrum source files
        if (isEncyclopeDiaLibrary() && foundSpectrumFiles())
        {
            return true;
        }

        return foundSpectrumFiles() && (_idFiles.size() > 0 && foundIdFiles());
    }

    public String getStatusString()
    {
        if (isMissingInSkyZip())
        {
            return "Missing in Skyline ZIP";
        }
        if (isAssayLibrary())
        {
            return "BiblioSpec library not built with mass spec results";
        }
        if (isUnsupportedLibrary())
        {
            return "Unsupported library: " + getLibType();
        }
        if (isPrositLibrary())
        {
            return "VALID";
        }
        boolean missingIdFilesInBlib = _idFiles.size() == 0;
        boolean missingSpectrumFilesInBlib = _spectrumFiles.size() == 0;
        boolean missingSpectrumFiles = !foundSpectrumFiles();
        boolean missingIdFiles = !foundIdFiles();
        if (!(missingSpectrumFilesInBlib || missingSpectrumFiles || missingIdFilesInBlib || missingIdFiles))
        {
            return "VALID";
        }
        else
        {
            String status = null;
            if (missingSpectrumFiles || missingIdFiles)
            {
                status = String.format("Missing %s%s%s files", missingSpectrumFiles ? "spectrum " : "",
                        missingSpectrumFiles && missingIdFiles ? "and " : "",
                        missingIdFiles ? "peptide Id " : "");
            }
            if (missingSpectrumFilesInBlib || missingIdFilesInBlib)
            {
                status = String.format("%s%s%s%s%s",
                        status == null ? "" : status + "; ",
                        missingSpectrumFilesInBlib ? "Spectrum file " : "",
                        (missingSpectrumFilesInBlib && missingIdFilesInBlib) ? "and " : "",
                        missingIdFilesInBlib ? "Peptide ID file " : "",
                        "names not found in the .blib library");
            }
            return status;
        }
    }

    public boolean hasMissingSpectrumFiles()
    {
        return !foundSpectrumFiles();
    }

    public boolean hasMissingIdFiles()
    {
        return !foundIdFiles();
    }

    public boolean foundSpectrumFiles()
    {
        return getSpectrumFiles().stream().allMatch(f -> !f.isPending() && f.found());
    }

    public boolean foundIdFiles()
    {
        return getIdFiles().stream().allMatch(f -> !f.isPending() && f.found());
    }

    public boolean foundSourceFiles()
    {
        return foundSpectrumFiles() && foundIdFiles();
    }

    public boolean isPrositLibrary()
    {
        // For a library based on Prosit we expect only one row in the SpectrumSourceFiles table,
        // We expect idFileName to be blank and the value in the fileName column to be "Prositintensity_prosit_publication_v1".
        // The value in the fileName column may be different in Skyline 21.1. This code will be have to be updated then.
        if(isBibliospecLibrary() && getSpectrumFiles().size() == 1 && getIdFiles().size() == 0)
        {
            return "Prositintensity_prosit_publication_v1".equals(getSpectrumFiles().get(0).getName());
        }
        return false;
    }

    private boolean isBibliospecLibrary()
    {
        return "bibliospec".equals(_libType) || "bibliospec_lite".equals(_libType);
    }

    public boolean isEncyclopeDiaLibrary()
    {
        return "elib".equals(_libType);
    }

    public boolean isUnsupportedLibrary()
    {
        return !(isBibliospecLibrary() || isEncyclopeDiaLibrary());
    }

    public boolean isAssayLibrary()
    {
        // https://skyline.ms/wiki/home/software/Skyline/page.view?name=building_spectral_libraries
        return isBibliospecLibrary() && getSpectrumFiles().stream().allMatch(f -> getFileName().toLowerCase().endsWith(".csv"));
    }

    public boolean isIncompleteBlib()
    {
        return !isPrositLibrary() && _spectrumFiles.size() == 0 || _idFiles.size() == 0;
    }

    public boolean isPending()
    {
        return getSpectrumFiles().stream().anyMatch(DataFile::isPending) ||
                getIdFiles().stream().anyMatch(DataFile::isPending);
    }

    public List<String> getMissingSpectrumFileNames()
    {
        return getSpectrumFiles().stream().filter(f -> !f.found()).map(DataFile::getName).collect(Collectors.toList());
    }

    public List<String> getMissingIdFileNames()
    {
        return getIdFiles().stream().filter(f -> !f.found()).map(DataFile::getName).collect(Collectors.toList());
    }

    public List<SpecLibSourceFile> getSpectrumFiles()
    {
        return _spectrumFiles != null ? Collections.unmodifiableList(_spectrumFiles) : Collections.emptyList();
    }

    public void setSpectrumFiles(List<SpecLibSourceFile> spectrumFiles)
    {
        _spectrumFiles = spectrumFiles;
    }

    public void addSpectrumFile(SpecLibSourceFile spectrumFile)
    {
        _spectrumFiles.add(spectrumFile);
    }

    public List<SpecLibSourceFile> getIdFiles()
    {
        return _idFiles != null ? Collections.unmodifiableList(_idFiles) : Collections.emptyList();
    }

    public void setIdFiles(List<SpecLibSourceFile> idFiles)
    {
        _idFiles = idFiles;
    }

    public void addIdFile(SpecLibSourceFile idFile)
    {
        _idFiles.add(idFile);
    }

    @NotNull
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("libName", getLibName());
        jsonObject.put("fileName", getFileName());
        jsonObject.put("libType", getLibType());
        jsonObject.put("size", getSize() != null ? FileUtils.byteCountToDisplaySize(getSize()) : "0");
        jsonObject.put("valid", isValid());
        jsonObject.put("status", getStatusString());
        jsonObject.put("spectrumFiles", getSourceFilesJSON(getSpectrumFiles()));
        jsonObject.put("idFiles", getSourceFilesJSON(getIdFiles()));
        return jsonObject;
    }

    @NotNull
    private JSONArray getSourceFilesJSON(List<SpecLibSourceFile> sourceFiles)
    {
        JSONArray result = new JSONArray();
        for (SpecLibSourceFile sourceFile: sourceFiles)
        {
            result.put(sourceFile.toJSON());
        }
        return result;
    }
}
