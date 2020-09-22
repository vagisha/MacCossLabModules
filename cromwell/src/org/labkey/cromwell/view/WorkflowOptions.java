package org.labkey.cromwell.view;

import org.labkey.api.util.HtmlString;
import org.labkey.cromwell.Workflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class WorkflowOptions
{
    private final Workflow _workflow;

    public WorkflowOptions(Workflow workflow)
    {
        _workflow = workflow;
    }

    public HtmlString getOptions()
    {
        String wdl = _workflow.getWdl();
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
                if(line.startsWith("String"))
                {
                    var param = line.split("\\s");
                    if(param.length > 1)
                    {
                        var paramName = param[1];
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
