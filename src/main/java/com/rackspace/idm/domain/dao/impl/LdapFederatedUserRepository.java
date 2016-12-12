package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.StaticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Repository operations for external providers that go against provisioned users.
 */
@LDAPComponent
public class LdapFederatedUserRepository extends LdapFederatedGenericRepository<FederatedUser> implements FederatedUserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapFederatedUserRepository.class);

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_RACKSPACE_FEDERATED_PERSON;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public Iterable<FederatedUser> getUsersByDomainId(String domainId) {
        return getObjects(searchFilterGetUsersByDomainId(domainId));
    }

    @Override
    public FederatedUser getUserByUsernameForIdentityProviderId(String username, String identityProviderId) {
        return getObject(searchFilterGetUserByUsername(username), getBaseDnWithIdpId(identityProviderId), SearchScope.ONE);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String identityProviderName) {
        return (Iterable) getObjects(searchFilterGetUsersByDomainId(domainId), getBaseDnWithIdpId(identityProviderName));
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId) {
        return countObjects(searchFilterGetUsersByDomainId(domainId), getBaseDnWithIdpId(identityProviderId));
    }

    @Override
    public Iterable<Group> getGroupsForFederatedUser(String userId) {
        // Just need to be implemented in the SQL profile.
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByGroupId(String groupId) {
        // Just need to be implemented in the SQL profile.
        throw new UnsupportedOperationException();
    }

    @Override
    public FederatedUser getUserById(String id) {
        return getObject(searchFilterGetUserById(id), SearchScope.SUB);
    }

    @Override
    public void deleteUser(FederatedUser federatedBaseUser) {
        deleteObject(federatedBaseUser);
    }

    @Override
    public FederatedUser getSingleExpiredFederatedUser() {
        try {
            final PaginatorContext<FederatedUser> page = getObjectsPaged(searchFilterExpired(), getBaseDn(), SearchScope.SUB, 0, 1);
            final List<FederatedUser> list = page.getTotalRecords() == 0 ? Collections.EMPTY_LIST : page.getValueList();
            return list.size() > 0 ? list.get(0) : null;
        } catch (Exception e) {
            LOGGER.error("Error retrieving expired federated user", e);
            return null;
        }
    }

    private Filter searchFilterGetUserById(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, getLdapEntityClass()).build();
    }
    private Filter searchFilterGetUserByUsername(String username) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_OBJECT_CLASS,  getLdapEntityClass()).build();
    }

    private Filter searchFilterGetUsersByDomainId(String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, getLdapEntityClass()).build();
    }

    private Filter searchFilterExpired() {
        return new LdapSearchBuilder()
                .addOrAttributes(Arrays.asList(
                        Filter.createLessOrEqualFilter(ATTR_FEDERATED_USER_EXPIRED_TIMESTAMP,  StaticUtils.encodeGeneralizedTime(new Date())),
                        Filter.createNOTFilter(Filter.createPresenceFilter(ATTR_FEDERATED_USER_EXPIRED_TIMESTAMP))
                ))
                .addEqualAttribute(ATTR_OBJECT_CLASS, getLdapEntityClass()).build();
    }

}
