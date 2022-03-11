SELECT mod.modId, mod.unimodId AS givenUnimodId, mod.runIds, modinfo.Id as modInfoId FROM
    (SELECT
         mod.Id AS modId,
         mod.unimodId AS unimodId,
         GROUP_CONCAT(DISTINCT run.id, ',') AS runIds,
     FROM targetedms.PeptideIsotopeModification pmod
              INNER JOIN targetedms.IsotopeModification mod ON mod.id = pmod.isotopeModId
              INNER JOIN targetedms.Peptide mol ON mol.id = pmod.peptideId
              INNER JOIN targetedms.PeptideGroup pg ON pg.id = mol.peptideGroupId
              INNER JOIN targetedms.Runs run ON run.id = pg.runId
     GROUP BY mod.Id, mod.unimodId
    ) mod
    LEFT OUTER JOIN panoramapublic.ExperimentIsotopeModInfo modinfo
                        ON mod.modId = modinfo.isotopeModId