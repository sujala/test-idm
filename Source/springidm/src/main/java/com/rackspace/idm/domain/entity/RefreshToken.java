package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;


public class RefreshToken extends Token implements Auditable {

    private String uniqueId;
    private String owner;
    private String requestor;

    public RefreshToken() {

    }

    public RefreshToken(String tokenString, DateTime tokenExpiration,
        String owner, String requestor) {

        super(tokenString, tokenExpiration);
        this.owner = owner;
        this.requestor = requestor;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
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
        RefreshToken other = (RefreshToken) obj;
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
        return true;
    }

    @Override
    public String toString() {
        return "RefreshToken [owner=\"" + owner + "\", requestor=\""
            + requestor + "\", expiration=\"" + getExpirationTime() + "\"]";
    }

    @Override
    public String getAuditContext() {
        return this.getTokenString();
    }
}
