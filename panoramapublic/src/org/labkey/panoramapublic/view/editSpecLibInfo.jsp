<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.panoramapublic.model.speclib.SpecLibSourceType" %>
<%@ page import="org.labkey.panoramapublic.model.speclib.SpecLibDependencyType" %>
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ page import="org.labkey.panoramapublic.model.speclib.SpecLibKey" %>
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
    JspView<PanoramaPublicController.EditSpecLibInfoForm> view = (JspView<PanoramaPublicController.EditSpecLibInfoForm>) HttpView.currentView();
    PanoramaPublicController.EditSpecLibInfoForm form = view.getModelBean();
    // PanoramaPublicController.EditSpecLibInfoForm form = bean.getForm();
    SpecLibKey libKey = SpecLibKey.from(form.getSpecLibKey());
    boolean supportedLibrary = libKey.getType().isSupported();
%>

<div style="margin-bottom:10px;">
    <b>Spectral Library</b>: <%=h(libKey.getName())%>
    <br/>
    <b>File</b>: <%=h(libKey.getFileNameHint())%>
</div>
<labkey:errors/>
<div id="editSpecLibInfoForm"/>

<script type="text/javascript">

    Ext4.onReady(function(){

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: "editSpecLibInfoForm",
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 150,
                width: 600,
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
                    name: 'specLibId', // targetedms.spectrumlibrary.id
                    value: <%=form.getSpecLibId()%>
                },
                    <%if(form.getSpecLibInfoId() != null){%>
                {
                    xtype: 'hidden',
                    name: 'specLibInfoId', // panoramapublic.speclibinfo.id
                    value: <%=form.getSpecLibInfoId()%>
                },
                    <%}%>
                {
                    xtype: 'hidden',
                    name: 'specLibKey',
                    value: <%=q(form.getSpecLibKey())%>
                },
                {
                    xtype: 'checkbox',
                    name: 'publicLibrary',
                    itemId: 'publicLibrary',
                    fieldLabel: "Public Library",
                    checked: <%=form.isPublicLibrary()%>,
                    boxLabel: 'Check this box if this is a publicly available library.',
                    listeners: {
                        change: function(cb, checked) {
                            publicLibraryCheckboxChanged(cb, checked);
                        }
                    }
                },
                {
                    xtype: 'textfield',
                    name: 'sourceUrl',
                    itemId: 'sourceUrl',
                    fieldLabel: "Library URL",
                    disabled: <%=!form.isPublicLibrary()%>,
                    value: <%=q(form.getSourceUrl())%>
                },
                {
                    xtype: 'combobox',
                    name: 'sourceType',
                    itemId: 'sourceType',
                    fieldLabel: "Library Source Files",
                    allowBlank: true,
                    editable: false,
                    hidden: <%=!supportedLibrary%>,
                    value: <%=form.getSourceType() != null ? q(form.getSourceType()) : null%>,
                    afterBodyEl: '<span style="font-size: 0.9em;">Location of the source files used to build the library if it is not a public library.</span>',
                    msgTarget : 'under',
                    store: [
                        <% for (SpecLibSourceType sourceType: SpecLibSourceType.values()) { %>
                        [ <%= q(sourceType.name()) %>, <%= q(sourceType.getLabel()) %> ],
                        <% } %>
                    ],
                    listeners: {
                        change: function(cb, newValue, oldValue, eOpts ) {
                            sourceTypeComboboxChanged(cb, newValue, oldValue);
                        }
                    }
                },

                {
                    xtype: 'textfield',
                    name: 'sourceAccession',
                    itemId: 'sourceAccession',
                    fieldLabel: "Accession",
                    disabled: <%=!SpecLibSourceType.OTHER_REPOSITORY.name().equals(form.getSourceType())%>,
                    hidden: <%=!supportedLibrary%>,
                    value: <%=q(form.getSourceAccession())%>,
                    afterLabelTextTpl: <%=q(helpPopup("Repository Accession", "Accession or identifier of the data in the repository. " +
                     "This can be a ProteomeXchange ID (e.g. PXD000001), a MassIVE identifier (e.g. MSV000000001)" +
                      " or a PeptideAtlas identifier (e.g. PASS00001)."))%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourceUsername',
                    itemId: 'sourceUsername',
                    fieldLabel: "User Name",
                    disabled: <%=!SpecLibSourceType.OTHER_REPOSITORY.name().equals(form.getSourceType())%>,
                    hidden: <%=!supportedLibrary%>,
                    value: <%=q(form.getSourceUsername())%>,
                    afterLabelTextTpl: <%=q(helpPopup("Repository Username", "Username to access the data in the repository if the data is private."))%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourcePassword',
                    itemId: 'sourcePassword',
                    fieldLabel: "Password",
                    disabled: <%=!SpecLibSourceType.OTHER_REPOSITORY.name().equals(form.getSourceType())%>,
                    hidden: <%=!supportedLibrary%>,
                    value: "", // Don't display the password; make the user enter it every time they edit
                    afterLabelTextTpl: <%=q(helpPopup("Repository Password", "Password to access the data in the repository if the data is private."))%>
                },
                {
                    xtype: 'combobox',
                    name: 'dependencyType',
                    fieldLabel: "Dependency Type",
                    allowBlank: true,
                    value: <%=form.getDependencyType() != null ? q(form.getDependencyType()) : null%>,
                    store: [
                        <% for (SpecLibDependencyType sourceType: SpecLibDependencyType.values()) { %>
                        [ <%= q(sourceType.name()) %>, <%= q(sourceType.getLabel()) %> ],
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
                            url: <%=q(urlFor(PanoramaPublicController.EditSpecLibInfoAction.class))%>,
                            method: 'POST'
                        });
                    }
                },
                {
                    text: 'Cancel',
                    cls: 'labkey-button',
                    hrefTarget: '_self',
                    href: <%=q(PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), getContainer()))%>
                }]
        });
    });

    function toggleTextFields(ownerCt, disable, fields) {
        for (var i = 0; i < fields.length; i += 1) {
            var tf = ownerCt.getComponent(fields[i]);
            if (tf) {
                if (disable) tf.setValue('');
                tf.setDisabled(disable);
            }
        }
    }

    function publicLibraryCheckboxChanged(cb, checked) {
        toggleTextFields(cb.ownerCt, !checked, ['sourceUrl']);
        if (checked) {
            var cbSrcType = cb.ownerCt.getComponent('sourceType');
            if (cbSrcType) cbSrcType.clearValue();
        }
    }

    function sourceTypeComboboxChanged(cb, newValue, oldValue) {
        var enable = newValue === <%=q(SpecLibSourceType.OTHER_REPOSITORY.name())%>;
        toggleTextFields(cb.ownerCt, !enable, ['sourceAccession', 'sourceUsername', 'sourcePassword']);
        // console.log("New value of source type is " + newValue);
        if (newValue && newValue !== '') {
            publicLibCb = cb.ownerCt.getComponent('publicLibrary');
            if (publicLibCb) publicLibCb.setValue(false);
        }
    }
</script>