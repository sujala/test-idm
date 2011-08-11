package com.rackspace.idm.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/11/11
 * Time: 3:49 PM
 */
@Component
public class AuthenticationServiceSelector {

    @Autowired
    private DefaultAuthenticationService defaultAuthenticationService;

    @Autowired
    private CloudAuthenticationService cloudAuthenticationService;

    @Value("#{properties.useCloudAuth}")
    private boolean useCloudAuth;

    public AuthenticationService getAuthenticationService(){
        if(useCloudAuth){
            return cloudAuthenticationService;
        }
        else{
            return defaultAuthenticationService;
        }
    }
}
