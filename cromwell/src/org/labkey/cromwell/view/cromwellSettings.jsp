<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.cromwell.CromwellController" %>
<%@ page import="org.labkey.cromwell.CromwellController.CromwellSettingsForm" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.admin.AdminUrls" %>
<%@ page import="java.util.Objects" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:errors/>
<%
    CromwellSettingsForm form = ((JspView<CromwellSettingsForm>) HttpView.currentView()).getModelBean();
%>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>

<div style="margin-top:15px;" id="cromwellSettingsForm"></div>

<script type="text/javascript">

    Ext4.onReady(function(){

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: "cromwellSettingsForm",
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
                    xtype: 'textfield',
                    fieldLabel: 'Cromwell server URL',
                    name: 'cromwellServer',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getCromwellServer())%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Cromwell server port',
                    name: 'port',
                    allowBlank: true,
                    width: 650,
                    value: <%=(form.getPort())%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'SCP user name',
                    name: 'scpUser',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getScpUser())%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'SCP key file path',
                    name: 'scpKeyFilePath',
                    allowBlank: false,
                    width: 650,
                    value: <%=q(form.getScpKeyFilePath())%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'SCP port',
                    name: 'scpPort',
                    allowBlank: true,
                    width: 650,
                    value: <%=form.getScpPort()%>
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Allowed projects',
                    name: 'allowedProjectNames',
                    allowBlank: true,
                    width: 650,
                    value: <%=q(form.getAllowedProjectNames())%>
                }
            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Save',
                cls: 'labkey-button primary',
                handler: function() {
                    const values = form.getForm().getValues();
                    form.submit({
                        url: <%=q(new ActionURL(CromwellController.CromwellSettingsAction.class, getContainer()).getLocalURIString())%>,
                        method: 'POST',
                        params: values
                    });
                },
                margin: '20 10 0 10'
            },
                {
                    text: 'Cancel',
                    cls: 'labkey-button',
                    handler: function() {
                        window.location = <%=q((PageFlowUtil.urlProvider(AdminUrls.class)).getAdminConsoleURL().getLocalURIString())%>;
                    }
                }
            ]
        });
    });
</script>