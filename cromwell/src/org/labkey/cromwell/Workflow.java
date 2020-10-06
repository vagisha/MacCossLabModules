package org.labkey.cromwell;

import org.apache.commons.lang3.StringUtils;
import org.labkey.cromwell.pipeline.CromwellException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
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

    public static List<CromwellInput> createInputs(Workflow workflow) throws CromwellException
    {
        WdlParser parser = new WdlParser();
        parser.parse(workflow.getWdl());

        if(parser.getWorkflowName() == null)
        {
            throw new CromwellException("Workflow name could not be read from WDL.");
        }
        if(parser.getParams() == null)
        {
            throw new CromwellException("Input param names could not be read from WDL.");
        }
        List<CromwellInput> inputs = new ArrayList<>();
        for(String param: parser.getParams())
        {
            CromwellInput input = new CromwellInput();
            input.setName(param);
            input.setWorkflowName(parser.getWorkflowName());
            inputs.add(input);
        }
        return inputs;
    }

    public static List<CromwellInput> populateInputs(List<CromwellInput> inputs, Map<String, String[]> inputValues) throws CromwellException
    {
        List<CromwellInput> returnList = new ArrayList<>(inputs.size());
        for(CromwellInput input: inputs)
        {
            if(inputValues.containsKey(input.getName()))
            {
                String[] values = inputValues.get(input.getName());
                if(values != null && values.length > 0)
                {
                    CromwellInput retInput = new CromwellInput();
                    retInput.setName(input.getName());
                    retInput.setValue(values[0]);
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
                            _workflowName = line.substring("workflow".length()).trim();
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
