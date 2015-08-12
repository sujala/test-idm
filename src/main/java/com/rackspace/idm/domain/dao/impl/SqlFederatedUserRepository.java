package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository;
import com.rackspace.idm.domain.sql.dao.GroupRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedUserRax;
import com.rackspace.idm.domain.sql.mapper.impl.FederatedUserRaxMapper;
import com.rackspace.idm.domain.sql.mapper.impl.GroupMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.UUID;

@SQLComponent
public class SqlFederatedUserRepository  implements FederatedUserDao {

    @Autowired
    private FederatedUserRaxMapper federatedUserRaxMapper;

    @Autowired
    private FederatedUserRepository federatedUserRepository;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupRepository groupRepository;

    @Override
    @Transactional
    public void addUser(IdentityProvider provider, FederatedUser user) {
        Assert.isTrue(user.getFederatedIdpUri().equals(provider.getUri()), "The user must have the same federated uri as the provider!");
        if (StringUtils.isBlank(user.getId())) {
            user.setId(getNextId());
        }
        federatedUserRepository.save(federatedUserRaxMapper.toSQL(user));
        user.setUniqueId(FederatedUserRaxMapper.UNIQUE_ID_PLACEHOLDER);
    }

    @Override
    @Transactional
    public void updateUser(FederatedUser user) {
        federatedUserRepository.save(federatedUserRaxMapper.toSQL(user, federatedUserRepository.findOne(user.getId())));
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
    public Iterable<Group> getGroupsForFederatedUser(String userId) {
        try {
            final SqlFederatedUserRax federatedUserRax = federatedUserRepository.findOne(userId);
            if (federatedUserRax != null) {
                return groupMapper.fromSQL(groupRepository.findAll(federatedUserRax.getRsGroupId()));
            }
        } catch (Exception ignored) {
        }
        return Collections.EMPTY_SET;
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByGroupId(String groupId) {
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findByRsGroupIdIn(Collections.singleton(groupId)));
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
