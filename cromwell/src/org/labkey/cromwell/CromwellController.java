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
import org.json.JSONObject;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.Group;
import org.labkey.api.security.GroupManager;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.WebPartView;
import org.labkey.cromwell.pipeline.CromwellPipelineJob;
import org.labkey.security.xml.GroupEnumType;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.DIV;

public class CromwellController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(CromwellController.class);
    public static final String NAME = "cromwell";

    public static final String PROPS_CROMWELL = "cromwell_properties";
    public static final String PROP_CROMWELL_SERVER_URL = "cromwell_server_url";
    public static final String PROP_CROMWELL_SERVER_PORT = "cromwell_server_port";
    public static final String PROP_SCP_USER = "cromwell_scp_user";
    public static final String PROP_SCP_KEY_FILE = "cromwell_scp_key_filepath";
    public static final String PROP_SCP_PORT = "cromwell_scp_port";
    public static final String PROP_ALLOWED_PROJECTS = "cromwell_allowed_projects";

    public CromwellController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(AdminOperationsPermission.class)
    public static class CromwellSettingsAction extends FormViewAction<CromwellSettingsForm>
    {
        @Override
        public void validateCommand(CromwellSettingsForm form, Errors errors)
        {
            if (StringUtils.isBlank(form.getCromwellServer()))
            {
                errors.reject(ERROR_MSG, "Cromwell server URL cannot be blank.");
            }
            if (StringUtils.isBlank(form.getScpUser()))
            {
                errors.reject(ERROR_MSG, "SCP user name cannot be blank.");
            }
            if (StringUtils.isBlank(form.getScpKeyFilePath()))
            {
                errors.reject(ERROR_MSG, "SCP key file path cannot be blank.");
            }
            List<String> invalidProjectNames = form.invalidProjectNames();
            if(invalidProjectNames.size() > 0)
            {
                errors.reject(ERROR_MSG, "Invalid project names: " + StringUtils.join(invalidProjectNames, ", "));
            }
        }

        @Override
        public ModelAndView getView(CromwellSettingsForm form, boolean reshow, BindException errors)
        {
            if(!reshow)
            {
                PropertyManager.PropertyMap map = PropertyManager.getEncryptedStore().getWritableProperties(PROPS_CROMWELL, false);
                if(map != null)
                {
                    form.setCromwellServer(map.get(PROP_CROMWELL_SERVER_URL));
                    form.setScpUser(map.get(PROP_SCP_USER));
                    form.setScpKeyFilePath(map.get(PROP_SCP_KEY_FILE));

                    String port = map.get(PROP_CROMWELL_SERVER_PORT);
                    if(port != null)
                    {
                        form.setPort(Integer.valueOf(port));
                    }
                    String scpPort = map.get(PROP_SCP_PORT);
                    if(scpPort != null)
                    {
                        form.setScpPort(Integer.valueOf(scpPort));
                    }
                    String allowedProjects = map.get(PROP_ALLOWED_PROJECTS);
                    if(allowedProjects != null)
                    {
                        form.setAllowedProjectNames(allowedProjects);
                    }

                }
            }
            JspView<CromwellSettingsForm> view = new JspView<>("/org/labkey/cromwell/view/cromwellSettings.jsp", form, errors);
            view.setFrame(WebPartView.FrameType.PORTAL);
            view.setTitle("Cromwell Settings");
            return view;
        }

        @Override
        public boolean handlePost(CromwellSettingsForm form, BindException errors)
        {
            PropertyManager.PropertyMap map = PropertyManager.getEncryptedStore().getWritableProperties(PROPS_CROMWELL, true);
            map.put(PROP_CROMWELL_SERVER_URL, form.getCromwellServer());
            map.put(PROP_CROMWELL_SERVER_PORT, form.getPort() != null ? String.valueOf(form.getPort()) : null);
            map.put(PROP_SCP_USER, form.getScpUser());
            map.put(PROP_SCP_KEY_FILE, form.getScpKeyFilePath());
            map.put(PROP_SCP_PORT, form.getScpPort() != null ? String.valueOf(form.getScpPort()) : null);
            map.put(PROP_ALLOWED_PROJECTS, form.getAllowedProjectNames());
            map.save();
            return true;
        }

        @Override
        public ModelAndView getSuccessView(CromwellSettingsForm form)
        {
            ActionURL adminConsoleUrl = PageFlowUtil.urlProvider(AdminUrls.class).getAdminConsoleURL();
            return new HtmlView(
                    DIV("Cromwell settings saved!",
                            BR(),
                            new Link.LinkBuilder("Back to Admin Console").href(adminConsoleUrl).build()));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Cromwell Settings");
        }

        @Override
        public URLHelper getSuccessURL(CromwellSettingsForm cromwellSettingsForm)
        {
            return null;
        }
    }

    public static class CromwellSettingsForm
    {
        private String _cromwellServer;
        private Integer _port;
        private String _scpUser;
        private String _scpKeyFilePath;
        private Integer _scpPort;
        private String _allowedProjectNames;

        public String getCromwellServer()
        {
            return _cromwellServer;
        }

        public void setCromwellServer(String cromwellServer)
        {
            _cromwellServer = cromwellServer;
        }

        public Integer getPort()
        {
            return _port;
        }

        public void setPort(Integer port)
        {
            _port = port;
        }

        public String getScpUser()
        {
            return _scpUser;
        }

        public void setScpUser(String scpUser)
        {
            _scpUser = scpUser;
        }

        public String getScpKeyFilePath()
        {
            return _scpKeyFilePath;
        }

        public void setScpKeyFilePath(String scpKeyFilePath)
        {
            _scpKeyFilePath = scpKeyFilePath;
        }

        public Integer getScpPort()
        {
            return _scpPort;
        }

        public void setScpPort(Integer scpPort)
        {
            _scpPort = scpPort;
        }

        public String getAllowedProjectNames()
        {
            return _allowedProjectNames;
        }

        public void setAllowedProjectNames(String allowedProjectNames)
        {
            _allowedProjectNames = allowedProjectNames;
        }

        public List<String> invalidProjectNames()
        {
            if (StringUtils.isBlank(_allowedProjectNames))
            {
                return Collections.emptyList();
            }
            String[] projNames = StringUtils.split(_allowedProjectNames, ',');
            List<String> invalidName = new ArrayList<>();
            for (String projName : projNames)
            {
                Container proj = ContainerManager.getForPath(projName.trim());
                if (proj == null)
                {
                    invalidName.add(projName);
                }
            }

            return invalidName;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public static class SubmitCromwellJob extends FormViewAction<CromwellJobForm>
    {
        @Override
        public void validateCommand(CromwellJobForm form, Errors errors)
        {
           if(!hasCromwellRole(getUser(), getContainer()))
           {
               errors.reject(ERROR_MSG, "User does not have permission to submit Cromwell jobs.");
           }
        }

        @Override
        public ModelAndView getView(CromwellJobForm form, boolean reshow, BindException errors)
        {
            if(!reshow)
            {
                var manager = CromwellManager.get();
                List<Workflow> workflows = manager.getWorkflows();
                Workflow workflow = workflows.get(0);
                form.setWorkflowId(workflow.getId());
                form.setWorkflowName(workflow.getName());
            }

            return new JspView<>("/org/labkey/cromwell/view/createJob.jsp", form, errors);
        }

        @Override
        public boolean handlePost(CromwellJobForm form, BindException errors)
        {
            int workflowId = form.getWorkflowId();
            Container container = getContainer();

            Workflow workflow = CromwellManager.get().getWorkflow(workflowId);
            if(workflow == null)
            {
                errors.reject(ERROR_MSG, "Could not find a workflow with id " + workflowId);
                return false;
            }

            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            if (root == null || !root.isValid())
            {
                errors.reject(ERROR_MSG, "No valid pipeline root found for " + container.getPath());
                return false;
            }

            CromwellManager cromwellManager = CromwellManager.get();

            ViewBackgroundInfo info = new ViewBackgroundInfo(container, getUser(), null);
            CromwellJob cromwellJob = new CromwellJob(workflowId, getContainer());
            cromwellJob.setInputs(form.getInputsJSON());
            cromwellJob = cromwellManager.saveNewJob(cromwellJob, getUser());

            CromwellPipelineJob job = new CromwellPipelineJob(info, root, workflow, cromwellJob);
            try
            {
                PipelineService.get().queueJob(job);
            }
            catch (PipelineValidationException e)
            {
                // lincsManager.deleteLincsPspJob(newPspJob);
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }

            int jobId = PipelineService.get().getJobId(getUser(), container, job.getJobGUID());
            cromwellJob.setPipelineJobId(jobId);
            cromwellManager.updateJob(cromwellJob, getUser());
            return true;
        }

        @Override
        public URLHelper getSuccessURL(CromwellJobForm lincsPspJobForm)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Create Cromwell Job");
        }
    }

    private static boolean hasCromwellRole(@NotNull User user, @NotNull Container container)
    {
        Group cromwellGroup = GroupManager.getGroup(ContainerManager.getRoot(), "Cromwell", GroupEnumType.SITE);
        return cromwellGroup != null && user.isInGroup(cromwellGroup.getUserId());

        //Role cromwellRole = RoleManager.getRole(CromwellRole.class);
        //return cromwellRole != null && container.getPolicy().getEffectiveRoles(user).contains(cromwellRole);
    }

    public static class CromwellJobForm extends ReturnUrlForm
    {
        private int _workflowId;
        private String _workflowName;
        private String _skylineTemplateUrl;
        private String _rawFilesFolderUrl;
        private String _targetFolderUrl;
        private String _rawFileExtention;
        private String _apiKey;
        private List<CromwellInput> _inputs;

        public int getWorkflowId()
        {
            return _workflowId;
        }

        public String getWorkflowName()
        {
            return _workflowName;
        }

        public void setWorkflowName(String workflowName)
        {
            _workflowName = workflowName;
        }

        public void setWorkflowId(int workflowId)
        {
            _workflowId = workflowId;
        }

        public String getSkylineTemplateUrl()
        {
            return _skylineTemplateUrl;
        }

        public void setSkylineTemplateUrl(String skylineTemplateUrl)
        {
            _skylineTemplateUrl = skylineTemplateUrl;
        }

        public String getRawFilesFolderUrl()
        {
            return _rawFilesFolderUrl;
        }

        public void setRawFilesFolderUrl(String rawFilesFolderUrl)
        {
            _rawFilesFolderUrl = rawFilesFolderUrl;
        }

        public String getTargetFolderUrl()
        {
            return _targetFolderUrl;
        }

        public void setTargetFolderUrl(String targetFolderUrl)
        {
            _targetFolderUrl = targetFolderUrl;
        }

        public String getRawFileExtention()
        {
            return _rawFileExtention;
        }

        public void setRawFileExtention(String rawFileExtention)
        {
            _rawFileExtention = rawFileExtention;
        }

        public String getApiKey()
        {
            return _apiKey;
        }

        public void setApiKey(String apiKey)
        {
            _apiKey = apiKey;
        }

        public String getInputsJSON()
        {
            return "testing_inputs: test";
        }

        public void setInputs(String inputsJSON)
        {
            List<CromwellInput> inputs = new ArrayList<>();
            JSONObject json = new JSONObject(inputsJSON);
            for (Iterator<String> it = json.keys(); it.hasNext(); )
            {
                String key = it.next();
                String value = json.getString(key);
                inputs.add(new CromwellInput(key, value));
            }
            _inputs = inputs;
        }

        public String getInputs()
        {
            JSONObject json = new JSONObject();
            for(CromwellInput input: getInputsArray())
            {
                json.put(input.getName(), input.getValue());
            }
            return json.toString();
        }

        public List<CromwellInput> getInputsArray()
        {
            return _inputs != null ? _inputs : Collections.emptyList();
        }
    }

    public static class CromwellInput
    {
        private String _name;
        private String _displayName;
        private String _value;

        public CromwellInput() {}

        public CromwellInput(String name, String displayName)
        {
            _name = name;
            _displayName = displayName;
        }

        public CromwellInput(String name, String formFieldName, String value)
        {
            this(name, formFieldName);
            _value = value;
        }

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public String getDisplayName()
        {
            return _displayName;
        }

        public void setDisplayName(String displayName)
        {
            _displayName = displayName;
        }

        public String getValue()
        {
            return _value;
        }

        public void setValue(String value)
        {
            _value = value;
        }

        public String getExtInputField()
        {
            /*
                {
                    xtype: 'xtype',
                    fieldLabel: 'Skyline template URL',
                    name: 'skylineTemplateUrl',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getSkylineTemplateUrl())%>,
                    afterBodyEl: '<span style="font-size: 0.9em;">WebDav URL of the Skyline template file (.sky)</span>',
                    msgTarget : 'under'
                }
                 */
            JSONObject inputJson = new JSONObject();
            inputJson.put("xtype", "xtype");
            inputJson.put("fieldLabel", getDisplayName());
            inputJson.put("name", getName());
            if(!StringUtils.isBlank(_value))
            {
                inputJson.put("value", _value);
            }
            inputJson.put("allowBlank", false);
            inputJson.put("width", 650);
            return inputJson.toString();
        }
    }
}