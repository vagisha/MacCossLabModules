package org.labkey.panoramapublic.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.attachments.Attachment;
import org.labkey.api.attachments.AttachmentParent;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ShortURLRecord;
import org.labkey.panoramapublic.CatalogImageAttachmentParent;
import org.labkey.panoramapublic.PanoramaPublicController;
import org.labkey.panoramapublic.PanoramaPublicManager;
import org.labkey.panoramapublic.PanoramaPublicSchema;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.view.publish.ShortUrlDisplayColumnFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class CatalogEntryTableInfo extends PanoramaPublicTable
{
    public CatalogEntryTableInfo(@NotNull PanoramaPublicSchema userSchema, ContainerFilter cf)
    {
        super(PanoramaPublicManager.getTableInfoDataValidation(), userSchema, cf, ContainerJoin.ShortUrlJoin);

        var accessUrlCol = wrapColumn("ShortURL", getRealTable().getColumn("ShortUrlEntityId"));
        accessUrlCol.setDisplayColumnFactory(new ShortUrlDisplayColumnFactory(FieldKey.fromParts("ShortUrl")));
        addColumn(accessUrlCol);

        var experimentTitleCol = wrapColumn("Title", getRealTable().getColumn("ShortUrlEntityId"));
        experimentTitleCol.setDisplayColumnFactory(colInfo -> new DataColumn(colInfo)
        {
            @Override
            public Object getValue(RenderContext ctx)
            {
                ShortURLRecord shortUrl = ctx.get(getColumnInfo().getFieldKey(), ShortURLRecord.class);
                ExperimentAnnotations expAnnotations = null;
                if (shortUrl != null)
                {
                    expAnnotations = ExperimentAnnotationsManager.getExperimentForShortUrl(shortUrl);
                }
                return expAnnotations != null ? expAnnotations.getTitle() : null;
            }
        });
        addColumn(experimentTitleCol);

        var imageFileNameCol = wrapColumn("ImageFile", getRealTable().getColumn("ShortUrlEntityId"));
        experimentTitleCol.setDisplayColumnFactory(colInfo -> new DataColumn(colInfo)
        {
            private ActionURL _downloadLink;

            @Override
            public Object getValue(RenderContext ctx)
            {
                ShortURLRecord shortUrl = ctx.get(getColumnInfo().getFieldKey(), ShortURLRecord.class);
                ExperimentAnnotations expAnnotations = shortUrl != null ? ExperimentAnnotationsManager.getExperimentForShortUrl(shortUrl) : null;
                if (expAnnotations != null)
                {
                    AttachmentParent ap = new CatalogImageAttachmentParent(shortUrl, expAnnotations);
                    AttachmentService svc = AttachmentService.get();
                    List<Attachment> attachments = svc.getAttachments(ap);
                    // There should be only one
                    if (attachments.size() > 0)
                    {
                        String fileName = attachments.get(0).getName();
                        _downloadLink = PanoramaPublicController.getCatalogImageDownloadURL(expAnnotations, fileName);
                        return fileName;
                    }
                }
                return null;
            }

            @Override
            public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
            {
                String fileName = (String) getValue(ctx);
                if (fileName != null && _downloadLink != null)
                {

                    out.write(fileName);
                    out.write(PageFlowUtil.iconLink("fa fa-download", null).href(_downloadLink).style("margin-left:10px;").toString());
//                    out.write("&nbsp;");)
//                    SPAN(fileName, new Link.LinkBuilder().href(_downloadLink).addClass("fa fa-download")LINK(at(style, "padding:5px;"), DOM.LK.FA("download"))
                }
                super.renderGridCellContents(ctx, out);
            }
        });
        addColumn(imageFileNameCol);

        List<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("Id"));
        visibleColumns.add(FieldKey.fromParts("Created"));
        visibleColumns.add(FieldKey.fromParts("CreatedBy"));
        visibleColumns.add(FieldKey.fromParts("ShortURL"));
        visibleColumns.add(FieldKey.fromParts("Title"));
        visibleColumns.add(FieldKey.fromParts("Description"));
        visibleColumns.add(FieldKey.fromParts("ImageFile"));
        visibleColumns.add(FieldKey.fromParts("Approved"));
        setDefaultVisibleColumns(visibleColumns);
    }
}
