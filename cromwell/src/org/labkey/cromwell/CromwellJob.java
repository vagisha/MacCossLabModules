package org.labkey.cromwell;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.labkey.api.data.Container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CromwellJob
{
    private int _id;
    private int _workflowId;
    private Container _container;
    private Integer _pipelineJobId;
    private String _cromwellJobId;
    private String _cromwellStatus;
    private String _inputs;

    private List<CromwellInput> _inputList;

    public CromwellJob() {}
    public CromwellJob(int workflowId, Container container)
    {
        _workflowId = workflowId;
        _container = container;
    }

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getWorkflowId()
    {
        return _workflowId;
    }

    public void setWorkflowId(int workflowId)
    {
        _workflowId = workflowId;
    }

    public void setContainer(Container container)
    {
        _container = container;
    }

    public Container getContainer()
    {
        return _container;
    }

    public String getInputs()
    {
        return _inputs;
    }

    public void setInputs(String inputs)
    {
        _inputs = inputs;
    }

    public Integer getPipelineJobId()
    {
        return _pipelineJobId;
    }

    public void setPipelineJobId(Integer pipelineJobId)
    {
        _pipelineJobId = pipelineJobId;
    }

    public String getCromwellJobId()
    {
        return _cromwellJobId;
    }

    public void setCromwellJobId(String cromwellJobId)
    {
        _cromwellJobId = cromwellJobId;
    }

    public String getCromwellStatus()
    {
        return _cromwellStatus;
    }

    public void setCromwellStatus(String cromwellStatus)
    {
        _cromwellStatus = cromwellStatus;
    }

    public List<CromwellInput> getInputList()
    {
        if(_inputList == null)
        {
            if(!StringUtils.isBlank(_inputs))
            {
                Workflow workflow = CromwellManager.get().getWorkflow(_workflowId);
                if(workflow != null)
                {
                    JSONObject json = new JSONObject(_inputs);
                    _inputList = new ArrayList<>();
                    for (Iterator<String> it = json.keys(); it.hasNext();)
                    {
                        String key = it.next();
                        String value = json.getString(key);
                        // Example: panorama_skyline_workflow.url_target_panorama_folder; where panorama_skyline_workflow is the workflow name
                        String workflowPrefix = workflow.getName() + ".";
                        if (key.startsWith(workflowPrefix))
                        {
                            key = key.replaceFirst(workflowPrefix, "");
                        }
                        CromwellInput input = new CromwellInput();
                        input.setName(key);
                        input.setValue(value);
                        input.setWorkflowName(workflow.getName());

                        _inputList.add(input);
                    }
                }
            }
        }
        return _inputList == null ? Collections.emptyList() : _inputList;
    }

//    public List<CromwellInput> getInputListWithApiKey(User user)
//    {
//        var inputList = getInputList();
//        for(CromwellInput input: inputList)
//        {
//            if(input.isApiKey())
//            {
//                PropertyManager.PropertyMap props = PropertyManager.getEncryptedStore().getWritableProperties(user, ContainerManager.getRoot(), PROPS_CROMWELL_USER, false);
//                if(props != null && props.get(PROP_USER_APIKEY) != null)
//                {
//                    input.setValue(props.get(PROP_USER_APIKEY));
//                }
//                break;
//            }
//        }
//        return inputList;
//    }

    public void setInputList(List<CromwellInput> inputList)
    {
        _inputList = inputList;
        _inputs = getInputsJSON(inputList,true); // Hide API Key
    }

    public static String getInputsJSON(List<CromwellInput> inputList, boolean hideApiKey)
    {
        JSONObject json = new JSONObject();
        for (CromwellInput input: inputList)
        {
            if(input.isApiKey() && hideApiKey)
            {
                json.put(input.getWorkflowName() + "." + input.getName(), ""); // Do not save the API Key in the database
            }
            else
            {
                json.put(input.getWorkflowName() + "." + input.getName(), input.getValue());
            }
        }
        return json.toString();
    }

    public static CromwellInput getApiKeyInput(List<CromwellInput> inputList)
    {
        return inputList.stream().filter(i -> i.getName().equals(CromwellInput.INPUT_API_KEY)).findFirst().orElse(null);
    }
}
