package org.labkey.cromwell.pipeline;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.labkey.cromwell.CromwellJob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CromwellUtil
{
    public static CromwellJobStatus submitJob(CromwellJob cromwellJob, Logger logger) throws IOException
    {
        try (CloseableHttpClient client = HttpClientBuilder.create().build())
        {
            String url = "http://127.0.0.1:8000/api/workflows/v1";
            // String url = "http://m002.grid.gs.washington.edu:8000/api/workflows/v1";
            HttpPost post = new HttpPost(url);
            File wdlFile = new File("C:\\Users\\vsharma\\WORK\\LabKey\\release20.7-SNAPSHOT\\files\\CromwellWorkflows\\@files\\Workflows\\skyline_panorama.wdl");
            File inputsFile = new File("C:\\Users\\vsharma\\WORK\\LabKey\\release20.7-SNAPSHOT\\files\\CromwellWorkflows\\@files\\Workflows\\panorama_skyline_inputs.json");
            FileBody wdlFileBody = new FileBody(wdlFile);
            FileBody inputsFileBody = new FileBody(inputsFile, ContentType.APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("workflowSource", wdlFileBody);
            builder.addPart("workflowInputs", inputsFileBody);
            HttpEntity entity = builder.build();
            post.setEntity(entity);
            logger.info("Submitting job to " + url);

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
    }

    public static CromwellJobStatus getJobStatus(CromwellJob cromwellJob, Logger logger) throws IOException
    {
        try (CloseableHttpClient client = HttpClientBuilder.create().build())
        {
            String url = "http://127.0.0.1:8000/api/workflows/v1";
            url = url + '/' + cromwellJob.getCromwellJobId() + "/status";

            // String url = "http://m002.grid.gs.washington.edu:8000/api/workflows/v1";
            HttpGet get = new HttpGet(url);
            logger.info("Checking status of job " + cromwellJob.getCromwellJobId() + " at " + url);

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
    }

    public static void submitJob_doesnotwork(CromwellJob cromwellJob, Logger logger) throws IOException
    {
        // Make a POST request
        // URL url = new URL("http://127.0.0.1:8000/api/workflows/v1");
        URL url = new URL("http://m002.grid.gs.washington.edu:8000/api/workflows/v1");

        HttpURLConnection conn = null;

        try
        {
            String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
            String CRLF = "\r\n"; // Line separator required by multipart/form-data.
            String charset = "UTF-8";
            File wdlFile = new File("C:\\Users\\vsharma\\WORK\\LabKey\\release20.7-SNAPSHOT\\files\\CromwellWorkflows\\@files\\Workflows\\skyline_panorama.wdl");
            File inputsFile = new File("C:\\Users\\vsharma\\WORK\\LabKey\\release20.7-SNAPSHOT\\files\\CromwellWorkflows\\@files\\Workflows\\panorama_skyline_inputs.json");

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            // conn.setRequestProperty("Content-Type", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);



            // conn.setRequestProperty("charset", "utf-8");
            //conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            //conn.setUseCaches(false);
            try (OutputStream output = conn.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true))
            {
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"wdlFile\"; filename=\"" + wdlFile.getName() + "\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
                writer.append(CRLF).flush();
                Files.copy(wdlFile.toPath(), output);
                output.flush(); // Important before continuing with writer!
                writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"workflowInputs\"; filename=\"" + inputsFile.getName() + "\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
                writer.append(CRLF).flush();
                Files.copy(inputsFile.toPath(), output);
                output.flush(); // Important before continuing with writer!
                writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

                // End of multipart/form-data.
                writer.append("--" + boundary + "--").append(CRLF).flush();
            }

            // log.info("Sending POST request to " + server.getUrl());
            System.out.println("Sending POST request to " + url);
            int responseCode = conn.getResponseCode();
            // log.info("Response code - " + responseCode);
            System.out.println("Response code: " + responseCode);
            String response;
            try (InputStream in = conn.getInputStream())
            {
                response = IOUtils.toString(in, StandardCharsets.UTF_8);
                System.out.println(response);
                // log.info("Response from server: " + response);
            }

            // String response = "{\"name\":\"LINCS_P100_DIA_Plate52y_annotated_minimized_2017-08-23_11-20-58\",\"assay\":\"P100\",\"status\":\"Waiting_To_Download\",\"id\":\"5c324f97b306063b135bf99c\",\"created\":\"2019-01-06T18:57:27.484Z\",\"last_modified\":\"2019-01-06T18:57:27.484Z\",\"level 2\":{\"panorama\":{\"method\":\"GET\",\"url\":\"https://panoramaweb-dr.gs.washington.edu/lincs/LINCS-DCIC/PSP/P100/runGCTReportApi.view?runId=32394&remote=true&reportName=GCT%20File%20P100\"}},\"level 3\":{\"panorama\":{\"method\":\"PUT\",\"url\":\"https://panoramaweb-dr.gs.washington.edu/_webdav/LINCS-DCIC/PSP/P100/%40files/GCT/LINCS_P100_DIA_Plate52y_annotated_minimized_2017-08-23_11-20-58_LVL3.gct\"}},\"level 4\":{\"panorama\":{\"method\":\"PUT\",\"url\":\"https://panoramaweb-dr.gs.washington.edu/_webdav/LINCS-DCIC/PSP/P100/%40files/GCT/LINCS_P100_DIA_Plate52y_annotated_minimized_2017-08-23_11-20-58_LVL4.gct\"}},\"config\":{\"panorama\":{\"method\":\"PUT\",\"url\":\"https://panoramaweb-dr.gs.washington.edu/_webdav/LINCS-DCIC/PSP/P100/%40files/GCT/LINCS_P100_DIA_Plate52y_annotated_minimized_2017-08-23_11-20-58.cfg\"}}}";
            // org.json.simple.JSONObject jsonResponse = getJsonObject(response);
            // parseResponseJson(jsonResponse, pspJob);
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
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
