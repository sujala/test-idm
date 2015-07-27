package com.rackspace.idm.domain.entity;

import com.rackspace.idm.GlobalConstants;

import java.util.*;

public interface TokenRevocationRecord {
    public static final String AUTHENTICATED_BY_WILDCARD_VALUE = "*";
    public static final String AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE = "<empty>";
    public static final String AUTHENTICATED_BY_ALL_SUBSTITUTE = "*";

    String getId();

    java.util.Date getCreateTimestamp();

    String getTargetToken();

    String getTargetIssuedToId();

    java.util.Date getTargetCreatedBefore();

    List<AuthenticatedByMethodGroup> getTargetAuthenticatedByMethodGroups();

    void setId(String id);

    void setTargetToken(String accessTokenString);

    void setTargetIssuedToId(String userRsId);

    void setTargetCreatedBefore(java.util.Date accessTokenExp);

    /**
     * A list of authentication groups that define which tokens should be revoked.
     *
     * @param authenticatedByMethodGroups
     */
    void setTargetAuthenticatedByMethodGroups(List<AuthenticatedByMethodGroup> authenticatedByMethodGroups);
}
