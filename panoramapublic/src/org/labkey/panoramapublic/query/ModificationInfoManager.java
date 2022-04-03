package org.labkey.panoramapublic.query;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Container;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.query.modification.ExperimentIsotopeModInfo;
import org.labkey.panoramapublic.query.modification.ExperimentModInfo;
import org.labkey.panoramapublic.query.modification.ExperimentStructuralModInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModificationInfoManager
{
    private ModificationInfoManager() {}

    public static ExperimentStructuralModInfo getStructuralModInfo(int modInfoId)
    {
        return new TableSelector(PanoramaPublicManager.getTableInfoExperimentStructuralModInfo()).getObject(modInfoId, ExperimentStructuralModInfo.class);
    }

    public static ExperimentIsotopeModInfo getIsotopeModInfo(int modInfoId)
    {
        ExperimentIsotopeModInfo modInfo = new TableSelector(PanoramaPublicManager.getTableInfoExperimentIsotopeModInfo()).getObject(modInfoId, ExperimentIsotopeModInfo.class);
        addAdditionalIsotopeUnimodInfos(modInfo);
        return modInfo;
    }

    private static void addAdditionalIsotopeUnimodInfos(ExperimentIsotopeModInfo modInfo)
    {
        if (modInfo != null)
        {
            List<ExperimentModInfo.UnimodInfo> unimodInfos = new TableSelector(PanoramaPublicManager.getTableInfoIsotopeUnimodInfo(),
                    new SimpleFilter(FieldKey.fromParts("ModInfoId"), modInfo.getId()), null).getArrayList(ExperimentModInfo.UnimodInfo.class);
            unimodInfos.forEach(modInfo::addUnimodInfo);
        }
    }

    public static ExperimentStructuralModInfo getStructuralModInfo(long modificationId, int experimentAnnotationsId)
    {
        return getModInfo(modificationId, experimentAnnotationsId, PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), ExperimentStructuralModInfo.class);
    }

    public static ExperimentIsotopeModInfo getIsotopeModInfo(long modificationId, int experimentAnnotationsId)
    {
        ExperimentIsotopeModInfo modInfo = getModInfo(modificationId, experimentAnnotationsId, PanoramaPublicManager.getTableInfoExperimentIsotopeModInfo(), ExperimentIsotopeModInfo.class);
        addAdditionalIsotopeUnimodInfos(modInfo);
        return modInfo;
    }

    private static <T extends ExperimentModInfo> T getModInfo(long modificationId, int experimentAnnotationsId, TableInfo tableInfo, Class<T> cls)
    {
        var filter = new SimpleFilter().addCondition(FieldKey.fromParts("experimentAnnotationsId"), experimentAnnotationsId);
        filter.addCondition(FieldKey.fromParts("ModId"), modificationId);
        return new TableSelector(tableInfo, filter, null).getObject(cls);
    }

    public static ExperimentStructuralModInfo saveStructuralModInfo (ExperimentStructuralModInfo modInfo, User user)
    {
        return Table.insert(user, PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), modInfo);
    }

    public static ExperimentIsotopeModInfo saveIsotopeModInfo (ExperimentIsotopeModInfo modInfo, User user)
    {
        var savedModInfo = Table.insert(user, PanoramaPublicManager.getTableInfoExperimentIsotopeModInfo(), modInfo);
        for (ExperimentModInfo.UnimodInfo unimodInfo: modInfo.getAdditionalUnimodInfos())
        {
            unimodInfo.setModInfoId(savedModInfo.getId());
            Table.insert(user, PanoramaPublicManager.getTableInfoIsotopeUnimodInfo(), unimodInfo);
        }
        return savedModInfo;
    }

    public static void deleteIsotopeModInfo(ExperimentIsotopeModInfo modInfo, int expAnnotationsId, Container container)
    {
        Table.delete(PanoramaPublicManager.getTableInfoIsotopeUnimodInfo(), new SimpleFilter(FieldKey.fromParts("modInfoId"), modInfo.getId()));
        deleteModInfo(modInfo, expAnnotationsId, container, PanoramaPublicManager.getTableInfoExperimentIsotopeModInfo());
    }

    public static void deleteStructuralModInfo(ExperimentModInfo modInfo, int expAnnotationsId, Container container)
    {
        deleteModInfo(modInfo, expAnnotationsId, container, PanoramaPublicManager.getTableInfoExperimentStructuralModInfo());
    }

    private static void deleteModInfo(ExperimentModInfo modInfo, int expAnnotationsId, Container container, TableInfo tableInfo)
    {
        ExperimentAnnotations experimentAnnotations = ExperimentAnnotationsManager.get(expAnnotationsId, container);
        if (experimentAnnotations != null && modInfo.getExperimentAnnotationsId() == expAnnotationsId)
        {
            Table.delete(tableInfo, modInfo.getId());
        }
    }

    public static List<ExperimentStructuralModInfo> getStructuralModInfosForExperiment(int experimentAnnotationsId, Container container)
    {
        return getModInfosForExperiment(experimentAnnotationsId, container, PanoramaPublicManager.getTableInfoExperimentStructuralModInfo(), ExperimentStructuralModInfo.class);
    }

    public static List<ExperimentIsotopeModInfo> getIsotopeModInfosForExperiment(int experimentAnnotationsId, Container container)
    {
        var modInfos = getModInfosForExperiment(experimentAnnotationsId, container, PanoramaPublicManager.getTableInfoExperimentIsotopeModInfo(), ExperimentIsotopeModInfo.class);
        modInfos.forEach(ModificationInfoManager::addAdditionalIsotopeUnimodInfos);
        return modInfos;
    }

    private static <T extends ExperimentModInfo> List<T> getModInfosForExperiment(int experimentAnnotationsId, Container container, TableInfo tableInfo, Class<T> cls)
    {
        var expAnnotations = ExperimentAnnotationsManager.get(experimentAnnotationsId);
        if (expAnnotations != null && expAnnotations.getContainer().equals(container))
        {
            var filter = new SimpleFilter().addCondition(FieldKey.fromParts("experimentAnnotationsId"), experimentAnnotationsId);
            return new TableSelector(tableInfo, filter, null).getArrayList(cls);
        }
        return Collections.emptyList();
    }

    public static boolean runsHaveStructuralModifications(List<Long> runIds, User user, Container container)
    {
        return runsHaveModifications(runIds, TargetedMSService.get().getTableInfoPeptideStructuralModification(), user, container);
    }

    public static boolean runsHaveIsotopeModifications(List<Long> runIds, User user, Container container)
    {
        return runsHaveModifications(runIds, TargetedMSService.get().getTableInfoPeptideIsotopeModification(), user, container);
    }

    private static boolean runsHaveModifications(List<Long> runIds, TableInfo tableInfo, User user, Container container)
    {
        if (runIds.size() == 0) return false;

        TargetedMSService svc = TargetedMSService.get();
        SQLFragment sql = new SQLFragment("SELECT mod.Id FROM ")
                .append(tableInfo, "mod")
                .append(" INNER JOIN ")
                .append(svc.getTableInfoGeneralMolecule(), "gm").append(" ON gm.Id = mod.peptideId ")
                .append(" INNER JOIN ")
                .append(svc.getTableInfoPeptideGroup(), "pg").append(" ON pg.Id = gm.peptideGroupId ")
                .append(" WHERE pg.runId IN (").append(StringUtils.join(runIds, ",")).append(") ");
        var schema = svc.getUserSchema(user, container).getDbSchema();
        sql = schema.getSqlDialect().limitRows(sql, 1);
        return new SqlSelector(schema, sql).exists();
    }

    public static List<Character> getIsotopeModificationSites(long isotopeModId, List<ITargetedMSRun> runs, User user)
    {
        Set<Character> sites = new HashSet<>();
        for (ITargetedMSRun run: runs)
        {
            sites.addAll(getIsotopeModificationSites(isotopeModId, run, user).stream()
                    .filter(aa -> !StringUtils.isBlank(aa))
                    .map(aa -> aa.charAt(0))
                    .collect(Collectors.toList()));
        }
        return sites.stream().collect(Collectors.toList());
    }

    private static List<String> getIsotopeModificationSites(long isotopeModId, ITargetedMSRun run, User user)
    {
        TargetedMSService svc = TargetedMSService.get();
        if (run != null)
        {
            SQLFragment sql = new SQLFragment("SELECT DISTINCT substring(pep.sequence, pimod.indexAA + 1, 1) FROM ")
                    .append(svc.getTableInfoPeptideIsotopeModification(), "pimod")
                    .append(" INNER JOIN ")
                    .append("targetedms.peptide AS pep").append(" ON pep.Id = pimod.peptideId ")
                    .append(" INNER JOIN ")
                    .append(svc.getTableInfoGeneralMolecule(), "gm").append(" ON gm.Id = pep.Id ")
                    .append(" INNER JOIN ")
                    .append(svc.getTableInfoPeptideGroup(), "pg").append(" ON pg.Id = gm.peptideGroupId ")
                    .append(" WHERE pg.runId = ?").add(run.getId())
                    .append(" AND pimod.IsotopeModId = ?").add(isotopeModId);
            return new SqlSelector(svc.getUserSchema(user, run.getContainer()).getDbSchema(), sql).getArrayList(String.class);
        }
        return Collections.emptyList();
    }
}
