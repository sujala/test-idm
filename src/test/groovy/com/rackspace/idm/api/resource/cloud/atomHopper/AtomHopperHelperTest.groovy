package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.openstack.docs.identity.api.v2.Token
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.Response

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/9/13
 * Time: 6:39 PM
 * To change this template use File | Settings | File Templates.
 */
class AtomHopperHelperTest extends Specification{
    @Shared AtomHopperHelper atomHopperHelper
    @Shared Configuration config
    @Shared UserService userService
    @Shared ScopeAccessService scopeAccessService

    def setupSpec(){
        atomHopperHelper = new AtomHopperHelper();
    }

    def "Get Auth Token expired token" (){
        given:
        setupMock()
        config.getString(_) >> "auth"
        User user = new User()
        user.uniqueId = "1"
        userService.getUser(_) >> user
        ScopeAccess scopeAccess = new ScopeAccess();
        Date expiredDate = new Date()
        expiredDate.minus(1)
        scopeAccess.setAccessTokenExp(expiredDate)
        scopeAccess.setClientId("1")
        scopeAccess.setAccessTokenString("token")
        scopeAccessService.getScopeAccessByUserId(_) >> scopeAccess
        UserScopeAccess scopeAccess2 = new UserScopeAccess()
        Date date = new Date()
        scopeAccess2.setAccessTokenExp(date.plus(1))
        scopeAccess2.setClientId("1")
        scopeAccess2.setAccessTokenString("token")
        scopeAccessService.updateExpiredUserScopeAccess(_, _, _) >> scopeAccess2

        when:
        String token = atomHopperHelper.getAuthToken()

        then:
        token != null;
    }

    def "Get Auth Token good token" (){
        given:
        setupMock()
        config.getString(_) >> "auth"
        User user = new User()
        user.uniqueId = "1"
        userService.getUser(_) >> user
        ScopeAccess scopeAccess = new ScopeAccess();
        Date date = new Date()
        scopeAccess.setAccessTokenExp(date.plus(1))
        scopeAccess.setClientId("1")
        scopeAccess.setAccessTokenString("token")
        scopeAccessService.getScopeAccessByUserId(_) >> scopeAccess

        when:
        String token = atomHopperHelper.getAuthToken()

        then:
        token != null;
    }

    def setupMock(){
        config = Mock()
        atomHopperHelper.config = config
        userService = Mock()
        atomHopperHelper.userService = userService
        scopeAccessService = Mock()
        atomHopperHelper.scopeAccessService = scopeAccessService

    }
}
