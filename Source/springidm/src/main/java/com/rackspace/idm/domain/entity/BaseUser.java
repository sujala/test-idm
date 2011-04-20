package com.rackspace.idm.domain.entity;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class BaseUser implements Serializable {
    private static final long serialVersionUID = 5720550447838228533L;
    
    private String uniqueId = null;

    @NotNull
    @Length(min = 1, max = 32)
    @Pattern(regexp = RegexPatterns.USERNAME, message = MessageTexts.USERNAME)
    private String username = null;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String customerId = null;
    private List<ClientGroup> groups;
  
    public BaseUser() {
    }

    public BaseUser(String username) {
        this.username = username;
    }

    public BaseUser(String username, String customerId, List<ClientGroup> groups) {
        this.username = username;
        this.customerId = customerId;
        this.groups = groups;
    }
    
    public BaseUser(String uniqueId, String username, String customerId, List<ClientGroup> groups) {
        this.uniqueId = uniqueId;
        this.username = username;
        this.customerId = customerId;
        this.groups = groups;
    }

    public BaseUser(String username, String customerId) {
        this.username = username;
        this.customerId = customerId;
    }
    
    public String getUniqueId() {
        return uniqueId;
    }

    protected void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            this.uniqueId = uniqueId;
        }
    }
    
    public String getUsername() {
        return username;
    }

    protected void setUsername(String username) {
        this.username = username;
    }

    public String getCustomerId() {
        return customerId;
    }

    protected void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<ClientGroup> getGroups() {
        return groups != null ? Collections.unmodifiableList(groups) : null;
    }

    protected void setGroups(List<ClientGroup> groups) {
        this.groups = groups;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((customerId == null) ? 0 : customerId.hashCode());
        result = prime * result + ((groups == null) ? 0 : groups.hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result
            + ((username == null) ? 0 : username.hashCode());
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
        BaseUser other = (BaseUser) obj;
        if (customerId == null) {
            if (other.customerId != null) {
                return false;
            }
        } else if (!customerId.equals(other.customerId)) {
            return false;
        }
        if (groups == null) {
            if (other.groups != null) {
                return false;
            }
        } else if (!groups.equals(other.groups)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BaseUser [uniqueId=" + uniqueId + ", username=" + username
            + ", customerId=" + customerId + ", groups=" + groups + "]";
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
        private static final long serialVersionUID = -42555229195315854L;
        private final String uniqueId;
        private final String username;
        private final String customerId;
        private final List<ClientGroup> groups;

        SerializationProxy(BaseUser baseUser) {
            this.uniqueId = baseUser.uniqueId;
            this.username = baseUser.username;
            this.customerId = baseUser.customerId;
            this.groups = baseUser.groups;
        }

        private Object readResolve() {
            return new BaseUser(uniqueId, username, customerId, groups);
        }

    }
}
