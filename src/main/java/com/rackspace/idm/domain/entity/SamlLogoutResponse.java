package com.rackspace.idm.domain.entity;

import lombok.Getter;
import lombok.Setter;
import org.opensaml.saml.saml2.core.LogoutResponse;

@Getter
@Setter
public class SamlLogoutResponse {
    private final LogoutResponse logoutResponse;
    private Exception exceptionThrown;

    public SamlLogoutResponse(LogoutResponse logoutResponse) {
        this.logoutResponse = logoutResponse;
    }

    public SamlLogoutResponse(LogoutResponse logoutResponse, Exception ex) {
        this.logoutResponse = logoutResponse;
        this.exceptionThrown = ex;
    }

    public boolean hasException() {
        return exceptionThrown != null;
    }
}
