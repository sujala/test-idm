package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.KeyCzarKeyMetadataDao;
import com.rackspace.idm.domain.entity.LdapKeyMetadata;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;

@LDAPComponent
public class LdapKeyCzarKeyMetadataRepository extends LdapGenericRepository<LdapKeyMetadata> implements KeyCzarKeyMetadataDao {

    @Autowired
    private IdentityConfig config;

    @Override
    public String getBaseDn() {
        return config.getKeyCzarDN();
    }

    @Override
    public LdapKeyMetadata getKeyMetadataByName(String metaName) {
        final Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_KEY_METADATA)
                .addEqualAttribute(ATTR_COMMON_NAME, metaName)
                .build();

        return getObject(filter, getBaseDn(), SearchScope.ONE);
    }

}