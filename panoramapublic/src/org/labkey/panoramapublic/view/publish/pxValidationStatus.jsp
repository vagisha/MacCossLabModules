<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.panoramapublic.model.ExperimentAnnotations" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.pipeline.PipelineStatusUrls" %>
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
    String jobIdStr = context.getActionURL().getParameter("jobId");
    Integer jobId = jobIdStr != null ? Integer.valueOf(jobIdStr) : null;
%>
<div>
    <span style="font-weight:bold;text-decoration:underline;margin-right:5px;">JOB STATUS</span>
    <% if (jobId != null ) { %><span><%=link("[Job Details]", PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlDetails(getContainer(), jobId))%></span> <% } %>
    <div id="jobStatusDiv"></div>
</div>
<div id="validationStatusDiv"></div>

<script type="text/javascript">

    var jobStatusDiv = document.getElementById("jobStatusDiv");
    var validationStatusDiv = document.getElementById("validationStatusDiv");
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

    var lastJobStatus;
    function appendStatus(json)
    {
        // console.log("in appendStatus");
        if (json)
        {
            var validationStatus = json["validationStatus"];
            var jobStatus = json["jobStatus"];
            if (jobStatus && (lastJobStatus && lastJobStatus !== jobStatus))
            {
                console.log(jobStatus);
                jobStatusDiv.innerHTML = jobStatusDiv.innerHTML + "</br>" + jobStatus;
            }
            if (validationStatus)
            {
                console.log(validationStatus);
                validationStatusDiv.innerHTML = "Validation Status: </br>" + validationStatus;
            }

            if (jobStatus)
            {
                const jobStatusLc = jobStatus.toLowerCase();
                if (!(jobStatusLc === "complete" || jobStatusLc === "error" || jobStatusLc === "cancelled" || jobStatusLc === "cancelling"))
                {
                    // If task is not complete then schedule another status update in one second.
                    setTimeout(makeRequest, 1000);
                }
            }
        }
        else
        {
            setTimeout(makeRequest, 1000);
        }
    }
</script>