SELECT mod.modId, mod.unimodId AS givenUnimodId, mod.RunIds, modinfo.Id as modInfoId FROM
(SELECT
    mod.Id AS modId,
    mod.unimodId AS unimodId,
    GROUP_CONCAT(DISTINCT run.id, ',') AS RunIds,
    FROM targetedms.peptidestructuralmodification pmod
    INNER JOIN targetedms.structuralmodification mod ON mod.id = pmod.structuralmodid
    INNER JOIN targetedms.peptide mol ON mol.id = pmod.peptideid
    INNER JOIN targetedms.peptidegroup pg ON pg.id = mol.peptidegroupid
    INNER JOIN targetedms.runs run ON run.id = pg.runId
    GROUP BY mod.Id, mod.unimodId
) mod
LEFT OUTER JOIN panoramapublic.experimentstructuralmodinfo modinfo
    ON mod.modId = modinfo.structuralmodid
