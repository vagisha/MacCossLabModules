package org.labkey.panoramapublic.query.speclib;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.targetedms.ISpectrumLibrary;
import org.labkey.api.targetedms.ITargetedMSRun;
import org.labkey.api.targetedms.TargetedMSService;
import org.labkey.api.targetedms.TargetedMSUrls;
import org.labkey.api.util.DOM;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.PanoramaPublicController;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.EM;
import static org.labkey.api.util.DOM.LI;
import static org.labkey.api.util.DOM.at;

public class LibraryDocumentsDisplayColumnFactory implements DisplayColumnFactory
{
    private static final FieldKey SPECLIB_INFO_ID = FieldKey.fromParts("specLibInfoId");
    private static final FieldKey RUN_IDS = FieldKey.fromParts("RunIds");

    public LibraryDocumentsDisplayColumnFactory() {}

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String specLibIds = ctx.get(colInfo.getFieldKey(), String.class);
                if (!StringUtils.isBlank(specLibIds))
                {
                    TargetedMSService svc = TargetedMSService.get();
                    User user = ctx.getViewContext().getUser();
                    List<ISpectrumLibrary> libraries = getLibraries(specLibIds.split(","), user, svc);
                    if (libraries.size() > 0)
                    {
                        Integer specLibInfoId = ctx.get(SPECLIB_INFO_ID, Integer.class);
                        String allRunIds = ctx.get(RUN_IDS, String.class);
                        List<DOM.Renderable> runLinks = new ArrayList<>();
                        for (ISpectrumLibrary library: libraries)
                        {
                            ITargetedMSRun run = svc.getRun(library.getRunId(), user);
                            if (run != null)
                            {
                                runLinks.add(getRunLink(run, library, specLibInfoId, allRunIds));
                            }
                        }
                        DOM.DIV(runLinks).appendTo(out);
                    }
                    else
                    {
                        out.write("No libraries found for Ids: " + PageFlowUtil.filter(specLibIds));
                    }
                }
            }

            private DOM.Renderable getRunLink(ITargetedMSRun run, ISpectrumLibrary library, Integer specLibInfoId, String allRunIds)
            {
                Path libPath = TargetedMSService.get().getLibraryFilePath(run, library);
                ActionURL viewSpecLibAction = new ActionURL(PanoramaPublicController.ViewSpecLibAction.class, run.getContainer());
                viewSpecLibAction.addParameter("specLibId", library.getId());
                // viewSpecLibAction.addParameter("libContainerId", run.getContainer().getId());
                if (allRunIds != null)
                {
                    viewSpecLibAction.addParameter("allRunIds", allRunIds);
                }
                if (specLibInfoId != null)
                {
                    viewSpecLibAction.addParameter("specLibInfoId", specLibInfoId);
                }
                ActionURL url = PageFlowUtil.urlProvider(TargetedMSUrls.class).getShowRunUrl(run.getContainer(), run.getId());
                return LI(new Link.LinkBuilder(run.getFileName()).href(url).clearClasses().build(),
                        HtmlString.NBSP, "[",
                        (libPath != null && Files.exists(libPath)) ?
                                new Link.LinkBuilder("Library").href(viewSpecLibAction).build()
                                : EM(at(style, "color:red;"), "Missing Library"),
                        "] "
                );
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(SPECLIB_INFO_ID);
                keys.add(RUN_IDS);
            }
        };
    }

    private List<ISpectrumLibrary> getLibraries(String[] ids, User user, TargetedMSService svc)
    {
        Set<Long> specLibIds = Arrays.stream(ids).map(Long::parseLong).collect(Collectors.toSet());
        List<ISpectrumLibrary> libraries = new ArrayList<>();
        specLibIds.forEach(id -> libraries.add(svc.getLibrary(id, null, user)));
        return libraries.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
