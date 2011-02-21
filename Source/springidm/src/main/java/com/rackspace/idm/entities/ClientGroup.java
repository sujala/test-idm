package com.rackspace.idm.entities;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class ClientGroup implements Serializable, Auditable{
    private static final long serialVersionUID = -5845388972735116102L;
    
    private String uniqueId = null;
    private String name = null;
    private String clientId = null;
    private String customerId = null;
    
    public ClientGroup() {
    }
    
    public ClientGroup(String clientId, String customerId, String name) {
        this.clientId = clientId;
        this.customerId = customerId;
        this.name = name;
    }
    
    public String getUniqueId() {
        return uniqueId;
    }
    
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
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
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result
            + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
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
        ClientGroup other = (ClientGroup) obj;
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
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "ClientGroup [uniqueId=" + uniqueId + ", name=" + name
            + ", clientId=" + clientId + ", customerId=" + customerId + "]";
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
        private String name = null;
        private String clientId = null;
        private String customerId = null;

        SerializationProxy(ClientGroup clientGroup) {
            this.clientId = clientGroup.clientId;
            this.customerId = clientGroup.customerId;
            this.name = clientGroup.name;
        }

        private Object readResolve() {
            return new ClientGroup(clientId, customerId, name);
        }

    }

    @Override
    public String getAuditContext() {
        String format = "groupName=%s,clientId=$s,customerId=%s";
        return String.format(format, name, clientId, customerId);
    }
}
