package org.labkey.panoramapublic.model.validation;

import org.labkey.panoramapublic.model.DbEntity;
import org.labkey.panoramapublic.model.validation.DataFile;
import org.labkey.panoramapublic.model.validation.DataValidation;
import org.labkey.panoramapublic.model.validation.SpecLibSourceFile;
import org.labkey.panoramapublic.query.DataValidationManager;

import java.util.List;
import java.util.stream.Collectors;

public class SpecLib extends DbEntity
{
    private int _validationId;
    private String _name;
    private long _diskSize;
    private String _libLsid;
    private String _libType; // BLIB, BLIB_PROSIT, BLIB_ASSAY_LIB, BLIB_NO_ID_FILES, ELIB, OTHER

    private List<SpecLibSourceFile> _spectrumFiles;
    private List<SpecLibSourceFile> _idFiles;

    public int getValidationId()
    {
        return _validationId;
    }

    public void setValidationId(int validationId)
    {
        _validationId = validationId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public long getDiskSize()
    {
        return _diskSize;
    }

    public void setDiskSize(long diskSize)
    {
        _diskSize = diskSize;
    }

    public String getLibLsid()
    {
        return _libLsid;
    }

    public void setLibLsid(String libLsid)
    {
        _libLsid = libLsid;
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
        return spectrumFiles().stream().allMatch(f -> !f.isPending() && f.found());
    }
    private boolean idFilesValid()
    {
        return idFiles().stream().allMatch(f -> !f.isPending() && f.found());
    }

    public boolean isPending()
    {
        return spectrumFiles().stream().anyMatch(DataFile::isPending) ||
                idFiles().stream().anyMatch(DataFile::isPending);
    }

    public List<String> getMissingSpectrumFileNames()
    {
        return spectrumFiles().stream().filter(f -> !f.found()).map(DataFile::getName).collect(Collectors.toList());
    }

    public List<String> getMissingIdFileNames()
    {
        return idFiles().stream().filter(f -> !f.found()).map(DataFile::getName).collect(Collectors.toList());
    }

    private List<SpecLibSourceFile> spectrumFiles()
    {
        if (_spectrumFiles == null)
        {
            init();
        }
        return _spectrumFiles;
    }

    private List<SpecLibSourceFile> idFiles()
    {
        if (_idFiles == null)
        {
            init();
        }
        return _idFiles;
    }

    private void init()
    {
//        DataValidation dv = new DataValidation(); // TODO
//        List<SpecLibSourceFile> allSourceFiles = DataValidationManager.getSpecLibSourceFiles(getId(), dv.getContainer());
//        _spectrumFiles = allSourceFiles.stream().filter(f -> f.isSpectrumFile()).collect(Collectors.toList());
//        _idFiles = allSourceFiles.stream().filter(f -> f.isIdFile()).collect(Collectors.toList());
    }
}
