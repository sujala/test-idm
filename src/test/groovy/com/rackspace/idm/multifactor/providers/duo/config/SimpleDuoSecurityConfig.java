package com.rackspace.idm.multifactor.providers.duo.config;

/**
 * Test helper to allow a static config for testing purposes.
 */
public class SimpleDuoSecurityConfig implements DuoSecurityConfig {
    private String integrationKey;
    private String secretKey;
    private String apiHostname;
    private int defaultTimeout;

    public SimpleDuoSecurityConfig(String integrationKey, String secretKey, String apiHostname, int defaultTimeout) {
        this.integrationKey = integrationKey;
        this.secretKey = secretKey;
        this.apiHostname = apiHostname;
        this.defaultTimeout = defaultTimeout;
    }

    @Override
    public String getIntegrationKey() {
        return integrationKey;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public String getApiHostName() {
        return apiHostname;
    }

    @Override
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    @Override
    public boolean allowServicesThatCostMoney() {
        return true;
    }
}
