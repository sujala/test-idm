package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class CreateIdentityAdminService implements CreateUserService {

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    UserService userService;

    @Autowired
    RoleService roleService;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    DomainService domainService;

    @Override
    public User setDefaultsAndCreateUser(org.openstack.docs.identity.api.v2.User userForCreate, User userForDefaults) {
        User user = this.userConverterCloudV20.fromUser(userForCreate);
        setUserDefaults(user);
        userService.addUserV20(user);
        return user;
    }

    public void setUserDefaults(User user) {
        if (StringUtils.isBlank(user.getDomainId())) {
            user.setDomainId(domainService.getDomainUUID());
        }

        CreateUserUtil.attachRoleToUser(roleService.getIdentityAdminRole(), user);

        if (!identityConfig.getReloadableConfig().getFeatureAETokensDecrypt()) {
            user.setTokenFormat(null);
        }
    }

}
