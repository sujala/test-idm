package com.rackspace.idm.entities;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.joda.time.DateTime;

import com.rackspace.idm.oauthAuthentication.Token;

public final class AccessToken extends Token implements Serializable {
    private static final long serialVersionUID = -3054005479579503671L;

    public static enum IDM_SCOPE {
        FULL, SET_PASSWORD
    }

    private String uniqueId;
    private String owner;
    private String requestor;
    private IDM_SCOPE idmScope = IDM_SCOPE.FULL;
    private boolean isTrusted = false;

    public AccessToken() {
        super(null, null);
    }

    public AccessToken(String tokenString, DateTime tokenExpiration,
        String owner, String requestor, IDM_SCOPE idmScope) {

        super(tokenString, tokenExpiration);
        this.owner = owner;
        this.requestor = requestor;
        this.idmScope = idmScope;
    }

    public AccessToken(String tokenString, DateTime tokenExpiration,
        String owner, String requestor, IDM_SCOPE idmScope, boolean isTrusted) {

        this(tokenString, tokenExpiration, owner, requestor, idmScope);
        this.isTrusted = isTrusted;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getRequestor() {
        return requestor;
    }

    public void setRequestor(String requestor) {
        this.requestor = requestor;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isRestrictedToSetPassword() {
        return IDM_SCOPE.SET_PASSWORD == idmScope;
    }

    public void setRestrictedToSetPassword() {
        idmScope = IDM_SCOPE.SET_PASSWORD;
    }

    public boolean getIsTrusted() {
        return this.isTrusted;
    }

    public boolean isClientToken() {
        return this.owner.equals(this.requestor);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + ((idmScope == null) ? 0 : idmScope.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result
            + ((requestor == null) ? 0 : requestor.hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
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
        if (idmScope == null) {
            if (other.idmScope != null) {
                return false;
            }
        } else if (!idmScope.equals(other.idmScope)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        if (requestor == null) {
            if (other.requestor != null) {
                return false;
            }
        } else if (!requestor.equals(other.requestor)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (isTrusted != other.isTrusted) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Token [token=" + getTokenString() + ", tokenExpiration="
            + getExpirationTime() + ", owner=" + owner + ", requestor="
            + requestor + ", idmScope=" + idmScope + ", isTrusted=" + isTrusted
            + "]";
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
        private String owner;
        private String requestor;
        private IDM_SCOPE idmScope;
        private boolean isTrusted;

        SerializationProxy(AccessToken token) {
            this.tokenString = token.getTokenString();
            this.expirationTime = token.getExpirationTime();
            this.uniqueId = token.uniqueId;
            this.owner = token.owner;
            this.requestor = token.requestor;
            this.idmScope = token.idmScope;
            this.isTrusted = token.isTrusted;
        }

        private Object readResolve() {
            AccessToken token = new AccessToken(tokenString, expirationTime,
                owner, requestor, idmScope, isTrusted);
            token.setUniqueId(uniqueId);
            return token;
        }

    }
}
