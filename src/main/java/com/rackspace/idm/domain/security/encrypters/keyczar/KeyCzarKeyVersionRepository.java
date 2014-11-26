package com.rackspace.idm.domain.security.encrypters.keyczar;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyCzarKeyVersionRepository extends LdapGenericRepository<LdapKeyVersion> implements KeyCzarKeyVersionDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyCzarKeyMetadataRepository.class);

    @Autowired
    private IdentityConfig config;

    private static final Filter filter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_KEY_DESCRIPTOR).build();

    @Override
    public String getBaseDn() {
        return config.getKeyCzarDN();
    }

    @Override
    public String getSortAttribute() {
        return ATTR_KEY_VERSION;
    }

    private String getKeyVersionsBaseDN(String metadataName) {
        return String.format("%s=%s,%s", ATTR_COMMON_NAME, metadataName, getBaseDn());
    }

    private String getKeyVersionDN(String metadataName, String version) {
        return String.format("%s=%s,%s", ATTR_KEY_VERSION, version, getKeyVersionsBaseDN(metadataName));
    }

    @Override
    public LdapKeyVersion getKeyVersionForMetadata(String metaName, String version) {
        return getObject(filter, getKeyVersionDN(metaName, version), SearchScope.BASE);
    }

    @Override
    public List<KeyVersion> getKeyVersionsForMetadata(String metaName) {
        final ArrayList<KeyVersion> result = new ArrayList<KeyVersion>();
        final Iterable<LdapKeyVersion> versions = getObjects(filter, getKeyVersionsBaseDN(metaName), SearchScope.ONE);
        for (LdapKeyVersion version : versions) {
            result.add(version);
        }
        return result;
    }

}