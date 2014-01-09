package com.rackspace.idm.multifactor.providers.duo.config;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IDM property file based configuration for accessing the Duo Security <a href="https://www.duosecurity.com/docs/duoverify">Verify API</a>
 */
@Component
public class ApacheConfigVerifyApiConfig extends ApacheConfigDuoSecurityConfig implements VerifyApiConfig {

    public static final String INTEGRATION_KEY_PROP_NAME = "duo.security.verify.integration.key";
    public static final String SECRET_KEY_PROP_NAME = "duo.security.verify.secret.key";
    public static final String API_HOSTNAME_PROP_NAME = "duo.security.verify.api.hostname";
    public static final String API_CONNECTION_TIMEOUT_PROP_NAME = "duo.security.verify.connection.timeout";
    public static final String PHONE_VERIFICATION_MESSAGE_PROP_NAME = "duo.security.verify.verification.message";

    @Autowired
    public ApacheConfigVerifyApiConfig(Configuration globalConfig) {
        super(globalConfig);
    }

    @Override
    protected String getIntegrationKeyPropertyName() {
        return INTEGRATION_KEY_PROP_NAME;
    }

    @Override
    protected String getSecretKeyPropertyName() {
        return SECRET_KEY_PROP_NAME;
    }

    @Override
    protected String getApiHostnamePropertyName() {
        return API_HOSTNAME_PROP_NAME;
    }

    @Override
    protected String getApiConnectionTimeoutPropertyName() {
        return API_CONNECTION_TIMEOUT_PROP_NAME;
    }

    @Override
    public String getPhoneVerificationMessage() {
        return getGlobalConfig().getString(PHONE_VERIFICATION_MESSAGE_PROP_NAME);
    }
}

