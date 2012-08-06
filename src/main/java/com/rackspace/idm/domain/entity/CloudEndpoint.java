package com.rackspace.idm.domain.entity;

public class CloudEndpoint {
    private boolean v1preferred;
    private CloudBaseUrl baseUrl;
    private String username;
    private String nastId;
    private Integer mossoId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNastId() {
        return nastId;
    }

    public void setNastId(String nastId) {
        this.nastId = nastId;
    }

    public Integer getMossoId() {
        return mossoId;
    }

    public void setMossoId(Integer mossoId) {
        this.mossoId = mossoId;
    }

    public boolean isV1preferred() {
        return v1preferred;
    }

    public void setV1preferred(boolean v1preferred) {
        this.v1preferred = v1preferred;
    }

    public CloudBaseUrl getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(CloudBaseUrl baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int oldPrime = 1231;
        final int newPrime = 1237;
        int result = 1;
        result = prime * result + ((baseUrl == null) ? 0 : baseUrl.hashCode());
        result = prime * result + ((mossoId == null) ? 0 : mossoId.hashCode());
        result = prime * result + ((nastId == null) ? 0 : nastId.hashCode());
        result = prime * result
            + ((username == null) ? 0 : username.hashCode());
        result = prime * result + (v1preferred ? oldPrime : newPrime);
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
        CloudEndpoint other = (CloudEndpoint) obj;
        if (baseUrl == null) {
            if (other.baseUrl != null) {
                return false;
            }
        } else if (!baseUrl.equals(other.baseUrl)) {
            return false;
        }
        if (mossoId == null) {
            if (other.mossoId != null) {
                return false;
            }
        } else if (!mossoId.equals(other.mossoId)) {
            return false;
        }
        if (nastId == null) {
            if (other.nastId != null) {
                return false;
            }
        } else if (!nastId.equals(other.nastId)) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        if (v1preferred != other.v1preferred) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CloudEndpoint [v1preferred=" + v1preferred + ", baseUrl="
            + baseUrl + ", username=" + username + ", nastId=" + nastId
            + ", mossoId=" + mossoId + "]";
    }
}
