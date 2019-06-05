package com.rackspace.idm.api.resource.cloud.email;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class MailTransferAgentClient implements EmailClient {

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

    public static final String PHONE_PIN_LOCKED_TEMPLATE_DIR_NAME = "phone_pin_locked";
    public static final String PHONE_PIN_LOCKED_EMAIL_BASE_DIR =  TEMPLATE_DIR + File.separator + PHONE_PIN_LOCKED_TEMPLATE_DIR_NAME;
    public static final String PHONE_PIN_PREFIX = "phone_pin_locked";

    @Autowired
    private IdentityConfig identityConfig;

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

    private static final BigDecimal BIG_DECIMAL_60 = new BigDecimal(60);

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

        // Set the validity time period for the token in minutes (rounding up)
        Instant tokenCreation = Instant.ofEpochMilli(token.getCreateTimestamp().getTime());
        Instant tokenExpiration = Instant.ofEpochMilli(token.getAccessTokenExp().getTime());
        Duration validityPeriod = Duration.between(tokenCreation, tokenExpiration);

        // Fixing per CID-1996 to account for discrepancy between expiration date being set by adding duration to an older datetime than creation date
        long validitySeconds = validityPeriod.getSeconds();
        if (validityPeriod.getNano() > 0) {
            // If there are nanoseconds, round up to the next second.
            validitySeconds++;
        }
        BigDecimal validityLengthMinutes = new BigDecimal(validitySeconds).divide(BIG_DECIMAL_60, RoundingMode.UP); // Convert seconds to minutes. Rounding up

        model.put(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_VALIDITY_PERIOD_PROP, validityLengthMinutes);
        model.put(EmailTemplateConstants.FORGOT_PASSWORD_TOKEN_EXPIRATION_PROP, Date.from(tokenExpiration));

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
        String registrationUrl = String.format(
                identityConfig.getReloadableConfig().getUnverifiedUserRegistrationUrlFormat(),
                user.getId(),user.getRegistrationCode());
        model.put(EmailTemplateConstants.INVITE_USER_REGISTRATION_URL, registrationUrl);
        model.put(EmailTemplateConstants.INVITE_TTL_HOURS_PROP,
                identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours());
        model.put(EmailTemplateConstants.INVITE_YEAR_PROP, Calendar.getInstance().get(Calendar.YEAR));

        // Calc the config
        EmailConfig emailConfig = emailConfigBuilder.buildEmailConfig(Collections.singletonList(user.getEmail()), UNVERIFIED_USER_EMAIL_BASE_DIR, INVITE_PREFIX);

        try {
            emailService.sendTemplatedMultiPartMimeEmail(emailConfig, model);
            return true;
        } catch (Exception ex) {
            String errMsg = String.format("Attempted to send invite to unverified user '%s', but encountered an error sending the email.", user.getId());
            logger.error(errMsg, ex);
            return false;
        }
    }

    @Override
    @Async
    public void asyncSendUnverifiedUserInviteMessage(User user) {
        sendUnverifiedUserInviteMessage(user);
    }

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

    @Async
    @Override
    public void asyncSendPhonePinLockedEmail(EndUser user) {
        sendPhonePinLockedEmail(user);
    }

    @Override
    public boolean sendPhonePinLockedEmail(EndUser user) {
        Validate.notNull(user);
        Validate.notEmpty(user.getEmail());
        Validate.notEmpty(user.getUsername());

        // Populate the velocity model
        Map<String, Object> model = new HashMap<>();
        model.put(EmailTemplateConstants.PHONE_PINE_LOCKED_USERNAME_PROP, user.getUsername());
        model.put(EmailTemplateConstants.INVITE_YEAR_PROP, Calendar.getInstance().get(Calendar.YEAR));

        // Calc the config
        EmailConfig emailConfig = emailConfigBuilder.buildEmailConfig(Collections.singletonList(user.getEmail()), PHONE_PIN_LOCKED_EMAIL_BASE_DIR, PHONE_PIN_PREFIX);

        boolean success = sendEmail(emailConfig, model);

        if (success) {
            Audit.logSendingPhonePinLockedEmailSuccess(user);
        } else {
            Audit.logSendingPhonePinLockedEmailFailure(user);
        }

        return success;
    }

    private boolean sendEmail(EmailConfig emailConfig, Map<String, Object> model) {
        try {
            emailService.sendTemplatedMultiPartMimeEmail(emailConfig, model);
            return true;
        } catch (Exception e) {
            logger.error("Unable to send email due to exception: {}", e);
            return false;
        }
    }

}
