package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

@Component
public class LdapIdentityProviderRepository extends LdapGenericRepository<IdentityProvider> implements IdentityProviderDao {

    public static final String NULL_OR_EMPTY_PARAMETER = "Null or Empty parameter";

    @Override
    public String getBaseDn() {
        return EXTERNAL_PROVIDERS_BASE_DN;
    }

    @Override
    public IdentityProvider getIdentityProviderByUri(String uri) {
        return getObject(searchByUriFilter(uri));
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        return getObject(searchByNameFilter(name));
    }

    Filter searchByUriFilter(String uri) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_URI, uri)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    Filter searchByNameFilter(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OU, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_EXTERNALPROVIDER).build();
    }

    @Override
    public void addObject(IdentityProvider object) {
        super.addObject(object);
        addOrganizationalUnit(object.getUniqueId(), LdapRepository.EXTERNAL_PROVIDERS_USER_CONTAINER_NAME);
    }
}
