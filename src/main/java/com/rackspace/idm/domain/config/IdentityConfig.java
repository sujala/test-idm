package com.rackspace.idm.domain.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class IdentityConfig {

    private static final String LOCALHOST = "localhost";

    //REQUIRED PROPERTIES
    private static final String EMAIL_FROM_EMAIL_ADDRESS = "email.return.email.address";
    private static final String EMAIL_MFA_ENABLED_SUBJECT = "email.mfa.enabled.subject";
    private static final String EMAIL_MFA_DISABLED_SUBJECT = "email.mfa.disabled.subject";
    private static final String EMAIL_LOCKED_OUT_SUBJECT = "email.locked.out.email.subject";
    private static final String EMAIL_HOST = "email.host";
    private static final String EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES = "email.send.to.only.rackspace.addresses.enabled";

    //OPTIONAL PROPERTIES


    private static final boolean REQUIRED = true;
    private static final boolean OPTIONAL = false;
    private static final String PROPERTY_SET_MESSAGE = "Configuration Property '%s' set with value '%s'";
    private static final String PROPERTY_ERROR_MESSAGE = "Configuration Property '%s' is NOT set but is required";
    private static final Logger logger = LoggerFactory.getLogger(IdentityConfig.class);

    @Autowired
    private Configuration config;

    @PostConstruct
    private void verifyConfigs() {
        // Verify and Log Required Values
        verifyAndLogProperty(EMAIL_FROM_EMAIL_ADDRESS, REQUIRED);
        verifyAndLogProperty(EMAIL_LOCKED_OUT_SUBJECT, REQUIRED);
        verifyAndLogProperty(EMAIL_MFA_ENABLED_SUBJECT, REQUIRED);
        verifyAndLogProperty(EMAIL_MFA_DISABLED_SUBJECT, REQUIRED);
        verifyAndLogProperty(EMAIL_HOST, OPTIONAL);
        verifyAndLogProperty(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, OPTIONAL);
    }

    private void verifyAndLogProperty(String property, boolean required) {
        String readProperty = config.getString(property);
        if (required && StringUtils.isBlank(readProperty)) {
            logger.error(String.format(PROPERTY_ERROR_MESSAGE, property));
        } else {
            logger.warn(String.format(PROPERTY_SET_MESSAGE, property, readProperty));
        }
    }

    public String getEmailFromAddress() {
        return config.getString(EMAIL_FROM_EMAIL_ADDRESS);
    }

    public String getEmailLockedOutSubject() {
        return config.getString(EMAIL_LOCKED_OUT_SUBJECT);
    }

    public String getEmailMFAEnabledSubject() {
        return config.getString(EMAIL_MFA_ENABLED_SUBJECT);
    }

    public String getEmailMFADisabledSubject() {
        return config.getString(EMAIL_MFA_DISABLED_SUBJECT);
    }

    public String getEmailHost() {
        return config.getString(EMAIL_HOST, LOCALHOST);
    }

    public boolean isSendToOnlyRackspaceAddressesEnabled() {
        return config.getBoolean(EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, true);
    }
}
