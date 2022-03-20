<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.UnimodModification" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.ChemElement" %>
<%@ page import="org.labkey.panoramapublic.proteomexchange.Formula" %>
<%@ page import="java.util.Map" %>
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
    JspView<PanoramaPublicController.CombinationModificationBean> view = (JspView<PanoramaPublicController.CombinationModificationBean>) HttpView.currentView();
    var bean = view.getModelBean();
    var form = bean.getForm();
    var modification = bean.getModification();
    var modFormula = bean.getModFormula();
    var unimodMods = bean.getUnimodModificationList();
    var returnUrl = form.getReturnURLHelper(PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), getContainer()));
%>
<labkey:errors/>

<style>
    .display-value {
        font-size: 14px;
        margin-top: 10px;
    }
</style>

<div id="combinationModInfoForm"/>

<script type="text/javascript">

    let modFormula = undefined;
    let selectedUnimod1 = undefined;
    let selectedUnimod2 = undefined;
    const elementOrder = {};
    let formPanel, combo1, combo2;

    Ext4.onReady(function(){

        modFormula = new Formula();
        <% for (var entry: modFormula.getElementCounts().entrySet()) { %>
        modFormula.addElement(<%=q(entry.getKey().getSymbol())%>, <%=entry.getValue()%>);
        <% } %>

        let idx = 0;
        <% for (var el: bean.getElementOrder()) { %>
            elementOrder[<%=q(el)%>] = idx++;
        <% } %>

        const combo1 = createUnimodCb(1);
        const combo2 = createUnimodCb(2);

        <% if (form.getUnimodId1() != null) { %>
            const record1 = combo1.getStore().getById(<%=form.getUnimodId1()%>);
            if (record1) selectedUnimod1 = record1.data;
        <% } %>

        <% if (form.getUnimodId2() != null) { %>
            const record2 = combo2.getStore().getById(<%=form.getUnimodId2()%>);
            if (record2) selectedUnimod2 = record2.data;
        <% } %>

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
                    width: 600,
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
                    width: 600,
                    style: {'text-align': 'center', 'margin': '10px 0 10px 0'}
                },
                combo2
            ],
            buttonAlign: 'left',
            buttons: [
                {
                    text: "Save",
                    cls: 'labkey-button primary',
                    handler: function() {
                        formPanel.submit({
                            url: <%=q(urlFor(PanoramaPublicController.DefineCombinationModificationAction.class))%>,
                            method: 'POST'
                        });
                    }
                },
                {text: 'Cancel',
                    cls: 'labkey-button',
                    handler: function() {
                        window.location = <%= q(form.getCancelActionURL()) %>;
                    }
                }]
        });

        if (selectedUnimod1 && selectedUnimod2) {
            updateFormulaDiff();
        }
    });

    function Formula() {

        this.elementCounts = {};

        this.subtractElement = function _subtractElement(el, count) {
            this.addElement(el, count * -1);
        }
        this.addElement = function _addElement(element, count) {
            const currentCount = this.elementCounts[element];
            if (currentCount) {
                count = currentCount + count;
            }
            this.elementCounts[element] = count;
        }
        this.subtractFormula = function _subtractFormula(otherFormula) {
            const newFormula = new Formula();
            Object.keys(this.elementCounts).forEach(el => newFormula.addElement(el, this.elementCounts[el]));
            Object.keys(otherFormula.elementCounts).forEach(el => newFormula.subtractElement(el, otherFormula.elementCounts[el]));

            return newFormula;
        }
        this.isEmpty = function _isEmpty() {
            return !Object.keys(this.elementCounts).find(el => this.elementCounts[el] !== 0);
        }
        this.getFormula = function _getFormula() {
            let posForm = '';
            let negForm = '';

            const sortedKeys = Object.keys(this.elementCounts).sort((a,b) => elementOrder[a] - elementOrder[b]);
            for (const elem of sortedKeys) {
                let cnt = this.elementCounts[elem];
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
            forceSelection: true, // restrict the selected value to one of the values in the list
            queryMode : 'local',
            anyMatch: true, // allow match at any position in the valueField's value
            displayField: 'displayName',
            valueField: 'unimodId',
            value: cbIdx === 1 ? <%=form.getUnimodId1() != null ? form.getUnimodId1() : null %> :
                                 <%=form.getUnimodId2() != null ? form.getUnimodId2() : null %>,
            store: createStore(),
            labelWidth: 160,
            width: 600,
            labelStyle: 'background-color: #E0E6EA; padding: 5px;',
            listeners: {
                select: function (combo, records){
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

        let html = '';
        html += selectedUnimod1 ? selectedUnimod1["formula"] : getSpan('---');
        html += getSpan('+');
        html += selectedUnimod2 ? selectedUnimod2["formula"] : getSpan('---');
        html += getSpan('=');
        html += totalFormula.getFormula();
        if (formulaBalanced) {
            html += '<span style="color:green;margin-left:10px;" class="fa fa-check-circle"></span>';
        }
        else {
            html += getSpan(' (Difference: ' + diffFormula.getFormula() + ')');
            html += '<span style="color:red;" class="fa fa-times-circle"></span>';
        }

        const el = formPanel.getComponent("formulaDiff");
        if (el != null)
        {
            if (!el.hasCls(alertCls)) el.addCls(alertCls);

            el.removeCls(formulaBalanced ? alertWarnCls : alertInfoCls);
            el.addCls(formulaBalanced ? alertInfoCls : alertWarnCls);
            el.update(html);
        }
    }

    function getSpan(str) {
        return '<span style="margin: 0 10px 0 10px;font-weight:bold;">' + str + '</span>';
    }

    function addUnimod(formula, unimodRecord) {
        if (unimodRecord) {
            const composition = unimodRecord['composition'];
            Object.keys(composition).forEach(el => formula.addElement(el, composition[el]));
        }
        return formula;
    }

    function createStore() {
        return Ext4.create('Ext.data.Store', {
            fields: ['id', 'unimodId', 'name', 'displayName', 'formula', 'composition'],
            data:   [
                <% for(UnimodModification mod: unimodMods){ %>
                {
                    "id":<%=mod.getId()%>,
                    "unimodId":<%=mod.getId()%>,
                    "name":<%=q(mod.getName())%>,
                    "displayName":<%= q(mod.getName() + ", " + mod.getNormalizedFormula() + ", Unimod:" + mod.getId()) %>,
                    "formula": <%= q(mod.getNormalizedFormula()) %>,
                    "composition": {
                        <% for (Map.Entry<ChemElement, Integer> entry: mod.getFormula().getElementCounts().entrySet()) { %>
                            <%=q(entry.getKey().getSymbol())%>:  <%=entry.getValue()%> ,
                        <% } %>
                        }
                },
                <% } %>
            ]
        });
    }
</script>
