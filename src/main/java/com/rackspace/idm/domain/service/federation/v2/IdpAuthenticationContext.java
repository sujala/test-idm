package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;

/**
 * Maps the IDP provided authentication context constructs to Identity ones.
 */
public enum IdpAuthenticationContext {

    PASSWORD(SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.PASSWORD),
    TIMESYNCTOKEN(SAMLConstants.TIMESYNCTOKEN_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.RSAKEY);

    private String samlAuthnContextClassRef;
    private AuthenticatedByMethodEnum idmAuthBy;

    IdpAuthenticationContext(String samlAuthnContextClassRef, AuthenticatedByMethodEnum idmAuthBy) {
        this.samlAuthnContextClassRef = samlAuthnContextClassRef;
        this.idmAuthBy = idmAuthBy;
    }

    public String getSamlAuthnContextClassRef() {
        return samlAuthnContextClassRef;
    }

    public AuthenticatedByMethodEnum getIdmAuthBy() {
        return idmAuthBy;
    }

    /**
     * Convert from the SAML provided value to the enum
     * @param value
     * @return
     */
    public static IdpAuthenticationContext fromSAMLAuthnContextClassRef(String value) {
        for (IdpAuthenticationContext samlAuthContext : values()) {
            if (samlAuthContext.samlAuthnContextClassRef.equalsIgnoreCase(value)) {
                return samlAuthContext;
            }
        }
        return null;
    }
}
