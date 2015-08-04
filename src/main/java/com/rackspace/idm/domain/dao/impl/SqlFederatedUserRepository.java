package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedUserRax;
import com.rackspace.idm.domain.sql.mapper.impl.FederatedUserRaxMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.UUID;

@SQLComponent
public class SqlFederatedUserRepository  implements FederatedUserDao {

    @Autowired
    private FederatedUserRaxMapper federatedUserRaxMapper;

    @Autowired
    private FederatedUserRepository federatedUserRepository;

    @Override
    public void addUser(IdentityProvider provider, FederatedUser user) {
        Assert.isTrue(user.getFederatedIdpUri().equals(provider.getUri()), "The user must have the same federated uri as the provider!");
        if (StringUtils.isBlank(user.getId())) {
            user.setId(getNextId());
        }
        federatedUserRepository.save(federatedUserRaxMapper.toSQL(user));
    }

    @Override
    public void updateUser(FederatedUser user) {
        SqlFederatedUserRax sqlFederatedUserRax = federatedUserRepository.findOne(user.getId());
        sqlFederatedUserRax = federatedUserRaxMapper.toSQL(user, sqlFederatedUserRax);
        federatedUserRepository.save(sqlFederatedUserRax);
    }

    @Override
    public FederatedUser getUserById(String id) {
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findOne(id));
    }

    @Override
    public Iterable<FederatedUser> getUsersByDomainId(String domainId) {
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findByDomainId(domainId));
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderName(String domainId, String identityProviderName) {
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findByDomainIdAndFederatedIdpUri(domainId, identityProviderName));
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String identityProviderName) {
        return federatedUserRepository.countByDomainIdAndFederatedIdpUri(domainId, identityProviderName);
    }

    @Override
    public FederatedUser getUserByToken(BaseUserToken token) {
        return getUserById(token.getIssuedToUserId());
    }

    @Override
    public FederatedUser getUserByUsernameForIdentityProviderName(String username, String identityProviderName) {
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findOneByUsernameAndFederatedIdpUri(username, identityProviderName));
    }

    private String getNextId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
