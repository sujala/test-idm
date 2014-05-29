package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.FederatedTokenDao;
import com.rackspace.idm.domain.entity.FederatedToken;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

@Component
public class LdapFederatedTokenRepository extends LdapGenericRepository<FederatedToken> implements FederatedTokenDao {

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_FEDERATEDUSERSCOPEACCESS;
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

    @Override
    public Iterable<FederatedToken> getFederatedTokensByUserId(String userId) {
        return getObjects(searchFilterGetTokensByUserId(userId));
    }

    private Filter searchFilterGetTokensByUserId(String userId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_USER_RS_ID, userId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_FEDERATEDUSERSCOPEACCESS).build();
    }

}
