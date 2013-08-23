package com.rackspace.idm.api.resource.cloud.devops;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthorizedException;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/26/13
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultDevOpsService implements DevOpsService {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    UserService userService;

    @Override
    @Async
    public void encryptUsers(String authToken) {
        authorizationService.verifyServiceAdminLevelAccess(getScopeAccessForValidToken(authToken));
        userService.reEncryptUsers();
    }

    private ScopeAccess getScopeAccessForValidToken(String authToken) {
        String errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.";
        if (StringUtils.isBlank(authToken)) {
            throw new NotAuthorizedException(errMsg);
        }
        ScopeAccess authScopeAccess = this.scopeAccessService.getScopeAccessByAccessToken(authToken);
        if (authScopeAccess == null || (authScopeAccess.isAccessTokenExpired(new DateTime()))) {
            throw new NotAuthorizedException(errMsg);
        }
        return authScopeAccess;
    }
}
