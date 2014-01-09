package com.rackspace.idm.multifactor.providers.duo.config;

import org.apache.commons.configuration.Configuration;

/**
 * A base class for retrieving the required configuration properties for a Duo Security 'integration' from an Apache commons configuration. Subclasses
 * will need to specify the names of the properties that should be read.
 */
public abstract class ApacheConfigDuoSecurityConfig implements DuoSecurityConfig {
    private Configuration globalConfig;

    public static final int DEFAULT_CONNECTION_TIMEOUT = 30000;
    public static final String PROP_NAME_DUO_TELEPHONY_ENABLED = "duo.telephony.enabled";

    public ApacheConfigDuoSecurityConfig(Configuration globalConfig) {
        this.globalConfig = globalConfig;
    }

    @Override
    public String getIntegrationKey() {
        return globalConfig.getString(getIntegrationKeyPropertyName());
    }

    @Override
    public String getSecretKey() {
        return globalConfig.getString(getSecretKeyPropertyName());
    }

    @Override
    public String getApiHostName() {
        return globalConfig.getString(getApiHostnamePropertyName());
    }

    @Override
    public int getDefaultTimeout() {
        return globalConfig.getInt(getApiConnectionTimeoutPropertyName(), DEFAULT_CONNECTION_TIMEOUT);
    }

    @Override
    public boolean allowServicesThatCostMoney() {
        return getGlobalConfig().getBoolean(PROP_NAME_DUO_TELEPHONY_ENABLED, false);
    }

    /**
     * Allow subclasses to access the config object.
     * @return
     */
    protected Configuration getGlobalConfig() {
        return globalConfig;
    }

    /**
     * The name of the property in the IDM property file containing the integration key
     * @return
     */
    protected abstract String getIntegrationKeyPropertyName();

    /**
     * The name of the property in the IDM property file containing the secret key
     * @return
     */
    protected abstract String getSecretKeyPropertyName();

    /**
     * The name of the property in the IDM property file containing the host name
     * @return
     */
    protected abstract String getApiHostnamePropertyName();

    /**
     * The name of the property in the IDM property file containing the connection timeout for the calls
     * @return
     */
    protected abstract String getApiConnectionTimeoutPropertyName();
}
