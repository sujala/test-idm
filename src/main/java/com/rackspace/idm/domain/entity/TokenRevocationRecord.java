package com.rackspace.idm.domain.entity;

import com.rackspace.idm.GlobalConstants;

import java.util.*;

public interface TokenRevocationRecord {
    public static final String AUTHENTICATED_BY_WILDCARD_VALUE = "*";
    public static final Set<String> AUTHENTICATED_BY_WILDCARD_SET = new HashSet<String>(Arrays.asList(AUTHENTICATED_BY_WILDCARD_VALUE));
    public static final List<Set<String>> AUTHENTICATED_BY_ALL_TOKENS_LIST = Arrays.asList(TokenRevocationRecord.AUTHENTICATED_BY_WILDCARD_SET);

    public static final Set<String> AUTHENTICATED_BY_PASSWORD_SET = new HashSet<String>(Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSWORD));
    public static final List<Set<String>> AUTHENTICATED_BY_PASSWORD_TOKENS_LIST = Arrays.asList(TokenRevocationRecord.AUTHENTICATED_BY_PASSWORD_SET);

    public static final Set<String> AUTHENTICATED_BY_API_SET = new HashSet<String>(Arrays.asList(GlobalConstants.AUTHENTICATED_BY_APIKEY));
    public static final List<Set<String>> AUTHENTICATED_BY_API_TOKENS_LIST = Arrays.asList(TokenRevocationRecord.AUTHENTICATED_BY_API_SET);

    String getId();

    java.util.Date getCreateTimestamp();

    String getTargetToken();

    String getTargetIssuedToId();

    java.util.Date getTargetCreatedBefore();

    List<Set<String>> getTargetAuthenticatedBy();

    void setId(String id);

    void setTargetToken(String accessTokenString);

    void setTargetIssuedToId(String userRsId);

    void setTargetCreatedBefore(java.util.Date accessTokenExp);

    /**
     * A set that contains the single value "*" means matches all tokens. An empty set means match tokens without any
     * authenticated by.
     *
     * @param authenticatedBy
     */
    void setTargetAuthenticatedBy(List<Set<String>> authenticatedBy);
}
