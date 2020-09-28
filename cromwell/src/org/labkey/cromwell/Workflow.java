package org.labkey.cromwell;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.labkey.cromwell.pipeline.CromwellException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Workflow
{
    private int _id;
    private String _name;
    private int _version;
    private String _wdl;

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public int getVersion()
    {
        return _version;
    }

    public void setVersion(int version)
    {
        _version = version;
    }

    public String getWdl()
    {
        return _wdl;
    }

    public void setWdl(String wdl)
    {
        _wdl = wdl;
    }

    public static List<CromwellController.CromwellInput> getInputs(Workflow workflow) throws CromwellException
    {
        WdlParser parser = new WdlParser();
        parser.parse(workflow.getWdl());

        if(parser.getWorkflowName() == null || parser.getParams() == null)
        {
            throw new CromwellException("Workflow name could not be read from WDL.");
        }
        if(parser.getParams() == null)
        {
            throw new CromwellException("Input param names could not be read from WDL.");
        }
        List<CromwellController.CromwellInput> inputs = new ArrayList<>();
        for(String param: parser.getParams())
        {
            CromwellController.CromwellInput input = new CromwellController.CromwellInput();
            input.setName(param);
            input.setDisplayName(param.replaceAll("_", " "));
            input.setWorkflowName(parser.getWorkflowName());
            inputs.add(input);
        }
        return inputs;
    }

    public static List<CromwellController.CromwellInput> populateInputs(List<CromwellController.CromwellInput> inputs, Map<String, String[]> inputValues) throws CromwellException
    {
        List<CromwellController.CromwellInput> returnList = new ArrayList<>(inputs.size());
        for(CromwellController.CromwellInput input: inputs)
        {
            if(inputValues.containsKey(input.getName()))
            {
                String[] values = inputValues.get(input.getName());
                if(values != null && values.length > 0)
                {
                    CromwellController.CromwellInput retInput = new CromwellController.CromwellInput();
                    retInput.setName(input.getName());
                    retInput.setValue(values[0]);
                    retInput.setDisplayName(input.getDisplayName());
                    retInput.setWorkflowName(input.getWorkflowName());
                    returnList.add(retInput);
                }
                else
                {
                    throw new CromwellException("Missing value for input '" + input.getName() + "'");
                }
            }
            else
            {
                throw new CromwellException("Missing input '" + input.getName() + "'");
            }
        }
        return returnList;
    }

    public static List<CromwellController.CromwellInput> copyInputsFromJob(List<CromwellController.CromwellInput> inputs, CromwellJob job) throws CromwellException
    {
        if(CollectionUtils.isEmpty(inputs))
        {
            return inputs;
        }
        String workflowName = inputs.get(0).getWorkflowName() + ".";
        Map<String, String[]> inputsMap = new HashMap<>();
        JSONObject json = new JSONObject(job.getInputs());
        for (Iterator<String> it = json.keys(); it.hasNext(); )
        {
            String key = it.next();
            String value = json.getString(key);
            // Example: panorama_skyline_workflow.url_target_panorama_folder; where panorama_skyline_workflow is the workflow name
            if(key.startsWith(workflowName))
            {
                key = key.replaceFirst(workflowName, "");
            }
            inputsMap.put(key, new String[]{value});
        }
        return populateInputs(inputs, inputsMap);
    }

    public static String getInputsJSON(List<CromwellController.CromwellInput> inputs)
    {
        JSONObject json = new JSONObject();
        for (CromwellController.CromwellInput input: inputs)
        {
            json.put(input.getWorkflowName() + "." + input.getName(), input.getValue());
        }
        return json.toString();
    }

    private static class WdlParser
    {
        private String _workflowName;
        private List<String> _params;

        public void parse(String wdl) throws CromwellException
        {
            if(StringUtils.isBlank(wdl))
            {
                return;
            }
            List<String> params = new ArrayList<>();
            try
            {
                try (BufferedReader reader = new BufferedReader(new StringReader(wdl)))
                {
                    String line;
                    while((line = reader.readLine()) != null)
                    {
                        line = line.trim();
                        if(line.startsWith("call") || line.startsWith("task") || line.startsWith("scatter"))
                        {
                            // We've reached the end of the list of input parameters
                            break;
                        }
                        if(line.startsWith("workflow"))
                        {
                            if(line.endsWith("{"))
                            {
                                line = line.substring(0, line.length() - 1).trim();
                            }
                            _workflowName = line.substring("workflow".length()).trim();;
                        }
                        else if(line.startsWith("String"))
                        {
                            var param = line.split("\\s");
                            if(param.length > 1)
                            {
                                params.add(param[1]);
                            }
                        }
                    }
                }
            }
            catch (IOException e)
            {
                throw new CromwellException("Error reading WDL. Error was: " + e.getMessage(), e);
            }
            _params = params;
        }

        public String getWorkflowName()
        {
            return _workflowName;
        }

        public List<String> getParams()
        {
            return _params;
        }
    }
}
