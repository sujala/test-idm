package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_DELEGATEDPERMISSION, requestAllAttributes = true)
public class DelegatedPermission extends Permission implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_RESOURCE_GROUP, objectClass = LdapRepository.OBJECTCLASS_DELEGATEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] resourceGroups;

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

    public void copyChanges(DelegatedPermission modified) {
        if (modified.getResourceGroups() != null) {
            setResourceGroups(modified.getResourceGroups());
        }
    }
}
