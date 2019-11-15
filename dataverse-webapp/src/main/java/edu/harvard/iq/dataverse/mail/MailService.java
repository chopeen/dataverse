/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mail;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.notification.dto.EmailNotificationDto;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service responsible for mail sending.
 */
@Stateless
public class MailService implements java.io.Serializable {

    private DataverseDao dataverseDao;
    private SettingsServiceBean settingsService;
    private MailMessageCreator mailMessageCreator;

    private Mailer mailSender;
    private ExecutorService executorService;


    @Resource(name = "mail/notifyMailSession")
    private Session session;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /* JEE requirement */
    public MailService() {
    }

    @Inject
    public MailService(DataverseDao dataverseDao, SettingsServiceBean settingsService, MailMessageCreator mailMessageCreator) {
        this.dataverseDao = dataverseDao;
        this.settingsService = settingsService;
        this.mailMessageCreator = mailMessageCreator;
    }

    @PostConstruct
    public void prepareMailSession() {
        mailSender = MailerBuilder
                .usingSession(session)
                .withDebugLogging(true)
                .buildMailer();

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // -------------------- LOGIC --------------------

    /**
     * Gathers template messages for given notification and sends email.
     *
     * @return true if email was sent or false if some error occurred and email could not be sent.
     */
    public Boolean sendNotificationEmail(EmailNotificationDto notification) {

        String userEmail = notification.getUserEmail();
        String systemEmail = settingsService.getValueForKey(Key.SystemEmail);

        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(notification, systemEmail);

        if (messageAndSubject._1().isEmpty() || messageAndSubject._2().isEmpty()) {
            return false;
        }

        return sendMail(userEmail, messageAndSubject._2(), messageAndSubject._1());
    }

    /**
     * Gathers template messages for given notification and sends email.
     *
     * @return true if email was sent or false if some error occurred and email could not be sent.
     */
    public Boolean sendNotificationEmail(EmailNotificationDto notification, AuthenticatedUser requestor) {

        String userEmail = notification.getUserEmail();

        Tuple2<String, String> messageAndSubject = mailMessageCreator.getMessageAndSubject(notification, requestor);

        if (messageAndSubject._1().isEmpty() && messageAndSubject._2().isEmpty()) {
            return false;
        }

        return sendMail(userEmail, messageAndSubject._2(), messageAndSubject._1());
    }

    public CompletableFuture<Boolean> sendMailAsync(String recipientsEmails, String subject, String messageText) {
        return CompletableFuture.supplyAsync(() -> sendMail(recipientsEmails, subject, messageText), executorService);
    }

    public CompletableFuture<Boolean> sendMailAsync(String replyEmail, String recipientsEmails, String subject, String messageText) {
        return CompletableFuture.supplyAsync(() -> sendMail(replyEmail, recipientsEmails, subject, messageText), executorService);
    }

    /**
     * Sends email(s).
     *
     * @param recipientsEmails - comma separated emails.
     */
    public boolean sendMail(String recipientsEmails, String subject, String messageText) {

        String message = mailMessageCreator.createMailFooterMessage(messageText, dataverseDao.findRootDataverse().getName(), getSystemAddress());

        Email email = EmailBuilder.startingBlank()
                .from(getSystemAddress())
                .withRecipients(mailMessageCreator.createRecipients(recipientsEmails, StringUtils.EMPTY))
                .withSubject(subject)
                .appendText(message)
                .buildEmail();

        return Try.run(() -> mailSender.sendMail(email))
                .map(emailSent -> true)
                .onFailure(Throwable::printStackTrace)
                .getOrElse(false);
    }

    /**
     * Sends email(s) with replay email.
     *
     * @param recipientsEmails - comma separated emails.
     */
    public boolean sendMail(String replyEmail, String recipientsEmails, String subject, String messageText) {

        Email email = EmailBuilder.startingBlank()
                .from(getSystemAddress())
                .withRecipients(mailMessageCreator.createRecipients(recipientsEmails, StringUtils.EMPTY))
                .withSubject(subject)
                .withReplyTo(replyEmail)
                .appendText(messageText)
                .buildEmail();

        return Try.run(() -> mailSender.sendMail(email))
                .map(emailSent -> true)
                .onFailure(Throwable::printStackTrace)
                .getOrElse(false);
    }

    // -------------------- PRIVATE --------------------

    private InternetAddress getSystemAddress() {
        String systemEmail = settingsService.getValueForKey(Key.SystemEmail);

        return Try.of(() -> new InternetAddress(systemEmail))
                .getOrElseThrow(throwable -> new IllegalArgumentException("Email will not be sent due to invalid email: " + systemEmail));
    }

    // -------------------- SETTERS --------------------

    void setMailSender(Mailer mailSender) {
        this.mailSender = mailSender;
    }
}
