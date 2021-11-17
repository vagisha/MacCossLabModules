<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.panoramapublic.model.speclib.SpecLibInfo" %>
<%@ page import="org.labkey.panoramapublic.model.speclib.SpecLibKey" %>
<%@ page import="org.labkey.panoramapublic.model.speclib.SpecLibSourceType" %>
<%@ page import="org.labkey.panoramapublic.model.speclib.SpecLibDependencyType" %>

<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<PanoramaPublicController.SpecLibInfoBean> view = (JspView<PanoramaPublicController.SpecLibInfoBean>) HttpView.currentView();
    PanoramaPublicController.SpecLibInfoBean bean = view.getModelBean();
    PanoramaPublicController.EditSpecLibInfoForm form = bean.getForm();
    SpecLibKey libKey = bean.getLibKey();
%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<labkey:errors/>

<form method="post"><labkey:csrf/>
    <table>
        <tr>
            <td class="labkey-form-label"><label for="sourceType">Source Type</label><%=helpPopup("Source Type", "")%></td>
            <td>
                <select name="sourceType" id="sourceType" value="<%=h(form.getSourceType())%>">
                    <% for (SpecLibSourceType sourceType : SpecLibSourceType.values()) { %>
                    <option value="<%=h(sourceType.name())%>"<% if (form.getSourceType().equals(sourceType.name())) { %> selected<% } %>>
                        <%=h(sourceType.getDescription())%></option>
                    <% } %>
                </select>
            </td>
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="sourceUrl">Source URL</label><%=helpPopup("Source URL", "")%></td>
            <td><input name="sourceUrl" id="sourceUrl" type="text" value="<%=h(form.getSourceUrl())%>"/></td>
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="sourceAccession">Source Accession</label><%=helpPopup("Source Accession", "")%></td>
            <td><input name="sourceAccession" id="sourceAccession" type="text" value="<%=h(form.getSourceAccession())%>"/></td>
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="sourceUsername">Source Username</label><%=helpPopup("Source Username", "")%></td>
            <td><input name="sourceUsername" id="sourceUsername" type="text" value="<%=h(form.getSourceUsername())%>"/></td>
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="sourcePassword">Source Password</label><%=helpPopup("Source Password", "")%></td>
            <td><input name="sourcePassword" id="sourcePassword" type="password" value="<%=h(form.getSourcePassword())%>"/></td>
        </tr>
        <tr>
            <td class="labkey-form-label"><label for="dependencyType">Dependency Type</label><%=helpPopup("Dependency Type", "")%></td>
            <td>
                <select name="dependencyType" id="dependencyType" value="<%=h(form.getDependencyType())%>">
                    <% for (SpecLibDependencyType dependencyType : SpecLibDependencyType.values()) { %>
                    <option value="<%=h(dependencyType.name())%>"<% if (form.getDependencyType().equals(dependencyType.name())) { %> selected<% } %>>
                        <%=h(dependencyType.getDescription())%></option>
                    <% } %>
                </select>
            </td>
        </tr>
        <tr>
            <td></td>
            <td><%=button("Submit").submit(true) %> <a class="labkey-button" href="<%=h(getViewContext().getContainer().getStartURL(getUser()))%>">Cancel</a></td>
        </tr>
    </table>
</form>

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
            case <%=SpecLibSourceType.LOCAL.ordinal()%>:
                toggleInput(url, false);
                toggleInput(pxid, false);
                toggleInput(accession, false);
                toggleInput(username, false);
                toggleInput(password, false);
                break;
            case <%=SpecLibSourceType.UNAVAILABLE.ordinal()%>:
                toggleInput(url, true);
                toggleInput(pxid, false);
                toggleInput(accession, false);
                toggleInput(username, false);
                toggleInput(password, false);
                break;
            case <%=SpecLibSourceType.OTHER_REPOSITORY.ordinal()%>:
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