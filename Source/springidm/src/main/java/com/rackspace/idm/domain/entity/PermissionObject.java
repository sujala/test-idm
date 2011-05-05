package com.rackspace.idm.domain.entity;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass = "clientPermission")
public class PermissionObject implements Auditable {
    private static final long serialVersionUID = -6160709891135914013L;

    @LDAPEntryField()
    private ReadOnlyEntry     ldapEntry;

    @LDAPField(attribute = "cn", objectClass = "clientPermission", inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String            permissionId;
    @LDAPField(attribute = "clientId", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String            clientId;
    @LDAPField(attribute = "RCN", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String            customerId;

    @LDAPField(attribute = "blob", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String            value;

    @LDAPField(attribute = "permissionType", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String            permissionType;

    @LDAPField(attribute = "title", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String            title;

    @LDAPField(attribute = "description", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String            description;

    @LDAPField(attribute = "grantedByDefault", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean           grantedByDefault;

    @LDAPField(attribute = "enabled", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean           enabled;

    @LDAPField(attribute = "resourceGroup", objectClass = "clientPermission", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String            resourceGroup;

    public PermissionObject() {
    }

    public PermissionObject(String customerId, String clientId, String permissionId, String value) {
        super();
        this.permissionId = permissionId;
        this.clientId = clientId;
        this.value = value;
        this.customerId = customerId;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    public ReadOnlyEntry getLdapEntry() {
        return ldapEntry;
    }

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getGrantedByDefault() {
        return grantedByDefault;
    }

    public void setGrantedByDefault(Boolean grantedByDefault) {
        this.grantedByDefault = grantedByDefault;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    @Override
    public String getAuditContext() {
        final String format = "Permission(clientId=%s,permissionId=%s)";
        return String.format(format, getClientId(), getPermissionId());
    }
    
    public void copyChanges(PermissionObject modified) {

        if (!StringUtils.isBlank(modified.getPermissionType())) {
            setPermissionType(modified.getPermissionType());
        }
        if (!StringUtils.isBlank(modified.getValue())) {
            setValue(modified.getValue());
        }
        if (!StringUtils.isBlank(modified.getDescription())) {
            setDescription(modified.getDescription());
        }
        if (!StringUtils.isBlank(modified.getTitle())) {
            setTitle(modified.getTitle());
        }
        if (modified.getEnabled() != null) {
            setEnabled(modified.getEnabled());
        }
        if (modified.getGrantedByDefault()) {
            setGrantedByDefault(modified.getGrantedByDefault());
        }
    }
}
