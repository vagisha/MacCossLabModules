package org.labkey.panoramapublic.query.speclib;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSUrls;
import org.labkey.api.util.DOM;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.query.SpecLibInfoManager;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LibraryDocumentsDisplayColumnFactory implements DisplayColumnFactory
{
    public LibraryDocumentsDisplayColumnFactory() {}

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String runIds = ctx.get(colInfo.getFieldKey(), String.class);
                if (!StringUtils.isBlank(runIds))
                {
                    List<ITargetedMSRun> runs = getRuns(runIds.split(","), ctx.getViewContext().getUser());
                    if (runs.size() > 0)
                    {
                        final User user = ctx.getViewContext().getUser();
                        List<DOM.Renderable> runLinks = new ArrayList<>();
                        runs.stream()
                                .filter(r -> r.getContainer().hasPermission(user, ReadPermission.class))
                                .forEach(r -> runLinks.add(getRunLinks(r)));
                        DOM.DIV(runLinks).appendTo(out);
                    }
                    else
                    {
                        out.write("No Skyline documents found for runIds: " + PageFlowUtil.filter(runIds));
                    }
                }
            }

            private DOM.Renderable getRunLinks(ITargetedMSRun run)
            {
                // TargetedMSService.get().getLibraryFilePath()
                ActionURL url = PageFlowUtil.urlProvider(TargetedMSUrls.class).getShowRunUrl(run.getContainer(), run.getId());
                return DOM.LI(new Link.LinkBuilder(run.getFileName()).href(url).clearClasses().build());
            }
        };
    }

    private List<ITargetedMSRun> getRuns(String[] ids, User user)
    {
        Set<Long> runIds = Arrays.stream(ids).map(Long::parseLong).collect(Collectors.toSet());
        return SpecLibInfoManager.getRuns(runIds, user);
    }
}
