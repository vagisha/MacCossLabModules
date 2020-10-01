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
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.LabKeyError;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.ReturnUrlForm;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.admin.AdminUrls;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.TableInfo;
import org.labkey.api.files.FileContentService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusUrls;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.Group;
import org.labkey.api.security.GroupManager;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.DOM;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.WebPartView;
import org.labkey.cromwell.pipeline.CromwellPipelineJob;
import org.labkey.security.xml.GroupEnumType;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    public static class SubmitCromwellJobAction extends FormViewAction<CromwellJobForm>
    {
        @Override
        public void validateCommand(CromwellJobForm form, Errors errors)
        {
            if(!getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(CromwellModule.NAME)))
            {
                errors.reject(ERROR_MSG, "Cromwell module is not enabled in the container. Cannot submit Cromwell jobs.");
                return;
            }
            if(!hasCromwellRole(getUser(), getContainer()))
            {
                errors.reject(ERROR_MSG, "User does not have permission to submit Cromwell jobs.");
                return;
            }

            Workflow workflow = form.getWorkflow();
            if(workflow == null)
            {
                errors.reject(ERROR_MSG, "Could not find a workflow in the request.");
            }
        }

        @Override
        public ModelAndView getView(CromwellJobForm form, boolean reshow, BindException errors) throws Exception
        {
            Workflow workflow;
            var manager = CromwellManager.get();
            if(reshow)
            {
                workflow = form.getWorkflow();
                if(workflow == null)
                {
                    errors.reject(ERROR_MSG, "No workflow found in request");
                    return new SimpleErrorView(errors);
                }
                List<CromwellInput> formInputs = Workflow.getInputs(workflow);
                formInputs = Workflow.populateInputs(formInputs, getViewContext().getRequest().getParameterMap());
                form.setInputsArray(formInputs);
            }
            else
            {
                if(form.getJobId() != null)
                {
                    // We are making a copy of an existing Cromwell job.
                    CromwellJob job = manager.getCromwellJob(form.getJobId());
                    if(job == null)
                    {
                        errors.reject(ERROR_MSG, "Could not find a job with Id " + form.getJobId());
                        return new SimpleErrorView(errors);
                    }

                    workflow = manager.getWorkflow(job.getWorkflowId());
                    form.setWorkflow(workflow);
                    List<CromwellInput> formInputs = Workflow.getInputs(workflow);
                    formInputs = Workflow.copyInputsFromJob(formInputs, job);
                    form.setInputsArray(formInputs);
                }
                else
                {
                    if(form.getWorkflow() == null)
                    {
                        List<Workflow> workflows = manager.getWorkflows();
                        workflow = workflows.get(0); // TODO
                    }
                    else
                    {
                        workflow = form.getWorkflow();
                    }
                    form.setWorkflow(workflow);
                    form.setInputsArray(Workflow.getInputs(workflow)); // Set the input fields based on the WDL definition
                }
            }
            return new JspView<>("/org/labkey/cromwell/view/createJob.jsp", form, errors);
        }

        @Override
        public boolean handlePost(CromwellJobForm form, BindException errors) throws Exception
        {
            Workflow workflow = form.getWorkflow();

            // We do not have form fields for the workflow inputs.  Read them from the request parameter map.
            List<CromwellInput> workflowInputs = Workflow.getInputs(workflow);
            workflowInputs = Workflow.populateInputs(workflowInputs, getViewContext().getRequest().getParameterMap());

            Container container = getContainer();
            PipeRoot root = PipelineService.get().findPipelineRoot(container);
            if (root == null || !root.isValid())
            {
                errors.reject(ERROR_MSG, "No valid pipeline root found for " + container.getPath());
                return false;
            }

            CromwellManager cromwellManager = CromwellManager.get();

            ViewBackgroundInfo info = new ViewBackgroundInfo(container, getUser(), null);
            CromwellJob cromwellJob = new CromwellJob(workflow.getId(), container);
            cromwellJob.setInputs(Workflow.getInputsJSON(workflowInputs));
            cromwellJob = cromwellManager.saveNewJob(cromwellJob, getUser());

            CromwellPipelineJob job = new CromwellPipelineJob(info, root, workflow, cromwellJob.getId());
            try
            {
                PipelineService.get().queueJob(job);
            }
            catch (PipelineValidationException e)
            {
                cromwellManager.deleteJob(cromwellJob);
                errors.reject(ERROR_MSG, e.getMessage());
                return false;
            }
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
        return cromwellGroup == null || user.isInGroup(cromwellGroup.getUserId());

        //Role cromwellRole = RoleManager.getRole(CromwellRole.class);
        //return cromwellRole != null && container.getPolicy().getEffectiveRoles(user).contains(cromwellRole);
    }

    public static class CromwellJobForm extends ReturnUrlForm
    {
        private Workflow _workflow;
        private List<CromwellInput> _inputs;
        private Integer _jobId;

        public void setWorkflowId(int workflowId)
        {
           setWorkflow(CromwellManager.get().getWorkflow(workflowId));
        }
        public int getWorkflowId()
        {
            return _workflow!= null ? _workflow.getId() : 0;
        }

        public String getWorkflowName()
        {
            return _workflow!= null ? _workflow.getName() : "NOT FOUND";
        }

        public void setWorkflow(Workflow workflow)
        {
            _workflow = workflow;
        }

        public Workflow getWorkflow()
        {
            return _workflow;
        }

        public Integer getJobId()
        {
            return _jobId;
        }

        public void setJobId(Integer jobId)
        {
            _jobId = jobId;
        }

        public List<CromwellInput> getInputsArray()
        {
            return _inputs != null ? _inputs : Collections.emptyList();
        }

        public void setInputsArray(List<CromwellInput> inputs)
        {
            _inputs = inputs;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public static class CopyCromwellJobAction extends SimpleRedirectAction<CromwellJobForm>
    {

        @Override
        public URLHelper getRedirectURL(CromwellJobForm form)
        {
            return new ActionURL(SubmitCromwellJobAction.class, getContainer()).addParameter("jobId", form.getJobId());
        }
    }

    @RequiresPermission(AdminPermission.class)
    public static class GetFileRootTreeAction extends ReadOnlyApiAction
    {
        @Override
        public Object execute(Object o, BindException errors) throws Exception
        {
            Path fileRootPath = FileContentService.get().getFileRootPath(getContainer());
            JSONObject root = getFolderTreeJSON(fileRootPath);
            root.put("expanded", true);

            HttpServletResponse resp = getViewContext().getResponse();
            resp.setContentType("application/json");
//            JSONObject ret = new JSONObject();
//            ret.put("root", root);
            resp.getWriter().write(root.toString());
            return null;
        }

        private JSONObject getFolderTreeJSON(Path rootPath) throws IOException
        {
            JSONObject root = new JSONObject();
            JSONArray rootChildren = new JSONArray();

            for (Path path : Files.walk(rootPath, 1).filter(Files::isDirectory).sorted(Comparator.naturalOrder()).collect(Collectors.toList()))
            {
                if(path.equals(rootPath))
                {
                    continue;
                }
                JSONObject childTree = getFolderTreeJSON(path);
                rootChildren.put(childTree);
            }

            root.put("text", rootPath.getFileName());
            root.put("expanded", false);
            root.put("iconCls", "x4-tree-icon-parent");
            if(rootChildren.length() > 0)
            {
                root.put("children", rootChildren);
            }
            else
            {
                root.put("leaf", true);
            }

            return root;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class CromwellJobDetailsAction extends SimpleViewAction<CromwellJobForm>
    {
        @Override
        public ModelAndView getView(CromwellJobForm form, BindException errors)
        {
            Integer cromwellJobId = form.getJobId();
            CromwellJob job = CromwellManager.get().getCromwellJob(cromwellJobId);
            if(job == null)
            {
                errors.addError(new LabKeyError("Could not find a Cromwell job for Id: " + cromwellJobId));
                return new SimpleErrorView(errors);
            }

            VBox view = new VBox();
            DataRegion dr = getCromwellJobDetailsDataRegion();

            DetailsView detailsView = new DetailsView(dr, job.getId());
            view.addView(detailsView);

            if(job.getPipelineJobId() != null)
            {
                ActionURL pipelineJobUrl = PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlDetails(getContainer(), job.getPipelineJobId());
                String pipelineJobStatus = PipelineService.get().getStatusFile(job.getPipelineJobId()).getStatus();
                view.addView(new HtmlView(new Link.LinkBuilder("View Pipeline Job. Status: " + pipelineJobStatus).href(pipelineJobUrl).build()));
            }

            view.setTitle("Cromwell Job Details");
            view.setFrame(WebPartView.FrameType.PORTAL);
            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Cromwell Job Details");
        }
    }

    @NotNull
    private static DataRegion getCromwellJobDetailsDataRegion()
    {
        DataRegion dr = new DataRegion();
        TableInfo tInfo = CromwellManager.get().getTableInfoJob();
        dr.setColumns(tInfo.getColumns("Id", "Created", "CreatedBy", "Container", "WorkflowId", "CromwellJobId", "CromwellStatus", "PipelineJobId", "Inputs"));

        dr.getDisplayColumn("Inputs").setVisible(false);
        dr.getDisplayColumn("PipelineJobId").setVisible(false);

        DataColumn pipelineJobCol = (DataColumn)dr.getDisplayColumn("PipelineJobId");

        //errCol.setPreserveNewlines(true);

        SimpleDisplayColumn inputsCol = new SimpleDisplayColumn(){

            @Override
            public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String json = ctx.get(FieldKey.fromParts("Inputs"), String.class);
                if(!StringUtils.isBlank(json))
                {
                    JSONObject jsonObj = new JSONObject(json);
                    DOM.PRE(jsonObj.toString(2)).appendTo(out);
                }
                else
                {
                    super.renderDetailsCellContents(ctx, out);
                }
            }
        };
        inputsCol.setCaption("JSON Inputs:");

        // List<DisplayColumn> columns = dr.getDisplayColumns();
        dr.addDisplayColumn(inputsCol);
        return dr;
    }

    @RequiresPermission(AdminPermission.class)
    public class WorkflowDetailsAction extends SimpleViewAction<WorkflowForm>
    {
        @Override
        public ModelAndView getView(WorkflowForm form, BindException errors)
        {
            int workflowId = form.getWorkflowId();
            Workflow workflow = CromwellManager.get().getWorkflow(workflowId);
            if(workflow == null)
            {
                errors.addError(new LabKeyError("Could not find a workflow for Id: " + workflowId));
                return new SimpleErrorView(errors);
            }

            VBox view = new VBox();
            DataRegion dr = getWorkflowDetailsDataRegion();

            DetailsView detailsView = new DetailsView(dr, workflow.getId());
            view.addView(detailsView);

            view.setTitle("Cromwell Workflow Details");
            view.setFrame(WebPartView.FrameType.PORTAL);
            return view;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            root.addChild("Cromwell Workflow Details");
        }
    }

    public static class WorkflowForm
    {
        private int _workflowId;

        public void setWorkflowId(int workflowId)
        {
            _workflowId = workflowId;
        }
        public int getWorkflowId()
        {
            return _workflowId;
        }
    }

    @NotNull
    private static DataRegion getWorkflowDetailsDataRegion()
    {
        DataRegion dr = new DataRegion();
        TableInfo tInfo = CromwellManager.get().getTableInfoWorkflow();
        dr.setColumns(tInfo.getColumns("Id", "Created", "CreatedBy", "Name", "Version", "wdl"));

        dr.getDisplayColumn("wdl").setVisible(false);

        SimpleDisplayColumn wdlCol = new SimpleDisplayColumn(){

            @Override
            public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String wdl = ctx.get(FieldKey.fromParts("wdl"), String.class);
                if(!StringUtils.isBlank(wdl))
                {
                    DOM.PRE(wdl).appendTo(out);
                }
                else
                {
                    super.renderDetailsCellContents(ctx, out);
                }
            }
        };
        wdlCol.setCaption("WDL:");
        dr.addDisplayColumn(wdlCol);
        return dr;
    }

    public static class CromwellInput
    {
        private String _name;
        private String _displayName;
        private String _value;
        private String _workflowName;

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

        public String getWorkflowName()
        {
            return _workflowName;
        }

        public void setWorkflowName(String workflowName)
        {
            _workflowName = workflowName;
        }

        public boolean isWebDavDirUrl()
        {
            return _name.startsWith("url_webdav_");
        }
        public boolean isWebdavFileUrl()
        {
            return _name.startsWith("url_webdav_file_");
        }

        public HtmlString getExtInputField()
        {
            /*
            Example:
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
            inputJson.put("xtype", "textfield");
            inputJson.put("fieldLabel", getDisplayName());
            inputJson.put("name", getName());
            if(!StringUtils.isBlank(_value))
            {
                inputJson.put("value", _value);
            }
            inputJson.put("allowBlank", false);
            inputJson.put("width", 650);
            return inputJson.getHtmlString();
        }
    }
}