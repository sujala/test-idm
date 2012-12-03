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
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_PATTERN)
public class Pattern implements UniqueId {
    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;
    @LDAPField(attribute = LdapRepository.ATTR_REGEX, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String regex;
    @LDAPField(attribute = LdapRepository.ATTR_ERRMSG, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String errMsg;
    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;


    @Override
    public String getUniqueId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
