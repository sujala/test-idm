package com.rackspace.idm.api.resource.cloud.email.impl;

import com.rackspace.idm.api.resource.cloud.email.EmailClient;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.DocumentService;
import com.rackspace.idm.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

@Component
public class MailTransferAgentClient implements EmailClient {

    private static final String MAIL_SMTP_HOST = "mail.smtp.host";
    private static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
    private static final String MAIL_SMTP_CONNECTIONTIMEOUT = "smtp.connectiontimeout";
    private static final String MAIL_SMTP_PORT = "mail.smtp.port";

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    DocumentService documentService;

    private static final String ONE_MINUTE = "60000";
    private static final String USERNAME = "[USERNAME]";
    private static final String RACKSPACE_EMAIL = "@rackspace.com";
    private static final String FORBIDDEN_MESSAGE = "Sending emails to external email accounts is NOT enabled";
    private static final String LOCKED_OUT_ERROR_MSG = "Error sending mfa locked out email to user '%s'";
    private static final String ENABLED_ERROR_MSG = "Error sending mfa enabled email to user '%s'";
    private static final String DISABLED_ERROR_MSG = "Error sending mfa disabled email to user '%s'";
    private static final Logger logger = LoggerFactory.getLogger(MailTransferAgentClient.class);

    private Properties properties;
    private Session session;
    private String from;
    private String lockedOutEmail;
    private String lockedOutEmailSubject;
    private String enabledEmail;
    private String enabledEmailSubject;
    private String disabledEmail;
    private String disabledEmailSubject;

    @PostConstruct
    private void postConstruct() {
        properties = getSessionProperties();
        session = Session.getDefaultInstance(properties);

        from = identityConfig.getEmailFromAddress();

        lockedOutEmail = documentService.getMFALockedOutEmail();
        lockedOutEmailSubject = identityConfig.getEmailLockedOutSubject();

        enabledEmail = documentService.getMFAEnabledEmail();
        enabledEmailSubject = identityConfig.getEmailMFAEnabledSubject();

        disabledEmail = documentService.getMFADisabledEmail();
        disabledEmailSubject = identityConfig.getEmailMFADisabledSubject();

        Assert.notNull(from);
        Assert.notNull(lockedOutEmail);
        Assert.notNull(lockedOutEmailSubject);
        Assert.notNull(enabledEmail);
        Assert.notNull(enabledEmailSubject);
        Assert.notNull(disabledEmail);
        Assert.notNull(disabledEmailSubject);
    }

    @Override
    @Async
    public void asyncSendMultiFactorLockedOutMessage(User user) {
        sendMultiFactorLockoutOutMessage(user);
    }

    @Override
    public boolean sendMultiFactorLockoutOutMessage(User user) {
        String body = lockedOutEmail.replace(USERNAME, user.getUsername());
        return sendEmail(user, lockedOutEmailSubject, body, LOCKED_OUT_ERROR_MSG);
    }

    @Override
    @Async
    public void asyncSendMultiFactorEnabledMessage(User user) {
        sendMultiFactorEnabledMessage(user);
    }

    @Override
    public boolean sendMultiFactorEnabledMessage(User user) {
        String body = enabledEmail.replace(USERNAME, user.getUsername());
        return sendEmail(user, enabledEmailSubject, body, ENABLED_ERROR_MSG);
    }

    @Override
    @Async
    public void asyncSendMultiFactorDisabledMessage(User user) {
        sendMultiFactorDisabledMessage(user);
    }

    @Override
    public boolean sendMultiFactorDisabledMessage(User user) {
        String body = disabledEmail.replace(USERNAME, user.getUsername());
        return sendEmail(user, disabledEmailSubject, body, DISABLED_ERROR_MSG);
    }

    @Override
    public boolean sendForgotPasswordMessage(User user, ScopeAccess token, String portal) {
        String body = String.format("%s", token.getAccessTokenString());
        return sendEmail(user, "Hosting Password Reset Instructions", body, String.format("Error sending password reset token to portal '%s'", portal));
    }

    @Override
    @Async
    public void asyncSendForgotPasswordMessage(User user, ScopeAccess token, String portal) {
        sendForgotPasswordMessage(user, token, portal);
    }

    private boolean sendEmail(User user, String subject, String body, String errorMsg) {
        Session localSession = session;

        /*
        Hack to allow changing the SMTP server at runtime during integration testing without impacting production
        runtime by changing the presumably static properties.
         */
        if (identityConfig.getReloadableConfig().createEmailSessionPerEmail()) {
            Properties emailServerProps = getSessionProperties();
            localSession = Session.getInstance(emailServerProps);
        }

        boolean success = true;
        try {
            String to = user.getEmail();
            Assert.notNull(to);
            Assert.notNull(subject);
            Assert.notNull(body);

            if (identityConfig.isSendToOnlyRackspaceAddressesEnabled()) {
                if (!to.toLowerCase().endsWith(RACKSPACE_EMAIL)) {
                    throw new ForbiddenException(FORBIDDEN_MESSAGE);
                }
            }

            MimeMessage message = new MimeMessage(localSession);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
        } catch (Exception ex) {
            logger.error(String.format(errorMsg, user.getUsername()), ex);
            success = false;
        }
        return success;
    }

    private Properties getSessionProperties() {
        Properties properties = System.getProperties();
        properties.setProperty(MAIL_SMTP_HOST, identityConfig.getStaticConfig().getEmailHost());
        properties.setProperty(MAIL_SMTP_TIMEOUT, ONE_MINUTE);
        properties.setProperty(MAIL_SMTP_CONNECTIONTIMEOUT, ONE_MINUTE);
        properties.setProperty(MAIL_SMTP_PORT, identityConfig.getStaticConfig().getEmailPort());
        return properties;
    }
}
