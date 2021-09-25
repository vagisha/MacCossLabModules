<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.panoramapublic.model.ExperimentAnnotations" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>

<labkey:errors/>

<%
    ViewContext context = getViewContext();
%>

<div id="statusDiv"></div>

<script type="text/javascript">

    var div = document.getElementById("statusDiv");
    var parameters = LABKEY.ActionURL.getParameters();

    Ext4.onReady(makeRequest);

    function makeRequest()
    {
        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('panoramapublic', 'pxValidationStatusApi.api', null, parameters),
            method: 'GET',
            success: LABKEY.Utils.getCallbackWrapper(appendStatus),
            failure: function(){setTimeout(makeRequest, 1000)}
        });
    }

    function appendStatus(json)
    {
        console.log("in appendStatus");
        if (json)
        {
            console.log("appending status");
            var status = json["status"];

            console.log(status);
            if (status.length > 0)
            {
                div.innerHTML = div.innerHTML + status + "<br>\n";
            }

            // If task is not complete then schedule another status update in one second.
            if (!json["complete"] && !json["error"])
            {
                setTimeout(makeRequest, 1000);
            }
        }
        else
        {
            setTimeout(makeRequest, 1000);
        }
    }
</script>