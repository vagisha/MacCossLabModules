<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
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
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.util.SafeToRender"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
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
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.collections.IntHashMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("skylinetoolsstore/js/functions.js");
    }
%>
<script src="https://code.jquery.com/ui/1.13.2/jquery-ui.min.js" nonce="<%=getScriptNonce()%>"></script>
<link rel="stylesheet" href="https://code.jquery.com/ui/1.13.2/themes/smoothness/jquery-ui.min.css">

<%
    JspView<?> me = HttpView.currentView();
    List<SkylineTool> tools = (List<SkylineTool>)me.getModelBean();

    final boolean admin = getUser().hasSiteAdminPermission();
    final boolean loggedIn = !getUser().isGuest();

    final String contextPath = AppProps.getInstance().getContextPath();
    final String imgDir = contextPath + "/skylinetoolsstore/img/";

%>
<style type="text/css">
    .tablewrap {width:100%; min-width:600px; margin-top:20px;}
    .tablewrap td {padding:0; margin:0;}
    .tablewrap:nth-child(odd) {background:#f4f4f4;}
    .leftfill {width:20px; height:100%; background:url('<%= h(imgDir) %>bg.jpg') repeat-y;}
    .icon {height:100px; width:100px; margin: 4px 0 0 10px; border:2px solid #dcdcdc;}
    .title {font-size: 150%; font-weight: 400; color:#0044cc; margin: 0 !important; padding: 0; float: left;}
    .title:hover {text-decoration: underline;}
    .title:active {color: #ff0000;}
    .toolSubtitle {font-size: 14px; margin:0; padding:0; clear: both;}
    .contentleft {width: 128px; vertical-align: top;}
    .contentright {vertical-align: top;}
    .contentcontainer {margin:0; padding:0;}
    .content {margin: 8px 12px 0 0; padding:0; text-align:justify;}
    .toolButtons {margin-top: 12px;}
    .styled-button{
        display:inline-flex;
        align-items:center;
        box-shadow:rgba(0,0,0,0.0.1) 0 1px 0 0;
        background-color:#5B74A8;
        border:1px solid #29447E;
        font-family:'Lucida Grande',Tahoma,Verdana,Arial,sans-serif;
        font-size:12px;
        font-weight:700;
        margin-top: 4px;
        padding:2px 6px;
        height:28px;
        color:#fff;
        border-radius:5px;
        cursor:pointer;
    }
    .styled-button:hover{background-color:#1e90ff; color:#f5f5dc;}
    a.styled-button{text-decoration:none; color:#fff;}
    a.styled-button:visited{color:#fff;}
    .toolOwners {width: 80%; min-width: 300px;}
    .ui-menu {width:240px;}
    .dropMenu {position: absolute;}
    .menuMouseArea {display: inline;}
    .sprocket {cursor: pointer; float: right;}
    .menuIconImg {width: 16px; height: 16px;}
    /* Font glyph counterpart of .menuIconImg, sized to line up with the images beside it. */
    .menuIcon {display: inline-block; width: 16px; font-size: 14px; color: #666;}

</style>

<% if (admin) { %>
<div style="float: left;">
    <button type="button" id="add-new-tool-btn" class="styled-button">Add New Tool</button>
    <% addHandler("add-new-tool-btn", "click",
            "$('#uploadForm').attr('action', " + q(SkylineToolStoreUrls.getInsertToolUrl(getContainer())) + "); " +
            "$('#uploadPopOwners').show(); $('#uploadFormToolId').val('0'); $('#uploadPop').modal('show')"); %>
</div>
<% } %>
<!--Manage Tool Owners Form-->
<%-- Site admin only, matching SetOwnersAction. Rendering it for everyone and relying on the menu
     item being hidden would put a live owners form in every visitor's page, guests included. --%>
<% if (admin) { %>
<div class="modal" id="manageOwnersPop" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <labkey:form action="<%=urlFor(SkylineToolsStoreController.SetOwnersAction.class)%>" method="post">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Manage tool owners</h4>
                </div>
                <div class="modal-body">
                    <label for="toolOwnersManage">Tool owners</label>
                    <input type="text" class="form-control toolOwners" id="toolOwnersManage" name="toolOwners" />
                    <input type="hidden" name="sender" value="<%= h(getActionURL()) %>" />
                    <%-- Set per tool when the dialog opens. Zero rather than blank, because an empty
                         string will not bind to the form's int and would fail before the action runs. --%>
                    <input type="hidden" id="ownersFormToolId" name="toolId" value="0" />
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
<!--Add Tool / Upload New Version Form-->
<div class="modal" id="uploadPop" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <%-- Serves both "Add New Tool" and per-tool "Upload new version", which are different
                 actions in different containers, so each handler below sets the action. Defaults to
                 adding a new tool. --%>
            <labkey:form id="uploadForm" action="<%=SkylineToolStoreUrls.getInsertToolUrl(getContainer())%>" enctype="multipart/form-data" method="post">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Upload tool zip file</h4>
                </div>
                <div class="modal-body">
                    <p>Browse to the zip file containing the tool you would like to upload.</p>
                    <input type="file" name="toolZip" />
<%-- Only "Add New Tool" uses this, and that is site admin only. Publishing a new version hides it
     with script, but hiding is not removing - a hidden input still posts, so it is gated here. --%>
<% if (admin) { %>
                    <span id="uploadPopOwners">
                        <label for="toolOwnersNew">Tool owners</label>
                        <input type="text" class="form-control toolOwners" id="toolOwnersNew" name="toolOwners" />
                    </span>
<% } %>
                    <input type="hidden" name="sender" value="<%= h(getActionURL()) %>" />
                    <%-- Zero for "Add New Tool", which InsertToolAction ignores. A blank value would
                         not bind to the form's int, so the upload would fail before the action. --%>
                    <input type="hidden" id="uploadFormToolId" name="toolId" value="0" />
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
<div class="modal" id="uploadSuppPop" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <%-- One dialog serves every tool, so the action is set per tool in the menu handler
                 below. insertSupplement is addressed to the tool's own container. --%>
            <labkey:form id="uploadSuppForm" enctype="multipart/form-data" method="post">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Upload supplementary file</h4>
                </div>
                <div class="modal-body">
                    <p>Browse to the supplementary file you would like to upload.</p>
                    <input type="file" name="suppFile" />
                    <%-- Set per tool when the dialog opens. See the note on ownersFormToolId above. --%>
                    <input type="hidden" id="suppFormToolId" name="toolId" value="0" />
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Upload Supplementary File</button>
                </div>
            </labkey:form>
        </div>
    </div>
</div>
<%-- Both delete dialogs have their body written per tool when they open, so only .modal-body is
     replaced. Emptying the whole element would take the header and footer with it. --%>
<!-- Delete Tool Dialog -->
<div class="modal" id="delToolAllDlg" tabindex="-1" role="dialog" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Delete</h4>
            </div>
            <div class="modal-body"></div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-danger" id="delToolAllOk">Ok</button>
            </div>
        </div>
    </div>
</div>
<!-- Delete Tool Latest Version Dialog -->
<div class="modal" id="delToolLatestDlg" tabindex="-1" role="dialog" data-backdrop="static" data-keyboard="false">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Delete latest version</h4>
            </div>
            <div class="modal-body"></div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-danger" id="delToolLatestOk">Ok</button>
            </div>
        </div>
    </div>
</div>

<div style="float: right;">
    <label for="sort-selector">Sort by:</label>
    <select id="sort-selector">
        <option value="name-asc">Name &uarr;</option>
        <option value="name-desc">Name &darr;</option>
        <option value="downloads-asc">Downloads &uarr;</option>
        <option value="downloads-desc">Downloads &darr;</option>
    </select>
</div>

<div id="all-tools" style="clear: both; padding-top: 2px;">
<%
    HashMap<Integer, String> toolOwners = new IntHashMap<>();
    for (SkylineTool tool : tools)
    {
        final String tableId = "table-" + tool.getName().replaceAll("[^A-Za-z0-9]", "");
        final ActionURL detailsUrl = SkylineToolStoreUrls.getToolDetailsUrl(tool);

        // Get supporting files in map <url, icon url>
        HashMap<String, String> suppFiles = SkylineToolsStoreController.getSupplementaryFiles(tool);
        Iterator suppIter = suppFiles.entrySet().iterator();
        boolean hasDocs = tool.hasDocumentation();
        int docCount = suppFiles.size() + (hasDocs ? 1 : 0);

        // Only the owners dialog reads this, and only a site admin gets that dialog. Computing it
        // for everyone walks the folder policy and looks up a user per assignment on every
        // anonymous page load, and leaves the addresses one careless edit away from being rendered.
        if (admin)
            toolOwners.put(tool.getRowId(), StringUtils.join(SkylineToolsStoreController.getToolOwners(tool), ", "));
        final boolean toolEditor = admin || tool.lookupContainer().hasPermission(getUser(), UpdatePermission.class);
        final SkylineTool[] allVersions = SkylineToolsStoreManager.get().getToolsByIdentifier(tool.getIdentifier());
        final boolean multipleVersions = allVersions.length > 1;
        final int numDownloads = Arrays.stream(allVersions).mapToInt(SkylineTool::getDownloads).sum();
%>

<table id="<%= h(tableId) %>" class="tablewrap"
       data-toolId="<%= tool.getRowId() %>" data-toolName="<%= h(tool.getName()) %>" data-toolVersion="<%= h(tool.getVersion()) %>" data-toolLsid="<%= h(tool.getIdentifier()) %>"
       data-toolDownloads="<%= numDownloads %>">
    <tr>
        <td class="leftfill"></td>
        <td class="contentleft">
            <a href="<%= h(detailsUrl) %>"><img src="<%= h(tool.getIconUrl()) %>" class="icon" alt="<%= h(tool.getName()) %>"></a>
        </td>
        <td class="contentright">
            <div class="contentcontainer">
                <span class="title"><a href="<%=h(detailsUrl)%>"><%= h(tool.getName()) %></a></span>
<% if (toolEditor) { %>
                <div class="menuMouseArea sprocket" alt="<%= h(tool.getName()) %>">
                    <img src="<%= h(imgDir) %>gear.png" title="Settings" />
                    <ul class="dropMenu">
                        <li><%=simpleLink("Upload new version").onClick(
                                "$('#uploadForm').attr('action', " + q(SkylineToolStoreUrls.getUpdateToolUrl(tool)) + "); " +
                                "$('#uploadPopOwners').hide(); $('#uploadFormToolId').val(" + tool.getRowId() + "); $('#uploadPop').modal('show')")%></li>
                        <li><%=simpleLink("Upload supplementary file").onClick(
                                "$('#uploadSuppForm').attr('action', " +
                                q(SkylineToolStoreUrls.getInsertSupplementUrl(tool)) + "); " +
                                "$('#suppFormToolId').val(" + tool.getRowId() + "); $('#uploadSuppPop').modal('show')")%></li>
<% if (multipleVersions) { %>
                        <li><%=simpleLink("Delete latest version").onClick("delToolLatest($(this))")%></li>
<% } %>
<% if (admin) { %>
                        <li><%=simpleLink("Delete").onClick("delToolAll($(this))")%></li>
                        <li><%=simpleLink("Manage tool owners").onClick("popToolOwners(" + tool.getRowId() + ")")%></li>
<% } %>
                    </ul>
                </div>
<% } %>
                <p class="toolSubtitle">Version: <%= h(tool.getVersion()) %> | Downloads: <%= h(numDownloads) %></p>
<% if (tool.getOrganization() != null) { %>
                <p class="toolSubtitle"><%= h(tool.getOrganization()) %></p>
<% } %>
<% if (tool.getProvider() != null) { %>
                <p class="toolSubtitle"><a href="<%= h(tool.getProvider()) %>" target="_blank"><%= h(tool.getProvider()) %></a></p>
<% } %>
                <p class="content"><%= h(tool.getDescription(), true) %><br />[<a href="<%=h(detailsUrl)%>">Tool Details</a>, <a href="/labkey/home/software/Skyline/tools/Support/<%=h(tool.getName())%>/project-begin.view" target="_blank">Support Board</a>]</p>

                <div class="toolButtons">

                    <%=link(unsafe("Download<span class=\"visually-hidden\">&nbsp;" + h(tool.getName()) + "</span>")).href(urlFor(SkylineToolsStoreController.DownloadToolAction.class).addParameter("id", tool.getRowId()).toString()).clearClasses().addClass("styled-button")%>
<%
    if (docCount == 1 && hasDocs) {
%>
                        <%=link(unsafe("Documentation<span class=\"visually-hidden\">&nbsp;" + h(tool.getName()) + "</span>")).href(tool.getDocsUrl()).clearClasses().addClass("styled-button").target("_blank").rel("noopener noreferrer")%>
<%
    } else if (docCount == 1) {
        Map.Entry suppPair = (Map.Entry)suppIter.next();
%>
                        <%=link(unsafe("Documentation<span class=\"visually-hidden\">&nbsp;" + h(tool.getName()) + "</span>")).href(suppPair.getKey().toString()).clearClasses().addClass("styled-button")%>
<% } else if (docCount > 1) { %>
                        <div class="menuMouseArea">
                            <button type="button" class="styled-button">Documentation<span class="visually-hidden"><%=h(tool.getName())%></span></button>
                            <ul class="dropMenu">
<% if (hasDocs) { %>
                                <li><a href="<%=h(tool.getDocsUrl())%>" target="_blank" rel="noopener noreferrer"><img class="menuIconImg" src="<%= h(imgDir) %>link.png" alt="Documentation">Online Documentation</a></li>
<% } %>
<%
        while (suppIter.hasNext()) {
            Map.Entry suppPair = (Map.Entry)suppIter.next();
%>
                                <li><a href="<%=h(suppPair.getKey())%>"><span class="<%=h(suppPair.getValue())%> menuIcon"></span><%= h(new File(suppPair.getKey().toString()).getName()) %></a></li>
<% } %>
                            </ul>
                        </div>
<% } %>
                </div>
            </div>
        </td>
    </tr>
</table>
<% } %>
</div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    var READ_MORE_TEXT = "Read more";
    var READ_LESS_TEXT = "Close";
    var BASE_SLIDE_TIME = 100;
    var LINE_THRESHOLD = 2.0;

    function adjustContent(element) {
        var newP = $("<p />").html($("<a />").text(READ_MORE_TEXT).click(function() {
            var content = $(this).parent().prev();
            var smallHeight = content.data("smallheight");
            var fullHeight = content.data("fullheight");
            var expand = content.height() < fullHeight;
            var slideTime = fullHeight - smallHeight + BASE_SLIDE_TIME;
            content.animate(
                {height: expand ? fullHeight : smallHeight},
                {queue: false, duration: slideTime}
            );
            $(this).fadeOut({
                queue: false, duration: slideTime / 2, always: function() {
                    $(this).text(expand ? READ_LESS_TEXT : READ_MORE_TEXT);
                    $(this).fadeIn({queue: false, duration: slideTime / 2});
                }}
            );
        }));

        var lineThresholdHeight = parseInt(element.css("line-height", "120%").css("line-height")) * LINE_THRESHOLD + 1;
        if (element.height() > lineThresholdHeight) {
            element.after(newP)
                   .data("fullheight", element.height()).data("smallheight", lineThresholdHeight)
                   .css("overflow", "hidden").height(lineThresholdHeight);
        }
    }

    var MENU_SLIDE_TIME = 100;
    function initMenu(element) {
        var myMenu = element.children(".dropMenu:first");
        if (myMenu.children().length > 0) {
            myMenu.menu().hide();
            element.click(function(e) {
                // Stop click from bubbling up to document click handler
                e.stopPropagation();
                // Only allow one menu open at a time
                if ($(this).children(".dropMenu:first").is(":hidden"))
                    closeMenus();
                myMenu.stop().slideToggle(MENU_SLIDE_TIME);
                myMenu.position({of: $(element).children(":first"), at: "left bottom", my: "left top"});
            });
        }
    }

    // Close menus on non-menu click
    $(document).click(function() {closeMenus();});

    function closeMenus() {
        $(".dropMenu:visible").slideUp(MENU_SLIDE_TIME);
    }

    $(".menuMouseArea").each(function() {initMenu($(this));});

    $(function() {
        $(".content").each(function() {adjustContent($(this));});
    });

<% if (admin) { %>
    var toolOwners = new Array();
<% for (SkylineTool tool : tools) { %>
    <%-- q() and not h(). See SkylineToolManageOwners.jsp. --%>
    toolOwners[<%= tool.getRowId() %>] = <%= q(toolOwners.get(tool.getRowId())) %>;
<%
        }
        SafeToRender users = SkylineToolsStoreController.getUsersForAutocomplete();
%>
    $(".toolOwners").each(function() {autocomplete($(this), <%=users%>);});

    function popToolOwners(id) {
        $('#ownersFormToolId').val(id);
        $('#manageOwnersPop').modal('show');
        var ownersTxt = $("#toolOwnersManage");
        ownersTxt.focus();
        ownersTxt.val(toolOwners[id]);
        if (ownersTxt.val())
            ownersTxt.val(ownersTxt.val() + ", ");
    }
<% } %>

    // .text() and not .html() in both. attr() hands back the DECODED attribute value, so the h()
    // that wrote data-toolName does not survive the round trip, and a tool owner can set the name.
    function delToolAll(sender) {
        var parentTable = sender.parents("table:first");
        $("#delToolAllDlg").data("toolTable", parentTable)
                           .find(".modal-body")
                           .empty()
                           .append($("<p></p>").text(
                                   "Completely delete " + parentTable.attr("data-toolName") + "?"));
        $("#delToolAllDlg").modal("show");
    }

    function delToolLatest(sender) {
        var parentTable = sender.parents("table:first");
        $("#delToolLatestDlg").data("toolTable", parentTable)
                              .find(".modal-body")
                              .empty()
                              .append($("<p></p>").text(
                                      "Delete version " + parentTable.attr("data-toolVersion") +
                                      " of " + parentTable.attr("data-toolName") + "?"));
        $("#delToolLatestDlg").modal("show");
    }

    function extractToolTable(data, lsid) {
        var parsedData = $.parseHTML(data);
        return $(parsedData).find('.tablewrap[data-toolLsid="' + lsid + '"]:first');
    }

    <%-- Bootstrap counterpart of setButtonsEnabled, which targets jQuery UI's button pane. --%>
    function setModalButtonsEnabled(modal, enable) {
        modal.find(".modal-footer button").prop("disabled", !enable);
    }

    function showDeleteAllError(toolTable) {
        var dlg = $("#delToolAllDlg");
        dlg.find(".modal-body").empty().append($("<p></p>").text(
                "An error occurred trying to delete " + toolTable.attr("data-toolName") + "."));
        $("#delToolAllOk").hide();
        setModalButtonsEnabled(dlg, true);
    }

    function showDeleteLatestError(toolTable) {
        var dlg = $("#delToolLatestDlg");
        dlg.find(".modal-body").empty().append($("<p></p>").text(
                "An error occurred trying to delete the latest version of " +
                toolTable.attr("data-toolName") + "."));
        $("#delToolLatestOk").hide();
        setModalButtonsEnabled(dlg, true);
    }

    // Put each delete dialog back the way it opened, however it was closed.
    $("#delToolAllDlg").on("hidden.bs.modal", function() {
        $("#delToolAllOk").show();
        setModalButtonsEnabled($(this), true);
    });
    $("#delToolLatestDlg").on("hidden.bs.modal", function() {
        $("#delToolLatestOk").show();
        setModalButtonsEnabled($(this), true);
    });

    $("#delToolAllOk").click(function() {
                var dlg = $("#delToolAllDlg");
                setModalButtonsEnabled(dlg, false);
                dlg.find(".modal-body").empty().append($("<p></p>").text("Please wait..."));
                var toolTable = dlg.data("toolTable");
                $.post("<%=h(urlFor(SkylineToolsStoreController.DeleteAction.class))%>", {
                    "toolId": toolTable.attr("data-toolId"),
                    "X-LABKEY-CSRF": LABKEY.CSRF
                }).done(function(data) {
                    // A refused delete comes back as an error view with status 200, so .fail() does
                    // not run. The store page always carries #uploadPop and an error view does not.
                    // Without this the row would explode off the page with the folders still there.
                    if ($($.parseHTML(data)).find("#uploadPop").length === 0) {
                        showDeleteAllError(toolTable);
                        return;
                    }
                    $("#delToolAllDlg").modal("hide");
                    toolTable.hide("explode");
                }).fail(function() {
                    showDeleteAllError(toolTable);
                });
    });

    $("#delToolLatestOk").click(function() {
                var dlg = $("#delToolLatestDlg");
                setModalButtonsEnabled(dlg, false);
                dlg.find(".modal-body").empty().append($("<p></p>").text("Please wait..."));
                var toolTable = dlg.data("toolTable");
                $.post("<%=h(urlFor(SkylineToolsStoreController.DeleteLatestAction.class))%>", {
                    "toolId": toolTable.attr("data-toolId"),
                    "X-LABKEY-CSRF": LABKEY.CSRF
                }).done(function(data) {
                    var newToolTable = extractToolTable(data, toolTable.attr("data-toolLsid"));
                    // A rejected delete comes back as an error view with status 200, so .fail()
                    // does not run and the tool's row is absent from the response. Without this
                    // the row would be replaced by nothing and the tool would appear deleted.
                    if (newToolTable.length === 0) {
                        showDeleteLatestError(toolTable);
                        return;
                    }
                    newToolTable.hide();
                    newToolTable.find(".menuMouseArea").each(function() {initMenu($(this));});
                    $("#delToolLatestDlg").modal("hide");
                    toolTable.hide("explode", function() {
                        $(this).replaceWith(newToolTable);
                        $(newToolTable).show("explode", function() {
                            adjustContent($(newToolTable).find(".content:first"));
                        });
                    });
                }).fail(function() {
                    showDeleteLatestError(toolTable);
                });
    });

    const sortTools = function(attr, desc) {
        let all = Array.from(document.querySelectorAll("#all-tools table"));
        all.sort((a, b) => {
            let aAttr = a.getAttribute(attr).toLowerCase();
            let bAttr = b.getAttribute(attr).toLowerCase();
            if (/^\d+$/.test(aAttr) && /^\d+$/.test(bAttr)) {
                aAttr = parseInt(aAttr);
                bAttr = parseInt(bAttr);
            }
            let result = 0;
            if (aAttr !== bAttr) {
                result = aAttr < bAttr ? -1 : 1;
            }
            return desc ? -result : result;
        });
        all.forEach(el => document.querySelector("#all-tools").appendChild(el));
    };
    const sortSelector = document.getElementById("sort-selector");
    sortSelector.onchange = function() {
        switch (this.value) {
            case "name-asc": sortTools("data-toolName", false); break;
            case "name-desc": sortTools("data-toolName", true); break;
            case "downloads-asc": sortTools("data-toolDownloads", false); break;
            case "downloads-desc": sortTools("data-toolDownloads", true); break;
        }
    };
    $(sortSelector).val("name-asc").change();

    initJqueryUiImages("<%= h(imgDir + "jquery-ui") %>");
</script>
