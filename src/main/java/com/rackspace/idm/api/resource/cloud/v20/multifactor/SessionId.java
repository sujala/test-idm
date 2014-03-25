package com.rackspace.idm.api.resource.cloud.v20.multifactor;

public interface SessionId {

    /**
     * Version of the sessionId format.
     *
     * @return
     */
    String getVersion();

    String getUserId();

    org.joda.time.DateTime getCreatedDate();

    org.joda.time.DateTime getExpirationDate();

    java.util.List<String> getAuthenticatedBy();
}
