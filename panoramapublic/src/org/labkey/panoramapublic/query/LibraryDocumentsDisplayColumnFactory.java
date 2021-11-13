package org.labkey.panoramapublic.query;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.TargetedMSUrls;
import org.labkey.api.util.DOM;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LibraryDocumentsDisplayColumnFactory implements DisplayColumnFactory
{
//    private ExperimentAnnotations _exptAnnotations;

    public LibraryDocumentsDisplayColumnFactory() {}

//    public LibraryDocumentsDisplayColumnFactory(ExperimentAnnotations exptAnnotations)
//    {
//        _exptAnnotations = exptAnnotations;
//    }

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
                    List<ITargetedMSRun> runs = getRuns(ctx.getContainer(), runIds.split(","));
                    if (runs.size() > 0)
                    {
                        List<DOM.Renderable> runLinks = new ArrayList<>();
                        runs.stream().forEach(r -> runLinks.add(getRunLinks(r)));
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

    private List<ITargetedMSRun> getRuns(Container container, String[] ids)
    {
//        Set<Container> containers = (_exptAnnotations != null && _exptAnnotations.isIncludeSubfolders()) ?
//                ContainerManager.getAllChildren(container)
//                : Set.of(container);

        Set<Long> runIds = Arrays.stream(ids).map(Long::parseLong).collect(Collectors.toSet());
        return PanoramaPublicManager.getRuns(runIds);
//        TargetedMSService svc = TargetedMSService.get();
//        return containers.stream().flatMap(c ->
//                svc.getRuns(c).stream().filter(r -> runIds.contains(r.getId())))
//                .collect(Collectors.toList());
    }
}
