package org.labkey.cromwell;

import org.labkey.api.security.roles.AbstractRootContainerRole;

public class CromwellRole extends AbstractRootContainerRole
{
    protected CromwellRole()
    {
        super("Cromwell Job Submitter", "Allows uer to submit jobs to the MacCoss lab's Cromwell server.",
                CromwellPermission.class
        );
        excludeGuests();
    }
}
