SELECT mod.modId, mod.unimodId AS givenUnimodId, mod.formula as givenFormula, mod.runIds, modinfo.Id as modInfoId, COALESCE(mod.unimodId, modInfo.unimodId) as unimodMatch FROM
    (SELECT
         smod.Id AS modId,
         smod.unimodId AS unimodId,
         smod.formula,
         GROUP_CONCAT(DISTINCT run.id, ',') AS runIds,
     FROM targetedms.PeptideStructuralModification pmod
              INNER JOIN targetedms.StructuralModification smod ON smod.id = pmod.structuralModId
              INNER JOIN targetedms.Peptide mol ON mol.id = pmod.peptideId
              INNER JOIN targetedms.PeptideGroup pg ON pg.id = mol.peptideGroupId
              INNER JOIN targetedms.Runs run ON run.id = pg.runId
     GROUP BY smod.Id, smod.unimodId, smod.formula
    ) mod
    LEFT OUTER JOIN panoramapublic.ExperimentStructuralModInfo modinfo
    ON mod.modId = modinfo.modId
