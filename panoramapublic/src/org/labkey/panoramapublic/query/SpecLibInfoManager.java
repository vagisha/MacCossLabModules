package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Filter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.model.speclib.SpecLibInfo;
import org.labkey.panoramapublic.model.speclib.SpecLibKey;
import org.labkey.panoramapublic.model.speclib.SpectrumLibrary;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SpecLibInfoManager
{
    private SpecLibInfoManager() {}

    public static SpecLibInfo[] getByRun(int runId)
    {
        SQLFragment sql = new SQLFragment(
                "SELECT sli.* FROM panoramapublic.speclibinforun slir " +
                        "JOIN panoramapublic.speclibinfo sli ON sli.id = slir.speclibinfoid " +
                        "WHERE runid = ?", runId);
        return new SqlSelector(PanoramaPublicSchema.getSchema(), sql).getArray(SpecLibInfo.class);
    }

    public static SpecLibInfo[] get(Container container)
    {
        SQLFragment sql = new SQLFragment(
                "SELECT sli.* FROM panoramapublic.speclibinforun slir " +
                        "JOIN panoramapublic.speclibinfo sli ON sli.id = slir.speclibinfoid " +
                        "JOIN targetedms.runs r ON r.id = slir.runid " +
                        "WHERE container = ?", container);
        return new SqlSelector(PanoramaPublicSchema.getSchema(), sql).getArray(SpecLibInfo.class);
    }

    public static SpecLibInfo get(int id)
    {
        Filter filter = new SimpleFilter(new FieldKey(null, "id"), id);
        return new TableSelector(PanoramaPublicManager.getTableInfoSpecLibInfo(), filter, null).getObject(SpecLibInfo.class);
    }

    public static SpecLibInfo update(SpecLibInfo specLibInfo, User user)
    {
        return Table.update(user, PanoramaPublicManager.getTableInfoSpecLibInfo(), specLibInfo, specLibInfo.getId());
    }

    public static SpecLibInfo addInfo(SpecLibInfo specLibInfo, User user)
    {
        return Table.insert(user, PanoramaPublicManager.getTableInfoSpecLibInfo(), specLibInfo);
    }

    public static List<ITargetedMSRun> getRuns(Set<Long> runIds, User user)
    {
        TargetedMSService svc = TargetedMSService.get();
        TableInfo runsTable = svc.getTableInfoRuns();
        var filter = new SimpleFilter().addInClause(FieldKey.fromParts("id"), runIds);
        List<RunContainer> runIdContainers = new TableSelector(runsTable,
                runsTable.getColumns("id", "container"), filter, null).getArrayList(RunContainer.class);


        return runIdContainers.stream()
                .filter(r -> r.getContainer().hasPermission(user, ReadPermission.class))
                .map(r -> svc.getRun(r.getId(), r.getContainer()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static @Nullable SpectrumLibrary getSpectrumLibrary(long specLibId, Container container, User user)
    {
        TargetedMSService svc = TargetedMSService.get();
        TableInfo table = svc.getUserSchema(user, container).getTable("spectrumlibrary");
        return new TableSelector(table).getObject(specLibId, SpectrumLibrary.class);
    }

    public static class RunContainer
    {
        private long _id;
        private Container _container;

        public long getId()
        {
            return _id;
        }

        public void setId(long id)
        {
            _id = id;
        }

        public Container getContainer()
        {
            return _container;
        }

        public void setContainer(Container container)
        {
            _container = container;
        }
    }

}
