package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_PERMISSION)
public class Permission implements Auditable {
    
    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_PERMISSION, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String permissionId;

    @LDAPField(attribute = LdapRepository.ATTR_CLIENT_ID, objectClass = LdapRepository.OBJECTCLASS_PERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String clientId;
    
    @LDAPField(attribute = LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, objectClass = LdapRepository.OBJECTCLASS_PERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String customerId;
    
    public Permission() {}
    
    public Permission(String customerId, String clientId, String permissionId) {
        this.customerId = customerId;
        this.clientId = clientId;
        this.permissionId = permissionId;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public String getAuditContext() {
        final String format = "Permission(clientId=%s,permissionId=%s)";
        return String.format(format, getClientId(), getPermissionId());
    }
}
