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
    JspView<PanoramaPublicController.SpecLibInfoBean> view = (JspView<PanoramaPublicController.SpecLibInfoBean>) HttpView.currentView();
    PanoramaPublicController.SpecLibInfoBean bean = view.getModelBean();
    PanoramaPublicController.EditSpecLibInfoForm form = bean.getForm();
    SpecLibKey libKey = bean.getLibKey();
    boolean supportedLibrary = libKey.getType().isSupported();
%>

<labkey:errors/>

<div>
    <em>Spectral Library</em>: <%=h(bean.getLibKey().getName())%>
    <br/>
    <em>File</em>: <%=h(bean.getLibKey().getFileNameHint())%>
</
</div>
<div id="editSpecLibInfoForm"/>

<script type="text/javascript">

    function clearTextFieldsAndDisable(ownerCt, fields) {
        for (var i = 0; i < fields.length; i += 1) {
            var tf = ownerCt.getComponent(fields[i]);
            if (tf) {
                tf.setValue('');
                // tf.setDisabled(true);
            }
        }
    }

    function publicLibraryCheckboxChanged(cb, checked) {
        var tf = cb.ownerCt.getComponent('sourceUrl');
        if (tf) tf.setDisabled(!checked);
        if (checked) {
            clearTextFieldsAndDisable(cb.ownerCt, ['sourceAccession', 'sourceUsername', 'sourcePassword']);
            var cbSrcType = cb.ownerCt.getComponent('sourceType');
            if (cbSrcType) {
                cbSrcType.setRawValue('');
                cbSrcType.setValue('');
                cbSrcType.clearValue();
            }

        }
    }

    function sourceTypeComboboxChanged(cb, newValue, oldValue) {
        var tf_accession = cb.ownerCt.getComponent('sourceAccession');
        var tf_username = cb.ownerCt.getComponent('sourceUsername');
        var tf_password = cb.ownerCt.getComponent('sourcePassword');
        var enable = newValue === <%=q(SpecLibSourceType.OTHER_REPOSITORY.name())%>;
        if (tf_accession) tf_accession.setDisabled(!enable);
        if (tf_username) tf_username.setDisabled(!enable);
        if (tf_password) tf_password.setDisabled(!enable);
        console.log("New value is " + newValue);

    }

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
                    name: 'id',
                    value: <%=form.getId()%>
                },
                {
                    xtype: 'hidden',
                    name: 'specLibId',
                    value: <%=form.getSpecLibId()%>
                },
                {
                    xtype: 'hidden',
                    name: 'specLibInfoId',
                    value: <%=form.getSpecLibInfoId()%>
                },
                {
                    xtype: 'checkbox',
                    name: 'publicLibrary',
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
                    hidden: <%=!supportedLibrary%>,
                    value: <%=form.getSourceType() != null ? q(form.getSourceType()) : null%>,
                    afterBodyEl: '<span style="font-size: 0.9em;">Location of the source files used to build the library.</span>',
                    msgTarget : 'under',
                    store: [
                        <% for (SpecLibSourceType sourceType: SpecLibSourceType.values()) { %>
                        [ <%= q(sourceType.name()) %>, <%= q(sourceType.getDescription()) %> ],
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
                    value: <%=q(form.getSourceAccession())%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourceUsername',
                    itemId: 'sourceUsername',
                    fieldLabel: "User Name",
                    disabled: <%=!SpecLibSourceType.OTHER_REPOSITORY.name().equals(form.getSourceType())%>,
                    hidden: <%=!supportedLibrary%>,
                    value: <%=q(form.getSourceUsername())%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourcePassword',
                    itemId: 'sourcePassword',
                    fieldLabel: "Password",
                    disabled: <%=!SpecLibSourceType.OTHER_REPOSITORY.name().equals(form.getSourceType())%>,
                    hidden: <%=!supportedLibrary%>,
                    value: <%=q(form.getSourcePassword())%>
                },
                {
                    xtype: 'combobox',
                    name: 'dependencyType',
                    fieldLabel: "Dependency Type",
                    allowBlank: false,
                    value: <%=form.getDependencyType() != null ? q(form.getDependencyType()) : null%>,
                    store: [
                        <% for (SpecLibDependencyType sourceType: SpecLibDependencyType.values()) { %>
                        [ <%= q(sourceType.name()) %>, <%= q(sourceType.getDescription()) %> ],
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
                    href: <%=q(getContainer().getStartURL(getUser()))%>
                }]
        });
    });
</script>