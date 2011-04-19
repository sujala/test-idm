package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

public class ScopeAccess implements Auditable {
    
    private String uniqueId;
    private String clientId;
    private String clientRCN;
    private String username;
    private String userRCN;
    private String accessToken;
    private String refreshToken;
    private DateTime accessTokenExpiration;
    private DateTime refreshTokenExpiration;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientRCN() {
        return clientRCN;
    }

    public void setClientRCN(String clientRCN) {
        this.clientRCN = clientRCN;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserRCN() {
        return userRCN;
    }

    public void setUserRCN(String userRCN) {
        this.userRCN = userRCN;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public DateTime getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(DateTime accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public DateTime getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(DateTime refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((accessToken == null) ? 0 : accessToken.hashCode());
        result = prime
            * result
            + ((accessTokenExpiration == null) ? 0 : accessTokenExpiration
                .hashCode());
        result = prime * result
            + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result
            + ((clientRCN == null) ? 0 : clientRCN.hashCode());
        result = prime * result
            + ((refreshToken == null) ? 0 : refreshToken.hashCode());
        result = prime
            * result
            + ((refreshTokenExpiration == null) ? 0 : refreshTokenExpiration
                .hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result + ((userRCN == null) ? 0 : userRCN.hashCode());
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
        ScopeAccess other = (ScopeAccess) obj;
        if (accessToken == null) {
            if (other.accessToken != null) {
                return false;
            }
        } else if (!accessToken.equals(other.accessToken)) {
            return false;
        }
        if (accessTokenExpiration == null) {
            if (other.accessTokenExpiration != null) {
                return false;
            }
        } else if (!accessTokenExpiration.equals(other.accessTokenExpiration)) {
            return false;
        }
        if (clientId == null) {
            if (other.clientId != null) {
                return false;
            }
        } else if (!clientId.equals(other.clientId)) {
            return false;
        }
        if (clientRCN == null) {
            if (other.clientRCN != null) {
                return false;
            }
        } else if (!clientRCN.equals(other.clientRCN)) {
            return false;
        }
        if (refreshToken == null) {
            if (other.refreshToken != null) {
                return false;
            }
        } else if (!refreshToken.equals(other.refreshToken)) {
            return false;
        }
        if (refreshTokenExpiration == null) {
            if (other.refreshTokenExpiration != null) {
                return false;
            }
        } else if (!refreshTokenExpiration.equals(other.refreshTokenExpiration)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (userRCN == null) {
            if (other.userRCN != null) {
                return false;
            }
        } else if (!userRCN.equals(other.userRCN)) {
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
    public String getAuditContext() {
        String format = "ScopeAccess(clientId=%s,username=%s)";
        return String.format(format, getClientId(), getUsername());
    }
}
