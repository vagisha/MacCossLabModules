<%
    /*
     * Copyright (c) 2008-2019 LabKey Corporation
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>
<%
    JspView<PanoramaPublicController.CatalogEntryBean> view = (JspView<PanoramaPublicController.CatalogEntryBean>) HttpView.currentView();
    var bean = view.getModelBean();
    var form = bean.getForm();

    var attachedFile = bean.getImageFileName();
    var attachmentUrl = bean.getImageUrl();

    ActionURL cancelUrl = PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), form.getContainer());

%>
<labkey:errors/>

<div style="margin-top:15px;" id="catalogEntryForm"></div>
<script type="text/javascript">

    Ext4.onReady(function(){

        let attachmentHtml = "";
        <% if (attachedFile != null) {%> attachmentHtml += "Attached Image: " + <%=qh(attachedFile)%>; <%}%>
        <% if (attachmentUrl != null) {%> attachmentHtml += <%=q(link("", attachmentUrl).iconCls("fa fa-download").style("margin-left:5px;").href(attachmentUrl))%> <%}%>

        var form = Ext4.create('Ext.form.Panel', {
            renderTo: "catalogEntryForm",
            standardSubmit: true,
            border: false,
            frame: false,
            defaults: {
                labelWidth: 125,
                width: 800,
                labelStyle: 'background-color: #E0E6EA; padding: 5px;'
            },
            items: [
                { xtype: 'hidden', name: 'X-LABKEY-CSRF', value: LABKEY.CSRF },
                { xtype: 'hidden', name: 'returnUrl', value: getReturnUrl() },
                {
                    xtype: 'textarea',
                    fieldLabel: 'Description',
                    name: 'datasetDescription',
                    width: 650,
                    value: <%=q(form.getDatasetDescription())%>,
                    msgTarget: 'side',
                    afterBodyEl: 'Maximum <b>75</b> words'
                },
                {
                    xtype: 'filefield',
                    name: 'imageFile',
                    fieldLabel: 'Image',
                    width: 650,
                    buttonText: 'Browse',
                    value: <%=q(form.getImageFile())%>,
                    msgTarget: 'side',
                    afterBodyEl: 'Image should be <b>600 x 400</b> pixels for best results. Maximum attachment size: <b>5MB</b>'
                }
            ],
            buttonAlign: 'left',
            buttons: [
                {
                    text: 'Submit',
                    cls: 'labkey-button primary',
                    handler: function() {
                        var values = form.getForm().getValues();
                        form.submit({
                            url: <%=q(getActionURL())%>,
                            method: 'POST',
                            params: values
                        });
                    },
                    margin: '30 10 0 0'
                },
                {
                    text: 'Cancel',
                    cls: 'labkey-button',
                    hrefTarget: '_self',
                    href: <%=q(cancelUrl)%>
                }
            ]
        });

        function getReturnUrl()
        {
            var returnUrl = LABKEY.ActionURL.getReturnUrl();
            return (returnUrl === undefined ? "" : returnUrl);
        }
    });
</script>