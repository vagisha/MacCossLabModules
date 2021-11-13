SELECT librarytype, name, filenamehint, skylinelibraryid, GROUP_CONCAT(DISTINCT runId, ',') AS RunIds
FROM targetedms.spectrumlibrary
GROUP BY librarytype, name, filenamehint, skylinelibraryid