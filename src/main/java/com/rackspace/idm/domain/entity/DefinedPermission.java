package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import org.apache.commons.lang.StringUtils;

@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_DEFINEDPERMISSION,requestAllAttributes=true)
public class DefinedPermission extends Permission implements Auditable {
    
    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;
    @LDAPField(attribute = LdapRepository.ATTR_BLOB, objectClass = LdapRepository.OBJECTCLASS_DEFINEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String value;
    @LDAPField(attribute = LdapRepository.ATTR_PERMISSION_TYPE, objectClass = LdapRepository.OBJECTCLASS_DEFINEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String permissionType;
    @LDAPField(attribute = LdapRepository.ATTR_TITLE, objectClass = LdapRepository.OBJECTCLASS_DEFINEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String title;
    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_DEFINEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;
    @LDAPField(attribute = LdapRepository.ATTR_GRANTED_BY_DEFAULT, objectClass = LdapRepository.OBJECTCLASS_DEFINEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean grantedByDefault;
    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_DEFINEDPERMISSION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Boolean enabled;
    
    public DefinedPermission() {}
    
    public DefinedPermission(String customerId, String clientId, String permissionId) {
        super(customerId, clientId, permissionId);
    }

    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_NAME, inRDN=true, filterUsage = FilterUsage.ALWAYS_ALLOWED)
    public String getPermissionId() {
        return super.getPermissionId();
    }
    
    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = StringUtils.isBlank(value) ? null : value;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = StringUtils.isBlank(permissionType) ? null : permissionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = StringUtils.isBlank(title) ? null : title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtils.isBlank(description) ? null : description;
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

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
        result = prime * result
            + ((grantedByDefault == null) ? 0 : grantedByDefault.hashCode());
        result = prime * result
            + ((ldapEntry == null) ? 0 : ldapEntry.hashCode());
        result = prime * result
            + ((permissionType == null) ? 0 : permissionType.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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

        DefinedPermission other = (DefinedPermission) obj;
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (enabled == null) {
            if (other.enabled != null) {
                return false;
            }
        } else if (!enabled.equals(other.enabled)) {
            return false;
        }
        if (grantedByDefault == null) {
            if (other.grantedByDefault != null) {
                return false;
            }
        } else if (!grantedByDefault.equals(other.grantedByDefault)) {
            return false;
        }
        if (ldapEntry == null) {
            if (other.ldapEntry != null) {
                return false;
            }
        } else if (!ldapEntry.equals(other.ldapEntry)) {
            return false;
        }
        if (permissionType == null) {
            if (other.permissionType != null) {
                return false;
            }
        } else if (!permissionType.equals(other.permissionType)) {
            return false;
        }
        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
    
    public void copyChanges(DefinedPermission modified) {

        if (modified.getPermissionType() != null) {
            setPermissionType(modified.getPermissionType());
        }
        if (modified.getValue() != null) {
            setValue(modified.getValue());
        }
        if (modified.getDescription() != null) {
            setDescription(modified.getDescription());
        }
        if (modified.getTitle() != null) {
            String t = modified.getTitle();
            setTitle(t);
        }
        if (modified.getEnabled() != null) {
            setEnabled(modified.getEnabled());
        }
        if (modified.getGrantedByDefault() != null) {
            setGrantedByDefault(modified.getGrantedByDefault());
        }
    }
}
