package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.EqualsAndHashCode;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
@EqualsAndHashCode
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_DOMAIN)
public class Domain implements Auditable, UniqueId {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String domainId;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean enabled;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_TENANT_RS_ID, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] tenantIds;

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getTenantIds() {
        return tenantIds;
    }

    public void setTenantIds(String[] tenantIds) {
        if(tenantIds != null){
            this.tenantIds = tenantIds.clone();
        }else{
            this.tenantIds = null;
        }
    }

    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public String getAuditContext() {
        String format = "domainId=%s";
        return String.format(format, getDomainId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }
}
