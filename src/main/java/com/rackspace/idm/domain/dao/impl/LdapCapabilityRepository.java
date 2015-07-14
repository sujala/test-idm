package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.CapabilityDao;
import com.rackspace.idm.domain.entity.Capability;
import com.unboundid.ldap.sdk.Filter;



/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
@LDAPComponent
public class LdapCapabilityRepository  extends LdapGenericRepository<Capability> implements CapabilityDao {
    public String getBaseDn(){
        return CAPABILITY_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_CAPABILITY;
    }

    public String getNextCapabilityId() {
        return getNextId(NEXT_CAPABILITY_ID);
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void addCapability(Capability capability) {
        addObject(capability);
    }

    @Override
    public Capability getCapability(String id, String type, String version) {
        return getObject(createCapabilityFilter(id, type, version));
    }

    @Override
    public Iterable<Capability> getCapabilities(String type, String version) {
        return getObjects(createCapabilitiesFilter(type,version));
    }

    @Override
    public void deleteCapability(String id, String type, String version) {
        deleteObject(createCapabilityFilter(id, type, version));
    }

    private Filter createCapabilityFilter(String capabilityId, String type, String version) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CAPABILITY_ID, capabilityId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY)
                .addEqualAttribute(ATTR_VERSION_ID, version)
                .addEqualAttribute(ATTR_OPENSTACK_TYPE, type).build();
    }

    private Filter createCapabilitiesFilter(String type, String version) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY)
                .addEqualAttribute(ATTR_VERSION_ID, version)
                .addEqualAttribute(ATTR_OPENSTACK_TYPE, type).build();
    }
}
