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
    .green-text {
        color:green;
    }
    .red-text {
        color:red;
    }
    .valid {
        color:green;
        font-weight: bold;
    }
    .invalid {
        color:red;
        font-weight: bold;
    }
    .invalid-bkground {
        background-color: #FFF5EE;
    }
</style>

<script type="text/javascript">

    // Useful links:
    // https://docs.sencha.com/extjs/4.2.2/extjs-build/examples/build/KitchenSink/ext-theme-neptune/#row-expander-grid
    // Column: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.grid.column.Column
    // Ext.XTemplate: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.XTemplate
    // Auto resize column: http://extjs-intro.blogspot.com/2014/04/extjs-grid-panel-column-resize.html
    // Flex column property: https://stackoverflow.com/questions/8241682/what-is-meant-by-the-flex-property-of-any-extjs4-layout/8242860
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
            items: [validationInfo(json["validation"]), modificationsInfo(json), skylineDocsInfo(json), spectralLibrariesInfo(json)]
        });
        // validationInfo(json["validation"]), modificationsInfo(json["modifications"]),skylineDocsInfo(json["skylineDocuments"]), spectralLibrariesInfo(json["spectrumLibraries"])
    }

    function validationInfo(json)
    {
        if(json)
        {
            return {
                xtype: 'panel',
                // bodyPadding: 5,
                layout: {type: 'vbox', align: 'left'},
                defaults: {
                    labelWidth: 150,
                    // width: 500,
                    labelStyle: 'background-color: #E0E6EA; padding: 5px;'
                },
                items: [
                        {xtype: 'label', text: 'Data validation for experiment in folder "' + h(json["folder"]) + '"'},
                        {xtype: 'label', text: 'Status: ' + h(json["status"])}]
            };
        }
        return {xtype: 'label', text: 'Missing JSON for property "validation"'};
    }

    function link(text, href, cssCls) {
        const cls = cssCls ? ' class="' + cssCls + '" ' : '';
        return '<a ' + cls + 'href="' + h(href) + '" target="_blank">' + h(text) + '</a>';
    }

    function documentLink(documentName, containerPath, runId) {
        var url = LABKEY.ActionURL.buildURL('targetedms', 'showPrecursorList.view', containerPath, {id: runId});
        return link(documentName, url);
    }

    function invalid() {
        return '<span style="color:red; font-weight:bold">INVALID</span>';
    }

    function modificationsInfo(json)
    {
        if(json["modifications"])
        {
            const modificationsStore = Ext4.create('Ext.data.Store', {
                storeId: 'modificationsStore',
                fields: ['id', 'unimodId', 'name', 'valid', 'modType', 'dbModId', 'modType', 'documents'],
                data: json,
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'modifications'
                    }
                },
                sorters: [
                    {
                        property: 'valid',
                        direction: 'ASC'
                    },
                    {
                        property: 'modType',
                        direction: 'DESC' // Structural modifications first
                    },
                    {
                        property: 'unimodId',
                        direction: 'ASC'
                    }
                ]
            });

            return Ext4.create('Ext.grid.Panel', {
                store: modificationsStore,
                storeId: 'modificationsStore',
                padding: 10,
                disableSelection: true,
                title: 'Modifications',
                columns: [{
                    text: 'Unimod Id',
                    dataIndex: 'unimodId',
                    width: 120,
                    sortable: false,
                    hideable: false,
                    renderer: function(value, metadata, record) {
                        // https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.grid.column.Column-cfg-renderer
                        var cls = record.get('valid') ? 'valid' : 'invalid';
                        metadata.tdCls = cls;
                        if (value) return link("UNIMOD:" + value, "https://www.unimod.org/modifications_view.php?editid1=" + value, cls);
                        else return "INVALID";
                    }
                }, {
                    text: 'Name',
                    dataIndex: 'name',
                    flex: 1, // https://stackoverflow.com/questions/8241682/what-is-meant-by-the-flex-property-of-any-extjs4-layout/8242860
                    sortable: false,
                    hideable: false,
                    renderer: function(v) { return h(v); }
                }, {
                    text: 'Document Count',
                    sortable: false,
                    hideable: false,
                    width: 150,
                    dataIndex: 'documents',
                    renderer: function(v) { return v.length; }
                }],
                plugins: [{
                    ptype: 'rowexpander',
                    rowBodyTpl : new Ext4.XTemplate(
                            // Ext.XTemplate: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.XTemplate
                            '<div style="background-color:#f1f1f1;padding:15px 5px 5px 5px;margin-top:5px;font-size:10pt;">',
                            '<ul>',
                            '<tpl for="documents">',
                            // https://stackoverflow.com/questions/5006273/extjs-xtemplate
                            '<li>{[this.renderDocLink(values)]} - {[this.renderPeptidesLink(values, parent.dbModId)]}</li>',
                            '</tpl>',
                            '</ul>',
                            '</div>',
                            {
                                renderDocLink: function(doc){
                                    return documentLink(doc.name, doc.container, doc.runId);
                                },
                                renderPeptidesLink: function(doc, dbModId){
                                    var url = LABKEY.ActionURL.buildURL('query', 'executeQuery.view', doc.container,
                                            {'schemaName': 'targetedms', 'query.queryName': 'PeptideStructuralModification',
                                                'query.PeptideId/PeptideGroupId/RunId~eq': doc.runId,
                                                'query.StructuralModId/Id~eq': dbModId})
                                    return link('[PEPTIDES]', url);
                                }
                            }
                            )
                }],
                collapsible: true,
                animCollapse: false
            });
        }
        return {xtype: 'label', text: 'Missing JSON for property "modifications"'};
    }

    function skylineDocsInfo(json)
    {
        if(json["skylineDocuments"])
        {
            var skylineDocsStore = Ext4.create('Ext.data.Store', {
                storeId:'skylineDocsStore',
                fields:['id', 'runId', 'name', 'container', 'valid', 'sampleFiles'],
                data: json,
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'skylineDocuments'
                    }
                },
                sorters: [
                    {
                        property: 'valid',
                        direction: 'ASC'
                    },
                    {
                        property: 'container',
                        direction: 'ASC'
                    },
                    {
                        property: 'runId',
                        direction: 'ASC'
                    }
                ]
            });

            return Ext4.create('Ext.grid.Panel', {
                store: skylineDocsStore,
                storeId: 'skylineDocsStore',
                padding: 10,
                disableSelection: true,
                title: 'Skyline Document Sample Files',
                columns: [{
                    text: 'Name',
                    dataIndex: 'name',
                    flex: 1, // https://stackoverflow.com/questions/8241682/what-is-meant-by-the-flex-property-of-any-extjs4-layout/8242860
                    sortable: false,
                    hideable: false,
                    renderer: function(value, metadata, record) {
                        return documentLink(value, record.get('container'), record.get('runId'));
                    }
                }, {
                    text: 'Sample File Count',
                    dataIndex: 'sampleFiles',
                    width:150,
                    sortable: false,
                    hideable: false,
                    renderer: function(value, metadata, record) {
                        var url = LABKEY.ActionURL.buildURL('targetedms', 'showReplicates.view', record.get('container'), {id: record.get('runId')});
                        return link(value.length, url);
                    }
                }, {
                    text: 'Status',
                    sortable: false,
                    hideable: false,
                    width: 150,
                    dataIndex: 'valid',
                    renderer: function(value, metadata) {
                        metadata.tdCls = value ? 'valid' : 'invalid';
                        return value ? "COMPLETE" : "INCOMPLETE"; }
                }],
                plugins: [{
                    ptype: 'rowexpander',
                    rowBodyTpl : new Ext4.XTemplate(
                            // Ext.XTemplate: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.XTemplate
                            '<div style="background-color:#f1f1f1;padding:5px;margin-top:5px;font-size:10pt;">',
                            '<table style="border:1px solid black; padding:5px;">',
                            '<tpl for="sampleFiles">',
                            // https://stackoverflow.com/questions/5006273/extjs-xtemplate
                            '<tr><td style="border:1px solid black; padding:5px;">{name}</td><td style="border:1px solid black; padding:5px;">{[this.renderStatus(values)]}</td></tr>',
                            '</tpl>',
                            '</table>',
                            '</div>',
                            {
                                renderStatus: function(sampleFile){
                                    var cls = sampleFile.found === true ? 'valid' : 'invalid';
                                    var status = "FOUND";
                                    if (sampleFile.found === false) status = "MISSING";
                                    if (sampleFile.ambiguous === true) status = "AMBIGUOUS";
                                    return '<span class="' + cls + '">' + status + '</span>';
                                }
                            }
                    )
                }],
                collapsible: true,
                animCollapse: false
            });
        }
        return {xtype: 'label', text: 'Missing JSON for property "skylineDocuments"'};
    }

    function spectralLibrariesInfo(json)
    {
        if(json["spectrumLibraries"])
        {
            var specLibStore = Ext4.create('Ext.data.Store', {
                storeId:'specLibStore',
                fields:['id', 'libName', 'libType', 'fileName', 'size', 'valid', 'status', 'spectrumFiles', 'idFiles', 'documents'],
                data: json,
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'spectrumLibraries'
                    }
                },
                sorters: [
                    {
                        property: 'valid',
                        direction: 'ASC'
                    },
                    {
                        property: 'libType',
                        direction: 'ASC'
                    }
                ]
            });

            return Ext4.create('Ext.grid.Panel', {
                store: specLibStore,
                storeId: 'specLibStore',
                padding: 10,
                disableSelection: true,
                title: 'Spectral Libraries',
                columns: [
                    {
                        text: 'Name',
                        dataIndex: 'libName', // TODO: link to a Spectrum Library webpart
                        flex: 3,
                        sortable: false,
                        hideable: false,
                        renderer: function(v) { return h(v); }
                    },
                    {
                        text: 'File Name',
                        dataIndex: 'fileName',
                        flex: 3,
                        sortable: false,
                        hideable: false,
                        renderer: function(v) { return h(v); }
                    },
                    {
                        text: 'File Size',
                        dataIndex: 'size',
                        sortable: false,
                        hideable: false
                    },
                    {
                        text: 'Spectrum Files',
                        dataIndex: 'spectrumFiles',
                        width: 150,
                        sortable: false,
                        hideable: false,
                        renderer: function (value) {
                            return value.length;
                        }
                    },
                    {
                        text: 'Peptide Id Files',
                        dataIndex: 'idFiles',
                        width: 150,
                        sortable: false,
                        hideable: false,
                        renderer: function (value) {
                            return value.length;
                        }
                    },
                    {
                        text: 'Documents',
                        dataIndex: 'documents',
                        width: 150,
                        sortable: false,
                        hideable: false,
                        renderer: function (value) {
                            return value.length;
                        }
                    },
                    {
                        text: 'Status',
                        dataIndex: 'valid',
                        sortable: false,
                        hideable: false,
                        flex: 1,
                        renderer: function (value, metadata)
                        {
                            metadata.tdCls = value === true ? 'valid' : 'invalid';
                            return value === true ? 'VALID' : 'INVALID';
                        }
                    }],
                plugins: [{
                    ptype: 'rowexpander',
                    rowBodyTpl : new Ext4.XTemplate(
                            '<div style="background-color:#f1f1f1;padding:5px;margin-top:5px;font-size:10pt;">',

                            '<div style="font-weight:bold; margin-top:10px;">Status: {[this.renderLibraryStatus(values.status, values.valid)]}</div>',

                            '<div style="font-weight:bold; margin-top:10px; text-decoration: underline;">Spectrum Files</div>',
                            '<table style="border:1px solid black; padding:5px; margin-top:5px;">',
                            '<tpl for="spectrumFiles">',
                            '<tr><td style="border:1px solid black; padding:5px;">{name}</td><td style="border:1px solid black; padding:5px;">{[this.renderStatus(values)]}</td></tr>',
                            '</tpl>',
                            '</table>',

                            '<div style="font-weight:bold; margin-top:10px; text-decoration: underline;">Peptide Id Files</div>',
                            '<table style="border:1px solid black; padding:5px;  margin-top:5px;">',
                            '<tpl for="idFiles">',
                            '<tr><td style="border:1px solid black; padding:5px;">{name}</td><td style="border:1px solid black; padding:5px;">{[this.renderStatus(values)]}</td></tr>',
                            '</tpl>',
                            '</table>',

                            '<div style="font-weight:bold; margin-top:10px; margin-bottom:10px; text-decoration: underline;">Skyline Documents With the Library</div>',
                            '<ul>',
                            '<tpl for="documents">',
                            '<li>{[this.renderDocLink(values)]}</li>',
                            '</tpl>',
                            '</ul>',

                            '</div>',
                            {
                                renderStatus: function(file){
                                    var cls = file.found === true ? 'valid' : 'invalid';
                                    var status = "FOUND";
                                    if (file.found === false) status = "MISSING";
                                    if (file.ambiguous === true) status = "AMBIGUOUS";
                                    return '<span class="' + cls + '">' + status + '</span>';
                                },
                                renderDocLink: function(doc){
                                    return documentLink(doc.name, doc.container, doc.runId);
                                },
                                renderLibraryStatus: function(status, valid){
                                    var cls = valid === true ? 'valid' : 'invalid';
                                    return '<span class="' + cls + '">' + status + '</span>';
                                },
                            }
                    )
                }],
                collapsible: true,
                animCollapse: false
            });
        }
        return {xtype: 'label', text: 'Missing JSON for property "spectrumLibraries"'};
    }

    function getVal(jsonProp)
    {
        return jsonProp ? h(jsonProp) : "NOT FOUND";
    }
</script>