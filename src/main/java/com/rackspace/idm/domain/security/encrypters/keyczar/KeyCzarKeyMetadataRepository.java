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

    private static final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_KEY_METADATA).build();

    @Override
    public String getBaseDn() {
        return config.getKeyCzarDN();
    }

    private String getMetadataDN(String metaName) {
        return String.format("%s=%s,%s", ATTR_COMMON_NAME, metaName, getBaseDn());
    }

    @Override
    public LdapKeyMetadata getKeyMetadataByName(String metaName) {
        return getObject(filter, getMetadataDN(metaName), SearchScope.BASE);
    }

}