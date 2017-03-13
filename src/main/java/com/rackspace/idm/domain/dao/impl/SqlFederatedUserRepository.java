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
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.rackspace.idm.domain.dao.impl.LdapRepository.CONTAINER_ROLES;

@SQLComponent
public class SqlFederatedUserRepository implements FederatedUserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFederatedUserRepository.class);

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
    public void updateUserAsIs(FederatedUser user) {
        throw new NotImplementedException("Not Implemented");
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
    public Iterable<FederatedUser> getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(List<String> approvedDomainIds, String identityProviderId) {
        throw new NotImplementedException();
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String identityProviderName) {
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findByDomainIdAndFederatedIdpId(domainId, identityProviderName));
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId) {
        return federatedUserRepository.countByDomainIdAndFederatedIdpId(domainId, identityProviderId);
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
    public FederatedUser getUserByUsernameForIdentityProviderId(String username, String identityProviderId) {
        return federatedUserRaxMapper.fromSQL(federatedUserRepository.findOneByUsernameAndFederatedIdpId(username, identityProviderId));
    }

    @Override
    public void deleteUser(FederatedUser federatedUser) {
        federatedUserRepository.delete(federatedUser.getId());
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, federatedUser.getUniqueId(), null));
    }

    @Override
    public FederatedUser getSingleExpiredFederatedUser() {
        try {
            final SqlFederatedUserRax user  = federatedUserRepository.findFirstByExpiredTimestampLessThanEqual(new Date());
            return federatedUserRaxMapper.fromSQL(user);
        } catch (Exception e) {
            LOGGER.error("Error retrieving expired federated user", e);
            return null;
        }
    }

    @Override
    public int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    private String getNextId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
