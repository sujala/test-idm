package com.rackspace.idm.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/11/11
 * Time: 4:20 PM
 */
@Component
public class CloudAuthenticationService implements AuthenticationService{

    @Autowired
    private DefaultAuthenticationService defaultAuthenticationService;

}
