package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_TENANT_ROLE)
public class TenantRole implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_ROLE_RS_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String roleRsId;

    @LDAPField(attribute = LdapRepository.ATTR_TENANT_RS_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] tenantIds;

    @LDAPField(attribute = LdapRepository.ATTR_CLIENT_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String clientId;

    @LDAPField(attribute = LdapRepository.ATTR_USER_RS_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String userId;

    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }

    private String name;
    private String description;

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public String getRoleRsId() {
        return roleRsId;
    }

    public void setRoleRsId(String roleRsId) {
        this.roleRsId = roleRsId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        this.tenantIds = (String[]) ArrayUtils.clone(tenantIds);
    }

    public void addTenantId(String tenantId) {
        List<String> tenants = new ArrayList<String>();
        if (tenantIds != null) {
            Collections.addAll(tenants, tenantIds);
        }
        if (!tenants.contains(tenantId)) {
            tenants.add(tenantId);
        }
        tenantIds = tenants.toArray(new String[tenants.size()]);
    }

    public void removeTenantId(String tenantId) {
        if (tenantIds == null || tenantIds.length == 0) {
            return;
        }
        List<String> tenants = new ArrayList<String>();
        Collections.addAll(tenants, tenantIds);
        if (tenants.contains(tenantId)) {
            tenants.remove(tenantId);
        }
        tenantIds = tenants.toArray(new String[tenants.size()]);
    }

    public boolean containsTenantId(String tenantId) {
        if (tenantIds == null || tenantIds.length == 0) {
            return false;
        }
        List<String> tenants = new ArrayList<String>();
        Collections.addAll(tenants, tenantIds);
        return tenants.contains(tenantId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + ((roleRsId == null) ? 0 : roleRsId.hashCode());
        result = prime * result + Arrays.hashCode(tenantIds);
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
        TenantRole other = (TenantRole) obj;
        if (clientId == null) {
            if (other.clientId != null) {
                return false;
            }
        } else if (!clientId.equals(other.clientId)) {
            return false;
        }
        if (roleRsId == null) {
            if (other.roleRsId != null) {
                return false;
            }
        } else if (!roleRsId.equals(other.roleRsId)) {
            return false;
        }
        if (!Arrays.equals(tenantIds, other.tenantIds)) {
            return false;
        }
        return true;
    }

    @Override
    public String getAuditContext() {
        String format = "roleRsId=%s,clientId=%s,tenantRsIds=%s";
        return String.format(format, getRoleRsId(), getClientId(), getTenantIds());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}
