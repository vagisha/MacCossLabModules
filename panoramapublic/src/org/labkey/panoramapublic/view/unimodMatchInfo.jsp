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
    JspView<PanoramaPublicController.UnimodMatchBean> view = (JspView<PanoramaPublicController.UnimodMatchBean>) HttpView.currentView();
    var bean = view.getModelBean();
    var form = bean.getForm();
    var modification = bean.getModification();
    var unimodMatches = bean.getUnimodMatches();
    var returnUrl = form.getReturnURLHelper(getContainer().getStartURL(getUser()));
%>
<labkey:errors/>

<style type="text/css">
    .display-value {
        font-size: 14px;
        margin-top: 10px;
    }
</style>

<div id="unimodMatchDiv"/>

<script type="text/javascript">

    Ext4.onReady(function(){

        var items = [
            {
                xtype: 'displayfield',
                fieldCls: 'display-value',
                fieldLabel: "Name",
                value: <%=q(modification.getName())%>
            },
            {
                xtype: 'displayfield',
                fieldCls: 'display-value',
                fieldLabel: "Formula",
                value: <%=q(modification.getFormula())%>
            },
            <% if (bean.isIsotopicMod()) { %>
            {
                xtype: 'displayfield',
                fieldCls: 'display-value',
                fieldLabel: "Label",
                value: <%=q(bean.getLabels())%>
            },
            <% } %>
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
        ];

        <% for (UnimodModification unimodMod: unimodMatches) { %>
            items.push(createForm(<%=unimodMod.getId()%>,
                    <%=q(unimodMod.getLink().getHtmlString())%>,
                    <%=q(unimodMod.getName())%>,
                    <%=q(unimodMod.getNormalizedFormula())%>,
                    <%=q(unimodMod.getModSitesWithPosition())%>,
                    <%=q(unimodMod.getTerminus())%>));
        <% } %>

        Ext4.create('Ext.panel.Panel', {
            renderTo: "unimodMatchDiv",
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 160,
                width: 500,
                labelStyle: 'background-color: #E0E6EA; padding: 5px;'
            },
            items: items,
            buttonAlign: 'left',
            buttons: [
                {
                    text: 'Cancel',
                    cls: 'labkey-button',
                    style: { marginTop: '20px' },
                    handler: function(btn) {
                        window.location = <%= q(returnUrl) %>;
                    }
                }]
        });
    });
    function createForm(unimodId, unimodLink, name, formula, sites, terminus)
    {
        var form = Ext4.create('Ext.form.Panel', {
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
                    xtype: 'label',
                    text: '--- Modification matches the following Unimod modification ---',
                    style: {'text-align': 'center', 'margin': '10px 0 10px 0'}
                },
                {
                    xtype: 'hidden',
                    name: 'id', // ExperimentAnnotationsId
                    value: <%=form.getId()%>
                },
                {
                    xtype: 'hidden',
                    name: <%=q(ActionURL.Param.returnUrl.name())%>,
                    value: <%=q(returnUrl)%>
                },
                {
                    xtype: 'hidden',
                    name: 'modificationId',
                    value: <%=form.getModificationId()%>
                },
                {
                    xtype: 'hidden',
                    name: 'unimodId',
                    value: unimodId
                },

                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Name",
                    value: '<div>' + name + ', ' + unimodLink + '</div>'
                },
                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Formula",
                    value: formula
                },
                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Sites",
                    value: sites
                },
                {
                    xtype: 'displayfield',
                    fieldCls: 'display-value',
                    fieldLabel: "Terminus",
                    value: terminus
                }
            ],
            buttonAlign: 'left',
            buttons: [
                {
                    text: "Save Match",
                    cls: 'labkey-button primary',
                    handler: function(button) {
                        button.setDisabled(true);
                        form.submit({
                            url: <%=q(urlFor(bean.isIsotopicMod()
                            ? PanoramaPublicController.MatchToUnimodIsotopeAction.class
                            : PanoramaPublicController.MatchToUnimodStructuralAction.class))%>,
                            method: 'POST'
                        });
                    }
                }]
        });
        return form;
    }
</script>
