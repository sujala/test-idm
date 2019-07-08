package com.rackspace.idm.domain.security.tokencache;

import com.rackspace.idm.domain.entity.ScopeAccess;
import lombok.Getter;

import java.time.Instant;
import java.util.Date;

@Getter
public class TokenCacheEntry {
    private String accessTokenStr;
    private Instant creationDate;
    private Instant expirationDate;
    private String uniqueId;

    public TokenCacheEntry(String accessTokenStr, Instant creationDate, Instant expirationDate, String uniqueId) {
        this.accessTokenStr = accessTokenStr;
        this.creationDate = creationDate;
        this.expirationDate = expirationDate;
        this.uniqueId = uniqueId;
    }

    public TokenCacheEntry(ScopeAccess token) {
        this.accessTokenStr = token.getAccessTokenString();
        this.creationDate = token.getCreateTimestamp().toInstant();
        this.expirationDate = token.getAccessTokenExp().toInstant();
        this.uniqueId = token.getUniqueId();
    }
}