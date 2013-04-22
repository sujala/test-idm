package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/30/12
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_PROPERTY)
public class Property implements UniqueId {
    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_PROPERTY, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;
    @LDAPField(attribute = LdapRepository.ATTR_VALUE, objectClass = LdapRepository.OBJECTCLASS_PROPERTY, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String value;

    @Override
    public String getUniqueId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
