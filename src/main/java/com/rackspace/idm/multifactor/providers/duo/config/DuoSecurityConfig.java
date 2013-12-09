package com.rackspace.idm.multifactor.providers.duo.config;

/**
 * Access to Duo Security REST services are provided through 'integrations'. Each integration has an integration key, secret key, and api host name associated with it. These values must
 * be used when consuming Duo Security services. 'Integrations' and the associated property values can be retrieved by logging into the <a href="https://admin.duosecurity.com/login?next=%2F">Duo Security website </a>.
 *
 * Each integration is associated with a distinct set of services exposed by Duo Security. For example, the Duo Security 'Admin API' provides services to create user accounts, phones, etc. The
 * Duo Security 'Verify API' provides services to send an SMS message to an arbitrary phone in order to provide a pin code. There is a one to one mapping between an 'integration' and one of these apis.
 */
public interface DuoSecurityConfig {
    /**
     * Integration key associated with the Duo Security integration.
     */
    String getIntegrationKey();

    /**
     * Secret key associated with the Duo Security integration.
     */
    String getSecretKey();

    /**
     * Api hostname associated with the Duo Security integration.
     */
    String getApiHostName();

    /**
     * Connection timeout to use for service calls used for this integration.
     */
    int getDefaultTimeout();
}
