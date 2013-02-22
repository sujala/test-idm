package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11
import com.rackspace.idm.api.resource.cloud.Validator
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.Pattern
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspacecloud.docs.auth.api.v1.User
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/12/12
 * Time: 1:11 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultCloud11ServiceGroovyTest extends RootServiceTest {

    @Shared DefaultCloud11Service service
    @Shared LdapPatternRepository ldapPatternRepository
    @Shared UserConverterCloudV11 userConverterCloudV11
    @Shared HttpServletRequest request

    def setupSpec(){
        service = new DefaultCloud11Service()
        validator = new Validator()
        service.cloudExceptionResponse = new CloudExceptionResponse()
        service.validator = validator
    }

    def cleanupSpec() {
    }

    def setup(){
        ldapPatternRepository = Mock()
        userConverterCloudV11 = Mock()
        validator.ldapPatternRepository = ldapPatternRepository
        service.userConverterCloudV11 = userConverterCloudV11

        mockAuthHeaderHelper(service)
        mockScopeAccessService(service)
        mockConfiguration(service)
        mockAuthorizationService(service)
        mockUserService(service)
        mockDomainService(service)
        mockTenantService(service)
        mockEndpointService(service)
        mockApplicationService(service)
    }

    def "Create User with valid username" () {
        given:
        allowAccess()
        User user = new User();
        user.id = "someN@me"
        user.mossoId = 9876543210
        def user1 = entityFactory.createUser()
        user1.mossoId = 1
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> null
        userConverterCloudV11.toUserDO(_) >> user1
        domainService.createNewDomain(_) >> "1"
        endpointService.getBaseUrlsByBaseUrlType(_) >> new ArrayList<CloudBaseUrl>()
        Application application = new Application()
        application.openStackType = "someType"
        applicationService.getByName(_) >> application
        ClientRole clientRole = new ClientRole()
        clientRole.id = "1"
        clientRole.name = "name"
        clientRole.clientId = "1"
        applicationService.getClientRoleByClientIdAndRoleName(_,_) >> clientRole
        endpointService.getBaseUrlById(_) >> new CloudBaseUrl()
        applicationService.getClientRoleById(_) >> clientRole

        when:
        Response.ResponseBuilder builder = service.createUser(request, null, uriInfo(), user)

        then:
        2 * domainService.addTenantToDomain(_,_)
        builder.build().status == 201
    }

    def "Create user should return userId in the location header and not username" () {
        given:
        allowAccess()
        User user = new User();
        user.id = "someN@me"
        user.mossoId = 9876543210
        def user1 = entityFactory.createUser().with {
            it.username = "my_username"
            it.mossoId = 1
            return it
        }
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> null
        userConverterCloudV11.toUserDO(_) >> user1
        userConverterCloudV11.toCloudV11User(_, _) >> v1Factory.createUser()
        domainService.createNewDomain(_) >> "1"
        endpointService.getBaseUrlsByBaseUrlType(_) >> new ArrayList<CloudBaseUrl>()
        Application application = new Application()
        application.openStackType = "someType"
        applicationService.getByName(_) >> application
        ClientRole clientRole = new ClientRole()
        clientRole.id = "1"
        clientRole.name = "name"
        clientRole.clientId = "1"
        applicationService.getClientRoleByClientIdAndRoleName(_,_) >> clientRole
        endpointService.getBaseUrlById(_) >> new CloudBaseUrl()
        applicationService.getClientRoleById(_) >> clientRole

        when:
        def response = service.createUser(request, null, uriInfo(), user).build()

        then:
        1 * userService.addUser(_) >> { com.rackspace.idm.domain.entity.User arg1 ->
            arg1.id = "userId"
        }
        response.status == 201
        !response.getMetadata().get("Location").toString().contains("userId")
        response.getMetadata().get("Location").toString().contains("my_username")
    }

    def "Create User with valid username: start with a number" () {
        given:
        allowAccess()
        User user = new User();
        user.id = "1someN@me"
        user.mossoId = 9876543210
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> null
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User()
        user1.setId("1")
        user1.setUsername("someN@me")
        user1.setMossoId(1)
        userConverterCloudV11.toUserDO(_) >> user1
        domainService.createNewDomain(_) >> "1"
        endpointService.getBaseUrlsByBaseUrlType(_) >> new ArrayList<CloudBaseUrl>()
        Application application = new Application()
        application.openStackType = "someType"
        applicationService.getByName(_) >> application
        ClientRole clientRole = new ClientRole()
        clientRole.id = "1"
        clientRole.name = "name"
        clientRole.clientId = "1"
        applicationService.getClientRoleByClientIdAndRoleName(_,_) >> clientRole
        endpointService.getBaseUrlById(_) >> new CloudBaseUrl()
        applicationService.getClientRoleById(_) >> clientRole


        when:
        Response.ResponseBuilder builder = service.createUser(request, null, uriInfo(), user)

        then:
        builder.build().status == 201
    }

    def "Create User with invalid username: empty" () {
        given:
        allowAccess()
        User user = new User();
        user.id = ""
        user.mossoId = 9876543210
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        Response.ResponseBuilder responseBuilder = service.createUser(request, null, uriInfo(), user)

        then:
        responseBuilder.build().status == 400
    }

    def "Create User with invalid username: null" () {
        given:
        allowAccess()
        User user = new User();
        user.id = null
        user.mossoId = 9876543210
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        Response.ResponseBuilder responseBuilder = service.createUser(request, null, uriInfo(), user)

        then:
        responseBuilder.build().status == 400
    }

    def "Create User with invalid username: spaces" () {
        given:
        allowAccess()
        User user = new User();
        user.id = "    "
        user.mossoId = 9876543210
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        Response.ResponseBuilder responseBuilder = service.createUser(request, null, uriInfo(), user)

        then:
        responseBuilder.build().status == 400
    }

    def "Update user: valid username" (){
        given:
        allowAccess()
        User user = new User();
        user.id = "jmunoz"
        user.enabled = true
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> new com.rackspace.idm.domain.entity.User()

        when:
        Response.ResponseBuilder responseBuilder = service.updateUser(request, "jmunoz", null, user)

        then:
        responseBuilder.build().status == 200
    }

    def "Update user: invalid username" (){
        given:
        allowAccess()
        User user = new User();
        user.id = "jmunoz!@#"
        user.enabled = true
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> new com.rackspace.idm.domain.entity.User()

        when:
        Response.ResponseBuilder responseBuilder = service.updateUser(request, "jmunoz", null, user)

        then:
        responseBuilder.build().status == 400
    }

    def "Update user: empty username" (){
        given:
        allowAccess()
        User user = new User();
        user.id = ""
        user.enabled = true
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> new com.rackspace.idm.domain.entity.User()

        when:
        Response.ResponseBuilder responseBuilder = service.updateUser(request, "jmunoz", null, user)

        then:
        responseBuilder.build().status == 400
    }

    def "GET - userByMossoId - returns 301" () {
        given:
        allowAccess()
        userService.getUserByTenantId(_) >> createUser("1", "someName", 123, "nast")


        when:
        Response.ResponseBuilder responseBuilder = service.getUserFromMossoId(request,123,null)

        then:
        Response response = responseBuilder.build()
        response.status == 301
        response.metadata.get("Location")[0] == "/v1.1/users/someName"
    }

    def "GET - userByNastId - returns 301" () {
        given:
        allowAccess()
        userService.getUserByTenantId(_) >> createUser("1", "someName", 123, "nast")


        when:
        Response.ResponseBuilder responseBuilder = service.getUserFromNastId(request,"nast",null)

        then:
        Response response = responseBuilder.build()
        response.status == 301
        response.metadata.get("Location")[0] == "/v1.1/users/someName"
    }

    def "doesBaseUrlBelongToRegion returns true when id is within range"(){
        when:
        config.getString("cloud.region") >> region
        def result = service.doesBaseUrlBelongToRegion(baseUrl)

        then:
        result == expected

        where:
        expected    | baseUrl           | region
        false       | cloudBaseUrl(null)| "UK"
        false       | cloudBaseUrl(1)   | "UK"
        false       | cloudBaseUrl(999) | "UK"
        true        | cloudBaseUrl(1000)| "UK"
        false       | cloudBaseUrl(1001)| "US"
        true        | cloudBaseUrl(999) | "US"
        true        | cloudBaseUrl(1)   | "US"
    }

    def "validateToken should not return a token when the token is expired"() {
        when:
        allowAccess()
        def token = "this_is_my_token"
        scopeAccessService.getScopeAccessByAccessToken(_) >> scopeAccess

        def response = service.validateToken(request, token, null, null, headers).build()

        then:
        response.status == 404
        response.getEntity().message.contains(token) == expected

        where:
        expected    | scopeAccess
        false       | expireScopeAccess(createUserScopeAccess())
        false       | expireScopeAccess(createImpersonatedScopeAccess())
    }

    def createUser(String id, String username, int mossoId, String nastId) {
        new com.rackspace.idm.domain.entity.User().with {
            it.id = id
            it.username = username
            it.mossoId = mossoId
            it.nastId = nastId
            return it
        }
    }

    def pattern(String name, String regex, String errMsg, String description){
        new Pattern().with {
            it.name = name
            it.regex = regex
            it.errMsg = errMsg
            it.description = description
            return it
        }
    }

    def cloudBaseUrl(Integer baseUrlId){
        new CloudBaseUrl().with {
            it.baseUrlId = baseUrlId
            return it
        }
    }

    def allowAccess(){
        request = Mock();
        request.getHeader(_) >> "token"

        authHeaderHelper.parseBasicParams(_) >> new HashMap<String, String>()
        scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(_,_,_) >> new UserScopeAccess()
        authorizationService.authorizeCloudIdentityAdmin(_) >> true
        authorizationService.authorizeCloudServiceAdmin(_) >> true
    }

}
