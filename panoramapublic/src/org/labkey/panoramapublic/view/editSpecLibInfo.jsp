<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.panoramapublic.model.SpecLibInfo" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
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
%>

<labkey:errors/>

<div>

</div>
<div id="editSpecLibInfoForm"></div>

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
                    name: 'id',
                    value: <%=form.getId()%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourceType',
                    fieldLabel: "Source Type",
                    value: form.getSourceType()
                },
                {
                    xtype: 'textfield',
                    name: 'sourceUrl',
                    fieldLabel: "Source URL",
                    value: <%=q(form.getSourceUrl())%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourceAccession',
                    fieldLabel: "Source Accession",
                    value: <%=q(form.getSourceAccession())%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourceUsername',
                    fieldLabel: "Source User Name",
                    value: <%=q(form.getSourceUsername())%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourcePassword',
                    fieldLabel: "Source Password",
                    value: <%=q(form.getSourcePassword())%>
                },
                {
                    xtype: 'textfield',
                    name: 'sourcePxid',
                    fieldLabel: "Source PXD Accession",
                    value: <%=q(form.getSourcePxid())%>
                },
                {
                    xtype: 'textfield',
                    name: 'dependencyType',
                    fieldLabel: "Dependency Type",
                    value: <%=form.getDependencyType()%>
                }
            ],
            buttonAlign: 'left',
            buttons: [{
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



<script type="text/javascript">
    let sourceSelect = document.getElementById("sourceType");
    let enabledBg = window.getComputedStyle(document.getElementById("sourceUrl"), null).getPropertyValue("background-color");
    let enabledColor = window.getComputedStyle(document.getElementById("sourceUrl"), null).getPropertyValue("color");
    let disabledBg = "#ddd";
    let disabledColor = "#aaa";
    let toggleInput = function(el, on) {
        el.disabled = !on;
        el.style.backgroundColor = on ? enabledBg : disabledBg;
        el.style.color = on ? enabledColor : disabledColor;
    };
    sourceSelect.addEventListener("change", () => {
        let url = document.getElementById("sourceUrl");
        let pxid = document.getElementById("sourcePxid");
        let accession = document.getElementById("sourceAccession");
        let username = document.getElementById("sourceUsername");
        let password = document.getElementById("sourcePassword");
        switch (parseInt(sourceSelect.value)) {
            case <%=SpecLibInfo.SourceType.SKYLINE%>:
                toggleInput(url, false);
                toggleInput(pxid, false);
                toggleInput(accession, false);
                toggleInput(username, false);
                toggleInput(password, false);
                break;
            case <%=SpecLibInfo.SourceType.PUBLIC_LIBRARY%>:
                toggleInput(url, true);
                toggleInput(pxid, false);
                toggleInput(accession, false);
                toggleInput(username, false);
                toggleInput(password, false);
                break;
            case <%=SpecLibInfo.SourceType.PX_REPOSITORY%>:
                toggleInput(url, false);
                toggleInput(pxid, true);
                toggleInput(accession, true);
                toggleInput(username, true);
                toggleInput(password, true);
                break;
        }
    });
    sourceSelect.dispatchEvent(new Event("change"));
</script>