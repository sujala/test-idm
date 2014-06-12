package com.rackspace.idm.api.resource.cloud.email.impl;

import com.rackspace.idm.api.resource.cloud.email.EmailClient;
import com.rackspace.idm.domain.config.IdentityConfig;
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

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    DocumentService documentService;

    private static final String ONE_MINUTE = "60000";
    private static final String USERNAME = "[USERNAME]";
    private static final String RACKSPACE_EMAIL = "@rackspace.com";
    private static final String FORBIDDEN_MESSAGE = "Sending emails to external email accounts is NOT enabled";
    private static final String LOCKED_OUT_ERROR_MSG = "Error sending locked out email to user '%s'";
    private static final Logger logger = LoggerFactory.getLogger(MailTransferAgentClient.class);

    private Properties properties;
    private Session session;
    private String from;
    private String lockedOutEmail;
    private String lockedOutEmailSubject;

    @PostConstruct
    private void postConstruct() {
        properties = System.getProperties();
        properties.setProperty("mail.smtp.localhost", identityConfig.getEmailHost());
        properties.setProperty("mail.smtp.timeout", ONE_MINUTE);
        properties.setProperty("smtp.connectiontimeout", ONE_MINUTE);

        session = Session.getDefaultInstance(properties);

        from = identityConfig.getEmailFromAddress();

        lockedOutEmail = documentService.getMFALockedOutEmail();
        lockedOutEmailSubject = identityConfig.getEmailLockedOutSubject();

        Assert.notNull(from);
        Assert.notNull(lockedOutEmail);
        Assert.notNull(lockedOutEmailSubject);
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

    private boolean sendEmail(User user, String subject, String body, String errorMsg) {
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

            MimeMessage message = new MimeMessage(session);
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
}
