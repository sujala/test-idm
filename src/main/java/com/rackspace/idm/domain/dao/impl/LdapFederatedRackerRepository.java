package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.FederatedRackerDao;
import com.rackspace.idm.domain.entity.Racker;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.stereotype.Component;

/**
 * Persistence methods for racker based identity providers.
 */
@LDAPComponent
public class LdapFederatedRackerRepository extends LdapFederatedGenericRepository<Racker> implements FederatedRackerDao {
    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_RACKER;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_RACKER_ID;
    }

    /**
     * Rackers are stored in identity providers uniquely by the rackerId (which is considered the id).
     *
     * @param id
     * @return
     */
    @Override
    public Racker getUserById(String id) {
        return getObject(searchFilterGetUserById(id), SearchScope.SUB);
    }

    @Override
    public Racker getUserByUsernameForIdentityProviderUri(String username, String uri) {
        return getUserById(Racker.asFederatedRackerId(username, uri));
    }

    @Override
    public Racker getUserByUsernameForIdentityProviderName(String username, String identityProviderName) {
        return getObject(searchFilterGetUserByUsername(username), getBaseDnWithIdpName(identityProviderName), SearchScope.ONE);
    }

    private Filter searchFilterGetUserById(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RACKER_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, getLdapEntityClass()).build();
    }

    private Filter searchFilterGetUserByUsername(String username) {
        return new LdapSearchBuilder()
                .addSubStringAttribute(ATTR_RACKER_ID, username, null, null)
                .addEqualAttribute(ATTR_OBJECT_CLASS,  getLdapEntityClass()).build();
    }

    @Override
    public void updateUser(Racker user) {
        throw new UnsupportedOperationException("Updating federated rackers is not supported");
    }
}
