package org.labkey.panoramapublic.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.query.modification.ExperimentStructuralModInfo;

import java.util.Collections;
import java.util.List;

public class ModificationInfoManager
{
    private ModificationInfoManager() {}

    public static ExperimentStructuralModInfo getStructuralModInfo(long modificationId, int experimentAnnotationsId)
    {
        var filter = new SimpleFilter().addCondition(FieldKey.fromParts("experimentAnnotationsId"), experimentAnnotationsId);
        filter.addCondition(FieldKey.fromParts("StructuralModId"), modificationId);

        return new TableSelector(PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), filter, null).getObject(ExperimentStructuralModInfo.class);
    }

    public static ExperimentStructuralModInfo save(ExperimentStructuralModInfo modInfo, User user)
    {
        return Table.insert(user, PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), modInfo);
    }

    public static ExperimentStructuralModInfo update(ExperimentStructuralModInfo modInfo, User user)
    {
        return Table.update(user, PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), modInfo, modInfo.getId());
    }

    public static List<ExperimentStructuralModInfo> getStructuralModsForExperiment(int experimentAnnotationsId, Container container)
    {
        var expAnnotations = ExperimentAnnotationsManager.get(experimentAnnotationsId);
        if (expAnnotations != null && expAnnotations.getContainer().equals(container))
        {
            var filter = new SimpleFilter().addCondition(FieldKey.fromParts("experimentAnnotationsId"), experimentAnnotationsId);
            return new TableSelector(PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), filter, null).getArrayList(ExperimentStructuralModInfo.class);
        }
        return Collections.emptyList();
    }
}
