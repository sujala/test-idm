package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.util.AuthHeaderHelper
import com.rackspacecloud.docs.auth.api.v1.User
import com.sun.jersey.api.uri.UriBuilderImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/25/13
 * Time: 3:11 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultCloud11ServiceTest extends Specification{
    @Autowired
    DefaultCloud11Service defaultCloud11Service

    @Shared HttpServletRequest request
    @Shared  UriInfo uriInfo
    @Shared AuthHeaderHelper authHeaderHelper
    @Shared ScopeAccessService scopeAccessService
    @Shared AuthorizationService authorizationService
    @Shared def randomMosso
    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom

    def setupSpec(){
        sharedRandom = ("$randomness").replace('-',"")
    }

    def setup(){
        setupMocks()
        allowAccess()
        Random random = new Random()
        randomMosso = 100000 + random.nextInt(1000)
    }

    def "CRUD user" (){
        given:
        String username = "userName"+randomness
        User user = createUser(username,"1234567890",randomMosso, null, true)
        User userForUpdate = createUser(username,"1234567890",randomMosso+1, "nast", true)


        when:
        Response.ResponseBuilder createResponseBuilder = defaultCloud11Service.createUser(request, null, uriInfo, user)
        Response.ResponseBuilder getResponseBuilder = defaultCloud11Service.getUser(request, username, null)
        Response.ResponseBuilder updateResponseBuilder = defaultCloud11Service.updateUser(request, username, null, userForUpdate)
        Response.ResponseBuilder deleteResponseBuilder = defaultCloud11Service.deleteUser(request, username, null)

        then:
        Response createResponse = createResponseBuilder.build()
        createResponse.status == 201
        User createUser = (User)createResponse.entity
        createUser.mossoId == user.mossoId
        createUser.nastId != null

        Response getResponse = getResponseBuilder.build()
        getResponse.status == 200
        User getUser = (User)getResponse.entity
        getUser.mossoId == user.mossoId
        getUser.nastId == createUser.nastId

        Response updateResponse = updateResponseBuilder.build()
        updateResponse.status == 200
        User updateUser = (User)updateResponse.entity
        updateUser.mossoId == randomMosso+1
        updateUser.nastId == "nast"

        Response deleteResponse = deleteResponseBuilder.build()
        deleteResponse.status == 204
    }

    def createUser(String id, String key, Integer mossoId, String nastId, boolean enabled) {
        new User().with {
            it.id = id
            it.key = key
            it.mossoId = mossoId
            it.nastId = nastId
            it.enabled = enabled
            return it
        }
    }

    def allowAccess(){
        request = Mock();
        request.getHeader(_) >> "token"

        uriInfo = Mock(UriInfo)
        UriBuilder builder = new UriBuilderImpl()
        uriInfo.getRequestUriBuilder() >> builder

        authHeaderHelper.parseBasicParams(_) >> new HashMap<String, String>()
        scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(_,_,_) >> new UserScopeAccess()
        authorizationService.authorizeCloudIdentityAdmin(_) >> true
        authorizationService.authorizeCloudServiceAdmin(_) >> true
    }

    def setupMocks(){
        authHeaderHelper = Mock()
        scopeAccessService = Mock()
        authorizationService = Mock()

        defaultCloud11Service.authHeaderHelper = authHeaderHelper
        defaultCloud11Service.scopeAccessService = scopeAccessService
        defaultCloud11Service.authorizationService  = authorizationService
    }
}
