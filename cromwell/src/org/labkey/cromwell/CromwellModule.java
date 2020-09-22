/*
 * Copyright (c) 2020 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.cromwell;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.SpringModule;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.DataView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.labkey.cromwell.CromwellController.PROPS_CROMWELL;
import static org.labkey.cromwell.CromwellController.PROP_ALLOWED_PROJECTS;

public class CromwellModule extends SpringModule
{
    public static final String NAME = "cromwell";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 20.000;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        BaseWebPartFactory workflowsWebpartFactory = new BaseWebPartFactory("Cromwell Workflows")
        {
            @Override
            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
            {
                CromwellSchema schema = new CromwellSchema(portalCtx.getUser(), portalCtx.getContainer());
                QuerySettings settings = schema.getSettings(portalCtx, "workflows", CromwellSchema.TABLE_WORKFLOW);
                settings.setShowReports(false);
                settings.setAllowCustomizeView(true);
                settings.setAllowChooseQuery(false);
                settings.setAllowChooseView(true);
                QueryView view = new QueryView(schema, settings, null);
                view.setTitle("Cromwell Workflows");
                return view;
            }
        };

        BaseWebPartFactory jobsWebpartFactory = new BaseWebPartFactory("Cromwell Jobs")
        {
            @Override
            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
            {
                CromwellSchema schema = new CromwellSchema(portalCtx.getUser(), portalCtx.getContainer());
                QuerySettings settings = schema.getSettings(portalCtx, "cromwell jobs", CromwellSchema.TABLE_JOB);
                settings.setShowReports(false);
                settings.setAllowCustomizeView(true);
                settings.setAllowChooseQuery(false);
                settings.setAllowChooseView(true);
                QueryView view = new QueryView(schema, settings, null)
                {
                    @Override
                    protected void populateButtonBar(DataView view, ButtonBar bar)
                    {
                        super.populateButtonBar(view, bar);

                        if (getContainer().hasPermission(getUser(), InsertPermission.class))
                        {
                            ActionURL insertURL = new ActionURL(CromwellController.SubmitCromwellJob.class, getContainer());
                            insertURL.addParameter(ActionURL.Param.returnUrl, getViewContext().getActionURL().getLocalURIString());
                            ActionButton insert = new ActionButton("Create New Job", insertURL);
                            insert.setActionType(ActionButton.Action.LINK);
                            insert.setDisplayPermission(AdminPermission.class);
                            bar.add(insert);
                        }
                    }
                };
                view.setTitle("Cromwell Jobs");
                return view;
            }
        };

        List<WebPartFactory> webpartFactoryList = new ArrayList<>();
        webpartFactoryList.add(workflowsWebpartFactory);
        webpartFactoryList.add(jobsWebpartFactory);
        return webpartFactoryList;
    }

    @Override
    protected void init()
    {
        addController(CromwellController.NAME, CromwellController.class);
        CromwellSchema.register(this);
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(new CromwellContainerListener());

        // Register the CromwellRole
        RoleManager.registerRole(new CromwellRole());

        // Add a link in the admin console to manage journals.
        ActionURL url = new ActionURL(CromwellController.CromwellSettingsAction.class, ContainerManager.getRoot());
        AdminConsole.addLink(AdminConsole.SettingsLinkType.Configuration, "cromwell settings", url, AdminOperationsPermission.class);

    }

    @Override
    @NotNull
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton(CromwellSchema.NAME);
    }

    @Override
    public boolean canBeEnabled(Container c)
    {
        PropertyManager.PropertyMap map = PropertyManager.getEncryptedStore().getWritableProperties(PROPS_CROMWELL, false);
        if(c.getProject() != null && map != null)
        {
            String allowedProjNames = map.get(PROP_ALLOWED_PROJECTS);
            if (!StringUtils.isBlank(allowedProjNames))
            {
                String containerProject = c.getProject().getName();
                String[] projNames = StringUtils.split(allowedProjNames, ',');
                for (String projName : projNames)
                {
                    if(containerProject.equals(projName.trim()))
                    {
                        return true;
                    }
                }
                return false;
            }
        }
        return super.canBeEnabled(c);
    }

    @Override
    public boolean getRequireSitePermission()
    {
        return super.getRequireSitePermission();
    }
}