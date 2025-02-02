<%@ page import="org.json.JSONObject" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.testresults.view.LongTermBean" %>
<%@ page import="static org.labkey.testresults.TestResultsModule.ViewType" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    /**
     * User: Yuval Boss, yuval(at)uw.edu
     * Date: 1/14/2015
     */
    JspView<?> me = (JspView<?>) HttpView.currentView();
    LongTermBean data = (LongTermBean)me.getModelBean();
    final String contextPath = AppProps.getInstance().getContextPath();
    String viewType = data.getViewType();
    Container c = getContainer();
%>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("TestResults/css/style.css");
        dependencies.add("TestResults/css/c3.min.css");
        dependencies.add("TestResults/js/d3.min.js");
        dependencies.add("TestResults/js/c3.min.js");
    }
%>

<%@include file="menu.jsp" %>
<br />
<form action="<%=h(new ActionURL(TestResultsController.LongTermAction.class, c))%>">
    View Type: <select name="viewType" onchange="this.form.submit()">
                    <option disabled selected> -- select an option -- </option>
                    <option id="<%=h(ViewType.WEEK)%>" value="<%=h(ViewType.WEEK)%>">Week</option>
                    <option id="<%=h(ViewType.MONTH)%>" value="<%=h(ViewType.MONTH)%>">Month</option>
                    <option id="<%=h(ViewType.YEAR)%>" value="<%=h(ViewType.YEAR)%>">Year</option>
                </select>
</form>

<% if(data != null) {
    JSONObject trendsJson = data.getTrends();
    JSONObject failureJson = data.getFailuresJson();
    JSONObject runCountPerDayJson = data.getRunsPerDayJson();
    //TODO figure out run count for date range selected...
%>
<div id="duration" style="width:700px; height:400px"></div>
<div id="passes" style="width:700px; height:400px"></div>
<div id="memory" style="width:700px; height:400px"></div>
<div style="float:left; width:700px;">
    <div id="failGraph" style="width:700px; height:400px"></div>
    <table id="failureTable"></table>
</div>

    <% if (trendsJson != null) { %>
        <script src="<%=h(contextPath)%>/TestResults/js/generateTrendCharts.js"></script>
        <script type="text/javascript">
            var trendsJson = jQuery.parseJSON(<%= q(trendsJson.toString())%>);
            var failureJson = jQuery.parseJSON(<%= q(failureJson.toString())%>);
            var runCountPerDayJson = jQuery.parseJSON(<%=q(runCountPerDayJson.toString())%>);
            generateTrendCharts(trendsJson, {showSubChart: <%=viewType.equals(ViewType.YEAR) || viewType.equals(ViewType.ALLTIME)%>});

            function subchartDomainUpdated(domain) { changeData(domain); }
            function changeData(domain) {
                if(failureJson == null)
                    return;
                var start = domain[0].mmddyyyy();
                var end = domain[1];

                var currDate = new Date(domain[0]);
                var testFailCount = {};
                var totalRuns = 0;
                while(currDate < end) {
                    var currDateStr = currDate.mmddyyyy();
                    var failures = failureJson[currDateStr];
                    var runCount = runCountPerDayJson[currDateStr];
                    if (Number.isInteger(runCount))
                        totalRuns += runCount;
                    if (failures != null) {
                        for (var i = 0; i < failures.length; i++) {
                            var fail = failures[i];
                            var testname = ""+ fail.testname;
                            if(!(testname in testFailCount)) {
                                testFailCount[testname] = 0;
                            }
                            testFailCount[testname]++;
                        }
                    }
                    currDate.setDate(currDate.getDate() + 1);
                }
                var failuresByFailToRunRatio = [];
                for (var key in testFailCount)
                {
                    if (testFailCount.hasOwnProperty(key))
                    {
                        failuresByFailToRunRatio.push([key,((testFailCount[key]/totalRuns)*100).toFixed(6)]);
                    }
                }
                // list of pairs [[testname,failures/total runs %],...]
                // sort by highest value - arr[1]
                failuresByFailToRunRatio.sort(function(a, b) {
                    a = a[1];
                    b = b[1];
                    return a < b ? 1 : (a > b ? -1 : 0);
                });
                $('#failureTable').empty();
                var failureTableHTML = "<tr><td>Test</td><td>Failures per Run(%)</td></tr>";
                let url = <%=jsURL(new ActionURL(TestResultsController.ShowFailures.class, c).addParameter("viewType", "yr"))%>;
                for(var i = 0; i < failuresByFailToRunRatio.length; i++) {
                    var failure = failuresByFailToRunRatio[i];
                    url.searchParams.set('failedTest', failure[0]);
                    failureTableHTML += '<tr>' +
                            '<td><a href="' + LABKEY.Utils.encodeHtml(url.toString()) + '">'+LABKEY.Utils.encodeHtml(failure[0])+'</a></td>' +
                            '<td>'+LABKEY.Utils.encodeHtml(failure[1])+'</td>' +
                    '</tr>';
                }
                $('#failureTable').append(failureTableHTML);
            }
            Date.prototype.mmddyyyy = function() {
                var mm = this.getMonth() + 1; // getMonth() is zero-based
                var dd = this.getDate();

                return [
                    (mm>9 ? '' : '0') + mm + "/",
                    (dd>9 ? '' : '0') + dd + "/",
                    this.getFullYear()
                ].join('');
            };
        </script>
    <%}%>
<%}%>

