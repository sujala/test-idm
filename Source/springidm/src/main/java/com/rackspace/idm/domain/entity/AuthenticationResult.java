package com.rackspace.idm.domain.entity;

public abstract class AuthenticationResult {

    private boolean authenticated = false;
    
    public AuthenticationResult(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int oldPrime = 1231;
        final int newPrime = 1237;
        int result = 1;
        result = prime * result + (authenticated ? oldPrime : newPrime);
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
        AuthenticationResult other = (AuthenticationResult) obj;
        if (authenticated != other.authenticated) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AuthenticationResult [authenticated=" + authenticated + "]";
    }
}
