<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.UnimodModification" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.ChemElement" %>
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
    JspView<PanoramaPublicController.CombinationModifiationBean> view = (JspView<PanoramaPublicController.CombinationModifiationBean>) HttpView.currentView();
    var bean = view.getModelBean();
    var form = bean.getForm();
    var modification = bean.getModification();
    var modFormula = bean.getModFormula();
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

    var modFormula;
    var selectedUnimod1;
    var selectedUnimod2;
    var formPanel;

    Ext4.onReady(function(){

        modFormula = new Formula();
        <% for (var entry: modFormula.getElementCounts().entrySet()) { %>
        modFormula.addElement(<%=q(entry.getKey().getSymbol())%>, <%=entry.getValue()%>);
        <% } %>

        var combo1 = createUnimodCb(1);
        var combo2 = createUnimodCb(2);

        formPanel = Ext4.create('Ext.form.Panel', {
            renderTo: "combinationModInfoForm",
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 160,
                width: 800,
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
                    value: <%=q(Formula.normalizeFormula(modification.getFormula()))%>
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
                    xtype: 'component',
                    itemId: 'formulaDiff',
                    style: {'text-align': 'left', 'margin': '10px 0 10px 0'}
                },
                combo1,
                {
                    xtype: 'label',
                    text: '--- AND ---',
                    style: {'text-align': 'center', 'margin': '10px 0 10px 0'}
                },
                combo2
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

    class Formula {
        constructor(){ this.elementCounts = {}; }
        addElement(element, count) {
            var currentCount = this.elementCounts[element];
            if (currentCount) {
                count = currentCount + count;
            }
            this.elementCounts[element] = count;
        }
        addFormula(otherFormula) {
            const newFormula = new Formula();
            for (const elem in this.elementCounts) {
                newFormula.addElement(elem, this.elementCounts[elem]);
            }
            for (const elem in otherFormula.elementCounts) {
                newFormula.addElement(elem, otherFormula.elementCounts[elem]);
            }
            return newFormula;
        }
        subtractFormula(otherFormula) {
            const newFormula = new Formula();
            for (const elem in this.elementCounts) {
                newFormula.addElement(elem, this.elementCounts[elem]);
            }
            for (const elem in otherFormula.elementCounts) {
                newFormula.addElement(elem, otherFormula.elementCounts[elem] * -1);
            }
            return newFormula;
        }
        isEmpty() {
            for (const elem in this.elementCounts) {
                if (this.elementCounts[elem] != 0) return false;
            }
            return true;
        }
        getFormula() {
            let posForm = '';
            let negForm = '';
            for (const elem in this.elementCounts) {
                // console.log(elem + " -> " + this.elementCounts[elem]);
                var cnt = this.elementCounts[elem];
                if (cnt > 0) {
                    posForm += elem + (cnt > 1 ? cnt : '');
                }
                else if (cnt < 0) {
                    cnt = cnt * -1;
                    negForm += elem + (cnt > 1 ? cnt : '');
                }
            }
            const sep = posForm && negForm ? ' - ' : negForm ? '-' : '';
            return posForm + sep + negForm;
        }
    }

    function createUnimodCb(cbIdx) {

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
            width: 800,
            labelStyle: 'background-color: #E0E6EA; padding: 5px;',
            listeners: {
                select: function (combo, records, eOpts){
                    const record = records[0];
                    if (cbIdx === 1) selectedUnimod1 = record.data;
                    else selectedUnimod2 = record.data;
                    updateFormulaDiff();
                }
            }
        });
    }

    const alertCls = 'alert';
    const alertInfoCls = 'alert-info';
    const alertWarnCls = 'alert-warning';

    function updateFormulaDiff() {

        let totalFormula = new Formula();
        totalFormula = addUnimod(totalFormula, selectedUnimod1);
        totalFormula = addUnimod(totalFormula, selectedUnimod2);
        const diffFormula = modFormula.subtractFormula(totalFormula);
        const formulaBalanced = diffFormula.isEmpty();

        var total = '(' + <%= qh(modification.getName()) %> + ') ' + <%= qh(modFormula.getFormula()) %>;
        var html = total;
        html += selectedUnimod1 ? '<span style="margin: 0 10px 0 10px;font-weight:bold;">-</span>' + selectedUnimod1["formula"] /*+ ' (' + selectedUnimod1['name'] + ')'*/ : "";
        html += selectedUnimod2 ? '<span style="margin: 0 10px 0 10px;font-weight:bold;">-</span>' + selectedUnimod2["formula"] /*+ ' (' + selectedUnimod2['name'] + ')'*/ : "";
        html += '<span style="margin: 0 10px 0 10px;font-weight:bold;">=</span>' + (formulaBalanced ? '<span style="color:green;" class="fa fa-check-circle"></span>'
                : diffFormula.getFormula() + '<span style="color:red;margin-left:10px;" class="fa fa-times-circle"></span>');

        var el = formPanel.getComponent("formulaDiff");
        if (el != null)
        {
            if (!el.hasCls(alertCls)) el.addCls(alertCls);

            if (formulaBalanced) {
                el.removeCls(alertWarnCls);
                el.addCls(alertInfoCls);
            }
            else {
                el.removeCls(alertInfoCls);
                el.addCls(alertWarnCls);
            }
            el.update(html);
        }
    }

    function addUnimod(formula, unimodRecord) {
        if (unimodRecord) {
            var composition = unimodRecord['composition'];
            for (const el in composition) {
                formula.addElement(el, composition[el]);
            }
        }
        return formula;
    }

    function createStore() {
        return Ext4.create('Ext.data.Store', {
            fields: ['unimodId', 'name', 'displayName', 'formula', 'composition'],
            data:   [
                <% for(UnimodModification mod: unimodMods){ %>
                {
                    "unimodId":<%=mod.getId()%>,
                    "name":<%=q(mod.getName())%>,
                    "displayName":<%= q(mod.getName() + ", " + mod.getNormalizedFormula() + ", Unimod:" + mod.getId()) %>,
                    "formula": <%= q(mod.getNormalizedFormula()) %>,
                    "composition": {
                        <% for (ChemElement el: mod.getFormula().getElementCounts().keySet()) { %>
                            <%=q(el.getSymbol())%>:  <%=mod.getFormula().getElementCounts().get(el)%> ,
                        <% } %>
                        }
                },
                <% } %>
            ]
        });
    }
</script>
