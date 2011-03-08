package com.rackspace.idm.domain.entity;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class Permission implements Serializable, Auditable {
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

    public Permission(String customerId, String clientId, String permissionId,
                      String value) {
        super();
        this.permissionId = permissionId;
        this.clientId = clientId;
        this.value = value;
        this.customerId = customerId;
    }

    public String getPermissionId() {
        return permissionId;
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
                + LDAP_SEPERATOR + this.permissionId; // + LDAP_SEPERATOR 
                //+ uniqueId + LDAP_SEPERATOR + type + LDAP_SEPERATOR + value;
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


    /**
     * Used by Java serialization. Produces the serialized form of the Token
     * class.
     *
     * @return The proxy instance of the Token class
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * Used by Java serialization. Prevent attempts to deserialize the Token
     * object directly, without using the proxy object.
     *
     * @param stream Used by Java serialization API
     * @throws java.io.InvalidObjectException By the Java serialization API
     */
    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException {
        throw new InvalidObjectException("Serialization proxy is required.");
    }

    /**
     * Serialized form for the Token object, based on the Serialization Proxy
     * pattern in the book Effective Java, 2nd Edition, p. 312
     * <p/>
     * I.e., this is what actually gets serialized.
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 3124210168273666242L;
        private String permissionId;
        private String clientId;
        private String value;
        private String customerId;
        private String type;
        private String uniqueId;

        SerializationProxy(Permission permission) {
            this.permissionId = permission.permissionId;
            this.clientId = permission.clientId;
            this.value = permission.value;
            this.customerId = permission.customerId;
            this.type = permission.type;
            this.uniqueId = permission.uniqueId;
        }

        private Object readResolve() {
            Permission permission = new Permission(customerId, clientId, permissionId, value);
            permission.setType(type);
            permission.setUniqueId(uniqueId);
            return permission;
        }

    }

    @Override
    public String getAuditContext() {
        String format = "permissionId=%s,clientId=%s,customerId=%s";
        return String.format(format, permissionId, clientId, customerId);
    }
}
