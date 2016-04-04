package com.rackspace.idm.api.resource.cloud;


import com.rackspace.idm.domain.config.IdentityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 * HACK ALERT!!!
 * This class exposes the IdentityConfig as a static variable and should not be used.
 * Instead the IdentityConfig should be autowired into the class needed to access configs.
 */
@Deprecated
@Component
public class IdentityConfigHolder {

    public static IdentityConfig IDENTITY_CONFIG = null;

    @Autowired
    public IdentityConfig identityConfig;

    @PostConstruct
    public void postConstruct() {
        IDENTITY_CONFIG = this.identityConfig;
    }

}
