package com.rackspace.idm.domain.security.tokencache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.exception.IdmException;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@JsonRootName(value = "tokenCacheConfig")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class TokenCacheConfigJson {
    private static final Logger LOG = LoggerFactory.getLogger(TokenCacheConfigJson.class);

    public static final String WILDCARD_IDENTITY_USER = "*";
    public static final String WILDCARD_IAM_USER = "iam:*";
    public static final Duration ZERO_DURATION = Duration.parse("PT0S");

    private static ObjectMapper mapper;

    private boolean enabled = false;
    private int maxSize = 0;
    private List<CacheableUserJson> cacheableUsers;

    static {
        mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public TokenCacheConfigJson(@JsonProperty("enabled") boolean enabled, @JsonProperty("maxSize") int maxSize, @JsonProperty("cacheableUsers") List<CacheableUserJson> cacheableUsers) {
        this.enabled = enabled;
        this.maxSize = maxSize;
        this.cacheableUsers = cacheableUsers;
    }

    public static TokenCacheConfigJson fromJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            TokenCacheConfigJson policy = mapper.readValue(json, TokenCacheConfigJson.class);
            return policy;
        } catch (Exception e) {
            throw new IdmException("Error processing token cache config", ErrorCodes.ERROR_CODE_INVALID_VALUE, e);
        }
    }

    /**
     * Finds the associated cacheable user for the given parameters. The matching occurs as follows:
     *
     * 1. Return the first direct match (not via wildcard)
     * 2. If no direct match, return the first wildcard match
     * 3. Else return null
     *
     * @param userManagementSystem
     * @param userId
     * @param authByMethods
     * @return
     */
    @JsonIgnore
    public CacheableUserJson findConfigForUserWithAuthMethods(UserManagementSystem userManagementSystem, String userId, List<String> authByMethods) {
        CacheableUserJson matchedUser = null;

        // If caching is disabled, always return 0 duration
        if (isEnabled()) {
            for (CacheableUserJson cacheableUser : cacheableUsers) {
                if (!cacheableUser.matchesUserAuthentication(userManagementSystem, userId, authByMethods)) {
                    continue;
                }

                // If matching user is an explicit match, immediately return.
                if (!cacheableUser.isWildCardUser()) {
                    // If direct match, we stop immediately. First one found wins.
                    matchedUser = cacheableUser;
                    break;
                } else if (matchedUser == null) {
                    // We only match on the first default match, but we keep looping in case there's a direct match later.
                    matchedUser = cacheableUser;
                }
            }
        }

        // return the default only if no exact match found. If no default, return ZERO duration
        return matchedUser;
    }

    public String toJson() throws JsonProcessingException {
        String json = mapper.writeValueAsString(this);
        return json;
    }

    @Override
    public String toString() {
        try {
            return toJson();
        } catch (JsonProcessingException e) {
            LOG.debug("Error converting token cache config to json", e);
            return "Error converting representation to string. Object representation: TokenCacheConfigJson{" +
                    "enabled=" + enabled +
                    ", maxSize=" + maxSize +
                    ", cacheableUsers=" + cacheableUsers +
                    '}';
        }
    }
}
