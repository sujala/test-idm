package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;
import com.rackspace.idm.exception.BadRequestException;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.commons.lang.StringUtils;

import static com.rackspace.idm.ErrorCodes.*;

/**
 * A simple bean for extracting the necessary information from a raw saml request - refines a federatedAuthRequest to be
 * a Federated Racker Auth Request
 */
public class FederatedRackerAuthRequest {

    @Delegate
    private FederatedAuthRequest federatedAuthRequest;

    @Getter
    private String username;

    @Getter
    AuthenticatedByMethodEnum authenticatedByForRequest;

    public FederatedRackerAuthRequest(FederatedAuthRequest federatedAuthRequest) {
        this.federatedAuthRequest = federatedAuthRequest;

        username = federatedAuthRequest.getWrappedSamlResponse().getUsername();
        authenticatedByForRequest = processAuthenticatedByForRequest();

        validateStructure();
    }

    private void validateStructure() {
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException("Invalid username. One, and only one, username must be provided.");
        }
        if (authenticatedByForRequest == null) {
            throw new BadRequestException("Unsupported authentication context class ref.", ERROR_CODE_FEDERATION2_INVALID_AUTH_CONTEXT);
        }
    }
    private AuthenticatedByMethodEnum processAuthenticatedByForRequest() {
        RackerAuthContextEnum val = RackerAuthContextEnum.fromSAMLAuthnContextClassRef(federatedAuthRequest.getIdpAuthContext());
        return val != null ? val.getIdmAuthBy() : null;
    }

    private enum RackerAuthContextEnum {
        PASSWORD(SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.PASSWORD),
        TIMESYNCTOKEN(SAMLConstants.TIMESYNCTOKEN_PROTECTED_AUTHCONTEXT_REF_CLASS, AuthenticatedByMethodEnum.RSAKEY);

        private String samlAuthnContextClassRef;
        private AuthenticatedByMethodEnum idmAuthBy;

        RackerAuthContextEnum(String samlAuthnContextClassRef, AuthenticatedByMethodEnum idmAuthBy) {
            this.samlAuthnContextClassRef = samlAuthnContextClassRef;
            this.idmAuthBy = idmAuthBy;
        }

        public AuthenticatedByMethodEnum getIdmAuthBy() {
            return idmAuthBy;
        }

        public static RackerAuthContextEnum fromSAMLAuthnContextClassRef(String value) {
            for (RackerAuthContextEnum rackerAuthContextEnum : values()) {
                if (rackerAuthContextEnum.samlAuthnContextClassRef.equalsIgnoreCase(value)) {
                    return rackerAuthContextEnum;
                }
            }
            return null;
        }
    }
}
