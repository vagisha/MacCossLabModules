<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:errors/>
<script type="text/javascript">
    LABKEY.requiresExt4Sandbox(true, function() {
        Ext4.onReady(function(){
            var webdavUrl = LABKEY.ActionURL.getBaseURL() + LABKEY.ActionURL.encodePath("_webdav" + LABKEY.Security.currentContainer.path + "/@files/RawFiles/");
            Ext4.get('webdav_url_link').update(Ext4.util.Format.htmlEncode(webdavUrl));
        });
    });
</script>
<p>
    Select one or more files or folders in the browser above and click the download icon ( <span class="fa fa-download"></span> ).
    Data can also be downloaded by mapping this folder as a network drive in Windows Explorer, or by using a <%=link("WebDAV").href("https://en.wikipedia.org/wiki/WebDAV").clearClasses()%>
    client such as <span class="nobr"><%=link("CyberDuck").href("https://cyberduck.io").clearClasses()%></span> or <span class="nobr"><%=link("WinSCP").href("https://winscp.net/eng/docs/introduction").clearClasses()%></span>.
    For details look at <%=link("Download data from Panorama Public").href("/wiki/home/page.view?name=download_public_data").clearClasses()%>.
    Use the following URL, login email and password to connect to this folder:
    <br/>
    <br/>
    URL: <b class="bold"><span class="nobr" id="webdav_url_link"></span></b>
    <br/>
    Login email: <b class="bold">public@proteinms.net</b>
    <br/>
    Password: <b class="bold">panorama</b>
</p>
