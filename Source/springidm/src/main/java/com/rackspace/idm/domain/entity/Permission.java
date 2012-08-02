package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_PERMISSION)
public class Permission implements Auditable {
    
    @LDAPEntryField()
    private ReadOnlyEntry     ldapEntry;
    
    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_PERMISSION, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String            permissionId;

    @LDAPField(attribute = LdapRepository.ATTR_CLIENT_ID, objectClass = LdapRepository.OBJECTCLASS_PERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String            clientId;
    
    @LDAPField(attribute = LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, objectClass = LdapRepository.OBJECTCLASS_PERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String            customerId;
    
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }
    
    @Override
    public String getAuditContext() {
        final String format = "Permission(clientId=%s,permissionId=%s)";
        return String.format(format, getClientId(), getPermissionId());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result
            + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result
            + ((ldapEntry == null) ? 0 : ldapEntry.hashCode());
        result = prime * result
            + ((permissionId == null) ? 0 : permissionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Permission other = (Permission) obj;
        if (clientId == null) {
            if (other.clientId != null) {
                return false;
            }
        } else if (!clientId.equals(other.clientId)) {
            return false;
        }
        if (customerId == null) {
            if (other.customerId != null) {
                return false;
            }
        } else if (!customerId.equals(other.customerId)) {
            return false;
        }
        if (ldapEntry == null) {
            if (other.ldapEntry != null) {
                return false;
            }
        } else if (!ldapEntry.equals(other.ldapEntry)) {
            return false;
        }
        if (permissionId == null) {
            if (other.permissionId != null) {
                return false;
            }
        } else if (!permissionId.equals(other.permissionId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Permission [ldapEntry=" + ldapEntry + ", permissionId="
            + permissionId + ", clientId=" + clientId + ", customerId="
            + customerId + "]";
    }

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }
}
