package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/9/13
 * Time: 6:39 PM
 * To change this template use File | Settings | File Templates.
 */
class AtomHopperHelperTest extends Specification{
    @Shared AtomHopperHelper atomHopperHelper
    @Shared IdentityConfig identityConfig
    @Shared UserService userService
    @Shared ScopeAccessService scopeAccessService

    def setupSpec(){
        atomHopperHelper = new AtomHopperHelper();
    }

    def "Get Auth Token good token" (){
        given:
        setupMock()
        identityConfig.getGaUsername() >> "auth"
        identityConfig.getCloudAuthClientId() >> "aclient"
        User user = new User()
        user.uniqueId = "1"
        userService.getUser(_) >> user
        ScopeAccess scopeAccess = new UserScopeAccess();
        Date date = new Date()
        scopeAccess.setAccessTokenExp(date.plus(1))
        scopeAccess.setClientId("1")
        scopeAccess.setAccessTokenString("token")
        scopeAccessService.addScopeAccess(_, _, _) >> scopeAccess

        when:
        String token = atomHopperHelper.getAuthToken()

        then:
        token != null;
    }

    def setupMock(){
        identityConfig = Mock()
        atomHopperHelper.identityConfig = identityConfig
        userService = Mock()
        atomHopperHelper.userService = userService
        scopeAccessService = Mock()
        atomHopperHelper.scopeAccessService = scopeAccessService

    }
}
