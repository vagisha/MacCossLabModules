package org.labkey.cromwell;

import org.labkey.api.admin.FolderExportPermission;
import org.labkey.api.data.Container;
import org.labkey.api.security.SecurableResource;
import org.labkey.api.security.SecurityPolicy;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.roles.AbstractModuleScopedRole;

public class CromwellRole extends AbstractModuleScopedRole
{
    protected CromwellRole()
    {
        super("Cromwell Job Submitter", "Can submit a job to the MacCoss lab's Cromwell server.",
                CromwellModule.class,
                ReadPermission.class,
                FolderExportPermission.class
        );
        excludeGuests();
    }

    @Override
    public boolean isApplicable(SecurityPolicy policy, SecurableResource resource)
    {
        return resource instanceof Container && ((Container) resource).getActiveModules().contains(getSourceModule());
    }

    @Override
    public boolean isAssignable()
    {
        return super.isAssignable();
    }
}
