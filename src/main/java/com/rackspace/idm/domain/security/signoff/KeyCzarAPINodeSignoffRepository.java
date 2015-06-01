package com.rackspace.idm.domain.security.signoff;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KeyCzarAPINodeSignoffRepository extends LdapGenericRepository<LdapAPINodeSignoff> implements KeyCzarAPINodeSignoffDao {

    @Autowired
    private IdentityConfig config;

    public static final String KEY_DISTRIBUTION_OU = "keydistribution";

    @Override
    public String getBaseDn() {
        return String.format("ou=%s,%s", KEY_DISTRIBUTION_OU, config.getStaticConfig().getKeyCzarDN());
    }

    @Override
    public String getSortAttribute() {
        return ATTR_NAME;
    }

    @Override
    public String[] getSearchAttributes() {
        return ATTR_KEY_SEARCH_ATTRIBUTES;
    }

    @Override
    public LdapAPINodeSignoff getByNodeAndMetaName(String metaName, String nodeName) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_API_NODE_SIGNOFF)
                .addEqualAttribute(ATTR_KEY_METADATA_ID, metaName)
                .addEqualAttribute(ATTR_NAME, nodeName)
                .build();

        final LdapAPINodeSignoff obj = getObject(filter, getBaseDn(), SearchScope.ONE);

        return obj;
    }

    @Override
    public void addOrUpdateObject(LdapAPINodeSignoff ldapAPINodeSignoff) {
        if (ldapAPINodeSignoff.getId() == null) {
            ldapAPINodeSignoff.setId(getNextId());
            addObject(ldapAPINodeSignoff);
        } else {
            updateObject(ldapAPINodeSignoff);
        }
    }

    @Override
    public String getNextId() {
        return super.getUuid();
    }
}