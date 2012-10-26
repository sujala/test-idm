package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/22/12
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_REGION)
public class Region implements Auditable {
    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_CLOUD, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String cloud;

    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean isEnabled;

    @LDAPField(attribute = LdapRepository.ATTR_USE_FOR_DEFAULT_REGION, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean isDefault;

    @Override
    public String getAuditContext() {
        return String.format("regionId=%s", name);
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }
}
