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
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@JsonRootName(value = "cacheableUser")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class CacheableUserJson {
    private static final Logger LOG = LoggerFactory.getLogger(CacheableUserJson.class);

    public static final String WILDCARD_USER = "*";
    public static final String MANAGEMENT_SYSTEM_CID = "CID";
    public static final String MANAGEMENT_SYSTEM_IAM = "IAM";

    private static ObjectMapper mapper;

    private String type;

    private List<String> userIds;

    private List<List<String>> authenticatedByLists;

    private Duration minimumValidityDuration;

    private Duration maximumCacheDuration;

    static {
        mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public CacheableUserJson(@JsonProperty("type") String type, @JsonProperty("userIds") List<String> userIds, @JsonProperty("authenticatedBy") List<List<String>> authenticatedByLists, @JsonProperty("minimumValidityDuration") Duration minimumValidityDuration, @JsonProperty("maximumCacheDuration") Duration maximumCacheDuration) {
        this.type = type;
        this.userIds = userIds;
        this.authenticatedByLists = authenticatedByLists;
        this.minimumValidityDuration = minimumValidityDuration;
        this.maximumCacheDuration = maximumCacheDuration;
    }

    @JsonIgnore
    public boolean isWildCardUser() {
        return userIds.contains(WILDCARD_USER);
    }

    public boolean matchesUserAuthentication(UserManagementSystem userManagementSystem, String userId, List<String> authByMethods) {
        return matchesUser(userManagementSystem, userId) && matchesAuthenticatedByList(authByMethods);
    }

    private boolean matchesAuthenticatedByList(List<String> authByMethods) {
        for (List<String> authenticatedByList : getAuthenticatedByLists()) {
            if (CollectionUtils.isEqualCollection(authenticatedByList, authByMethods)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesUser(UserManagementSystem userManagementSystem, String userId) {
        return matchesUserManagementSystem(userManagementSystem) && ((userIds.contains(userId) || isWildCardUser()));
    }

    private boolean matchesUserManagementSystem(UserManagementSystem userManagementSystem) {
        return userManagementSystem == getUserManagementSystemForUser();
    }

    @JsonIgnore
    private UserManagementSystem getUserManagementSystemForUser() {
        if (MANAGEMENT_SYSTEM_CID.equalsIgnoreCase(type)) {
            return UserManagementSystem.CID;
        }
        else if (MANAGEMENT_SYSTEM_IAM.equalsIgnoreCase(type)) {
            return UserManagementSystem.IAM;
        }
        return null;
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
            return "Error converting representation to string. Object representation: CacheableUserJson{" +
                    "type='" + type + '\'' +
                    ", userIds=" + userIds +
                    ", authenticatedByLists=" + authenticatedByLists +
                    ", minimumValidityDuration=" + minimumValidityDuration +
                    ", maximumCacheDuration=" + maximumCacheDuration +
                    '}';
        }
    }
}
