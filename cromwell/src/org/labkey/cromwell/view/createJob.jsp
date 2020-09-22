<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.cromwell.CromwellController" %>
<%@ page import="org.labkey.cromwell.CromwellController.CromwellJobForm" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.action.UrlProvider" %>
<%@ page import="org.labkey.api.data.ContainerFilter" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:errors/>
<%
    CromwellJobForm form = ((JspView<CromwellJobForm>) HttpView.currentView()).getModelBean();
%>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>

<div style="margin-top:15px;" id="newJobForm"></div>

<script type="text/javascript">

    Ext4.onReady(function(){

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: "newJobForm",
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 250,
                width: 800,
                labelStyle: 'background-color: #E0E6EA; padding: 5px;'
            },
            items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                {
                    xtype: 'displayfield',
                    fieldLabel: "Workflow",
                    value: <%=q(form.getWorkflowName())%>
                },
                {
                    xtype:'hidden',
                    name: 'workflowId',
                    value: <%=form.getWorkflowId()%>
                },
                {
                    xtype:'hidden',
                    name: 'workflowName',
                    value: <%=q(form.getWorkflowName())%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Skyline template URL',
                    name: 'skylineTemplateUrl',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getSkylineTemplateUrl())%>,
                    afterBodyEl: '<span style="font-size: 0.9em;">WebDav URL of the Skyline template file (.sky)</span>',
                    msgTarget : 'under'
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Raw files directory URL',
                    name: 'rawFilesFolderUrl',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getRawFilesFolderUrl())%>,
                    afterBodyEl: '<span style="font-size: 0.9em;">WebDav URL of the raw data directory</span>',
                    msgTarget : 'under'
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Raw files extension',
                    name: 'rawFileExtention',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getRawFileExtention())%>,
                    afterBodyEl: '<span style="font-size: 0.9em;">e.g. RAW, mzML, mzXML etc.</span>',
                    msgTarget : 'under'
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Target folder',
                    name: 'targetFolderUrl',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getTargetFolderUrl())%>,
                    afterBodyEl: '<span style="font-size: 0.9em;">Folder where the Skyline document should be uploaded</span>',
                    msgTarget : 'under'
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'API Key',
                    name: 'apiKey',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getApiKey())%>
                }
            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Submit Job',
                cls: 'labkey-button primary',
                handler: function() {
                    var values = form.getForm().getValues();
                    form.submit({
                        url: <%=q(new ActionURL(CromwellController.SubmitCromwellJob.class, getContainer()).getLocalURIString())%>,
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
                        window.location = <%= q(form.getReturnURLHelper(PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(getContainer())).toString())%>;
                    }
                }
            ]
        });
    });
</script>