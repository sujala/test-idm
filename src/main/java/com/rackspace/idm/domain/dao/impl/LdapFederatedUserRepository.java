package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.Debug;
import com.unboundid.util.StaticUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

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
    public Iterable<FederatedUser> getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(List<String> approvedDomainIds, String identityProviderId) {
        return (Iterable) getObjects(searchFilterGetUsersNotInApprovedDomainIds(approvedDomainIds), getBaseDnWithIdpId(identityProviderId));
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByDomainIdAndIdentityProviderId(String domainId, String identityProviderId) {
        return (Iterable) getObjects(searchFilterGetUsersByDomainId(domainId), getBaseDnWithIdpId(identityProviderId));
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByIdentityProviderId(String identityProviderId) {
        return (Iterable) getObjects(searchFilterGetFederatedUser(), getBaseDnWithIdpId(identityProviderId));
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId) {
        return countObjects(searchFilterGetUsersByDomainId(domainId), getBaseDnWithIdpId(identityProviderId));
    }

    @Override
    public int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(String domainId, String identityProviderId) {
        try {
            return countObjects(searchFilterGetUnexpiredUsersByDomainId(domainId), getBaseDnWithIdpId(identityProviderId));
        } catch (IndexOutOfBoundsException e) {
            /*
             Eating this exception. For some odd reason if this search is done on a newly started up directory that does
             not have an existing fed person with the rsFederatedUserExpiredTimestamp on it, UnboundId does not receive
             from the directory (or incorretly processes the results) such that no searchentry is returned - resulting
             in an IndexOutOfBoundsException.

             In this case, we can just return 0 as there would be NO unexpired users and ignore the exception.
             */
        }
        return 0;
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

    private Filter searchFilterGetFederatedUser() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, getLdapEntityClass()).build();
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

    private Filter searchFilterGetUsersNotInApprovedDomainIds(List<String> approvedDomainIds) {
        LdapSearchBuilder builder = new LdapSearchBuilder();
        for (String domainId : approvedDomainIds) {
            builder.addNotEqualAttribute(ATTR_DOMAIN_ID, domainId);
        }
        return builder.build();
    }

    private Filter searchFilterGetUnexpiredUsersByDomainId(String domainId) {
        /*
         Add 1 ms to search date since a user expiration date == current time would be considered expired and there is
         not simply a "Greater" filter
         */
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addGreaterOrEqualAttribute(ATTR_FEDERATED_USER_EXPIRED_TIMESTAMP, StaticUtils.encodeGeneralizedTime(new DateTime().plusMillis(1).toDate()))
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
