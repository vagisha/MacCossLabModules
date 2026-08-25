<%
/*
 * Copyright (c) 2017-2026 LabKey Corporation
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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.skylinetoolsstore.SkylineToolsStoreController" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ page import="org.labkey.api.util.SafeToRender" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("skylinetoolsstore/js/functions.js");
        dependencies.add("skylinetoolsstore/css/toolstore.css");
    }
%>
<%
    JspView<SkylineToolsStoreController.SetOwnersForm> me =
            (JspView<SkylineToolsStoreController.SetOwnersForm>) HttpView.currentView();
    SkylineToolsStoreController.SetOwnersForm form = me.getModelBean();

    final String toolOwners = StringUtils.trimToEmpty(form.getToolOwners());
    final String returnUrl = form.getReturnUrl();

    final boolean admin = getUser().hasSiteAdminPermission();
    final SafeToRender autocompleteUsers = admin ? SkylineToolsStoreController.getUsersForAutocomplete() : HtmlString.unsafe("\"\"");
    pageContext.setAttribute("autocompleteUsers", autocompleteUsers);
%>

<labkey:errors/>

<labkey:form action="<%= urlFor(SkylineToolsStoreController.SetOwnersAction.class) %>" enctype="multipart/form-data" method="post">
    <p>
        <label for="toolOwners">Tool owners </label><br />
        <%-- Rendered here rather than set by the script below. The script needs jQuery from a CDN,
             and where that does not load the box came up empty. Submitting it strips every owner
             from every one of the tool's version folders. --%>
        <input style="width: 400px; max-width: 100%;" type="text" id="toolOwners" name="toolOwners"
               value="<%= h(toolOwners) %>" /><br /><br />
        <br />
<% if (returnUrl != null) { %>
        <input type="hidden" name="returnUrl" value="<%= h(returnUrl) %>" />
<% } %>
        <input type="hidden" name="toolId" value="<%= form.getToolId() %>" />
        <input type="submit" value="Update Tool Owners" />
    </p>
</labkey:form>

<br />
<%= PageFlowUtil.generateBackButton() %>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    var ownersTxt = $("#toolOwners");
    ownersTxt.focus();

    autocomplete(ownersTxt, ${autocompleteUsers});
</script>
