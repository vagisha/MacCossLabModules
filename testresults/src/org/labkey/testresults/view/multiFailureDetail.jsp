<%@ page import="org.json.JSONObject" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.testresults.model.RunDetail" %>
<%@ page import="org.labkey.testresults.model.TestFailDetail" %>
<%@ page import="org.labkey.testresults.view.TestsDataBean" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.TreeMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    /**
     * User: Yuval Boss, yuval(at)uw.edu
     * Date: 10/05/2015
     */
    JspView<?> me = (JspView<?>) HttpView.currentView();
    TestsDataBean data = (TestsDataBean)me.getModelBean();
    final String contextPath = AppProps.getInstance().getContextPath();
    String viewType = data.getViewType();
    Container c = getContainer();
    RunDetail[] runs = data.getStatRuns();
    if(runs.length > 1) {
        Arrays.sort(runs, new Comparator<RunDetail>()
        {
            @Override
            public int compare(RunDetail o1, RunDetail o2)
            {
                return o2.getRevision() - o1.getRevision();
            }
        });
    }
    Map<String, List<TestFailDetail>> languageFailures = new TreeMap<>();
    TestFailDetail[] fails = data.getFailures();
    for(TestFailDetail f: fails) {
        if(!languageFailures.containsKey(f.getTestName()))
            languageFailures.put(f.getTestName(), new ArrayList<>());
        languageFailures.get(f.getTestName()).add(f);
    }
    Map<String, Map<String, Double>> languageBreakdown = data.getLanguageBreakdown(languageFailures);
    List<String> users = new ArrayList<>();

    DateFormat df = new SimpleDateFormat("MM/dd HH:mm");
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
<form action="<%=h(new ActionURL(TestResultsController.ShowFailures.class, c))%>">
    View Type: <select name="viewType">
    <option disabled selected> -- select an option -- </option>
    <option id="posttime" value="posttime">Day</option>
    <option id="wk" value="wk">Week</option>
    <option id="mo" value="mo">Month</option>
    <option id="yr" value="yr">Year</option>
    <option id="at" value="at">The Beginning of Time</option>
    </select>
    <input type="submit" value="Submit">
</form>
<!-- Selects the View Type in the combobox -->
<script type="text/javascript">
    document.getElementById("<%=h(viewType)%>").selected = "true";
</script>

<!-- main content of page -->
<%if(data.getStatRuns().length > 0) { %>
<div style="float:left;">
    <h2>All Failures</h2>
    <h4>Viewing data for: <%=h(df.format(data.getStartDate()))%> - <%=h(df.format(data.getEndDate()))%></h4>
    <h4>Total failures: <%=runs.length%></h4>
    <h4>Unique users: <%=users.size()%></h4>
</div>
<br />
<!-- Bar & Pie chart containers -->
<div id="failGraph" style="width:700px; height:400px;"></div>
<div id="piechart" style="width:250px; height:250px;"></div>

<table class="decoratedtable" style="float:left;">
    <tr style="height:20px;">
    <th style="height:20px; padding:0;">Sorted by revision</th>
    </tr>
    <%
    Collections.reverse(Arrays.asList(runs)); // so most recent is on top
    Map<Date, Integer> dates = new TreeMap<Date, Integer>();  // maps dates to count of failures per run
    for(RunDetail run: runs) { %>
        <tr>
            <td>
                <p style="width:200px;"><a href="<%=h(new ActionURL(TestResultsController.ShowRunAction.class, c).addParameter("runId", run.getId()))%>">
                <%=h(run.getUserName())%> <br />
                Duration: <%=run.getDuration()%> <br />
                OS: <%=h(run.getOs())%> <br />
                Post Time: <%=formatDateTime(run.getPostTime())%> <br />
                Rev: <%=run.getRevision()%> <br />
                Run Failures: <%=run.getFailures().length%>
                </a></p>
            </td>
            <td>
                <%  int failCounter = 0;
                    for(TestFailDetail fail: run.getFailures()) {
                            failCounter++; %>
                        <pre style="text-align: left; padding:10px;"
                             class="pass-<%=fail.getPass()%>">Pass: <%=fail.getPass()%> Language: <%=h(fail.getLanguage())%> --- <%=h(fail.getStacktrace())%>
                        </pre>
                    <%}%>
                <%
                    if(!dates.containsKey(run.getPostTime()))
                        dates.put(run.getPostTime(), 0);
                    int currentCount = dates.get(run.getPostTime());
                    dates.put(run.getPostTime(),currentCount+failCounter);
                %>
            </td>
        </tr>
    <%}%>
</table>

<!-- Pie Chart -->
<script type="text/javascript">
    var piechart = c3.generate({
        bindto: '#piechart',
        data: {
            columns: [
                <%
                    Map<String, Double> lang =languageBreakdown.get("");
                   for(String l: lang.keySet()) {
                    Double percent = lang.get(l) * 100;
                %>

                ['<%=h(l)%>', <%=percent.intValue()%>],
                <%}%>  ],
            type : 'pie'
        },
        color: {
            pattern: ['#FFB82E', '#A078A0', '#20B2AA', '#F08080', '#FF8B2E']
        } ,
        pie: {
            label: {
                format: function (value, ratio, id) {
                    return "";
                }
            }
        }
    });
</script>
<!-- Failure/Day bar chart -->
<%
    JSONObject failureTrends = new JSONObject();
    // populate json with failure count for each date
    for(Map.Entry<Date, Integer> entry : dates.entrySet()) {
        failureTrends.append("avgFailures", entry.getValue());
        failureTrends.append("dates", entry.getKey().getTime());
    }
%>
<% if(!failureTrends.isEmpty()) {%>
    <script type="text/javascript">
        var failureJSON = jQuery.parseJSON( <%= q(failureTrends.toString()) %> );
        var dates = failureJSON.dates;
        for (var i = 0; i < dates.length; i++) {
            var d = new Date(dates[i]);
            dates[i] = d;
        }
        var avgFailures = failureJSON.avgFailures;
        if(dates.length >= 1)
            dates.unshift('x');
        avgFailures.unshift("<%=h("")%> failures");

            var failTrendChart = c3.generate({
                bindto: '#failGraph',
                data: {
                    x: 'x',
                    columns: [
                        dates,
                        avgFailures
                    ],
                    type: 'bar',
                    onclick: function(d, i) {
                        console.log("onclick", d.x, i);
                    }
                },
                subchart: {
                    show: true,
                    size: {
                        height: 20
                    }
                },
                axis: {
                    x: {
                        type: 'timeseries',
                        localtime: false,
                        tick: {
                            rotate: 90,
                            fit:true,
                            culling: {
                                max: 8
                            },
                            format: '%d/%m'
                        }
                    }
                }
            });
    </script>
<%}%>
<%}%>