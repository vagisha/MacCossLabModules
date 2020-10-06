<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.cromwell.CromwellController" %>
<%@ page import="org.labkey.cromwell.CromwellController.CromwellJobForm" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.security.permissions.AdminPermission" %>
<%@ page import="org.labkey.api.security.roles.RoleManager" %>
<%@ page import="org.labkey.cromwell.CromwellInput" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("File");
    }
%>
<labkey:errors/>
<%
    CromwellJobForm form = ((JspView<CromwellJobForm>) HttpView.currentView()).getModelBean();
%>

<div style="margin-top:15px;" id="newJobForm"></div>

<% if(form.getJobId() != null) { %>
<div style="font-weight: bold; color: red;">
    NOTE: Submitting a job with the same input parameters as a previous job will overwrite any output files that were created by the previous job.
</div>
<% } %>

<script type="text/javascript">

    Ext4.onReady(function(){

        Ext4.define('TreeStoreModel', {
            extend: 'Ext.data.Model',
            fields: [
                { name: 'text', type: 'string' },
                // { name: 'webdavPath', type: 'string' },
                { name: 'expanded', defaultValue: false },
                { name: 'children' },
                { name: 'leaf' },
                { name: 'iconCls', type: 'string' },
                { name: 'isFile', type: 'boolean', defaultValue: false }
            ]
        });

        var folderTreeStore = Ext4.create('Ext.data.TreeStore', {
            proxy: {
                type: 'ajax',
                url: LABKEY.ActionURL.buildURL('cromwell', 'getFileRootTree.api'),
                extraParams: {requiredPermission: <%=q(RoleManager.getPermission(AdminPermission.class).getUniqueName())%>}
            },
            root: {
                expanded: true,
                text: ""
            },
            model: 'TreeStoreModel',
            autoLoad: true
        });

        var filesTreeStore = Ext4.create('Ext.data.TreeStore', {
            proxy: {
                type: 'ajax',
                url: LABKEY.ActionURL.buildURL('cromwell', 'getFileRootTree.api'),
                extraParams: {requiredPermission: <%=q(RoleManager.getPermission(AdminPermission.class).getUniqueName())%>, files: true}
            },
            root: {
                expanded: true,
                text: ""
            },
            model: 'TreeStoreModel',
            autoLoad: true
        });

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: "newJobForm",
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 250,
                width: 650,
                margin: '0 10 0 10',
                labelStyle: 'background-color: #E0E6EA; padding: 5px;'
            },
            items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                {
                    xtype: 'displayfield',
                    fieldLabel: "Workflow",
                    width: 650,
                    value: <%=q(form.getWorkflowName())%>
                },
                {
                    xtype:'hidden',
                    name: 'workflowId',
                    value: <%=form.getWorkflowId()%>
                }
                <%for (CromwellInput input: form.getInputsArray()) {
                    var inputFieldId = input.getName() + "_input";
                    var hashInputFieldId = "#" + inputFieldId;
                %>
                ,
                {
                    xtype: 'textfield',
                    fieldLabel: <%=qh(input.getDisplayName())%>,
                    name: <%=q(input.getName())%>,
                    value: <%=q(input.getValue())%>,
                    allowBlank: false,
                    width: 650,
                    margin: '20 10 0 10',
                    id: <%=q(inputFieldId)%>
                }

                <%if(input.isWebdav()) { %>
                ,{
                    xtype: 'treepanel',
                    fieldLabel: 'Choose directory',
                    store: <% if(input.isWebdavFileUrl()) { %> filesTreeStore <% } else { %> folderTreeStore <% } %>,
                    rootVisible: false,
                    enableDrag: false,
                    useArrows : false,
                    autoScroll: true,
                    title : '',
                    border: true,
                    width: 650,
                    height:100,
                    margin: '0 10 0 10',
                    region     : 'east',
                    split      : true,
                    listeners: {
                        select: function(node, record, index, eOpts){
                            // console.log("the record is... " + record.get('id') + ". Path: " + record.getPath('text'));
                            var displayField = Ext4.ComponentQuery.query(<%=q(hashInputFieldId)%>)[0];
                            // displayField.setValue(record.get('webdavPath'));
                            var path = record.getPath('text');
                            path = path.replace(/^\/+/, '')
                            displayField.setValue(path);
                        },
                        <%if(input.isWebdavFileUrl()) { %>
                        beforeselect: function(node, record, index, eOpts)
                        {
                            // Do not allow selection of directories
                            if(!record.data.isFile) return false;
                        },
                        <% } %>
                        afterrender: function(component, eOpts)
                        {
                            var displayField = Ext4.ComponentQuery.query(<%=q(hashInputFieldId)%>)[0];
                            var path = displayField.getValue();
                            console.log("Selected path is " + path);
                            this.selectPath('//' + path, 'text', '/', function (success, node) {
                                    // var nodeEl = Ext.get(tree.view.getNode(n));
                                    // nodeEl.scrollIntoView(tree.view.el, false, true);
                                console.log("success is " + success);
                            });
                        }
                    }
                }
                <% } %>
                <% } %>
            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Submit Job',
                cls: 'labkey-button primary',
                handler: function() {
                    var values = form.getForm().getValues();
                    form.submit({
                        url: <%=q(buildURL(CromwellController.SubmitCromwellJobAction.class))%>,
                        method: 'POST',
                        params: values
                    });
                },
                margin: '20 10 0 10'
            },
                {
                    text: 'Cancel',
                    cls: 'labkey-button',
                    handler: function(btn) {
                        window.location = <%=q(form.getReturnURLHelper(PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(getContainer())).toString())%>;
                    }
                }
            ]
        });
    });
</script>