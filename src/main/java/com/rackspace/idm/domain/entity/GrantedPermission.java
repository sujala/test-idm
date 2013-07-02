package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_GRANTEDPERMISSION, requestAllAttributes = true)
public class GrantedPermission extends Permission implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;
    @LDAPField(attribute = LdapRepository.ATTR_RESOURCE_GROUP, objectClass = LdapRepository.OBJECTCLASS_GRANTEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] resourceGroups;

    public GrantedPermission() {}

    public GrantedPermission(String customerId, String clientId, String permissionId) {
        super(customerId, clientId, permissionId);
    }

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

    public void setResourceGroups(String[] resourceGroups) {
        this.resourceGroups = (String[]) ArrayUtils.clone(resourceGroups);
    }

    public void copyChanges(GrantedPermission modified) {
        if (modified.getResourceGroups() != null) {
            setResourceGroups(modified.getResourceGroups());
        }
    }
}
