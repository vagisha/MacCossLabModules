<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
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
    <span style="font-weight:bold;text-decoration:underline;margin-right:5px;">Job Status</span>
    <% if (jobId != null ) { %><span><%=link("[Job Details]", PageFlowUtil.urlProvider(PipelineStatusUrls.class).urlDetails(getContainer(), jobId))%></span> <% } %>
    <span id="jobStatusSpan"></span>
</div>
<div style="margin-top:10px;" id="validationProgressDiv"></div>
<div style="margin-top:10px;"><div id="validationStatusDiv"></div></div>

<style type="text/css">
    .green {
        color:green;
    }
    .red {
        color:red;
    }
    .bold {
        font-weight: bold;
    }
    div.table-header {
        font-weight:bold;
        text-decoration: underline;
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

    // Links that helped:
    // https://docs.sencha.com/extjs/4.2.2/extjs-build/examples/build/KitchenSink/ext-theme-neptune/#row-expander-grid
    // https://docs.sencha.com/extjs/4.2.5/#!/api/Ext.grid.Panel
    // Column: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.grid.column.Column
    // https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.grid.column.Column-cfg-renderer
    // Ext.XTemplate: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.XTemplate
    // Auto resize column: http://extjs-intro.blogspot.com/2014/04/extjs-grid-panel-column-resize.html
    // Flex column property: https://stackoverflow.com/questions/8241682/what-is-meant-by-the-flex-property-of-any-extjs4-layout/8242860
    // https://forum.sencha.com/forum/showthread.php?247657-4-1-1-How-to-set-style-for-grid-cell-content
    // https://stackoverflow.com/questions/5006273/extjs-xtemplate

    var htmlEncode = Ext4.util.Format.htmlEncode;

    var jobStatusSpan = document.getElementById("jobStatusSpan");
    var validationProgressDiv = document.getElementById("validationProgressDiv");
    var validationStatusDiv = document.getElementById("validationStatusDiv");
    var parameters = LABKEY.ActionURL.getParameters();
    var lastJobStatus = "";

    Ext4.onReady(makeRequest);

    function makeRequest() {
        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('panoramapublic', 'pxValidationStatusApi.api', null, parameters),
            method: 'GET',
            success: LABKEY.Utils.getCallbackWrapper(displayStatus),
            failure: function () {
                setTimeout(makeRequest, 1000)
            }
        });
    }

    function displayStatus(json) {

        function getValidationProgressHtml(validationProgress) {
            var html = "";
            if (validationProgress) {
                for (var i = 0; i < validationProgress.length; i++) {
                    html += htmlEncode(validationProgress[i]) + "</br>";
                }
            }
            return html;
        }

        if (json) {
            var jobStatus = json["jobStatus"];
            var validationProgress = json["validationProgress"];
            var validationStatus = json["validationStatus"];

            if (jobStatus && lastJobStatus !== jobStatus) {
                // console.log(jobStatus);
                jobStatusSpan.innerHTML = jobStatus;
                lastJobStatus = jobStatus;
            }
            if (validationProgress) {
                validationProgressDiv.innerHTML = getValidationProgressHtml(validationProgress);
            }
            if (validationStatus) {
                validationProgressDiv.innerHTML = "";
                displayValidationStatus(validationStatus);
            }

            if (jobStatus) {
                const jobStatusLc = jobStatus.toLowerCase();
                if (!(jobStatusLc === "complete" || jobStatusLc === "error" || jobStatusLc === "cancelled" || jobStatusLc === "cancelling")) {
                    // If task is not complete then schedule another status update in one second.
                    setTimeout(makeRequest, 1000);
                }
            }
        }
        else {
            setTimeout(makeRequest, 1000);
        }
    }

    function displayValidationStatus(json) {
        Ext4.create('Ext.panel.Panel', {
            title: 'Data Validation Status',
            renderTo: 'validationStatusDiv',
            items: [validationInfo(json["validation"]), skylineDocsInfo(json), modificationsInfo(json), spectralLibrariesInfo(json)]
        });
    }

    function validationInfo(json) {

        function getStatusCls(statusId) {
            return statusId === 3 ? 'bold green' : (statusId !== -1 ? 'bold red' : '');
        }

        function getStatusValidHtml(json) {
            return '<div>The data is valid for a "complete" ProteomeXchange submission.  ' +
                    'You can view the validation details below.';
        }

        function problems(json) {
            var problems = '';
            if (json["modificationsValid"] === false) problems += '<li>Modifications without a Unimod ID</li>';
            if (json["sampleFilesValid"] === false) problems += '<li>Missing raw data files</li>';
            if (json["specLibsComplete"] === false) problems += '<li>Incomplete spectral library data</li>';
            return '</br>Problems found: <ul>' + problems + '</ul>';
        }

        function getIncompleteDataHtml(json) {
            return '<div>The data can be assigned a ProteomeXchange ID but it is not valid for a "complete" ProteomeXchange submission. ' +
                    problems(json) +
                    'You can view the validation details in the tables below. ' +
                    'For a "complete" submission try submitting after fixing the problems reported. ' +
                    'Otherwise, you can continue with an incomplete submission.';
        }

        function getStatusInvalidHtml(json) {
            return '<div>The data cannot be assigned a ProteomeXchange ID. ' +
                    problems(json) +
                    'You can view the validation details in the tables below. ' +
                    'Try submitting the data after fixing the problems reported. ' +
                    'Otherwise, you can submit the data without a ProteomeXchange ID. ';
        }

        function getStatusDetails(json) {
            const statusId = json["status"];
            return statusId === 3 ? getStatusValidHtml(json)
                    : statusId === 2 ? getIncompleteDataHtml(json) : getStatusInvalidHtml(json);
        }

        function getButtonText(json) {
            const statusId = json["status"];
            return statusId === 3 ? "Continue Submission"
                    : statusId === 2 ? "Continue with an Incomplete PX Submission" : "Continue without a ProteomeXchange ID";
        }

        function getButtonLink(json) {
            var url = LABKEY.ActionURL.buildURL('targetedms', 'submitExperiment.view', null,
                    {id: json["experimentAnnotationsId"], validationId: json["id"], "doSubfolderCheck": false, "validateForPx": false});
            return url;
        }

        if (json) {
            return {
                xtype:  'panel',
                layout: {type: 'anchor', align: 'left'},
                style:  {margin: '10px'},
                items:  [
                            {xtype: 'component', padding: '10, 5, 0, 5', html: 'Folder: ' + htmlEncode(json["folder"])},
                            {
                                xtype:   'component',
                                padding: '0, 5, 10, 5',
                                cls:     getStatusCls(json['statusId']),
                                html:    'Status: ' + htmlEncode(json["status"])
                            },
                            {
                                xtype:   'panel',
                                padding: '0, 5, 10, 5',
                                border: false,
                                layout: {type: 'anchor', align: 'left'},
                                items: [
                                    {xtype: 'component', margin: '0 0 5 0', html: getStatusDetails(json)},
                                    {xtype: 'button', text: getButtonText(json), href: getButtonLink(json)}
                                ]
                            }
                        ]
            };
        }
        return {xtype: 'label', text: 'Missing JSON property "validation"'};
    }

    function link(text, href, cssCls) {
        const cls = cssCls ? ' class="' + cssCls + '" ' : '';
        return '<a ' + cls + ' href="' + htmlEncode(href) + '" target="_blank">' + htmlEncode(text) + '</a>';
    }

    function documentLink(documentName, containerPath, runId) {
        var url = LABKEY.ActionURL.buildURL('targetedms', 'showPrecursorList.view', containerPath, {id: runId});
        return link(documentName, url);
    }

    function unimodLink(unimodId, cls) {
        return link("UNIMOD:" + unimodId, "https://www.unimod.org/modifications_view.php?editid1=" + unimodId, cls);
    }

    function invalid() {
        return '<span class="red bold">INVALID</span>';
    }

    function modificationsInfo(json) {

        if (json["modifications"]) {
            const modificationsStore = Ext4.create('Ext.data.Store', {
                storeId: 'modificationsStore',
                fields:  ['id', 'unimodId', 'name', 'valid', 'modType', 'dbModId', 'modType', 'documents', 'possibleUnimodMatches'],
                data:    json,
                proxy:   { type: 'memory', reader: { type: 'json', root: 'modifications' }},
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
                store:    modificationsStore,
                storeId: 'modificationsStore',
                padding:  10,
                disableSelection: true,
                collapsible: true,
                animCollapse: false,
                viewConfig: {enableTextSelection: true},
                title: 'Modifications',
                columns: {
                    items: [
                    {
                        text: 'Unimod Id',
                        dataIndex: 'unimodId',
                        width: 120,
                        renderer: function (value) {
                            // Can do metadata.style = "color:green;" or metadata.tdCls = 'green'
                            if (value) return unimodLink(value, 'green bold');
                            else return invalid();
                        }
                    },
                    {
                        text: 'Name',
                        dataIndex: 'name',
                        flex: 1,
                        renderer: function (v) { return htmlEncode(v); }
                    },
                    {
                        text: 'Document Count',
                        width: 150,
                        dataIndex: 'documents',
                        renderer: function (v) { return v.length; }
                    }],
                    defaults: {
                        sortable: false,
                        hideable: false
                    }
                },
                plugins: [{
                    ptype: 'rowexpander',
                    rowBodyTpl: new Ext4.XTemplate(
                            // Ext.XTemplate: https://docs.sencha.com/extjs/4.2.1/#!/api/Ext.XTemplate
                            '<div style="background-color:#f1f1f1;padding:15px 10px 15px 10px;margin-top:5px;font-size:10pt;">',

                            '<tpl if="possibleUnimodMatches.length &gt; 0">',
                            '<div class="table-header">Possible Unimod Matches</div>',
                            '<table style="border:1px solid black; padding:5px; margin-top:5px;">',
                            '<thead><tr><th style="border:1px solid black; padding:5px;">Unimod ID</th>',
                            '<th style="border:1px solid black; padding:5px;">Name</th>',
                            '<th style="border:1px solid black; padding:5px;">Composition</th>',
                            '<th style="border:1px solid black; padding:5px;">Sites</th>',
                            '</tr></thead>',
                            '<tbody>',
                            '<tpl for="possibleUnimodMatches">',
                            '<tr>',
                            '<td style="border:1px solid black; padding:5px;">{[this.renderUnimodLink(values)]}</td>',
                            '<td style="border:1px solid black; padding:5px;">{name}</td>',
                            '<td style="border:1px solid black; padding:5px;">{formula}</td>',
                            '<td style="border:1px solid black; padding:5px;">{sites}</td>',
                            '</tr>',
                            '</tpl>',
                            '</tbody></table>',
                            '</tpl>',


                            '<div style="font-weight:bold; margin-top:10px; margin-bottom:10px; text-decoration: underline;">Skyline Documents with Modification</div>',
                            '<table style="border:1px solid black; padding:5px; margin-top:5px;">',
                            '<tpl for="documents">',
                            '<tr>',
                            '<td style="border:1px solid black; padding:5px;">{[this.renderDocLink(values)]}</td>',
                            '<td style="border:1px solid black; padding:5px;">{[this.renderPeptidesLink(values, parent.dbModId, parent.modType)]}</td>',
                            '</tr>',
                            '</tpl>',
                            '</table>',
                            '</div>',
                            '</div>',
                            {
                                renderDocLink: function (doc) {
                                    return documentLink(doc.name, doc.container, doc.runId);
                                },
                                renderPeptidesLink: function (doc, dbModId, modType) {
                                    var params = {
                                        'schemaName': 'targetedms',
                                        'query.PeptideId/PeptideGroupId/RunId~eq': doc.runId,
                                        'query.queryName': queryName = modType === 'STRUCTURAL' ? 'PeptideStructuralModification' : 'PeptideIsotopeModification'
                                    };
                                    if (modType === 'STRUCTURAL') params['query.StructuralModId/Id~eq'] = dbModId;
                                    else params['query.IsotopeModId/Id~eq'] = dbModId;
                                    return link('[PEPTIDES]', LABKEY.ActionURL.buildURL('query', 'executeQuery.view', doc.container, params));
                                },
                                renderUnimodLink: function (values) {
                                    return unimodLink(values.unimodId);
                                }
                            }
                    )
                }]
            });
        }
        return {xtype: 'label', text: 'Missing JSON property "modifications"'};
    }

    function skylineDocsInfo(json) {
        if (json["skylineDocuments"]) {
            var skylineDocsStore = Ext4.create('Ext.data.Store', {
                storeId: 'skylineDocsStore',
                fields: ['id', 'runId', 'name', 'container', 'valid', 'sampleFiles'],
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
                columns: [
                    {
                        text: 'Name',
                        dataIndex: 'name',
                        flex: 1, // https://stackoverflow.com/questions/8241682/what-is-meant-by-the-flex-property-of-any-extjs4-layout/8242860
                        sortable: false,
                        hideable: false,
                        renderer: function (value, metadata, record) {
                            return documentLink(value, record.get('container'), record.get('runId'));
                        }
                    },
                    {
                        text: 'Sample File Count',
                        dataIndex: 'sampleFiles',
                        width: 150,
                        sortable: false,
                        hideable: false,
                        renderer: function (value, metadata, record) {
                            var url = LABKEY.ActionURL.buildURL('targetedms', 'showReplicates.view', record.get('container'), {id: record.get('runId')});
                            return link(value.length, url);
                        }
                    },
                    {
                        text: 'Status',
                        sortable: false,
                        hideable: false,
                        width: 150,
                        dataIndex: 'valid',
                        renderer: function (value, metadata) {
                            metadata.tdCls = value ? 'valid' : 'invalid';
                            return value ? "COMPLETE" : "INCOMPLETE";
                        }
                    },
                    {
                        text: '',
                        sortable: false,
                        hideable: false,
                        width: 200,
                        dataIndex: 'container',
                        renderer: function (value, metadata, record) {
                            // var iconCls = 'fa-file fa-arrow-up labkey-fa-upload-files';
                            // metadata.tdCls = iconCls;
                            metadata.style = 'text-align: center';
                            if (record.get('valid') === false) {
                                var url = LABKEY.ActionURL.buildURL('project', 'begin.view', value, {pageId: 'Raw Data'});
                                return '<a href="' + htmlEncode(url) + '" target="_blank">[Upload]</a>';
                            }
                            return "";
                            // return link("Upload Files", url, 'iconUpload');
                        }
                    }],
                plugins: [{
                    ptype: 'rowexpander',
                    rowBodyTpl: new Ext4.XTemplate(
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
                                renderStatus: function (sampleFile) {
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

    function spectralLibrariesInfo(json) {
        if (json["spectrumLibraries"]) {
            var specLibStore = Ext4.create('Ext.data.Store', {
                storeId: 'specLibStore',
                fields: ['id', 'libName', 'libType', 'fileName', 'size', 'valid', 'status', 'spectrumFiles', 'idFiles', 'documents'],
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
                        renderer: function (v) {
                            return htmlEncode(v);
                        }
                    },
                    {
                        text: 'File Name',
                        dataIndex: 'fileName',
                        flex: 3,
                        sortable: false,
                        hideable: false,
                        renderer: function (v) {
                            return htmlEncode(v);
                        }
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
                        width: 120,
                        sortable: false,
                        hideable: false,
                        renderer: function (value) {
                            return value.length;
                        }
                    },
                    {
                        text: 'Peptide Id Files',
                        dataIndex: 'idFiles',
                        width: 120,
                        sortable: false,
                        hideable: false,
                        renderer: function (value) {
                            return value.length;
                        }
                    },
                    {
                        text: 'Documents',
                        dataIndex: 'documents',
                        width: 100,
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
                        flex: 2,
                        renderer: function (value, metadata) {
                            metadata.tdCls = value === true ? 'valid' : 'invalid';
                            return value === true ? 'COMPLETE' : 'INCOMPLETE';
                        }
                    }],
                plugins: [{
                    ptype: 'rowexpander',
                    rowBodyTpl: new Ext4.XTemplate(
                            '<div style="background-color:#f1f1f1;padding:5px;margin-top:5px;font-size:10pt;">',

                            '<div style="font-weight:bold; margin-top:10px;">Status: {[this.renderLibraryStatus(values.status, values.valid)]} [Update Library]</div>',

                            '<tpl if="spectrumFiles.length &gt; 0">',
                            '<div style="font-weight:bold; margin-top:10px; text-decoration: underline;">Spectrum Files</div>',
                            '<table style="border:1px solid black; padding:5px; margin-top:5px;">',
                            '<tpl for="spectrumFiles">',
                            '<tr><td style="border:1px solid black; padding:5px;">{name}</td><td style="border:1px solid black; padding:5px;">{[this.renderStatus(values)]}</td></tr>',
                            '</tpl>',
                            '</table>',
                            '</tpl>',

                            '<tpl if="idFiles.length &gt; 0">',
                            '<div style="font-weight:bold; margin-top:10px; text-decoration: underline;">Peptide Id Files</div>',
                            '<table style="border:1px solid black; padding:5px;  margin-top:5px;">',
                            '<tpl for="idFiles">',
                            '<tr><td style="border:1px solid black; padding:5px;">{name}</td><td style="border:1px solid black; padding:5px;">{[this.renderStatus(values)]}</td></tr>',
                            '</tpl>',
                            '</table>',
                            '</tpl>',

                            '<div style="font-weight:bold; margin-top:10px; margin-bottom:10px; text-decoration: underline;">Skyline Documents With the Library</div>',
                            '<ul>',
                            '<tpl for="documents">',
                            '<li>{[this.renderDocLink(values)]}</li>',
                            '</tpl>',
                            '</ul>',

                            '</div>',
                            {
                                renderStatus: function (file) {
                                    var cls = file.found === true ? 'valid' : 'invalid';
                                    var status = "FOUND";
                                    if (file.found === false) status = "MISSING";
                                    if (file.ambiguous === true) status = "AMBIGUOUS";
                                    return '<span class="' + cls + '">' + status + '</span>';
                                },
                                renderDocLink: function (doc) {
                                    return documentLink(doc.name, doc.container, doc.runId);
                                },
                                renderLibraryStatus: function (status, valid) {
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

</script>