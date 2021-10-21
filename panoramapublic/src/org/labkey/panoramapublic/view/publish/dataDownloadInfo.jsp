<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<labkey:errors/>
<script type="text/javascript">

    LABKEY.requiresExt4Sandbox(true, function() {
        Ext4.onReady(function(){
            var webdavUrl = LABKEY.ActionURL.getBaseURL() + "_webdav" + LABKEY.ActionURL.getContainer() + "/@files/RawFiles/";
            // Ext4.get('webdav_url_link').update("<a href=\"" + encodeURI(webdavUrl) + "\">" + webdavUrl + "</a>");
            Ext4.get('webdav_url_link').update(webdavUrl);
            // console.log(Ext4.String.htmlEncode(webdavUrl));
        });
    });
</script>
<p>
    Raw data can be downloaded by selecting one or more files or folders in the browser above and clicking the download icon ( <span class="fa fa-download"></span> ).
    Data can also be downloaded by mapping this folder as a network drive in Windows Explorer, or by using a <%=link("WebDAV").href("https://en.wikipedia.org/wiki/WebDAV")%>
    client such as <span class="nobr"><%=link("CyberDuck").href("https://cyberduck.io")%></span> or <span class="nobr"><%=link("WinSCP").href("https://winscp.net/eng/docs/introduction")%></span>.
    For details look at <%=link("Download data from Panorama Public").href("/wiki/home/page.view?name=download_public_data")%>. Use the following URL and login to connect to this folder:
    <br/>
    <br/>
    URL: <b class="bold"><span class="nobr" id="webdav_url_link"></span></b>
    <br/>
    Login email: <b class="bold">public@proteinms.net</b>
    <br/>
    Password: <b class="bold">panorama</b>
</p>
