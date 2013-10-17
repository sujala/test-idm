package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.StringUtils;
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
        if (StringUtils.isBlank(uri)) {
            getLogger().error(NULL_OR_EMPTY_PARAMETER);
            return null;
        }
        return getObject(searchByUriFilter(uri));
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        if (StringUtils.isBlank(name)) {
            getLogger().error(NULL_OR_EMPTY_PARAMETER);
            return null;
        }
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
}
