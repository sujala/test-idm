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
        int result = 1;
        result = prime * result + (authenticated ? 1231 : 1237);
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
