SELECT lib.*,
       lib.RunIds AS SkylineDocuments,
       lib.SpecLibId AS Details,
       run.Container AS libContainer,
       libinfo.id AS specLibInfoId
FROM
(SELECT librarytype,
        name,
        filenamehint,
        skylinelibraryid,
        GROUP_CONCAT(DISTINCT runId, ',') AS RunIds, -- Skyline documents that have this library
        MAX(Id) AS SpecLibId -- One spectral library that we can use as the example
 FROM targetedms.spectrumlibrary
 GROUP BY librarytype, name, filenamehint, skylinelibraryid
) lib
INNER JOIN targetedms.spectrumlibrary lib2 ON lib2.id = lib.SpecLibId
INNER JOIN targetedms.runs run on lib2.runId = run.id
LEFT OUTER JOIN panoramapublic.speclibinfo libInfo ON
libInfo.librarytype = lib.librarytype
AND libInfo.name = lib.name
AND ((libInfo.filenamehint IS NULL AND lib.filenamehint IS NULL) OR (libInfo.filenamehint = lib.filenamehint))
AND ((libinfo.skylinelibraryid IS NULL AND lib.skylinelibraryid IS NULL) OR libinfo.skylinelibraryid = lib.skylinelibraryid)