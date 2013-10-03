package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.RackerDao;
import com.rackspace.idm.domain.entity.Racker;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

@Component
public class LdapRackerRepository extends LdapGenericRepository<Racker> implements RackerDao {

    @Override
    public String getBaseDn() {
        return RACKERS_BASE_DN;
    }

    public String getLdapEntityClass() {
        return OBJECTCLASS_RACKER;
    }

    @Override
    public void addRacker(Racker racker) {
        addObject(racker);
    }

    @Override
    public void deleteRacker(String rackerId) {
        deleteObject(searchFilterGetRackerByRackerId(rackerId));
    }

    @Override
    public Racker getRackerByRackerId(String rackerId) {
        return getObject(searchFilterGetRackerByRackerId(rackerId));
    }

    private Filter searchFilterGetRackerByRackerId(String rackerId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RACKER_ID, rackerId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKER).build();
    }
}
