package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/9/13
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class AtomHopperHelper {
    @Autowired
    private Configuration config;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private UserService userService;

    private ObjectFactory objectFactory = new ObjectFactory();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String getAuthToken() throws IOException, JAXBException {
        logger.warn("Getting admin token ...");
        User user = userService.getUser(config.getString("ga.username"));
        ScopeAccess access = scopeAccessService.getScopeAccessByUserId(user.getId());
        String clientId = access.getClientId();
        if(access.getAccessTokenExp().before(new Date())){
            access = scopeAccessService.updateExpiredUserScopeAccess(user.getUniqueId(), clientId);
        }
        return access.getAccessTokenString();
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
