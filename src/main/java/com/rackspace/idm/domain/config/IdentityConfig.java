package com.rackspace.idm.domain.config;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum;
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
    private static final String SCOPED_TOKEN_EXPIRATION_SECONDS = "token.scoped.expirationSeconds";
    private static final String CLOUD_AUTH_CLIENT_ID = "cloudAuth.clientId";

    public static final String IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP = "cloudAuth.adminRole";
    public static final String IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP = "cloudAuth.serviceAdminRole";
    public static final String IDENTITY_USER_ADMIN_ROLE_NAME_PROP = "cloudAuth.userAdminRole";
    public static final String IDENTITY_USER_MANAGE_ROLE_NAME_PROP = "cloudAuth.userManagedRole";
    public static final String IDENTITY_DEFAULT_USER_ROLE_NAME_PROP = "cloudAuth.userRole";

    public static final String IDENTITY_PROVISIONED_TOKEN_FORMAT = "feature.provisioned.defaultTokenFormat";
    private static final String IDENTITY_PROVISIONED_TOKEN_FORMAT_DEFAULT = "UUID";

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
        verifyAndLogProperty(SCOPED_TOKEN_EXPIRATION_SECONDS, REQUIRED);
        verifyAndLogProperty(CLOUD_AUTH_CLIENT_ID, REQUIRED);

        verifyAndLogProperty(IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_USER_ADMIN_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_USER_MANAGE_ROLE_NAME_PROP, REQUIRED);
        verifyAndLogProperty(IDENTITY_DEFAULT_USER_ROLE_NAME_PROP, REQUIRED);
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

    public int getScopedTokenExpirationSeconds() {
        return config.getInt(SCOPED_TOKEN_EXPIRATION_SECONDS);
    }

    public String getCloudAuthClientId() {
      return config.getString(CLOUD_AUTH_CLIENT_ID);
    }

    public String getIdentityUserAdminRoleName() {
        return config.getString(IDENTITY_USER_ADMIN_ROLE_NAME_PROP);
    }

    public String getIdentityIdentityAdminRoleName() {
        return config.getString(IDENTITY_IDENTITY_ADMIN_ROLE_NAME_PROP);
    }

    public String getIdentityServiceAdminRoleName() {
        return config.getString(IDENTITY_SERVICE_ADMIN_ROLE_NAME_PROP);
    }

    public String getIdentityDefaultUserRoleName() {
        return config.getString(IDENTITY_DEFAULT_USER_ROLE_NAME_PROP);
    }

    public String getIdentityUserManagerRoleName() {
        return config.getString(IDENTITY_USER_MANAGE_ROLE_NAME_PROP);
    }

    public String getIdentityProvisionedTokenFormat() {
        return config.getString(IDENTITY_PROVISIONED_TOKEN_FORMAT, IDENTITY_PROVISIONED_TOKEN_FORMAT_DEFAULT);
    }

}
