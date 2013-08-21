package com.rackspace.idm.domain.entity;

import lombok.Data;

@Data
public class UserAuthenticationResult extends AuthenticationResult {

    private final User user;
    
    public UserAuthenticationResult(User user, boolean authenticated) {
        super(authenticated);
        this.user = user;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
