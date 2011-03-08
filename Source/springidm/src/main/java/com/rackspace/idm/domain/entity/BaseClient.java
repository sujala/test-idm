package com.rackspace.idm.domain.entity;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class BaseClient implements Serializable {
    private static final long serialVersionUID = -1260927822525896505L;
    
    protected String clientId = null;
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    protected String customerId = null;
    protected List<Permission> permissions;

    public BaseClient() {
    }

    public BaseClient(String clientId, String customerId) {
        this.clientId = clientId;
        this.customerId = customerId;
    }

    public BaseClient(String clientId, String customerId, List<Permission> permissions) {
        this.clientId = clientId;
        this.customerId = customerId;
        this.permissions = permissions;
    }

    public String getClientId() {
        return clientId;
    }
    
    protected void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    protected void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public List<Permission> getPermissions() {
        return permissions != null ? Collections.unmodifiableList(permissions) : null;
    }
    
    protected void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
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
            + ((permissions == null) ? 0 : permissions.hashCode());
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
        BaseClient other = (BaseClient) obj;
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
        if (permissions == null) {
            if (other.permissions != null) {
                return false;
            }
        } else if (!permissions.equals(other.permissions)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TokenClient [clientId=" + clientId + ", customerId="
            + customerId + ", permissions=" + permissions + "]";
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
     *
     * I.e., this is what actually gets serialized.
     *
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = -5052183110369433550L;
        private String clientId;
        private String customerId;
        private List<Permission> permissions;

        SerializationProxy(BaseClient baseClient) {
            this.clientId = baseClient.clientId;
            this.customerId = baseClient.customerId;
            this.permissions = baseClient.permissions;
        }

        private Object readResolve() {
            return new BaseClient(clientId, customerId, permissions);
        }

    }
}
