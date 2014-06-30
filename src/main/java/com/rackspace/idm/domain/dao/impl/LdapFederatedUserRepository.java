package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class LdapFederatedUserRepository extends LdapGenericRepository<FederatedUser> implements FederatedUserDao {

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_RACKSPACEPERSON;
    }

    @Override
    public void addUser(IdentityProvider provider, FederatedUser user) {
        Assert.isTrue(user.getFederatedIdpUri().equals(provider.getUri()), "The user must have the same federated uri as the provider!");
        user.setId(getNextId());
        addObject(getBaseDnWithIdpName(provider.getName()), user);
    }

    @Override
    public void updateUser(FederatedUser user) {
        updateObject(user);
    }

    @Override
    public FederatedUser getUserByToken(UserScopeAccess token) {
        return getUserById(token.getUserRsId());
    }

    @Override
    public FederatedUser getUserByUsernameForIdentityProviderName(String username, String identityProviderName) {
        return getObject(searchFilterGetUserByUsername(username), getBaseDnWithIdpName(identityProviderName), SearchScope.ONE);
    }

    @Override
    public Iterable<FederatedUser> getUsersByDomainId(String domainId) {
        return getObjects(searchFilterGetUsersByDomainId(domainId));
    }

    @Override
    public FederatedUser getUserById(String id) {
        return getObject(searchFilterGetUserById(id), SearchScope.SUB);
    }

    @Override
    public String getNextId() {
        return getUuid();
    }

    @Override
    public String getBaseDn() {
        return EXTERNAL_PROVIDERS_BASE_DN;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    private String getBaseDnWithIdpName(String idpName) {
        return String.format("ou=users,ou=%s,%s", idpName, EXTERNAL_PROVIDERS_BASE_DN);
    }

    private Filter searchFilterGetUserByUsername(String username) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACE_FEDERATED_PERSON).build();
    }

    private Filter searchFilterGetUserById(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACE_FEDERATED_PERSON).build();
    }

    private Filter searchFilterGetUsersByDomainId(String domainId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_DOMAIN_ID, domainId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACE_FEDERATED_PERSON).build();

    }

}
