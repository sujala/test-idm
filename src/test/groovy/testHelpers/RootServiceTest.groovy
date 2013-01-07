package testHelpers

import com.rackspace.idm.api.converter.cloudv20.AuthConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.CapabilityConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.DomainsConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.EndpointConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.PoliciesConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.PolicyConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.QuestionConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.RegionConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.RoleConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.SecretQAConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.ServiceConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.TenantConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.TokenConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20
import com.rackspace.idm.api.resource.cloud.Validator
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.migration.CloudMigrationService
import com.rackspace.idm.api.resource.cloud.v11.Cloud11Service
import com.rackspace.idm.api.resource.cloud.v11.DefaultCloud11Service
import com.rackspace.idm.api.resource.cloud.v11.DelegateCloud11Service
import com.rackspace.idm.api.resource.cloud.v20.CloudGroupBuilder
import com.rackspace.idm.api.resource.cloud.v20.CloudKsGroupBuilder
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.api.resource.cloud.v20.DefaultRegionService
import com.rackspace.idm.api.resource.cloud.v20.DelegateCloud20Service
import com.rackspace.idm.api.resource.cloud.v20.PolicyValidator
import com.rackspace.idm.api.resource.pagination.Paginator
import com.rackspace.idm.domain.entity.ClientScopeAccess
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.RackerScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ApiDocService
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthenticationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.CapabilityService
import com.rackspace.idm.domain.service.CloudRegionService
import com.rackspace.idm.domain.service.CustomerService
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.GroupService
import com.rackspace.idm.domain.service.PasswordComplexityService
import com.rackspace.idm.domain.service.PolicyService
import com.rackspace.idm.domain.service.QuestionService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.SecretQAService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.TokenService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultApiDocService
import com.rackspace.idm.domain.service.impl.DefaultApplicationService
import com.rackspace.idm.domain.service.impl.DefaultAuthenticationService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.rackspace.idm.domain.service.impl.DefaultCapabilityService
import com.rackspace.idm.domain.service.impl.DefaultCloudRegionService
import com.rackspace.idm.domain.service.impl.DefaultCustomerService
import com.rackspace.idm.domain.service.impl.DefaultDomainService
import com.rackspace.idm.domain.service.impl.DefaultEndpointService
import com.rackspace.idm.domain.service.impl.DefaultGroupService
import com.rackspace.idm.domain.service.impl.DefaultPasswordComplexityService
import com.rackspace.idm.domain.service.impl.DefaultPolicyService
import com.rackspace.idm.domain.service.impl.DefaultQuestionService
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultSecretQAService
import com.rackspace.idm.domain.service.impl.DefaultTenantService
import com.rackspace.idm.domain.service.impl.DefaultTokenService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.exception.ExceptionHandler
import com.rackspace.idm.validation.PrecedenceValidator
import com.rackspace.idm.validation.Validator20
import com.unboundid.ldap.sdk.ReadOnlyEntry
import org.apache.commons.configuration.Configuration
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
    @Shared AtomHopperClient atomHopperClient
    @Shared Validator validator
    @Shared Validator20 validator20
    @Shared PrecedenceValidator precedenceValidator
    @Shared PolicyValidator policyValidator
    @Shared CloudGroupBuilder cloudGroupBuilder
    @Shared CloudKsGroupBuilder cloudKsGroupBuilder

    // converters
    @Shared AuthConverterCloudV20 authConverter
    @Shared EndpointConverterCloudV20 endpointConverter
    @Shared RoleConverterCloudV20 roleConverter
    @Shared ServiceConverterCloudV20 serviceConverter
    @Shared TenantConverterCloudV20 tenantConverter
    @Shared TokenConverterCloudV20 tokenConverter
    @Shared UserConverterCloudV20 userConverter
    @Shared DomainConverterCloudV20 domainConverter
    @Shared DomainsConverterCloudV20 domainsConverter
    @Shared PolicyConverterCloudV20 policyConverter
    @Shared PoliciesConverterCloudV20 policiesConverter
    @Shared CapabilityConverterCloudV20 capabilityConverter
    @Shared RegionConverterCloudV20 regionConverter
    @Shared QuestionConverterCloudV20 questionConverter
    @Shared SecretQAConverterCloudV20 secretQAConverter

    //services
    @Shared ApplicationService applicationService
    @Shared ApiDocService apiDocService
    @Shared DomainService domainService
    @Shared CustomerService customerService
    @Shared CapabilityService capabilityService
    @Shared QuestionService questionService
    @Shared ScopeAccessService scopeAccessService
    @Shared PasswordComplexityService passwordComplexityService
    @Shared TenantService tenantService
    @Shared TokenService tokenService
    @Shared SecretQAService secretQAService
    @Shared EndpointService endpointService
    @Shared AuthorizationService authorizationService
    @Shared UserService userService
    @Shared AuthenticationService authenticationService
    @Shared GroupService groupService
    @Shared CloudRegionService cloudRegionService
    @Shared DefaultCapabilityService defaultCapabilityService
    @Shared DefaultAuthorizationService defaultAuthorizationService
    @Shared DefaultEndpointService defaultEndpointService
    @Shared DefaultApiDocService defaultApiDocService
    @Shared DefaultTenantService defaultTenantService
    @Shared DefaultGroupService defaultGroupService
    @Shared DefaultUserService defaultUserService
    @Shared DefaultCustomerService defaultCustomerService
    @Shared DefaultTokenService defaultTokenService
    @Shared DefaultSecretQAService defaultSecretQAService
    @Shared DefaultScopeAccessService defaultScopeAccessService
    @Shared DefaultApplicationService defaultApplicationService
    @Shared DefaultQuestionService defaultQuestionService
    @Shared DefaultPasswordComplexityService defaultPasswordComplexityService
    @Shared DefaultDomainService defaultDomainService
    @Shared DefaultCloudRegionService defaultCloudRegionService
    @Shared DefaultAuthenticationService defaultAuthenticationService
    @Shared DefaultPolicyService defaultPolicyService
    @Shared PolicyService policyService
    @Shared Cloud11Service cloud11Service
    @Shared DelegateCloud11Service delegateCloud11Service
    @Shared DefaultCloud11Service defaultCloud11Service
    @Shared CloudMigrationService cloudMigrationService
    @Shared DefaultRegionService defaultRegionService
    @Shared DefaultCloud20Service defaultCloud20Service
    @Shared DelegateCloud20Service delegateCloud20Service

    @Shared HttpHeaders headers
    @Shared Paginator userPaginator
    @Shared Paginator domainPaginator
    @Shared Paginator applicationRolePaginator
    @Shared Paginator tenantRolePaginator

    @Shared def jaxbMock

    @Shared def authToken = "token"

    /*
        Mock Converters
    */
    def mockAuthConverter(rootService) {
        authConverter = Mock()
        authConverter.toAuthenticationResponse(_, _, _, _) >> v2Factory.createAuthenticateResponse()
        authConverter.toImpersonationResponse(_) >> v1Factory.createImpersonationResponse()
        rootService.authConverterCloudV20 = authConverter
    }

    def mockEndpointConverter(rootService) {
        endpointConverter = Mock()
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createCloudBaseUrl()
        endpointConverter.toEndpoint(_) >> v2Factory.createEndpoint()
        endpointConverter.toEndpointList(_) >> v2Factory.createEndpointList()
        endpointConverter.toEndpointListFromBaseUrls(_) >> v2Factory.createEndpointList()
        endpointConverter.toEndpointTemplate(_) >> v1Factory.createEndpointTemplate()
        endpointConverter.toEndpointTemplateList(_) >> v1Factory.createEndpointTemplateList()
        endpointConverter.toServiceCatalog(_) >> v2Factory.createServiceCatalog()
        rootService.endpointConverterCloudV20 = endpointConverter
    }

    def mockRoleConverter(rootService) {
        roleConverter = Mock()
        roleConverter.toRole(_) >> v2Factory.createRole()
        roleConverter.toRoleFromClientRole(_) >> v2Factory.createRole()
        roleConverter.toRoleListFromClientRoles(_) >> v2Factory.createRoleList()
        roleConverter.toRoleListFromClientRoles(_) >> v2Factory.createRoleList()
        rootService.roleConverterCloudV20 = roleConverter
    }

    def mockServiceConverter(rootService) {
        serviceConverter = Mock()
        serviceConverter.toService(_) >> v1Factory.createService()
        serviceConverter.toServiceList(_) >> v1Factory.createServiceList()
        rootService.serviceConverterCloudV20 = serviceConverter
    }

    def mockTenantConverter(rootService) {
        tenantConverter = Mock()
        tenantConverter.toTenant(_) >> v2Factory.createTenant()
        tenantConverter.toTenantDO(_) >> entityFactory.createTenant()
        tenantConverter.toTenantList(_) >> v2Factory.createTenantList()
        rootService.tenantConverterCloudV20 = tenantConverter
    }

    def mockTokenConverter(rootService) {
        tokenConverter = Mock()
        tokenConverter.toToken(_) >> v2Factory.createToken()
        tokenConverter.toToken(_, _) >> v2Factory.createToken()
        rootService.tokenConverterCloudV20 = tokenConverter
    }

    def mockUserConverter(rootService) {
        userConverter = Mock()
        userConverter.toUser(_) >> v2Factory.createUser()
        userConverter.toUserDO(_) >> entityFactory.createUser()
        userConverter.toUserForAuthenticateResponse(_ as Racker, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toUserForAuthenticateResponse(_ as User, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toUserForCreate(_) >> v1Factory.createUserForCreate()
        userConverter.toUserList(_) >> v2Factory.createUserList()
        rootService.userConverterCloudV20 = userConverter
    }

    def mockDomainConverter(rootService) {
        domainConverter = Mock()
        domainConverter.toDomain(_) >> v1Factory.createDomain()
        domainConverter.toDomainDO(_) >> entityFactory.createDomain()
        rootService.domainConverterCloudV20 = domainConverter
    }

    def mockDomainsConverter(rootService) {
        domainsConverter = Mock()
        domainsConverter.toDomains(_) >> v1Factory.createDomains()
        domainsConverter.toDomainsDO(_) >> entityFactory.createDomains()
        rootService.domainsConverterCloudV20 = domainsConverter
    }

    def mockPolicyConverter(rootService) {
        policyConverter = Mock()
        policyConverter.toPolicy(_) >> v1Factory.createPolicy()
        policyConverter.toPolicyDO(_) >> entityFactory.createPolicy()
        policyConverter.toPolicyForPolicies(_) >> v1Factory.createPolicy()
        rootService.policyConverterCloudV20 = policyConverter
    }

    def mockPoliciesConverter(rootService) {
        policiesConverter = Mock()
        policiesConverter.toPolicies(_) >> v1Factory.createPolicies()
        policiesConverter.toPoliciesDO(_) >> entityFactory.createPolicies()
        rootService.policiesConverterCloudV20 = policiesConverter
    }

    def mockCapabilityConverter(rootService) {
        capabilityConverter = Mock()
        capabilityConverter.fromCapability(_) >> entityFactory.createCapability()
        capabilityConverter.fromCapabilities(_) >> entityFactory.createCapabilities()
        capabilityConverter.toCapability(_) >> jaxbMock
        capabilityConverter.toCapabilities(_) >> jaxbMock
        capabilityConverter.toServiceApis(_) >> jaxbMock
        rootService.capabilityConverterCloudV20 = capabilityConverter
    }


    def mockRegionConverter(rootService) {
        regionConverter = Mock()
        regionConverter.fromRegion(_) >> entityFactory.createRegion()
        regionConverter.toRegion(_) >> jaxbMock
        regionConverter.toRegions(_) >> jaxbMock
        rootService.regionConverterCloudV20 = regionConverter
    }

    def mockQuestionConverter(rootService) {
        questionConverter = Mock()
        questionConverter.fromQuestion(_) >> entityFactory.createQuestion()
        questionConverter.toQuestion(_) >> jaxbMock
        questionConverter.toQuestions(_) >> jaxbMock
        rootService.questionConverter = questionConverter
    }

    def mockSecretQAConverter(rootService) {
        secretQAConverter = Mock()
        secretQAConverter.fromSecretQA(_) >> entityFactory.createSecretQA()
        secretQAConverter.toSecretQA(_) >> jaxbMock
        secretQAConverter.toSecretQAs(_) >> jaxbMock
        rootService.secretQAConverterCloudV20 = secretQAConverter
    }

    /*
        Mock Services
    */

    def mockApplicationService(rootService) {
        applicationService = Mock()
        rootService.applicationService = applicationService
    }

    def mockApiDocService(rootService) {
        apiDocService = Mock()
        rootService.apiDocService = apiDocService
    }

    def mockDomainService(rootService) {
        domainService = Mock()
        rootService.domainService = domainService
    }

    def mockCustomerService(rootService) {
        customerService = Mock()
        rootService.customerService = customerService
    }

    def mockCapabilityService(rootService) {
        capabilityService = Mock()
        rootService.capabilityService = capabilityService
    }

    def mockQuestionService(rootService) {
        questionService = Mock()
        rootService.questionService = questionService
    }

    def mockScopeAccessService(rootService) {
        scopeAccessService = Mock()
        rootService.scopeAccessService = scopeAccessService
    }

    def mockPasswordComplexityService(rootService) {
        passwordComplexityService = Mock()
        rootService.passwordComplexityService = passwordComplexityService
    }

    def mockTenantService(rootService) {
        tenantService = Mock()
        rootService.tenantService = tenantService
    }

    def mockTokenService(rootService) {
        tokenService = Mock()
        rootService.tokenService = tokenService
    }

    def mockSecretQAService(rootService) {
        secretQAService = Mock()
        rootService.secretQAService = secretQAService
    }

    def mockEndpointService(rootService) {
        endpointService = Mock()
        rootService.endpointService = endpointService
    }

    def mockAuthorizationService(rootService) {
        authorizationService = Mock()
        rootService.authorizationService = authorizationService
    }

    def mockUserService(rootService) {
        userService = Mock()
        rootService.userService = userService
    }

    def mockAuthenticationService(rootService) {
        authenticationService = Mock()
        rootService.authenticationService = authenticationService
    }

    def mockGroupService(rootService) {
        groupService = Mock()
        rootService.groupService = groupService
    }

    def mockCloudRegionService(rootService) {
        cloudRegionService = Mock()
        rootService.cloudRegionService = cloudRegionService
    }

    def mockDefaultCapabilityService(rootService) {
        defaultCapabilityService = Mock()
        rootService.defaultCapabilityService = defaultCapabilityService
    }

    def mockDefaultAuthorizationService(rootService) {
        defaultAuthorizationService = Mock()
        rootService.defaultAuthorizationService = defaultAuthorizationService
    }

    def mockDefaultEndpointService(rootService) {
        defaultEndpointService = Mock()
        rootService.defaultEndpointService = defaultEndpointService
    }

    def mockDefaultApiDocService(rootService) {
        defaultApiDocService = Mock()
        rootService.defaultApiDocService = defaultApiDocService
    }

    def mockDefaultTenantService(rootService) {
        defaultTenantService = Mock()
        rootService.defaultTenantService = defaultTenantService
    }

    def mockDefaultGroupService(rootService) {
        defaultGroupService = Mock()
        rootService.defaultGroupService = defaultGroupService
    }

    def mockDefaultUserService(rootService) {
        defaultUserService = Mock()
        rootService.defaultUserService = defaultUserService
    }

    def mockDefaultCustomerService(rootService) {
        defaultCustomerService = Mock()
        rootService.defaultCustomerService = defaultCustomerService
    }

    def mockDefaultTokenService(rootService) {
        defaultTokenService = Mock()
        rootService.defaultTokenService = defaultTokenService
    }

    def mockDefaultSecretQAService(rootService) {
        defaultSecretQAService = Mock()
        rootService.defaultSecretQAService = defaultSecretQAService
    }

    def mockDefaultScopeAccessService(rootService) {
        defaultScopeAccessService = Mock()
        rootService.defaultScopeAccessService = defaultScopeAccessService
    }

    def mockDefaultApplicationService(rootService) {
        defaultApplicationService = Mock()
        rootService.defaultApplicationService = defaultApplicationService
    }

    def mockDefaultQuestionService(rootService) {
        defaultQuestionService = Mock()
        rootService.defaultQuestionService = defaultQuestionService
    }

    def mockDefaultPasswordComplexityService(rootService) {
        defaultPasswordComplexityService = Mock()
        rootService.defaultPasswordComplexityService = defaultPasswordComplexityService
    }

    def mockDefaultDomainService(rootService) {
        defaultDomainService = Mock()
        rootService.defaultDomainService = defaultDomainService
    }

    def mockDefaultCloudRegionService(rootService) {
        defaultCloudRegionService = Mock()
        rootService.defaultCloudRegionService = defaultCloudRegionService
    }

    def mockDefaultAuthenticationService(rootService) {
        defaultAuthenticationService = Mock()
        rootService.defaultAuthenticationService = defaultAuthenticationService
    }

    def mockDefaultPolicyService(rootService) {
        defaultPolicyService = Mock()
        rootService.defaultPolicyService = defaultPolicyService
    }

    def mockPolicyService(rootService) {
        policyService = Mock()
        rootService.policyService = policyService
    }

    def mockCloud11Service(rootService) {
        cloud11Service = Mock()
        rootService.cloud11Service = cloud11Service
    }

    def mockDelegateCloud11Service(rootService) {
        delegateCloud11Service = Mock()
        rootService.delegateCloud11Service = delegateCloud11Service
    }

    def mockDefaultCloud11Service(rootService) {
        defaultCloud11Service = Mock()
        rootService.defaultCloud11Service = defaultCloud11Service
    }

    def mockCloudMigrationService(rootService) {
        cloudMigrationService = Mock()
        rootService.cloudMigrationService = cloudMigrationService
    }

    def mockDefaultRegionService(rootService) {
        defaultRegionService = Mock()
        rootService.defaultRegionService = defaultRegionService
    }

    def mockDefaultCloud20Service(rootService) {
        defaultCloud20Service = Mock()
        rootService.defaultCloud20Service = defaultCloud20Service
    }

    def mockDelegateCloud20Service(rootService) {
        delegateCloud20Service = Mock()
        rootService.delegateCloud20Service = delegateCloud20Service
    }

    /*
        Mock Builders
    */
    def mockCloudGroupBuilder(rootService) {
        cloudGroupBuilder = Mock()
        rootService.cloudGroupBuilder = cloudGroupBuilder
    }

    def mockCloudKsGroupBuilder(rootService) {
        cloudKsGroupBuilder = Mock()
        rootService.cloudKsGroupBuilder = cloudKsGroupBuilder
    }

    /*
        Mock Validators
    */
    def mockValidator(rootService) {
        validator = Mock()
        rootService.validator = validator
    }

    def mockValidator20(rootService) {
        validator20 = Mock()
        rootService.validator20 = validator20
    }

    def mockPolicyValidator(rootService) {
        policyValidator = Mock()
        rootService.policyValidator = policyValidator
    }

    def mockPrecedenceValidator(rootService) {
        precedenceValidator = Mock()
        rootService.precedenceValidator = precedenceValidator
    }

    /*
        Paginator Mocks
    */

    def mockUserPaginator(rootService) {
        userPaginator = Mock()
        rootService.userPaginator = userPaginator
    }

    def mockDomainPaginator(rootService) {
        domainPaginator = Mock()
        rootService.domainPaginator = domainPaginator
    }

    def mockApplicationRolePaginator(rootService) {
        applicationRolePaginator = Mock()
        rootService.applicationRolePaginator = applicationRolePaginator
    }

    def mockTenantRolePaginator(rootService) {
        tenantRolePaginator = Mock()
        rootService.tenantRolePaginator = tenantRolePaginator
    }

    /*
        misc. mocks
    */

    def mockAtomHopperClient(rootService) {
        atomHopperClient = Mock()
        rootService.atomHopperClient = atomHopperClient
    }

    def mockConfiguration(rootService) {
        config = Mock()
        rootService.config = config
    }

    def uriInfo() {
        return uriInfo("http://absolute.path/to/resource")
    }

    def uriInfo(String absolutePath) {
        def absPath
        try {
            absPath = new URI(absolutePath)
        } catch (Exception ex) {
            absPath = new URI("http://absolute.path/to/resource")
        }

        def builderMock = Mock(UriBuilder)
        def uriInfo = Mock(UriInfo)

        builderMock.path(_ as String) >> builderMock
        builderMock.path(null) >> builderMock
        builderMock.build() >> absPath
        uriInfo.getRequestUriBuilder() >> builderMock

        return uriInfo
    }

    def createClientScopeAccess() {
        return createClientScopeAccess("clientId", "tokenString", false, false)
    }

    def createClientScopeAccess(String clientId, String tokenString, boolean isExpired, boolean refresh) {
        def scopeAccess = Mock(ClientScopeAccess)
        clientId = clientId ? clientId : "clientId"
        tokenString = tokenString ? tokenString : "tokenString"
        def dn = "accessToken$tokenString,cn=TOKENS,clientId=$clientId"
        def entry = createEntry(dn)

        scopeAccess.getAccessTokenString() >> tokenString
        scopeAccess.getClientId() >> clientId
        scopeAccess.getUniqueId() >> dn
        scopeAccess.getLDAPEntry() >> entry
    }

    def createRackerScopeAcccss() {
        return createRackerScopeAccess("tokenString", "rackerId", false, false)
    }

    def createRackerScopeAccess(String tokenString, String rackerId, boolean isExpired, boolean refresh) {
        def scopeAccess = Mock(RackerScopeAccess)
        tokenString = tokenString ? tokenString : "tokenString"
        rackerId = rackerId ? rackerId : "rackerId"
        def dn = "accessToken=$tokenString,cn=TOKENS,rsI$rackerId,ou=rackers"
        def entry = createEntry(dn)

        scopeAccess.getAccessTokenString() >> tokenString
        scopeAccess.getRackerId() >> rackerId
        scopeAccess.getUniqueId() >> dn
        scopeAccess.getLDAPEntry() >> entry
        scopeAccess.isAccessTokenExpired(_) >> isExpired
        scopeAccess.isAccessTokenWithinRefreshWindow(_) >> refresh
        return scopeAccess
    }

    def createUserScopeAccess() {
        return createUserScopeAccess("tokenString", "userRsId", "clientId", false, false)
    }

    def createUserScopeAccess(String tokenString, String userRsId, String clientId, boolean isExpired, boolean refresh) {
        def scopeAccess = Mock(UserScopeAccess)
        tokenString = tokenString ? tokenString : "tokenString"
        userRsId = userRsId ? userRsId : "userRsId"
        clientId = clientId ? clientId : "clientId"
        def dn = "acessToken=$tokenString,cn=TOKENS,rsId=$userRsId,ou=users"
        def entry = createEntry(dn)

        scopeAccess.getAccessTokenString() >> tokenString
        scopeAccess.getUserRsId() >> userRsId
        scopeAccess.getClientId() >> clientId
        scopeAccess.getLDAPEntry() >> entry
        scopeAccess.getUniqueId() >> dn
        scopeAccess.isAccessTokenExpired(_) >> isExpired
        scopeAccess.isAccessTokenWithinRefreshWindow(_) >> refresh

        return scopeAccess
    }

    def createEntry(String dn) {
        dn = dn ? dn : "accessToken=GENERIC,cn=TOKENS,dn=STRING"
        def mock = Mock(ReadOnlyEntry)
        mock.getDN() >> dn
        mock.getAttributeValue(_) >> { arg ->
            return arg[0]
        }
        return mock
    }
}
