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
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.util.AuthHeaderHelper
import com.rackspacecloud.docs.auth.api.v1.User
import com.sun.jersey.api.uri.UriBuilderImpl
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/12/12
 * Time: 1:11 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultCloud11ServiceGroovyTest extends Specification{
    @Shared Validator validator
    @Shared LdapPatternRepository ldapPatternRepository
    @Shared HttpServletRequest request
    @Shared AuthHeaderHelper authHeaderHelper
    @Shared ScopeAccessService scopeAccessService
    @Shared Configuration config
    @Shared AuthorizationService authorizationService
    @Shared UserService userService
    @Shared UserConverterCloudV11 userConverterCloudV11
    @Shared DomainService domainService
    @Shared TenantService tenantService
    @Shared EndpointService endpointService
    @Shared ApplicationService clientService
    @Shared  UriInfo uriInfo
    @Shared CloudExceptionResponse cloudExceptionResponse

    @Shared DefaultCloud11Service defaultCloud11Service

    def setupSpec(){
        defaultCloud11Service = new DefaultCloud11Service()
    }

    def cleanupSpec() {
    }

    def "Create User with valid username" () {
        given:
        setupMock()
        allowAccess()
        User user = new User();
        user.id = "someN@me"
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
        clientService.getByName(_) >> application
        ClientRole clientRole = new ClientRole()
        clientRole.id = "1"
        clientRole.name = "name"
        clientRole.clientId = "1"
        clientService.getClientRoleByClientIdAndRoleName(_,_) >> clientRole
        endpointService.getBaseUrlById(_) >> new CloudBaseUrl()
        clientService.getClientRoleById(_) >> clientRole


        when:
        Response.ResponseBuilder builder = defaultCloud11Service.createUser(request,null,uriInfo,user)

        then:
        builder.build().status == 201
    }

    def "Create User with valid username: start with a number" () {
        given:
        setupMock()
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
        clientService.getByName(_) >> application
        ClientRole clientRole = new ClientRole()
        clientRole.id = "1"
        clientRole.name = "name"
        clientRole.clientId = "1"
        clientService.getClientRoleByClientIdAndRoleName(_,_) >> clientRole
        endpointService.getBaseUrlById(_) >> new CloudBaseUrl()
        clientService.getClientRoleById(_) >> clientRole


        when:
        Response.ResponseBuilder builder = defaultCloud11Service.createUser(request,null,uriInfo,user)

        then:
        builder.build().status == 201
    }

    def "Create User with invalid username: empty" () {
        given:
        setupMock()
        allowAccess()
        User user = new User();
        user.id = ""
        user.mossoId = 9876543210
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.createUser(request,null,uriInfo,user)

        then:
        responseBuilder.build().status == 400
    }

    def "Create User with invalid username: null" () {
        given:
        setupMock()
        allowAccess()
        User user = new User();
        user.id = null
        user.mossoId = 9876543210
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.createUser(request,null,uriInfo,user)

        then:
        responseBuilder.build().status == 400
    }

    def "Create User with invalid username: spaces" () {
        given:
        setupMock()
        allowAccess()
        User user = new User();
        user.id = "    "
        user.mossoId = 9876543210
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.createUser(request,null,uriInfo,user)

        then:
        responseBuilder.build().status == 400
    }

    def "Update user: valid username" (){
        given:
        setupMock()
        allowAccess()
        User user = new User();
        user.id = "jmunoz"
        user.enabled = true
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> new com.rackspace.idm.domain.entity.User()

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.updateUser(request, "jmunoz", null, user)

        then:
        responseBuilder.build().status == 200
    }

    def "Update user: invalid username" (){
        given:
        setupMock()
        allowAccess()
        User user = new User();
        user.id = "jmunoz!@#"
        user.enabled = true
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> new com.rackspace.idm.domain.entity.User()

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.updateUser(request, "jmunoz", null, user)

        then:
        responseBuilder.build().status == 400
    }

    def "Update user: empty username" (){
        given:
        setupMock()
        allowAccess()
        User user = new User();
        user.id = ""
        user.enabled = true
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern
        userService.getUser(_) >> new com.rackspace.idm.domain.entity.User()

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.updateUser(request, "jmunoz", null, user)

        then:
        responseBuilder.build().status == 400
    }

    def "GET - userByMossoId - returns 301" () {
        given:
        setupMock()
        allowAccess()
        userService.getUserByTenantId(_) >> createUser("1", "someName", 123, "nast")


        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getUserFromMossoId(request,123,null)

        then:
        Response response = responseBuilder.build()
        response.status == 301
        response.metadata.get("Location")[0] == "/v1.1/users/someName"
    }

    def "GET - userByNastId - returns 301" () {
        given:
        setupMock()
        allowAccess()
        userService.getUserByTenantId(_) >> createUser("1", "someName", 123, "nast")


        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getUserFromNastId(request,"nast",null)

        then:
        Response response = responseBuilder.build()
        response.status == 301
        response.metadata.get("Location")[0] == "/v1.1/users/someName"
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

    def allowAccess(){
        request = Mock();
        request.getHeader(_) >> "token"

        authHeaderHelper.parseBasicParams(_) >> new HashMap<String, String>()
        scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(_,_,_) >> new UserScopeAccess()
        authorizationService.authorizeCloudIdentityAdmin(_) >> true
        authorizationService.authorizeCloudServiceAdmin(_) >> true
    }

    def setupMock(){
        validator = new Validator();
        ldapPatternRepository = Mock()
        validator.ldapPatternRepository = ldapPatternRepository
        authHeaderHelper = Mock()
        scopeAccessService = Mock()
        config = Mock()
        authorizationService = Mock()
        userService = Mock()
        userConverterCloudV11 = Mock()
        domainService = Mock()
        tenantService = Mock()
        endpointService = Mock()
        clientService = Mock()
        uriInfo = Mock(UriInfo)
        UriBuilder builder = new UriBuilderImpl()
        uriInfo.getRequestUriBuilder() >> builder
        cloudExceptionResponse = new CloudExceptionResponse()

        defaultCloud11Service.setValidator(validator)
        defaultCloud11Service.setAuthHeaderHelper(authHeaderHelper)
        defaultCloud11Service.setScopeAccessService(scopeAccessService)
        defaultCloud11Service.setConfig(config)
        defaultCloud11Service.setAuthorizationService(authorizationService)
        defaultCloud11Service.setUserService(userService)
        defaultCloud11Service.setUserConverterCloudV11(userConverterCloudV11)
        defaultCloud11Service.setDomainService(domainService)
        defaultCloud11Service.setTenantService(tenantService)
        defaultCloud11Service.setEndpointService(endpointService)
        defaultCloud11Service.setApplicationService(clientService)
        defaultCloud11Service.setCloudExceptionResponse(cloudExceptionResponse)
    }
}
