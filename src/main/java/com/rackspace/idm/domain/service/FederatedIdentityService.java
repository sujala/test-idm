package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.SamlAuthResponse;
import org.opensaml.saml2.core.Response;

public interface FederatedIdentityService {
    SamlAuthResponse processSamlResponse(Response samlResponse);
}
