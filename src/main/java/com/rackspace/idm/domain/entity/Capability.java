package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge.munoz
 * Date: 9/6/12
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
@EqualsAndHashCode(exclude = "description")
@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_CAPABILITY)
public class Capability implements Auditable, UniqueId {
    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String rsId;

    @LDAPField(attribute = LdapRepository.ATTR_CAPABILITY_ID, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_ACTION, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String action;

    @LDAPField(attribute = LdapRepository.ATTR_URL, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String url;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_RESOURCES, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private List<String> resources;

    @LDAPField(attribute = LdapRepository.ATTR_OPENSTACK_TYPE, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String type;

    @LDAPField(attribute = LdapRepository.ATTR_VERSION_ID, objectClass = LdapRepository.OBJECTCLASS_CAPABILITY, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String version;

    public String getAuditContext() {
        String format = "capability=%s";
        return String.format(format, id);
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public void setUniqueId(String id){
        this.ldapEntry.setDN(id);
    }
}
