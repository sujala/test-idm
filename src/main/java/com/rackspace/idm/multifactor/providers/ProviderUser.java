package com.rackspace.idm.multifactor.providers;

/**
 * Identifies a particular user within a 3rd party system.
 */
public interface ProviderUser {

    /**
     * A string that uniquely identifier of the user within the external provider.
     *
     * @return
     */
    String getProviderId();
}
