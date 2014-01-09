package com.rackspace.idm.multifactor.providers.duo.config;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IDM property file based configuration for accessing the Duo Security <a href="http://www.duosecurity.com/docs/authapi">Auth API</a>
 *
 */
@Component
public class ApacheConfigAuthApiConfig extends ApacheConfigDuoSecurityConfig implements AuthApiConfig {

    public static final String INTEGRATION_KEY_PROP_NAME = "duo.security.auth.integration.key";
    public static final String SECRET_KEY_PROP_NAME = "duo.security.auth.secret.key";
    public static final String API_HOSTNAME_PROP_NAME = "duo.security.auth.api.hostname";
    public static final String API_CONNECTION_TIMEOUT_PROP_NAME = "duo.security.auth.connection.timeout";

    @Autowired
    public ApacheConfigAuthApiConfig(Configuration globalConfig) {
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
}

