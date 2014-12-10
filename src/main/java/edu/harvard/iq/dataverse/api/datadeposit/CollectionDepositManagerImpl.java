package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.apache.abdera.parser.ParseException;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordEntry;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

public class CollectionDepositManagerImpl implements CollectionDepositManager {

    private static final Logger logger = Logger.getLogger(CollectionDepositManagerImpl.class.getCanonicalName());
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;
    @EJB
    EjbDataverseEngine engineSvc;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    ForeignMetadataImportServiceBean foreignMetadataImportService;
    @EJB
    SwordServiceBean swordService;
    @EJB
    SettingsServiceBean settingsService;

    @Override
    public DepositReceipt createNew(String collectionUri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {

        AuthenticatedUser user = swordAuth.auth(authCredentials);

        urlManager.processUrl(collectionUri);
        String dvAlias = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("dataverse") && dvAlias != null) {

            logger.fine("attempting deposit into this dataverse alias: " + dvAlias);

            Dataverse dvThatWillOwnDataset = dataverseService.findByAlias(dvAlias);

            if (dvThatWillOwnDataset != null) {

                if (swordAuth.hasAccessToModifyDataverse(user, dvThatWillOwnDataset)) {

                    logger.fine("multipart: " + deposit.isMultipart());
                    logger.fine("binary only: " + deposit.isBinaryOnly());
                    logger.fine("entry only: " + deposit.isEntryOnly());
                    logger.fine("in progress: " + deposit.isInProgress());
                    logger.fine("metadata relevant: " + deposit.isMetadataRelevant());

                    if (deposit.isEntryOnly()) {
                        // do a sanity check on the XML received
                        try {
                            SwordEntry swordEntry = deposit.getSwordEntry();
                            logger.fine("deposit XML received by createNew():\n" + swordEntry.toString());
                        } catch (ParseException ex) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Can not create dataset due to malformed Atom entry: " + ex);
                        }

                        Dataset dataset = new Dataset();
                        dataset.setOwner(dvThatWillOwnDataset);
                        String nonNullDefaultIfKeyNotFound = "";
                        String protocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
                        String authority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
                        String separator = settingsService.getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
                        dataset.setProtocol(protocol);
                        dataset.setAuthority(authority); 
                        dataset.setDoiSeparator(separator);
                        dataset.setIdentifier(datasetService.generateIdentifierSequence(protocol, authority, separator));
                        logger.fine("DS Deposit identifier: " + dataset.getIdentifier());
                        DatasetVersion newDatasetVersion = dataset.getEditVersion();

                        String foreignFormat = SwordUtil.DCTERMS;
                        try {
                            foreignMetadataImportService.importXML(deposit.getSwordEntry().toString(), foreignFormat, newDatasetVersion);
                        } catch (Exception ex) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "problem calling importXML: " + ex);
                        }

                        swordService.addDatasetContact(newDatasetVersion);
                        swordService.addDatasetSubject(newDatasetVersion);

                        Dataset createdDataset = null;
                        try {
                            createdDataset = engineSvc.submit(new CreateDatasetCommand(dataset, user, false));
                        } catch (EJBException | CommandException ex) {
                            Throwable cause = ex;
                            StringBuilder sb = new StringBuilder();
                            sb.append(ex.getLocalizedMessage());
                            while (cause.getCause() != null) {
                                cause = cause.getCause();
                                /**
                                 * @todo move this ConstraintViolationException
                                 * check to CreateDatasetCommand. Can be
                                 * triggered if you don't call
                                 * dataset.setIdentifier() or if you feed it
                                 * date format we don't like. Once this is done
                                 * we should be able to drop EJBException from
                                 * the catch above and only catch
                                 * CommandException
                                 *
                                 * See also Have commands catch
                                 * ConstraintViolationException and turn them
                                 * into something that inherits from
                                 * CommandException · Issue #1009 ·
                                 * IQSS/dataverse -
                                 * https://github.com/IQSS/dataverse/issues/1009
                                 */
                                if (cause instanceof ConstraintViolationException) {
                                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                                        sb.append(" Invalid value: '").append(violation.getInvalidValue()).append("' for ")
                                                .append(violation.getPropertyPath()).append(" at ")
                                                .append(violation.getLeafBean()).append(" - ")
                                                .append(violation.getMessage());
                                    }
                                }
                            }
                            logger.info(sb.toString());
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't create dataset: " + sb.toString());
                        }
                        if (createdDataset != null) {
                            ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                            String baseUrl = urlManager.getHostnamePlusBaseUrlPath(collectionUri);
                            DepositReceipt depositReceipt = receiptGenerator.createDatasetReceipt(baseUrl, createdDataset);
                            return depositReceipt;
                        } else {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Problem creating dataset. Null returned.");
                        }
                    } else if (deposit.isBinaryOnly()) {
                        // get here with this:
                        // curl --insecure -s --data-binary "@example.zip" -H "Content-Disposition: filename=example.zip" -H "Content-Type: application/zip" https://sword:sword@localhost:8181/dvn/api/data-deposit/v1/swordv2/collection/dataverse/sword/
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Binary deposit to the collection IRI via POST is not supported. Please POST an Atom entry instead.");
                    } else if (deposit.isMultipart()) {
                        // get here with this:
                        // wget https://raw.github.com/swordapp/Simple-Sword-Server/master/tests/resources/multipart.dat
                        // curl --insecure --data-binary "@multipart.dat" -H 'Content-Type: multipart/related; boundary="===============0670350989=="' -H "MIME-Version: 1.0" https://sword:sword@localhost:8181/dvn/api/data-deposit/v1/swordv2/collection/dataverse/sword/hdl:1902.1/12345
                        // but...
                        // "Yeah, multipart is critically broken across all implementations" -- http://www.mail-archive.com/sword-app-tech@lists.sourceforge.net/msg00327.html
                        throw new UnsupportedOperationException("Not yet implemented");
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "expected deposit types are isEntryOnly, isBinaryOnly, and isMultiPart");
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + user.getDisplayInfo().getTitle() + " is not authorized to create a dataset in this dataverse.");
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataverse: " + dvAlias);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not determine target type or identifier from URL: " + collectionUri);
        }
    }

}
