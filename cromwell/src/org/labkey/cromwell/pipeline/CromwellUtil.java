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
import org.json.JSONObject;
import org.labkey.api.data.PropertyManager;
import org.labkey.cromwell.CromwellJob;
import org.labkey.cromwell.Workflow;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.labkey.cromwell.CromwellController.PROPS_CROMWELL;
import static org.labkey.cromwell.CromwellController.PROP_CROMWELL_SERVER_PORT;
import static org.labkey.cromwell.CromwellController.PROP_CROMWELL_SERVER_URL;

public class CromwellUtil
{
    private static String CROMWELL_API_PATH = "/api/workflows/v1";
    private static String CROMWELL_STATUS_ENDPOINT = "/status";

    public static CromwellJobStatus submitJob(Workflow workflow, String inputsJson, Logger logger) throws CromwellException
    {
        URI uri = buildCromwellServerUri();

        try (CloseableHttpClient client = HttpClientBuilder.create().build())
        {
            // String url = "http://127.0.0.1:8000/api/workflows/v1";
            // String url = "http://m002.grid.gs.washington.edu:8000/api/workflows/v1";
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

    private static URI buildCromwellServerUri() throws CromwellException
    {
        return buildCromwellServerUri(CROMWELL_API_PATH);
    }

    private static URI buildCromwellServerUri(String path) throws CromwellException
    {
        PropertyManager.PropertyMap map = PropertyManager.getEncryptedStore().getWritableProperties(PROPS_CROMWELL, false);
        if(map == null)
        {
            throw new CromwellException("Could not find Cromwell settings.");
        }
        String cromwellServerUrl = map.get(PROP_CROMWELL_SERVER_URL);
        String cromwellServerPort = map.get(PROP_CROMWELL_SERVER_PORT);
        if(StringUtils.isBlank(cromwellServerUrl))
        {
            throw new CromwellException("Could not find Cromwell server URL");
        }

        try
        {
            URIBuilder builder = new URIBuilder(cromwellServerUrl);
            if(!StringUtils.isBlank(cromwellServerPort))
            {
                builder = builder.setPort(Integer.valueOf(cromwellServerPort));
            }
            return builder.setPath(path).build();
        }
        catch (URISyntaxException e)
        {
            throw new CromwellException("Error building Cromwell server URI. Error was: " + e.getMessage(), e);
        }
    }

    public static CromwellJobStatus getJobStatus(CromwellJob cromwellJob, Logger logger) throws CromwellException
    {
        String path = CROMWELL_API_PATH + '/' + cromwellJob.getCromwellJobId() + CROMWELL_STATUS_ENDPOINT;
        URI uri = buildCromwellServerUri(path);
        try (CloseableHttpClient client = HttpClientBuilder.create().build())
        {
            // String url = "http://m002.grid.gs.washington.edu:8000/api/workflows/v1";
            HttpGet get = new HttpGet(uri);
            logger.info("Checking status of job " + cromwellJob.getCromwellJobId() + " at " + uri);

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
}
