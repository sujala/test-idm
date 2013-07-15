package com.rackspace.idm.domain.entity;

import lombok.Data;

@Data
public abstract class AuthenticationResult {

    private boolean authenticated = false;
    
    public AuthenticationResult(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @Override
    public String toString() {
        return "AuthenticationResult [authenticated=" + authenticated + "]";
    }
}
