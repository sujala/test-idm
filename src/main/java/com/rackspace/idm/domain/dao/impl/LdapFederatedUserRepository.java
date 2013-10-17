package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.FederatedToken;
import com.rackspace.idm.domain.entity.User;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.stereotype.Component;

@Component
public class LdapFederatedUserRepository extends LdapGenericRepository<User> implements FederatedUserDao {

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_RACKSPACEPERSON;
    }

    @Override
    public void addUser(User user, String idpName) {
        addObject(getBaseDn(idpName), user);
    }

    @Override
    public User getUserByToken(FederatedToken token) {
        return getUserByUsername(token.getUsername(), token.getIdpName());
    }

    @Override
    public User getUserByUsername(String username, String idpName) {
        return getObject(searchFilterGetUserByUsername(username), getBaseDn(idpName), SearchScope.ONE);
    }

    private String getBaseDn(String idpName) {
        return "ou=users,ou=" + idpName + "," + EXTERNAL_PROVIDERS_BASE_DN;
    }

    private Filter searchFilterGetUserByUsername(String username) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_UID, username)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON).build();
    }
}
