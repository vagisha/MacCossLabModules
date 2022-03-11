SELECT mod.modId, mod.unimodId AS givenUnimodId, mod.runIds, modinfo.Id as modInfoId FROM
    (SELECT
         mod.Id AS modId,
         mod.unimodId AS unimodId,
         GROUP_CONCAT(DISTINCT run.id, ',') AS runIds,
     FROM targetedms.PeptideStructuralModification pmod
              INNER JOIN targetedms.StructuralModification mod ON mod.id = pmod.structuralModId
              INNER JOIN targetedms.Peptide mol ON mol.id = pmod.peptideId
              INNER JOIN targetedms.PeptideGroup pg ON pg.id = mol.peptideGroupId
              INNER JOIN targetedms.Runs run ON run.id = pg.runId
     GROUP BY mod.Id, mod.unimodId
    ) mod
    LEFT OUTER JOIN panoramapublic.ExperimentStructuralModInfo modinfo
                        ON mod.modId = modinfo.structuralModId
