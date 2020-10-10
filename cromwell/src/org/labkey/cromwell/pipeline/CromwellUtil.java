package org.labkey.cromwell.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.data.PropertyManager;
import org.labkey.cromwell.Workflow;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.labkey.cromwell.CromwellController.PROPS_CROMWELL;
import static org.labkey.cromwell.CromwellController.PROP_CROMWELL_SERVER_PORT;
import static org.labkey.cromwell.CromwellController.PROP_CROMWELL_SERVER_URL;
import static org.labkey.cromwell.CromwellController.PROP_SCP_KEY_FILE;
import static org.labkey.cromwell.CromwellController.PROP_SCP_PORT;
import static org.labkey.cromwell.CromwellController.PROP_SCP_USER;

public class CromwellUtil
{
    private static String CROMWELL_API_PATH = "/api/workflows/v1";

    public static CromwellJobStatus submitJob(CromwellProperties properties, Workflow workflow, String inputsJson, Logger logger) throws CromwellException
    {
        URI uri = properties.buildJobSubmitUri();

        try (CloseableHttpClient client = HttpClientBuilder.create().build())
        {
            // String url = "http://127.0.0.1:8000/api/workflows/v1";
            HttpPost post = new HttpPost(uri);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("workflowSource", new StringBody(workflow.getWdl(), ContentType.DEFAULT_BINARY));
            builder.addPart("workflowInputs", new StringBody(inputsJson, ContentType.APPLICATION_JSON));
            HttpEntity entity = builder.build();
            post.setEntity(entity);
            logger.info("Submitting job to " + uri);

            try (CloseableHttpResponse response = client.execute(post))
            {
                ResponseHandler<String> handler = new BasicResponseHandler();
                StatusLine status = response.getStatusLine();

                if (status.getStatusCode() == HttpStatus.SC_OK || status.getStatusCode() == HttpStatus.SC_CREATED)
                {
                    String resp = handler.handleResponse(response);
                    JSONObject json = new JSONObject(resp);
                    // {"id":"ac73fdba-51f5-4aa2-8a09-bd5f7f9611fa","status":"Submitted"}
                    return new CromwellJobStatus(json.getString("id"), json.getString("status"));
                }
                else
                {
                    logger.error("Job submission failed. Response code was " + status.getStatusCode() + " " +  status.getReasonPhrase());
                    EntityUtils.consume(response.getEntity());
                    return null;
                }
            }
        }
        catch (IOException e)
        {
            throw new CromwellException("Error occurred submitting job.", e);
        }
    }

    public static CromwellJobStatus getJobStatus(URI jobStatusUri, Logger logger) throws CromwellException
    {
        try (CloseableHttpClient client = HttpClientBuilder.create().build())
        {
            HttpGet get = new HttpGet(jobStatusUri);

            try (CloseableHttpResponse response = client.execute(get))
            {
                ResponseHandler<String> handler = new BasicResponseHandler();
                StatusLine status = response.getStatusLine();

                if (status.getStatusCode() == HttpStatus.SC_OK)
                {
                    String resp = handler.handleResponse(response);
                    JSONObject json = new JSONObject(resp);
                    // {"id":"ac73fdba-51f5-4aa2-8a09-bd5f7f9611fa","status":"Submitted"}
                    return new CromwellJobStatus(json.getString("id"), json.getString("status"));
                }
                else
                {
                    logger.error("Checking job status failed. Response code was " + status.getStatusCode() + " " +  status.getReasonPhrase());
                    EntityUtils.consume(response.getEntity());
                    return null;
                }
            }
        }
        catch (IOException e)
        {
            throw new CromwellException("Error checking status of job.", e);
        }
    }

    public static List<String> getJobLogs(URI jobLogsUri) throws CromwellException
    {
        List<String> logFiles = new ArrayList<>();

        try (CloseableHttpClient client = HttpClientBuilder.create().build())
        {
            HttpGet get = new HttpGet(jobLogsUri);

            try (CloseableHttpResponse response = client.execute(get))
            {
                ResponseHandler<String> handler = new BasicResponseHandler();
                StatusLine status = response.getStatusLine();

                if (status.getStatusCode() == HttpStatus.SC_OK)
                {
                    String resp = handler.handleResponse(response);
                    JSONObject json = new JSONObject(resp);
                    JSONObject calls = json.getJSONObject("calls");
                    for (Iterator<String> it = calls.keys(); it.hasNext(); )
                    {
                        String key = it.next();
                        JSONArray values = calls.getJSONArray(key);
                        for(int i = 0; i < values.length(); i++)
                        {
                            JSONObject info = values.getJSONObject(i);
                            String stdout = info.getString("stdout");
                            String stderr = info.getString("stderr");

                            JSONObject callCaching = info.getJSONObject("callCaching");
                            if(callCaching != null && callCaching.containsKey("hit"))
                            {
                                if(callCaching.getBoolean("hit"))
                                {
                                    var callRoot = info.getString("callRoot");
                                    stdout = callRoot + "/cacheCopy/execution/stdout";
                                    stderr = callRoot + "/cacheCopy/execution/stderr";
                                }
                            }

                            logFiles.add(stdout);
                            logFiles.add(stderr);
                        }
                    }
                }
                else
                {
                    EntityUtils.consume(response.getEntity());
                    throw new CromwellException("Error getting list of log files for job. Response code was " + status.getStatusCode() + " " +  status.getReasonPhrase());
                }
            }
        }
        catch (IOException e)
        {
            throw new CromwellException("Error getting list of log files for job.", e);
        }

        return logFiles;
    }

    public static CromwellProperties readCromwellProperties() throws CromwellException
    {
        PropertyManager.PropertyMap map = PropertyManager.getEncryptedStore().getWritableProperties(PROPS_CROMWELL, false);
        if(map == null)
        {
            throw new CromwellException("Could not find Cromwell settings.");
        }
        String cromwellServerUrlStr = map.get(PROP_CROMWELL_SERVER_URL);
        String cromwellServerPortStr = map.getOrDefault(PROP_CROMWELL_SERVER_PORT, "-1");
        String scpUser = map.get(PROP_SCP_USER);
        String scpPortStr = map.getOrDefault(PROP_SCP_PORT, "22");
        String scpKeyFilePath = map.get(PROP_SCP_KEY_FILE);

        if(StringUtils.isBlank(cromwellServerUrlStr))
        {
            throw new CromwellException("Could not find Cromwell server URL");
        }
        if(StringUtils.isBlank(scpUser))
        {
            throw new CromwellException("Could not find SCP user name");
        }
        if(StringUtils.isBlank(scpKeyFilePath))
        {
            throw new CromwellException("Could not find SCP key file path");
        }
        int cromwellServerPort;
        try
        {
            cromwellServerPort = Integer.parseInt(cromwellServerPortStr);
        }
        catch(NumberFormatException e)
        {
            throw new CromwellException("Invalid Cromwell server port value: " + cromwellServerPortStr);
        }
        int scpPort;
        try
        {
            scpPort = Integer.parseInt(scpPortStr);
        }
        catch(NumberFormatException e)
        {
            throw new CromwellException("Invalid SCP port value: " + scpPortStr);
        }

        URI cromwellServerUri;
        try
        {
            URIBuilder builder = new URIBuilder(cromwellServerUrlStr);
            if(cromwellServerPort != -1)
            {
                builder = builder.setPort(cromwellServerPort);
            }
            cromwellServerUri = builder.build();
        }
        catch (URISyntaxException e)
        {
            throw new CromwellException("Error parsing Cromwell server URL. Error was: " + e.getMessage(), e);
        }

        CromwellProperties props = new CromwellProperties(cromwellServerUri, scpUser, scpPort, scpKeyFilePath);
        return props;
    }

    public static class CromwellJobStatus
    {
        private final String _jobId;
        private final String _jobStatus;

        public CromwellJobStatus(String jobId, String jobStatus)
        {
            _jobId = jobId;
            _jobStatus = jobStatus;
        }

        public String getJobId()
        {
            return _jobId;
        }

        public String getJobStatus()
        {
            return _jobStatus;
        }

        public boolean success()
        {
            return "Succeeded".equalsIgnoreCase(_jobStatus);
        }
        public boolean submitted()
        {
            return "Submitted".equalsIgnoreCase(_jobStatus);
        }
        public boolean running()
        {
            return "Running".equalsIgnoreCase(_jobStatus);
        }
        public boolean failed()
        {
            return "Failed".equalsIgnoreCase(_jobStatus);
        }
    }

    public static class CromwellProperties
    {
        private final URI _cromwellServerUri;
        private final String _scpUser;
        private final int _scpPort;
        private final String _scpKeyFilePath;

        private CromwellProperties(URI cromwellServerUri, String scpUser, int scpPort, String scpKeyFilePath)
        {
            _cromwellServerUri = cromwellServerUri;
            _scpUser = scpUser;
            _scpPort = scpPort;
            _scpKeyFilePath = scpKeyFilePath;
        }

        public URI getCromwellServerUri()
        {
            return _cromwellServerUri;
        }

        public String getScpUser()
        {
            return _scpUser;
        }

        public int getScpPort()
        {
            return _scpPort;
        }

        public String getScpKeyFilePath()
        {
            return _scpKeyFilePath;
        }

        public String getScpHost()
        {
            return _cromwellServerUri.getHost();
        }

        private URI buildCromwellServerUri(String path)
        {
            return _cromwellServerUri.resolve(path);
        }

        public URI buildJobSubmitUri()
        {
            return buildCromwellServerUri(CROMWELL_API_PATH);
        }

        public URI buildJobStatusUri(String cromwellJobId)
        {
            String path = CROMWELL_API_PATH + '/' + cromwellJobId + "/status";
            return buildCromwellServerUri(path);
        }

        public URI buildjobLogsUrl(String cromwellJobId) throws URISyntaxException
        {
            String path = CROMWELL_API_PATH + '/' + cromwellJobId + "/metadata";
            // Query params to limit returned JSON to the keys we are interesed in
            // includeKey=calls&includeKey=stdout&includeKey=stderr&includeKey=callCaching"
            URI uri = new URIBuilder(buildCromwellServerUri(path))
                    .addParameter("includeKey", "calls")
                    .addParameter("includeKey", "stdout")
                    .addParameter("includeKey", "stderr")
                    .addParameter("includeKey", "callCaching")
                    .addParameter("includeKey", "callRoot")
                    .build();

            return uri;

        }
    }
}
