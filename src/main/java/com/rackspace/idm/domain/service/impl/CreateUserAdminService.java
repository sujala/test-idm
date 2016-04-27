package com.rackspace.idm.domain.service.impl;


import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.CreateUserService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.UserService;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class CreateUserAdminService implements CreateUserService {

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    UserService userService;

    @Autowired
    RequestContextHolder requestContextHolder;

    @Override
    public User setDefaultsAndCreateUser(org.openstack.docs.identity.api.v2.User userForCreate, User userForDefaults) {
        User user = this.userConverterCloudV20.fromUser(userForCreate);

        userService.configureNewUserAdmin(user, CreateUserUtil.isCreateUserOneCall(userForCreate));

        IdentityUserTypeEnum callerUserType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
        if (!(IdentityUserTypeEnum.IDENTITY_ADMIN.equals(callerUserType) || IdentityUserTypeEnum.SERVICE_ADMIN.equals(callerUserType))
                || !identityConfig.getReloadableConfig().getFeatureAETokensDecrypt()) {
            user.setTokenFormat(null);
        }

        //always provision mosso and nast tenants when create a user admin
        boolean isCreateUserOneCall = CreateUserUtil.isCreateUserOneCall(userForCreate);
        //only provision mosso and nast tenants if this is a one-user call and the domain is numeric
        userService.addUserV20(user, isCreateUserOneCall,  isCreateUserOneCall && isNumeric(userForCreate.getDomainId()));
        return user;
    }

    private boolean isNumeric(String domainId) {
        try {
            Integer.parseInt(domainId);
        } catch(NumberFormatException e) {
            return false;
        }
        return true;
    }

}
