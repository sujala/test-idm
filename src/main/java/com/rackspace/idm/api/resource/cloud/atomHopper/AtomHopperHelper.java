package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class AtomHopperHelper {
    @Autowired
    private IdentityConfig identityConfig;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private UserService userService;

    private static final List<String> AUTH_BY_SYSTEM_LIST = Collections.unmodifiableList(Arrays.asList(AuthenticatedByMethodEnum.SYSTEM.getValue()));

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public String getAuthToken() {
        logger.debug("Getting admin token ...");
        User user = userService.getUser(identityConfig.getStaticConfig().getGaUsername());
        ScopeAccess access = scopeAccessService.addScopeAccess(user,
                identityConfig.getStaticConfig().getCloudAuthClientId(), AUTH_BY_SYSTEM_LIST);

        return access.getAccessTokenString();
    }

    public void entityConsume(HttpEntity entity) throws IOException {
        if(entity != null){
            EntityUtils.consume(entity);
        }
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
