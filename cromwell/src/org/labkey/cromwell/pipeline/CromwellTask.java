package org.labkey.cromwell.pipeline;

import com.hierynomus.sshj.key.KeyAlgorithms;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.files.FileContentService;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.security.User;
import org.labkey.api.util.FileType;
import org.labkey.api.util.FileUtil;
import org.labkey.cromwell.CromwellInput;
import org.labkey.cromwell.CromwellJob;
import org.labkey.cromwell.CromwellManager;
import org.labkey.cromwell.Workflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.labkey.cromwell.CromwellController.PROPS_CROMWELL;
import static org.labkey.cromwell.CromwellController.PROP_CROMWELL_SERVER_PORT;
import static org.labkey.cromwell.CromwellController.PROP_CROMWELL_SERVER_URL;
import static org.labkey.cromwell.CromwellController.PROP_SCP_KEY_FILE;
import static org.labkey.cromwell.CromwellController.PROP_SCP_PORT;
import static org.labkey.cromwell.CromwellController.PROP_SCP_USER;

public class CromwellTask extends PipelineJob.Task<CromwellTask.Factory>
{
    public CromwellTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @Override
    public @NotNull RecordedActionSet run() throws PipelineJobException
    {
        PipelineJob job = getJob();
        CromwellJobSupport support = job.getJobSupport(CromwellJobSupport.class);

        job.getLogger().info("Submitting Cromwell job for " + support.getWorkflow().getName());

        runCromwellJob(support, job.getUser(), job.getLogger());

        job.getLogger().info("Finished running Cromwell job.");

        return new RecordedActionSet();
    }

    private void runCromwellJob(CromwellJobSupport jobSupport, User user, Logger log) throws PipelineJobException
    {
        int cromwellJobId = jobSupport.getCromwellJobId();
        CromwellManager manager = CromwellManager.get();
        CromwellJob cromwellJob = manager.getCromwellJob(cromwellJobId);
        if(cromwellJob == null)
        {
            throw new PipelineJobException("Could not find a CromwellJob for id: " + cromwellJobId);
        }

        if(cromwellJob.getPipelineJobId() == null)
        {
            Integer pipelineJobId = (PipelineService.get().getJobId(getJob().getUser(), getJob().getContainer(), getJob().getJobGUID()));
            cromwellJob.setPipelineJobId(pipelineJobId);
            manager.updateJob(cromwellJob, user);
        }

        Workflow workflow = manager.getWorkflow(cromwellJob.getWorkflowId());
        List<CromwellInput> inputList = cromwellJob.getInputList();
        if (inputList.size() == 0)
        {
            throw new PipelineJobException("Job submission failed. Error getting input list from inputs");
        }

        // Set the API Key in the inputs
        CromwellInput input = CromwellJob.getApiKeyInput(inputList);
        if(input != null)
        {
            input.setValue(jobSupport.getPanoramaApiKey());
        }

        Path jobDir;
        try
        {
            jobDir = saveWdlAndInputs(workflow, inputList, getJob().getContainer(), log);
        }
        catch (CromwellException e)
        {
            throw new PipelineJobException(e.getMessage(), e);
        }

        String lastStatus = "";
        CromwellUtil.CromwellJobStatus status;
        CromwellUtil.CromwellProperties cromwellProps;
        try
        {
            cromwellProps = CromwellUtil.readCromwellProperties();

            status = CromwellUtil.submitJob(cromwellProps, workflow, CromwellJob.getInputsJSON(inputList, false), log);
            if(status != null)
            {
                cromwellJob.setCromwellJobId(status.getJobId());
                cromwellJob.setCromwellStatus(status.getJobStatus());
                manager.updateJob(cromwellJob, user);
                lastStatus = status.getJobStatus();

                if(!status.submitted())
                {
                    // TODO: what do we expect to see here?
                    throw new PipelineJobException("Job submission failed. Status returned from Cromwell server was " + status.getJobStatus());
                }
            }
            else
            {
                throw new PipelineJobException("Job submission failed. Could not get status from Cromwell server.");
            }
        }
        catch (CromwellException e)
        {
            throw new PipelineJobException("Job submission failed with error message " + e.getMessage(), e);
        }

        final int sleepTime = 1 * 20 * 1000;
        int attempts = 5;

        URI jobStatusUri = cromwellProps.buildJobStatusUri(status.getJobId());

        log.info("Checking status of job " + cromwellJob.getCromwellJobId() + " at " + jobStatusUri);
        while(true)
        {
            try
            {
                Thread.sleep(sleepTime);
            }
            catch (InterruptedException e)
            {
                log.error("Cancelled task.", e);
                break;
            }

            try
            {
                var pipelineJobStatus = PipelineService.get().getStatusFile(getJob().getJobGUID()).getStatus();
                if(PipelineJob.TaskStatus.cancelling.matches(pipelineJobStatus))
                {
                    break;
                }

                status = CromwellUtil.getJobStatus(jobStatusUri, log);
                if(status == null && attempts > 0)
                {
                    log.info("Did not get job status.  Job may not yet have started running. Trying again...");
                    attempts--;
                    continue;
                }
                if(!lastStatus.equalsIgnoreCase(status.getJobStatus()))
                {
                    log.info("Cromwell job status: " + status.getJobStatus());
                    lastStatus = status.getJobStatus();
                    cromwellJob.setCromwellStatus(status.getJobStatus());
                    manager.updateJob(cromwellJob, user);
                }

                if(status.success())
                {
                    log.info("Cromwell job successfully completed.");
                    break;
                }
                if(status.failed())
                {
                    log.error("Cromwell job failed.");
                    // TODO: throw an exception here so the pipeline job status is set to failed too?
                    break;
                }
                if(status.success())
                {
                    log.error("Cromwell job succeeded!");
                    break;
                }
            }
            catch (CromwellException e)
            {
                log.error("An error occurred getting Cromwell job status", e);
                // TODO: throw an exception here so the pipeline job status is set to failed too?
                break;
            }
        }

        // Copy log files from the Cromwell server to the job directory
        try
        {
            copyLogs(cromwellProps, status.getJobId(), jobDir);
        }
        catch (CromwellException e)
        {
            e.printStackTrace();
        }
    }

    private void copyLogs(CromwellUtil.CromwellProperties properties, String cromwellJobId, Path jobDir) throws CromwellException
    {
        // Get a list of log files that we will copy
        List<String> logFilePaths = null;
        try
        {
            logFilePaths = CromwellUtil.getJobLogs(properties.buildjobLogsUrl(cromwellJobId));
        }
        catch (URISyntaxException e)
        {
            throw new CromwellException("Error building URI to get job log paths", e);
        }

        // ssh.useCompression(); // Can lead to significant speedup (needs JZlib in classpath)
        DefaultConfig defaultConfig = new DefaultConfig();
        defaultConfig.setKeyAlgorithms(Collections.singletonList(KeyAlgorithms.SSHRSA()));
        /*
        scp -i ~/cromwell/.ssh_cromwell_rsa cromwell-transfer@m002.grid.gs.washington.edu /data/scratch/ssd/cromwell/cromwell-executions/panorama_skyline_workflow/d6c28dec-a1ba-4e4c-ae98-1e2cb59d16a4/call-list_raw_files/cacheCopy/execution/stdout
         */
        try (SSHClient ssh = new SSHClient(defaultConfig))
        {
            File privateKey = new File(properties.getScpKeyFilePath());
            KeyProvider keys = ssh.loadKeys(privateKey.getPath());
            ssh.connect(properties.getScpHost(), Integer.valueOf(properties.getScpPort()));
            ssh.authPublickey(properties.getScpUser(), keys);
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.loadKnownHosts();

            for(String logFile: logFilePaths)
            {
                try
                {
                    ssh.newSCPFileTransfer().download(logFile, getLocalPath(jobDir, logFile, cromwellJobId));
                }
                catch(IOException e)
                {
                    throw new CromwellException("Error copying file from Cromwell server: " + logFile, e);
                }
            }
        }
        catch (IOException e)
        {
            throw new CromwellException("Copying cromwell log files failed", e);
        }
    }

    private String getLocalPath(Path jobDir, String logFile, String cromwellJobId)
    {
        // /data/scratch/ssd/cromwell/cromwell-executions/panorama_skyline_workflow/d6c28dec-a1ba-4e4c-ae98-1e2cb59d16a4/call-download_raw_file/shard-0/execution/stderr
        int idx = logFile.indexOf(cromwellJobId);
        String localPath = logFile.substring(idx + cromwellJobId.length() + 1);
        localPath = localPath.replace("/execution", "").replaceAll("/", ".");
        return jobDir.resolve(localPath).toString();
    }

    private Path saveWdlAndInputs(Workflow workflow, List<CromwellInput> inputList, Container container, Logger logger) throws CromwellException
    {
        Path fileRootPath = FileContentService.get().getFileRootPath(container, FileContentService.ContentType.files);
        Path workflowsDir = fileRootPath.resolve("Cromwell");
        if(!Files.exists(workflowsDir))
        {
            createDir(workflowsDir);
        }
        String jobFolder = FileUtil.makeFileNameWithTimestamp(workflow.getName() + "_" + workflow.getVersion());
        Path jobDir = workflowsDir.resolve(jobFolder);
        createDir(jobDir);

        Path wdlFile =  jobDir.resolve("workflow.wdl");
        writeFile(wdlFile, workflow.getWdl(), "Could not write to wdl file ");

        Path inputsFile = jobDir.resolve("workflow.inputs.json");
        writeFile(inputsFile, CromwellJob.getInputsJSON(inputList, true), "Could not write to inputs JSON file ");

        return jobDir;
    }

    private void writeFile(Path wdlFile, String text, String s) throws CromwellException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(wdlFile))
        {
            writer.write(text);
        }
        catch (IOException e)
        {
            throw new CromwellException(s + wdlFile + ". Error was: " + e.getMessage(), e);
        }
    }

    private void createDir(Path dir) throws CromwellException
    {
        try
        {
            Files.createDirectory(dir);
        }
        catch (IOException e)
        {
            throw new CromwellException("Could not create directory " + dir + ". Error was: " + e.getMessage(), e);
        }
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        public Factory()
        {
            super(CromwellTask.class);
        }

        @Override
        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new CromwellTask(this, job);
        }

        @Override
        public List<FileType> getInputTypes()
        {
            return Collections.emptyList();
        }

        @Override
        public List<String> getProtocolActionNames()
        {
            return Collections.emptyList();
        }

        @Override
        public String getStatusName()
        {
            return "SUBMIT CROMWELL JOB";
        }

        @Override
        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }
    }
}
