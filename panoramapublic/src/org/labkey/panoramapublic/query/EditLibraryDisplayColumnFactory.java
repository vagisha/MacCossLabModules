package org.labkey.panoramapublic.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.panoramapublic.PanoramaPublicController;
import org.labkey.panoramapublic.model.ExperimentAnnotations;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

public class EditLibraryDisplayColumnFactory implements DisplayColumnFactory
{
    private static final FieldKey EXPT_ANNOT_FK = FieldKey.fromParts("experimentAnnotationsId");
    private static final FieldKey SPECLIB_INFO_ID_FK = FieldKey.fromParts("specLibInfoId");
    private static final FieldKey SPECLIB_RUNID_FK = FieldKey.fromParts("specLibId", "runId");
    private static final FieldKey SPEC_LIB_CONTAINER_FK = FieldKey.fromParts("specLibId", "runId", "container");

    public EditLibraryDisplayColumnFactory() {}

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                Long specLibId = ctx.get(colInfo.getFieldKey(), Long.class);
                if (specLibId != null)
                {
                    ExperimentAnnotations exptAnnotations = ExperimentAnnotationsManager.getInContainer(ctx.getContainer());
                    if (exptAnnotations != null)
                    {
                        Integer exptAnnotationsId = ctx.get(EXPT_ANNOT_FK, Integer.class);
                        if (exptAnnotationsId != null && exptAnnotations.getId() != exptAnnotationsId)
                        {
                            out.write("Experiment id mismatch. Id: " + exptAnnotationsId);
                            return;
                        }

//                        Long runId = ctx.get(SPECLIB_RUNID_FK, Long.class);
                        String libContainerId = ctx.get(SPEC_LIB_CONTAINER_FK, String.class);

                        Integer specLibInfoId = ctx.get(SPECLIB_INFO_ID_FK, Integer.class);
                        String linkTxt = specLibInfoId != null ? "Edit" : "Add";

//                        String name = ctx.get(FieldKey.fromParts("name"), String.class);
//                        String libraryType = ctx.get(FieldKey.fromParts("librarytype"), String.class);
//                        String fileNameHint = ctx.get(FieldKey.fromParts("filenamehint"), String.class);
//                        String skylineLibraryId = ctx.get(FieldKey.fromParts("skylinelibraryid"), String.class);
//                        if (name == null || libraryType == null)
//                        {
//                            out.write("Missing library name and type");
//                            return;
//                        }

//                        SpecLibKey key = new SpecLibKey(name, libraryType, fileNameHint, skylineLibraryId);
                        ActionURL editUrl = new ActionURL(PanoramaPublicController.EditSpecLibInfoAction.class, ctx.getContainer());
//                        editUrl.addParameter("specLibKey", key.getKey());
                        editUrl.addParameter("id", exptAnnotations.getId());
                        editUrl.addParameter("specLibId", specLibId);
                        editUrl.addParameter("libContainerId", libContainerId);
                        if (specLibInfoId != null)
                        {
                            editUrl.addParameter("specLibInfoId", specLibInfoId);
                        }

                        out.write(PageFlowUtil.link(linkTxt).href(editUrl).toString());
                        return;
                    }
                }
                super.renderGridCellContents(ctx, out);
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
//                keys.add(FieldKey.fromParts("name"));
//                keys.add(FieldKey.fromParts("librarytype"));
//                keys.add(FieldKey.fromParts("filenamehint"));
//                keys.add(FieldKey.fromParts("skylinelibraryid"));
                keys.add(SPECLIB_INFO_ID_FK);
                keys.add(EXPT_ANNOT_FK);
//                keys.add(SPECLIB_RUNID_FK);
                keys.add(SPEC_LIB_CONTAINER_FK);
            }
        };
    }
}
