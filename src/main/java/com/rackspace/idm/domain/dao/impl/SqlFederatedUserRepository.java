package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository;
import com.rackspace.idm.domain.sql.dao.GroupRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedUserRax;
import com.rackspace.idm.domain.sql.mapper.impl.FederatedUserRaxMapper;
import com.rackspace.idm.domain.sql.mapper.impl.GroupMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.UUID;

import static com.rackspace.idm.domain.dao.impl.LdapRepository.CONTAINER_ROLES;

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

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addUser(IdentityProvider provider, FederatedUser user) {
        Assert.isTrue(user.getFederatedIdpUri().equals(provider.getUri()), "The user must have the same federated uri as the provider!");
        if (StringUtils.isBlank(user.getId())) {
            user.setId(getNextId());
        }
        final SqlFederatedUserRax sqlFederatedUserRax = federatedUserRepository.save(federatedUserRaxMapper.toSQL(user));

        // Save necessary LDIF for rollback
        final FederatedUser federatedUser = federatedUserRaxMapper.fromSQL(sqlFederatedUserRax, user);
        final String dn = federatedUser.getUniqueId();

        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, federatedUser.getUniqueId(), federatedUserRaxMapper.toLDIF(federatedUser)));
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, federatedUserRaxMapper.toContainerDN(dn, CONTAINER_ROLES),
                federatedUserRaxMapper.toContainerLDIF(dn, CONTAINER_ROLES)));
    }

    @Override
    @Transactional
    public void updateUser(FederatedUser user) {
        final SqlFederatedUserRax sqlFederatedUserRax = federatedUserRepository.save(
                federatedUserRaxMapper.toSQL(user, federatedUserRepository.findOne(user.getId())));

        final FederatedUser federatedUser = federatedUserRaxMapper.fromSQL(sqlFederatedUserRax, user);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, federatedUser.getUniqueId(), federatedUserRaxMapper.toLDIF(federatedUser)));
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
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findByDomainIdAndFederatedIdpName(domainId, identityProviderName));
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String identityProviderName) {
        return federatedUserRepository.countByDomainIdAndFederatedIdpName(domainId, identityProviderName);
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
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findOneByUsernameAndFederatedIdpName(username, identityProviderName));
    }

    @Override
    public void deleteUser(FederatedUser federatedUser) {
        federatedUserRepository.delete(federatedUser.getId());
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, federatedUser.getUniqueId(), null));
    }

    private String getNextId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
