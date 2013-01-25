package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/7/12
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
@EqualsAndHashCode(exclude = "description")
@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_BASEURL)
public class ServiceApi implements UniqueId {
    @LDAPField(attribute = LdapRepository.ATTR_VERSION_ID, objectClass = LdapRepository.OBJECTCLASS_BASEURL, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String version;

    @LDAPField(attribute = LdapRepository.ATTR_OPENSTACK_TYPE, objectClass = LdapRepository.OBJECTCLASS_BASEURL, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String type;

    private String description;

    @Override
    public String getUniqueId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
