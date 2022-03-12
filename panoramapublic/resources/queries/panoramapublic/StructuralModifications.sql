SELECT mod.modId, mod.unimodId AS givenUnimodId, mod.runIds, modinfo.Id as modInfoId FROM
    (SELECT
         smod.Id AS modId,
         smod.unimodId AS unimodId,
         GROUP_CONCAT(DISTINCT run.id, ',') AS runIds,
     FROM targetedms.PeptideStructuralModification pmod
              INNER JOIN targetedms.StructuralModification smod ON smod.id = pmod.structuralModId
              INNER JOIN targetedms.Peptide mol ON mol.id = pmod.peptideId
              INNER JOIN targetedms.PeptideGroup pg ON pg.id = mol.peptideGroupId
              INNER JOIN targetedms.Runs run ON run.id = pg.runId
     GROUP BY smod.Id, smod.unimodId
    ) mod
    LEFT OUTER JOIN panoramapublic.ExperimentStructuralModInfo modinfo
                        ON mod.modId = modinfo.modId
