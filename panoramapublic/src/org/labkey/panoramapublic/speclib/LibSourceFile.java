package org.labkey.panoramapublic.speclib;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LibSourceFile
{
    private final String spectrumSourceFile;
    private final String idFile;
    private final Set<String> scoreTypes;

    public LibSourceFile(String spectrumSourceFile, String idFile, Set<String> scoreTypes)
    {
        this.spectrumSourceFile = !StringUtils.isBlank(spectrumSourceFile) ? Paths.get(spectrumSourceFile).getFileName().toString() : null;
        this.idFile = !StringUtils.isBlank(idFile) ? Paths.get(idFile).getFileName().toString() : null;
        this.scoreTypes = scoreTypes != null && !scoreTypes.isEmpty() ? scoreTypes : null;
    }

    public boolean hasSpectrumSourceFile()
    {
        return spectrumSourceFile != null;
    }

    public String getSpectrumSourceFile()
    {
        return spectrumSourceFile;
    }

    public boolean hasIdFile()
    {
        return idFile != null;
    }

    public String getIdFile()
    {
        return idFile;
    }

    public boolean containsScoreType(String scoreType)
    {
        return scoreTypes != null && scoreTypes.contains(scoreType);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibSourceFile that = (LibSourceFile) o;
        return Objects.equals(getSpectrumSourceFile(), that.getSpectrumSourceFile())
                && Objects.equals(getIdFile(), that.getIdFile())
                && Objects.equals(scoreTypes, that.scoreTypes);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getSpectrumSourceFile(), getIdFile(), scoreTypes);
    }

    // For a MaxQuant search we need all of these files.  Only msms.txt is listed in the SpectrumSourceFiles table of .blib
    // https://skyline.ms/wiki/home/software/Skyline/page.view?name=building_spectral_libraries
    public static List<String> MAX_QUANT_ID_FILES = List.of("msms.txt", "mqpar.xml", "evidence.txt", "modifications.xml");

    public boolean isMaxQuantSearch()
    {
        return (hasIdFile() && getIdFile().endsWith("msms.txt")) || containsScoreType("MAXQUANT SCORE");
    }
}