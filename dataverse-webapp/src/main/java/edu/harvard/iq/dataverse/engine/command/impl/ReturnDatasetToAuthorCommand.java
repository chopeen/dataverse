package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import io.vavr.control.Option;

import java.util.List;

@RequiredPermissions(Permission.PublishDataset)
public class ReturnDatasetToAuthorCommand extends AbstractDatasetCommand<Dataset> {

    private final String comment;

    public ReturnDatasetToAuthorCommand(DataverseRequest aRequest, Dataset anAffectedDvObject, String comment) {
        super(aRequest, anAffectedDvObject);
        this.comment = comment;
    }

    @Override
    public Dataset execute(CommandContext ctxt)  {

        final Dataset dataset = getDataset();

        if (!dataset.getLatestVersion().isInReview()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.reject.datasetNotInReview"), this);
        }

        dataset.getEditVersion().setLastUpdateTime(getTimestamp());
        dataset.setModificationTime(getTimestamp());

        ctxt.engine().submit(new RemoveLockCommand(getRequest(), getDataset(), DatasetLock.Reason.InReview));
        WorkflowComment workflowComment = new WorkflowComment(dataset.getEditVersion(), WorkflowComment.Type.RETURN_TO_AUTHOR, comment, (AuthenticatedUser) this.getUser());
        ctxt.datasets().addWorkflowComment(workflowComment);

        updateDatasetUser(ctxt);
        ctxt.em().flush();
        Dataset savedDataset = ctxt.em().merge(dataset);
        
        /*
            So what we're doing here is sending notifications to the authors who do not have publish permissions
            First get users who can publish - or in this case review
            Then get authors.
            Then remove reviewers from the autors list
            Finally send a notification to the remaining (non-reviewing) authors - Hey! your dataset was rejected.
        */
        Option<AuthenticatedUser> requestorOption = Option.of(getUser())
                .filter(User::isAuthenticated)
                .map(user -> (AuthenticatedUser)user)
                .orElse(Option.none());
        List<AuthenticatedUser> reviewers = ctxt.permissions().getUsersWithPermissionOn(Permission.PublishDataset, savedDataset);
        List<AuthenticatedUser> authors = ctxt.permissions().getUsersWithPermissionOn(Permission.EditDataset, savedDataset);
        authors.removeAll(reviewers);
        for (AuthenticatedUser au : authors) {
            requestorOption
                    .peek(user -> ctxt.notifications().sendNotificationWithEmail(au, getTimestamp(), NotificationType.RETURNEDDS,
                            savedDataset.getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION, user, comment));
        }


        ctxt.index().indexDataset(savedDataset, true);

        return savedDataset;

    }


}
