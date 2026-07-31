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
<%@ page import="org.labkey.api.security.permissions.AdminPermission" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ page import="org.labkey.api.util.SafeToRender" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("skylinetoolsstore/js/functions.js");
    }
%>
<%
    JspView<SkylineToolsStoreController.SetOwnersForm> me =
            (JspView<SkylineToolsStoreController.SetOwnersForm>) HttpView.currentView();
    SkylineToolsStoreController.SetOwnersForm form = me.getModelBean();

    final String contextPath = AppProps.getInstance().getContextPath();
    final String cssDir = contextPath + "/skylinetoolsstore/css/";
    final String imgDir = contextPath + "/skylinetoolsstore/img/";
    final String jsDir = contextPath + "/skylinetoolsstore/js/";

    final String toolOwners = StringUtils.trimToEmpty(form.getToolOwners());
    final String sender = form.getSender();

    // Same check SetOwnersAction makes, so a folder admin who is not a site admin still gets the
    // autocomplete list rather than an empty one.
    final boolean storeAdmin = getContainer().hasPermission(getUser(), AdminPermission.class);
    final SafeToRender autocompleteUsers = storeAdmin ? SkylineToolsStoreController.getUsersForAutocomplete() : HtmlString.unsafe("\"\"");
    pageContext.setAttribute("autocompleteUsers", autocompleteUsers);
%>

<labkey:errors/>

<labkey:form action="<%= urlFor(SkylineToolsStoreController.SetOwnersAction.class) %>" enctype="multipart/form-data" method="post">
    <p>
        <label for="toolOwners">Tool owners </label><br />
        <input style="width: 400px; max-width: 80%;" type="text" id="toolOwners" name="toolOwners" /><br /><br />
        <br />
<% if (sender != null) { %>
        <input type="hidden" name="sender" value="<%= h(sender) %>" />
<% } %>
        <input type="hidden" name="toolId" value="<%= form.getToolId() %>" />
        <input type="submit" value="Update Tool Owners" />
    </p>
</labkey:form>

<br />
<%= PageFlowUtil.generateBackButton() %>

<%-- See SkylineToolDetails.jsp: jQuery UI replaces Bootstrap's $.fn.tooltip, and LabKey's ready
     handler then applies a jQuery UI tooltip to every [title] on the page. --%>
<script nonce="<%=getScriptNonce()%>">
    var lkBootstrapTooltip = jQuery.fn.tooltip && jQuery.fn.tooltip.noConflict
            ? jQuery.fn.tooltip.noConflict() : null;
</script>
<script src="https://code.jquery.com/ui/1.13.2/jquery-ui.min.js" nonce="<%=getScriptNonce()%>"></script>
<script nonce="<%=getScriptNonce()%>">
    if (lkBootstrapTooltip)
        jQuery.fn.tooltip = lkBootstrapTooltip;
</script>
<link rel="stylesheet" href="https://code.jquery.com/ui/1.13.2/themes/smoothness/jquery-ui.min.css">

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    var ownersTxt = $("#toolOwners");
    ownersTxt.focus();
    <%-- q() and not h(). A script element holds raw text, so the entities h() writes would show up
         literally in the box, and a backslash would end the string early. --%>
    ownersTxt.val(<%= q(toolOwners) %>);

    autocomplete(ownersTxt, ${autocompleteUsers});
    initJqueryUiImages("<%= h(imgDir + "jquery-ui") %>");
</script>
