package com.rackspace.idm.event;

import javax.annotation.Nullable;

/**
 * Provides canned data about requests for protected resources. These requests may be concurrently processed by multiple
 * concurrent listeners - both synchronously and asynchronously.
 *
 * Ideally this event would have used higher level entity objects (e.g. BaseUser), but not all API methods use the
 * requestContext to populate the the effective caller. Until they do we just pass in fine grained data. Can't create
 */
public interface PrivateApiEvent extends ApiEvent {
    @Nullable
    String getCallerToken();

    /**
     * Returns the token used for determining authorization for service. If an impersonation token was used, this would
     * return the user token dynamically generated that replaces the impersonation token.
     * @return
     */
    @Nullable
    String getEffectiveCallerToken();

    /**
     * The original "caller" on the protected API (e.g the owner of the x-auth-token provided in call). If the caller
     * is impersonating another user (e.g. x-auth-token is an
     * impersonation token), then this returns the impersonator info (not the info on the person being impersonated)
     *
     * Must always return a value. If data is unknown should populate the caller with data signifying data is unknown.
     *
     * @return
     */
    Caller getCaller();

    /**
     * The caller of the API service as seen from the perspective of the service itself. When the "real" caller is impersonating another
     * user (e.g. x-auth-token is an impersonation token), the "caller" from most services' perspective is the
     * user being impersonated (not the user doing the impersonation). The few exceptions to this include validation and
     * token revocation.
     *
     * Must always return a value. If data is unknown should populate the caller with data signifying data is unknown.
     *
     * @return
     */
    Caller getEffectiveCaller();
}
