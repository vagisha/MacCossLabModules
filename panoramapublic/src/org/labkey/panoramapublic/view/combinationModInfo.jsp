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

    var selectedUnimod1;
    var selectedUnimod2;
    var formPanel;

    Ext4.onReady(function(){

        var combo1 = createUnimodCb(1);
        var combo2 = createUnimodCb(2);

        formPanel = Ext4.create('Ext.form.Panel', {
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
                    value: <%=q(UnimodModification.normalizeFormula(modification.getFormula()))%>
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
                combo1,
                {
                    xtype: 'label',
                    text: '--- AND ---',
                    style: {'text-align': 'center', 'margin': '10px 0 10px 0'}
                },
                combo2,
                {
                    xtype: 'component',
                    itemId: 'formulaSum',
                    style: {'text-align': 'left', 'margin': '10px 0 10px 0'}
                }
            ],
            buttonAlign: 'left',
            buttons: [
                {
                    text: "Save",
                    cls: 'labkey-button primary',
                    handler: function(button) {
                        // button.setDisabled(true);
                        formPanel.submit({
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

    function createUnimodCb(cbIdx)
    {
        return Ext4.create('Ext.form.field.ComboBox', {
            xtype: 'combo',
            name: 'unimodId' + cbIdx,
            itemId: 'unimodId' + cbIdx,
            fieldLabel: "Unimod Modification " + cbIdx,
            allowBlank: false,
            editable : true,
            queryMode : 'local',
            displayField: 'displayName',
            valueField: 'unimodId',
            value: cbIdx === 1 ? <%=form.getUnimodId1() != null ? form.getUnimodId1() : null %> :
                                 <%=form.getUnimodId2() != null ? form.getUnimodId2() : null %>,
            store: createStore(),
            labelWidth: 160,
            width: 500,
            labelStyle: 'background-color: #E0E6EA; padding: 5px;',
            listeners: {
                select: function (combo, records, eOpts){
                    const record = records[0];
                    if (cbIdx === 1) selectedUnimod1 = record.data;
                    else selectedUnimod2 = record.data;
                    updateFormulaTotal();
                }
            }
        });
    }
    function updateFormulaTotal()
    {
        var total = <%= qh(modification.getFormula()) %>;
        var text = '';
        text += selectedUnimod1 ? selectedUnimod1["formula"] : "---";
        text += "   +    ";
        text += selectedUnimod2 ? selectedUnimod2["formula"] : "---";
        text += "   =   " + total;
        console.log(text);
        var el = formPanel.getComponent("formulaSum");
        if (el != null)
        {
            el.addCls('alert alert-info');
            el.update(text);
        }
    }
    function createStore()
    {
        return Ext4.create('Ext.data.Store', {
            fields: ['unimodId','displayName', 'formula'],
            data:   [
                <% for(UnimodModification mod: unimodMods){ %>
                {
                    "unimodId":<%=mod.getId()%>,
                    "displayName":<%= q(mod.getName() + ", " + mod.getNormalizedFormula() + ", Unimod:" + mod.getId()) %>,
                    "formula": <%= q(mod.getNormalizedFormula()) %>
                },
                <% } %>
            ]
        });
    }
</script>
