package com.rackspace.idm.domain.security.tokencache;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Racker;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CacheKey {
    String userTypeName;
    String userId;
    List<String> authByMethods;

    public CacheKey(BaseUser user, Collection<String> authByList) {
        Validate.notNull(user);
        Validate.isTrue(StringUtils.isNotBlank(user.getId()));
        Validate.notNull(authByList);

        this.userTypeName = user.getClass().getName();
        this.userId = user.getId();

        // Auth by order is irrelevant. However, Just to simplify things, always sort them alphabetically in cache key
        this.authByMethods = authByList.stream().sorted().collect(Collectors.toList());
    }

    public UserManagementSystem userManagementSystem() {
        return userTypeName.equals(Racker.class.getName()) ? UserManagementSystem.IAM : UserManagementSystem.CID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey cacheKey = (CacheKey) o;
        return userTypeName.equals(cacheKey.userTypeName) &&
                userId.equals(cacheKey.userId) &&
                authByMethods.equals(cacheKey.authByMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userTypeName, userId, authByMethods);
    }

    /**
     * The toString is a convenience for debugging. No guarantees made about the format or content.
     */
    @Override
    public String toString() {
        return "CacheKey{" +
                "userId='" + userId + '\'' +
                ", authByMethods=" + authByMethods +
                '}';
    }
}
