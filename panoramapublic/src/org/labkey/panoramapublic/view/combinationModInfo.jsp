<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.UnimodModification" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>
<%
    JspView<PanoramaPublicController.CombinationModifiationBean> view = (JspView<PanoramaPublicController.CombinationModifiationBean>) HttpView.currentView();
    var bean = view.getModelBean();
    var form = bean.getForm();
    var modification = bean.getModification();
    var unimodMods = bean.getUnimodModificationList();
    var returnUrl = form.getReturnURLHelper(getContainer().getStartURL(getUser()));
%>
<labkey:errors/>

<style type="text/css">
    .display-value {
        font-size: 14px;
        margin-top: 10px;
    }
</style>

<div id="combinationModInfoForm"/>

<script type="text/javascript">

    Ext4.onReady(function(){

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: "combinationModInfoForm",
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 160,
                width: 500,
                labelStyle: 'background-color: #E0E6EA; padding: 5px;'
            },
            items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                {
                    xtype: 'hidden',
                    name: 'id', // ExperimentAnnotationsId
                    value: <%=form.getId()%>
                },
                {
                    // instead of generateReturnUrlFormField(returnUrl)
                    xtype: 'hidden',
                    name: <%=q(ActionURL.Param.returnUrl.name())%>,
                    value: <%=q(returnUrl)%>
                },
                {
                    xtype: 'hidden',
                    name: 'modificationId',
                    value: <%=form.getModificationId()%>
                },
                <% if(form.getStructuralModInfoId() != null){ %>
                {
                    xtype: 'hidden',
                    name: 'structuralModInfoId', // panoramapublic.ExperimentStructuralModInfo.id
                    value: <%=form.getStructuralModInfoId()%>
                },
                <% } %>
                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Modification Name",
                    value: <%=q(modification.getName())%>
                },
                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Formula",
                    value: <%=q(modification.getFormula())%>
                },
                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Amino Acid(s)",
                    value: <%=q(modification.getAminoAcid())%>
                },
                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Terminus",
                    value: <%=q(modification.getTerminus())%>
                },
                {
                    xtype: 'label',
                    text: 'This modification is a combination of',
                    style: {'text-align': 'center', 'margin': '10px 0 10px 0'}
                },
                {
                    xtype: 'combo',
                    name: 'unimodId1',
                    itemId: 'unimodId1',
                    fieldLabel: "Unimod Modification 1",
                    allowBlank: false,
                    editable : true,
                    queryMode : 'local',
                    value: <%=form.getUnimodId1() != null ? form.getUnimodId1() : null%>,
                    store: [
                        <% for (UnimodModification mod: unimodMods) { %>
                        [ <%= mod.getId() %>, <%= q(mod.getName() + ", " + mod.getNormalizedFormula() + ", Unimod:" + mod.getId()) %> ],
                        <% } %>
                    ]
                },
                {
                    xtype: 'label',
                    text: '--- AND ---',
                    style: {'text-align': 'center', 'margin': '10px 0 10px 0'}
                },
                {
                    xtype: 'combo',
                    name: 'unimodId2',
                    itemId: 'unimodId2',
                    fieldLabel: "Unimod Modification 2",
                    allowBlank: false,
                    editable: true,
                    queryMode : 'local',
                    value: <%=form.getUnimodId2() != null ? form.getUnimodId2() : null%>,
                    store: [
                        <% for (UnimodModification mod: unimodMods) { %>
                        [ <%= mod.getId() %>, <%= q(mod.getName() + ", " + mod.getNormalizedFormula()) %> ],
                        <% } %>
                    ]
                }
            ],
            buttonAlign: 'left',
            buttons: [
                {
                    text: "Save",
                    cls: 'labkey-button primary',
                    handler: function(button) {
                        button.setDisabled(true);
                        form.submit({
                            url: <%=q(urlFor(PanoramaPublicController.DefineCombinationModificationAction.class))%>,
                            method: 'POST'
                        });
                    }
                },
                {
                    text: 'Cancel',
                    cls: 'labkey-button',
                    handler: function(btn) {
                        window.location = <%= q(returnUrl) %>;
                    }
                }]
        });
    });
</script>
