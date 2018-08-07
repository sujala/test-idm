package com.rackspace.idm.api.resource.cloud.email;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.DocumentService;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.mail.Session;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
public class MailTransferAgentClient implements EmailClient {

    private static final String MAIL_SMTP_HOST = "mail.smtp.host";
    private static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
    private static final String MAIL_SMTP_CONNECTIONTIMEOUT = "smtp.connectiontimeout";
    private static final String MAIL_SMTP_PORT = "mail.smtp.port";

    /**
     * Path where we store all the email templates within the config directory
     */
    public static final String TEMPLATE_DIR = "email_templates";

    public static final String FORGOT_PASSWORD_TEMPLATE_DIR_NAME = "forgot_password";
    public static final String FORGOT_PASSWORD_EMAIL_BASE_DIR =  TEMPLATE_DIR + File.separator + FORGOT_PASSWORD_TEMPLATE_DIR_NAME;

    public static final String FORGOT_PASSWORD_PREFIX = "fp";

    public static final String MFA_TEMPLATE_DIR_NAME = "multifactor";
    public static final String MFA_EMAIL_BASE_DIR = TEMPLATE_DIR + File.separator + MFA_TEMPLATE_DIR_NAME;
    public static final String MFA_ENABLED_PREFIX = "enabled";
    public static final String MFA_DISABLED_PREFIX = "disabled";
    public static final String MFA_LOCKED_OUT_PREFIX = "lockedout";


    public static final String UNVERIFIED_USER_TEMPLATE_DIR_NAME = "unverified_user";
    public static final String UNVERIFIED_USER_EMAIL_BASE_DIR =  TEMPLATE_DIR + File.separator + UNVERIFIED_USER_TEMPLATE_DIR_NAME;
    public static final String INVITE_PREFIX = "invite";

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EmailConfigBuilder emailConfigBuilder;

    @Autowired
    private EmailService emailService;

    private static final String ONE_MINUTE = "60000";
    private static final String USERNAME = "[USERNAME]";
    public static final String RACKSPACE_EMAIL = "@rackspace.com";
    public static final String FORBIDDEN_MESSAGE = "Sending emails to external email accounts is NOT enabled";
    private static final String LOCKED_OUT_ERROR_MSG = "Error sending mfa locked out email to user '%s'";
    private static final String ENABLED_ERROR_MSG = "Error sending mfa enabled email to user '%s'";
    private static final String DISABLED_ERROR_MSG = "Error sending mfa disabled email to user '%s'";
    private static final Logger logger = LoggerFactory.getLogger(MailTransferAgentClient.class);

    private Properties properties;

    @Setter
    private Session session;

    private String lockedOutEmail;
    private String lockedOutEmailSubject;
    private String enabledEmail;
    private String enabledEmailSubject;
    private String disabledEmail;
    private String disabledEmailSubject;

    @PostConstruct
    private void postConstruct() {
        properties = getSessionProperties();
        session = Session.getInstance(properties);

        lockedOutEmail = documentService.getMFALockedOutEmail();
        lockedOutEmailSubject = identityConfig.getEmailLockedOutSubject();

        enabledEmail = documentService.getMFAEnabledEmail();
        enabledEmailSubject = identityConfig.getEmailMFAEnabledSubject();

        disabledEmail = documentService.getMFADisabledEmail();
        disabledEmailSubject = identityConfig.getEmailMFADisabledSubject();

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
        return sendMfaEmail(user, LOCKED_OUT_ERROR_MSG, MFA_LOCKED_OUT_PREFIX);
    }

    @Override
    @Async
    public void asyncSendMultiFactorEnabledMessage(User user) {
        sendMultiFactorEnabledMessage(user);
    }

    @Override
    public boolean sendMultiFactorEnabledMessage(User user) {
        return sendMfaEmail(user, ENABLED_ERROR_MSG, MFA_ENABLED_PREFIX);
    }

    @Override
    @Async
    public void asyncSendMultiFactorDisabledMessage(User user) {
        sendMultiFactorDisabledMessage(user);
    }

    @Override
    public boolean sendMultiFactorDisabledMessage(User user) {
        return sendMfaEmail(user, DISABLED_ERROR_MSG, MFA_DISABLED_PREFIX);
    }

    @Override
    public boolean sendForgotPasswordMessage(User user, ScopeAccess token, String portal) {
        if (StringUtils.isBlank(portal)) {
            throw new IllegalArgumentException("Portal must be provided");
        }

        /*
        Populate the velocity model
         */
        Map<String, Object> model = new HashMap<String, Object>();
        model.put(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_BEAN_PROP, token);
        model.put(EmailTemplateConstants.FORGOT_PASSWORD_USER_BEAN_PROP, user);
        model.put(EmailTemplateConstants.FORGOT_PASSWORD_USER_NAME_PROP, user.getUsername());
        model.put(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_STRING_PROP, token.getAccessTokenString());

        //set the validity time period for the token
        DateTime tokenCreation = new DateTime(token.getCreateTimestamp());
        DateTime tokenExpiration = new DateTime(token.getAccessTokenExp());
        int validityLength = Minutes.minutesBetween(tokenCreation, tokenExpiration).getMinutes();

        model.put(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_VALIDITY_PERIOD_PROP, validityLength);
        model.put(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_EXPIRATION_PROP, tokenExpiration.toDate());

        //Calc the config
        String basePathForEmailConfig = FORGOT_PASSWORD_EMAIL_BASE_DIR + File.separator + portal.toLowerCase();
        EmailConfig emailConfig = emailConfigBuilder.buildEmailConfig(Arrays.asList(user.getEmail()), basePathForEmailConfig, FORGOT_PASSWORD_PREFIX);

        try {
            emailService.sendTemplatedTextEmail(emailConfig, model);
            return true;
        } catch (Exception ex) {
            logger.error("Attempted to be send a forgot password reset token, but encountered an error sending the email.", ex);
            return false;
        }
    }

    @Override
    @Async
    public void asyncSendForgotPasswordMessage(User user, ScopeAccess token, String portal) {
        sendForgotPasswordMessage(user, token, portal);
    }

    @Override
    public boolean sendUnverifiedUserInviteMessage(User user) {
        Validate.notNull(user);
        Validate.notEmpty(user.getId());
        Validate.isTrue(user.isUnverified());
        Validate.notEmpty(user.getEmail());
        Validate.notEmpty(user.getRegistrationCode());

        // Populate the velocity model
        Map<String, Object> model = new HashMap<>();
        model.put(EmailTemplateConstants.INVITE_USER_ID_PROP, user.getId());
        model.put(EmailTemplateConstants.INVITE_REGISTRATION_CODE_PROP, user.getRegistrationCode());


        // Calc the config
        EmailConfig emailConfig = emailConfigBuilder.buildEmailConfig(Arrays.asList(user.getEmail()), UNVERIFIED_USER_EMAIL_BASE_DIR, INVITE_PREFIX);

        try {
            emailService.sendTemplatedMultiPartMimeEmail(emailConfig, model);
            return true;
        } catch (Exception ex) {
            logger.error("Attempted to be send a unverified user invite, but encountered an error sending the email.", ex);
            return false;
        }
    }

    @Override
    @Async
    public void asyncSendUnverifiedUserInviteMessage(User user) {
        sendUnverifiedUserInviteMessage(user);
    }

    /**
     *
     * @param user
     * @param errorMsg
     * @return
     * @deprecated Should use new velocity mechanism and email service to send email
     */
    @Deprecated
    private boolean sendMfaEmail(User user, String errorMsg, String prefix) {
        //put everything in try block as failure sending email should not be considered failure.
        try {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put(EmailTemplateConstants.MFA_USER_NAME_PROP, user.getUsername());

            //Calc the config. Could reject non-rackspace emails if so configured
            EmailConfig emailConfig = emailConfigBuilder.buildEmailConfig(Arrays.asList(user.getEmail()), MFA_EMAIL_BASE_DIR, prefix);
            emailService.sendTemplatedTextEmail(emailConfig, model);
            return true;
        } catch (Exception ex) {
            logger.error(String.format(errorMsg, user.getUsername()), ex);
            return false;
        }
    }

    /**
     *
     * @deprecated Should use email service to send email
     */
    @Deprecated
    private Properties getSessionProperties() {
        Properties properties = System.getProperties();
        properties.setProperty(MAIL_SMTP_HOST, identityConfig.getStaticConfig().getEmailHost());
        properties.setProperty(MAIL_SMTP_TIMEOUT, ONE_MINUTE);
        properties.setProperty(MAIL_SMTP_CONNECTIONTIMEOUT, ONE_MINUTE);
        properties.setProperty(MAIL_SMTP_PORT, String.valueOf(identityConfig.getStaticConfig().getEmailPort()));
        return properties;
    }
}
