package com.rackspace.idm.domain.entity;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.joda.time.DateTime;


public final class AccessToken extends Token implements Serializable {
    private static final long serialVersionUID = -3054005479579503671L;

    public static enum IDM_SCOPE {
        FULL, SET_PASSWORD
    }

    private String uniqueId;
    private IDM_SCOPE idmScope = IDM_SCOPE.FULL;
    private boolean isTrusted = false;

    private BaseUser user;
    private BaseClient client;

    public AccessToken() {
        super(null, null);
    }

    public AccessToken(String tokenString, DateTime tokenExpiration,
        BaseUser user, BaseClient client, IDM_SCOPE idmScope) {
        super(tokenString, tokenExpiration);
        if (user instanceof User) {
            this.user = ((User) user).getBaseUser();
        } else {
            this.user = user;
        }
        if (client instanceof Client) {
            this.client = ((Client) client).getBaseClient();
        } else {
            this.client = client;
        }
        this.idmScope = idmScope;
    }

    public AccessToken(String tokenString, DateTime tokenExpiration,
        BaseUser user, BaseClient client, IDM_SCOPE idmScope, boolean isTrusted) {

        this(tokenString, tokenExpiration, user, client, idmScope);
        this.isTrusted = isTrusted;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public BaseUser getTokenUser() {
        return user;
    }

    public void setTokenUser(BaseUser user) {
        if (user instanceof User) {
            this.user = ((User) user).getBaseUser();
        } else {
            this.user = user;
        }
    }

    public BaseClient getTokenClient() {
        return client;
    }

    public void setTokenClient(BaseClient client) {
        if (client instanceof Client) {
            this.client = ((Client) client).getBaseClient();
        } else {
            this.client = client;
        }
    }

    public String getRequestor() {
        if (client == null) {
            return null;
        }
        return client.getClientId();
    }

    public String getOwner() {
        if (user != null) {
            return user.getUsername();
        }
        return client.getClientId();
    }

    public boolean isRestrictedToSetPassword() {
        return IDM_SCOPE.SET_PASSWORD == idmScope;
    }

    public void setRestrictedToSetPassword() {
        idmScope = IDM_SCOPE.SET_PASSWORD;
    }

    public boolean isTrusted() {
        return this.isTrusted;
    }

    public boolean isClientToken() {
        return user == null && client != null;
    }

    public boolean hasClientPermissions() {
        return isClientToken() && client.permissions != null
            && client.permissions.size() > 0;
    }

    public boolean hasUserGroups() {
        return !isClientToken() && user != null && user.groups != null
            && user.groups.size() > 0;
    }

    public String getAuditString() {
        String auditString = null;
        if (isClientToken()) {
            String format = "Client:%s";
            auditString = String.format(format, client.getClientId());
        } else {
            String rackerFormat = "Racker:%s-Client:%s";
            String userFormat = "User:%s-Client:%s";
            auditString = isTrusted ? String.format(rackerFormat,
                user.getUsername(), client.getClientId()) : String.format(
                userFormat, user.getUsername(), client.getClientId());
        }
        return auditString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((client == null) ? 0 : client.hashCode());
        result = prime * result
            + ((idmScope == null) ? 0 : idmScope.hashCode());
        result = prime * result + (isTrusted ? 1231 : 1237);
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        AccessToken other = (AccessToken) obj;
        if (client == null) {
            if (other.client != null) {
                return false;
            }
        } else if (!client.equals(other.client)) {
            return false;
        }
        if (idmScope != other.idmScope) {
            return false;
        }
        if (isTrusted != other.isTrusted) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AccessToken [uniqueId=" + uniqueId + ", idmScope=" + idmScope
            + ", isTrusted=" + isTrusted + ", user=" + user + ", client="
            + client + "]";
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
     */
    private void readObject(ObjectInputStream stream)
        throws InvalidObjectException {
        throw new InvalidObjectException("Serializtion proxy is required.");
    }

    /**
     * Serialized form for the Token object, based on the Serialization Proxy
     * pattern in the book Effective Java, 2nd Edition, p. 312
     * 
     * I.e., this is what actually gets serialized.
     * 
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 1797445240684729565L;

        private String tokenString;
        private DateTime expirationTime;
        private String uniqueId;
        private BaseUser user;
        private BaseClient client;
        private IDM_SCOPE idmScope;
        private boolean isTrusted;

        SerializationProxy(AccessToken token) {
            this.tokenString = token.getTokenString();
            this.expirationTime = token.getExpirationTime();
            this.uniqueId = token.uniqueId;
            this.user = token.user;
            this.client = token.client;
            this.idmScope = token.idmScope;
            this.isTrusted = token.isTrusted;
        }

        private Object readResolve() {
            AccessToken token = new AccessToken(tokenString, expirationTime,
                user, client, idmScope, isTrusted);
            token.setUniqueId(uniqueId);
            return token;
        }

    }
}
