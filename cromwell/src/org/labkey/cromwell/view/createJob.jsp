<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.cromwell.CromwellController" %>
<%@ page import="org.labkey.cromwell.CromwellController.CromwellJobForm" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
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
<% if(form.getJobId() != null) { %>
<div style="font-weight: bold; color: red;">
    NOTE: Submitting a job with the same input parameters as a previous job will overwrite any output files that were created by the previous job.
</div>
<% } %>

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
                <%for (CromwellController.CromwellInput input: form.getInputsArray()) { %>
                    <%=input.getExtInputField()%>,
                 <% } %>

            ],
            buttonAlign: 'left',
            buttons: [{
                text: 'Submit Job',
                cls: 'labkey-button primary',
                handler: function() {
                    var values = form.getForm().getValues();
                    form.submit({
                        url: <%=q(new ActionURL(CromwellController.SubmitCromwellJobAction.class, getContainer()).getLocalURIString())%>,
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