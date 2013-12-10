package com.rackspace.idm.multifactor.providers;

/**
 */
public interface ProviderPhone {

    /**
     * Returns a string that uniquely identifies the phone within the provider system.
     * @return
     */
    String getProviderId();
}
