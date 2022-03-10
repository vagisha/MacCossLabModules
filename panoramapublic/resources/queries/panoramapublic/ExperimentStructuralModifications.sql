SELECT mod.modId, mod.unimodId, mod.RunIds, mod.experimentAnnotationsId, modinfo.Id as modInfoId FROM
(SELECT
    mod.Id AS modId,
    mod.unimodId AS unimodId,
    GROUP_CONCAT(DISTINCT run.id, ',') AS RunIds,
    expAnnot.id AS experimentAnnotationsId
    FROM targetedms.peptidestructuralmodification pmod
    INNER JOIN targetedms.structuralmodification mod ON mod.id = pmod.structuralmodid
    INNER JOIN targetedms.peptide mol ON mol.id = pmod.peptideid
    INNER JOIN targetedms.peptidegroup pg ON pg.id = mol.peptidegroupid
    INNER JOIN targetedms.runs run ON run.id = pg.runId
    INNER JOIN exp.runs er ON er.lsid = run.experimentrunlsid
    INNER JOIN exp.rungroups rg ON rg.lsid = er.RunGroups.lsid
    INNER JOIN panoramapublic.experimentannotations expAnnot ON expAnnot.experimentId = rg.rowid
    GROUP BY mod.Id, mod.unimodId, expAnnot.Id
) mod
LEFT OUTER JOIN panoramapublic.experimentstructuralmodinfo modinfo
    ON mod.modId = modinfo.structuralmodid
    AND mod.experimentAnnotationsId = modinfo.ExperimentAnnotationsId
