package com.rackspace.idm.domain.decorator;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;

/**
 * Maps saml authn contexts to appropriate identity authenticated by values
 */
public enum SAMLAuthContext {

    PASSWORD(SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.PASSWORD),
    TIMESYNCTOKEN(SAMLConstants.TIMESYNCTOKEN_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.RSAKEY);

    private String samlAuthnContextClassRef;
    private AuthenticatedByMethodEnum idmAuthBy;

    SAMLAuthContext(String samlAuthnContextClassRef, AuthenticatedByMethodEnum idmAuthBy) {
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
    public static SAMLAuthContext fromSAMLAuthnContextClassRef(String value) {
        for (SAMLAuthContext samlAuthContext : values()) {
            if (samlAuthContext.samlAuthnContextClassRef.equalsIgnoreCase(value)) {
                return samlAuthContext;
            }
        }
        return null;
    }
}
