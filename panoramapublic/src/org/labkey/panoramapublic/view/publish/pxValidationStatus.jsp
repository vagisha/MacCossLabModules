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
<div>
    <span style="font-weight:bold;text-decoration:underline;margin-right:5px;">VALIDATION DETAILS</span>
    </br>
    <div id="validationStatusDiv"></div>
</div>

<style type="text/css">
    .valid {
        color:green;
        font-weight: bold;
    }
    .invalid {
        color:red;
        background-color: #FFF5EE;
        font-weight: bold;
    }
    .invalid-bkground {
        background-color: #FFF5EE;
    }
</style>

<script type="text/javascript">

    var h = Ext4.util.Format.htmlEncode;

    var jobStatusDiv = document.getElementById("jobStatusDiv");
    var validationStatusDiv = document.getElementById("validationStatusDiv");
    var parameters = LABKEY.ActionURL.getParameters();

    Ext4.onReady(makeRequest);

    function makeRequest()
    {
        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('panoramapublic', 'pxValidationStatusApi.api', null, parameters),
            method: 'GET',
            success: LABKEY.Utils.getCallbackWrapper(displayStatus),
            failure: function(){setTimeout(makeRequest, 1000)}
        });
    }

    var lastJobStatus;

    function displayStatus(json)
    {
        // console.log("in displayStatus");
        if (json)
        {
            var validationStatus = json["validationStatus"];
            var jobStatus = json["jobStatus"];
            if (jobStatus && (lastJobStatus && lastJobStatus !== jobStatus))
            {
                console.log(jobStatus);
                jobStatusDiv.innerHTML = jobStatusDiv.innerHTML + "</br>" + jobStatus;
                lastJobStatus = jobStatus;
            }
            if (validationStatus)
            {
                console.log(validationStatus);
                displayValidationStatus(validationStatus);
                // validationStatusDiv.innerHTML = getValidationStatusHtml(validationStatus);
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

    function displayValidationStatus(json)
    {
        var validationInfoPanel = Ext4.create('Ext.panel.Panel',{
            title: 'Data Validation Status',
            renderTo: 'validationStatusDiv',
            items: [validationInfo(json["validation"]), modificationsInfo(json["modifications"])]
        });
        // validationInfo(json["validation"]), modificationsInfo(json["modifications"]),skylineDocsInfo(json["skylineDocuments"]), spectralLibrariesInfo(json["spectrumLibraries"])
    }

    function validationInfo(json)
    {
        if(json)
        {
            return {
                xtype: 'panel',
                bodyPadding: 5,
                layout: {type: 'vbox', align: 'left'},
                defaults: {
                    labelWidth: 150,
                    width: 500,
                    labelStyle: 'background-color: #E0E6EA; padding: 5px;'
                },
                items: [
                        {xtype: 'label', text: 'Data validation for experiment in folder "' + h(json["folder"]) + '"'},
                        {xtype: 'label', text: 'Status: ' + h(json["status"])}]
            };
        }
        return {xtype: 'label', text: 'Missing JSON for property "validation"'};
    }

    function modificationsInfo(json)
    {
        if(json)
        {
            return {
                xtype: 'panel',
                bodyPadding: 5,
                layout: {
                    type: 'table',
                    columns: 3,
                    tableAttrs: {
                        style: {
                            width: '40%'
                        }
                    },
                    tdAttrs: {
                        style: {
                            border: '1px solid black'
                        }
                    }
                },
                defaults: {
                    bodyStyle: 'padding:5px;border:none'
                },
                items: function() {
                    var modsValid = [];
                    var modsInvalid = [];
                    Ext4.each(json, function(mod) {
                        if (mod.valid === true)
                        {
                            modsValid.push({
                                xtype: 'label',
                                text: h(mod.unimod),
                                cls: 'valid'
                            });
                            modsValid.push({
                                html: h(mod.name) // TODO: link to Unimod e.g. https://www.unimod.org/modifications_view.php?editid1=6
                            });
                            modsValid.push({
                                html: function(){
                                    var docList = "<ul>";
                                    Ext4.each(mod["documents"], function(doc){
                                        docList += "<li>" + doc.name + "</li>";
                                    }, this);
                                    docList += "</ul>";
                                    return docList;
                                }()
                            });
                        }
                        else
                        {
                            modsInvalid.push({
                                xtype: 'label',
                                text: "INVALID",
                                cls: 'invalid',
                                cellCls: 'invalid-bkground'
                            });
                            modsInvalid.push({
                                html: h(mod.name)
                            });
                            modsInvalid.push({
                                html: function(){
                                    var docCount = mod["documents"].length;
                                    var docList = docCount > 1 ? "<ul>" : "";
                                    Ext4.each(mod["documents"], function(doc){
                                        var docLink = LABKEY.ActionURL.buildURL('targetedms', 'showPrecursorList', null, {id: doc.runId});
                                        var queryParams = {'schemaName': 'targetedms',
                                            'query.queryName': 'PeptideStructuralModification',
                                            'query.PeptideId/PeptideGroupId/RunId~eq': doc.runId,
                                            'query.StructuralModId/Id~eq': 1
                                        };
                                        var peptidesLink = LABKEY.ActionURL.buildURL("query", "executeQuery", null, queryParams);
                                        if (docCount > 1) docList += "<li><a href='" + docLink + "'>" + doc.name + "</a></li>";
                                        else docList += "<a href='" + peptidesLink + "'>" + doc.name + "</a>";
                                    }, this);
                                    if (docCount > 1) docList += "</ul>";
                                    return docList;
                                }(),
                                cellCls: 'invalid-bkground'
                            });
                        }
                    }, this);
                    console.log(modsValid);
                    console.log(modsInvalid);
                    return modsValid.concat(modsInvalid);
                }()
            };
        }
        return {xtype: 'label', text: 'Missing JSON for property "modifications"'};
    }

    function spectralLibrariesInfo(json)
    {
        if(json)
        {

        }
        return {xtype: 'label', text: 'Missing JSON for property "skylineDocuments"'};
    }

    function skylineDocsInfo(json)
    {
        if(json)
        {
            return {
                xtype: 'panel',
                bodyPadding: 5,
                layout: {type: 'vbox', align: 'left'},
                defaults: {
                    labelWidth: 150,
                    width: 500,
                    labelStyle: 'background-color: #E0E6EA; padding: 5px;'
                },
                items: function() {
                    var docsValid = [];
                    var docsInvalid = [];
                    Ext4.each(json, function(doc) {
                        if (doc.valid === true)
                        {
                            docsInvalid.push({
                                xtype: 'label',
                                text: doc.name,
                            });
                        }
                        else
                        {
                            docsInvalid.push({
                                xtype: 'label',
                                text: doc.name,
                            });
                        }
                    }, this);
                    console.log(modsValid);
                    console.log(modsInvalid);
                    return modsValid.concat(modsInvalid);
                }()
            };
        }
        return {xtype: 'label', text: 'Missing JSON for property "spectrumLibraries"'};
    }

    function getVal(jsonProp)
    {
        return jsonProp ? h(jsonProp) : "NOT FOUND";
    }
</script>