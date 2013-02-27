package testHelpers

import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11
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
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials
import com.rackspace.idm.api.resource.cloud.v20.AuthWithPasswordCredentials
import com.rackspace.idm.api.resource.cloud.v20.AuthWithToken
import com.rackspace.idm.api.resource.cloud.v20.CloudGroupBuilder
import com.rackspace.idm.api.resource.cloud.v20.CloudKsGroupBuilder
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.api.resource.cloud.v20.DefaultRegionService
import com.rackspace.idm.api.resource.cloud.v20.DelegateCloud20Service
import com.rackspace.idm.api.resource.cloud.v20.PolicyValidator
import com.rackspace.idm.api.resource.pagination.Paginator
import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.AuthDao
import com.rackspace.idm.domain.dao.CustomerDao
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.ClientScopeAccess
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.RackerScopeAccess
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
import com.rackspace.idm.util.AuthHeaderHelper
import com.rackspace.idm.util.RSAClient
import com.rackspace.idm.validation.InputValidator
import com.rackspace.idm.validation.ObjectConverter
import com.rackspace.idm.validation.PrecedenceValidator
import com.rackspace.idm.validation.Validator20
import com.unboundid.ldap.sdk.ReadOnlyEntry
import org.apache.commons.configuration.Configuration
import org.joda.time.DateTime
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
    @Shared RSAClient rsaClient
    @Shared Validator validator
    @Shared Validator20 validator20
    @Shared PrecedenceValidator precedenceValidator
    @Shared PolicyValidator policyValidator
    @Shared InputValidator inputValidator
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
    @Shared UserConverterCloudV11 userConverterV11
    @Shared DomainConverterCloudV20 domainConverter
    @Shared DomainsConverterCloudV20 domainsConverter
    @Shared PolicyConverterCloudV20 policyConverter
    @Shared PoliciesConverterCloudV20 policiesConverter
    @Shared CapabilityConverterCloudV20 capabilityConverter
    @Shared RegionConverterCloudV20 regionConverter
    @Shared QuestionConverterCloudV20 questionConverter
    @Shared SecretQAConverterCloudV20 secretQAConverter
    @Shared ObjectConverter objectConverter

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

    // Dao's
    @Shared ApplicationDao applicationDao
    @Shared ScopeAccessDao scopeAccessDao
    @Shared UserDao userDao
    @Shared TenantDao tenantDao
    @Shared EndpointDao endpointDao
    @Shared TenantRoleDao tenantRoleDao
    @Shared CustomerDao customerDao
    @Shared ApplicationRoleDao applicationRoleDao
    @Shared AuthDao authDao
    @Shared DomainDao domainDao

    @Shared HttpHeaders headers
    @Shared AuthHeaderHelper authHeaderHelper
    @Shared Paginator userPaginator
    @Shared Paginator domainPaginator
    @Shared Paginator applicationRolePaginator
    @Shared Paginator tenantRolePaginator
    @Shared AuthWithToken authWithToken
    @Shared AuthWithPasswordCredentials authWithPasswordCredentials
    @Shared AuthWithApiKeyCredentials authWithApiKeyCredentials

    @Shared def jaxbMock

    @Shared def authToken = "token"
    @Shared def entityFactory = new EntityFactory()
    @Shared def entityFactoryForValidation = new EntityFactoryForValidation()
    @Shared def v1Factory = new V1Factory()
    @Shared def v2Factory = new V2Factory()

    @Shared def defaultExpirationHours = 12
    @Shared def defaultRefreshHours = 6
    @Shared def defaultImpersonationHours = 1

    @Shared def defaultExpirationSeconds = 3600 * defaultExpirationHours
    @Shared def defaultRefreshSeconds = 3600 * defaultRefreshHours
    @Shared def defaultImpersonationExpirationSeconds = 3600 * defaultImpersonationHours

    /*
        Mock Converters
    */
    def mockAuthConverterCloudV20(service) {
        authConverter = Mock()
        authConverter.toAuthenticationResponse(_, _, _, _) >> v2Factory.createAuthenticateResponse()
        authConverter.toImpersonationResponse(_) >> v1Factory.createImpersonationResponse()
        service.authConverterCloudV20 = authConverter
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
        tenantConverter.toTenantDO(_) >> entityFactory.createTenant()
        service.tenantConverterCloudV20 = tenantConverter
    }

    def mockTokenConverter(service) {
        tokenConverter = Mock()
        tokenConverter.toToken(_) >> v2Factory.createToken()
        tokenConverter.toToken(_, _) >> v2Factory.createToken()
        service.tokenConverterCloudV20 = tokenConverter
    }

    def mockUserConverter(service) {
        userConverter = Mock()
        userConverter.toUser(_) >> v2Factory.createUser()
        userConverter.toUserDO(_) >> entityFactory.createUser()
        userConverter.toUserForAuthenticateResponse(_ as Racker, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toUserForAuthenticateResponse(_ as User, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toUserForCreate(_) >> v1Factory.createUserForCreate()
        userConverter.toUserList(_) >> v2Factory.createUserList()
        service.userConverterCloudV20 = userConverter
    }

    def mockDomainConverter(service) {
        domainConverter = Mock()
        domainConverter.toDomain(_) >> v1Factory.createDomain()
        domainConverter.toDomainDO(_) >> entityFactory.createDomain()
        service.domainConverterCloudV20 = domainConverter
    }

    def mockDomainsConverter(service) {
        domainsConverter = Mock()
        domainsConverter.toDomains(_) >> v1Factory.createDomains()
        domainsConverter.toDomainsDO(_) >> entityFactory.createDomains()
        service.domainsConverterCloudV20 = domainsConverter
    }

    def mockPolicyConverter(service) {
        policyConverter = Mock()
        policyConverter.toPolicy(_) >> v1Factory.createPolicy()
        policyConverter.toPolicyDO(_) >> entityFactory.createPolicy()
        policyConverter.toPolicyForPolicies(_) >> v1Factory.createPolicy()
        service.policyConverterCloudV20 = policyConverter
    }

    def mockPoliciesConverter(service) {
        policiesConverter = Mock()
        policiesConverter.toPolicies(_) >> v1Factory.createPolicies()
        policiesConverter.toPoliciesDO(_) >> entityFactory.createPolicies()
        service.policiesConverterCloudV20 = policiesConverter
    }

    def mockCapabilityConverter(service) {
        capabilityConverter = Mock()
        capabilityConverter.fromCapability(_) >> entityFactory.createCapability()
        capabilityConverter.fromCapabilities(_) >> entityFactory.createCapabilities()
        capabilityConverter.toCapability(_) >> jaxbMock
        capabilityConverter.toCapabilities(_) >> jaxbMock
        capabilityConverter.toServiceApis(_) >> jaxbMock
        service.capabilityConverterCloudV20 = capabilityConverter
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
        userConverterV11.toUserDO(_) >> entityFactory.createUser()
        userConverterV11.toCloudV11User(_) >> v1Factory.createUser()
        userConverterV11.openstackToCloudV11User(_, _) >> v1Factory.createUser()
        userConverterV11.toCloudV11User(_, _) >> entityFactory.createUser()
        service.userConverterCloudV11 = userConverterV11
    }

    /*
        Mock Services
    */

    def mockApplicationService(service) {
        applicationService = Mock()
        service.applicationService = applicationService
    }

    def mockApiDocService(service) {
        apiDocService = Mock()
        service.apiDocService = apiDocService
    }

    def mockDomainService(service) {
        domainService = Mock()
        service.domainService = domainService
    }

    def mockCustomerService(service) {
        customerService = Mock()
        service.customerService = customerService
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

    def mockUserService(service) {
        userService = Mock()
        service.userService = userService
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

    def mockDefaultCapabilityService(service) {
        defaultCapabilityService = Mock()
        service.defaultCapabilityService = defaultCapabilityService
    }

    def mockDefaultAuthorizationService(service) {
        defaultAuthorizationService = Mock()
        service.defaultAuthorizationService = defaultAuthorizationService
    }

    def mockDefaultEndpointService(service) {
        defaultEndpointService = Mock()
        service.defaultEndpointService = defaultEndpointService
    }

    def mockDefaultApiDocService(service) {
        defaultApiDocService = Mock()
        service.defaultApiDocService = defaultApiDocService
    }

    def mockDefaultTenantService(service) {
        defaultTenantService = Mock()
        service.defaultTenantService = defaultTenantService
    }

    def mockDefaultGroupService(service) {
        defaultGroupService = Mock()
        service.defaultGroupService = defaultGroupService
    }

    def mockDefaultUserService(service) {
        defaultUserService = Mock()
        service.defaultUserService = defaultUserService
    }

    def mockDefaultCustomerService(service) {
        defaultCustomerService = Mock()
        service.defaultCustomerService = defaultCustomerService
    }

    def mockDefaultTokenService(service) {
        defaultTokenService = Mock()
        service.defaultTokenService = defaultTokenService
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

    def mockDefaultPolicyService(service) {
        defaultPolicyService = Mock()
        service.defaultPolicyService = defaultPolicyService
    }

    def mockPolicyService(service) {
        policyService = Mock()
        service.policyService = policyService
    }

    def mockCloud11Service(service) {
        cloud11Service = Mock()
        service.cloud11Service = cloud11Service
    }

    def mockDelegateCloud11Service(service) {
        delegateCloud11Service = Mock()
        service.delegateCloud11Service = delegateCloud11Service
    }

    def mockDefaultCloud11Service(service) {
        defaultCloud11Service = Mock()
        service.defaultCloud11Service = defaultCloud11Service
    }

    def mockCloudMigrationService(service) {
        cloudMigrationService = Mock()
        service.cloudMigrationService = cloudMigrationService
    }

    def mockDefaultRegionService(service) {
        defaultRegionService = Mock()
        service.defaultRegionService = defaultRegionService
    }

    def mockDefaultCloud20Service(service) {
        defaultCloud20Service = Mock()
        service.defaultCloud20Service = defaultCloud20Service
    }

    def mockDelegateCloud20Service(service) {
        delegateCloud20Service = Mock()
        service.delegateCloud20Service = delegateCloud20Service
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

    def mockCustomerDao(service) {
        customerDao = Mock()
        service.customerDao = customerDao
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

    def mockPolicyValidator(service) {
        policyValidator = Mock()
        service.policyValidator = policyValidator
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

    /*
        misc. mocks
    */

    def mockAtomHopperClient(service) {
        atomHopperClient = Mock()
        service.atomHopperClient = atomHopperClient
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

        return uriInfo
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
            it.setLdapEntry(createLdapEntryWithDn(dn))
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
            it.username = username
            it.impersonatingUsername = impUsername
            it.impersonatingToken = impToken
            it.setLdapEntry(createLdapEntryWithDn(dn))
            return it
        }
    }
    def createPasswordResetScopeAccess() {
        return createPasswordResetScopeAccess("tokenString", "clientId", "userRsId", new DateTime().plusHours(defaultExpirationHours + 1).toDate())
    }

    def createPasswordResetScopeAccess(String tokenString, String clientId, String userRsId, Date expiration) {
        clientId = clientId ? clientId : "clientId"
        tokenString = tokenString ? tokenString : "tokenString"
        userRsId = userRsId ? userRsId : "userRsId"
        def dn = "accessToken=$tokenString,cn=TOKENS,userRsId=$userRsId"

        new PasswordResetScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.userRsId = userRsId
            it.clientId = clientId
            it.setLdapEntry(createLdapEntryWithDn(dn))
            return it
        }
    }

    def createRackerScopeAcccss() {
        return createRackerScopeAccess("tokenString", "rackerId", new DateTime().plusHours(defaultExpirationHours + 1).toDate())
    }

    def createRackerScopeAccess(String tokenString, String rackerId, Date expiration) {
        tokenString = tokenString ? tokenString : "tokenString"
        rackerId = rackerId ? rackerId : "rackerId"
        def dn = "accessToken=$tokenString,cn=TOKENS,rsId=$rackerId,ou=rackers"

        new RackerScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.rackerId = rackerId
            it.setLdapEntry(createLdapEntryWithDn(dn))
            return it
        }
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
            it.setLdapEntry(createLdapEntryWithDn(dn))
            return it
        }
    }

    def createLdapEntryWithDn(String dn) {
        return new ReadOnlyEntry(dn)
    }

    def allowUserAccess() {
        allowAccess(createUserScopeAccess())
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

    def mm(String value){
        return value.multiply(1000)
    }

    def mmm(String value){
        return value.multiply(100000)
    }
}
