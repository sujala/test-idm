package com.rackspace.idm.authorizationService;

import java.util.List;

import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.XACMLRequestCreationException;

public interface AuthorizationService {

    AuthorizationRequest createRequest(List<Entity> entities)
        throws XACMLRequestCreationException;

    boolean doAuthorization(AuthorizationRequest request)
        throws ForbiddenException;
}