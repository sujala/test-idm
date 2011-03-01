package com.rackspace.idm.domain.entity;

import java.util.List;

public class EndPoints {
    String userDN = null;
    String username = null;
    String nastId = null;
    Integer mossoId = null;
    List<String> endpoints;
    
    public String getUserDN() {
        return userDN;
    }
    
    public void setUserDN(String userDN) {
        this.userDN = userDN;
    }
    
    public List<String> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((endpoints == null) ? 0 : endpoints.hashCode());
        result = prime * result + ((userDN == null) ? 0 : userDN.hashCode());
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
        EndPoints other = (EndPoints) obj;
        if (endpoints == null) {
            if (other.endpoints != null) {
                return false;
            }
        } else if (!endpoints.equals(other.endpoints)) {
            return false;
        }
        if (userDN == null) {
            if (other.userDN != null) {
                return false;
            }
        } else if (!userDN.equals(other.userDN)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EndPoints [userDN=" + userDN + ", endpoints=" + endpoints + "]";
    }
}
