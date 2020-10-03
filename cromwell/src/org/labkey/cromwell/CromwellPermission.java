package org.labkey.cromwell;

import org.labkey.api.security.permissions.AbstractPermission;

public class CromwellPermission extends AbstractPermission
{
    protected CromwellPermission()
    {
        super("Submit Cromwell Jobs", "Can submit jobs to the MacCoss lab's Cromwell server.", CromwellModule.class);
    }
}
