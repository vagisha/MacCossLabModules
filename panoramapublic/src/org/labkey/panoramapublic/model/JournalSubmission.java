package org.labkey.panoramapublic.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.panoramapublic.query.SubmissionManager;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class JournalSubmission
{
    private final JournalExperiment _journalExperiment;
    private List<Submission> _submissions;

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

    public @NotNull List<Submission> getSubmissions()
    {
        return Collections.unmodifiableList(submissions());
    }

    private List<Submission> submissions()
    {
        if(_submissions == null)
        {
            _submissions = SubmissionManager.getSubmissionsNewestFirst(getJournalExperimentId());
        }
        return _submissions;
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

    public @NotNull List<Submission> getCopiedSubmissions()
    {
        return submissions().stream().filter(Submission::wasCopied).collect(Collectors.toUnmodifiableList());
    }

    public @Nullable Submission getLatestCopiedSubmission()
    {
        List<Submission> copiedSubmissions = getCopiedSubmissions();
        return copiedSubmissions.size() == 0 ? null : copiedSubmissions.get(0);
    }

    public @Nullable Submission getSubmissionForCopiedExperiment(int copiedExperimentId)
    {
        return submissions().stream().filter(s -> s.wasCopied() && s.getCopiedExperimentId() == copiedExperimentId).findFirst().orElse(null);
    }

    public int getCurrentVersion()
    {
        List<Submission> copiedSubmissions = getCopiedSubmissions();
        return copiedSubmissions.size() == 0 ? 0 : copiedSubmissions.get(0).getVersion();
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
