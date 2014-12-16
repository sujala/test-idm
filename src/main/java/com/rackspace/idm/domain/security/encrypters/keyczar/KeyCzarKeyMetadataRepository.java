package com.rackspace.idm.domain.security.encrypters.keyczar;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KeyCzarKeyMetadataRepository extends LdapGenericRepository<LdapKeyMetadata> implements KeyCzarKeyMetadataDao {

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