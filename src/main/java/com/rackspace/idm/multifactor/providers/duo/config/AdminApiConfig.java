package com.rackspace.idm.multifactor.providers.duo.config;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuration for accessing the admin API
 */
@Component
public class AdminApiConfig extends IdmDuoSecurityConfig {
    public static final String INTEGRATION_KEY_PROP_NAME = "duo.security.admin.integration.key";
    public static final String SECRET_KEY_PROP_NAME = "duo.security.admin.secret.key";
    public static final String API_HOSTNAME_PROP_NAME = "duo.security.admin.api.hostname";
    public static final String API_CONNECTION_TIMEOUT_PROP_NAME = "duo.security.admin.connection.timeout";

    @Autowired
    public AdminApiConfig(Configuration globalConfig) {
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

