package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.query.DataValidationManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SpecLib
{
    private int _id;
    private int _validationId;
    private String _libName;
    private String _fileName;
    private Long _diskSize;
    private String _libType; // BLIB, BLIB_PROSIT, BLIB_ASSAY_LIB, BLIB_NO_ID_FILES, ELIB, OTHER

    List<SpecLibSourceFile> _spectrumFiles;
    List<SpecLibSourceFile> _idFiles;

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

    public Long getDiskSize()
    {
        return _diskSize;
    }

    public void setDiskSize(Long diskSize)
    {
        _diskSize = diskSize;
    }

    public String getLibType()
    {
        return _libType;
    }

    public void setLibType(String libType)
    {
        _libType = libType;
    }

    public boolean isValid()
    {
        return spectrumFilesValid() && idFilesValid();
    }

    private boolean spectrumFilesValid()
    {
        return getSpectrumFiles().stream().allMatch(f -> !f.isPending() && f.found());
    }
    private boolean idFilesValid()
    {
        return getIdFiles().stream().allMatch(f -> !f.isPending() && f.found());
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
        if (_spectrumFiles == null)
        {
            _spectrumFiles = DataValidationManager.getSpectrumSourceFiles(getId());
        }
        return _spectrumFiles;
    }

    public List<SpecLibSourceFile> getIdFiles()
    {
        if (_idFiles == null)
        {
            _idFiles = DataValidationManager.getIdSourceFiles(getId());
        }
        return _idFiles;
    }

    public void setSpectrumFiles(List<SpecLibSourceFile> spectrumFiles)
    {
        _spectrumFiles = spectrumFiles;
    }

    public void setIdFiles(List<SpecLibSourceFile> idFiles)
    {
        _idFiles = idFiles;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpecLib specLib = (SpecLib) o;
        return getLibName().equals(specLib.getLibName())
                && getFileName().equals(specLib.getFileName())
                && Objects.equals(getDiskSize(), specLib.getDiskSize())
                && Objects.equals(getLibType(), specLib.getLibType())
                && Objects.equals(getSpectrumFiles(), specLib.getSpectrumFiles())
                && Objects.equals(getIdFiles(), specLib.getIdFiles());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getLibName(), getFileName(), getDiskSize(), getLibType(), getSpectrumFiles(), getIdFiles());
    }
}
