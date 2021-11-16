SELECT lib.*, libinfo.id AS specLibInfoId, libinfo.experimentAnnotationsId FROM
(SELECT librarytype,
        name,
        filenamehint,
        skylinelibraryid,
        GROUP_CONCAT(DISTINCT runId, ',') AS RunIds,
        MAX(Id) AS SpecLibId,
        MAX(Id) AS Details
 FROM targetedms.spectrumlibrary
 GROUP BY librarytype, name, filenamehint, skylinelibraryid
) lib
LEFT OUTER JOIN panoramapublic.speclibinfo libInfo ON
libInfo.librarytype = lib.librarytype
AND libInfo.name = lib.name
AND libInfo.filenamehint = lib.filenamehint
AND libinfo.skylinelibraryid = lib.skylinelibraryid