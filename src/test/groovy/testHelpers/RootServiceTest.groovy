package testHelpers

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11
import com.rackspace.idm.api.converter.cloudv20.*
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.email.EmailClient
import com.rackspace.idm.api.resource.cloud.v11.Cloud11Service
import com.rackspace.idm.api.resource.cloud.v11.DefaultCloud11Service
import com.rackspace.idm.api.resource.cloud.v20.*
import com.rackspace.idm.api.resource.pagination.Paginator
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.api.security.DefaultRequestContextHolder
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.IdentityConfig.ReloadableConfig
import com.rackspace.idm.domain.config.IdentityConfig.RepositoryConfig
import com.rackspace.idm.domain.config.IdentityConfig.StaticConfig
import com.rackspace.idm.domain.dao.*
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.security.TokenFormatSelector
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator
import com.rackspace.idm.domain.service.*
import com.rackspace.idm.domain.service.impl.*
import com.rackspace.idm.exception.ExceptionHandler
import com.rackspace.idm.exception.IdmExceptionHandler
import com.rackspace.idm.multifactor.service.MultiFactorService
import com.rackspace.idm.util.AuthHeaderHelper
import com.rackspace.idm.util.CryptHelper
import com.rackspace.idm.util.RSAClient
import com.rackspace.idm.util.SamlUnmarshaller
import com.rackspace.idm.validation.*
import com.rackspace.idm.validation.entity.Constants
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
import org.junit.Rule
import org.openstack.docs.identity.api.v2.ObjectFactory
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo
/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/24/12
 * Time: 2:26 PM
 * To change this template use File | Settings | File Templates.
 */

class RootServiceTest extends Specification {

    @Shared Configuration config
    @Shared IdentityConfig identityConfig
    @Shared StaticConfig staticConfig
    @Shared ReloadableConfig reloadableConfig
    @Shared RepositoryConfig repositoryConfig
    @Shared AtomHopperClient atomHopperClient
    @Shared EmailClient emailClient
    @Shared RSAClient rsaClient
    @Shared Validator validator
    @Shared Validator20 validator20
    @Shared PrecedenceValidator precedenceValidator
    @Shared InputValidator inputValidator
    @Shared CloudGroupBuilder cloudGroupBuilder
    @Shared CloudKsGroupBuilder cloudKsGroupBuilder
    @Shared JAXBObjectFactories jaxbObjectFactories
    @Shared ObjectFactory openStackIdentityV2Factory

    // converters
    @Shared AuthConverterCloudV20 authConverter
    @Shared AuthConverterCloudV11 authConverterCloudV11
    @Shared EndpointConverterCloudV20 endpointConverter
    @Shared RoleConverterCloudV20 roleConverter
    @Shared ServiceConverterCloudV20 serviceConverter
    @Shared TenantConverterCloudV20 tenantConverter
    @Shared TokenConverterCloudV20 tokenConverter
    @Shared UserConverterCloudV20 userConverter
    @Shared UserConverterCloudV11 userConverterV11
    @Shared DomainConverterCloudV20 domainConverter
    @Shared RegionConverterCloudV20 regionConverter
    @Shared QuestionConverterCloudV20 questionConverter
    @Shared SecretQAConverterCloudV20 secretQAConverter
    @Shared ObjectConverter objectConverter
    @Shared MobilePhoneConverterCloudV20 mobilePhoneConverterCloudV20
    @Shared OTPDeviceConverterCloudV20 otpDeviceConverterCloudV20
    @Shared SamlUnmarshaller samlUnmarshaller

    //services
    @Shared ApplicationService applicationService
    @Shared DomainService domainService
    @Shared QuestionService questionService
    @Shared ScopeAccessService scopeAccessService
    @Shared PasswordComplexityService passwordComplexityService
    @Shared TenantService tenantService
    @Shared TokenService tokenService
    @Shared SecretQAService secretQAService
    @Shared EndpointService endpointService
    @Shared AuthorizationService authorizationService
    @Shared UserService userService

    /**
     * New classes should use the IdmExceptionHandler interface rather than the concrete ExceptionHandler class
     */
    @Deprecated
    @Shared ExceptionHandler exceptionHandler

    @Shared IdmExceptionHandler idmExceptionHandler
    @Shared AuthenticationService authenticationService
    @Shared GroupService groupService
    @Shared CloudRegionService cloudRegionService
    @Shared DefaultAuthorizationService defaultAuthorizationService
    @Shared DefaultEndpointService defaultEndpointService
    @Shared DefaultTenantService defaultTenantService
    @Shared DefaultGroupService defaultGroupService
    @Shared DefaultUserService defaultUserService
    @Shared DefaultTokenService defaultTokenService
    @Shared DefaultSecretQAService defaultSecretQAService
    @Shared DefaultScopeAccessService defaultScopeAccessService
    @Shared DefaultApplicationService defaultApplicationService
    @Shared DefaultQuestionService defaultQuestionService
    @Shared DefaultPasswordComplexityService defaultPasswordComplexityService
    @Shared DefaultDomainService defaultDomainService
    @Shared DefaultCloudRegionService defaultCloudRegionService
    @Shared DefaultAuthenticationService defaultAuthenticationService
    @Shared Cloud11Service cloud11Service
    @Shared DefaultCloud11Service defaultCloud11Service
    @Shared DefaultRegionService defaultRegionService
    @Shared DefaultCloud20Service defaultCloud20Service
    @Shared PropertiesService propertiesService
    @Shared CryptHelper cryptHelper
    @Shared DefaultFederatedIdentityService defaultFederatedIdentityService;
    @Shared IdentityUserService  identityUserService
    @Shared MultiFactorCloud20Service multiFactorCloud20Service;
    @Shared MultiFactorService multiFactorService;
    @Shared RoleService roleService
    @Shared DefaultRequestContextHolder requestContextHolder
    @Shared TokenRevocationService tokenRevocationService
    @Shared CacheableKeyCzarCrypterLocator cacheableKeyCzarCrypterLocator
    @Shared DefaultAuthenticateResponseService authenticateResponseService
    @Shared AETokenService aeTokenService
    @Shared AETokenRevocationService aeTokenRevocationService

    // Dao's
    @Shared ApplicationDao applicationDao
    @Shared ScopeAccessDao scopeAccessDao
    @Shared UserDao userDao
    @Shared RackerDao rackerDao
    @Shared TenantDao tenantDao
    @Shared EndpointDao endpointDao
    @Shared TenantRoleDao tenantRoleDao
    @Shared ApplicationRoleDao applicationRoleDao
    @Shared AuthDao authDao
    @Shared DomainDao domainDao
    @Shared MobilePhoneDao mobilePhoneDao

    @Shared HttpHeaders headers
    @Shared AuthHeaderHelper authHeaderHelper
    @Shared Paginator userPaginator
    @Shared Paginator endUserPaginator
    @Shared Paginator domainPaginator
    @Shared Paginator applicationRolePaginator
    @Shared Paginator tenantRolePaginator
    @Shared Paginator clientPaginator
    @Shared AuthWithToken authWithToken
    @Shared AuthWithPasswordCredentials authWithPasswordCredentials
    @Shared AuthWithApiKeyCredentials authWithApiKeyCredentials
    @Shared TokenFormatSelector tokenFormatSelector

    @Shared RequestContext requestContext
    @Shared AuthenticationContext authenticationContext
    @Shared SecurityContext securityContext

    @Shared def jaxbMock

    @Shared def authToken = "token"
    @Shared def entityFactory = new EntityFactory()
    @Shared def entityFactoryForValidation = new EntityFactoryForValidation()
    @Shared def v1Factory = new V1Factory()
    @Shared def v2Factory = new V2Factory()

    @Shared def defaultExpirationHours = 24
    @Shared def defaultRackerExpirationHours = 12
    @Shared def defaultCloudAuthExpirationHours = 24
    @Shared def defaultCloudAuthRackerExpirationHours = 12
    @Shared def defaultRefreshHours = 6
    @Shared def defaultImpersonationHours = 1

    @Shared def defaultExpirationSeconds = 3600 * defaultExpirationHours
    @Shared def defaultRackerExpirationSeconds = 3600 * defaultRackerExpirationHours
    @Shared def defaultRefreshSeconds = 3600 * defaultRefreshHours
    @Shared def defaultImpersonationExpirationSeconds = 3600 * defaultImpersonationHours
    @Shared def defaultCloudAuthExpirationSeconds = 3600 * defaultCloudAuthExpirationHours
    @Shared def defaultCloudAuthRackerExpirationSeconds = 3600 * defaultCloudAuthRackerExpirationHours

    /*
        Mock Converters
    */
    def mockAuthConverterCloudV20(service) {
        authConverter = Mock()
        authConverter.toAuthenticationResponse(_, _, _, _) >> v2Factory.createAuthenticateResponse()
        authConverter.toImpersonationResponse(_) >> v1Factory.createImpersonationResponse()
        service.authConverterCloudV20 = authConverter
    }

    def mockAuthConverterCloudV11(service) {
        authConverterCloudV11 = Mock()
        service.authConverterCloudV11 = authConverterCloudV11
    }


    def mockEndpointConverter(service) {
        endpointConverter = Mock()
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createCloudBaseUrl()
        endpointConverter.toEndpoint(_) >> v2Factory.createEndpoint()
        endpointConverter.toEndpointList(_) >> v2Factory.createEndpointList()
        endpointConverter.toEndpointListFromBaseUrls(_) >> v2Factory.createEndpointList()
        endpointConverter.toEndpointTemplate(_) >> v1Factory.createEndpointTemplate()
        endpointConverter.toEndpointTemplateList(_) >> v1Factory.createEndpointTemplateList()
        endpointConverter.toServiceCatalog(_) >> v2Factory.createServiceCatalog()
        service.endpointConverterCloudV20 = endpointConverter
    }

    def mockRoleConverter(service) {
        roleConverter = Mock()
        roleConverter.toRole(_) >> v2Factory.createRole()
        roleConverter.toRoleFromClientRole(_) >> v2Factory.createRole()
        roleConverter.toRoleListFromClientRoles(_) >> v2Factory.createRoleList()
        roleConverter.toRoleListFromClientRoles(_) >> v2Factory.createRoleList()

        roleConverter.fromRole(_, _) >> entityFactory.createClientRole()

        service.roleConverterCloudV20 = roleConverter
    }

    def mockServiceConverter(service) {
        serviceConverter = Mock()
        serviceConverter.toService(_) >> v1Factory.createService()
        serviceConverter.toServiceList(_) >> v1Factory.createServiceList()
        service.serviceConverterCloudV20 = serviceConverter
    }

    def mockTenantConverter(service) {
        tenantConverter = Mock()
        tenantConverter.toTenant(_) >> v2Factory.createTenant()
        tenantConverter.fromTenant(_) >> entityFactory.createTenant()
        service.tenantConverterCloudV20 = tenantConverter
    }

    def mockTokenConverter(service) {
        tokenConverter = Mock()
        tokenConverter.toToken(_, _) >> v2Factory.createToken()
        service.tokenConverterCloudV20 = tokenConverter
    }

    def mockAuthResponseService(service) {
        authenticateResponseService = Mock()
        service.authenticateResponseService = authenticateResponseService
    }

    def mockSamlUnmarshaller(service) {
        samlUnmarshaller = Mock()
        service.samlUnmarshaller = samlUnmarshaller
    }

    def mockUserConverter(service) {
        userConverter = Mock()
        userConverter.toUser(_) >> v2Factory.createUser()
        userConverter.fromUser(_) >> entityFactory.createUser()
        userConverter.toRackerForAuthenticateResponse(_ as Racker, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toRackerForAuthenticateResponse(_ as User, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toUserForCreate(_) >> v1Factory.createUserForCreate()
        userConverter.toUserList(_) >> v2Factory.createUserList()
        service.userConverterCloudV20 = userConverter
    }

    def mockDomainConverter(service) {
        domainConverter = Mock()
        domainConverter.toDomain(_) >> v1Factory.createDomain()
        domainConverter.fromDomain(_) >> entityFactory.createDomain()
        service.domainConverterCloudV20 = domainConverter
    }

    def mockRegionConverter(service) {
        regionConverter = Mock()
        regionConverter.fromRegion(_) >> entityFactory.createRegion()
        regionConverter.toRegion(_) >> jaxbMock
        regionConverter.toRegions(_) >> jaxbMock
        service.regionConverterCloudV20 = regionConverter
    }

    def mockQuestionConverter(service) {
        questionConverter = Mock()
        questionConverter.fromQuestion(_) >> entityFactory.createQuestion()
        questionConverter.toQuestion(_) >> jaxbMock
        questionConverter.toQuestions(_) >> jaxbMock
        service.questionConverter = questionConverter
    }

    def mockSecretQAConverter(service) {
        secretQAConverter = Mock()
        secretQAConverter.fromSecretQA(_) >> entityFactory.createSecretQA()
        secretQAConverter.toSecretQA(_) >> jaxbMock
        secretQAConverter.toSecretQAs(_) >> jaxbMock
        service.secretQAConverterCloudV20 = secretQAConverter
    }

    def mockUserConverter11(service) {
        userConverterV11 = Mock()
        userConverterV11.fromUser(_) >> entityFactory.createUser()
        userConverterV11.toCloudV11User(_) >> v1Factory.createUser()
        userConverterV11.openstackToCloudV11User(_, _) >> v1Factory.createUser()
        userConverterV11.toCloudV11User(_, _) >> entityFactory.createUser()

        service.userConverterCloudV11 = userConverterV11
    }

    def mockRolesConverter(service) {
        rolesConverter = Mock()
        rolesConverter.toRoleJaxbFromClientRole(_) >> jaxbMock
        rolesConverter.toRoleJaxbFromTenantRole(_) >> jaxbMock
        rolesConverter.toRoleJaxbFromRoleString(_) >> jaxbMock
        rolesConverter.toClientRole(_) >> entityFactory.createClientRole()
        service.rolesConverter = rolesConverter
    }

    /*
        Mock Services
    */

    def mockApplicationService(service) {
        applicationService = Mock()
        service.applicationService = applicationService
    }

    def mockDomainService(service) {
        domainService = Mock()
        service.domainService = domainService
    }

    def mockCapabilityService(service) {
        capabilityService = Mock()
        service.capabilityService = capabilityService
    }

    def mockQuestionService(service) {
        questionService = Mock()
        service.questionService = questionService
    }

    def mockScopeAccessService(service) {
        scopeAccessService = Mock()
        service.scopeAccessService = scopeAccessService
    }

    def mockPasswordComplexityService(service) {
        passwordComplexityService = Mock()
        service.passwordComplexityService = passwordComplexityService
    }

    def mockTenantService(service) {
        tenantService = Mock()
        service.tenantService = tenantService
    }

    def mockFederatedIdentityService(service) {
        defaultFederatedIdentityService = Mock()
        service.federatedIdentityService = defaultFederatedIdentityService
    }

    def mockIdentityUserService(service) {
        identityUserService = Mock()
        service.identityUserService = identityUserService
    }

    def mockMultiFactorCloud20Service(service) {
        multiFactorCloud20Service = Mock(MultiFactorCloud20Service)
        service.multiFactorCloud20Service = multiFactorCloud20Service
    }

    def mockTokenService(service) {
        tokenService = Mock()
        service.tokenService = tokenService
    }

    def mockSecretQAService(service) {
        secretQAService = Mock()
        service.secretQAService = secretQAService
    }

    def mockEndpointService(service) {
        endpointService = Mock()
        service.endpointService = endpointService
    }

    def mockAuthorizationService(service) {
        authorizationService = Mock()
        service.authorizationService = authorizationService
    }

    def mockCacheableKeyCzarCrypterLocator(service) {
        cacheableKeyCzarCrypterLocator = Mock()
        service.cacheableKeyCzarCrypterLocator = cacheableKeyCzarCrypterLocator
    }

    def mockUserService(service) {
        userService = Mock()
        service.userService = userService
    }

    def mockIdentityConfig(service) {
        identityConfig = Mock()
        staticConfig = Mock()
        reloadableConfig = Mock()
        repositoryConfig = Mock()
        identityConfig.getStaticConfig() >> staticConfig
        identityConfig.getReloadableConfig() >> reloadableConfig
        identityConfig.getRepositoryConfig() >> repositoryConfig
        service.identityConfig = identityConfig
    }
    def mockMultiFactorService(service) {
        multiFactorService = Mock()
        service.multiFactorService = multiFactorService
    }

    def mockExceptionHandler(service){
        exceptionHandler = Mock()
        service.exceptionHandler = exceptionHandler
    }

    def mockIdmExceptionHandler(service){
        idmExceptionHandler = Mock()
        service.idmExceptionHandler = idmExceptionHandler
    }

    def mockPhoneCoverterCloudV20(service) {
        mobilePhoneConverterCloudV20 = Mock()
        service.mobilePhoneConverterCloudV20 = mobilePhoneConverterCloudV20
    }

    def mockOTPDeviceConverterCloudV20(service) {
        otpDeviceConverterCloudV20 = Mock()
        service.otpDeviceConverterCloudV20 = otpDeviceConverterCloudV20
    }

    def mockAuthenticationService(service) {
        authenticationService = Mock()
        service.authenticationService = authenticationService
    }

    def mockGroupService(service) {
        groupService = Mock()
        service.groupService = groupService
    }

    def mockCloudRegionService(service) {
        cloudRegionService = Mock()
        service.cloudRegionService = cloudRegionService
    }

    def mockDefaultAuthorizationService(service) {
        defaultAuthorizationService = Mock()
        service.defaultAuthorizationService = defaultAuthorizationService
    }

    def mockDefaultEndpointService(service) {
        defaultEndpointService = Mock()
        service.defaultEndpointService = defaultEndpointService
    }

    def mockDefaultTenantService(service) {
        defaultTenantService = Mock()
        service.defaultTenantService = defaultTenantService
    }

    def mockPropertiesService(service){
        propertiesService = Mock()
        service.propertiesService = propertiesService
    }

    def mockCryptHelper(service) {
        cryptHelper = Mock()
        service.cryptHelper = cryptHelper
    }

    def mockDefaultGroupService(service) {
        defaultGroupService = Mock()
        service.defaultGroupService = defaultGroupService
    }

    def mockDefaultUserService(service) {
        defaultUserService = Mock()
        service.defaultUserService = defaultUserService
    }

    def mockDefaultTokenService(service) {
        defaultTokenService = Mock()
        service.defaultTokenService = defaultTokenService
    }

    def mockAeTokenService(service) {
        aeTokenService = Mock()
        service.aeTokenService = aeTokenService
    }

    def mockAeTokenRevocationService(service) {
        aeTokenRevocationService = Mock()
        service.aeTokenRevocationService = aeTokenRevocationService
    }

    def mockDefaultSecretQAService(service) {
        defaultSecretQAService = Mock()
        service.defaultSecretQAService = defaultSecretQAService
    }

    def mockDefaultScopeAccessService(service) {
        defaultScopeAccessService = Mock()
        service.defaultScopeAccessService = defaultScopeAccessService
    }

    def mockDefaultApplicationService(service) {
        defaultApplicationService = Mock()
        service.defaultApplicationService = defaultApplicationService
    }

    def mockDefaultQuestionService(service) {
        defaultQuestionService = Mock()
        service.defaultQuestionService = defaultQuestionService
    }

    def mockDefaultPasswordComplexityService(service) {
        defaultPasswordComplexityService = Mock()
        service.defaultPasswordComplexityService = defaultPasswordComplexityService
    }

    def mockDefaultDomainService(service) {
        defaultDomainService = Mock()
        service.defaultDomainService = defaultDomainService
    }

    def mockDefaultCloudRegionService(service) {
        defaultCloudRegionService = Mock()
        service.defaultCloudRegionService = defaultCloudRegionService
    }

    def mockDefaultAuthenticationService(service) {
        defaultAuthenticationService = Mock()
        service.defaultAuthenticationService = defaultAuthenticationService
    }

    def mockCloud11Service(service) {
        cloud11Service = Mock()
        service.cloud11Service = cloud11Service
    }

    def mockDefaultCloud11Service(service) {
        defaultCloud11Service = Mock()
        service.defaultCloud11Service = defaultCloud11Service
    }

    def mockDefaultRegionService(service) {
        defaultRegionService = Mock()
        service.defaultRegionService = defaultRegionService
    }

    def mockDefaultCloud20Service(service) {
        defaultCloud20Service = Mock()
        service.defaultCloud20Service = defaultCloud20Service
    }

    def mockCloud20Service(service) {
        defaultCloud20Service = Mock()
        service.cloud20Service = defaultCloud20Service
    }

    def mockRoleService(service) {
        roleService = Mock()
        service.roleService = roleService
    }

    def mockRequestContextHolder(service) {
        requestContextHolder = Mock()
        service.requestContextHolder = requestContextHolder
        requestContext = Mock(RequestContext)
        securityContext = Mock(SecurityContext)
        requestContextHolder.getRequestContext() >> requestContext
        requestContext.getSecurityContext() >> securityContext
        authenticationContext = Mock(AuthenticationContext)
        requestContextHolder.getAuthenticationContext() >> authenticationContext
    }

    def mockTokenRevocationService(service) {
        tokenRevocationService = Mock()
        service.tokenRevocationService = tokenRevocationService
    }

    /*
        Mock Dao
    */

    def mockScopeAccessDao(service) {
        scopeAccessDao = Mock()
        service.scopeAccessDao = scopeAccessDao
    }

    def mockUserDao(service) {
        userDao = Mock()
        service.userDao = userDao
    }

    def mockRackerDao(service) {
        rackerDao = Mock()
        service.rackerDao = rackerDao
    }

    def mockTenantDao(service) {
        tenantDao = Mock()
        service.tenantDao = tenantDao
    }

    def mockEndpointDao(service) {
        endpointDao = Mock()
        service.endpointDao = endpointDao
    }

    def mockApplicationDao(service) {
        applicationDao = Mock()
        service.applicationDao = applicationDao
    }

    def mockTenantRoleDao(service) {
        tenantRoleDao = Mock()
        service.tenantRoleDao = tenantRoleDao
    }

    def mockApplicationRoleDao(service) {
        applicationRoleDao = Mock()
        service.applicationRoleDao = applicationRoleDao
    }

    def mockAuthDao(service) {
        authDao = Mock()
        service.authDao = authDao
    }

    def mockDomainDao(service) {
        domainDao = Mock()
        service.domainDao = domainDao
    }

    def mockMobilePhoneRepository(service) {
        mobilePhoneDao = Mock()
        service.mobilePhoneDao = mobilePhoneDao
    }

    /*
        Mock Builders
    */
    def mockCloudGroupBuilder(service) {
        cloudGroupBuilder = Mock()
        service.cloudGroupBuilder = cloudGroupBuilder
    }

    def mockCloudKsGroupBuilder(service) {
        cloudKsGroupBuilder = Mock()
        service.cloudKsGroupBuilder = cloudKsGroupBuilder
    }

    /*
        Mock Validators
    */
    def mockValidator(service) {
        validator = Mock()
        service.validator = validator
    }

    def mockValidator20(service) {
        validator20 = Mock()
        service.validator20 = validator20
    }

    def mockPrecedenceValidator(service) {
        precedenceValidator = Mock()
        service.precedenceValidator = precedenceValidator
    }

    def mockInputValidator(service) {
        inputValidator = Mock()
        service.inputValidator = inputValidator
    }

    def mockObjectConverter(service) {
        objectConverter = Mock()
        service.objectConverter = objectConverter
    }

    /*
        Paginator Mocks
    */

    def mockUserPaginator(service) {
        userPaginator = Mock()
        service.userPaginator = userPaginator
    }

    def mockEndUserPaginator(service) {
        endUserPaginator = Mock()
        service.endUserPaginator = endUserPaginator
    }

    def mockDomainPaginator(service) {
        domainPaginator = Mock()
        service.domainPaginator = domainPaginator
    }

    def mockApplicationRolePaginator(service) {
        applicationRolePaginator = Mock()
        service.applicationRolePaginator = applicationRolePaginator
    }

    def mockTenantRolePaginator(service) {
        tenantRolePaginator = Mock()
        service.tenantRolePaginator = tenantRolePaginator
    }

    def mockClientPaginator(service) {
        clientPaginator = Mock()
        service.clientPaginator = clientPaginator
    }

    /*
        misc. mocks
    */

    def mockAtomHopperClient(service) {
        atomHopperClient = Mock()
        service.atomHopperClient = atomHopperClient
    }

    def mockEmailClient(service) {
        emailClient = Mock()
        service.emailClient = emailClient
    }

    def mockRSAClient(service) {
        rsaClient = Mock()
        service.rsaClient = rsaClient
    }

    def mockConfiguration(service) {
        config = Mock()
        service.config = config
    }

    def mockAuthHeaderHelper(service) {
        authHeaderHelper = Mock()
        service.authHeaderHelper = authHeaderHelper
    }

    def mockAuthWithToken(service) {
        authWithToken = Mock()
        service.authWithToken = authWithToken
    }

    def mockAuthWithPasswordCredentials(service) {
        authWithPasswordCredentials = Mock()
        service.authWithPasswordCredentials = authWithPasswordCredentials
    }

    def mockAuthWithApiKeyCredentials(service) {
        authWithApiKeyCredentials = Mock()
        service.authWithApiKeyCredentials = authWithApiKeyCredentials 
    }

    def mockJAXBObjectFactories(service) {
        jaxbObjectFactories = Mock()
        openStackIdentityV2Factory = Mock()
        jaxbObjectFactories.getOpenStackIdentityV2Factory() >> openStackIdentityV2Factory
        service.jaxbObjectFactories = jaxbObjectFactories
    }

    def mockTokenFormatSelector(service) {
        tokenFormatSelector = Mock()
        service.tokenFormatSelector = tokenFormatSelector
    }

    /*
        Miscellaneous methods
     */
    
    def uriInfo() {
        return uriInfo("http://absolute.path/to/resource")
    }

    def uriInfo(String absolutePath) {
        def builderMock = Mock(UriBuilder)
        def uriInfo = Mock(UriInfo)

        builderMock.path(_ as String) >> { String arg1 ->
            absolutePath = absolutePath + "/" + arg1
            return builderMock
        }
        builderMock.path(null) >> builderMock
        builderMock.build() >> {
            try {
                return new URI(absolutePath)
            } catch (Exception ex) {
                return new URI("http://absolute.path/to/resource")
            }
        }
        uriInfo.getRequestUriBuilder() >> builderMock
        uriInfo.getAbsolutePath() >> new URI(absolutePath)

        return uriInfo
    }

    def createScopeAccess() {
        return createScopeAccess("tokenString", new DateTime().plusHours(1).toDate())
    }

    def createScopeAccess(String tokenString, Date expiration) {
        tokenString = tokenString ? tokenString : "tokenString"
        def dn = "accessToken=$tokenString,cn=TOKENS,rsId=1234"
        new ScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.uniqueId = dn
            return it
        }
    }

    def createClientScopeAccess() {
        return createClientScopeAccess("clientId", "tokenString", new DateTime().plusHours(defaultExpirationHours + 1).toDate())
    }

    def createClientScopeAccess(String clientId, String tokenString, Date expiration) {
        clientId = clientId ? clientId : "clientId"
        tokenString = tokenString ? tokenString : "tokenString"
        def dn = "accessToken=$tokenString,cn=TOKENS,clientId=$clientId"

        new ClientScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.clientId = clientId
            it.uniqueId = dn
            return it
        }
    }

    def createImpersonatedScopeAccess() {
        return createImpersonatedScopeAccess("username", "impUsername", "tokenString", "impToken", new DateTime().plusHours(defaultExpirationHours + 1).toDate())
    }

    def createImpersonatedScopeAccess(String username, String impUsername, String tokenString, String impToken, Date expiration) {
        username = username ? username : "username"
        impUsername = impUsername ? impUsername : "impersonatingUsername"
        tokenString = tokenString ? tokenString : "tokenString"
        impToken = impToken ? impToken : "impersonatedTokenString"
        def dn = "accessToken=$tokenString,cn=TOKENS,userRsId=$username,ou=users"

        new ImpersonatedScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.impersonatingToken = impToken
            it.uniqueId = dn
            return it
        }
    }

    def createRackerScopeAcccss() {
        return createRackerScopeAccess("tokenString", "rackerId", new DateTime().plusHours(defaultExpirationHours + 1).toDate())
    }

    def createRackerScopeAccess(String tokenString, String rackerId, Date expiration) {
        return createRackerScopeAccess(tokenString, rackerId, "clientId", expiration)
    }

    def createRackerScopeAccess(String tokenString, String rackerId, String clientId, Date expiration) {
        tokenString = tokenString ? tokenString : "tokenString"
        rackerId = rackerId ? rackerId : "rackerId"
        def dn = "accessToken=$tokenString,cn=TOKENS,rsId=$rackerId,ou=rackers"

        new RackerScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.rackerId = rackerId
            it.clientId = clientId
            it.uniqueId = dn
            return it
        }
    }

    def createUserScopeAccessWithAuthBy(String authBy) {
        UserScopeAccess userScopeAccess = createUserScopeAccess()
        userScopeAccess.getAuthenticatedBy().add(authBy)
        return userScopeAccess
    }

    def createUserScopeAccess() {
        return createUserScopeAccess("tokenString", "userRsId", "clientId", new DateTime().plusHours(defaultExpirationHours + 1).toDate())
    }

    def expireScopeAccess(scopeAccess) {
        scopeAccess.accessTokenExp = new DateTime().minusHours(1).toDate()
        return scopeAccess
    }

    def createUserScopeAccess(String tokenString, String userRsId, String clientId, Date expiration) {
        tokenString = tokenString ? tokenString : "tokenString"
        userRsId = userRsId ? userRsId : "userRsId"
        clientId = clientId ? clientId : "clientId"
        def dn = "acessToken=$tokenString,cn=TOKENS,rsId=$userRsId,ou=users"

        new UserScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.userRsId = userRsId
            it.clientId = clientId
            it.uniqueId = dn
            return it
        }
    }

    def createFederatedToken() {
        return createFederatedToken("tokenString", "dedicated", "username")
    }

    def createFederatedToken(String tokenString, String idpName, String username) {
        new UserScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = new DateTime().plusDays(1).toDate()
            it.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION)
            return it
        }
    }

    def allowUserAccess() {
        allowAccess(createUserScopeAccess())
    }

    def allowFederatedTokenAccess() {
        allowAccess(createFederatedToken())
    }

    def allowRackerAccess() {
        allowAccess(createRackerScopeAcccss())
    }

    def allowImpersonatedAccess() {
        allowAccess(createImpersonatedScopeAccess())
    }

    def allowAccess(scopeAccess) {
        scopeAccessService.getScopeAccessByAccessToken(_) >> scopeAccess
    }

    def m(String value){
        return value.multiply(100)
    }

    def t(String value){
        return value.multiply(Constants.MAX_TOKEN_LENGTH)
    }

    def mm(String value){
        return value.multiply(1000)
    }

    def mmm(String value){
        return value.multiply(100000)
    }
}
