package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.FederatedBaseUserDao;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Provides common functions for federated identity providers.
 *
 * @param <T>
 */
public abstract class LdapFederatedGenericRepository<T extends FederatedBaseUser> extends LdapGenericRepository<T> implements FederatedBaseUserDao<T>{

    @Override
    public void addUser(IdentityProvider provider, T user) {
        Assert.isTrue(user.getFederatedIdpUri().equals(provider.getUri()), "The user must have the same federated uri as the provider!");
        if (StringUtils.isBlank(user.getId())) {
            user.setId(getNextId());
        }
        addObject(getBaseDnWithIdpName(provider.getName()), user);
    }

    @Override
    public void updateUser(T user) {
        updateObject(user);
    }

    @Override
    public T getUserByToken(BaseUserToken token) {
        return getUserById(token.getIssuedToUserId());
    }

    @Override
    public String getNextId() {
        return getUuid();
    }

    @Override
    public String getBaseDn() {
        return EXTERNAL_PROVIDERS_BASE_DN;
    }

    protected String getBaseDnWithIdpName(String idpName) {
        return String.format("ou=%s,ou=%s,%s", EXTERNAL_PROVIDERS_USER_CONTAINER_NAME, idpName, EXTERNAL_PROVIDERS_BASE_DN);
    }

    @Override
    public abstract String getLdapEntityClass();

    @Override
    public abstract String getSortAttribute();
}
