package org.labkey.cromwell.pipeline;

import org.labkey.cromwell.CromwellJob;
import org.labkey.cromwell.Workflow;

public interface CromwellJobSupport
{
    Workflow getWorkflow();
    CromwellJob getCromwellJob();
}
