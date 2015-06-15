package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Repository operations for external providers that go against provisioned users.
 */
@Component
public class LdapFederatedUserRepository extends LdapFederatedGenericRepository<FederatedUser> implements FederatedUserDao {
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
    public FederatedUser getUserByUsernameForIdentityProviderName(String username, String identityProviderName) {
        return getObject(searchFilterGetUserByUsername(username), getBaseDnWithIdpName(identityProviderName), SearchScope.ONE);
    }

    @Override
    public FederatedUser getUserById(String id) {
        return getObject(searchFilterGetUserById(id), SearchScope.SUB);
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
}
