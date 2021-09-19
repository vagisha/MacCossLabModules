package org.labkey.panoramapublic.query;

import org.labkey.api.data.Container;
import org.labkey.panoramapublic.model.DataValidation;
import org.labkey.panoramapublic.model.SkyDocSampleFile;
import org.labkey.panoramapublic.model.SpecLibSourceFile;

import java.util.Collections;
import java.util.List;

public class DataValidationManager
{
    public static List<SkyDocSampleFile> getSkyDocSampleFiles(int skyDocValidationId, Container container /* Container of SkylineDocValidation table */)
    {
        return Collections.emptyList();
    }

    public static List<SpecLibSourceFile> getSpecLibSourceFiles(int specLibValidationId, Container container /* Container of DataValidation table */)
    {
        return Collections.emptyList();
    }
}
