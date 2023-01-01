package org.labkey.panoramapublic.view.publish;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.Button;
import org.labkey.api.util.HtmlString;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.VBox;
import org.labkey.api.view.WebPartView;
import org.labkey.panoramapublic.PanoramaPublicController;
import org.labkey.panoramapublic.model.CatalogEntry;
import org.labkey.panoramapublic.model.ExperimentAnnotations;
import org.labkey.panoramapublic.query.CatalogEntryManager;
import org.labkey.panoramapublic.security.PanoramaPublicSubmitterPermission;

import java.util.Set;

import static org.labkey.api.util.DOM.Attribute.src;
import static org.labkey.api.util.DOM.B;
import static org.labkey.api.util.DOM.BR;
import static org.labkey.api.util.DOM.DIV;
import static org.labkey.api.util.DOM.IMG;
import static org.labkey.api.util.DOM.U;
import static org.labkey.api.util.DOM.at;

public class CatalogEntryWebPart extends VBox
{
    public CatalogEntryWebPart(ExperimentAnnotations expAnnotations, User user)
    {
        Container container = expAnnotations.getContainer();
        setTitle("Panorama Public Catalog Entry");
        setFrame(WebPartView.FrameType.PORTAL);

        CatalogEntry entry = CatalogEntryManager.getEntryForExperiment(expAnnotations);

        if (entry == null)
        {
            addView(new HtmlView(DIV("This dataset does not have an entry in the Panorama Public slideshow catalog. " +
                            "Click the button below to add an entry.",
                    BR(),
                    new Button.ButtonBuilder("Add Catalog Entry").href(
                            new ActionURL(PanoramaPublicController.AddCatalogEntryAction.class, container)
                                    .addParameter("id", expAnnotations.getId())
                    ))));
        }
        else
        {
            Button changeApprovedStateBtn = null;
            if (user.hasSiteAdminPermission())
            {
                String btnTxt = entry.isPendingApproval() || !entry.isApproved() ? "Approve" : "Reject";
                changeApprovedStateBtn = new Button.ButtonBuilder(btnTxt).href(
                        new ActionURL(PanoramaPublicController.EditCatalogEntryAction.class, container)
                                .addParameter("id", expAnnotations.getId())
                                .addReturnURL(getContextURLHelper())).build();
            }
            addView(new HtmlView(DIV(
                    DIV(B("Approved: "), entry.isPendingApproval() ? "Pending approval" : entry.isApproved() ? "Yes" : "No",
                            changeApprovedStateBtn == null ? "" : changeApprovedStateBtn),
                    DIV(B(U("Description:")), BR(), entry.getDescription()),
                    IMG(at(src, PanoramaPublicController.getCatalogImageDownloadUrl(expAnnotations, entry.getImageFileName()))),
                    BR(), BR(),
                    new Button.ButtonBuilder("Edit").href(
                            new ActionURL(PanoramaPublicController.EditCatalogEntryAction.class, container)
                                    .addParameter("id", expAnnotations.getId())
                                    .addReturnURL(getContextURLHelper())),
                    HtmlString.NBSP,
                    new Button.ButtonBuilder("Delete").href(
                                    new ActionURL(PanoramaPublicController.DeleteCatalogEntryAction.class, container)
                                            .addParameter("id", expAnnotations.getId())
                                            .addReturnURL(getContextURLHelper()))
                            .usePost("Are you sure you want to delete the Panorama Public catalog slideshow entry for this experiment?")
            )));
        }

//        else if(expAnnotations.getContainer().equals(container))
//        {
//            // There is already an experiment defined in this container.
//            PanoramaPublicController.ExperimentAnnotationsDetails experimentDetails = new PanoramaPublicController.ExperimentAnnotationsDetails(getViewContext().getUser(), expAnnotations, fullDetails);
//            JspView<PanoramaPublicController.ExperimentAnnotationsDetails> view = new JspView<>("/org/labkey/panoramapublic/view/expannotations/experimentDetails.jsp", experimentDetails);
//            addView(view);
//            ActionURL url = PanoramaPublicController.getViewExperimentDetailsURL(expAnnotations.getId(), container);
//            setTitleHref(url);
//            if (portalCtx.hasPermission(AdminOperationsPermission.class))
//            {
//                NavTree navTree = new NavTree();
//                navTree.addChild("ProteomeXchange", new ActionURL(PanoramaPublicController.GetPxActionsAction.class, container).addParameter("id", expAnnotations.getId()));
//                navTree.addChild("Data Validation", new ActionURL(PanoramaPublicController.ViewPxValidationsAction.class, container).addParameter("id", expAnnotations.getId()));
//                navTree.addChild("DOI", new ActionURL(PanoramaPublicController.DoiOptionsAction.class, container).addParameter("id", expAnnotations.getId()));
//                navTree.addChild("Make Data Public", new ActionURL(PanoramaPublicController.MakePublicAction.class, container).addParameter("id", expAnnotations.getId()));
//
//                if (expAnnotations.isJournalCopy())
//                {
//                    JournalSubmission submission = SubmissionManager.getSubmissionForJournalCopy(expAnnotations);
//                    ExperimentAnnotations sourceExpt = submission != null ? ExperimentAnnotationsManager.get(submission.getExperimentAnnotationsId()) : null;
//                    if (sourceExpt != null)
//                    {
//                        navTree.addChild("Source Experiment", PageFlowUtil.urlProvider(ProjectUrls.class).getBeginURL(sourceExpt.getContainer()));
//                    }
//                }
//                setNavMenu(navTree);
//            }
//        }
    }

    public static boolean canBeDisplayed(ExperimentAnnotations expAnnotations, User user)
    {
        return expAnnotations.isJournalCopy() // This is an experiment in the Panorama Public project
                && expAnnotations.isPublic() // The folder is public
                && expAnnotations.getContainer().hasOneOf(user, Set.of(AdminPermission.class, PanoramaPublicSubmitterPermission.class));
    }
}
