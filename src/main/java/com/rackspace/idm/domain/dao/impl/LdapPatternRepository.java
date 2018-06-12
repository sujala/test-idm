package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.PatternDao;
import com.rackspace.idm.domain.entity.Pattern;
import com.unboundid.ldap.sdk.Filter;

@LDAPComponent
public class LdapPatternRepository extends LdapGenericRepository<Pattern> implements PatternDao {

    public String getBaseDn(){
        return PATTERN_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_PATTERN;
    }

    public String getNextId() {
        return getUuid();
    }

    @Override
    public Pattern getPattern(String name) {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_PATTERN)
                .addEqualAttribute(LdapRepository.ATTR_NAME, name).build();
        return getObject(searchFilter);
    }
}
