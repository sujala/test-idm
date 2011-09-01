package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_TENANT)
public class Tenant implements Auditable{

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;
    
    @LDAPField(attribute=LdapRepository.ATTR_TENANT_ID, objectClass=LdapRepository.OBJECTCLASS_TENANT, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String tenantId;
    
    @LDAPField(attribute=LdapRepository.ATTR_ENABLED, objectClass=LdapRepository.OBJECTCLASS_TENANT, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private boolean enabled;
    
    @LDAPField(attribute=LdapRepository.ATTR_DESCRIPTION, objectClass=LdapRepository.OBJECTCLASS_TENANT, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String description;
    
    @LDAPField(attribute=LdapRepository.ATTR_NAME, objectClass=LdapRepository.OBJECTCLASS_TENANT, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String name;
    
    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }
    
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }
    
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAuditContext() {
        // TODO Auto-generated method stub
        return null;
    }

}
