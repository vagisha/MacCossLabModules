<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.UnimodModification" %>
<%@ page import="org.labkey.api.util.StringUtilsLabKey" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.Formula" %>
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
    var returnUrl = form.getReturnURLHelper(PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), getContainer()));

    var modMatchesTxt = unimodMatches.size() == 0 ?
            "No Unimod matches were found for the modification." :
            "The modification matches " + StringUtilsLabKey.pluralize(unimodMatches.size(), "Unimod modification") +
                    ". To view the Unimod definition click the link next to the Unimod name. " +
                    " Click the \"Save Match\" button to associate the Unimod Id with the modification.";
    var defineComboModButton = button("Combination Modification")
            .href(new ActionURL(PanoramaPublicController.DefineCombinationModificationAction.class, getContainer())
                    .addParameter("id", form.getId()).addParameter("modificationId", form.getModificationId())
            .addReturnURL(form.getReturnActionURL(PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), getContainer())))).build();

%>
<labkey:errors/>

<style type="text/css">
    .display-value {
        font-size: 14px;
        margin-top: 10px;
    }
</style>

<div id="cancelButtonDiv"/>
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
                value: <%=q(Formula.normalizeFormula(modification.getFormula()))%>
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
            {
                xtype: 'component',
                cls: <%=unimodMatches.size() == 0 ? qh("labkey-error alert-warning") : qh("alert")%>,
                html: '<div>' + <%=qh(modMatchesTxt)%> + '</div>'
            },
        ];

        <% for (int i = 0; i < unimodMatches.size(); i++) {
            UnimodModification unimodMatch = unimodMatches.get(i);
        %>
            items.push(createForm(<%=unimodMatches.size() > 1 ? i : -1%>,
                    <%=unimodMatch.getId()%>,
                    <%=q(unimodMatch.getLink().getHtmlString())%>,
                    <%=q(unimodMatch.getName())%>,
                    <%=q(unimodMatch.getNormalizedFormula())%>,
                    <%=q(unimodMatch.getModSitesWithPosition())%>,
                    <%=q(unimodMatch.getTerminus())%>));
        <% } %>

        Ext4.create('Ext.panel.Panel', {
            renderTo: "unimodMatchDiv",
            border: false,
            frame: false,
            defaults: {
                labelWidth: 160,
                width: 800,
                labelStyle: 'background-color: #E0E6EA; padding: 5px;'
            },
            items: items
        });


        Ext4.create('Ext.panel.Panel', {
            renderTo: "cancelButtonDiv",
            border: false,
            frame: false,
            defaults: {
                width: 800
            },
            <%if (!bean.isIsotopicMod()) { %> // Combination modifications can only de defined for structural modifications.
            items: [
                {
                    xtype: 'component',
                    cls: 'alert-warning alert',
                    style: 'margin-top:10px;',
                    html: '<div>Define a custom ' + '<%=defineComboModButton%>'
                            + ' if this modification is a combination of two modifications.'
                            + '</div>'
                }
            ],
            <% } %>
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
    function createForm(index, unimodId, unimodLink, name, formula, sites, terminus)
    {
        var form = Ext4.create('Ext.form.Panel', {
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 160,
                width: 500,
                labelStyle: 'background-color: #C0C6CA; padding: 5px;'
            },
            items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                {
                    xtype: 'label',
                    text: '---------------- Unimod Match ' + (index > -1 ? index + 1 : '') + ' ----------------',
                    style: {'text-align': 'left', 'margin': '10px 0 10px 0'}
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
