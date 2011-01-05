package com.rackspace.idm.entities;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class Permission implements Serializable {
    private static final long serialVersionUID = -4289257131504718968L;
    public static final String LDAP_SEPERATOR = "::";

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String permissionId;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String clientId;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String value;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String customerId;
    private String type;
    private String uniqueId;

    public Permission() {
    }

    public String getPermissionId() {
        return permissionId;
    }

    public Permission(String customerId, String clientId, String permissionId,
        String value) {
        super();
        this.permissionId = permissionId;
        this.clientId = clientId;
        this.value = value;
        this.customerId = customerId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getPermissionLDAPserialization() {

        if (this.customerId == null || this.clientId == null
            || this.permissionId == null) {
            return null;
        }
        
        return this.customerId + LDAP_SEPERATOR + this.clientId
        + LDAP_SEPERATOR + this.permissionId;
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
            + ((permissionId == null) ? 0 : permissionId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        if (permissionId == null) {
            if (other.permissionId != null) {
                return false;
            }
        } else if (!permissionId.equals(other.permissionId)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
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

    @Override
    public String toString() {
        return "Permission [clientId=" + clientId + ", customerId="
            + customerId + ", permissionId=" + permissionId + ", type=" + type
            + ", uniqueId=" + uniqueId + ", value=" + value + "]";
    }
}
