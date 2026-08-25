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
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.data.ContainerManager" %>
<%@ page import="org.labkey.api.security.permissions.DeletePermission" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.util.DOM" %>
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ page import="org.labkey.api.util.SafeToRender" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.skylinetoolsstore.SkylineToolsStoreController" %>
<%@ page import="org.labkey.skylinetoolsstore.SkylineToolsStoreManager" %>
<%@ page import="org.labkey.skylinetoolsstore.model.SkylineTool" %>
<%@ page import="org.labkey.skylinetoolsstore.view.SkylineToolStoreUrls" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Objects" %>
<%@ page import="static org.labkey.api.util.DOM.SPAN" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("skylinetoolsstore/js/functions.js");
        dependencies.add("skylinetoolsstore/css/toolstore.css");
    }

    // A font glyph carries no alt text, so each link names itself with a title of "Edit <property>",
    // which is both the tooltip and the accessible name.
    public final HtmlString editIconImgHtml = DOM.createHtml(
            SPAN(DOM.cl("fa", "fa-pencil", "editToolIcon").at(DOM.Attribute.aria_hidden, "true")));
%>

<%
    JspView<?> me = HttpView.currentView();
    final SkylineTool tool = (SkylineTool)me.getModelBean();
    final boolean admin = getUser().hasSiteAdminPermission();

    final String contextPath = AppProps.getInstance().getContextPath();
    final String imgDir = contextPath + "/skylinetoolsstore/img/";

    final SafeToRender autocompleteUsers = admin ? SkylineToolsStoreController.getUsersForAutocomplete() : HtmlString.unsafe("\"\"");

    // Get supporting files in map <url, icon url>
    HashMap<String, String> suppFiles = SkylineToolsStoreController.getSupplementaryFiles(tool);
    Iterator suppIter = suppFiles.entrySet().iterator();

    final String toolOwners = StringUtils.join(SkylineToolsStoreController.getToolOwners(tool), ", ");

    // Cannot be null here - the page was resolved from this tool.
    final Container toolContainer = tool.lookupContainer();
    // UpdateToolAction refuses anything but the latest version, so the item that reaches it is
    // offered only there. On an older version's page it cost the owner a whole upload first.
    final boolean isLatestVersion = tool.getLatest();
    // isEditor is the store's own definition of who may use the editing controls, and it requires
    // Update, Insert and Delete together, which is what the Editor role on a tool folder carries.
    final boolean toolEditor = tool.isEditor(getUser());
    // DeleteSupplementAction requires only Delete. Checked separately so the icon follows the
    // permission the action actually enforces.
    final boolean canDeleteSuppFiles = toolContainer.hasPermission(getUser(), DeletePermission.class);
    final SkylineTool[] allVersions = SkylineToolsStoreController.sortToolsByCreateDate(SkylineToolsStoreManager.get().getToolsByIdentifier(tool.getIdentifier()));
    final boolean multipleVersions = allVersions.length > 1;

    // DeleteLatestAction deletes the newest version and refuses a row id naming any other, so the
    // item belongs only on the newest version's page. It checks Delete on that version's own folder,
    // which is not this page's folder when an older version is on screen, and owners are set one
    // folder at a time, so the two policies can differ.
    final SkylineTool latestVersion = allVersions[0];
    final Container latestVersionContainer = latestVersion.lookupContainer();
    // getRowId returns an Integer, so compare values rather than references.
    final boolean canDeleteLatest = Objects.equals(tool.getRowId(), latestVersion.getRowId())
            && latestVersionContainer != null
            && latestVersionContainer.hasPermission(getUser(), DeletePermission.class);
    final int numDownloads = Arrays.stream(allVersions).mapToInt(SkylineTool::getDownloads).sum();

    ActionURL toolDetailsUrl = SkylineToolStoreUrls.getToolDetailsUrl(tool);
    ActionURL toolDetailsLatestUrl = SkylineToolStoreUrls.getToolDetailsLatestUrl(tool);
%>
<style>
a { text-decoration: none; }
.logoWrap {
    height: 100px;
    width: 100px;
    float: left;
    margin: 4px;
    border: 2px solid #dcdcdc;
}
/* The pencil sits in the bottom right corner of the tool logo. Bootstrap sets border-box globally,
   so the logo's 100px width already includes its 2px borders. With the 4px margin its far corner is
   104px in, and the chip below is 18px square, so 82px insets it 4px from that corner. The source
   branch measured this with jQuery UI at run time, which is no longer available. */
#editIcon {
    position: absolute;
    left: 82px;
    top: 82px;
}
/* Edit pencils. Font glyphs size from font-size - the ".barItem img" rule below covers only
   images. */
.editToolIcon {
    font-size: 13px;
    color: #999;
    margin-left: 6px;
}
a:hover .editToolIcon, a:focus .editToolIcon {color: #126495;}
/* This pencil sits on top of the uploaded tool logo, so no single colour is readable on every
   image. A translucent chip behind it gives the glyph something to sit on. The fixed line-height
   makes the chip 18px square, which is what the offset above is measured against - the source
   branch could place it with jQuery UI at run time, and this has to be arithmetic instead. */
#editIcon .editToolIcon {
    color: #333;
    background: rgba(255, 255, 255, 0.85);
    border-radius: 3px;
    padding: 2px 3px;
    margin: 0;
    font-size: 14px;
    line-height: 1;
}
/* Repeated at this specificity because the rule above is an id selector, which outranks the
   a:hover rule and would otherwise hold this pencil at its resting colour. */
#editIcon:hover .editToolIcon, #editIcon:focus .editToolIcon {color: #126495;}
/* Supplementary file type icons. Font glyphs size from font-size, so the ".barItem img" rule
   further down does not reach them. */
.suppFileIcon {font-size: 14px; color: #666; margin-right: 5px;}
.headerwrap {display: block; overflow: hidden;}
.headerwrap h3 {margin: 0 !important; padding: 5px 0 0; font-weight: 500 !important;}
.headerwrap p {margin: 0;}
.headerwrap h2 {margin: 0 !important; padding: 0 !important; font-weight: 500 !important;}
.block {
    float: left;
    margin: 0 0 0 3px !important;
    padding-right: 20px !important;
    padding-bottom: 15px;
}
.block p {padding-top: 4px;}
.importantLink {font-weight: 700; color: red; text-decoration: underline;}
.importantLink:hover {color: #000; text-decoration: none;}
.importantLink:active {color: #f60;}
.leftstyle {
    background: url('<%= h(imgDir) %>bg.jpg') repeat-y;
    margin-top: 20px;
    float: left;
    width: 100%;
}
.bottombar a > div {color: #126495;}
#allVersionsPop a {color: #126495;}
#allVersionsPop a:hover {color: #000;}
#toolOwners {width: 80%; min-width: 300px;}
#editToolDlg input[type=text],#editToolDlg textarea {width: 80%; min-width: 400px;}
#editToolDlg textarea {height: 80%; min-height: 200px;}
#toolDescription {text-align: justify;}
#downloadArea {margin: 15px auto 0 auto; text-align: center;}
.deleteSuppFile {
    cursor: pointer;
    color: #999;
    margin-left: 8px;
}
.deleteSuppFile:hover, .deleteSuppFile:focus {color: #cd0a0a;}
.itemsbox {
    min-height: 60px;
    min-width: 190px;
    border: 1px solid #000;
    background-color: #F5F6F7;
    -moz-border-radius: 5px;
    -webkit-border-radius: 5px;
    -khtml-border-radius: 5px;
    border-radius: 5px;
    margin-right: 20px;
    padding-left: 10px;
    margin-top: 25px;
    overflow: visible;
    float: left;
}
#addMissingProp img {float: right; margin-top: 4px;}
.barItem {
    background: #F5F6F7;
    padding: 0 0 5px;
    margin: 10px 24px 0 0;
    z-index: 1;
}
.barItem img {width: 15px; height: 15px;}
.itemsbox legend {
    font-size: 110%;
    font-weight: 600;
    margin: -10px 10px 0 0;
    position: relative;
    background-color: #F5F6F7;
    color: #000;
    border: 1px solid #000;
    max-width:150px;
}
.banner-button {
    display: inline-flex;
    align-items: center;
    margin: 0;
    padding: 15px;
    height: 25px;
    color: #fff;
    border-radius: 5px;
    font-size: 115%;
    font-weight: bold;
    border: 1px solid #215da0;
    text-shadow: -1px -1px #2e6db3;
    box-shadow: 0 2px #ccc;
    text-align: center;
    background: #73a0e2; /* Old browsers */
    /* IE9 SVG, needs conditional override of 'filter' to 'none' */
    background: url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/Pgo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgdmlld0JveD0iMCAwIDEgMSIgcHJlc2VydmVBc3BlY3RSYXRpbz0ibm9uZSI+CiAgPGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVjZ2ctZ2VuZXJhdGVkIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIwJSIgeTI9IjEwMCUiPgogICAgPHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzczYTBlMiIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiMyMTVkYTAiIHN0b3Atb3BhY2l0eT0iMSIvPgogIDwvbGluZWFyR3JhZGllbnQ+CiAgPHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEiIGhlaWdodD0iMSIgZmlsbD0idXJsKCNncmFkLXVjZ2ctZ2VuZXJhdGVkKSIgLz4KPC9zdmc+);
    background: -moz-linear-gradient(top,  #73a0e2 0%, #215da0 100%); /* FF3.6+ */
    background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#73a0e2), color-stop(100%,#215da0)); /* Chrome,Safari4+ */
    background: -webkit-linear-gradient(top,  #73a0e2 0%,#215da0 100%); /* Chrome10+,Safari5.1+ */
    background: -o-linear-gradient(top,  #73a0e2 0%,#215da0 100%); /* Opera 11.10+ */
    background: -ms-linear-gradient(top,  #73a0e2 0%,#215da0 100%); /* IE10+ */
    background: linear-gradient(to bottom,  #73a0e2 0%,#215da0 100%); /* W3C */
    filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#73a0e2', endColorstr='#215da0',GradientType=0 ); /* IE6-8 */
}
.banner-button-small {
    display: inline-flex;
    align-items: center;
    margin: 0;
    padding: 10px 12px;
    height: 15px;
    color: #fff;
    border-radius: 5px;
    font-size: 75%;
    font-weight: bold;
    border: 1px solid #6d0019;
    text-shadow: -1px -1px #6d0019;
    box-shadow: 0 2px #ccc;
    text-align: center;
    background: #a90329; /* Old browsers */
    /* IE9 SVG, needs conditional override of 'filter' to 'none' */
    background: url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/Pgo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgdmlld0JveD0iMCAwIDEgMSIgcHJlc2VydmVBc3BlY3RSYXRpbz0ibm9uZSI+CiAgPGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVjZ2ctZ2VuZXJhdGVkIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIwJSIgeTI9IjEwMCUiPgogICAgPHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzczYTBlMiIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiMyMTVkYTAiIHN0b3Atb3BhY2l0eT0iMSIvPgogIDwvbGluZWFyR3JhZGllbnQ+CiAgPHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEiIGhlaWdodD0iMSIgZmlsbD0idXJsKCNncmFkLXVjZ2ctZ2VuZXJhdGVkKSIgLz4KPC9zdmc+);
    background: -moz-linear-gradient(top,  #a90329 0%, #6d0019 100%); /* FF3.6+ */
    background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#a90329), color-stop(100%,#6d0019)); /* Chrome,Safari4+ */
    background: -webkit-linear-gradient(top,  #a90329 0%,#6d0019 100%); /* Chrome10+,Safari5.1+ */
    background: -o-linear-gradient(top,  #a90329 0%,#6d0019 100%); /* Opera 11.10+ */
    background: -ms-linear-gradient(top,  #a90329 0%,#6d0019 100%); /* IE10+ */
    background: linear-gradient(to bottom,  #a90329 0%,#6d0019 100%); /* W3C */
    filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#a90329', endColorstr='#6d0019',GradientType=0 ); /* IE6-8 */
}
.sprocket {float: right; margin: 0 0 8px 12px;}
/* The gear opens the menu, so it is a button and can be reached by keyboard. Strip the chrome a
   button comes with so it still looks like a bare icon. */
.sprocketToggle {background: none; border: none; padding: 0; cursor: pointer;}
.sprocketIcon {font-size: 26px; color: #666;}
.sprocketToggle:hover .sprocketIcon, .sprocketToggle:focus .sprocketIcon {color: #126495;}
/* Scoped to this menu on purpose. A bare .dropdown-menu rule would also widen LabKey's own header
   and admin menus, which are Bootstrap dropdowns on the same page. */
.sprocket .dropdown-menu {min-width: 240px;}
.boldfont {font-weight: 700;}
</style>
<%-- Bootstrap 3 modals. The structure is fixed by LabKey's own test component,
     components/bootstrap/ModalDialog, which finds a dialog by .modal-dialog plus .modal-title and
     its buttons by visible text. Submits are <button> and not <input type="submit"> for the same
     reason. Every dialog holding entered data uses a static backdrop, so a stray click beside it
     cannot throw that data away. Only allVersionsPop, which is read only, closes on a backdrop
     click.

     No "fade" class. ModalDialog.waitForReady waits for the body to be displayed and non-empty and
     does not wait out a CSS transition, so a fading modal hands back a dialog whose buttons are not
     yet clickable. Animating them is what made these dialogs flaky before. --%>
<div class="modal" id="allVersionsPop" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title">All versions</h4>
            </div>
            <div class="modal-body">
<%
    for (SkylineTool iVersion : allVersions) {
        boolean viewingThis = iVersion.getVersion().equals(tool.getVersion());
 %>
                <p<% if (iVersion.getLatest()) { %> class="boldfont"<% } %>>
                    <%= h(iVersion.getPrettyCreated()) %> |
<% if (!viewingThis) { %>
                    <a href="<%=h(SkylineToolStoreUrls.getToolDetailsUrl(iVersion))%>">
<% } %>
                        <%= h(iVersion.getName()) %> (version <%= h(iVersion.getVersion()) %>)
<% if (!viewingThis) { %>
                    </a>
<% } %>
                </p>
<% } %>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
<!--Manage Tool Owners Form-->
<%-- Site admin only, matching SetOwnersAction and the menu item that opens it. --%>
<% if (admin) { %>
<div class="modal" id="manageOwnersPop" tabindex="-1" role="dialog" data-backdrop="static">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <labkey:form action="<%=urlFor(SkylineToolsStoreController.SetOwnersAction.class)%>" method="post">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Manage tool owners</h4>
                </div>
                <div class="modal-body">
                    <label for="toolOwners">Tool owners</label>
                    <input type="text" class="form-control" id="toolOwners" name="toolOwners" />
                    <input type="hidden" name="returnUrl" value="<%= h(toolDetailsUrl) %>" />
                    <input type="hidden" name="toolId" value="<%= h(tool.getRowId()) %>" />
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Update Tool Owners</button>
                </div>
            </labkey:form>
        </div>
    </div>
</div>
<% } %>
<!--Upload New Version Form-->
<div class="modal" id="uploadPop" tabindex="-1" role="dialog" data-backdrop="static">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <labkey:form action="<%=SkylineToolStoreUrls.getUpdateToolUrl(tool)%>" enctype="multipart/form-data" method="post">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Upload tool zip file</h4>
                </div>
                <div class="modal-body">
                    <p>Browse to the zip file containing the tool you would like to upload.</p>
                    <input type="file" size="50" name="toolZip" />
                    <input type="hidden" name="returnUrl" value="<%= h(toolDetailsUrl) %>" />
                    <input type="hidden" name="toolId" value="<%= h(tool.getRowId()) %>" />
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Upload Tool</button>
                </div>
            </labkey:form>
        </div>
    </div>
</div>
<!--Upload Supplementary File Form-->
<div class="modal" id="uploadSuppPop" tabindex="-1" role="dialog" data-backdrop="static">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <labkey:form action="<%=SkylineToolStoreUrls.getInsertSupplementUrl(tool)%>" enctype="multipart/form-data" method="post">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Upload supplementary file</h4>
                </div>
                <div class="modal-body">
                    <p>Browse to the supplementary file you would like to upload.</p>
                    <input type="file" size="50" name="suppFile" />
                    <input type="hidden" name="returnUrl" value="<%= h(toolDetailsUrl) %>" />
                    <input type="hidden" name="toolId" value="<%= h(tool.getRowId()) %>" />
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Upload Supplementary File</button>
                </div>
            </labkey:form>
        </div>
    </div>
</div>
<!--Delete Tool Dialog-->
<div class="modal" id="delToolAllDlg" tabindex="-1" role="dialog" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Delete</h4>
            </div>
            <div class="modal-body">
                <p>Are you sure you want to completely delete <%= h(tool.getName()) %>?</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-danger" id="delToolAllOk">Ok</button>
            </div>
        </div>
    </div>
</div>
<!--Delete Tool Latest Version Dialog-->
<div class="modal" id="delToolLatestDlg" tabindex="-1" role="dialog" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Delete latest version</h4>
            </div>
            <div class="modal-body">
                <p>Are you sure you want to delete <%= h(allVersions[0].getName()) %> version <%= h(allVersions[0].getVersion()) %>?</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-danger" id="delToolLatestOk">Ok</button>
            </div>
        </div>
    </div>
</div>
<!--Edit Tool Properties Dialog-->
<div class="modal" id="editToolDlg" tabindex="-1" role="dialog" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Edit tool properties</h4>
            </div>
            <div class="modal-body">
                <h3></h3>
                <input type="text" class="form-control" />
                <textarea class="form-control"></textarea>
                <input id="editIconFile" type="file" />
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-primary" id="editToolOk">Ok</button>
            </div>
        </div>
    </div>
</div>

<div class="headerwrap">
    <%-- Positioned so the edit pencil can be placed against the logo's corner in CSS. --%>
    <div style="float:left; width:351px; position:relative;">
        <img id="toolIcon" src="<%= h(tool.getIconUrl()) %>" class="logoWrap" alt="<%= h(tool.getName()) %>">
<% if (toolEditor) { %>
        <%=simpleLink(editIconImgHtml).addClass("toolProperty").id("editIcon").title("Edit Icon").onClick("editTool($(this), 'Icon')")%>
<% } %>
        <div class="block">
            <h2><%= h(tool.getName()) %></h2>
            <p>
                Version <%= h(tool.getVersion()) %>
<% if (allVersions.length > 1) { %>
                [<%=simpleLink("View All").onClick("$('#allVersionsPop').modal('show')")%>]
            </p>
<% } %>
            </p>
            <p>Uploaded <%= h(tool.getPrettyCreated()) %></p>

            <% if (!tool.getLatest()) { %>
            <p>
                <a class="importantLink" href="<%=h(SkylineToolStoreUrls.getToolDetailsUrl(allVersions[0]))%>">See latest version</a>
            <p>
<% } %>
        </div>

        <%
            Container supportContainer = getContainer().getChild("Support");
            Container toolSupportBoard = supportContainer != null ? supportContainer.getChild(tool.getName()) : null;
            if (toolSupportBoard == null)
                toolSupportBoard = ContainerManager.getForPath("/home/support");
        %>
        <% if (toolSupportBoard != null) { %>
        <button id="tool-support-board-btn" class="banner-button-small">Support Board</button>
        <% addHandler("tool-support-board-btn", "click", "window.open(" + q(urlProvider(ProjectUrls.class).getBeginURL(toolSupportBoard)) + ", '_blank', 'noopener,noreferrer')"); %>
        <% } %>
    </div>
<% if (toolEditor) { %>
    <%-- Bootstrap 3 dropdown. data-toggle="dropdown" is all the wiring it needs. Bootstrap opens and
         closes the menu, closes it on a click elsewhere or on Escape, and allows only one open at a
         time. Right aligned because the gear floats at the right edge of the banner. --%>
    <div class="dropdown sprocket">
        <button type="button" id="toolSettingsMenu" class="sprocketToggle dropdown-toggle"
                data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" title="Settings">
            <%-- A font glyph carries no alt text, so the button's name comes from the hidden span. --%>
            <span class="fa fa-cogs sprocketIcon" aria-hidden="true"></span><span class="sr-only">Settings</span>
        </button>
        <ul class="dropdown-menu dropdown-menu-right" aria-labelledby="toolSettingsMenu">
<% if (isLatestVersion) { %>
            <li><%=simpleLink("Upload new version").onClick("$('#uploadPop').modal('show')")%></li>
<% } %>
            <li><%=simpleLink("Upload supplementary file").onClick("$('#uploadSuppPop').modal('show')")%></li>
<% if (multipleVersions && canDeleteLatest) { %>
            <li><%=simpleLink("Delete latest version").onClick("$('#delToolLatestDlg').modal('show')")%></li>
<% } %>
<% if (admin) { %>
            <li><%=simpleLink("Delete").onClick("$('#delToolAllDlg').modal('show')")%></li>
            <li><%=simpleLink("Manage tool owners").onClick("popToolOwners()")%></li>
<% } %>
        </ul>
    </div>
<% } %>

    <p id="toolDescription" class="toolProperty" title="Description">
        <span class="toolPropertyValue"><%= h(tool.getDescription(), true) %></span>
<% if (toolEditor) { %>
        <%=simpleLink(editIconImgHtml).title("Edit Description").onClick("editTool($(this))")%>
<% } %>
    </p>
    <div id="downloadArea">
        <button id="download-tool-btn" class="banner-button">Download <%=h(tool.getName())%></button>
        <% addHandler("download-tool-btn", "click", "downloadTool(" + tool.getRowId() + ")"); %>
        <br>
        <strong>Downloaded: <span id="downloadcounter"><%= numDownloads %></span></strong>
    </div>
</div>

<%
    boolean hasDocumentation = tool.hasDocumentation();
%>
<% if (hasDocumentation || suppIter.hasNext()) { %>
<div id="documentationbox" class="itemsbox">
    <legend>Documentation</legend>
<% if (hasDocumentation) { %>
    <div class="barItem">
        <a href="<%=h(tool.getDocsUrl())%>" target="_blank" rel="noopener noreferrer">
        <img src="<%= h(imgDir) %>link.png" alt="Documentation" />
        <span>Online Documentation</span>
        </a>
    </div>
<% } %>
<%
    while (suppIter.hasNext()) {
        Map.Entry suppPair = (Map.Entry)suppIter.next();
%>
    <div class="barItem suppfile">
        <a href="<%=h(suppPair.getKey())%>">
        <span class="<%=h(suppPair.getValue())%> suppFileIcon" aria-hidden="true"></span>
        <span class="suppfilename"><%= h(new File(suppPair.getKey().toString()).getName()) %></span>
        </a>
<% if (canDeleteSuppFiles) { %>
        <span class="fa fa-trash deleteSuppFile" title="Delete this file" role="button" tabindex="0"></span>
<% } %>
    </div>
<% } %>
</div>
<% } %>
<div id="toolinformationbox" class="itemsbox">
    <legend>Tool Information</legend>
<% if (tool.getOrganization() != null || toolEditor) { %>
    <div class="barItem toolProperty" title="Organization">
        <!--<img src="<%= h(imgDir) %>organization.png" alt="Organization" /> -->
        <span class="boldfont">Organization:</span>
        <span class="toolPropertyValue"><%= h(tool.getOrganization()) %></span>
<% if (toolEditor) { %>
       <%=simpleLink(editIconImgHtml).title("Edit Organization").onClick("editTool($(this))")%>
<% } %>
    </div>
<% } %>
<% if (tool.getAuthors() != null || toolEditor) { %>
    <div class="barItem toolProperty" title="Authors">
        <!--<img src="<%= h(imgDir) %>author.png" alt="Authors" />-->
        <span class="boldfont">Authors:</span>
        <span class="toolPropertyValue"><%= h(tool.getAuthors()) %></span>
<% if (toolEditor) { %>
        <%=simpleLink(editIconImgHtml).title("Edit Authors").onClick("editTool($(this), 'author')")%>
<% } %>
    </div>
<% } %>
<% if (tool.getLanguages() != null || toolEditor) { %>
    <div class="barItem toolProperty" title="Languages">
        <!--<img src="<%= h(imgDir) %>language_type.png" alt="Languages" />-->
        <span class="boldfont">Languages:</span>
        <span class="toolPropertyValue"><%= h(tool.getLanguages()) %></span>
<% if (toolEditor) { %>
        <%=simpleLink(editIconImgHtml).title("Edit Languages").onClick("editTool($(this))")%>
<% } %>
    </div>
<% } %>
<% if (tool.getProvider() != null || toolEditor) { %>
    <div class="barItem toolProperty" title="Provider's Website">
        <!--<img src="<%= h(imgDir) %>link.png" alt="Provider" />-->
        <span class="boldfont">More Information:</span>
        <a href="<%= h(tool.getProvider()) %>" target="_blank" rel="noopener noreferrer"><span class="toolPropertyValue"><%= h(tool.getProvider()) %></span></a>
<% if (toolEditor) { %>
        <%=simpleLink(editIconImgHtml).title("Edit Provider's Website").onClick("editTool($(this), 'provider')")%>
<% } %>
    </div>


<% } %>
</div>


<script type="text/javascript" nonce="<%=getScriptNonce()%>">
<% if (canDeleteSuppFiles) { %>
    $(".deleteSuppFile").on("click keydown", function(e) {
        // The icon can be reached by keyboard, where only Enter (13) and Space (32) should delete.
        // keydown rather than keypress, which is deprecated, and Space has to be stopped or the
        // page scrolls behind the confirm.
        if (e.type === "keydown") {
            if (e.which !== 13 && e.which !== 32)
                return;
            e.preventDefault();
        }

        var suppFileItem = $(this).closest(".suppfile");
        var targetDel = suppFileItem.find(".suppfilename").text().trim();
        if (!confirm("Really delete the supplementary file \"" + targetDel + "\"?"))
            return;

        $.post("<%=h(SkylineToolStoreUrls.getDeleteSupplementUrl(tool))%>", {
            "toolId": <%= h(tool.getRowId()) %>,
            "suppFile": targetDel,
            "X-LABKEY-CSRF": LABKEY.CSRF
        }).done(function() {
            suppFileItem.remove();
            if ($("#documentationbox").children(".suppfile").length === 0)
                $("#documentationbox").hide();
        }).fail(function() {
            alert("An error occurred while trying to delete the file.");
        });
    });
<% } %>
    var REPLACE_TEXT_FADE_TIME = 250;

    function downloadTool(toolId) {
        if (getCookie("<%= h(SkylineToolsStoreController.DownloadToolAction.DOWNLOADED_COOKIE_PREFIX) %>" + toolId) != "1") {
            var downloadCounter = $("#downloadcounter");
            downloadCounter.fadeOut(REPLACE_TEXT_FADE_TIME, function() {
                downloadCounter.html(parseInt(downloadCounter.html()) + 1);
                downloadCounter.fadeIn(REPLACE_TEXT_FADE_TIME);
            });
        }

        window.location.href = <%= q(urlFor(SkylineToolsStoreController.DownloadToolAction.class).addParameter("id", tool.getRowId())) %>;
    }

    function popToolOwners() {
        var ownersTxt = $("#toolOwners");
        $("#manageOwnersPop").modal("show");
        <%-- q() and not h(). See SkylineToolManageOwners.jsp. --%>
        ownersTxt.focus().val(<%= q(toolOwners) %>);
        if (ownersTxt.val())
            ownersTxt.val(ownersTxt.val() + ", ");
    }

    <%-- Scoped to one modal so it cannot reach another dialog on the page. --%>
    function setModalButtonsEnabled(modal, enable) {
        modal.find(".modal-footer button").prop("disabled", !enable);
    }

    <%-- Reports a refused request inside the modal that made it, and leaves Cancel as the way out.
         Scoped to that modal, so a refusal in one cannot disable the controls of another. The
         actions answer a refusal with an error status and a JSON body naming the reason, so show
         that when there is one. Added as a text node, so a message carrying markup is displayed
         rather than parsed. --%>
    function showModalError(modal, xhr, fallback) {
        var message = (xhr && xhr.responseJSON && xhr.responseJSON.exception) || fallback;
        modal.find(".modal-body").empty().append($("<p></p>").text(message));
        <%-- The confirm button, whatever it is styled as. The delete dialogs use btn-danger and the
             edit dialog btn-primary, while Cancel is the one carrying data-dismiss. --%>
        modal.find(".modal-footer button:not([data-dismiss])").hide();
        setModalButtonsEnabled(modal, true);
    }

    $("#delToolAllOk").click(function() {
        var dlg = $("#delToolAllDlg");
        setModalButtonsEnabled(dlg, false);
        // Posted over ajax rather than by submitting a form. The action answers with the page to go
        // to, so a refusal can be shown in this modal instead of replacing the page with an error
        // view.
        $.post(<%=q(urlFor(SkylineToolsStoreController.DeleteAction.class))%>, {
            "toolId": <%=tool.getRowId()%>,
            "X-LABKEY-CSRF": LABKEY.CSRF
        }).done(function(data) {
            window.location = data.successUrl;
        }).fail(function(xhr) {
            showModalError(dlg, xhr, "An error occurred trying to delete " +
                    <%=q(tool.getName())%> + ".");
        });
    });

    $("#delToolLatestOk").click(function() {
        var dlg = $("#delToolLatestDlg");
        setModalButtonsEnabled(dlg, false);
        // A POST, not a navigation. DeleteLatestAction deletes a container, so it must not be
        // reachable by GET, and the CSRF token cannot ride on a navigation.
        // Addressed to allVersions[0], not the version being viewed. This item is offered on an
        // older version's page too, and the action always removes the newest one, so both the URL
        // and the id have to name that version.
        // returnUrl is the page to come back to. The action rewrites the name and version it
        // carries when the deleted version supplied them, and returns the result.
        $.post(<%=q(SkylineToolStoreUrls.getDeleteLatestUrl(allVersions[0]).getLocalURIString())%>, {
            "toolId": <%=allVersions[0].getRowId()%>,
            "returnUrl": <%=q(toolDetailsLatestUrl.getLocalURIString())%>,
            "X-LABKEY-CSRF": LABKEY.CSRF
        }).done(function(data) {
            window.location = data.successUrl;
        }).fail(function(xhr) {
            showModalError(dlg, xhr, "An error occurred trying to delete the latest version of " +
                    <%=q(allVersions[0].getName())%> + ".");
        });
    });

    // The body is rebuilt on every open, so keep the markup the page shipped with.
    var editDlgOriginalBody = $("#editToolDlg .modal-body").html();

    $("#editToolOk").click(function() {
                var dlg = $("#editToolDlg");
                var body = dlg.find(".modal-body");
                setModalButtonsEnabled(dlg, false);
                var propName = dlg.data("propName");
                var propValue;
                var isIcon = (propName.toLowerCase() == "icon") ? true : false;
                var postData;
                if (!isIcon) {
                    propValue = body.children("input:text:visible, textarea:visible").first().val().replace(/r?\n/g, "\r\n").replace(/\\*$/, "");
                    postData = {
                        "toolId": <%= tool.getRowId() %>,
                        "propName": propName,
                        "propValue": propValue
                    };
                } else {
                    var iconFile = document.getElementById("editIconFile").files[0];
                    if (!iconFile) {
                        // With nothing chosen this used to post the string "undefined", which
                        // arrives as a text property named Icon rather than a file. The action
                        // refuses that, but the refusal is an error view with status 200, so the
                        // handler below took it for success and faded the icon away.
                        body.find(".labkey-error").remove();
                        body.append($('<p class="labkey-error"></p>').text("Please choose an image file."));
                        setModalButtonsEnabled(dlg, true);
                        return;
                    }
                    postData = new FormData();
                    postData.append("toolId", <%= tool.getRowId() %>);
                    postData.append("propName", propName);
                    postData.append("propValue", iconFile);
                }

                body.empty().append($("<p></p>").text("Please wait..."));
                // Raw jQuery does not attach the CSRF token the way LABKEY.Ajax does, so the header
                // below sends it explicitly. That covers both the FormData and url-encoded cases.
                $.ajax({
                    type: "POST",
                    headers: {"X-LABKEY-CSRF": LABKEY.CSRF},
                    url: "<%=h(SkylineToolStoreUrls.getUpdatePropertyUrl(tool))%>",
                    data: postData,
                    success: function() {
                        $("#editToolDlg").modal("hide");
                        var container = $("#editToolDlg").data("propValueContainer");
                        if (isIcon) {
                            // The tool's own icon url, not whatever the img is showing. A tool with
                            // no icon yet shows the shared placeholder, and cache-busting that
                            // re-requested the placeholder rather than the image just uploaded.
                            var newImgSrc = <%= q(tool.getFolderUrl() + "icon.png") %> +
                                    "?" + (new Date()).getTime();
                            container.animate({opacity: 0}, REPLACE_TEXT_FADE_TIME, function() {
                                // one("load") rather than load(fn). jQuery 3 removed the event
                                // shorthand and kept load() as the ajax method, so the callback
                                // never ran and the icon stayed faded out until the page was
                                // reloaded. Bound before the src is set, so a load that finishes
                                // immediately cannot beat the handler.
                                // "load error" and not just "load". An image that fails to load
                                // would otherwise leave the logo at opacity 0, which is the state
                                // this handler exists to get out of.
                                container.one("load error", function() {
                                    $(this).animate({opacity: 1}, REPLACE_TEXT_FADE_TIME);
                                }).attr("src", newImgSrc);
                            });
                            return;
                        }
                        var containerParent = container.parent();
                        if (containerParent.is("a") && containerParent.attr("href") == container.text())
                            containerParent.attr("href", propValue);
                        container.parents(".toolProperty:first").fadeOut(REPLACE_TEXT_FADE_TIME, function() {
                            // Text nodes with real <br> between them. The value is whatever the
                            // owner typed, so it must not be parsed as markup.
                            container.empty();
                            propValue.split(/\n/).forEach(function(line, i) {
                                if (i > 0)
                                    container.append($("<br />"));
                                container.append(document.createTextNode(line));
                            });
                            $(this).fadeIn(REPLACE_TEXT_FADE_TIME);
                        });
                    },
                    error: function() {
                        body.empty().append($("<p></p>").text(
                                'An error occurred trying to edit "' + propName + '".'));
                        $("#editToolOk").hide();
                        setModalButtonsEnabled(dlg, true);
                    },
                    contentType: (!isIcon ? "application/x-www-form-urlencoded; charset=UTF-8" : false),
                    processData: !isIcon
                });
    });

    // Put a dialog back the way it opened, whether it closed on success, Cancel or the X. A refused
    // request replaces the body with the reason and hides the confirm button, so both are restored.
    function restoreOnClose(dialogId) {
        var dlg = $("#" + dialogId);
        var original = dlg.find(".modal-body").html();
        dlg.on("hidden.bs.modal", function() {
            dlg.find(".modal-body").html(original);
            dlg.find(".modal-footer button").show();
            setModalButtonsEnabled(dlg, true);
        });
    }

    restoreOnClose("editToolDlg");
    restoreOnClose("delToolAllDlg");
    restoreOnClose("delToolLatestDlg");

    $("#editToolDlg").keydown(function (e) {
        var body = $(this).find(".modal-body");
        if (e.keyCode == 13 &&
            (body.children("input:text:visible").length > 0 ||
            (e.ctrlKey && body.children("textarea:visible").length > 0)))
            $("#editToolOk").trigger("click");
    });

    function editTool(sender, property) {
        var parent = sender.closest(".toolProperty");
        var propName = property || parent.attr("title");
        var propValueContainer = (propName.toLowerCase() != "icon") ?
            parent.find(".toolPropertyValue:first") : $("#toolIcon");

        var targetType = "input:text";
        var hideType = "textarea, input:file";
        if (propName.toLowerCase() == "description") {
            targetType = "textarea";
            hideType = "input:text, input:file";
        } else if (propName.toLowerCase() == "icon") {
            targetType = "input:file";
            hideType = "input:text, textarea";
        }

        var dlg = $("#editToolDlg").data("propName", propName)
                                   .data("propValueContainer", propValueContainer);
        var body = dlg.find(".modal-body").html(editDlgOriginalBody);
        // #editIcon carries .toolProperty itself, so closest() returns the link, whose title is the
        // tooltip rather than the property name. Every other pencil sits inside its property's row.
        body.children("h3:first").text(parent.is(sender) ? propName : parent.attr("title"));
        body.children(hideType).hide();
        dlg.modal("show");
        body.children(targetType + ":first").show().focus().val(propValueContainer.text());
    }

    autocomplete($("#toolOwners"), <%=autocompleteUsers%>);
</script>
