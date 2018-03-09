package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.FederatedBaseUserDao;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.FederatedBaseUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.service.EncryptionService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Provides common functions for federated identity providers.
 *
 * @param <T>
 */
public abstract class LdapFederatedGenericRepository<T extends FederatedBaseUser> extends LdapGenericRepository<T> implements FederatedBaseUserDao<T>{

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public void addUser(IdentityProvider provider, T user) {
        Assert.isTrue(user.getFederatedIdpUri().equals(provider.getUri()), "The user must have the same federated uri as the provider!");
        if (StringUtils.isBlank(user.getId())) {
            user.setId(getNextId());
        }
        if(user instanceof FederatedUser) {
            encryptionService.setUserEncryptionSaltAndVersion((FederatedUser)user);
        }

        addObject(getBaseDnWithIdpId(provider.getProviderId()), user);
    }

    @Override
    public void updateUser(T user) {
        updateObject(user);
    }

    @Override
    public void updateUserAsIs(T user) {
        updateObjectAsIs(user);
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

    protected String getBaseDnWithIdpId(String idpId) {
        return String.format("ou=%s,ou=%s,%s", EXTERNAL_PROVIDERS_USER_CONTAINER_NAME, idpId, EXTERNAL_PROVIDERS_BASE_DN);
    }

    @Override
    public abstract String getLdapEntityClass();

    @Override
    public abstract String getSortAttribute();
}
