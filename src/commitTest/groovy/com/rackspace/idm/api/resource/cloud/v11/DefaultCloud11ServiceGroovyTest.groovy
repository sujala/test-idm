package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.domain.config.providers.cloudv11.Core11XMLWriter
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.validation.Validator
import com.rackspacecloud.docs.auth.api.v1.User
import com.rackspacecloud.docs.auth.api.v1.UserCredentials
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled
import org.apache.http.HttpStatus
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import javax.xml.bind.JAXBElement

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
        mockIdentityConfig(service)
    }

    @Unroll
    def "GET - userByMossoId - returns 301 - domainUserAdminLookup = #domainUserAdminLookup" () {
        given:
        allowAccess()
        def mossoId = "123"
        def user = createUser("1", "someName", Integer.valueOf(mossoId), "nast")


        when:
        Response.ResponseBuilder responseBuilder = service.getUserFromMossoId(request, Integer.valueOf(mossoId),null)

        then:
        Response response = responseBuilder.build()
        response.status == 301
        response.metadata.get("Location")[0] == "/v1.1/users/someName"

        1 * reloadableConfig.isUserAdminLookUpByDomain() >> domainUserAdminLookup

        if (domainUserAdminLookup) {
            1 * userService.getUserAdminByTenantId(mossoId) >> user
            0 * userService.getUserByTenantId(mossoId)
        } else {
            0 * userService.getUserAdminByTenantId(mossoId)
            1 * userService.getUserByTenantId(mossoId) >> user
        }

        where:
        domainUserAdminLookup << [true, false]
    }

    @Unroll
    def "GET - userByNastId - returns 301 - domainUserAdminLookup = #domainUserAdminLookup" () {
        given:
        allowAccess()
        def nastId = "nast"
        def user = createUser("1", "someName", 123, nastId)


        when:
        Response.ResponseBuilder responseBuilder = service.getUserFromNastId(request, nastId,null)

        then:
        Response response = responseBuilder.build()
        response.status == 301
        response.metadata.get("Location")[0] == "/v1.1/users/someName"

        1 * reloadableConfig.isUserAdminLookUpByDomain() >> domainUserAdminLookup

        if (domainUserAdminLookup) {
            1 * userService.getUserAdminByTenantId(nastId) >> user
            0 * userService.getUserByTenantId(nastId)
        } else {
            0 * userService.getUserAdminByTenantId(nastId)
            1 * userService.getUserByTenantId(nastId) >> user
        }

        where:
        domainUserAdminLookup << [true, false]
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

    def "Verify that Impersonation scope access create date is set" () {
        given:
        def username = "username"
        def token = "token"
        def createdDate = new Date()
        def expiredDate = new Date().plus(1)
        ImpersonatedScopeAccess sa = new ImpersonatedScopeAccess().with {
            it.accessTokenString = token
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
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN) >> true
        authorizationService.authorizeCloudServiceAdmin(_) >> true
    }

}
