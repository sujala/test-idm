package com.rackspace.idm.multifactor.providers;

public interface ProviderAvailability {

    /**
     * Returns whether or not the provider is available by doing a simple status check. This does not guarantee that any particular
     * service is available, only that the provider is responding to "ping" requests (which may have different meanings per provider).
     *
     * @return
     */
    boolean available();
}
