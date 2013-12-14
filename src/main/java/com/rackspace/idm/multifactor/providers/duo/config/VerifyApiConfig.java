package com.rackspace.idm.multifactor.providers.duo.config;

/**
 * Marker interface for the Verify API configuration
 */
public interface VerifyApiConfig extends DuoSecurityConfig {
    String getPhoneVerificationMessage();
}
