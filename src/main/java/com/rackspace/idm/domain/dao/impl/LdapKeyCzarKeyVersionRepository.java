package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.KeyCzarKeyVersionDao;
import com.rackspace.idm.domain.entity.KeyVersion;
import com.rackspace.idm.domain.entity.LdapKeyVersion;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@LDAPComponent
public class LdapKeyCzarKeyVersionRepository extends LdapGenericRepository<LdapKeyVersion> implements KeyCzarKeyVersionDao {

    @Autowired
    private IdentityConfig config;

    @Override
    public String getBaseDn() {
        return config.getStaticConfig().getKeyCzarDN();
    }

    @Override
    public String getSortAttribute() {
        return ATTR_KEY_VERSION;
    }

    @Override
    public String[] getSearchAttributes() {
        return ATTR_KEY_SEARCH_ATTRIBUTES;
    }

    private String getKeyVersionsBaseDN(String metadataName) {
        return String.format("%s=%s,%s", ATTR_COMMON_NAME, metadataName, getBaseDn());
    }

    @Override
    public List<KeyVersion> getKeyVersionsForMetadata(String metaName) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_KEY_DESCRIPTOR)
                .build();

        final ArrayList<KeyVersion> result = new ArrayList<KeyVersion>();
        final Iterable<LdapKeyVersion> versions = getObjects(filter, getKeyVersionsBaseDN(metaName), SearchScope.ONE);
        for (LdapKeyVersion version : versions) {
            result.add(version);
        }

        return result;
    }

}