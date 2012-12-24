package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.converter.cloudv20.*
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.resource.cloud.Validator
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.domain.service.*
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ExceptionHandler
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.validation.*
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.api.resource.pagination.Paginator
import com.rackspace.idm.domain.service.ApplicationService
import testHelpers.V1Factory
import testHelpers.V2Factory

import javax.ws.rs.core.HttpHeaders
import com.rackspace.idm.domain.service.TenantService
import testHelpers.EntityFactory

import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo
import javax.xml.bind.JAXBElement

/*
 This class uses the application context but mocks the ldap interactions
 */
class DefaultCloud20ServiceTest extends Specification {

    @Shared DefaultCloud20Service cloud20Service
    @Shared AuthenticationService authenticationService
    @Shared AuthorizationService authorizationService
    @Shared CapabilityService capabilityService
    @Shared ApplicationService clientService
    @Shared Configuration config
    @Shared EndpointService endpointService
    @Shared JAXBObjectFactories objFactories
    @Shared ScopeAccessService scopeAccessService
    @Shared TenantService tenantService
    @Shared GroupService cloudGroupService
    @Shared UserService userService
    @Shared AtomHopperClient atomHopperClient
    @Shared Validator validator
    @Shared Validator20 validator20
    @Shared DefaultRegionService defaultRegionService
    @Shared DomainService domainService
    @Shared PolicyService policyService
    @Shared QuestionService questionService
    @Shared SecretQAService qaService
    @Shared Paginator paginator
    @Shared PrecedenceValidator precedenceValidator
    @Shared DelegateCloud20Service delegateCloud20Service
    @Shared CloudGroupBuilder cloudGroupBuilder
    @Shared CloudKsGroupBuilder cloudKsGroupBuilder

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
    @Shared ExceptionHandler exceptionHandler

    @Shared HttpHeaders headers
    @Shared def jaxbMock

    @Shared def authToken = "token"
    @Shared def offset = "0"
    @Shared def limit = "25"
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def questionId = "id"
    @Shared def roleId = "roleId"

    @Shared def adminRole
    @Shared def userAdminRole
    @Shared def defaultUserRole
    @Shared def specialRole
    @Shared def tenantRoleList
    @Shared def serviceAdminRole
    @Shared def genericRole
    @Shared def userAdmin
    @Shared def defaultUser
    @Shared def userNotInDomain
    @Shared def adminUser
    @Shared def serviceAdmin

    @Shared def defaultTenantRole
    @Shared def userAdminTenantRole
    @Shared def adminTenantRole
    @Shared def serviceAdminTenantRole

    @Shared def List<ClientRole> identityRoles

    @Shared def serviceAdminRoleId = "0"
    @Shared def adminRoleId = "1"
    @Shared def userAdminRoleId = "2"
    @Shared def defaultRoleId = "3"

    @Shared def entityFactory
    @Shared def v1Factory
    @Shared def v2Factory

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")

        cloud20Service = new DefaultCloud20Service()
        exceptionHandler = new ExceptionHandler()
        objFactories = new JAXBObjectFactories()

        exceptionHandler.objFactories = objFactories

        cloud20Service.exceptionHandler = exceptionHandler

        entityFactory = new EntityFactory()
        v1Factory = new V1Factory()
        v2Factory = new V2Factory()
    }

    def setup() {
        mockServices()

        headers = Mock()
        jaxbMock = Mock(JAXBElement)
    }

    def "validateOffset null offset sets offset to 0"() {
        when:
        def offset = cloud20Service.validateOffset(null)

        then:
        offset == 0
    }

    def "validateOffset negative offset throws bad request"() {
        when:
        cloud20Service.validateOffset("-5")

        then:
        thrown(BadRequestException)
    }

    def "validateOffset blank offset throws bad request"() {
        when:
        cloud20Service.validateOffset("")

        then:
        thrown(BadRequestException)
    }

    def "validateOffset valid offset sets offset"() {
        when:
        def offset = cloud20Service.validateOffset("10")

        then:
        offset == 10
    }

    def "validateLimit null limit sets limit to default"() {
        when:
        config.getInt(_) >> 25
        def limit = cloud20Service.validateLimit(null)

        then:
        limit == 25
    }

    def "validateLimit negative limit throws bad request"() {
        when:
        cloud20Service.validateLimit("-5")

        then:
        thrown(BadRequestException)
    }

    def "validateLimit blank limit throws bad request"() {
        when:
        cloud20Service.validateLimit("")

        then:
        thrown(BadRequestException)
    }

    def "validateLimit limit is 0 sets to default"() {
        when:
        config.getInt(_) >> 25
        def limit = cloud20Service.validateLimit("0")

        then:
        limit == 25
    }

    def "question create verifies Identity admin level access and adds Question"() {
        given:
        mockQuestionConverter()
        allowAccess()


        when:
        def response = cloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion()).build()

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
        1 * questionService.addQuestion(_) >> "questionId"
        response.getStatus() == 201
        response.getMetadata().get("location")[0] != null
    }

    def "question create handles exceptions"() {
        given:
        mockQuestionConverter()

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]

        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        questionService.addQuestion(_) >> { throw new BadRequestException() }

        when:
        def response1 = cloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())
        def response2 = cloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())
        def response3 = cloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401
        response2.build().status == 403
        response3.build().status == 400
    }

    def "question delete verifies Identity admin level access and deletes question"() {
        given:
        mockQuestionConverter()
        allowAccess()

        when:
        def response = cloud20Service.deleteQuestion(authToken, questionId).build()

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
        1 * questionService.deleteQuestion(questionId)

        response.getStatus() == 204
    }

    def "question delete handles exceptions"() {
        given:
        mockQuestionConverter()

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        questionService.deleteQuestion(questionId) >> { throw new NotFoundException() }

        when:
        def response1 = cloud20Service.deleteQuestion(authToken, questionId).build()
        def response2 = cloud20Service.deleteQuestion(authToken, questionId).build()
        def response3 = cloud20Service.deleteQuestion(authToken, questionId).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 404

    }

    def "question update verifies Identity admin level access"() {
        given:
        mockQuestionConverter()
        allowAccess()

        when:
        def response = cloud20Service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion())

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_)
        response.build().status == 204
    }

    def "question update updates question"() {
        given:
        mockQuestionConverter()
        allowAccess()

        when:
        def response = cloud20Service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion()).build()

        then:
        1 * questionService.updateQuestion(questionId, _)
        response.status == 204
    }

    def "question update handles exceptions"() {
        given:
        mockQuestionConverter()

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]

        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        questionService.updateQuestion(sharedRandom, _) >> { throw new BadRequestException() }
        questionService.updateQuestion("1$sharedRandom", _) >> { throw new NotFoundException() }

        when:
        def response1 = cloud20Service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response2 = cloud20Service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response3 = cloud20Service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response4 = cloud20Service.updateQuestion(authToken, "1$sharedRandom", entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401
        response2.build().status == 403
        response3.build().status == 400
        response4.build().status == 404
    }

    def "question(s) get verifies user level access"() {
        given:
        mockQuestionConverter()
        allowAccess()

        when:
        cloud20Service.getQuestion(authToken, questionId)
        cloud20Service.getQuestions(authToken)

        then:
        2 * authorizationService.verifyUserLevelAccess(_)
    }

    def "question(s) get gets question and returns it (them)"() {
        given:
        mockQuestionConverter()
        allowAccess()

        def questionList = [
                entityFactory.createQuestion("1", "question1"),
                entityFactory.createQuestion("2", "question2")
        ].asList()

        when:
        def response1 = cloud20Service.getQuestion(authToken, questionId).build()
        def response2 = cloud20Service.getQuestions(authToken).build()

        then:
        1 * questionService.getQuestion(_) >> entityFactory.createQuestion()
        1 * questionService.getQuestions() >> questionList

        1 * questionConverter.toQuestion(_) >> { arg1 ->
            def arg = arg1.get(0)
            assert(arg.id.equalsIgnoreCase("id"))
            assert(arg.question.equalsIgnoreCase("question"))
            return jaxbMock
        }

        1 * questionConverter.toQuestions(_) >> { arg1 ->
            def list = arg1.get(0)
            assert(list.size == 2)
            assert(list.get(0).id.equals("1"))
            assert(list.get(0)).question.equals("question1")
            assert(list.get(1).id.equals("2"))
            assert(list.get(1).question.equals("question2"))
            return jaxbMock
        }

        response1.status == 200
        response2.status == 200

    }

    def "question(s) get handles exceptions"() {
        given:
        mockQuestionConverter()

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(authToken) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyUserLevelAccess(mock) >> { throw new ForbiddenException() }
        questionService.getQuestion("1$questionId") >> {throw new NotFoundException() }

        def secondMock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken("1$authToken") >>> [ null, secondMock ]
        authorizationService.verifyUserLevelAccess(secondMock) >> { throw new ForbiddenException() }

        when:
        def questionResponse1 = cloud20Service.getQuestion(authToken, questionId).build()
        def questionResponse2 = cloud20Service.getQuestion(authToken, questionId).build()
        def questionResponse3 = cloud20Service.getQuestion(authToken, "1$questionId").build()

        def questionsResponse1 = cloud20Service.getQuestions("1$authToken").build()
        def questionsResponse2 = cloud20Service.getQuestions("1$authToken").build()

        then:
        questionResponse1.status == 401
        questionResponse2.status == 403
        questionResponse3.status == 404

        questionsResponse1.status == 401
        questionsResponse2.status == 403
    }

    def "updateCapabilities verifies identity admin level access"() {
        given:
        mockCapabilityConverter()
        allowAccess()

        when:
        cloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(), "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "updateCapabilities updates capability" (){
        given:
        mockCapabilityConverter()
        allowAccess()

        when:
        def response = cloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest","1").build()

        then:
        response.status == 204
    }

    def "updateCapabilities handles exceptions" (){
        given:
        mockCapabilityConverter()

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.updateCapabilities(_, _, _) >> { throw new BadRequestException() }

        when:
        def response1 = cloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()
        def response2 = cloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()
        def response3 = cloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "capabilites get verifies identity admin level access"() {
        given:
        mockCapabilityConverter()
        allowAccess()

        when:
        cloud20Service.getCapabilities(authToken, "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "Capabilities get gets and returns capabilities" () {
        given:
        mockCapabilityConverter()
        allowAccess()

        jaxbMock.getValue() >> v1Factory.createCapabilities([ v1Factory.createCapability("1", "capability") ].asList())

        when:
        def response = cloud20Service.getCapabilities(authToken,"computeTest","1").build()

        then:
        1 * capabilityService.getCapabilities(_, _)
        def com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities entity = response.getEntity()
        entity.getCapability().get(0).id.equals("1")
        entity.getCapability().get(0).name.equals("capability")
        response.status == 200
    }

    def "capabilities get handles exceptions" () {
        given:
        mockCapabilityConverter()

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.getCapabilities(_, _) >> { throw new BadRequestException() }

        when:
        def response1 = cloud20Service.getCapabilities("badToken","computeTest","1").build()
        def response2 = cloud20Service.getCapabilities("badToken","computeTest","1").build()
        def response3 = cloud20Service.getCapabilities("badToken","computeTest","1").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "deleteCapabilities verifies identity admin level access"() {
        given:
        mockCapabilityConverter()
        allowAccess()

        when:
        cloud20Service.removeCapabilities(authToken, "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "deleteCapabilities deletes capability" () {
        given:
        mockCapabilityConverter()
        allowAccess()

        when:
        def response = cloud20Service.removeCapabilities(authToken , "computeTest", "1").build()

        then:
        response.status == 204
        1 * capabilityService.removeCapabilities("computeTest", "1")
    }

    def "deleteCapabilities handles exceptions"() {
        given:
        mockCapabilityConverter()

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.removeCapabilities(_, _) >> { throw new BadRequestException() }

        when:
        def response1 = cloud20Service.removeCapabilities(authToken, null, null).build()
        def response2 = cloud20Service.removeCapabilities(authToken, null, null).build()
        def response3 = cloud20Service.removeCapabilities(authToken, null, null).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    //Helper Methods
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
        builderMock.build() >> absPath
        uriInfo.getRequestUriBuilder() >> builderMock

        return uriInfo
    }

    def allowAccess() {
        scopeAccessService.getScopeAccessByAccessToken(_) >> Mock(ScopeAccess)
    }

    def mockAuthConverter() {
        authConverter = Mock()
        authConverter.toAuthenticationResponse(_, _, _, _) >> v2Factory.createAuthenticateResponse()
        authConverter.toImpersonationResponse(_) >> v1Factory.createImpersonationResponse()
        cloud20Service.authConverterCloudV20 = authConverter
    }

    def mockEndpointConverter() {
        endpointConverter = Mock()
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createCloudBaseUrl()
        endpointConverter.toEndpoint(_) >> v2Factory.createEndpoint()
        endpointConverter.toEndpointList(_) >> v2Factory.createEndpointList()
        endpointConverter.toEndpointListFromBaseUrls(_) >> v2Factory.createEndpointList()
        endpointConverter.toEndpointTemplate(_) >> v1Factory.createEndpointTemplate()
        endpointConverter.toEndpointTemplateList(_) >> v1Factory.createEndpointTemplateList()
        endpointConverter.toServiceCatalog(_) >> v2Factory.createServiceCatalog()
        cloud20Service.endpointConverterCloudV20 = endpointConverter
    }

    def mockRoleConverter() {
        roleConverter = Mock()
        roleConverter.toRole(_) >> v2Factory.createRole()
        roleConverter.toRoleFromClientRole(_) >> v2Factory.createRole()
        roleConverter.toRoleListFromClientRoles(_) >> v2Factory.createRoleList()
        roleConverter.toRoleListFromClientRoles(_) >> v2Factory.createRoleList()
        cloud20Service.roleConverterCloudV20 = roleConverter
    }

    def mockServiceConverter() {
        serviceConverter = Mock()
        serviceConverter.toService(_) >> v1Factory.createService()
        serviceConverter.toServiceList(_) >> v1Factory.createServiceList()
        cloud20Service.serviceConverterCloudV20 = serviceConverter
    }

    def mockTenantConverter() {
        tenantConverter = Mock()
        tenantConverter.toTenant(_) >> v2Factory.createTenant()
        tenantConverter.toTenantDO(_) >> entityFactory.createTenant()
        tenantConverter.toTenantList(_) >> v2Factory.createTenantList()
        cloud20Service.tenantConverterCloudV20 = tenantConverter
    }

    def mockTokenConverter() {
        tokenConverter = Mock()
        tokenConverter.toToken(_) >> v2Factory.createToken()
        tokenConverter.toToken(_, _) >> v2Factory.createToken()
        cloud20Service.tokenConverterCloudV20 = tokenConverter
    }

    def mockUserConverter() {
        userConverter = Mock()
        userConverter.toUser(_) >> v2Factory.createUser()
        userConverter.toUserDO(_) >> entityFactory.createUser()
        userConverter.toUserForAuthenticateResponse(_ as Racker, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toUserForAuthenticateResponse(_ as User, _) >> v2Factory.createUserForAuthenticateResponse()
        userConverter.toUserForCreate(_) >> v1Factory.createUserForCreate()
        userConverter.toUserList(_) >> v2Factory.createUserList()
        cloud20Service.userConverterCloudV20 = userConverter
    }

    def mockDomainConverter() {
        domainConverter = Mock()
        domainConverter.toDomain(_) >> v1Factory.createDomain()
        domainConverter.toDomainDO(_) >> entityFactory.createDomain()
        cloud20Service.domainConverterCloudV20 = domainConverter
    }

    def mockDomainsConverter() {
        domainsConverter = Mock()
        domainsConverter.toDomains(_) >> v1Factory.createDomains()
        domainsConverter.toDomainsDO(_) >> entityFactory.createDomains()
        cloud20Service.domainsConverterCloudV20 = domainsConverter
    }

    def mockPolicyConverter() {
        policyConverter = Mock()
        policyConverter.toPolicy(_) >> v1Factory.createPolicy()
        policyConverter.toPolicyDO(_) >> entityFactory.createPolicy()
        policyConverter.toPolicyForPolicies(_) >> v1Factory.createPolicy()
        cloud20Service.policyConverterCloudV20 = policyConverter
    }

    def mockPoliciesConverter() {
        policiesConverter = Mock()
        policiesConverter.toPolicies(_) >> v1Factory.createPolicies()
        policiesConverter.toPoliciesDO(_) >> entityFactory.createPolicies()
        cloud20Service.policiesConverterCloudV20 = policiesConverter
    }

    def mockCapabilityConverter() {
        capabilityConverter = Mock()
        capabilityConverter.fromCapability(_) >> entityFactory.createCapability()
        capabilityConverter.fromCapabilities(_) >> entityFactory.createCapabilities()
        capabilityConverter.toCapability(_) >> jaxbMock
        capabilityConverter.toCapabilities(_) >> jaxbMock
        capabilityConverter.toServiceApis(_) >> jaxbMock
        cloud20Service.capabilityConverterCloudV20 = capabilityConverter
    }


    def mockRegionConverter() {
        regionConverter = Mock()
        regionConverter.fromRegion(_) >> entityFactory.createRegion()
        regionConverter.toRegion(_) >> jaxbMock
        regionConverter.toRegions(_) >> jaxbMock
        cloud20Service.regionConverterCloudV20 = regionConverter
    }

    def mockQuestionConverter() {
        questionConverter = Mock()
        questionConverter.fromQuestion(_) >> entityFactory.createQuestion()
        questionConverter.toQuestion(_) >> jaxbMock
        questionConverter.toQuestions(_) >> jaxbMock
        cloud20Service.questionConverter = questionConverter
    }

    def mockSecretQAConverter() {
        secretQAConverter = Mock()
        secretQAConverter.fromSecretQA(_) >> entityFactory.createSecretQA()
        secretQAConverter.toSecretQA(_) >> jaxbMock
        secretQAConverter.toSecretQAs(_) >> jaxbMock
        cloud20Service.secretQAConverterCloudV20 = secretQAConverter
    }

    def mockServices() {
        authenticationService = Mock()
        authorizationService = Mock()
        clientService = Mock()
        capabilityService = Mock()
        config = Mock()
        endpointService = Mock()
        objFactories = Mock()
        scopeAccessService = Mock()
        tenantService = Mock()
        cloudGroupService = Mock()
        userService = Mock()
        atomHopperClient = Mock()
        validator = Mock()
        validator20 = Mock()
        defaultRegionService = Mock()
        domainService = Mock()
        policyService = Mock()
        questionService = Mock()
        qaService = Mock()
        paginator = Mock()
        precedenceValidator = Mock()
        delegateCloud20Service = Mock()
        cloudGroupBuilder = Mock()
        cloudKsGroupBuilder = Mock()

        cloud20Service.authenticationService = authenticationService
        cloud20Service.authorizationService = authorizationService
        cloud20Service.capabilityService = capabilityService
        cloud20Service.clientService = clientService
        cloud20Service.config = config
        cloud20Service.endpointService = endpointService
        cloud20Service.scopeAccessService = scopeAccessService
        cloud20Service.tenantService = tenantService
        cloud20Service.cloudGroupService = cloudGroupService
        cloud20Service.userService = userService
        cloud20Service.atomHopperClient = atomHopperClient
        cloud20Service.validator = validator
        cloud20Service.validator20 = validator20
        cloud20Service.defaultRegionService = defaultRegionService
        cloud20Service.domainService = domainService
        cloud20Service.policyService = policyService
        cloud20Service.questionService = questionService
        cloud20Service.secretQAService = qaService
        cloud20Service.userPaginator = paginator
        cloud20Service.domainPaginator = paginator
        cloud20Service.applicationRolePaginator = paginator
        cloud20Service.precedenceValidator = precedenceValidator
        cloud20Service.delegateCloud20Service = delegateCloud20Service
        cloud20Service.cloudGroupBuilder = cloudGroupBuilder
        cloud20Service.cloudKsGroupBuilder = cloudKsGroupBuilder
    }

    /*
    def cleanupSpec() {
    }

    // Belongs in UserService groovy tests
    def "create a user sets the default region"() {
        given:
        createMocks()
        allowAccess()

        userDao.isUsernameUnique(_) >> true
        cloudRegionService.getDefaultRegion("US") >> region()
        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        Pattern pattern = pattern("username","^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        userService.addUser(user)

        then:
        user.region == "DFW"
    }

    //Belongs in UserService groovy tests
    def "create user without default region throws bad request"() {
        given:
        createMocks()
        allowAccess()
        Pattern pattern = pattern("username","^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        userDao.isUsernameUnique(_) >> true

        cloudRegionService.getDefaultRegion("US") >> null

        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        when:
        userService.addUser(user)

        then:
        thrown(BadRequestException)
    }


    // this is for release (1.0.12 or seomthing) migration related 12/03/2012
    //Belongs in UserSErvice groovy Tests
    def "add User allows a user to be created with a username beginning with a number"() {
        given:
        createMocks()
        allowAccess()
        userDao.isUsernameUnique(_) >> true
        cloudRegionService.getDefaultRegion(_) >> region()

        def user = new UserForCreate().with {
                it.username = "1user$sharedRandom"
                it.email = "email@email.com"
                return it
            }
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        def response = cloud20Service.addUser(headers, uriInfo(), authToken, user)

        then:
        response.build().status == 201
    }
    //Belongs in UserSErvice groovy Tests
    def "add User with password"() {
        given:
        createMocks()
        allowAccess()
        userDao.isUsernameUnique(_) >> true
        cloudRegionService.getDefaultRegion(_) >> region()

        def user = new UserForCreate().with {
            it.username = "1user$sharedRandom"
            it.email = "email@email.com"
            it.password = "Password1"
            return it
        }
        Pattern pattern1 = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        Pattern pattern2 = pattern("password","^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z\\d=+`|\\(){}\\[\\]:;\"'<>,.?/£~!@#%^&*_-]{8,}\$","Some error","desc")
        Pattern pattern3 = pattern("email","^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern1 >> pattern3 >> pattern2 >> pattern3

        when:
        def response = cloud20Service.addUser(headers, uriInfo(), authToken, user)

        then:
        response.build().status == 201
    }

    //Belongs in UserSErvice groovy Tests
    def "add User with bad password"() {
        given:
        createMocks()
        allowAccess()
        userDao.isUsernameUnique(_) >> true
        cloudRegionService.getDefaultRegion(_) >> region()

        def user = new UserForCreate().with {
            it.username = "1user$sharedRandom"
            it.email = "email@email.com"
            it.password = "password1"
            return it
        }
        Pattern pattern1 = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        Pattern pattern2 = pattern("username","^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z\\d=+`|\\(){}\\[\\]:;\"'<>,.?/£~!@#%^&*_-]{8,}\$","Some error","desc")
        ldapPatternRepository.getPattern(_) >>> [ pattern1 >> pattern2 ]

        when:
        def response = cloud20Service.addUser(headers, uriInfo(), authToken, user)

        then:
        response.build().status == 400
    }

    //Belongs in UserSErvice groovy Tests
    def "add User with bad password 2"() {
        given:
        createMocks()
        allowAccess()
        userDao.isUsernameUnique(_) >> true
        cloudRegionService.getDefaultRegion(_) >> region()

        def user = new UserForCreate().with {
            it.username = "1user$sharedRandom"
            it.email = "email@email.com"
            it.password = "Pass1"
            return it
        }
        Pattern pattern1 = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        Pattern pattern2 = pattern("username","^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z\\d=+`|\\(){}\\[\\]:;\"'<>,.?/£~!@#%^&*_-]{8,}\$","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern1 >> pattern2

        when:
        def response = cloud20Service.addUser(headers, uriInfo(), authToken, user)

        then:
        response.build().status == 400
    }

    //Belongs in UserSErvice groovy Tests
    def "Add user with emtpy username"() {
        given:
        createMocks()
        allowAccess()
        userDao.isUsernameUnique(_) >> true
        cloudRegionService.getDefaultRegion(_) >> region()

        def user = new UserForCreate().with {
            it.username = ""
            it.email = "email@email.com"
            return it
        }
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        def response = cloud20Service.addUser(headers, uriInfo(), authToken, user)

        then:
        response.build().status == 400
    }

    //Belongs in UserSErvice groovy Tests
    def "Add user with null username"() {
        given:
        createMocks()
        allowAccess()
        userDao.isUsernameUnique(_) >> true
        cloudRegionService.getDefaultRegion(_) >> region()

        def user = new UserForCreate().with {
            it.username = null
            it.email = "email@email.com"
            return it
        }
        Pattern pattern = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern

        when:
        def response = cloud20Service.addUser(headers, uriInfo(), authToken, user)

        then:
        response.build().status == 400
    }

    //Belongs in UserSErvice groovy Tests
    def "Update user: username null" (){
        given:
        createMocks()
        allowAccess()
        def user = new UserForCreate().with {
            it.username = null
            it.email = "email@email.com"
            return it
        }
        def retrievedUser = new User().with {
            it.username = "somename"
            it.email = "some@email.com"
            it.id = "1"
            return it
        }
        userDao.getUserById(_) >> retrievedUser
        Pattern pattern1 = pattern("username","^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Some error","desc")
        ldapPatternRepository.getPattern(_) >> pattern1
        scopeAccessService.getScopeAccessListByUserId(_) >> new ArrayList<ScopeAccess>();

        when:
        def response = cloud20Service.updateUser(headers,authToken,"1",user)

        then:
        response.build().status == 200
    }

    def "listUsers verifies token"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(_)

    }

    def "listUsers returns caller"() {
        given:
        createMocks()
        allowAccess()

        userDao.getUserByUsername(_) >> user(sharedRandom, null)
        authorizationService.authorizeCloudUser(_) >> true

        when:
        def response = cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit).build()

        then:
        response.status == 200
        response.entity.user[0].username.equals(sharedRandom)
    }

    def "listUsers verifies userAdmin Access level"() {
        given:
        createMocks()
        allowAccess()
        authorizationService.authorizeCloudUserAdmin(_) >> false

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "listUsers calls getAllUsersPaged with no filters"() {
        given:
        createMocks()
        allowAccess()

        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * userDao.getAllUsersPaged(null, Integer.parseInt(offset), Integer.parseInt(limit))
    }

    def "listUsers calls getAllUsersPaged with domainId filter"() {
        given:
        createMocks()
        allowAccess()
        def user = user(sharedRandom, null)
        user.domainId = "123456789"

        def userList = [ ] as User[]
        userList = userList.toList()
        FilterParam[] filters = [new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, "123456789")] as FilterParam[]
        userDao.getUserByUsername(_) >> user
        userDao.getAllUsersPaged(_, _, _) >> userContext(Integer.parseInt(offset), Integer.parseInt(limit), userList)
        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        when:
        cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit)

        then:
        1 * userDao.getAllUsersPaged(_, _, _)
    }

    def "listUsers throws bad request"() {
        given:
        createMocks()
        allowAccess()

        userDao.getUserByUsername(_) >> user(sharedRandom, null)
        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        when:
        def response = cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit).build()

        then:
        response.status == 400
    }

    def "listUsers returns userList"() {
        given:
        createMocks()
        allowAccess()
        def userList = [ user("user1", null), user("user2", null), user("user3", null) ] as User[]
        userList = userList.toList()
        def userContext = userContext(Integer.parseInt(offset), Integer.parseInt(limit), userList)

        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> true
        userDao.getUserByUsername(_) >> user(sharedRandom, null)
        userDao.getAllUsersPaged(_, _, _) >> userContext

        when:
        def response = cloud20Service.listUsers(null, uriInfo(), authToken, offset, limit).build()

        then:
        response.status == 200
        response.entity.user[0].username == "user1"
        response.entity.user[1].username == "user2"
        response.entity.user[2].username == "user3"

    }

    def "listUsersWithRole verifies access level"() {
        given:
        createMocks()
        allowAccess()

        clientRoleDao.getClientRole(_) >> clientRole()
        tenantDao.getMultipleTenantRoles(_, _, _) >> new PaginatorContext<String>();

        when:
        cloud20Service.listUsersWithRole(null, uriInfo(), authToken, roleId, offset, limit)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "listUsersWithRole throws notFoundException"() {
        given:
        createMocks()
        allowAccess()

        clientDao.getClientRoleById(_) >> null

        when:
        cloud20Service.listUsersWithRole(null, uriInfo(), authToken, roleId, offset, limit)

        then:
        thrown(NotFoundException)
    }

    def "listUsersWithRole gets role"() {
        given:
        createMocks()
        allowAccess()
        DefaultUserService service = Mock()
        cloud20Service.userService = service

        cloud20Service.userService.getUsersWithRole(_, _, _, _) >> new PaginatorContext<User>();

        when:
        cloud20Service.listUsersWithRole(null, uriInfo(), authToken, roleId, offset, limit)

        then:
        1 * clientRoleDao.getClientRole(_) >> clientRole()
    }

    def "listUsersWithRole caller user-admin with no domain throws badRequest"() {
        given:
        createMocks()
        allowAccess()

        DefaultUserService service = Mock()
        cloud20Service.userService = service

        tenantDao.getMultipleTenantRoles(_, _, _) >> new PaginatorContext<String>();
        clientRoleDao.getClientRole(_) >> clientRole()
        cloud20Service.userService.getUserByScopeAccess(_) >> user("username", null)
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        cloud20Service.listUsersWithRole(null, uriInfo(), authToken, roleId, offset, limit)

        then:
        thrown(BadRequestException)
    }

    def "listUsersWithRole caller is userAdmin"() {
        given:
        createMocks()
        allowAccess()
        DefaultUserService service = Mock()
        cloud20Service.userService = service

        clientRoleDao.getClientRole(_) >> clientRole()
        cloud20Service.userService.getUserByScopeAccess(_) >> user("username", "domainId")
        cloud20Service.userService.getUsersWithRole(_, _, _, _) >> new PaginatorContext<User>();
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        def response = cloud20Service.listUsersWithRole(null, uriInfo(), authToken, roleId, offset, limit)

        then:
        response.build().status == 200
    }

    def "listUsersWithRole service-admin calls getMultipleTenantRoles"() {
        given:
        createMocks()
        allowAccess()

        clientRoleDao.getClientRole(_) >> clientRole()
        authorizationService.authorizeCloudUserAdmin(_) >> false

        when:
        cloud20Service.listUsersWithRole(null, uriInfo(), authToken, roleId, offset, limit)

        then:
        1 * tenantDao.getMultipleTenantRoles(_, _, _) >> new PaginatorContext<String>()
    }

    def "listUsersWithRole user-admin calls getAllUsersNoLimit"() {
        given:
        createMocks()
        def access = new UserScopeAccess()
        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        access.accessTokenExp = calendar.getTime()
        access.accessTokenString = calendar.getTime().toString()
        access.username = "username"

        def userList = new ArrayList<User>()
        userList.add(user("username", null))
        def users = new Users()
        users.setUsers(userList)


        scopeAccessService.getScopeAccessByAccessToken(_) >> access
        clientRoleDao.getClientRole(_) >> clientRole()
        userDao.getUserByUsername(_) >> user("username", "domainId")
        userDao.getAllUsersNoLimit(_) >> users
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        cloud20Service.listUsersWithRole(null, uriInfo(), authToken, roleId, offset, limit)

        then:
        1 * userDao.getAllUsersNoLimit(_)
    }



    def "validateLimit limit is too large sets to default max"() {
        when:
        def value = configuration.getInt("ldap.paging.limit.max") + 1
        def limit = cloud20Service.validateLimit(value.toString())

        then:
        limit == configuration.getInt("ldap.paging.limit.max")
    }

    def "validateLimit limit is valid sets limit"() {
        when:
        def value = configuration.getInt("ldap.paging.limit.max") - 1
        def limit = cloud20Service.validateLimit(value.toString())

        then:
        limit == value
    }

    def "addUserRole verifies userAdmin Access"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "addUserRole gets user to add role to"() {
        given:
        createMocks()
        allowAccess()
        clientRoleDao.getClientRole(_) >> clientRole()

        when:
        cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom)

        then:
        1 * userDao.getUserById(_) >> user("username", "domainId")

    }

    def "addUserRole verifies role"() {
        given:
        createMocks()
        allowAccess()
        userDao.getUserById(_) >> user("username", "domainId")

        when:
        cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom)

        then:
        1 * clientRoleDao.getClientRole(_) >> clientRole()
    }

    def "addUserRole returns forbidden"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        // called
        userDao.getUserById(_) >> userNotInDomain

        // caller
        userDao.getUserByUsername(_) >> userAdmin

        authorizationService.authorizeCloudUserAdmin(_) >> true

        clientRoleDao.getClientRole(_) >> clientRole()
        when:
        def status = cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status

        then:
        status == 403
    }

    def "addUserRole throws forbidden (multiple identity:* roles)"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        def user1 = user("username", "domainId")

        userDao.getUserById(_) >> user1
        userDao.getUserByUsername(_) >> user1

        clientRoleDao.getClientRole(_) >> defaultUserRole

        // check multiple identityRoles
        tenantDao.getTenantRolesForUser(_) >> [ defaultTenantRole ].asList()
        clientDao.getClientRoleById(_) >> defaultUserRole

        when:
        def response = cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build()

        then:
        response.status == 403
    }

    def "addUserRole successfully adds role to user"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        //added role
        clientRoleDao.getClientRole(sharedRandom) >>> [
                clientRole("userAdminAccessible", 1000),
                clientRole("userAdminaccessible", 1500)
        ] >> genericRole

        // getUserIdentityRole for caller
        clientDao.getClientByClientId(_) >> new Application()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                userAdminTenantRole, userAdminTenantRole,
                adminTenantRole, adminTenantRole, adminTenantRole,
                serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole
        ]

        // check multiple identityRoles
        tenantDao.getTenantRolesForUser(_) >> [ defaultTenantRole ].asList()
        clientDao.getClientRoleById(_) >> genericRole

        authorizationService.authorizeCloudUserAdmin(_) >>> [
                true, true,
                false, false, false,
                false, false, false, false,
        ]

        //caller
        userDao.getUserByUsername(_) >>> [
                userAdmin, userAdmin,
                adminUser, adminUser, adminUser,
                serviceAdmin, serviceAdmin, serviceAdmin, serviceAdmin,
        ]

        //addedTo
        userDao.getUserById(_) >>> [
                defaultUser, defaultUser,
                defaultUser, userAdmin, adminUser,
                defaultUser, userAdmin, adminUser, serviceAdmin,
        ]

        clientDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole()

        when:
        def statuses = []
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)

        then:
        for (status in statuses) {
            assert(status == 200)
        }
    }

    def "deleteUserRole validates user to delete from and user deleting"() {
        given:
        createMocks()
        allowAccess()

        def user = user("username", "domainId")

        when:
        cloud20Service.deleteUserRole(headers, authToken, sharedRandom, sharedRandom)

        then:
        1 * userDao.getUserById(_) >> user
        1 * userDao.getUserByUsername(_) >> user
    }

    def "deleteUserRole prevents user from removing their own identity:* role"() {
        given:
        createMocks()
        allowAccess()

        def user = user("username", "domainId", "userId")
        def tenantRoles = [ tenantRole("tenantRole", sharedRandom) ].asList()
        def clientRole = clientRole("identity:role", 1000)

        userDao.getUserById(_) >> user
        userDao.getUserByUsername(_) >> user
        tenantDao.getTenantRolesForUser(_) >> tenantRoles
        clientDao.getClientRoleById(_) >> clientRole
        authorizationService.authorizeCloudUserAdmin(_) >> false

        //setup getUser

        when:
        def statuses = []
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, sharedRandom).build().status)

        then:
        for (status in statuses) {
            assert(status == 403)
        }
    }

    def "deleteUserRole returns forbidden"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        def caller = user("caller", "domainId1", "1$sharedRandom")
        userDao.getUserById(_) >>> [ userNotInDomain ] >> defaultUser
        userDao.getUserByUsername(_) >> caller

        authorizationService.authorizeCloudUserAdmin(_) >> true >> false

        // pass getUsersGlobalRoles
        tenantDao.getTenantRolesForUser(defaultUser) >> [ tenantRole("genericTenantRole", "2$sharedRandom") ].asList()
        clientDao.getClientRoleById("2$sharedRandom") >>> [
                clientRole("roleToBeRemoved", 1000),
                clientRole("roleToBeRemoved", 1000),
                clientRole("roleToBeRemoved", 1000),
        ]

        clientRoleDao.getClientRole(_) >>> [
                clientRole("roleToBeRemoved1", 500),
                clientRole("roleToBeRemoved2", 100),
                clientRole("roleToBeRemoved3", 50)
        ]

        // setup precedence cases
        // callers identity role
        clientDao.getClientByClientId(_) >> new Application()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        //fail: UA delete from A and SA
        //fail: A delete from SA
        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                userAdminTenantRole, userAdminTenantRole, adminTenantRole,
                userAdminTenantRole, userAdminTenantRole, adminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(defaultUser, identityRoles) >>> [
                adminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole
        ] >> defaultTenantRole


        when:
        def  statuses = []
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)

        then:
        for (status in statuses) {
            assert(status == 403)
        }
    }

    def "deleteUserRole is successful"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        def caller = user("caller", "domainId1", "1$sharedRandom")
        userDao.getUserById(_) >> defaultUser
        userDao.getUserByUsername(_) >> caller

        authorizationService.authorizeCloudUserAdmin(_) >> true >> false

        // pass getUsersGlobalRoles
        tenantDao.getTenantRolesForUser(defaultUser) >> [ tenantRole("genericTenantRole", "2$sharedRandom") ].asList()
        clientDao.getClientRoleById("2$sharedRandom") >> clientRole("getRole", 1000)

        // setup precedence cases
        // callers identity role
        clientDao.getClientByClientId(_) >> new Application()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        // caller weight
        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                userAdminTenantRole, userAdminTenantRole,
                adminTenantRole, adminTenantRole, adminTenantRole,
                serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole
        ]

        // called weight
        tenantRoleDao.getTenantRoleForUser(defaultUser, identityRoles) >>> [
                defaultTenantRole, userAdminTenantRole,
                defaultTenantRole, userAdminTenantRole, adminTenantRole,
                defaultTenantRole, userAdminTenantRole, adminTenantRole, serviceAdminTenantRole
        ]

        // calledRole
        clientRoleDao.getClientRole(_) >>> [
                defaultUserRole, userAdminRole,
                defaultUserRole, userAdminRole, adminRole,
                defaultUserRole, userAdminRole, adminRole, serviceAdminRole
        ]


        when:
        def statuses = []
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)
        statuses.add(cloud20Service.deleteUserRole(headers, authToken, sharedRandom, "2$sharedRandom").build().status)

        then:
        for (status in statuses) {
            assert(status == 204)
        }
    }

    def "addRole verifies identity:admin level access"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.addRole(headers, uriInfo(), authToken, role())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "addRole returns badRequest"() {
        given:
        createMocks()
        allowAccess()

        when:
        def responses = []
        responses.add(cloud20Service.addRole(headers, uriInfo(), authToken, role()).build())
        responses.add(cloud20Service.addRole(headers, uriInfo(), authToken, null).build())

        then:
        for (response in responses) {
            assert(response.status == 400)
        }
    }

    def "addRole returns conflict"() {
        given:
        createMocks()
        allowAccess()

        clientRoleDao.getClientRoleByApplicationAndName(_, _) >> clientRole()
        clientDao.getClientByClientId(_) >> application()
        def role = role()
        role.name = "genericRole"

        when:
        def response = cloud20Service.addRole(headers, uriInfo(), authToken, role).build()

        then:
        response.status == 409
    }

    def "addRole returns forbidden (insufficient priveleges)"() {
        given:
        createMocks()
        allowAccess()
        def identityRole = role()
        identityRole.name = "identity:hahah"

        authorizationService.verifyServiceAdminLevelAccess(_) >> { throw new ForbiddenException() }


        when:
        def response = cloud20Service.addRole(headers, uriInfo(), authToken, identityRole).build()

        then:
        response.status == 403
    }

    def "addRole sets default weight for created role"() {
        given:
        createMocks()
        allowAccess()
        def role = role()
        role.name = "genericRole"

        clientDao.getClientByClientId(_) >> application()
        clientRoleDao.getNextRoleId() >> "10"

        when:
        def responseOne = cloud20Service.addRole(headers, uriInfo(), authToken, role).build()
        def responseTwo = cloud20Service.addRole(headers, uriInfo(), authToken, role).build()

        then:
        clientRoleDao.addClientRole(_, _) >> { arg1, arg2 ->
            arg2.id == "10"
            arg2.clientId == "1234"
            arg2.name == "genericRole"
            arg2.rsWeight == configuration.getInt("cloudAuth.special.rsWeight")
        }
    }

    def "deleteRole throws bad request; null roleId"() {
        given:
        createMocks()
        allowAccess()

        when:
        def reponse = cloud20Service.deleteRole(headers, authToken, null)

        then:
        reponse.build().status == 400
    }

    def "deleteRole throws bad request; identity role"() {
        given:
        createMocks()
        allowAccess()

        def cRole = identityRole("identity:role", 500, "roleId")

        clientRoleDao.getClientRole(_) >> cRole

        when:
        def response = cloud20Service.deleteRole(headers, authToken, "roleId")

        then:
        response.build().status == 403
    }

    def "deleteRole throws forbidden; insufficent precedence"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        clientRoleDao.getClientRole("roleId") >> clientRole("name", 50)
        userDao.getUserByUsername(_) >> adminUser

        clientDao.getClientByClientId(_) >> application()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_) >> adminTenantRole

        when:
        def response = cloud20Service.deleteRole(headers, authToken, "roleId")

        then:
        response.build().status == 403
    }

    def "deleteRole deletes role"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        clientRoleDao.getClientRole("roleId") >> clientRole("name", 1000)
        userDao.getUserByUsername(_) >>> [ adminUser, serviceAdmin ]

        clientDao.getClientByClientId(_) >> application()

        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [ adminTenantRole, serviceAdminTenantRole ]

        when:
        def responseOne = cloud20Service.deleteRole(headers, authToken, "roleId")
        def responseTwo = cloud20Service.deleteRole(headers, authToken, "roleId")

        then:
        responseOne.build().status == 204
        responseTwo.build().status == 204
    }

    def "adding role to user on tenant returns bad request"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        def caller = user("caller", "domainId1", "1$sharedRandom")

        userDao.getUserById(_) >> defaultUser
        userDao.getUserByUsername(_) >> caller
        tenantDao.getTenant(sharedRandom) >> new Tenant()
        clientRoleDao.getClientRole(_) >>> [
                defaultUserRole,
                userAdminRole,
                adminRole,
                serviceAdminRole
        ]

        when:
        def statuses = []
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)

        then:
        for (status in statuses) {
            assert(status == 403)
        }
    }

    def "adding role to user on tenant returns forbidden (insufficient access)"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        authorizationService.authorizeCloudUserAdmin(_) >> true >> false
        userDao.getUserById(_) >> userNotInDomain >> defaultUser
        userDao.getUserByUsername(_) >> userAdmin
        tenantDao.getTenant(sharedRandom) >> new Tenant()

        clientRoleDao.getClientRole(_) >> clientRole("rolerole", 50)

        clientDao.getClientByClientId(_) >> new Application()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        //setup addee weight
        tenantRoleDao.getTenantRoleForUser(defaultUser, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole
        ] >> defaultTenantRole

        //setup caller weight
        tenantRoleDao.getTenantRoleForUser(userAdmin, identityRoles) >>> [
                defaultTenantRole, userAdminTenantRole, adminTenantRole,
                defaultTenantRole, userAdminTenantRole, adminTenantRole
        ]

        when:
        def statuses = []
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, sharedRandom, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, sharedRandom, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, sharedRandom, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, sharedRandom, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, sharedRandom, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, sharedRandom, sharedRandom, sharedRandom, sharedRandom).build().status)

        then:
        for (status in statuses) {
            assert(status == 403)
        }
    }

    def "adding role to user on tenant succeeds"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        authorizationService.authorizeCloudUserAdmin(_) >> true >> false
        userDao.getUserById(_) >> defaultUser
        userDao.getUserByUsername(_) >> userAdmin
        tenantDao.getTenant(_) >> new Tenant()
        clientDao.getClientByClientId(_) >> application()
        clientDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        clientRoleDao.getClientRole(sharedRandom) >>> [
                clientRole("availableToUserAdmin", 1000),
                clientRole("availableToAdmin", 500),
                clientRole("availableToAdmin", 100),
                clientRole("availableToServiceAdmin", 50)
        ]

        //setup addee weight
        tenantRoleDao.getTenantRoleForUser(defaultUser, identityRoles) >>> [
                defaultTenantRole,
                defaultTenantRole,
                userAdminTenantRole,
                adminTenantRole
        ]
        //setup caller weight
        tenantRoleDao.getTenantRoleForUser(userAdmin, identityRoles) >>> [
                userAdminTenantRole,
                adminTenantRole,
                adminTenantRole,
                serviceAdminTenantRole
        ]

        when:
        def statuses = []
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.addRolesToUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)

        then:
        for (status in statuses) {
            assert(status == 200)
        }
    }

    def "deleting role from user on tenant returns forbidden (insufficient access)"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        tenantDao.getTenant(_) >> new Tenant()
        userDao.getUserById(_) >> userNotInDomain >> defaultUser
        userDao.getUserByUsername(_) >> userAdmin
        authorizationService.authorizeCloudUserAdmin(_) >> true >> false
        clientDao.getClientByClientId(_) >> application()
        clientDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        clientRoleDao.getClientRole(sharedRandom) >>> [
                clientRole("role", 500),
                clientRole("role", 250),
                clientRole("role", 100),
                clientRole("role", 500),
                clientRole("role", 250),
                clientRole("role", 50)
        ]

        //setup caller weight
        tenantRoleDao.getTenantRoleForUser(userAdmin, identityRoles) >>> [
                userAdminTenantRole,
                adminTenantRole,
                userAdminTenantRole,
                userAdminTenantRole,
                userAdminTenantRole,
                adminTenantRole
        ]

        // setup called weight
        tenantRoleDao.getTenantRoleForUser(defaultUser, identityRoles) >>> [
                adminTenantRole,
                serviceAdminTenantRole
        ] >> defaultTenantRole

        when:
        def statuses = []
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)

        then:
        for (status in statuses) {
            assert(status == 403)
        }
    }

    def "deleting role from user on tenant succeeds"() {
        given:
        createMocks()
        allowAccess()
        setupUsersAndRoles()

        tenantDao.getTenant(_) >> new Tenant()
        userDao.getUserById(_) >> defaultUser
        userDao.getUserByUsername(_) >> userAdmin
        authorizationService.authorizeCloudUserAdmin(_) >> true >> false
        clientDao.getClientByClientId(_) >> application()
        clientDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole()
        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        clientRoleDao.getClientRole(sharedRandom) >>> [
                clientRole("role", 1000),
                clientRole("role", 500),
                clientRole("role", 250),
                clientRole("role", 50),
                clientRole("role", 50)
        ]

        //setup caller weight
        tenantRoleDao.getTenantRoleForUser(userAdmin, identityRoles) >>> [
                userAdminTenantRole,
                adminTenantRole,
                adminTenantRole,
                serviceAdminTenantRole,
                serviceAdminTenantRole
        ]

        //setup called weight
        tenantRoleDao.getTenantRoleForUser(defaultUser, identityRoles) >>> [
                defaultTenantRole,
                defaultTenantRole,
                userAdminTenantRole,
                adminTenantRole,
                serviceAdminTenantRole
        ]

        when:
        def statuses = []
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)
        statuses.add(cloud20Service.deleteRoleFromUserOnTenant(headers, authToken, sharedRandom, sharedRandom, sharedRandom).build().status)

        then:
        for (status in statuses) {
            assert(status == 204)
        }
    }

    def "getSecretQA returns 200" () {
        given:
        createMocks()
        allowAccess()

        User user = new User()
        user.id = "1"
        user.username = "name"
        user.secretQuestion = "question"
        user.secretAnswer = "answer"

        userDao.getUserById(_) >> user
        userService.getUser(_) >> user
        userDao.getUserByUsername(_) >> user

        when:
        def responseBuilder = cloud20Service.getSecretQAs(authToken,"1")

        then:
        Response response = responseBuilder.build()
        response.status == 200
    }

    def "createSecretQA returns 200" () {
        given:
        createMocks()
        allowAccess()

        User user = new User()
        user.id = "1"
        user.username = "name"
        user.secretQuestion = "question"
        user.secretAnswer = "answer"
        Question question = new Question()
        question.setQuestion("question")

        userDao.getUserById(_) >> user
        userService.getUser(_) >> user
        userDao.getUserByUsername(_) >> user
        questionDao.getQuestion(_) >>  question

        when:
        def responseBuilder = cloud20Service.createSecretQA(authToken,"1", secretQA("1", "question", "answer"))

        then:
        Response response = responseBuilder.build()
        response.status == 200
    }

    def "Impersonate a disabled user" () {
        given:
        createMocks()
        allowAccessForImpersonate()
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User()
        user.username = "someUser"
        User user1 = new User()
        user1.username = "someUser"
        user1.enabled = false
        userDao.getUserByUsername(_) >> user1
        tenantDao.getTenantRolesForUser(_) >> [tenantRole("identity:default")].asList()
        def mockedClientRole = Mock(ClientRole)
        mockedClientRole.getName() >> "identity:default"
        clientDao.getClientRoleById(_) >> mockedClientRole
        UserScopeAccess userScopeAccess = new UserScopeAccess()
        userScopeAccess.setAccessTokenString("someToken")
        scopeAccessService.updateExpiredUserScopeAccess(_,_) >> userScopeAccess
        ScopeAccess scopeAccess = new UserScopeAccess()
        scopeAccess.setAccessTokenExp(new Date().minus(1))
        scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(_,_) >> scopeAccess

        when:
        def responseBuilder = cloud20Service.impersonate(headers, authToken, impersonation(user))

        then:
        responseBuilder.build().status == 200
    }

    ImpersonationRequest impersonation(org.openstack.docs.identity.api.v2.User user) {
        new ImpersonationRequest().with {
            it.user = user
            return it
        }
    }
//helper methods
    def createMocks() {
        headers = Mock()
        precedenceValidator = Mock()
        scopeAccessDao = Mock()
        scopeAccessService = Mock()
        authorizationService = Mock()
        cloudRegionService = Mock()
        tenantDao = Mock()
        tenantRoleDao = Mock()
        questionDao = Mock()
        capabilityDao = Mock()
        clientDao = Mock()
        clientRoleDao = Mock()
        userDao = Mock()

        cloud20Service.userService = userService


        cloud20Service.scopeAccessService = scopeAccessService
        userService.scopeAccessService = scopeAccessService

        cloud20Service.authorizationService = authorizationService

        userService.cloudRegionService = cloudRegionService

        userService.tenantDao = tenantDao
        tenantService.tenantDao = tenantDao

        userService.tenantRoleDao = tenantRoleDao
        userService.applicationRoleDao = clientRoleDao
        tenantService.tenantRoleDao = tenantRoleDao
        clientService.tenantRoleDao = tenantRoleDao

        questionService.questionDao = questionDao

        capabilityService.ldapCapabilityRepository = capabilityDao

        clientService.applicationDao = clientDao
        tenantService.clientDao = clientDao

        clientService.applicationRoleDao = clientRoleDao

        userService.userDao = userDao
        userService.scopeAccesss = scopeAccessDao
        userService.applicationRoleDao = clientRoleDao

        ldapPatternRepository = Mock()
        validator.ldapPatternRepository = ldapPatternRepository

    }

    def setupUsersAndRoles() {
        defaultUserRole = identityRole("identity:default", configuration.getInt("cloudAuth.defaultUser.rsWeight"), defaultRoleId)
        userAdminRole = identityRole("identity:user-admin", configuration.getInt("cloudAuth.userAdmin.rsWeight"), userAdminRoleId)
        specialRole = identityRole("specialRole", configuration.getInt("cloudAuth.special.rsWeight"), "")
        adminRole = identityRole("identity:admin", configuration.getInt("cloudAuth.admin.rsWeight"), adminRoleId)
        serviceAdminRole = identityRole("identity:service-admin", configuration.getInt("cloudAuth.serviceAdmin.rsWeight"), serviceAdminRoleId)
        genericRole = clientRole("genericRole", 500)

        userAdmin = user("user-admin", "domainId1", sharedRandom)
        defaultUser = user("default-user", "domainId1", sharedRandom)
        userNotInDomain = user("default-user", "domainId2", sharedRandom)
        adminUser = user("admin", "", sharedRandom)
        serviceAdmin = user("service-admin", "", sharedRandom)

        defaultTenantRole = tenantRole("identity:default", defaultRoleId)
        userAdminTenantRole = tenantRole("identity:user-admin", userAdminRoleId)
        adminTenantRole = tenantRole("identity:admin", adminRoleId)
        serviceAdminTenantRole = tenantRole("identity:service-admin", serviceAdminRoleId)

        identityRoles = new ArrayList<ClientRole>()
        identityRoles.add(defaultUserRole);
        identityRoles.add(userAdminRole);
        identityRoles.add(adminRole);
        identityRoles.add(serviceAdminRole);
    }

    def mockScopeAccess() {
        ScopeAccess scopeAccess = Mock()

        def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
        def entry = new ReadOnlyEntry("DN", attribute)

        scopeAccess.getLDAPEntry() >> entry
    }

    def allowAccess() {
        def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
        def entry = new ReadOnlyEntry("DN", attribute)

        ClientScopeAccess clientScopeAccess = Mock()
        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        clientScopeAccess.accessTokenExp = calendar.getTime()
        clientScopeAccess.accessTokenString = calendar.getTime().toString()

        clientScopeAccess.getLDAPEntry() >> entry
        scopeAccessService.getScopeAccessByAccessToken(_) >> clientScopeAccess
    }

    def allowAccessForImpersonate() {
        def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
        def entry = new ReadOnlyEntry("DN", attribute)

        ClientScopeAccess clientScopeAccess = Mock()
        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        clientScopeAccess.accessTokenExp = calendar.getTime()
        clientScopeAccess.accessTokenString = calendar.getTime().toString()

        clientScopeAccess.getLDAPEntry() >> entry
        UserScopeAccess userScopeAccess = new UserScopeAccess()
        scopeAccessService.getScopeAccessByAccessToken(_) >> clientScopeAccess >> userScopeAccess
    }

    def user(String username, String email, Boolean enabled, String displayName) {
        new User().with {
            it.uniqueId = "some dn"
            it.username = username
            it.email = email
            it.enabled = enabled
            it.displayName = displayName
            return it
        }
    }

    def user(name, domainId) {
        new User().with {
            it.uniqueId = "some dn"
            it.username = name
            it.domainId = domainId
            it.enabled = true
            return it
        }
    }

    def user(name, domainId, userId) {
        new User().with {
            it.id = userId
            it.domainId = domainId
            it.username = name
            it.enabled = true
            it.uniqueId = "some dn"
            return it
        }
    }

    def region() {
        new Region().with {
            it.cloud = "US"
            it.name = "DFW"
            it.isEnabled = true
            it.isDefault = true
            return it
        }
    }

    def scopeAccess() {
        new ClientScopeAccess().with {
            Calendar calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            it.accessTokenExp = calendar.getTime()
            it.accessTokenString = calendar.getTime().toString()
            return it
        }
    }

    def jaxbQuestion() {
        return jaxbQuestion("id", "question")
    }

    def jaxbQuestion(String id, String question) {
        objFactories.getRackspaceIdentityExtRaxgaV1Factory().createQuestion().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def question() {
        question("id", "question")
    }

    def question(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def getCapability(String action, String id, String name, String description, String url, List<String> resources) {
        new Capability().with {
            it.action = action
            it.id = id
            it.name = name
            it.description = description
            it.url = url
            for(String list : resources){
                it.resources.add(list)
            }
            return it
        }
    }

    def uriInfo() {
        UriInfo uriInfo = Mock()
        UriBuilder uriBuilder = Mock()
        uriInfo.getRequestUriBuilder() >> uriBuilder
        uriInfo.getAbsolutePath() >> new URI("http://path.to/resource")
        uriBuilder.path(_) >> uriBuilder
        uriBuilder.build() >> new URI()
        return uriInfo
    }

    def userContext(offset, limit, list) {
        new PaginatorContext<User>().with {
            it.limit = limit
            it.offset = offset
            it.totalRecords = list.size
            it.valueList = list
            return it
        }
    }

    def role() {
        new Role().with {
            return it
        }
    }

    def clientRole() {
        new ClientRole().with {
            it.clientId = "1234"
            it.id = "1234"
            it.name = "testRole"
            return it
        }
    }

    def clientRole(String name, int rsWeight) {
        new ClientRole().with {
            it.clientId = "1234"
            it.id = "1234"
            it.name = name
            it.rsWeight = rsWeight
            return it
        }
    }

    def identityRole(String name, int rsWeight, String id) {
        new ClientRole().with {
            it.id = id
            it.name = name
            it.rsWeight = rsWeight
            return it
        }
    }

    def tenantRole(String name) {
        new TenantRole().with {
            it.name = name
            it.roleRsId = "roleRsId"
            return it
        }
    }

    def tenantRole(String name, String roleRsId) {
        new TenantRole().with {
            it.name = name
            it.roleRsId = roleRsId
            return it
        }
    }

    def application() {
        new Application().with() {
            it.clientId = "1234"
            it.enabled = true
            return it
        }
    }

    def secretQA(String id, String question, String answer) {
        new SecretQA().with {
            it.id = id
            it.question = question
            it.answer = answer
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
    */
}
