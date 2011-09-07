package com.rackspace.idm.domain.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_TENANT_ROLE)
public class TenantRole implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_TENANT_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] tenantIds;

    @LDAPField(attribute = LdapRepository.ATTR_CLIENT_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String clientId;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String[] getTenantIds() {
        return tenantIds;
    }

    public void setTenantIds(String[] tenantIds) {
        this.tenantIds = tenantIds;
    }

    public void addTenantId(String tenantId) {
        List<String> tenants = new ArrayList<String>();
        if (tenantIds != null || tenantIds.length > 0) {
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
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (!Arrays.equals(tenantIds, other.tenantIds)) {
            return false;
        }
        return true;
    }

    @Override
    public String getAuditContext() {
        String format = "role=%s,clientId=%s,tenantIds=%s";
        return String.format(format, getName(), getClientId(), getTenantIds());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}
