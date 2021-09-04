package org.labkey.panoramapublic.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.panoramapublic.query.ExperimentAnnotationsManager;
import org.labkey.panoramapublic.query.SubmissionManager;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JournalSubmission
{
    private final JournalExperiment _journalExperiment;
    private List<Submission> _submissions;
    private List<Submission> _obsoleteSubmissions; // Submissions that are no longer associated with an experiment copy
                                                   // in the journal project because the journal copy was deleted.
                                                   // The rows are kept in the Submission table as a log of all submissions.
    private int _currentVersion;

    public JournalSubmission(@NotNull JournalExperiment journalExperiment)
    {
        _journalExperiment = journalExperiment;
    }

    public JournalExperiment getJournalExperiment()
    {
        return _journalExperiment;
    }

    public int getJournalExperimentId()
    {
        return _journalExperiment.getId();
    }

    public int getJournalId()
    {
        return _journalExperiment.getJournalId();
    }

    public int getExperimentAnnotationsId()
    {
        return _journalExperiment.getExperimentAnnotationsId();
    }

    public ShortURLRecord getShortAccessUrl()
    {
        return _journalExperiment.getShortAccessUrl();
    }

    public ShortURLRecord getShortCopyUrl()
    {
        return _journalExperiment.getShortCopyUrl();
    }

    public Date getCreated()
    {
        return _journalExperiment.getCreated();
    }

    public int getCreatedBy()
    {
        return _journalExperiment.getCreatedBy();
    }

    public Date getModified()
    {
        return _journalExperiment.getModified();
    }

    public int getModifiedBy()
    {
        return +_journalExperiment.getModifiedBy();
    }

    public Integer getAnnouncementId()
    {
        return _journalExperiment.getAnnouncementId();
    }

    public Integer getReviewerId()
    {
        return _journalExperiment.getReviewer();
    }

    public @NotNull List<Submission> getAllSubmissions()
    {
        return Stream.of(_submissions, _obsoleteSubmissions).flatMap(Collection::stream).collect(Collectors.toUnmodifiableList());
    }

    private List<Submission> submissions()
    {
        if (_submissions == null)
        {
            List<Submission> allSubmissions = SubmissionManager.getSubmissionsNewestFirst(getJournalExperimentId());
            _submissions = allSubmissions.stream().filter(s -> !s.isObsolete()).collect(Collectors.toList());
            _obsoleteSubmissions = allSubmissions.stream().filter(Submission::isObsolete).collect(Collectors.toList());

            Integer maxDataVersion = ExperimentAnnotationsManager.getMaxVersionForExperiment(getExperimentAnnotationsId());
            _currentVersion = maxDataVersion == null ? 0 : maxDataVersion;
        }
        return _submissions;
    }

    public @NotNull List<Submission> getCopiedSubmissions()
    {
        return submissions().stream().filter(Submission::hasCopy).collect(Collectors.toUnmodifiableList());
    }

    public @Nullable Submission getLatestSubmission()
    {
        return submissions().size() > 0 ? submissions().get(0) : null;
    }

    public @Nullable Submission getPendingSubmission()
    {
        return submissions().stream().filter(Submission::isPending).findFirst().orElse(null);
    }

    public boolean hasPendingSubmission()
    {
        return getPendingSubmission() != null;
    }

    public @Nullable Submission getLatestCopiedSubmission()
    {
        List<Submission> copiedSubmissions = getCopiedSubmissions();
        return copiedSubmissions.size() > 0 ? copiedSubmissions.get(0) : null;
    }

    public @Nullable Submission getSubmissionForCopiedExperiment(int copiedExperimentId)
    {
        return submissions().stream().filter(s -> s.hasCopy() && s.getCopiedExperimentId() == copiedExperimentId).findFirst().orElse(null);
    }

    public int getCurrentVersion()
    {
        return _currentVersion;
    }

    public int getNextVersion()
    {
        return getCurrentVersion() + 1;
    }

    public boolean isLatestExperimentCopy(int copiedExperimentId)
    {
        Submission lastCopied = getLatestCopiedSubmission();
        return lastCopied != null && copiedExperimentId == lastCopied.getCopiedExperimentId();
    }

    public boolean isLatestSubmission(int submissionId)
    {
        Submission submission = getLatestSubmission();
        return submission != null && submission.getId() == submissionId;
    }
}
