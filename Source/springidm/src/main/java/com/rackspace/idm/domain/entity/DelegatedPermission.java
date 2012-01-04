package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;

import java.util.Arrays;

@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_DELEGATEDPERMISSION, requestAllAttributes = true)
public class DelegatedPermission extends Permission implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;
    @LDAPField(attribute = LdapRepository.ATTR_RESOURCE_GROUP, objectClass = LdapRepository.OBJECTCLASS_DELEGATEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] resourceGroups;

    public DelegatedPermission() {}

    @Override
    @LDAPGetter(attribute = LdapRepository.ATTR_NAME, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED)
    public String getPermissionId() {
        return super.getPermissionId();
    }

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public String[] getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(String[] resourceGroups) {
       if (resourceGroups == null) {
            this.resourceGroups = null;
        } else {
            String[] copy = new String[resourceGroups.length];
            for(int i = 0; i < resourceGroups.length; i++){
                copy[i] = resourceGroups[i];
            }
           this.resourceGroups = copy;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((ldapEntry == null) ? 0 : ldapEntry.hashCode());
        result = prime * result + Arrays.hashCode(resourceGroups);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DelegatedPermission other = (DelegatedPermission) obj;
        if (ldapEntry == null) {
            if (other.ldapEntry != null) {
                return false;
            }
        } else if (!ldapEntry.equals(other.ldapEntry)) {
            return false;
        }
        if (!Arrays.equals(resourceGroups, other.resourceGroups)) {
            return false;
        }
        return true;
    }

    public void copyChanges(DelegatedPermission modified) {
        if (modified.getResourceGroups() != null) {
            setResourceGroups(modified.getResourceGroups());
        }
    }
}
