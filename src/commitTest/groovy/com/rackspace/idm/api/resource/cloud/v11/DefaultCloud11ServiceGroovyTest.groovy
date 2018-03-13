package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.domain.config.providers.cloudv11.Core11XMLWriter
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.validation.Validator
import com.rackspacecloud.docs.auth.api.v1.User
import com.rackspacecloud.docs.auth.api.v1.UserCredentials
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled
import org.apache.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import javax.xml.bind.JAXBElement
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
    JAXBObjectFactories jaxbObjectFactories = new JAXBObjectFactories()
    CredentialValidator credentialValidator

    @Shared Validator realValidator

    def setupSpec(){
        service = new DefaultCloud11Service()
        service.cloudExceptionResponse = new CloudExceptionResponse()

        realValidator = new Validator()
    }

    def cleanupSpec() {
    }

    def setup(){
        userConverterCloudV11 = Mock()
        credentialValidator = Mock(CredentialValidator)
        service.userConverterCloudV11 = userConverterCloudV11
        service.credentialValidator = credentialValidator

        mockAuthHeaderHelper(service)
        mockScopeAccessService(service)
        mockConfiguration(service)
        mockAuthorizationService(service)
        mockUserService(service)
        mockTenantService(service)
        mockEndpointService(service)
        mockEndpointService(service)
        mockAtomHopperClient(service)
        mockValidator(service)
        mockTokenRevocationService(service)
        mockRequestContextHolder(service)
        mockAuthWithApiKeyCredentials(service)
        mockAuthConverterCloudV11(service)
    }

    def "addBaseUrl handles missing attribute error"() {
        given:
        service.validator = realValidator
        allowAccess()
        def baseUrl = entityFactory.createBaseUrl(null)

        when:
        def response = service.addBaseURL(null, null, baseUrl)

        then:
        response.status == HttpStatus.SC_BAD_REQUEST

        cleanup:
        service.validator = validator
    }

    def "Create User" () {
        given:
        allowAccess()

        def user = new User();
        def userEntity = entityFactory.createUser().with( {
            it.id = "userId"
            it.username = "my_username"
            return it
        })

        def caller = entityFactory.createUser().with {
            it.username = "caller"
            return it
        }

        when:
        def response = service.createUser(request, null, uriInfo(), user).build()

        then:
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * validator.validate11User(user)
        1 * userConverterCloudV11.fromUser(user) >> userEntity
        1 * userService.configureNewUserAdmin(userEntity, true);
        1 * userService.addUserv11(userEntity)
        1 * scopeAccessService.getOpenstackEndpointsForUser(userEntity) >> []
        1 * userConverterCloudV11.toCloudV11User(userEntity, []) >> user
        response.status == 201
        response.entity == user
        !response.getMetadata().get("Location").toString().contains("userId")
        response.getMetadata().get("Location").toString().contains("my_username")
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

    def "Updating enabled user to disabled sends a feed to atomHopper"(){
        given:
        allowAccess()
        mockValidator(service)
        User user = new User().with {
            it.id = "jmunoz"
            it.key = "1234567890"
            it.enabled = false
            it.nastId = "nastId"
            it.mossoId = 10001
            return it
        }
        com.rackspace.idm.domain.entity.User updateUser = createUser("jmunoz","jorge",10001, "nastId")
        updateUser.enabled = true

        userService.getUser(_) >> updateUser

        when:
        service.updateUser(request, "jmunoz", null, user)

        then:
        1 * atomHopperClient.asyncPost(_, AtomHopperConstants.DISABLED)
        1 * atomHopperClient.asyncPost(_, AtomHopperConstants.UPDATE)

    }

    def "Updating disabled user to disabled does not send a feed to atomHopper"(){
        given:
        allowAccess()
        mockValidator(service)
        User user = new User().with {
            it.id = "jmunoz"
            it.key = "1234567890"
            it.enabled = false
            it.nastId = "nastId"
            it.mossoId = 10001
            return it
        }

        com.rackspace.idm.domain.entity.User updateUser = createUser("jmunoz","jorge",10001, "nastId")
        updateUser.enabled = false

        userService.getUser(_) >> updateUser

        when:
        service.updateUser(request, "jmunoz", null, user)

        then:
        1 * atomHopperClient.asyncPost(_, AtomHopperConstants.UPDATE)

    }

    def "Set enabled user to disabled sends a feed to atomHopper"(){
        given:
        allowAccess()
        mockValidator(service)
        UserWithOnlyEnabled user = new UserWithOnlyEnabled().with {
            it.id = "jmunoz"
            it.enabled = false
            return it
        }
        com.rackspace.idm.domain.entity.User updateUser = createUser("jmunoz","jorge",10001, "nastId")
        updateUser.enabled = true

        userService.getUser(_) >> updateUser

        when:
        service.setUserEnabled(request, "jmunoz",  user, null)

        then:
        1 * atomHopperClient.asyncPost(_,_)

    }

    def "Set disabled user to disabled does not send a feed to atomHopper"(){
        given:
        allowAccess()
        mockValidator(service)
        UserWithOnlyEnabled user = new UserWithOnlyEnabled().with {
            it.id = "jmunoz"
            it.enabled = false
            return it
        }
        com.rackspace.idm.domain.entity.User updateUser = createUser("jmunoz","jorge",10001, "nastId")
        updateUser.enabled = false

        userService.getUser(_) >> updateUser

        when:
        service.setUserEnabled(request, "jmunoz",  user, null)

        then:
        0 * atomHopperClient.asyncPost(_,_)
    }

    def "Do not add v1Default if set to false and tenant contains v1Defaults" ()  {
        def baseUrlRef = v1Factory.createBaseUrlRef()
        baseUrlRef.v1Default = false
        def tenant = entityFactory.createTenant()
        tenant.v1Defaults = ["2"].asList()

        when:
        service.replaceAddV1Default(baseUrlRef, tenant, "1")

        then:
        0 * endpointService.getBaseUrlById(_)
    }

    def "Do not add v1Default if set to true and tenant contains v1Defaults" ()  {
        def baseUrlRef = v1Factory.createBaseUrlRef()
        baseUrlRef.v1Default = true
        def tenant = entityFactory.createTenant()
        tenant.v1Defaults = ["2"].asList()
        def cloudBaseUrl = entityFactory.createCloudBaseUrl()
        cloudBaseUrl.serviceName = "serviceName"

        when:
        service.replaceAddV1Default(baseUrlRef, tenant, "1")

        then:
        2 * endpointService.getBaseUrlById(_) >> cloudBaseUrl
    }

    def "Verify that Impersonation scope access create date is set" () {
        given:
        def username = "username"
        def token = "token"
        def createdDate = new Date()
        def expiredDate = new Date().plus(1)
        ImpersonatedScopeAccess sa = new ImpersonatedScopeAccess().with {
            it.accessTokenString = token
            it.impersonatingUsername = username
            it.createTimestamp = createdDate
            it.accessTokenExp = expiredDate
            it
        }
        when:
        def usa = service.getUserFromImpersonatedScopeAccess(sa)

        then:
        usa.accessTokenString == token
        usa.accessTokenExp == expiredDate
        usa.createTimestamp == createdDate
    }

    def "test hideAdminUrls" () {
        given:
        def adminUrl = "adminUrl"
        def internalUrl = "internalUrl"
        def publicUrl = "publicUrl"
        def baseUrl= new CloudBaseUrl().with {
            it.adminUrl = adminUrl
            it.internalUrl = internalUrl
            it.publicUrl = publicUrl
            it
        }
        def endpoint = new OpenstackEndpoint().with {
            it.baseUrls  = [baseUrl, baseUrl].asList()
            it
        }
        def endpoints = [endpoint, endpoint].asList()

        when:
        service.hideAdminUrls(endpoints)

        then:
        endpoints != null
        endpoints.size() == 2
        endpoints.baseUrls.size() == 2
        endpoints.baseUrls[0].publicUrl[0] == publicUrl
        endpoints.baseUrls[0].publicUrl[1] == publicUrl
        endpoints.baseUrls[1].publicUrl[0] == publicUrl
        endpoints.baseUrls[1].publicUrl[1] == publicUrl
        endpoints.baseUrls[0].internalUrl[0] == internalUrl
        endpoints.baseUrls[0].internalUrl[1] == internalUrl
        endpoints.baseUrls[1].internalUrl[0] == internalUrl
        endpoints.baseUrls[1].internalUrl[1] == internalUrl
        endpoints.baseUrls[0].adminUrl[0] == null
        endpoints.baseUrls[0].adminUrl[1] == null
        endpoints.baseUrls[1].adminUrl[0] == null
        endpoints.baseUrls[1].adminUrl[1] == null
    }

    def "revoke token returns 404 if the token belongs to a racker" () {
        given:
        allowAccess()
        def token = "token"
        def sa = new RackerScopeAccess().with {
            it.accessTokenString = token
            it.accessTokenExp = new Date().plus(1)
            it
        }

        when:
        def result = service.revokeToken(request, token, headers)

        then:
        result.build().status == 404
        1 * scopeAccessService.getScopeAccessByAccessToken(token) >> sa
    }

    def "revoke token returns 204 if the token belongs to a user" () {
        given:
        allowAccess()
        def token = "token"
        def sa = new UserScopeAccess().with {
            it.accessTokenString = token
            it.accessTokenExp = new Date().plus(1)
            it
        }

        when:
        def result = service.revokeToken(request, token, headers)

        then:
        result.build().status == 204
        1 * tokenRevocationService.revokeToken(token)
        1 * scopeAccessService.getScopeAccessByAccessToken(token) >> sa
    }

    def "remove v1Defaults" () {
        given:
        def baseUrlRef = v1Factory.createBaseUrlRef()
        def tenant = entityFactory.createTenant().with {
            it.baseUrlIds = ["1","2","3"].asList()
            it.v1Defaults = ["1","2","3"].asList()
            it
        }
        def cloudBaseUrl = entityFactory.createCloudBaseUrl().with {
            it.serviceName = "serviceName"
            it
        }

        when:
        service.replaceAddV1Default(baseUrlRef, tenant, String.valueOf(baseUrlRef.id))

        then:
        endpointService.getBaseUrlById(_) >> cloudBaseUrl
        notThrown(ConcurrentModificationException)
    }

    /**
     * It's difficult to demonstrate that the v1.1 auth service uses cached roles through an integration/api
     * test because roles are not returned in the response like they are with v2.0 auth or validate.
     *
     * The ScopeAccessService getServiceCatalogInfo method is verified through other tests to appropriate use cached
     * roles or not based on various property configuration settings.
     *
     * By showing that this method calls the method that has been verified to correctly use (or not) the cache
     * we verify this service would use the cached roles itself, as appropriate.
     */
    def "v11 auth retrieves service catalog info from DefaultScopeAccessService"() {
        given:

        // Mock Request - Auth 1.1 does nothing with this
        request = Mock(HttpServletRequest)

        // Mock URI Info - Auth 1.1 does nothing with this
        UriInfo uriInfo = Mock()

        // Mock headers and set to XML content since there isn't a JSON writer for Auth 1.1 request bodies
        headers = Mock(HttpHeaders)
        headers.getMediaType() >> MediaType.APPLICATION_XML_TYPE

        // Generate the xml request body
        UserCredentials cred = v1Factory.createUserKeyCredentials("user", "key")
        JAXBElement<UserCredentials> jaxCred = jaxbObjectFactories.rackspaceCloudV1ObjectFactory.createCredentials(cred)
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream()
        createCore11XMLWriter().writeTo(jaxCred, null, null, null, null, null, entityStream)
        String body = entityStream.toString()

        com.rackspace.idm.domain.entity.User user = entityFactory.createUser()
        authWithApiKeyCredentials.authenticate(_, _) >> new UserAuthenticationResult(user, true)

        UserScopeAccess sa = entityFactory.createUserToken()
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, sa)
        scopeAccessService.createScopeAccessForUserAuthenticationResult(_) >> authResponseTuple

        requestContextHolder.getRequestContext() >> new AuthenticationContext()
        authConverterCloudV11.toCloudv11AuthDataJaxb(_, _) >> new com.rackspacecloud.docs.auth.api.v1.AuthData()

        when:
        service.authenticate(request, uriInfo, headers,  body)

        then:
        1 * scopeAccessService.getServiceCatalogInfo(user) >> new ServiceCatalogInfo()
    }

    def "deleteUser - Delete userAdminDN on domain when deleting user-admin" () {
        given:
        allowAccess()
        mockDomainService(service)
        def user = entityFactory.createUser()

        when:
        service.deleteUser(request, user.username, headers)

        then:
        1 * userService.deleteUser(user)
        1 * userService.getUser(user.username) >> user
        1 * domainService.removeDomainUserAdminDN(user)
    }

    def createCore11XMLWriter() {
        Core11XMLWriter core11XMLWriter = new Core11XMLWriter()

        HashMap<String, String> corev11NsPrefixMap = new HashMap<String, String>()
        corev11NsPrefixMap.put("http://www.w3.org/2005/Atom", "atom")
        corev11NsPrefixMap.put("http://docs.rackspacecloud.com/auth/api/v1.1", "")
        core11XMLWriter.setNsPrefixMap(corev11NsPrefixMap)
        return core11XMLWriter
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
