package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.speclib.SpecLibInfo;
import org.labkey.panoramapublic.model.speclib.SpectrumLibrary;

import java.util.List;

public class SpecLibInfoManager
{
    private SpecLibInfoManager() {}

    public static SpecLibInfo get(int id, Container container)
    {
        SQLFragment sql = new SQLFragment("SELECT * FROM ")
                .append(PanoramaPublicManager.getTableInfoSpecLibInfo(), "slib")
                .append(" INNER JOIN ")
                .append(PanoramaPublicManager.getTableInfoExperimentAnnotations(), "exp")
                .append(" ON exp.Id = slib.experimentAnnotationsId ")
                .append(" WHERE slib.Id = ? ").add(id)
                .append(" AND exp.Container = ?").add(container);
         return new SqlSelector(PanoramaPublicManager.getSchema(), sql).getObject(SpecLibInfo.class);
    }

    public static SpecLibInfo save(SpecLibInfo specLibInfo, User user)
    {
        return Table.insert(user, PanoramaPublicManager.getTableInfoSpecLibInfo(), specLibInfo);
    }

    public static SpecLibInfo update(SpecLibInfo specLibInfo, User user)
    {
        return Table.update(user, PanoramaPublicManager.getTableInfoSpecLibInfo(), specLibInfo, specLibInfo.getId());
    }

    public static List<SpecLibInfo> getForExperiment(int experimentAnnotationsId, Container container)
    {
        var filter = new SimpleFilter().addCondition(FieldKey.fromParts("experimentAnnotationsId"), experimentAnnotationsId);
        return new TableSelector(PanoramaPublicManager.getTableInfoSpecLibInfo(), filter, null).getArrayList(SpecLibInfo.class);
    }

    public static @Nullable SpectrumLibrary getSpectrumLibrary(long specLibId, @Nullable Container container, User user)
    {
        ISpectrumLibrary library = TargetedMSService.get().getLibrary(specLibId, null, user);
        return library != null ? new SpectrumLibrary(library) : null;
    }
}
