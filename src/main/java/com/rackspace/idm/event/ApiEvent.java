package com.rackspace.idm.event;

import javax.annotation.Nullable;

public interface ApiEvent {
    @Nullable
    String getRemoteIp();

    @Nullable
    String getForwardedForIp();

    IdentityApiResourceRequest getResourceContext();

    /**
     * The transactionId associated with the event
     *
     * @return
     */
    String getRequestId();

    /**
     * Unique identifier of the API Node processing the event
     * @return
     */
    String getNodeName();

    /**
     * A classification of the event.
     *
     * @return
     */
    ApiResourceType getResourceType();
}
