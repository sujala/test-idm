package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ApprovedDomainIds
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.EmailDomains
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.converter.cloudv20.*
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.entity.User.UserType
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.FederatedIdentityService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.exception.*
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import com.rackspace.idm.validation.Cloud20CreateUserValidator
import com.rackspace.idm.validation.Validator20
import com.unboundid.ldap.sdk.DN
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.dozer.DozerBeanMapper
import org.joda.time.DateTime
import org.opensaml.core.config.InitializationService
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.*
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.xml.bind.JAXBElement

import static org.apache.http.HttpStatus.*

class DefaultCloud20ServiceTest extends RootServiceTest {

    @Shared JAXBObjectFactories objFactories
    @Shared DefaultCloud20Service service

    @Shared Validator20 realValidator20

    @Shared ScopeAccess scopeAccessMock

    @Shared def offset = 0
    @Shared def limit = 25
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def questionId = "id"
    @Shared def roleId = "roleId"
    @Shared def userId = "userId"

    @Shared def identityAdmin, userAdmin, userManage, defaultUser

    def setupSpec() {
        InitializationService.initialize()
        sharedRandom = ("$sharedRandomness").replace('-',"")
    }

    def setup() {
        //service being tested
        service = new DefaultCloud20Service()

        realValidator20 = new Validator20()

        objFactories = new JAXBObjectFactories()
        service.jaxbObjectFactories = objFactories

        exceptionHandler = new ExceptionHandler()
        exceptionHandler.objFactories = objFactories
        service.exceptionHandler = exceptionHandler

        mockServices()
        mockMisc()
        service.requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(_) >> new UserScopeAccess()

        headers = Mock(HttpHeaders)
        jaxbMock = Mock(JAXBElement)
    }

    def "addEndpointTemplate handles DuplicateException"() {
        given:
        allowUserAccess()
        def endpointTemplate = entityFactory.createEndpointTemplate("name")
        endpointTemplate.type = "type"
        def endpointConverter = Mock(EndpointConverterCloudV20)
        service.endpointConverterCloudV20 = endpointConverter
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createCloudBaseUrl().with {
            it.openstackType = "type"
            return it
        }
        applicationService.checkAndGetApplicationByName(_) >> new Application().with {
            it.openStackType = "type"
            return it
        }
        endpointService.addBaseUrl(_) >> {throw new DuplicateException()}

        when:
        def response = service.addEndpointTemplate(null, null, authToken, endpointTemplate)

        then:
        response.build().getStatus() == 409
    }

    def "addEndpointTemplate handles Missing Name Attribute"() {
        given:
        allowUserAccess()
        def endpointTemplate = entityFactory.createEndpointTemplate(null)
        def endpointConverter = Mock(EndpointConverterCloudV20)
        service.endpointConverterCloudV20 = endpointConverter
        service.validator20 = realValidator20

        reloadableConfig.getFeatureEndpointTemplateDisableNameType() >> false
        mockIdentityConfig(service.validator20)

        when:
        def response = service.addEndpointTemplate(null, null, authToken, endpointTemplate)

        then:
        response.build().getStatus() == SC_BAD_REQUEST

        cleanup:
        service.validator20 = validator20
    }

    def "question create verifies Identity admin level access and adds Question"() {
        given:
        mockQuestionConverter(service)

        when:
        def response = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion()).build()

        then:
        response.getStatus() == 201

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * questionService.addQuestion(_) >> "questionId"
        response.getMetadata().get("location")[0] != null
    }

    def "question create handles exceptions"() {
        given:
        mockQuestionConverter(service)

        when:
        def response1 = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}
        0 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)

        when:
        def response2 = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())

        then:
        response2.build().status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN) >> {throw new ForbiddenException()}

        when:
        def response3 = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())

        then:
        response3.build().status == 400

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * questionService.addQuestion(_) >> {throw new BadRequestException()}
    }

    def "question delete verifies Identity admin level access and deletes question"() {
        given:
        mockQuestionConverter(service)

        when:
        def response = service.deleteQuestion(authToken, questionId).build()

        then:
        response.getStatus() == 204

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * questionService.deleteQuestion(questionId)
    }

    def "question delete handles exceptions"() {
        given:
        mockQuestionConverter(service)

        when:
        def response1 = service.deleteQuestion(authToken, questionId).build()

        then:
        response1.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}
        0 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)

        when:
        def response2 = service.deleteQuestion(authToken, questionId).build()

        then:
        response2.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN) >> {throw new ForbiddenException()}

        when:
        def response3 = service.deleteQuestion(authToken, questionId).build()

        then:
        response3.status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * questionService.deleteQuestion(questionId) >> { throw new NotFoundException() }
    }

    def "question update verifies Identity admin level access"() {
        given:
        mockQuestionConverter(service)

        when:
        def response = service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion())

        then:
        response.build().status == 204

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "question update updates question"() {
        given:
        mockQuestionConverter(service)

        when:
        def response = service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion()).build()

        then:
        response.status == 204

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * questionService.updateQuestion(questionId, _)
    }

    def "question update handles exceptions"() {
        given:
        mockQuestionConverter(service)

        when:
        def response1 = service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}
        0 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)

        when:
        def response2 = service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())

        then:
        response2.build().status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN) >> {throw new ForbiddenException()}

        when:
        def response3 = service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())

        then:
        response3.build().status == 400

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * questionService.updateQuestion(sharedRandom, _) >> {throw new BadRequestException()}

        when:
        def response4 = service.updateQuestion(authToken, "1$sharedRandom", entityFactory.createJAXBQuestion())

        then:
        response4.build().status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * questionService.updateQuestion("1$sharedRandom", _) >> { throw new NotFoundException() }
    }

    def "question(s) get verifies user level access"() {
        given:
        mockQuestionConverter(service)

        when:
        service.getQuestion(authToken, questionId)
        service.getQuestions(authToken)

        then:
        2 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
    }

    def "question(s) get gets question and returns it (them)"() {
        given:
        mockQuestionConverter(service)

        def questionList = [
                entityFactory.createQuestion("1", "question1"),
                entityFactory.createQuestion("2", "question2")
        ].asList()

        when:
        def response1 = service.getQuestion(authToken, questionId).build()
        def response2 = service.getQuestions(authToken).build()

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

    def "question get handles exceptions"() {
        given:
        mockQuestionConverter(service)

        when:
        def questionResponse1 = service.getQuestion(authToken, questionId).build()

        then:
        questionResponse1.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}
        0 * requestContext.verifyEffectiveCallerIsNotAFederatedUserOrRacker()
        0 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)

        when:
        def questionResponse2 = service.getQuestion(authToken, questionId).build()

        then:
        questionResponse2.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotAFederatedUserOrRacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER) >> {throw new ForbiddenException()}

        when:
        def questionResponse3 = service.getQuestion(authToken, "1$questionId").build()

        then:
        questionResponse3.status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotAFederatedUserOrRacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * questionService.getQuestion("1$questionId") >> {throw new NotFoundException()}
    }

    def "questions get handles exceptions"() {
        when:
        def questionsResponse1 = service.getQuestions(authToken).build()

        then:
        questionsResponse1.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}
        0 * requestContext.verifyEffectiveCallerIsNotAFederatedUserOrRacker()
        0 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)

        when:
        def questionsResponse2 = service.getQuestions(authToken).build()

        then:
        questionsResponse2.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotAFederatedUserOrRacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER) >> {throw new ForbiddenException()}
    }

    def "assignRoleToUser provisions role and adds role to user"() {
        given:
        def user = entityFactory.createUser()
        def role = Mock(ClientRole)

        when:
        service.assignRoleToUser(user, role)

        then:
        1 * role.getClientId()
        1 * role.getName()
        1 * role.getId()

        then:
        1 * tenantService.addTenantRoleToUser(user, _, true)
    }

    @Unroll
    def "listUsers (defaultUser caller) verifies token and caller "() {
        given:
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.DEFAULT_USER

        when:
        def response = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

        response.status == 200
    }

    @Unroll
    def "listUsers (identity admin or service admin) gets own domain users when enabled: #userType"() {
        given:
        reloadableConfig.getTenantDefaultDomainId() >> "default"
        User user = entityFactory.createUser()

        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> userType

        def userContextMock = Mock(PaginatorContext)
        userContextMock.getValueList() >> [].asList()

        when:
        def response = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        1 * reloadableConfig.getTenantDefaultDomainId() >> "123"
        1 * identityUserService.getEndUsersPaged(_) >> userContextMock
        1 * idmPathUtils.createLinkHeader(_, _) >> "link header"

        response.status == 200
        response.getMetadata().get("Link").get(0) == "link header"

        where:
        userType << [IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityUserTypeEnum.SERVICE_ADMIN]
    }

    def "listUsers (user-admin) gets own domain users"() {
        given:
        reloadableConfig.getTenantDefaultDomainId() >> "default"

        User user = entityFactory.createUser().with {
            it.domainId = "aDomain"
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> user

        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        def userContextMock = Mock(PaginatorContext)
        userContextMock.getValueList() >> [].asList()
        ListUsersSearchParams params = new ListUsersSearchParams().with {
            it.userType = UserType.VERIFIED.name()
            it
        }

        when:
        def response = service.listUsers(headers, uriInfo(), authToken, params).build()

        then:
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRoles(IdentityUserTypeEnum.DEFAULT_USER, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.roleName)
        (1.._) * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.roleName) >> false
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(_)

        1 * identityUserService.getEndUsersPaged(params) >> userContextMock
        1 * idmPathUtils.createLinkHeader(_, _) >> "link header"

        and:
        response.status == 200
        response.getMetadata().get("Link").get(0) == "link header"
    }

    @Unroll
    def "listUsers: when caller in default domain, list limited to own user when enabled for caller: #callerType"() {
        given:
        reloadableConfig.getTenantDefaultDomainId() >> "default"

        User user = entityFactory.createUser().with {
            it.domainId = "default"
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> user
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN

        when:
        def response = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        0 * identityUserService.getEndUsersPaged(_, _)
        0 * idmPathUtils.createLinkHeader(_, _) >> "link header"
        1 * userConverter.toUserList(_) >> { args ->
            def userList = args[0]
            assert userList.size() == 1
            assert userList.get(0).id == user.id
        }
        response.status == 200

        where:
        callerType << [ IdentityUserTypeEnum.SERVICE_ADMIN,  IdentityUserTypeEnum.IDENTITY_ADMIN
                        ,  IdentityUserTypeEnum.USER_ADMIN,  IdentityUserTypeEnum.USER_MANAGER,
                        IdentityUserTypeEnum.DEFAULT_USER]
    }

    def "listUsers (user-manage) gets own domain users"() {
        given:
        reloadableConfig.getTenantDefaultDomainId() >> "default"
        User user = entityFactory.createUser()

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> user
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER

        def userContextMock = Mock(PaginatorContext)
        userContextMock.getValueList() >> [].asList()

        when:
        def response = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        1 * reloadableConfig.getTenantDefaultDomainId() >> "123"
        1 * identityUserService.getEndUsersPaged(_) >> userContextMock
        1 * idmPathUtils.createLinkHeader(_, _) >> "link header"

        and:
        response.status == 200
        response.getMetadata().get("Link").get(0) == "link header"
    }

    def "listUsers handles exceptions"() {
        given:
        reloadableConfig.getTenantDefaultDomainId() >> "default"

        mockUserConverter(service)
        def callerToken = entityFactory.createUserToken()

        when: "Token not valid"
        def response1 = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> { throw new NotAuthorizedException()}
        response1.status == 401

        when: "Caller not found"
        def response2 = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> callerToken
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> { throw new NotFoundException()}
        response2.status == 404

        when: "Caller not authorized"
        def response3 = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> callerToken
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> {entityFactory.createUser()}
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRoles(_, _) >> {throw new ForbiddenException()}
        response3.status == 403

        when: "Caller can view all domain users but doesn't have a domain"
        def response4 = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams()).build()

        then:
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> callerToken
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser("username", null, null, "region")
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        reloadableConfig.getTenantDefaultDomainId() >> "123"
        response4.status == 400

        when: "caller not authorized to use contactId"
        def response5 = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams(null, null, null, null, null, null, "contactId", null)).build()

        then:
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> callerToken
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser("username", null, null, "region")
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        reloadableConfig.getTenantDefaultDomainId() >> "123"
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName()) >> false
        response5.status == SC_FORBIDDEN

        when: "using both 'admin_only' and `contact_id`"
        def response6 = service.listUsers(headers, uriInfo(), authToken, new ListUsersSearchParams(null, null, null, null, true, null, "contactId", null)).build()

        then:
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> callerToken
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser("username", null, null, "region")
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        reloadableConfig.getTenantDefaultDomainId() >> "123"
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName()) >> true
        response6.status == SC_BAD_REQUEST
    }

    def "listUsersWithRole verifies user admin level access"() {
        given:
        mockUserConverter(service)

        def contextMock = Mock(PaginatorContext)
        contextMock.getValueList() >> [].asList()

        userService.getUsersWithRole(_, _, _) >> contextMock
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN)
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN
    }

    def "listUsersWithRole (user admin caller) filters by domain and gets users"() {
        given:
        mockUserConverter(service)
        mockUserPaginator(service)
        def domain = "callerDomain"

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >>  entityFactory.createUser("caller", null, domain, "region")
        userPaginator.createLinkHeader(_, _) >> "link header"

        def contextMock = Mock(PaginatorContext)
        contextMock.getValueList() >> [].asList()

        when:
        def response = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        userService.getUsersWithDomainAndRole(_, _, _, _) >> { arg1, arg2, arg3, arg4 ->
            assert(arg1.equals(domain))
            return contextMock
        }

        response.getMetadata().get("Link") != null
        response.status == 200
    }

    def "listUsersWithRole (admin or service admin) filters only by role and gets users"() {
        given:
        mockUserConverter(service)
        mockUserPaginator(service)
        allowUserAccess()

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole(null).with {
            it.id = roleId
            return it
        }
        userPaginator.createLinkHeader(_, _) >> "link header"

        def contextMock = Mock(PaginatorContext)
        contextMock.getValueList() >> [].asList()

        when:
        def response = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        userService.getUsersWithRole(_, _, _) >> { arg1, arg2, arg3 ->
            assert(arg1.equals(roleId))
            return contextMock
        }

        response.getMetadata().get("Link") != null
        response.status == 200
    }

    def "listUsersWithRole handles exceptions"() {
        given:
        mockUserConverter(service)
        mockUserPaginator(service)

        def caller =  entityFactory.createUser("caller", null, null, "region")

        when:
        def response1 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        response1.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}

        when:
        def response2 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        response2.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN) >> {throw new ForbiddenException()}

        when:
        def response3 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        response3.status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN)
        1 * applicationService.getClientRoleById(roleId) >>  null

        when:
        caller.domainId = null
        def response4 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        response4.status == 400

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN)
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * applicationService.getClientRoleById(roleId) >> entityFactory.createClientRole()
    }

    def "addUserRole verifies user-admin access and role to add"() {
        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        1 * userService.checkAndGetUserById(userId) >> entityFactory.createUser()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * applicationService.getClientRoleById(roleId) >> entityFactory.createClientRole(null)
    }

    def "addUserRole verifies caller precedence over user to modify and role to add"() {
        given:
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def roleToAdd = entityFactory.createClientRole(null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(_, roleToAdd)
    }

    def "addUserRole checks for existing identity:* role"() {
        given:
        def user = entityFactory.createUser()
        user.id = "somethingdifferentfromcaller"
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(null)

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * tenantService.getGlobalRolesForUser(_)
        1 * applicationService.getClientRoleById(roleId) >> roleToAdd

    }

    def "addUserRole verifies user is within caller domain when caller is user-admin"() {
        given:
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def roleToAdd = entityFactory.createClientRole(null)

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 *applicationService.getClientRoleById(roleId) >> roleToAdd
    }

    def "addUserRole adds role"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "somethingdifferentfromcaller"
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(null)

        when:
        def response = service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        response.status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * applicationService.getClientRoleById(roleId) >> roleToAdd
        1 * tenantService.addTenantRoleToUser(user, _, true)
    }

    def "addUserRole not allowed without scopeAccess"() {
        when:
        def response = service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        response.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}
    }

    def "addUserRole not allowed without precedence"() {
        given:
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def roleToAdd = entityFactory.createClientRole(null)

        when:
        def response = service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        response.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * applicationService.getClientRoleById(roleId) >> roleToAdd
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> { throw new ForbiddenException() }
    }

    def "addUserRole returns not found when user does not exist"() {
        given:
        def roleToAdd = entityFactory.createClientRole(null)

        scopeAccessService.getScopeAccessByAccessToken(_) >> Mock(ScopeAccess)
        userService.checkAndGetUserById(("1$userId")) >> { throw new NotFoundException()}
        applicationService.getClientRoleById(roleId) >> roleToAdd

        when:
        def response = service.addUserRole(headers, authToken, "1$userId", roleId).build()

        then:
        response.status == 404
    }

    def "deleteUserRole verifies userAdmin level access"() {
        when:
        service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
    }

    def "deleteUserRole verifies user to modify is within callers domain when caller is user-admin"() {
        given:
        def user = entityFactory.createUser()
        def caller = Mock(User)

        when:
        service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(_) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
    }

    def "deleteUserRole gets users globalRoles"() {
        given:
        def user = entityFactory.createUser()
        user.id = "someotherid"
        def caller = entityFactory.createUser()

        when:
        service.deleteUserRole(headers, authToken, user.id, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * tenantService.getGlobalRolesForUser(user) >> [ entityFactory.createTenantRole("name") ].asList()

    }

    def "deleteUserRole prevents a user from deleting their own role"() {
        given:
        def user = entityFactory.createUser()

        when:
        def response = service.deleteUserRole(headers, authToken, user.id, roleId).build()

        then:
        response.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(user.id) >> user
    }

    def "deleteUserRole verifies callers precedence over user and role to be deleted"() {
        given:
        def tenantRole = entityFactory.createTenantRole("name").with {
            it.roleRsId = roleId
            return it
        }

        def user = entityFactory.createUser()
        user.id = "differentid"
        def caller = entityFactory.createUser()

        when:
        service.deleteUserRole(headers, authToken, user.id, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * tenantService.getGlobalRolesForUser(_) >> [ tenantRole ].asList()
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(_, _)

    }

    def "deleteUserRole deletes role"() {
        given:
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = roleId
            return it
        }

        def user = entityFactory.createUser()
        user.id = "differentid"
        def caller = entityFactory.createUser()

        when:
        def response = service.deleteUserRole(headers, authToken, user.id, roleId).build()

        then:
        response.status == 204

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        1 * tenantService.getGlobalRolesForUser(_) >> [ tenantRole ].asList()
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(_, _)
        1 * tenantService.deleteTenantRoleForUser(_, _, true)
    }

    def "deleteUserRole handles exceptions"() {
        given:
        def user = entityFactory.createUser()
        user.id = "someotherid"
        def caller = entityFactory.createUser()

        when:
        def response1 = service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        response1.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}

        when:
        def response2 = service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        response2.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER) >> {throw new ForbiddenException()}

        when:
        def response3 = service.deleteUserRole(headers, authToken, "1$userId", roleId).build()

        then:
        response3.status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(_) >> {throw new NotFoundException()}

        when:
        def response4 = service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        response4.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(_) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_) >> {throw new ForbiddenException()}
    }


    def "addRole verifies admin access"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()

        when:
        service.addRole(headers, uriInfo(), authToken, v2Factory.createRole())

        then:
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "addRole verifies racker does not have access"() {
        given:
        def token = "rackerToken"
        def sa = new RackerScopeAccess().with {
            it.accessTokenString = token
            it.accessTokenExp = new Date().plus(1)
            it
        }
        mockRoleConverter(service)
        mockScopeAccessService(service)

        when:
        def response = service.addRole(headers, uriInfo(), token, v2Factory.createRole())

        then:
        response.build().status == 403
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN) >> { throw new ForbiddenException() }
    }

    def "addRole calls validateRoleForCreation"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()

        def roleMock = Mock(Role)
        roleMock.getServiceId() == ''
        roleMock.getName() >> "name"

        when:
        service.addRole(headers, uriInfo(), authToken, roleMock)

        then:
        1 * validator20.validateRoleForCreation(roleMock)
    }

    def "addRole gets service"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()
        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.IDENTITY_ADMIN

        when:
        service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", "serviceId", null))

        then:
        1 * applicationService.checkAndGetApplication("serviceId")
    }

    def "addRole adds role and returns location"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()
        applicationService.checkAndGetApplication(_) >> entityFactory.createApplication()
        requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken) >> createUserScopeAccess()
        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.IDENTITY_ADMIN

        when:
        def response = service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", null, null)).build()

        then:
        1 * applicationService.addClientRole(_)
        response.status == 201
        response.getMetadata().get("location") != null
    }

    def "addRole calls validator to validate role for creation"() {
        when:
        def response = service.addRole(headers, uriInfo(), authToken, null).build()

        then:
        1 * validator20.validateRoleForCreation(_)
    }

    def "deleteRole verifies admin level access"() {
        when:
        service.deleteRole(headers, authToken, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "deleteRole verifies callers precedence over role to be deleted"() {
        given:
        reloadableConfig.getDeleteRoleAssignedToUser() >> true
        roleService.isRoleAssigned(_) >> false
        allowUserAccess()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        service.deleteRole(headers, authToken, roleId)

        then:
        1 * userService.getUserByAuthToken(_) >> entityFactory.createUser()
        1 * precedenceValidator.verifyCallerRolePrecedence(_, _)
    }

    def "deleteRole deletes role"() {
        given:
        allowUserAccess()
        reloadableConfig.getDeleteRoleAssignedToUser() >> false
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = service.deleteRole(headers, authToken, roleId).build()

        then:
        1 * applicationService.deleteClientRole(_)
        response.status == 204
    }

    def "deleteRole handles exceptions"() {
        given:
        def role = entityFactory.createClientRole("identity:role").with {
            it.id = "unique"
            return it
        }

        applicationService.getClientRoleById("unique") >> role
        roleService.isRoleAssigned(role.id) >> true

        when: "not authorized"
        def response1 = service.deleteRole(headers, authToken, roleId).build()

        then:
        response1.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}

        when: "forbidden"
        def response2 = service.deleteRole(headers, authToken, roleId).build()

        then:
        response2.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN) >> {throw new ForbiddenException()}

        when: "bad request"
        def response3 = service.deleteRole(headers, authToken, null).build()

        then:
        response3.status == 400

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)

        when: "forbidden"
        def response4 = service.deleteRole(headers, authToken, "unique").build()

        then:
        response4.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * applicationService.getClientRoleById("unique") >> role
        1 * roleService.isRoleAssigned(role.id) >> true
    }

    def "deleteRole handles exceptions when role is assigned to a provisioned/federated user"() {

        given:
        reloadableConfig.getDeleteRoleAssignedToUser() >> true
        allowUserAccess()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        roleService.isRoleAssigned(_) >> true

        when:
        def response1 = service.deleteRole(headers, authToken, roleId).build()

        then:
        response1.status == 403
    }

    def "addRolesToUserOnTenant verifies user-manage level access"() {
        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker();
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
    }

    def "addRolesToUserOnTenant verifies tenant access"() {
        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker();
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
    }

    def "addRolesToUserOnTenant verifies tenant"() {
        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker();
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
    }

    def "addRolesToUserOnTenant verifies user"() {
        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker();
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
        1 * userService.checkAndGetUserById("userId")
    }

    def "addRolesToUserOnTenant verifies that user to modify is within callers domain when caller is user-admin"() {
        given:
        def user = entityFactory.createUser()

        when:
        def response = service.addRolesToUserOnTenant(headers, authToken, "tenantId", "id", "role1").build()

        then:
        response.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
        1 * userService.checkAndGetUserById("id") >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}
    }

    def "addRolesToUserOnTenant verifies role is not a user type role"() {
        given:
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()

        when:
        def response = service.addRolesToUserOnTenant(headers, authToken, "tenantId", "id", "role1").build()

        then:
        response.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker();
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >>  caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
        1 * userService.checkAndGetUserById("id") >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * applicationService.getClientRoleById(_) >> entityFactory.createClientRole("identity:user-admin")
    }

    def "addRolesToUserOnTenant verifies callers precedence"() {
        given:
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def role = entityFactory.createClientRole()


        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "id", "role1").build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker();
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >>  caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
        1 * userService.checkAndGetUserById("id") >> user
        1 * applicationService.getClientRoleById(_) >> role
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(IdentityUserTypeEnum.USER_ADMIN, role)
    }

    def "addRolesToUserOnTenant adds role to user on tenant"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()
        def role = entityFactory.createClientRole()

        when:
        def response = service.addRolesToUserOnTenant(headers, authToken, "tenantId", "id", "role1").build()

        then:
        response.status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker();
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >>  caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId") >> entityFactory.createTenant()
        1 * userService.checkAndGetUserById("id") >> user
        1 * applicationService.getClientRoleById(_) >> role
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(IdentityUserTypeEnum.USER_ADMIN, role)
        1 * tenantService.addTenantRoleToUser(user, _, true)
    }

    def "deleteRoleFromUserOnTenant verifies user manage level access"() {
        when:
        def response1 = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        response1.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> {throw new NotAuthorizedException()}

        when:
        def response2 = service.deleteRoleFromUserOnTenant(headers, "1$authToken", "tenant1", "user1", "role1").build()

        then:
        response2.status == 403

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER) >> { throw new ForbiddenException() }
    }

    def "deleteRoleFromUserOnTenant verifies tenant access"() {
        when:
        service.deleteRoleFromUserOnTenant(headers, authToken, "tenantId", "user1", "role1")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
    }

    def "deleteRoleFromUserOnTenant verifies user"() {
        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenantId", "user1", "role1").build()

        then:
        response.status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * userService.checkAndGetUserById("user1") >> { throw new NotFoundException() }
    }

    def "deleteRoleFromUserOnTenant verifies tenant"() {
        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenantId", "user1", "role1").build()

        then:
        response.status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant(_) >> { throw new NotFoundException() }
    }

    def "deleteRoleFromUserOnTenant verifies user belongs to callers domain when caller is user admin"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenantId", "id", "role1").build()

        then:
        response.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
        1 * userService.checkAndGetUserById("id") >> user
        1 * applicationService.getClientRoleById("role1") >> entityFactory.createClientRole()
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}
    }

    def "deleteRoleFromUserOnTenant verifies role"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenantId", "id", "roleId").build()

        then:
        response.status == 404

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
        1 * userService.checkAndGetUserById("id") >> user
        1 * applicationService.getClientRoleById("roleId") >> {throw new NotFoundException()}
    }

    def "deleteRoleFromUserOnTenant verifies caller precedence"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        when:
        service.deleteRoleFromUserOnTenant(headers, authToken, "tenantId", "id", "roleId").build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId")
        1 * userService.checkAndGetUserById("id") >> user
        1 * applicationService.getClientRoleById("roleId") >> entityFactory.createClientRole()
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(IdentityUserTypeEnum.USER_ADMIN, _)
    }

    def "deleteRoleFromUserOnTenant deletes role from user"() {
        given:
        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant().with {
            it.tenantId = "tenantId"
            it
        }
        def clientRole = entityFactory.createClientRole()
        def tenantRole = entityFactory.createTenantRole().with {
            it.tenantIds.add("tenantId")
            it
        }

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenantId", "id", "roleId").build()

        then:
        response.status == 204

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess("tenantId")
        1 * tenantService.checkAndGetTenant("tenantId") >> tenant
        1 * userService.checkAndGetUserById("id") >> user
        1 * applicationService.getClientRoleById("roleId") >> clientRole
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(IdentityUserTypeEnum.USER_ADMIN, _)
        1 * tenantService.checkAndGetTenantRoleForUserById(user, "roleId") >>  tenantRole
        1 * tenantService.deleteTenantOnRoleForUser(user, _, _)
    }

    def "getSecretQA - return valid response"() {
        given:
        mockSecretQAConverter(service)
        def user = entityFactory.createUser()

        when:
        def response = service.getSecretQA(headers, authToken, "id").build()

        then:
        response.status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled();
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);
        1 * userService.checkAndGetUserById(_) >> user
    }

    def "getSecretQAs returns 200"() {
        given:
        mockSecretQAConverter(service)
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "id"

        when:
        def response = service.getSecretQAs(authToken,"id").build()

        then:
        response.status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotAFederatedUserOrRacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.isSelf(_, _) >> true
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);
        1 * secretQAService.getSecretQAs(_) >> entityFactory.createSecretQAs()
    }

    def "createSecretQA returns 200"() {
        given:
        mockSecretQAConverter(service)
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "id"

        userService.getUser(_) >> user
        userService.checkAndGetUserById(_) >> user
        identityUserService.getProvisionedUserById(_) >> user

        when:
        def response = service.createSecretQA(authToken,"1", v1Factory.createSecretQA()).build()

        then:
        response.status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotAFederatedUserOrRacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.isSelf(_, _) >> true
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);
        1 * secretQAService.addSecretQA("1", _)
    }


    def "Impersonate a disabled user"() {
        given:
        mockAuthConverterCloudV20(service)

        def v20user = v2Factory.createUser()
        v20user.username = "impersonatingUsername"
        def impRequest = v1Factory.createImpersonationRequest(v20user)
        def entityUser = entityFactory.createUser("impersonatedUser", null, null, "region").with {
            it.enabled = false
            return it
        }

        def callerToken =  new UserScopeAccess().with {
            it.accessTokenExp = new DateTime().plusDays(1).toDate()
            it.accessTokenString = "token"
            return it
        }

        def impersonatedToken = new ImpersonatedScopeAccess().with {
            return it
        }

        userService.checkAndGetUserByName(_) >> entityUser
        tenantService.getGlobalRolesForUser(entityUser) >> [
                entityFactory.createTenantRole("identity:default")
        ].asList()

        when:
        def responseBuilder = service.impersonate(headers, authToken, impRequest)

        then:
        responseBuilder.build().status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * securityContext.getEffectiveCallerToken() >> callerToken
        1 * authorizationService.verifyEffectiveCallerCanImpersonate()
        1 * scopeAccessService.processImpersonatedScopeAccessRequest(_, _, _, _, _) >> impersonatedToken
    }

    def "authenticateFederatedDomain throws BadRequest if domain is invalid"() {
        given:
        def authRequest = createAuthenticationRequest(false)
        def domain = v1Factory.createDomain("id", "notRACKSPACE")

        when:
        service.authenticateFederatedDomain(authRequest, domain)

        then:
        thrown(BadRequestException)
    }

    def "authenticateFederatedDomain handles authentication with password credentials"() {
        given:
        mockDomainConverter(service)
        mockAuthConverterCloudV20(service)

        def authRequest = createAuthenticationRequest(false)
        def domain = v1Factory.createDomain("id", "RACKSPACE")
        def racker = entityFactory.createRacker()
        def authResult = entityFactory.createUserAuthenticationResult(racker, true)

        when:
        service.authenticateFederatedDomain(authRequest, domain)

        then:
        1 * validator20.validatePasswordCredentials(_)
        1 * rackerAuthenticationService.authenticateRackerUsernamePassword(_, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> createRackerScopeAcccss()
        1 * tenantService.getEphemeralRackerTenantRoles(racker.id)
    }

    def "authenticateFederatedDomain handles authentication with RSA credentials"() {
        given:
        mockDomainConverter(service)
        mockAuthConverterCloudV20(service)

        def authRequest = createAuthenticationRequest(true)
        def domain = v1Factory.createDomain("id", "RACKSPACE")
        def racker = entityFactory.createRacker()
        def authResult = entityFactory.createUserAuthenticationResult(racker, true)

        when:
        service.authenticateFederatedDomain(authRequest, domain)

        then:
        1 * validator20.validateUsername(_)
        1 * rackerAuthenticationService.authenticateRackerRSA(_, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> createRackerScopeAcccss()
        1 * tenantService.getEphemeralRackerTenantRoles(racker.id)
    }

    def "assert getting user password credentials returns forbidden"() {
        when:
        def response = service.getUserPasswordCredentials(headers, authToken, "userId")

        then:
        response.build().status == SC_FORBIDDEN

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
    }

    def "identity:admin should not get other users password credentials"() {
        given:
        allowUserAccess()

        when:
        def response = service.getUserPasswordCredentials(headers, authToken, "userId").build()

        then:
        response.status == 403
    }

    def "getUserCredential verifies user is in callers domain when caller is user-admin"() {
        given:
        allowUserAccess()

        def user = Mock(User)
        def caller = Mock(User)

        userService.checkAndGetUserById(_) >> user
        identityUserService.getProvisionedUserById(_) >> caller

        when:
        service.getUserApiKeyCredentials(headers, authToken, "userId")

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContextHolder.getRequestContext().verifyEffectiveCallerIsNotARacker()
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
    }

    def "getUserCredential verifies caller is user when caller is default user"() {
        given:
        allowUserAccess()

        def user = Mock(User)
        def caller = Mock(User)
        caller.getId() >> "unique"

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller

        when:
        def result = service.getUserPasswordCredentials(headers, authToken, "userId")

        then:
        result.build().status == 403
    }

    def "getUserCredential verifies user with id exists"() {
        given:
        allowUserAccess()

        authorizationService.authorizeCloudServiceAdmin(_) >> false

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, "userId")

        then:
        result.build().status == 404
    }

    def "service-admin can get user apikey credentials"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser().with {
            it.apiKey = "apiKey"
            return it
        }
        def caller = entityFactory.createUser()

        when:
        def response2 = service.getUserApiKeyCredentials(headers, authToken, "userId")

        then:
        response2.build().status == 200

        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContextHolder.getRequestContext().verifyEffectiveCallerIsNotARacker()
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
    }

    def "addDomain verifies access level"() {
        given:
        allowUserAccess()
        mockDomainConverter(service)

        when:
        service.addDomain(authToken, uriInfo(), v1Factory.createDomain())

        then:
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName());
    }

    def "domainMultiFactorEnforcementLevel can not be set on domain creation"() {
        given:
        mockDomainConverter(service)
        def domain = v1Factory.createDomain().with {
            it.domainMultiFactorEnforcementLevel = DomainMultiFactorEnforcementLevelEnum.REQUIRED
            it
        }
        when:
        service.addDomain(authToken, uriInfo(), domain)

        then:
        1 * requestContext.getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName());
        1 * validator20.validateDomainForCreation(domain) >> { arg ->
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain d = arg[0]
            assert d.domainMultiFactorEnforcementLevel == null
        }
    }

    @Unroll
    def "addDomain - verify domain type can be assigned for authorized user: hasRole = #hasRole"() {
        given:
        mockDomainConverter(service)

        def domain = v1Factory.createDomain().with {
            it.type = "CLOUD_US"
            it
        }

        when:
        service.addDomain(authToken, uriInfo(), domain)

        then:
        1 * requestContext.getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName());
        1 * validator20.validateDomainForCreation(domain) >> { arg ->
            com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain d = arg[0]
            if (hasRole) {
                assert d.type == domain.type
            } else {
                assert d.type == null
            }
        }

        where:
        hasRole << [true, false]
    }


    def "addDomain adds domain with duplicate exception and success"() {
        given:
        allowUserAccess()
        domainConverter = Mock(DomainConverterCloudV20)
        service.domainConverterCloudV20 = domainConverter

        def domain1 = Mock(Domain)
        def domain2 = Mock(Domain)

        domainConverter.fromDomain(_) >>> [
                domain1,
                domain2
        ]

        when:
        def response1 = service.addDomain(authToken, uriInfo(), v1Factory.createDomain())
        def response2 = service.addDomain(authToken, uriInfo(), v1Factory.createDomain())

        then:
        1 * domainService.addDomain(domain1) >> { throw new DuplicateException() }
        1 * domainService.addDomain(domain2)

        response1.build().status == 409
        response2.build().status == 201
    }

    @Unroll
    def "updateDomain a domain's type can only be updated by users with domain admin role and feature flag is enabled: hasAdminRole = #hasAdminRole, featureEnabled = #featureEnabled"() {
        given:
        allowUserAccess()
        def domain = v1Factory.createDomain().with {
            it.type = RandomStringUtils.randomAlphanumeric(8).toUpperCase()
            it
        }
        def user = new User().with {
            it.domainId = domain.id
            it
        }
        requestContextHolder.getRequestContext().getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> user

        when:
        service.updateDomain(authToken, domain.id, domain)

        then:
        1 * identityConfig.getReloadableConfig().isFeatureSettingDomainTypeEnabled() >> featureEnabled
        authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName()) >> hasAdminRole
        1 * domainService.checkAndGetDomain(domain.id) >> new Domain()
        1 * domainService.updateDomain(_) >> { args ->
            Domain d = args[0]
            if (hasAdminRole && featureEnabled) {
                assert d.type == domain.type
            } else {
                assert d.type == null
            }
        }

        where:
        hasAdminRole | featureEnabled
        true         | true
        true         | false
        false        | false
        false        | true
    }

    def "getEndpointsByDomainId verifies admin access"() {
        given:
        mockEndpointConverter(service)

        domainService.checkAndGetDomain(_) >> entityFactory.createDomain()

        when:
        service.getEndpointsByDomainId(authToken, "domainId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "getEndpointsByDomainId verifies domain"() {
        given:
        allowUserAccess()
        mockEndpointConverter(service)

        when:
        service.getEndpointsByDomainId(authToken, "domainId")

        then:
        1 * domainService.checkAndGetDomain(_) >> entityFactory.createDomain()
    }

    def "getEndpointsByDomainId gets endpoints for domainId"() {
        given:
        allowUserAccess()
        mockEndpointConverter(service)

        domainService.checkAndGetDomain(_) >> entityFactory.createDomain()

        when:
        def response = service.getEndpointsByDomainId(authToken, "domainId")

        then:
        1 * tenantService.getTenantsFromNameList(_)
        1 * endpointService.getEndpointsFromTenantList(_)
        1 * endpointConverter.toEndpointList(_)

        response.build().status == 200
    }

    def "removeTenantFromDomain verifies identity-admin access level when flag is disabled"() {
        when:
        identityConfig.getReloadableConfig().isUseRoleForTenantManagementEnabled() >> false
        service.removeTenantFromDomain(authToken, "domainId", "tenantId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "removeTenantFromDomain verifies identity-admin access level when flag is enabled"() {
        when:
        identityConfig.getReloadableConfig().isUseRoleForTenantManagementEnabled() >> true
        service.removeTenantFromDomain(authToken, "domainId", "tenantId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName())
    }

    def "removeTenantFromDomain removes tenant from domain"() {
        given:
        allowUserAccess()

        when:
        def response = service.removeTenantFromDomain(authToken, "domainId", "tenantId")

        then:
        1 * domainService.removeTenantFromDomain(_, _)

        response.build().status == 204
    }

    def "method getAccessibleDomainsEndpointsForUser gets endpoints by user"() {
        given:
        def userId = "userId"
        def domainId = "domainId"
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()
        def endpoints = [entityFactory.createOpenstackEndpoint()].asList()
        def tenants = [entityFactory.createTenant()].asList()
        def tenantRoles = [entityFactory.createTenantRole()].asList()

        when:
        def response = service.getAccessibleDomainsEndpointsForUser(authToken, userId, domainId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.DEFAULT_USER
        1 * authorizationService.isSelf(user, user) >> true
        1 * userService.checkAndGetUserById(_) >> user
        1 * domainService.checkAndGetDomain(_) >> domain
        1 * scopeAccessService.getOpenstackEndpointsForUser(_) >> endpoints
        1 * tenantService.getTenantsByDomainId(_) >> tenants
        response.build().status == 200
    }

    def "authenticate verifies the request body"() {
        given:
        def authRequestNoCredentialsNoToken = v2Factory.createAuthenticationRequest()

        when:
        def response = service.authenticate(headers, authRequestNoCredentialsNoToken).build()

        then:
        response.status == 400
    }

    def "authenticate accepts either tenantId or tenantName but NOT both"() {
        given:
        def authRequestWithTenantIdAndTenantName = v2Factory.createAuthenticationRequest("tokenString", "tenantId", "tenantName")

        when:
        def response = service.authenticate(headers, authRequestWithTenantIdAndTenantName).build()

        then:
        response.status == 400
    }

    def "listCredentials verifies user level access"() {
        when:
        def result = service.listCredentials(headers, authToken, "userId", null, null).build()

        then:
        result.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER) >> { throw new NotAuthorizedException() }
    }

    def "listCredentials gets caller from requestContext and user from path parameter"() {
        when:
        service.listCredentials(headers, authToken, "userId", null, null)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById("userId")
    }

    @Unroll
    def "listCredentials: #userType can retrieve credentials for users within same domain"() {
        given:
        def user = entityFactory.createUser().with {
            it.apiKey = "apiKey"
            it
        }
        def caller = entityFactory.createUser()

        when:
        def response = service.listCredentials(headers, authToken, "userId", null, null)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById("userId") >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)

        where:
        userType << [IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    def "listCredentials allows all user types to retrieve own credentials"() {
        given:
        def user = entityFactory.createUser().with {
            it.apiKey = "apiKey"
            it
        }
        def caller = entityFactory.createUser()

        when:
        def response = service.listCredentials(headers, authToken, "id", null, null)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById("id") >> user
        1 * authorizationService.isSelf(caller, user) >> true

        0 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        0 * authorizationContext.getIdentityUserType()
    }

    def "listCredentials authorizes service admin"() {
        given:
        def user = entityFactory.createUser().with {
            it.apiKey = "apiKey"
            it
        }
        def caller = entityFactory.createUser()


        when:
        def response = service.listCredentials(headers, authToken, "userId", null, null)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById("userId") >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
    }

    def "listCredentials checks for apikey"() {
        given:
        def user = Mock(User)
        def caller = entityFactory.createUser()

        when:
        def response = service.listCredentials(headers, authToken, "userId", null, null)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById("userId") >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        2 * user.getApiKey() >> "apiKey"
        1 * user.getUsername() >> "username"
    }

    def "addService checks if caller is service admin"() {
        when:
        def result = service.addService(headers, uriInfo(), authToken, null).build()

        then:
        result.status == 401

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN) >> {throw new NotAuthorizedException()}
    }

    def "addService checks for null values"() {
        given:
        allowUserAccess()

        expect:
        if(serviceName != null) serviceToAdd.setName(serviceName)
        if(serviceType != null) serviceToAdd.setType(serviceType)
        def result = service.addService(headers, uriInfo(), authToken, serviceToAdd).build()
        result.status == status

        where:
        serviceToAdd    | serviceName   | serviceType   | status
        null            | null          | null          | 400
        new Service()   | null          | null          | 400
        new Service()   | "name"        | null          | 400
        new Service()   | "name"        | "serviceType" | 201
    }

    def "addService handles DuplicateException"() {
        given:
        allowUserAccess()
        def serviceToAdd = new Service()
        serviceToAdd.name = "name"
        serviceToAdd.type = "type"
        applicationService.add(_) >> {throw new DuplicateException()}

        when:
        def result = service.addService(headers, uriInfo(), authToken, serviceToAdd).build()

        then:
        result.status == 409
    }

    def "addTenant checks for null or empty tenant name"() {
        given:
        allowUserAccess()
        def tenant = new org.openstack.docs.identity.api.v2.Tenant()

        expect:
        if(name != null) tenant.name = name
        def result = service.addTenant(headers, uriInfo(), authToken, tenant).build()
        result.status == 400

        where:
        name | _
        ""   | _
        null | _

    }

    def "addTenant handles DuplicateException"() {
        given:
        allowUserAccess()
        def tenant = new org.openstack.docs.identity.api.v2.Tenant()
        tenant.name = "name"
        reloadableConfig.getTenantDefaultDomainId() >> "defaultDomain"
        domainService.getDomain("defaultDomain") >> entityFactory.createDomain("defaultDomain")
        tenantService.addTenant(_) >> {throw new DuplicateException()}
        mockTenantConverter(service)

        when:
        def result = service.addTenant(headers, uriInfo(), authToken, tenant).build()

        then:
        result.status == 409
    }

    def "listTenants gets tenants from user"() {
        given:
        mockTenantConverter(service)
        def user = entityFactory.createUser()

        when:
        def result = service.listTenants(headers, authToken, false, null, null).build()

        then:
        result.status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * tenantService.getTenantsForUserByTenantRoles(user)  >> [].asList()
    }

    def "getAdminsForDefaultUser - Default user can't get admin for any other user"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when: "same domain, but different user"
        service.getUserAdminsForUser(authToken, "userId")

        then:
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> true
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> false
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user

        then: "get forbidden"
        thrown(ForbiddenException)
    }

    def "getAdminsForDefaultUser - when caller is userAdmin, can't specify user in different domain"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "callerDomain", "REGION")
        def user = entityFactory.createUser("user", "userId", "userDomain", "REGION")

        when:
        service.getUserAdminsForUser(authToken, "userId")

        then:
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> false
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> true
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user

        then:
        thrown(ForbiddenException)
    }

    def "getAdminsForDefaultUser - A user-admin without a domain is not allowed to list admins in a different domain"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", null, "REGION")
        def user = entityFactory.createUser("user", "userId", "aDomain", "REGION")

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        0 * domainService.getEnabledDomainAdmins(_, _)
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> false
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> true
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user

        then:
        thrown(ForbiddenException)
    }

    def "getAdminsForDefaultUser - A user-admin with a domain is not allowed to list admins in a null domain"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "aDomain", "REGION")
        def user = entityFactory.createUser("user", "userId", null, "REGION")

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        0 * domainService.getEnabledDomainAdmins(_, _)
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> false
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> true
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user

        then:
        thrown(ForbiddenException)
    }

    def "getAdminsForDefaultUser - a list of enabled admins is returned by getAdminsForDefaultUser"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        1 * domainService.getEnabledDomainAdmins("domainId")
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> false
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> true
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user

        response.status == 200
    }

    def "getAdminsForDefaultUser - A list of admins is not retrieve if the target user has no domain"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", null, "REGION")
        def user = entityFactory.createUser("user", "userId", null, "REGION")

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        response.status == 200
        0 * domainService.getEnabledDomainAdmins(_, _)
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> false
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> true
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user
    }

    def "Get phone pin verifies user level access throws NotAuthorizedException"() {
        when:
        def result = service.getPhonePin(authToken, "userId").build()

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> { throw new NotAuthorizedException() }
        assert result.status == 401
    }

    def "Get phone pin successfully retrieve the pin"() {
        given:
        BaseUser caller = entityFactory.createUser().with {
            it.uniqueId = "uniqueId"
            it.id = "userId"
            it
        }
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")
        def phonePinEntity = entityFactory.createPhonePin()

        when:
        def result = service.getPhonePin(authToken, "userId").build()

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.checkAndGetEndUserById(_) >> user
        1 * phonePinService.checkAndGetPhonePin(_) >> phonePinEntity

        assert result.status == 200

        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = result.entity
        assert phonePin.pin == phonePinEntity.pin
    }

    def "Get phone pin returns 403 with caller-userId and user-userId are different"() {
        given:
        BaseUser caller = entityFactory.createUser().with {
            it.uniqueId = "uniqueId"
            it.id = "userId"
            it
        }

        when:
        def result = service.getPhonePin(authToken, "userId2").build()

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller

        assert result.status == 403
    }

    def "Get phone pin returns 403 for a racker impersonated token"() {
        given:
        BaseUser caller = entityFactory.createUser().with {
            it.uniqueId = "uniqueId"
            it.id = "userId"
            it
        }
        requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest() >> true

        when:
        def result = service.getPhonePin(authToken, "userId").build()

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller

        assert result.status == 403
    }

    def "getAdminsForDefaultUser - Non-Expired Fed users will return admins"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createFederatedUser("user").with {
            it.domainId = "domainId"
            it.id = "userId"
            it.expiredTimestamp = new DateTime().plusDays(1).toDate()
            it
        }

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        1 * domainService.getEnabledDomainAdmins("domainId")
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> false
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> true
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user
    }

    def "identity admin should be able to retrieve users api key"() {
        given:
        def caller = entityFactory.createUser().with {
            it.id = "1"
            return it
        }
        def user = entityFactory.createUser().with {
            it.id = "2"
            it.apiKey = "apiKey"
            return it
        }

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, userId).build()

        then:
        result.status == 200

        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContextHolder.getRequestContext().verifyEffectiveCallerIsNotARacker()
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
    }

    def "getAdminsForDefaultUser - Expired Fed users will not throw exception"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createFederatedUser("user").with {
            it.domainId = "domainId"
            it.id = "userId"
            it.expiredTimestamp = new DateTime().minusHours(1).toDate()
            it
        }

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName()) >> false
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName()) >> true
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * identityUserService.checkAndGetUserById("userId") >> user
    }

    def "default user should not be able to retrieve another default users api key"() {
        given:
        def caller = entityFactory.createUser().with {
            it.id = "1"
            return it
        }
        def user = entityFactory.createUser().with {
            it.id = "2"
            it.apiKey = "apiKey"
            return it
        }
        allowUserAccess()

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, "2").build()

        then:
        result.status == 403

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}
    }

    def "authenticateFederatedDomain sets token authenticatedBy with password credentials"() {
        given:
        mockDomainConverter(service)
        mockAuthConverterCloudV20(service)
        def racker = entityFactory.createRacker()
        def authResult = entityFactory.createUserAuthenticationResult(racker, true)
        def authenticationRequest = new AuthenticationRequest().with {
            it.credential = v2Factory.createJAXBPasswordCredentialsBase("username", "password")
            it
        }
        def domain = v1Factory.createDomain("id", "RACKSPACE")

        when:
        service.authenticateFederatedDomain(authenticationRequest, domain)

        then:
        1 * rackerAuthenticationService.authenticateRackerUsernamePassword(_, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> { arg1, arg2, List<String> arg3 ->
            assert (arg3.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD))
            createRackerScopeAcccss()
        }
    }

    def "authenticateFederatedDomain sets token authenticatedBy with rsa credentials"() {
        given:
        def rackerToken = new RackerScopeAccess().with {
            it.accessTokenString = "x"
            it
        }
        mockDomainConverter(service)
        mockAuthConverterCloudV20(service)
        def racker = entityFactory.createRacker()
        def authResult = entityFactory.createUserAuthenticationResult(racker, true)
        def authenticationRequest = new AuthenticationRequest().with {
            it.credential = v2Factory.createJAXBRsaCredentials("username", "password")
            it
        }
        def domain = v1Factory.createDomain("id", "RACKSPACE")

        when:
        service.authenticateFederatedDomain(authenticationRequest, domain)

        then:
        1 * rackerAuthenticationService.authenticateRackerRSA(_, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> { arg1, arg2, List<String> arg4 ->
            assert (arg4.contains(GlobalConstants.AUTHENTICATED_BY_RSAKEY))
            createRackerScopeAcccss()
        } >> rackerToken
        1 * authConverter.toRackerAuthenticationResponse(_, _, _, _) >> new AuthenticateResponse()
    }

    def "calling getUsers with email query param returns the user"() {
        given:
        ListUsersSearchParams params = new ListUsersSearchParams().with {
            it.email = "test@rackspace.com"
            it.userType = "ALL"
            it
        }

        when:
        def result = service.listUsers(headers, uriInfo(), authToken, params).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.roleName)
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName()) >> true
        1 * userService.getUsersByEmail('test@rackspace.com', UserType.ALL) >> [entityFactory.createUser()].asList()

        then:
        result.status == 200
    }

    def "userAdmin calling getUsersByEmail filters subUsers by domain"() {
        given:
        requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN

        def converter = new UserConverterCloudV20()
        converter.mapper = new DozerBeanMapper()
        converter.roleConverterCloudV20 = Mock(RoleConverterCloudV20)
        converter.groupConverterCloudV20 =  Mock(GroupConverterCloudV20)
        converter.authorizationService = Mock(AuthorizationService)
        converter.identityConfig = new IdentityConfig(Mock(Configuration), Mock(Configuration))
        converter.basicMultiFactorService = Mock(BasicMultiFactorService)
        service.userConverterCloudV20 = converter

        def email = "email@gmail.com"
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def subUser1 = entityFactory.createUser("subUser1", "userId1", "domainId", "region").with {
            it.email = email
            it
        }
        def subUser2 = entityFactory.createUser("subUser2", "userId2", "notDomainId", "region").with {
            it.email = email
            it
        }
        ListUsersSearchParams params = new ListUsersSearchParams().with {
            it.email = "email@test.com"
            it.userType = "ALL"
            it
        }
        PaginatorContext context = new PaginatorContext()
        context.update(Arrays.asList(subUser1, subUser2), params.paginationRequest.effectiveMarker, params.paginationRequest.effectiveLimit)

        when:
        def result = service.listUsers(headers, uriInfo(), authToken, params).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.roleName)
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName()) >> false
        1 * requestContext.getEffectiveCaller() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * userService.getUsersByEmail(_, UserType.ALL) >> [subUser1, subUser2].asList()
        1 * authorizationService.hasSameDomain(caller, subUser1) >> true
        1 * authorizationService.hasSameDomain(caller, subUser2) >> false

        then:
        result.status == 200
        UserList users = result.entity
        users.user.size() == 1
    }

    def "updateUser: Disabling a enabled user sends an atom feed"(){
        given:
        allowUserAccess()
        def converter = new UserConverterCloudV20()
        converter.mapper = new DozerBeanMapper()
        converter.roleConverterCloudV20 = Mock(RoleConverterCloudV20)
        converter.groupConverterCloudV20 =  Mock(GroupConverterCloudV20)
        service.userConverterCloudV20 = converter

        UserForCreate user = new UserForCreate().with {
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = true
        updateUser.id = 2
        identityUserService.getEndUserById(_) >> updateUser
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        userService.isUsernameUnique(_) >> true
        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.SERVICE_ADMIN

        when:
        service.updateUser(headers, authToken, "2", user)

        then:
        1 * atomHopperClient.asyncPost(_, FeedsUserStatusEnum.DISABLED, _)
        1 * atomHopperClient.asyncPost(_, FeedsUserStatusEnum.UPDATE, _)
    }

    def "updateUser: Update token format as a identity admin when ae tokens enabled doesn't reset the data"() {
        given:
        allowUserAccess()

        UserForCreate userInput = Mock(UserForCreate)
        userInput.getId() >> "2"

        User user = Mock(User)
        user.getId() >> "2"

        User caller = Mock(User)
        caller.getId() >> "123"

        userService.checkAndGetUserById("2") >> user
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        when:
        service.updateUser(headers, authToken, "2", userInput)

        then:
        0 * userInput.setTokenFormat(_)
    }

    def "updateUser: Update token format as a service admin doesn't reset the data"() {
        given:
        allowUserAccess()
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        UserForCreate userInput = Mock(UserForCreate)
        userInput.getId() >> "2"

        User user = Mock(User)
        user.getId() >> "2"

        User caller = Mock(User)
        caller.getId() >> "123"

        userService.checkAndGetUserById("2") >> user
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        when:
        service.updateUser(headers, authToken, "2", userInput)

        then:
        0 * userInput.setTokenFormat(_)
    }

    def "updateUser: Update token format as a non service/identity admin reset the data"() {
        given:
        allowUserAccess()
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        UserForCreate userInput = Mock(UserForCreate)
        userInput.getId() >> "2"

        User user = Mock(User)
        user.getId() >> "2"

        User caller = Mock(User)
        caller.getId() >> "123"

        identityUserService.getEndUserById("2") >> user
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        when:
        service.updateUser(headers, authToken, "2", userInput)

        then:
        1 * userInput.setTokenFormat(null)
    }

    def "updateUser: Disabling a disabled user does not send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = false
        updateUser.id = 2
        userService.checkAndGetUserById(_) >> updateUser
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        userService.isUsernameUnique(_) >> true

        when:
        service.updateUser(headers, authToken, "2", user)

        then:
        0 * atomHopperClient.asyncPost(_, _, _)
    }

    def "updateUser: Enabling a enabled user does not send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "2"
            it.enabled = true
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = true
        updateUser.id = 2
        userService.checkAndGetUserById(_) >> updateUser
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        userService.isUsernameUnique(_) >> true

        when:
        service.updateUser(headers, authToken, "2", user)

        then:
        0 * atomHopperClient.asyncPost(_, _, _)
    }

    def "Enabling a enabled user using 'setUserEnabled' does not send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "2"
            it.enabled = true
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = true
        updateUser.id = 2
        userService.checkAndGetUserById(_) >> updateUser

        when:
        service.setUserEnabled(headers, authToken, "2", user)

        then:
        0 * atomHopperClient.asyncPost(_, _, _)
    }

    def "Disabling a disabled user using 'setUserEnabled' does not send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = false
        updateUser.id = 2
        userService.checkAndGetUserById(_) >> updateUser

        when:
        service.setUserEnabled(headers, authToken, "2", user)

        then:
        0 * atomHopperClient.asyncPost(_, _, _)
    }

    def "Enabling a disabled user using 'setUserEnabled' does send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = true
        updateUser.id = 2
        userService.checkAndGetUserById(_) >> updateUser

        when:
        service.setUserEnabled(headers, authToken, "2", user)

        then:
        1 * atomHopperClient.asyncPost(_, _, _)
    }

    def "Disabling a enabled user using 'setUserEnabled' does send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "2"
            it.enabled = true
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = false
        updateUser.id = 2
        userService.checkAndGetUserById(_) >> updateUser

        when:
        service.setUserEnabled(headers, authToken, "2", user)

        then:
        1 * atomHopperClient.asyncPost(_, _, _)
    }

    def "updateUser: Enabling a disabled user does send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = Mock(UserConverterCloudV20)
        UserForCreate user = new UserForCreate().with {
            it.id = "2"
            it.enabled = true
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = true
        updateUser.id = 2
        User updatedUser = entityFactory.createUser()
        updateUser.enabled = false
        updateUser.id = 2

        identityUserService.getEndUserById(_) >> updateUser
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        userService.isUsernameUnique(_) >> true
        service.userConverterCloudV20.fromUser(_) >> updatedUser
        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.SERVICE_ADMIN

        when:
        service.updateUser(headers, authToken, "2", user)

        then:
        1 * atomHopperClient.asyncPost(_, FeedsUserStatusEnum.ENABLED, _)
        1 * atomHopperClient.asyncPost(_, FeedsUserStatusEnum.UPDATE, _)
    }

    def "updateUser: Disabling a enabled user does send an atom feed"(){
        given:
        allowUserAccess()
        service.userConverterCloudV20 = Mock(UserConverterCloudV20)
        UserForCreate user = new UserForCreate().with {
            it.id = "2"
            it.enabled = true
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = true
        updateUser.id = 2
        User updatedUser = entityFactory.createUser()
        updateUser.enabled = false
        updateUser.id = 2

        identityUserService.getEndUserById(_) >> updateUser
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        userService.isUsernameUnique(_) >> true
        service.userConverterCloudV20.fromUser(_) >> updatedUser
        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.SERVICE_ADMIN

        when:
        service.updateUser(headers, authToken, "2", user)

        then:
        1 * atomHopperClient.asyncPost(_, FeedsUserStatusEnum.UPDATE, _)
    }

    def "updateUser does not allow a user to enable their own account"() {
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            return it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = false
        updateUser.id = 2
        userService.checkAndGetUserById(_) >> updateUser
        userService.getUserByAuthToken(_) >> entityFactory.createUser()

        when:
        service.updateUser(headers, authToken, "2", user)

        then:
        0 * atomHopperClient.asyncPost(_, _, _)
    }

    def "updateUser does not allow a user to disable their own account"() {
        given:
        allowUserAccess()
        service.userConverterCloudV20 = new UserConverterCloudV20()
        UserForCreate user = new UserForCreate().with {
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            it
        }

        User updateUser = entityFactory.createUser()
        updateUser.enabled = false
        updateUser.id = 2
        identityUserService.getEndUserById(_) >> updateUser
        def caller = entityFactory.createUser()
        caller.id = "2"
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.SERVICE_ADMIN

        when:
        def result = service.updateUser(headers, authToken, "2", user).build()

        then:
        result.status == 400
    }

    def "updateUser validates that user ID in URL matches user ID in updated user"() {
        given:
        allowUserAccess()
        UserForCreate user = new UserForCreate().with {
            it.username = "name"
            it.id = "1"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            it
        }
        identityUserService.getEndUserById(_) >> entityFactory.createUser()
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

        when:
        def response = service.updateUser(headers, authToken, "2", user).build()

        then:
        response.status == 400
    }



    def "addUserCredential validates that user to update matches credentials for passwordCredentials"() {
        given:
        allowUserAccess()
        def mediaType = Mock(MediaType)
        mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) >> true
        headers.getMediaType() >> mediaType
        def user = new User().with({
            it.username = "otherUser"
            it
        })
        userService.checkAndGetUserById(_) >> user
        def credentials = v2Factory.createPasswordCredentialsBase()
        def body = v2Factory.createStringifiedXmlBodyforCredentials(credentials)

        when:
        def result = service.addUserCredential(headers, uriInfo(), authToken, "someUser", body).build()

        then:
        result.status == 400
    }

    def "addUserCredential validates that user to update matches credentials for apiKeyCredentials"() {
        given:
        allowUserAccess()
        def mediaType = Mock(MediaType)
        mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) >> true
        headers.getMediaType() >> mediaType
        def user = new User().with({
            it.username = "otherUser"
            it
        })
        userService.checkAndGetUserById(_) >> user
        def apiKeyCredentials = v2Factory.createApiKeyCredentials("someUser", "someApiKey1")
        def body = v2Factory.createStringifiedXmlBodyforCredentials(apiKeyCredentials)

        when:
        def result = service.addUserCredential(headers, uriInfo(), authToken, "someUser", body).build()

        then:
        result.status == 400
    }

    def "addUserCredential with api key calls updateUser with new api key set"() {
        given:
        allowUserAccess()
        def mediaType = Mock(MediaType)
        mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) >> true
        headers.getMediaType() >> mediaType
        def user = new User().with({
            it.username = "someUser"
            it
        })
        userService.checkAndGetUserById(_) >> user

        def apiKeyCredentials = v2Factory.createApiKeyCredentials("someUser", "someApiKey1")
        def body = v2Factory.createStringifiedXmlBodyforCredentials(apiKeyCredentials)

        when:
        def result = service.addUserCredential(headers, uriInfo(), authToken, "someUser", body).build()

        then:
        userService.updateUser(_) >> { args -> args[0].apiKey == "someApiKey1" }
        result.status == 201
    }

    def "addUserCredential should restrict unverified users"() {
        given:
        allowUserAccess()
        def mediaType = Mock(MediaType)
        mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) >> true
        headers.getMediaType() >> mediaType
        def user = new User().with({
            it.username = "someUser"
            it.unverified = true
            it
        })
        userService.checkAndGetUserById(_) >> user
        def apiKeyCredentials = v2Factory.createApiKeyCredentials("someUser", "someApiKey1")
        def body = v2Factory.createStringifiedXmlBodyforCredentials(apiKeyCredentials)

        when:
        def result = service.addUserCredential(headers, uriInfo(), authToken, "someUser", body).build()

        then:
        result.status == 403
    }

    @Unroll("service admin deleting product roles on #userRole returns #expectedResult")
    def "service admin CAN delete a users product roles"() {
        given:
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def user = entityFactory.createUser("user", "userId2", "domainId", "region")

        when:
        def deleteRolesResult = service.deleteUserRoles(headers, authToken, "1", "rbac").build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(_) >> user

        if (precedenceThrowsForbidden) {
            precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_) >> {throw new ForbiddenException()}
        } else {
            1 * tenantService.deleteRbacRolesForUser(user)
        }

        deleteRolesResult.status == expectedResult

        where:
        userRole                 | precedenceThrowsForbidden | expectedResult
        "identity:service-admin" | true                      | SC_FORBIDDEN
        "identity:admin"         | false                     | SC_NO_CONTENT
        "identity:user-admin"    | false                     | SC_NO_CONTENT
        "identity:user-manage"   | false                     | SC_NO_CONTENT
        "identity:default"       | false                     | SC_NO_CONTENT
    }

    @Unroll("identity admin deleting product roles on #userRole returns #expectedResult")
    def "identity admin CAN delete a users product roles"() {
        given:
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def user = entityFactory.createUser("user", "userId2", "domainId", "region")

        when:
        def deleteRolesResult = service.deleteUserRoles(headers, authToken, "1", "rbac").build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(_) >> user

        if (precedenceThrowsForbidden) {
            precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_) >> {throw new ForbiddenException()}
        } else {
            1 * tenantService.deleteRbacRolesForUser(user)
        }

        deleteRolesResult.status == expectedResult

        where:
        userRole                 | precedenceThrowsForbidden | expectedResult
        "identity:service-admin" | true                      | SC_FORBIDDEN
        "identity:admin"         | true                      | SC_FORBIDDEN
        "identity:user-admin"    | false                     | SC_NO_CONTENT
        "identity:user-manage"   | false                     | SC_NO_CONTENT
        "identity:default"       | false                     | SC_NO_CONTENT
    }

    @Unroll("user admin deleting product roles on #userRole returns #expectedResult")
    def "user admin CAN delete a users product roles"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def user = entityFactory.createUser("user", "userId2", "domainId", "region")

        when:
        def deleteRolesResult = service.deleteUserRoles(headers, authToken, "1", "rbac").build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(_) >> user

        if (precedenceThrowsForbidden) {
            precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_) >> {throw new ForbiddenException()}
        } else {
            1 * tenantService.deleteRbacRolesForUser(user)
        }

        deleteRolesResult.status == expectedResult

        where:
        userRole                 | precedenceThrowsForbidden | expectedResult
        "identity:service-admin" | true                      | SC_FORBIDDEN
        "identity:admin"         | true                      | SC_FORBIDDEN
        "identity:user-admin"    | true                      | SC_FORBIDDEN
        "identity:user-manage"   | false                     | SC_NO_CONTENT
        "identity:default"       | false                     | SC_NO_CONTENT
    }

    def "user admin CANNOT delete a different domain users product roles"() {
        given:
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def user = entityFactory.createUser("user", "userId2", "domainId2", "region")

        when:
        def deleteRolesResult = service.deleteUserRoles(headers, authToken, "1", "rbac").build()

        then:
        deleteRolesResult.status == SC_FORBIDDEN

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById(_) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}
    }

    @Unroll("user manage deleting product roles on #userRole returns #expectedResult")
    def "user manage CAN delete a users product roles"() {
        given:
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def user = entityFactory.createUser("user", "userId2", "domainId", "region")

        when:
        def deleteRolesResult = service.deleteUserRoles(headers, authToken, "1", "rbac").build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(_) >> user

        if (precedenceThrowsForbidden) {
            precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_) >> {throw new ForbiddenException()}
        } else {
            1 * tenantService.deleteRbacRolesForUser(user)
        }

        deleteRolesResult.status == expectedResult

        where:
        userRole                 | precedenceThrowsForbidden | expectedResult
        "identity:service-admin" | true                      | SC_FORBIDDEN
        "identity:admin"         | true                      | SC_FORBIDDEN
        "identity:user-admin"    | true                      | SC_FORBIDDEN
        "identity:user-manage"   | true                      | SC_FORBIDDEN
        "identity:default"       | false                     | SC_NO_CONTENT
    }

    def "user manage CANNOT delete a different domain users product roles"() {
        given:
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def user = entityFactory.createUser("user", "userId2", "domainId2", "region")

        when:
        def deleteRolesResult = service.deleteUserRoles(headers, authToken, "1", "rbac").build()

        then:
        deleteRolesResult.status == SC_FORBIDDEN

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.checkAndGetUserById(_) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}
    }

    @Unroll("default user deleting product roles on #userRole returns #expectedResult")
    def "default user CANNOT delete a users product roles"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def user = entityFactory.createUser("user", "userId2", "domainId", "region")

        when:
        def deleteRolesResult = service.deleteUserRoles(headers, authToken, "1", "rbac").build()

        then:
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        if (precedenceThrowsForbidden) {
            precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_) >> {throw new ForbiddenException()}
        }

        deleteRolesResult.status == expectedResult

        where:
        userRole                 | precedenceThrowsForbidden | expectedResult
        "identity:service-admin" | true                      | SC_FORBIDDEN
        "identity:admin"         | true                      | SC_FORBIDDEN
        "identity:user-admin"    | true                      | SC_FORBIDDEN
        "identity:user-manage"   | true                      | SC_FORBIDDEN
        "identity:default"       | true                      | SC_FORBIDDEN
    }

    @Unroll
    def "delete api key credential using admin token - callerType = #callerType"() {
        given:
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def userId = "userId2"
        def user = entityFactory.createUser("caller", userId, "domainId", "region")
        user.apiKey = "key"

        when:
        def response = service.deleteUserCredential(headers, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build()

        then:
        response.status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById(userId) >> user
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * userService.updateUser(user) >> { arg ->
            com.rackspace.idm.domain.entity.User updateUser = arg[0]
            assert updateUser.apiKey == ""
        }

        where:
        callerType << [IdentityUserTypeEnum.SERVICE_ADMIN, IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    @Unroll
    def "self delete api key credentials - callerType = #callerType"() {
        given:
        def caller = entityFactory.createUser("caller", userId, "domainId", "region")
        caller.apiKey = "key"

        when:
        def result = service.deleteUserCredential(headers, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build()

        then:
        if (callerType == IdentityUserTypeEnum.USER_ADMIN) {
            result.status == SC_FORBIDDEN
        } else {
            result.status == SC_NO_CONTENT
        }

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById(userId) >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * authorizationService.isSelf(caller, caller) >> true
        0 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(caller)
        userService.updateUser(caller) >> { arg ->
            com.rackspace.idm.domain.entity.User updateUser = arg[0]
            assert updateUser.apiKey == ""
        }

        where:
        callerType << IdentityUserTypeEnum.values()
    }

    def "error check: delete api key credential"() {
        given:
        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def userId = "userId2"
        def user = entityFactory.createUser("caller", userId, "domainId", "region")
        user.apiKey = "key"

        when: "caller does not have precedence over user"
        def response = service.deleteUserCredential(headers, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build()

        then:
        response.status == SC_FORBIDDEN

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById(userId) >> user
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}

        when: "caller and user do not belong to the same domain"
        response = service.deleteUserCredential(headers, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build()


        then:
        response.status == SC_FORBIDDEN

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById(userId) >> user
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}
    }

    def "Add Group to user"() {
        given:
        User user = entityFactory.createRandomUser()
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        identityUserService.checkAndGetUserById(_) >> user
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        identityUserService.getEndUsersByDomainId(_, UserType.ALL) >> [user].asList()

        when:
        Response.ResponseBuilder response = service.addUserToGroup(headers, authToken, "1", "2")

        then:
        1 * atomHopperClient.asyncPost(_, _, _)
        1 * identityUserService.addGroupToEndUser(_, _)
        response.build().status == 204
    }

    def "Add Group to user with subUsers adds groups for each subuser"() {
        given:
        User user = entityFactory.createRandomUser()
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        identityUserService.checkAndGetUserById(_) >> user
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        userService.isUserInGroup(user.id, "1") >> false
        identityUserService.getEndUsersByDomainId(_, UserType.ALL) >> [user, entityFactory.createRandomUser()].asList()

        when:
        Response.ResponseBuilder response = service.addUserToGroup(headers, authToken, "1", user.id)

        then:
        2 * atomHopperClient.asyncPost(_, _, _)
        2 * identityUserService.addGroupToEndUser(_, _)
        response.build().status == 204
    }

    def "Add an existing Group to user"() {
        given:
        User user = entityFactory.createRandomUser()
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        identityUserService.checkAndGetUserById(_) >> user
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        userService.isUserInGroup(user.id, "1") >> true
        identityUserService.getEndUsersByDomainId(_, UserType.ALL) >> [user, entityFactory.createRandomUser()].asList()

        when:
        Response.ResponseBuilder response = service.addUserToGroup(headers, authToken, "1", user.id)

        then:
        0 * atomHopperClient.asyncPost(_, _, _)
        0 * identityUserService.addGroupToEndUser(_, _)
        response.build().status == 204
    }

    def "Delete Group from user"() {
        given:
        User user = entityFactory.createRandomUser()
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        identityUserService.checkAndGetUserById(_) >> user
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        identityUserService.getEndUsersByDomainId(_, UserType.ALL) >> [user].asList()
        userService.isUserInGroup(_, _) >> true

        when:
        Response.ResponseBuilder response = service.removeUserFromGroup(headers, authToken, "1", user.id)

        then:
        1 * atomHopperClient.asyncPost(_, _, _)
        1 * identityUserService.removeGroupFromEndUser("1", user.id)
        response.build().status == 204
    }

    def "Delete Group from user with subUsers removes from each"(){
        given:
        User user = entityFactory.createRandomUser()

        when:
        Response.ResponseBuilder response = service.removeUserFromGroup(headers, authToken, "1", user.id)

        then:
        response.build().status == 204

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        1 * identityUserService.checkAndGetUserById(_) >> user
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.USER_ADMIN
        1 * identityUserService.getEndUsersByDomainId(_, UserType.ALL) >> [user, entityFactory.createRandomUser()].asList()
        1 *userService.isUserInGroup(_, _) >> true
        2 * atomHopperClient.asyncPost(_, _, _)
        2 * identityUserService.removeGroupFromEndUser(_, _)
    }

    def "Remove v1Default if it exists" () {
        given:
        allowUserAccess()
        def tenant = entityFactory.createTenant()
        tenant.v1Defaults = ["1","2"].asList()
        tenant.baseUrlIds = ["1","2", "3"].asList()
        def cloudBaseUrl = entityFactory.createCloudBaseUrl()
        cloudBaseUrl.baseUrlId = 1

        when:
        service.deleteEndpoint(headers, authToken, "1", "1")

        then:
        1 * tenantService.checkAndGetTenant(_) >> tenant
        1 * endpointService.checkAndGetEndpointTemplate(_) >> cloudBaseUrl
        1 * tenantService.updateTenant(_)
        tenant.v1Defaults.size() == 1
    }

    def "Remove endpoint if it exists" () {
        given:
        allowUserAccess()
        def tenant = entityFactory.createTenant()
        tenant.baseUrlIds = ["1","2", "3"].asList()
        def cloudBaseUrl = entityFactory.createCloudBaseUrl()
        cloudBaseUrl.baseUrlId = 1

        when:
        service.deleteEndpoint(headers, authToken, "1", "1")

        then:
        1 * tenantService.checkAndGetTenant(_) >> tenant
        1 * endpointService.checkAndGetEndpointTemplate(_) >> cloudBaseUrl
        1 * tenantService.updateTenant(_)
        tenant.baseUrlIds.size() == 2
    }

    def "user admin can add user-managed role to a default user"() {
        given:
        allowUserAccess()
        def userId = "1"
        def roleId = "7"

        def cRole = entityFactory.createClientRole().with {
            it.id = roleId
            it.name = "identity:user-manage"
            return it
        }

        def user = entityFactory.createUser().with {
            it.username = "user"
            it.id = userId
            return it
        }

        def caller = entityFactory.createUser().with {
            it.username = "caller"
            return it
        }
        def tenantRole = entityFactory.createTenantRole().with {
            it.name = "identity:default"
            return it
        }
        def userRoles = [tenantRole].asList()

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        applicationService.getClientRoleById(roleId) >> cRole
        userService.checkAndGetUserById(userId) >> user
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        tenantService.getGlobalRolesForUser(_) >> userRoles
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        authorizationService.hasDefaultUserRole(_) >> true

        then:
        1 * tenantService.addTenantRoleToUser(_, _, true)
    }

    def "addUser - calls correct services"() {
        // Setup mocks
        mockCreateSubUserService(service)
        def createUserValidatorMock = Mock(Cloud20CreateUserValidator)
        service.createUserValidator = createUserValidatorMock
        def userForCreate = v2Factory.createUser()
        service.userConverterCloudV20 = userConverter

        def caller = entityFactory.createUser()

        when:
        def response = service.addUser(headers, uriInfo(), authToken, userForCreate)

        then:
        response.build().status == SC_CREATED

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >>  caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * createUserValidatorMock.validateCreateUserAndGetUserForDefaults(userForCreate, caller) >> entityFactory.createUser()
        1 * authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.USER_ADMIN
        1 * createSubUserService.setDefaultsAndCreateUser(_, _) >> entityFactory.createUser()
        1 * userConverter.toUser(_, true) >> v2Factory.createUser()
    }

    def "add new user when user is not authorized returns 403 response" () {
        given:
        allowUserAccess()

        def user = v1Factory.createUserForCreate()

        when:
        def result = service.addUser(headers, uriInfo(), authToken, user).build()

        then:
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER) >> { throw new ForbiddenException()}
        result.status == 403
    }

    def "deleteUser - Delete userAdminDN on domain when deleting user-admin" () {
        given:
        allowUserAccess()
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }

        when:
        service.deleteUser(headers, authToken, "userId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * identityUserService.checkAndGetUserById(_) >> user
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.USER_ADMIN
        1 * domainService.removeDomainUserAdminDN(user)
        1 * identityUserService.deleteUser(_)
    }

    def "User with user-manage role can delete user" () {
        given:
        allowUserAccess()
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }

        when:
        service.deleteUser(headers, authToken, "userId")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * identityUserService.checkAndGetUserById(_) >> user
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_MANAGER
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER
        1 * identityUserService.deleteUser(_)
    }

    def "User with user-manage role can delete user with user-manage role" () {
        given:
        allowUserAccess()
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }

        when:
        def result = service.deleteUser(headers, authToken, "userId")

        then:
        result.build().status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * identityUserService.checkAndGetUserById(_) >> user
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_MANAGER
        1 *  precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.USER_MANAGER
        1 * identityUserService.deleteUser(_)
    }

    @Unroll
    def "User with user-manage role can update different user with role: #userType"() {
        given:
        allowUserAccess()

        userId = "1"
        UserForCreate user = new UserForCreate().with {
            it.id = userId
            return it
        }
        User retrievedUser = entityFactory.createUser().with {
            it.id = userId
            return it
        }

        User caller = entityFactory.createUser().with {
            it.id = "5"
            return it
        }

        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(_) >> retrievedUser
        def expectedStatus = IdentityUserTypeEnum.DEFAULT_USER == userType ? 200 : 403
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER
        if (IdentityUserTypeEnum.DEFAULT_USER != userType) {
            1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(retrievedUser) >> {
                throw new ForbiddenException()
            }
        }

        when:
        def result = service.updateUser(headers, authToken, "1", user).build()

        then:
        result.status == expectedStatus

        where:
        userType << IdentityUserTypeEnum.values()
    }

    def "Add user-manage role to user with identity admin role gives 401" () {
        given:
        allowUserAccess()
        ClientRole clientRole = new ClientRole().with {
            it.name = "identity:user-manage"
            return it
        }

        User user = entityFactory.createUser()
        user.id = "somthingdifferentfromcaller"
        User caller = entityFactory.createUser()
        roleService.isIdentityUserTypeRole(clientRole) >> true

        when:
        def result = service.addUserRole(headers, authToken, "abc", "123")

        then:
        1 * applicationService.getClientRoleById(_) >> clientRole
        1 * userService.checkAndGetUserById(_) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * authorizationService.hasDefaultUserRole(_) >> false
        result.build().status == 400
    }

    def "Add user-manage role to default user with different domain return 403" () {
        given:
        allowUserAccess()
        ClientRole clientRole = new ClientRole().with {
            it.name = "identity:user-manage"
            return it
        }
        def user = entityFactory.createUser().with {
            it.domainId = "1"
            return it
        }
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it.domainId = "2"
            return it
        }

        when:
        def result = service.addUserRole(headers, authToken, "abc", "123")

        then:
        result.build().status == 403

        1 * applicationService.getClientRoleById(_) >> clientRole
        1 * userService.checkAndGetUserById(_) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> {throw new ForbiddenException()}
    }

    def "Add user-manage role to default user" () {
        given:
        allowUserAccess()
        ClientRole clientRole = new ClientRole().with {
            it.name = "identity:user-manage"
            return it
        }
        def user = entityFactory.createUser()
        user.id = "something different from caller"
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        roleService.isIdentityUserTypeRole(clientRole) >> true

        when:
        def result = service.addUserRole(headers, authToken, "abc", "123")

        then:
        result.build().status == 200

        1 * applicationService.getClientRoleById(_) >> clientRole
        1 * userService.checkAndGetUserById(_) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * authorizationService.hasDefaultUserRole(_) >> true
    }

    def "User with user-manage role can get user by ID" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()
        User caller = entityFactory.createUser()

        when:
        def result = service.getUserById(headers, authToken, "abc123")

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getEndUserById(_) >> user
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER
        1 * authorizationService.verifyDomain(_, _)
        result.build().status == 200
    }

    def "default users can't get user other users by ID" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()
        User caller = entityFactory.createUser()

        when:
        def result = service.getUserById(headers, authToken, "abc123")

        then:
        result.build().status == 404 // Obfuscated exception

        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.DEFAULT_USER
        0 * identityUserService.getEndUserById(_) >> user
        0 * authorizationService.verifyDomain(_, _)
    }

    def "User with user-manage role can get user by name" () {
        given:
        allowUserAccess()

        User user = entityFactory.createUser()
        User caller = entityFactory.createUser()

        when:
        def result = service.getUserByName(headers, authToken, "testName")

        then:
        result.build().status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * userService.getUser(_) >> user
        1 * authorizationService.hasSameDomain(caller, user) >> true
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.DEFAULT_USER, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName())
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName()) >> false
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER
    }

    def "User with user-manage role can get user by email" () {
        given:
        User user = entityFactory.createUser()
        def users = [user].asList()
        User caller = entityFactory.createUser()
        ListUsersSearchParams params = new ListUsersSearchParams().with {
            it.email = "test@rackspace.com"
            it.userType = "ALL"
            it
        }
        requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_MANAGER

        when:
        def result = service.listUsers(headers, uriInfo(), authToken, params)

        then:
        result.build().status == 200

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName())
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName()) >> false
        1 * requestContext.getEffectiveCaller() >> caller
        1 * userService.getUsersByEmail(_, UserType.ALL) >> users
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_MANAGER
    }

    def "User with user-manage can get user's api-key" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()
        user.apiKey = "apikeyyay"
        User caller = entityFactory.createUser()

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, "abc123")

        then:
        result.build().status == 200

        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContextHolder.getRequestContext().verifyEffectiveCallerIsNotARacker()
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
    }

    def "federated users cannot get user's api-key" () {
        given:
        User user = entityFactory.createUser()
        user.apiKey = "apikeyyay"

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, "abc123")

        then:
        result.build().status == 403

        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().verifyEffectiveCallerIsNotARacker() >> { throw new ForbiddenException() }
        0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        0 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled()
    }

    def "User with user-manage role cannot get User Admin's API key" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()
        User caller = entityFactory.createUser()

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, "abc123")

        then:
        result.build().status == 403

        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContextHolder.getRequestContext().verifyEffectiveCallerIsNotARacker()
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user) >> { throw  new ForbiddenException() }
        0 * requestContextHolder.getRequestContext().getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_MANAGER
    }

    def "deleteUserCredential returns 404 if the apiKeyCredentials are not found"() {
        given:
        allowUserAccess()
        def user = entityFactory.createUser().with {
            it.apiKey = apiKey
            it
        }
        userService.checkAndGetUserById(userId) >> user

        when:
        def result = service.deleteUserCredential(headers, authToken, userId, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS).build()

        then:
        result.getStatus() == 404

        where:
        apiKey << [null, ""]
    }


    def "[B-82794] Verify if the API error is properly obfuscated"() {
        given:
        allowUserAccess()
        User user = entityFactory.createUser().with {
            it.id = "userId"
            it
        }
        User caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN

        def result

        when:
        identityUserService.getEndUserById("notfound") >> null
        result = service.getUserById(headers, authToken, "notfound")

        then:
        result.build().status == 404

        when:
        identityUserService.getEndUserById("forbidden") >> user
        authorizationService.verifyDomain(caller, user) >> { throw new ForbiddenException() }
        result = service.getUserById(headers, authToken, "forbidden")

        then:
        result.build().status == 404

        when:
        requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.DEFAULT_USER
        result = service.getUserById(headers, authToken, "nomatch")

        then:
        result.build().status == 404
    }

    @Unroll
    def "getUserById returns the password expiration for a user - featureEnabled = #featureEnabled"() {
        given:
        identityConfig.getReloadableConfig().isIncludePasswordExpirationDateForGetUserResponsesEnabled() >> featureEnabled
        allowUserAccess()
        def user = new org.openstack.docs.identity.api.v2.User()
        def fedUser = new org.openstack.docs.identity.api.v2.User()
        def userEntity = entityFactory.createUser()
        def fedUserEntity = entityFactory.createFederatedUser()
        User caller = entityFactory.createUser()
        def pwdExpiration = new DateTime()
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN

        when: "get a provisioned user by ID"
        Response response = service.getUserById(headers, authToken, user.id).build()

        then:
        1 * identityUserService.getEndUserById(user.id) >> userEntity
        1 * userConverter.toUser(userEntity) >> user
        if (featureEnabled) {
            1 * userService.getPasswordExpiration(userEntity) >> pwdExpiration
        } else {
            0 * userService.getPasswordExpiration(userEntity) >> pwdExpiration
        }
        response.status == 200
        def responseEntity = response.getEntity()
        if (featureEnabled) {
            responseEntity.passwordExpiration.toGregorianCalendar().getTime() == pwdExpiration
        } else {
            responseEntity.passwordExpiration == null
        }

        when: "get a federated user by ID"
        response = service.getUserById(headers, authToken, fedUserEntity.id).build()
        responseEntity = response.getEntity()

        then:
        1 * identityUserService.getEndUserById(fedUserEntity.id) >> fedUserEntity
        1 * userConverter.toUser(fedUserEntity) >> fedUser
        0 * userService.getPasswordExpiration(_)
        response.status == 200
        responseEntity.passwordExpiration == null

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "getUserByName returns the password expiration for a user - featureEnabled = #featureEnabled"() {
        given:
        identityConfig.getReloadableConfig().isIncludePasswordExpirationDateForGetUserResponsesEnabled() >> featureEnabled
        allowUserAccess()
        def user = new org.openstack.docs.identity.api.v2.User()
        def userEntity = entityFactory.createUser()
        EndUser caller = entityFactory.createUser()
        def pwdExpiration = new DateTime()
        userService.getUserByScopeAccess(_, false) >> caller
        userService.getUserByScopeAccess(_) >> caller
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.DEFAULT_USER, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.roleName)
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.IDENTITY_V20_LIST_USERS_GLOBAL.getRoleName()) >> true

        when: "get a provisioned user by ID"
        Response response = service.getUserByName(headers, authToken, userEntity.username).build()

        then:
        1 * userService.getUser(userEntity.username) >> userEntity
        1 * userConverter.toUser(userEntity) >> user
        if (featureEnabled) {
            1 * userService.getPasswordExpiration(userEntity) >> pwdExpiration
        } else {
            0 * userService.getPasswordExpiration(userEntity) >> pwdExpiration
        }
        response.status == 200
        def responseEntity = response.getEntity()
        if (featureEnabled) {
            new DateTime(responseEntity.passwordExpiration.toGregorianCalendar().getTime()) == pwdExpiration
        } else {
            responseEntity.passwordExpiration == null
        }

        where:
        featureEnabled << [true, false]
    }

    def "Fed Auth : Verify routing based on API version"() {
        mockAuthConverterCloudV20(service)
        def headers = Mock(HttpHeaders)

        when: "Set 2.0 API header"
        headers.getRequestHeader(GlobalConstants.HEADER_IDENTITY_API_VERSION) >> [GlobalConstants.FEDERATION_API_V2_0]
        service.authenticateFederated(headers, new byte[0], false)

        then: "Sent to v2.0"
        1 * federatedIdentityService.processV2SamlResponse(_, _)
    }

    def "Update identity provider with approvedDomainIds"() {
        given:
        def mockAuthorizationService = Mock(AuthorizationService)

        def mockFederatedIdentityService = Mock(FederatedIdentityService)
        service.federatedIdentityService = mockFederatedIdentityService
        service.authorizationService = mockAuthorizationService

        IdentityConfig identityConfig = Mock(IdentityConfig)
        IdentityConfig.ReloadableConfig reloadableConfig = Mock(IdentityConfig.ReloadableConfig)
        identityConfig.getReloadableConfig() >> reloadableConfig
        service.identityConfig = identityConfig

        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider idp = new com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.approvedDomainId = ["id", "id3", "id2"].asList()

            it.approvedDomainIds = approvedDomainIds
            it
        }
        def existingIdp = new IdentityProvider().with {
            it.name = "name"
            it.approvedDomainIds = ["id", "id2", "id3"].asList()
            it
        }
        mockFederatedIdentityService.checkAndGetIdentityProviderWithMetadataById(_) >> existingIdp
        mockAuthorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PROVIDER_MANAGER.getRoleName()) >> true

        when: "Update IDP with same list of approvedDomainIds"
        service.updateIdentityProvider(headers, uriInfo(), authToken, "id", idp)

        then: "Assert no query was made to retrieve all user not in approvedDomainIds list"
        0 * identityUserService.getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(_, _)

        when: "Update IDP with new list of approvedDomainIds"
        idp.approvedDomainIds.approvedDomainId.add("id4")
        service.updateIdentityProvider(headers, uriInfo(), authToken, "id", idp)

        then: "Assert query was made to retrieve all user not in approvedDomainIds list"
        1 * identityUserService.getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(_, _)
    }

    def "Update identityProvider's mapping policy with YAML mediaType" () {
        def mockFederatedIdentityService = Mock(FederatedIdentityService)
        service.federatedIdentityService = mockFederatedIdentityService
        headers.getMediaType() >> GlobalConstants.TEXT_YAML_TYPE
        def user = new User().with {
            it.domainId = "id"
            it
        }

        IdentityProvider identityProvider = new IdentityProvider()

        when:
        def response = service.updateIdentityProviderPolicy(headers, authToken, "id", '--- policy: name')

        then:
        1 * mockFederatedIdentityService.checkAndGetIdentityProvider(_) >> identityProvider
        1 * requestContextHolder.getRequestContext().getEffectiveCaller() >> user
        1 * mockFederatedIdentityService.updateIdentityProvider(_)
        1 * reloadableConfig.getGroupDefaultDomainId() >> "domainId"
        1 * userService.getGroupsForUser(_) >> []
        response.build().status == SC_NO_CONTENT
    }

    @Unroll
    def "Test repository config for default policy can be changed: type = #type" () {
        given:
        def mockFederatedIdentityService = Mock(FederatedIdentityService)
        service.federatedIdentityService = mockFederatedIdentityService
        mockFederatedIdentityService.checkAndGetIdentityProvider(_) >> new IdentityProvider()
        mockFederatedIdentityService.checkAndGetDefaultMappingPolicyProperty() >> new IdentityProperty().with {
            it.value = policy
            it.valueType = type
            it
        }
        def caller = entityFactory.createUser()
        requestContext.getEffectiveCaller() >> caller
        reloadableConfig.getGroupDefaultDomainId() >> "1"
        userService.getGroupsForUser(_) >> []
        headers.getAcceptableMediaTypes() >> [MediaType.APPLICATION_JSON_TYPE, GlobalConstants.TEXT_YAML_TYPE]

        when: "Getting default policy file"
        def response = service.getIdentityProviderPolicy(headers, authToken, "id")

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)

        where:
        policy                         | type
        '{"policy": {"name":"name"}"}' | "JSON"
        "--- policy: name: name"       | "YAML"
    }

    def "test cannot delete client role assigned to user groups"() {
        given:
        allowUserAccess()
        def roleId = RandomStringUtils.randomAlphanumeric(8)
        def role = new ClientRole().with {
            it.id = roleId
            it
        }

        when:
        def response = service.deleteRole(headers, "token", roleId).build()

        then:
        1 * applicationService.getClientRoleById(roleId) >> role
        1 * roleService.isRoleAssigned(roleId) >> 1
        response.status == SC_FORBIDDEN
        response.getEntity().message == DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE
    }

    def "Deleting a domain must also delete associated user groups"() {
        given:
        allowUserAccess()
        def domainId = "domainId"
        def domain = entityFactory.createDomain(domainId).with {
            it.enabled = false
            it
        }

        reloadableConfig.getTenantDefaultDomainId() >> "default"
        domainService.checkAndGetDomain(domainId) >> domain
        identityUserService.getEndUsersByDomainId(domainId, UserType.ALL) >> [].asList()

        UserGroup userGroup1 = new UserGroup().with {
            it.domainId = domainId
            it.name = "group1"
            it
        }

        UserGroup userGroup2 = new UserGroup().with {
            it.domainId = domainId
            it.name = "group2"
            it
        }

        when: "Delete a domain with multiple user groups"
        service.deleteDomain(authToken, domainId)

        then:
        1 * userGroupService.getGroupsForDomain(domainId) >> [userGroup1, userGroup2].asList()
        2 * userGroupService.deleteGroup(_)

        when: "Delete a domain with no user groups"
        service.deleteDomain(authToken, domainId)

        then:
        1 * userGroupService.getGroupsForDomain(domainId) >> [].asList()
        0 * userGroupService.deleteGroup(_)
    }

    def "updateIdentityProvider: avoid duplicate emailDomains"() {
        given:
        allowUserAccess()
        mockFederatedIdentityService(service)

        def emailDomain = "emailDomain.com"

        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider identityProvider = new com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider().with {
            it.emailDomains = new EmailDomains().with {
                it.emailDomain = [emailDomain, emailDomain, emailDomain.toUpperCase()].asList()
                it
            }
            it
        }

        when:
        service.updateIdentityProvider(headers, uriInfo(), authToken, "id", identityProvider)

        then:
        1 * federatedIdentityService.checkAndGetIdentityProviderWithMetadataById("id") >> new IdentityProvider()
        1 * federatedIdentityService.updateIdentityProvider(_ as IdentityProvider) >> { args ->
            IdentityProvider provider = args[0]
            assert provider.emailDomains.size() == 1
            null
        }
    }

    @Unroll
    def "listEndpointsForToken: service catalog is not filtered for impersonation tokens of suspended users, featureEnabled = #featureEnabled"() {
        given:
        def authToken = "authToken"
        def token = "token"
        def impersonatedToken = "impersonatedToken"
        def impersonatedTokenEntity = new UserScopeAccess()
        def impersonatedUser = new User()
        def serviceCatalogInfo = new ServiceCatalogInfo()
        allowUserAccess()
        mockJAXBObjectFactories(service)
        jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpoints(_) >> Mock(JAXBElement)
        requestContextHolder.getRequestContext().getSecurityContext().getCallerToken() >> impersonatedTokenEntity

        when:
        service.listEndpointsForToken(null, authToken, token, false)

        then: "correct services are called to build the service catalog"
        1 * scopeAccessService.getScopeAccessByAccessToken(token) >> new ImpersonatedScopeAccess().with {
            it.impersonatingToken = impersonatedToken
            it
        }
        1 * scopeAccessService.getScopeAccessByAccessToken(impersonatedToken) >> impersonatedTokenEntity
        1 * userService.getUserByScopeAccess(impersonatedTokenEntity, false) >> impersonatedUser
        1 * scopeAccessService.getServiceCatalogInfo(impersonatedUser) >> serviceCatalogInfo

        and: "services are called to determine if the user is suspended"
        1 * authorizationService.restrictTokenEndpoints(serviceCatalogInfo) >> true // The user is suspended
        1 * identityConfig.getReloadableConfig().shouldDisplayServiceCatalogForSuspendedUserImpersonationTokens() >> featureEnabled

        and: "objectFactory or endpoint converter is called based on if the service catalog should be populated"
        jaxbObjectFactories.getOpenStackIdentityV2Factory().createEndpointList() >> { args ->
            // This is only called if the feature to display the service catalog for impersonated tokens of suspended users is disabled
            assert !featureEnabled
            return new EndpointList()
        }
        endpointConverter.toEndpointList(_) >> { args ->
            assert featureEnabled
            return new EndpointList()
        }

        cleanup:
        service.jaxbObjectFactories = new JAXBObjectFactories()

        where:
        featureEnabled << [true, false]
    }

    def "listEndpointsForToken: Skip authorization checks if the token being used is the same token to list endpoints for token"() {
        given:
        def authToken = "authToken"
        def token = "token"
        def impersonatedTokenEntity = new UserScopeAccess()
        impersonatedTokenEntity.setAccessTokenString(token)
        requestContextHolder.getRequestContext().getSecurityContext().getCallerToken() >> impersonatedTokenEntity

        when: "token passed in url and impersonated token are same"
        service.listEndpointsForToken(null, authToken, token, false)

        then: "authorization service should skip the check for caller identity"
        0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.GET_TOKEN_ENDPOINTS_GLOBAL.getRoleName())
    }

    def "listEndpointsForToken: Do not skip authorization checks if the token being used is not same token to list endpoints for token"() {
        given:
        def authToken = "authToken"
        def token = "token"
        def impersonatedTokenEntity = new UserScopeAccess()
        impersonatedTokenEntity.setAccessTokenString("differentToken")
        requestContextHolder.getRequestContext().getSecurityContext().getCallerToken() >> impersonatedTokenEntity

        when: "token passed in url and impersonated token are not same"
        service.listEndpointsForToken(null, authToken, token, false)

        then: "authorization service should not skip the check for caller identity"
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityRole.GET_TOKEN_ENDPOINTS_GLOBAL.getRoleName())
    }

    @Unroll
    def "getIdentityProviders: list providers using no query params: userType = #userType"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser()
        def rcn = RandomStringUtils.randomAlphanumeric(8)
        def domain = entityFactory.createDomain().with {
            it.domainId = caller.domainId
            it.rackspaceCustomerNumber = rcn
            it
        }
        def otherDomainInRcn = entityFactory.createDomain().with {
            it.domainId = RandomStringUtils.randomAlphanumeric(8)
            it.rackspaceCustomerNumber = rcn
            it
        }
        def idpWithOneDomain = entityFactory.createIdentityProviderWithoutCertificate().with {
            it.approvedDomainIds = [caller.domainId]
            it
        }
        def idpWithTwoDomains = entityFactory.createIdentityProviderWithoutCertificate().with {
            it.approvedDomainIds = [caller.domainId, "domainNotInRcn"]
            it
        }
        if (userType == IdentityRole.RCN_ADMIN.roleName) {
            1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.RCN_ADMIN.roleName) >> true
            0 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.roleName,
                    IdentityUserTypeEnum.USER_MANAGER.roleName) >> false
            1 * domainService.getDomain(caller.domainId) >> domain
            1 * domainService.getDomainsByRCN(domain.rackspaceCustomerNumber) >> [domain, otherDomainInRcn]
            1 * federatedIdentityService.findIdentityProvidersExplicitlyApprovedForDomains([caller.domainId, otherDomainInRcn.domainId]) >> [idpWithOneDomain, idpWithTwoDomains]
        } else {
            1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityUserTypeEnum.USER_ADMIN.roleName,
                                                                                                IdentityUserTypeEnum.USER_MANAGER.roleName) >> true
            1 * federatedIdentityService.findIdentityProvidersExplicitlyApprovedForDomain(caller.domainId) >> [idpWithOneDomain, idpWithTwoDomains]
            0 * domainService.getDomainsByRCN(domain.rackspaceCustomerNumber)
        }

        when:
        service.getIdentityProviders(headers, authToken, new IdentityProviderSearchParams())

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * userService.getGroupsForUser(_) >> []
        1 * identityProviderConverterCloudV20.toIdentityProviderList(_) >> { args ->
            def idps = args[0]
            assert idps.size() == 1
            assert idps[0] == idpWithOneDomain
        }

        where:
        userType << [IdentityUserTypeEnum.USER_ADMIN.roleName,
                     IdentityUserTypeEnum.USER_MANAGER.roleName,
                     IdentityRole.RCN_ADMIN.roleName]
    }

    def "getIdentityProviders: list provider using emailDomain query param"() {
        given:
        allowUserAccess()
        mockFederatedIdentityService(service)

        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def emailDomain = "emailDomain.com"
        IdentityProviderSearchParams identityProviderSearchParams = new IdentityProviderSearchParams()
        identityProviderSearchParams.emailDomain = emailDomain

        when:
        service.getIdentityProviders(headers, authToken, identityProviderSearchParams)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * userService.getGroupsForUser(_) >> []
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(_)
    }

    def "getIdentityProviders: ignore empty/blank query params when used with emailDomain query param"() {
        given:
        allowUserAccess()
        mockFederatedIdentityService(service)

        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def emailDomain = "emailDomain.com"
        IdentityProviderSearchParams identityProviderSearchParams = new IdentityProviderSearchParams()
        identityProviderSearchParams.emailDomain = emailDomain
        identityProviderSearchParams.name = name
        identityProviderSearchParams.approvedDomainId = domainId
        identityProviderSearchParams.approvedTenantId = tenantId
        identityProviderSearchParams.issuer = issuer
        identityProviderSearchParams.idpType = idpType

        when:
        service.getIdentityProviders(headers, authToken, identityProviderSearchParams)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * userService.getGroupsForUser(_) >> []
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(_)

        where:
        name   | domainId | tenantId | issuer | idpType
        ""     | null     | null     | null   | null
        " "    | null     | null     | null   | null
        null   | ""       | null     | null   | null
        null   | " "      | null     | null   | null
        null   | null     | ""       | null   | null
        null   | null     | " "      | null   | null
        null   | null     | null     | ""     | null
        null   | null     | null     | " "    | null
        null   | null     | null     | null   | ""
        null   | null     | null     | null   | " "
    }

    def "getIdentityProviders: error check query params"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def emailDomain = "emailDomain.com"
        IdentityProviderSearchParams identityProviderSearchParams = new IdentityProviderSearchParams()

        when: "emailDomain with other query params"
        identityProviderSearchParams.emailDomain = emailDomain
        identityProviderSearchParams.name = name
        identityProviderSearchParams.approvedDomainId = domainId
        identityProviderSearchParams.approvedTenantId = tenantId
        identityProviderSearchParams.issuer = issuer
        identityProviderSearchParams.idpType = idpType
        def response = service.getIdentityProviders(headers, authToken, identityProviderSearchParams)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * userService.getGroupsForUser(_) >> []
        response.build().status == 400

        where:
        name   | domainId | tenantId | issuer | idpType
        "name" | null     | null     | null   | null
        null   | "id"     | null     | null   | null
        null   | null     | "id"     | null   | null
        null   | null     | null     | "iss"  | null
        null   | null     | null     | null   | "type"
    }

    def "listUsersForTenant: test with contactId query param"() {
        def tenantId = "tenantId"
        def contactId = "contactId"

        when:
        service.listUsersForTenant(headers, uriInfo(), authToken, tenantId, new ListUsersForTenantParams(null, contactId, new PaginationParams()))

        then:
        1 * requestContext.securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasTenantAccess(tenantId)
        1 * tenantService.getEnabledUsersWithContactIdForTenant(tenantId, contactId)
    }

    def "listUsersForTenant: error check"() {
        allowUserAccess()
        def tenantId = "tenantId"
        def contactId = "contactId"
        def roleId = "roleId"
        def scopeAccess = new UserScopeAccess()

        when: "provide both roleId and contactId query params"
        requestContext.securityContext.getAndVerifyEffectiveCallerToken(authToken) >> scopeAccess
        def response = service.listUsersForTenant(headers, uriInfo(), authToken, tenantId, new ListUsersForTenantParams(roleId, contactId, new PaginationParams()))

        then: "BadRequest"
        response.build().status == SC_BAD_REQUEST
    }

    @Unroll
    def "delete endpoint template requires the endpoint template to not be a member of an assignment rule - memberOfRule == #memberOfRule, expectedStatus == #expectedStatus"() {
        given:
        allowUserAccess()
        def endpointTemplateId = RandomStringUtils.randomAlphanumeric(8)
        def cloudBaseUrl = new CloudBaseUrl().with {
            it.baseUrlId = endpointTemplateId
            it.enabled = false
            it
        }
        endpointService.checkAndGetEndpointTemplate(endpointTemplateId) >> cloudBaseUrl
        tenantService.getTenantsForEndpoint(endpointTemplateId) >> []
        def rulesForEndpoint = []
        ruleService.findEndpointAssignmentRulesForEndpointTemplateId(endpointTemplateId) >> rulesForEndpoint
        if (memberOfRule) {
            rulesForEndpoint << new TenantTypeRule()
        }

        when:
        def response = service.deleteEndpointTemplate(headers, authToken, endpointTemplateId)

        then:
        response.build().status == expectedStatus

        where:
        memberOfRule    | expectedStatus
        true            | SC_FORBIDDEN
        false           | SC_NO_CONTENT
    }

    def "updateUser: Admin user updates federated user's contactId"() {
        given:
        allowUserAccess()
        def userId = "id"
        def contactId = "contactId"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setContactId(contactId)
        FederatedUser federatedUser = entityFactory.createFederatedUser().with {
            it.id = userId
            it
        }

        when:
        def response = service.updateUser(headers, authToken, userId, userForCreate).build()

        then:
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * identityUserService.updateFederatedUser(_)
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

        response.status == SC_OK
    }

    def "updateUser: federated user error check"() {
        given:
        allowUserAccess()
        def userId = "id"
        def contactId = "contactId"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setContactId(contactId)
        FederatedUser federatedUser = entityFactory.createFederatedUser().with {
            it.id = userId
            it
        }
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

        when: "access level role is not identity-admin or above"
        def response = service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "Response is 200 and 401 Forbidden will not be thrown"
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false
        0 * identityUserService.updateFederatedUser(_)

        response.status == SC_OK

        when: "federated user not found"
        response = service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "return 404"
        1 * identityUserService.getEndUserById(userId) >> null
        0 * identityUserService.updateFederatedUser(_)
        response.status == SC_NOT_FOUND
    }

    def "updateUser: validate provisioned and federated user's contactId length on update"() {
        given:
        allowUserAccess()
        def userId = "userId"
        def contactId = "contactId"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setContactId(contactId)
        FederatedUser federatedUser = entityFactory.createFederatedUser().with {
            it.id = userId
            it
        }
        User user = entityFactory.createUser().with {
            it.id = userId
            it
        }

        when: "update federated user"
        def response = service.updateUser(headers, authToken, userId, userForCreate).build()

        then:
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * validator20.validateStringMaxLength("contactId", contactId, Validator20.MAX_LENGTH_64)

        response.status == SC_OK

        when: "update provisioned user"
        response = service.updateUser(headers, authToken, userId, userForCreate).build()

        then:
        2 * identityUserService.getEndUserById(userId) >> user
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * validator20.validateStringMaxLength("contactId", contactId, Validator20.MAX_LENGTH_64)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
        1 * authorizationContext.getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * userService.updateUser(_)

        response.status == SC_OK
    }

    @Unroll
    def "updateUser: in order to update a user's username the feature must be enabled and the user must have the required role: updateUsernameFeatureEnabled = #updateUsernameFeatureEnabled, userHasRole = #userHasRole"() {
        given:
        allowUserAccess()
        def usernameCanBeUpdated = updateUsernameFeatureEnabled && userHasRole
        def userId = "userId"
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def userForCreate = new UserForCreate().with {
            it.username = "newUsername"
            it
        }

        when:
        service.updateUser(headers, authToken, userId, userForCreate)

        then:
        // The caller is checked for the update username role only if the feature is enabled
        (0..1) * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_UPDATE_USERNAME.getRoleName()) >> userHasRole
        1 * identityConfig.getReloadableConfig().isUsernameUpdateAllowed() >> updateUsernameFeatureEnabled
        1 * userService.isUsernameUnique(userForCreate.username) >> true
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        if (usernameCanBeUpdated) {
            1 * userService.updateUser(_)
            2 * identityUserService.getEndUserById(userId) >> user // this service is called a second time after the update to load the new values
        } else {
            0 * userService.updateUser(_)
            1 * identityUserService.getEndUserById(userId) >> user
        }

        where:
        [updateUsernameFeatureEnabled, userHasRole] << [[true, false], [true, false]].combinations()
    }

    def "federated auth does not expose the core contactId"() {
        given:
        mockAuthConverterCloudV20(service)
        UserForAuthenticateResponse user = new UserForAuthenticateResponse().with {
            it.contactId = "contactId"
            it.domainId = "domainId"
            it.id = "id"
            it.federatedIdp = "idp"
            it
        }
        AuthenticateResponse authenticateResponse = new AuthenticateResponse().with {
            it.user = user
            it.token = v2Factory.createToken()
            it
        }
        when:
        def response = service.authenticateFederated(headers, new byte[0], false).build()

        then:
        1 * authConverterCloudV20.toAuthenticationResponse(_) >> authenticateResponse

        response.status == SC_OK
        response.entity.user.contactId == null
    }

    def "adding global role to user with existing tenant role throw 400 BadRequest"() {
        given:
        allowUserAccess()

        def roleId = "roleId"
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def tenantRole = entityFactory.createTenantRole().with {
            it.tenantIds.add("tenantId")
            it
        }

        when:
        def response = service.addUserRole(headers, authToken, user.id, roleId).build()

        then:
        response.status == SC_BAD_REQUEST

        1 * applicationService.getClientRoleById(roleId) >> entityFactory.createClientRole()
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * tenantService.getTenantRoleForUserById(user, roleId) >> tenantRole
    }

    def "adding role to user on tenant with existing global role throw 400 BadRequest"() {
        given:
        def roleId = "roleId"
        def tenantId = "tenantId"
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def tenantRole = entityFactory.createTenantRole()

        when:
        def response = service.addRolesToUserOnTenant(headers, authToken, tenantId, user.id, roleId).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.verifyEffectiveCallerIsNotARacker()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * applicationService.getClientRoleById(roleId) >> entityFactory.createClientRole()
        1 * tenantService.getTenantRoleForUserById(user, roleId) >> tenantRole

        response.status == SC_BAD_REQUEST
    }

    def "grantRolesToUser: grant roles to user"() {
        given:
        allowUserAccess()
        mockRoleAssignmentConverter(service)
        def userId = "userId"
        def user = new com.rackspace.idm.domain.entity.User().with {
            it.id = userId
            it
        }

        def caller = new com.rackspace.idm.domain.entity.User().with {
            it.id = "callerId"
            it
        }

        def assignment = new TenantAssignment().with {
            ta ->
                ta.onRole = roleId
                ta.forTenants.add("tenantId")
                ta
        }
        RoleAssignments roleAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(assignment)
                    tas
            }
            it
        }

        when:
        def response = service.grantRolesToUser(headers, authToken, userId, roleAssignments).build()

        then:
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getTargetEndUser() >> user
        1 * authorizationService.verifyEffectiveCallerHasManagementAccessToUser(userId)
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * userService.replaceRoleAssignmentsOnUser(user, roleAssignments, IdentityUserTypeEnum.USER_ADMIN)
        1 * userService.getRoleAssignmentsOnUser(user, _) >> new PaginatorContext<>()
        1 * roleAssignmentConverter.toRoleAssignmentsWeb(_)

        response.status == SC_OK
    }

    def "grantRolesToUser: error check"() {
        given:
        allowUserAccess()
        def userId = "userId"

        when: "roleAssignment is null"
        def response = service.grantRolesToUser(headers, authToken, userId, null).build()

        then:
        response.status == SC_BAD_REQUEST
    }

    @Unroll
    def "addUserToDomain: calls correct service methods - userType = #userType"() {
        given:
        def domain = entityFactory.createDomain("someDomainId")
        def user = entityFactory.createUser()
        def role = entityFactory.createTenantRole()

        when:
        def response = service.addUserToDomain(authToken, domain.domainId, user.id).build()

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> userType
        1 * tenantService.getGlobalRolesForUser(user) >> [role]
        1 * userService.updateUser(user)
        1 * delegationService.removeConsumerFromExplicitDelegationAgreementAssignments(user)

        if (IdentityUserTypeEnum.SERVICE_ADMIN == userType || IdentityUserTypeEnum.IDENTITY_ADMIN == userType) {
            1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
            0 * domainService.getDomainAdmins(_)
            0 * domainService.updateDomainUserAdminDN(_)
        }  else if (IdentityUserTypeEnum.USER_ADMIN == userType) {
            0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
            1 * domainService.getDomainAdmins(domain.domainId) >> []
            1 * domainService.updateDomainUserAdminDN(user)
        } else if (IdentityUserTypeEnum.USER_MANAGER == userType || IdentityUserTypeEnum.DEFAULT_USER == userType) {
            0 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
            0 * domainService.getDomainAdmins(_)
            0 * domainService.updateDomainUserAdminDN(_)
        }

        response.status == SC_NO_CONTENT

        where:
        userType << IdentityUserTypeEnum.values()
    }

    def "addUserToDomain: error check for user-admins"() {
        given:
        allowUserAccess()
        def domain = entityFactory.createDomain("someDomainId")
        def user = entityFactory.createUser()
        def existingUserAdmin = entityFactory.createUser().with {
            it.domainId = domain.domainId
            it
        }

        when: "domain has an existing user-admin"
        service.addUserToDomain(authToken, domain.domainId, user.id)

        then:
        thrown(ForbiddenException)
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.USER_ADMIN
        1 * domainService.getDomainAdmins(domain.domainId) >> [existingUserAdmin]
    }

    def "removeTenantFromDomain deletes tenant assigned roles off of the DA for the tenant"() {
        given:
        allowUserAccess()
        def tenantId = RandomStringUtils.randomAlphanumeric(8)
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def tenantRole = entityFactory.createTenantRole().with {
            it.tenantIds << tenantId
            it
        }

        when:
        service.removeTenantFromDomain(authToken, domainId, tenantId)

        then:
        1 * domainService.removeTenantFromDomain(tenantId, domainId)
        1 * delegationService.getTenantRolesForDelegationAgreementsForTenant(tenantId) >> [tenantRole]
        1 * tenantService.deleteTenantFromTenantRole(tenantRole, tenantId)
    }

    @Unroll
    def "addTenantToDomain deletes tenant assigned roles off of the DA for the tenant: tenantDomainId = #tenantDomainId , domainTenantIds = #domainTenantIds, tenantRoleShouldBeDeleted = #tenantRoleShouldBeDeleted"() {
        given:
        allowUserAccess()
        def domainId = "domainId"
        def tenantId = "tenantId"
        def tenant = entityFactory.createTenant().with {
            it.tenantId = tenantId
            it.domainId = tenantDomainId
            it
        }
        def domain = entityFactory.createDomain().with {
            it.domainId = domainId
            it.tenantIds = domainTenantIds
            it
        }
        def tenantRole = entityFactory.createTenantRole().with {
            it.tenantIds << tenantId
            it
        }

        // roles are only deleted on the DA if the tenant does not point to the domain and the domain does not point to the tenant
        if (tenantRoleShouldBeDeleted) {
            1 * delegationService.getTenantRolesForDelegationAgreementsForTenant(tenantId) >> [tenantRole]
            1 * tenantService.deleteTenantFromTenantRole(tenantRole, tenantId)
        } else {
            0 * delegationService.getTenantRolesForDelegationAgreementsForTenant(tenantId) >> [tenantRole]
            0 * tenantService.deleteTenantFromTenantRole(tenantRole, tenantId)
        }

        tenantService.getTenantIdsForDomain(_) >> domainTenantIds

        when: "add tenant to domain where tenant already points to that domain"
        service.addTenantToDomain(authToken, domainId, tenantId)

        then:
        1 * tenantService.checkAndGetTenant(tenantId) >> tenant
        1 * domainService.addTenantToDomain(tenant, domain)
        1 * domainService.checkAndGetDomain(domainId) >> domain

        where:
        tenantDomainId  | domainTenantIds   | tenantRoleShouldBeDeleted
        null            | null              | true
        null            | []                | true
        null            | ["otherTenantId"] | true
        "otherDomainId" | null              | true
        "otherDomainId" | []                | true
        "otherDomainId" | ["otherTenantId"] | true
        null            | ["tenantId"]      | false
        "otherDomainId" | ["tenantId"]      | false
        "domainId"      | null              | false
        "domainId"      | []                | false
        "domainId"      | ["tenantId"]      | false
        "domainId"      | ["otherTenantId"] | false
    }

    def "verify addTenantToDomain uses getDeleteAllTenantRolesWhenTenantIsRemovedFromDomain feature flag"() {
        given:
        allowUserAccess()
        def domainId = "domainId"
        def tenantId = "tenantId"
        def tenant = entityFactory.createTenant().with {
            it.tenantId = tenantId
            it.domainId = "domainId2"
            it
        }
        def domain = entityFactory.createDomain().with {
            it.domainId = domainId
            it.tenantIds = []
            it
        }

        when:
        reloadableConfig.getDeleteAllTenantRolesWhenTenantIsRemovedFromDomain() >> true
        service.addTenantToDomain(authToken, domainId, tenantId)

        then:
        1 * tenantService.checkAndGetTenant(tenantId) >> tenant
        1 * domainService.checkAndGetDomain(domainId) >> domain
        1 * tenantService.getTenantRolesForTenant(tenantId)

        when:
        reloadableConfig.getDeleteAllTenantRolesWhenTenantIsRemovedFromDomain() >> false
        service.addTenantToDomain(authToken, domainId, tenantId)

        then:
        1 * tenantService.checkAndGetTenant(tenantId) >> tenant
        1 * domainService.checkAndGetDomain(domainId) >> domain
        0 * tenantService.getTenantRolesForTenant(tenantId)
    }

    def "verify removeTenantFromDomain uses getDeleteAllTenantRolesWhenTenantIsRemovedFromDomain feature flag"() {
        given:
        allowUserAccess()
        def domainId = "domainId"
        def tenantId = "tenantId"

        when:
        reloadableConfig.getDeleteAllTenantRolesWhenTenantIsRemovedFromDomain() >> true
        service.removeTenantFromDomain(authToken, domainId, tenantId)

        then:
        1 * tenantService.getTenantRolesForTenant(tenantId)

        when:
        reloadableConfig.getDeleteAllTenantRolesWhenTenantIsRemovedFromDomain() >> false
        service.removeTenantFromDomain(authToken, domainId, tenantId)

        then:
        0 * tenantService.getTenantRolesForTenant(tenantId)
    }

    def "listUserGroups: allow using a DA tokens to retrieve groups"() {
        given:
        def domain = entityFactory.createDomain().with {
            it.userAdminDN = new DN("rsId=id")
            it
        }
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def agreement = entityFactory.createDelegationAgreement()
        def daToken = entityFactory.createUserToken().with {
            it.userRsId = caller.id
            it.delegationAgreementId = "daId"
            it
        }

        when:
        service.listUserGroups(headers, authToken, caller.id)

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> daToken
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daToken.delegationAgreementId) >> agreement
        1 * domainService.getDomain(agreement.getDomainId()) >> domain
        1 * identityUserService.getGroupsForEndUser(_)
    }

    def "listUserGroups: error check"() {
        given:
        mockExceptionHandler(service)
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }
        def agreement = entityFactory.createDelegationAgreement()
        def daToken = entityFactory.createUserToken().with {
            it.userRsId = caller.id
            it.delegationAgreementId = "daId"
            it
        }

        when: "user-admin's DN not set on domain"
        service.listUserGroups(headers, authToken, caller.id)

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> daToken
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daToken.delegationAgreementId) >> agreement
        1 * domainService.getDomain(agreement.getDomainId()) >> entityFactory.createDomain() // Domain with no userAdmin DN
        1 * exceptionHandler.exceptionResponse(_ as NotFoundException) >> Response.serverError()

        when: "DA set on token was not found"
        service.listUserGroups(headers, authToken, caller.id)

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken) >> daToken
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * delegationService.getDelegationAgreementById(daToken.delegationAgreementId) >> null
        1 * exceptionHandler.exceptionResponse(_ as NotFoundException) >> Response.serverError()
    }

    def "sendUnverifiedUserInvite: calls correct services"() {
        given:
        def domainId = "domainId"
        def caller = entityFactory.createUser().with {
            it.domainId = domainId
            it
        }
        def unverifiedUser = entityFactory.createUser().with {
            it.email = "test@rackspace.com"
            it.domainId = domainId
            it.unverified = true
            it
        }

        when: "caller is a user admin"
        service.sendUnverifiedUserInvite(headers, uriInfo(), authToken, unverifiedUser.id)

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getProvisionedUserById(unverifiedUser.id) >> unverifiedUser
        1 * authorizationService.getIdentityTypeRoleAsEnum(caller) >> IdentityUserTypeEnum.USER_ADMIN
        1 * identityConfig.getReloadableConfig().getUnverifiedUserRegistrationCodeSize() >> 32
        1 * userService.updateUser(unverifiedUser)
        1 * emailClient.asyncSendUnverifiedUserInviteMessage(unverifiedUser) >> { args ->
            com.rackspace.idm.domain.entity.User user = args[0]
            assert user.registrationCode != null
            assert user.inviteSendDate != null
            assert user.registrationCode.size() == 32
        }

        when: "caller is a service admin"
        caller.domainId = "otherDomain"
        service.sendUnverifiedUserInvite(headers, uriInfo(), authToken, unverifiedUser.id)

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getProvisionedUserById(unverifiedUser.id) >> unverifiedUser
        1 * authorizationService.getIdentityTypeRoleAsEnum(caller) >> IdentityUserTypeEnum.SERVICE_ADMIN
        1 * identityConfig.getReloadableConfig().getUnverifiedUserRegistrationCodeSize() >> 32
        1 * userService.updateUser(unverifiedUser)
        1 * emailClient.asyncSendUnverifiedUserInviteMessage(unverifiedUser) >> { args ->
            com.rackspace.idm.domain.entity.User user = args[0]
            assert user.registrationCode != null
            assert user.inviteSendDate != null
            assert user.registrationCode.size() == 32
        }

        when: "caller is an identity admin"
        caller.domainId = "otherDomain"
        service.sendUnverifiedUserInvite(headers, uriInfo(), authToken, unverifiedUser.id)

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getProvisionedUserById(unverifiedUser.id) >> unverifiedUser
        1 * authorizationService.getIdentityTypeRoleAsEnum(caller) >> IdentityUserTypeEnum.IDENTITY_ADMIN
        1 * identityConfig.getReloadableConfig().getUnverifiedUserRegistrationCodeSize() >> 32
        1 * userService.updateUser(unverifiedUser)
        1 * emailClient.asyncSendUnverifiedUserInviteMessage(unverifiedUser) >> { args ->
            com.rackspace.idm.domain.entity.User user = args[0]
            assert user.registrationCode != null
            assert user.inviteSendDate != null
            assert user.registrationCode.size() == 32
        }
    }

    def "sendUnverifiedUserInvite: error check"() {
        given:
        mockExceptionHandler(service)
        def domainId = "domainId"
        def caller = entityFactory.createUser().with {
            it.domainId = domainId
            it
        }
        def unverifiedUser = entityFactory.createUser().with {
            it.email = "test@rackspace.com"
            it.domainId = domainId
            it.unverified = true
            it
        }

        when: "unverified user not found"
        service.sendUnverifiedUserInvite(headers, uriInfo(), authToken, unverifiedUser.id)

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getProvisionedUserById(unverifiedUser.id) >> null
        1 * exceptionHandler.exceptionResponse(_ as NotFoundException) >> Response.serverError()


        when: "caller is a user admin in another domain"
        caller.domainId = "otherDomain"
        service.sendUnverifiedUserInvite(headers, uriInfo(), authToken, unverifiedUser.id)

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getProvisionedUserById(unverifiedUser.id) >> unverifiedUser
        1 * authorizationService.getIdentityTypeRoleAsEnum(caller) >> IdentityUserTypeEnum.USER_ADMIN
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()

        when: "caller is a user manager in another domain"
        service.sendUnverifiedUserInvite(headers, uriInfo(), authToken, unverifiedUser.id)

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * identityUserService.getProvisionedUserById(unverifiedUser.id) >> unverifiedUser
        1 * authorizationService.getIdentityTypeRoleAsEnum(caller) >> IdentityUserTypeEnum.USER_MANAGER
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()

        when: "caller is a default user in same domain"
        service.sendUnverifiedUserInvite(headers, uriInfo(), authToken, unverifiedUser.id)

        then:
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, null) >> {
            throw new ForbiddenException()
        }
        1 * exceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "acceptUnverifiedUserInvite: calls correct services"() {
        given:
        // Setup mocks
        mockValidator20(service)
        mockJAXBObjectFactories(service)
        service.userConverterCloudV20 = Mock(UserConverterCloudV20)

        def userId = "id"
        def registrationCode = "code"
        UserForCreate user = new UserForCreate().with {
            it.id = userId
            it.username = "username"
            it.registrationCode = registrationCode
            it.password = "password"
            it.secretQA = v2Factory.createSecretQA()
            it
        }
        def entityUser = entityFactory.createUser().with {
            it.id = userId
            it.registrationCode = registrationCode
            it.inviteSendDate = new Date()
            it.unverified = true
            it.enabled = false
            it.username = null
            it
        }

        when:
        def response = service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        response.build().status == SC_OK

        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        // Validate username
        1 * validator20.validateRequiredAttribute("username", user.username)
        1 * validator.validateUsername(user.username)
        // Validate password
        1 * validator20.validateRequiredAttribute("password", user.password)
        1 * validator.validatePasswordForCreateOrUpdate(user.password)
        // Validate secretQA
        1 * validator20.validateRequiredAttribute("answer", user.secretQA.answer)
        1 * validator20.validateRequiredAttribute("question", user.secretQA.question)

        1 * identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 48
        1 * userService.updateUser(entityUser)
        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        1 * service.userConverterCloudV20.toUser(entityUser) >> v2Factory.createUser()
        1 * openStackIdentityV2Factory.createUser(_) >> new JAXBElement<User>(org.openstack.docs.identity.api.v2.ObjectFactory._User_QNAME, User.class, null, v2Factory.createUser())
    }

    def "acceptUnverifiedUserInvite: allows to set optional attribute phone pin"() {
        given:
        // Setup mocks
        mockJAXBObjectFactories(service)
        service.userConverterCloudV20 = Mock(UserConverterCloudV20)

        def userId = "id"
        def registrationCode = "code"
        UserForCreate user = new UserForCreate().with {
            it.id = userId
            it.username = "username"
            it.registrationCode = registrationCode
            it.password = "password"
            it.secretQA = v2Factory.createSecretQA()
            it.phonePin = "342156"
            it
        }
        def entityUser = entityFactory.createUser().with {
            it.id = userId
            it.registrationCode = registrationCode
            it.inviteSendDate = new Date()
            it.unverified = true
            it.enabled = false
            it.username = null
            it
        }

        identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 48
        service.userConverterCloudV20.toUser(entityUser) >> v2Factory.createUser()
        openStackIdentityV2Factory.createUser(_) >> new JAXBElement<User>(org.openstack.docs.identity.api.v2.ObjectFactory._User_QNAME, User.class, null, v2Factory.createUser())

        when:
        def response = service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        response.build().status == SC_OK

        1 * userService.updateUser(entityUser) >> { args ->
            def u = args[0]
            assert u.phonePin == user.phonePin
        }
        1 * validator20.validatePhonePin(user.phonePin)
    }

    def "acceptUnverifiedUserInvite: Not allowed to update phone PIN if PIN is locked"() {
        given:
        // Setup mocks
        mockJAXBObjectFactories(service)
        service.userConverterCloudV20 = Mock(UserConverterCloudV20)

        def userId = "id"
        def registrationCode = "code"
        UserForCreate user = new UserForCreate().with {
            it.id = userId
            it.username = "username"
            it.registrationCode = registrationCode
            it.password = "password"
            it.secretQA = v2Factory.createSecretQA()
            it.phonePin = "342156"
            it
        }
        User entityUser = Mock()
        entityUser.id >> userId
        entityUser.registrationCode >> registrationCode
        entityUser.inviteSendDate >> new Date()
        entityUser.unverified >> true
        entityUser.enabled >> false
        entityUser.username >> null

        identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 48
        service.userConverterCloudV20.toUser(entityUser) >> v2Factory.createUser()
        openStackIdentityV2Factory.createUser(_) >> new JAXBElement<User>(org.openstack.docs.identity.api.v2.ObjectFactory._User_QNAME, User.class, null, v2Factory.createUser())

        when:
        def responseBuilder = service.acceptUnverifiedUserInvite(headers, uriInfo(), user)
        def response = responseBuilder.build()
        def responseEntity = response.getEntity()

        then:
        1 * entityUser.phonePinState >> PhonePinStateEnum.LOCKED
        0 * entityUser.updatePhonePin(user.phonePin)
        0 * userService.updateUser(entityUser)
        0 * validator20.validatePhonePin(user.phonePin)

        and:
        response.status == SC_FORBIDDEN
        responseEntity.code == HttpStatus.SC_FORBIDDEN
        responseEntity.message == "Error code: 'PP-004'; User's PIN is locked."
    }

    def "acceptUnverifiedUserInvite: When update phone PIN, call appropriate update PIN service"() {
        given:
        // Setup mocks
        mockJAXBObjectFactories(service)
        service.userConverterCloudV20 = Mock(UserConverterCloudV20)

        def userId = "id"
        def registrationCode = "code"
        UserForCreate user = new UserForCreate().with {
            it.id = userId
            it.username = "username"
            it.registrationCode = registrationCode
            it.password = "password"
            it.secretQA = v2Factory.createSecretQA()
            it.phonePin = "342156"
            it
        }
        User entityUser = Mock()
        entityUser.id >> userId
        entityUser.registrationCode >> registrationCode
        entityUser.inviteSendDate >> new Date()
        entityUser.unverified >> true
        entityUser.enabled >> false
        entityUser.username >> null

        identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 48
        service.userConverterCloudV20.toUser(entityUser) >> v2Factory.createUser()
        openStackIdentityV2Factory.createUser(_) >> new JAXBElement<User>(org.openstack.docs.identity.api.v2.ObjectFactory._User_QNAME, User.class, null, v2Factory.createUser())

        when:
        def response = service.acceptUnverifiedUserInvite(headers, uriInfo(), user).build()

        then:
        1 * entityUser.phonePinState >> PhonePinStateEnum.ACTIVE
        1 * userService.updateUser(entityUser)
        1 * validator20.validatePhonePin(user.phonePin)

        and:
        1 * entityUser.updatePhonePin(user.phonePin)

        and: "results are appropriate"
        response.status == SC_OK
    }

    def "acceptUnverifiedUserInvite: error check"() {
        given:
        // Setup Mocks
        mockExceptionHandler(service)
        mockJAXBObjectFactories(service)

        service.userConverterCloudV20 = Mock(UserConverterCloudV20)
        def userId = "id"
        def registrationCode = "code"
        UserForCreate user = new UserForCreate().with {
            it.id = userId
            it.username = "username"
            it.registrationCode = registrationCode
            it.password = "password"
            it.secretQA = v2Factory.createSecretQA()
            it
        }
        def entityUser = entityFactory.createUser().with {
            it.id = userId
            it.registrationCode = registrationCode
            it.inviteSendDate = new Date()
            it.unverified = true
            it.enabled = false
            it.username = null
            it
        }
        def invalidUser = entityFactory.createUser().with {
            it.unverified = false
            it
        }

        when: "user is not found"
        service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        1 * identityUserService.getProvisionedUserById(user.getId()) >> null
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND_MESSAGE)
            return Response.status(SC_NOT_FOUND)
        }

        when: "user is not unverified"
        service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        1 * identityUserService.getProvisionedUserById(user.getId()) >> invalidUser
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND_MESSAGE)
            return Response.status(SC_NOT_FOUND)
        }

        when: "invite has not been sent"
        user.registrationCode = null
        service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND_MESSAGE)
            return Response.status(SC_NOT_FOUND)
        }

        when: "invalid registration code"
        user.registrationCode = "badCode"
        service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND, ErrorCodes.ERROR_CODE_UNVERIFIED_USERS_INVITE_NOT_FOUND_MESSAGE)
            return Response.status(SC_NOT_FOUND)
        }

        when: "username is not unique"
        user.registrationCode = registrationCode
        service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        1 * validator20.validateRequiredAttribute("username", user.username)
        1 * identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 48
        1 * validator.validateUsername(user.username) >> {throw new DuplicateUsernameException()}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            return Response.status(SC_CONFLICT)
        }

        when: "secretQA not provided"
        user.secretQA = null
        service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        1 * identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 48
        1 * validator20.validateRequiredAttribute("username", user.username)
        1 * validator.validateUsername(user.username)
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], BadRequestException, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE, "Secret question and answer are required attributes.")
            return Response.status(SC_BAD_REQUEST)
        }

        when: "expired registration code"
        user.secretQA = v2Factory.createSecretQA()
        service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        1 * identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 0
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], ForbiddenException, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, "Your registration code has expired, please request a new invite.")
            return Response.status(SC_FORBIDDEN)
        }

        when: "invalid phone pin"
        user.phonePin = "invalid"
        def response = service.acceptUnverifiedUserInvite(headers, uriInfo(), user)

        then:
        response.build().status == SC_BAD_REQUEST

        1 * identityUserService.getProvisionedUserById(user.getId()) >> entityUser
        1 * identityConfig.getReloadableConfig().getUnverifiedUserInvitesTTLHours() >> 48
        1 * validator20.validatePhonePin("invalid") >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            return Response.status(SC_BAD_REQUEST)
        }

        0 * userService.updateUser(_)
    }

    def "updateUser: update unverified user's contactId"() {
        given:
        allowUserAccess()
        mockJAXBObjectFactories(service)
        def user = entityFactory.createUnverifiedUser()
        def userForUpdate = new UserForCreate().with {
            it.contactId = "contactId"
            it
        }

        when:
        def response = service.updateUser(headers, authToken, user.id, userForUpdate)

        then:
        response.build().status == SC_OK

        1 * identityUserService.getEndUserById(user.id) >> user
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * userService.updateUser(user)
        1 * identityUserService.getEndUserById(user.id) >> user
        1 * service.userConverterCloudV20.toUser(user) >> v2Factory.createUser()
        1 * openStackIdentityV2Factory.createUser(_) >> new JAXBElement<User>(org.openstack.docs.identity.api.v2.ObjectFactory._User_QNAME, User.class, null, v2Factory.createUser())
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
    }

    def "updateUser: verify that only the contactId of a unverified user can be updated"() {
        given:
        allowUserAccess()
        mockJAXBObjectFactories(service)
        def user = entityFactory.createUnverifiedUser()
        def userForUpdate = new UserForCreate().with {
            it.contactId = "contactId"
            it.username = "badUsername"
            it.email = "email@test.com"
            it.domainId = "badDomainId"
            it
        }

        when:
        def response = service.updateUser(headers, authToken, user.id, userForUpdate)

        then:
        response.build().status == SC_OK

        1 * identityUserService.getEndUserById(user.id) >> user
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * userService.updateUser(user) >> { args ->
            com.rackspace.idm.domain.entity.User userEntity = args[0]
            assert userEntity.domainId == user.domainId
            assert userEntity.email == user.email
            assert userEntity.contactId == userForUpdate.contactId
            assert userEntity.domainId == user.domainId
        }
        1 * identityUserService.getEndUserById(user.id) >> user
        1 * service.userConverterCloudV20.toUser(user) >> v2Factory.createUser()
        1 * openStackIdentityV2Factory.createUser(_) >> new JAXBElement<User>(org.openstack.docs.identity.api.v2.ObjectFactory._User_QNAME, User.class, null, v2Factory.createUser())
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

    }

    def "updateUser: assert only admins are allowed to update the contactId of a unverified user"() {
        given:
        allowUserAccess()
        mockExceptionHandler(service)

        def user = entityFactory.createUnverifiedUser()
        def userForUpdate = new UserForCreate().with {
            it.contactId = "contactId"
            it
        }

        when:
        service.updateUser(headers, authToken, user.id, userForUpdate)

        then:

        1 * identityUserService.getEndUserById(user.id) >> user
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], ForbiddenException, null, "Not Authorized")
            return Response.status(SC_FORBIDDEN)
        }
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()
    }

    def "updateUser: verifies user's contactId can not be empty"() {
        given:
        allowUserAccess()
        mockExceptionHandler(service)

        // Create test users
        def user = entityFactory.createUser()
        def unverifiedUser = entityFactory.createUnverifiedUser()
        def fedUser = entityFactory.createFederatedUser()

        def userForUpdate = new UserForCreate().with {
            it.contactId = "contactId"
            it
        }

        when: "provisioned user update"
        service.updateUser(headers, authToken, user.id, userForUpdate)

        then:
        1 * identityUserService.getEndUserById(user.id) >> user
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * validator20.validateAttributeIsNotEmpty("contactId", userForUpdate.contactId) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            return Response.status(SC_BAD_REQUEST)
        }
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

        when: "unverified user update"
        service.updateUser(headers, authToken, unverifiedUser.id, userForUpdate)

        then:
        1 * identityUserService.getEndUserById(unverifiedUser.id) >> unverifiedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * validator20.validateAttributeIsNotEmpty("contactId", userForUpdate.contactId) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            return Response.status(SC_BAD_REQUEST)
        }
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

        when: "federated user update"
        service.updateUser(headers, authToken, fedUser.id, userForUpdate)

        then:
        1 * identityUserService.getEndUserById(fedUser.id) >> fedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true
        1 * validator20.validateAttributeIsNotEmpty("contactId", userForUpdate.contactId) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            return Response.status(SC_BAD_REQUEST)
        }
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> entityFactory.createUser()

    }

    def "getTenantById - returns valid response"() {
        given:
        mockTenantConverter(service)
        mockJAXBObjectFactories(service)

        when:
        def response = service.getTenantById(headers, authToken, "tenantId").build()

        then:
        response.status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createTenant(_) >> Mock(JAXBElement)
        1 * tenantConverter.toTenant(_)
    }

    def "getDomainTenants - returns valid response"() {
        given:
        mockTenantConverter(service)
        mockJAXBObjectFactories(service)
        mockIdmCommonUtils(service)

        when:
        def response = service.getDomainTenants(authToken, "tenantId", "true").build()

        then:
        response.status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * domainService.checkAndGetDomain(_) >> entityFactory.createDomain()
        1 * idmCommonUtils.getBoolean("true") >> true
        1 * tenantService.getTenantsByDomainId(_, true) >> []
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createTenants(_) >> Mock(JAXBElement)
        1 * tenantConverter.toTenantList(_)
    }

    def "updateTenant - returns valid response"() {
        given:
        mockTenantConverter(service)
        mockJAXBObjectFactories(service)
        def tenant = v2Factory.createTenant()

        when:
        def response = service.updateTenant(headers, authToken, "tenantId", tenant).build()

        then:
        response.status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * validator20.validateTenantType(tenant)
        1 * tenantService.checkAndGetTenant(_)  >> entityFactory.createTenant()
        1 * tenantService.updateTenant(_)
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createTenant(_) >> Mock(JAXBElement)
        1 * tenantConverter.toTenant(_)
    }

    def "addTenant uses identity admin when feature flag is disabled"() {
        given:
        allowUserAccess()
        mockTenantConverter(service)
        domainService.getDomain(_) >> entityFactory.createDomain("defaultDomain")
        def tenant = new org.openstack.docs.identity.api.v2.Tenant()
        tenantConverter.toTenant(_) >> entityFactory.createTenant()
        tenant.name = "name"
        service.jaxbObjectFactories = new JAXBObjectFactories()

        when:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> false
        def response = service.addTenant(headers, uriInfo(), authToken, tenant).build()

        then:
        response.status == SC_CREATED

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);
    }

    def "addTenant uses service-admin or rs-tenant-admin role when feature flag is enabled"() {
        given:
        allowUserAccess()
        mockTenantConverter(service)
        domainService.getDomain(_) >> entityFactory.createDomain("defaultDomain")
        def tenant = new org.openstack.docs.identity.api.v2.Tenant()
        tenant.name = "name"
        service.jaxbObjectFactories = new JAXBObjectFactories()

        when:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def response = service.addTenant(headers, uriInfo(), authToken, tenant).build()

        then:
        response.status == SC_CREATED

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
    }

    def "addTenantToDomain uses identity admin when feature flag is disabled"() {
        given:
        allowUserAccess()
        domainService.checkAndGetDomain(_) >> entityFactory.createDomain()
        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()

        when:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> false
        def response = service.addTenantToDomain(authToken, "domainId", "tenantId").build()

        then:
        response.status == SC_NO_CONTENT

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);
    }

    def "addTenantToDomain uses service-admin or rs-tenant-admin role when feature flag is enabled"() {
        given:
        allowUserAccess()
        domainService.checkAndGetDomain(_) >> entityFactory.createDomain()
        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()

        when:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def response = service.addTenantToDomain(authToken, "domainId", "tenantId").build()

        then:
        response.status == SC_NO_CONTENT

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
    }

    def "deleteTenant uses identity admin when feature flag is disabled"() {
        given:
        when:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> false
        def response = service.deleteTenant(headers, authToken, "tenantId").build()

        then:
        response.status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "deleteTenant uses service-admin or rs-tenant-admin role when feature flag is enabled"() {
        given:
        allowUserAccess()

        when:
        reloadableConfig.isUseRoleForTenantManagementEnabled() >> true
        def response = service.deleteTenant(headers, authToken, "tenantId").build()

        then:
        response.status == SC_NO_CONTENT

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_TENANT_ADMIN.getRoleName());
    }

    def "addDomain uses service-admin or rs-domain-admin role"() {
        given:
        allowUserAccess()
        mockDomainConverter(service)
        domainService.getDomain(_) >> entityFactory.createDomain("defaultDomain")
        def domain = v1Factory.createDomain()

        when:
        def response = service.addDomain(authToken, uriInfo(), domain).build()

        then:
        response.status == SC_CREATED

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName());
    }

    def "deleteDomain uses service-admin or rs-domain-admin role when feature flag is enabled"() {
        given:
        allowUserAccess()
        def domainId = "domainId"
        def domain = entityFactory.createDomain(domainId).with {
            it.enabled = false
            it
        }
        reloadableConfig.getTenantDefaultDomainId() >> "default"
        domainService.checkAndGetDomain(_) >> domain
        identityUserService.getEndUsersByDomainId(domainId, UserType.ALL) >> [].asList()
        userGroupService.getGroupsForDomain(domainId) >> [].asList()

        when:
        def response = service.deleteDomain(authToken, "domainId").build()

        then:
        response.status == SC_NO_CONTENT

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_DOMAIN_ADMIN.getRoleName());
    }

    def "addEndpointTemplate uses identity admin when feature flag is disabled"() {
        given:
        mockEndpointConverter(service)
        def endpointTemplate = v1Factory.createEndpointTemplate().with {
            it.serviceId = "serviceId"
            it
        }
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createEndpointTemplate(endpointTemplate.name)

        when:
        reloadableConfig.isUseRoleForEndpointManagementEnabled() >> false
        def response = service.addEndpointTemplate(headers, uriInfo(), authToken, endpointTemplate).build()

        then:
        response.status == SC_CREATED

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "addEndpointTemplate uses service-admin or rs-endpoint-admin role when feature flag is enabled"() {
        given:
        allowUserAccess()
        mockEndpointConverter(service)
        def endpointTemplate = v1Factory.createEndpointTemplate().with {
            it.serviceId = "serviceId"
            it
        }
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createEndpointTemplate(endpointTemplate.name)

        when:
        reloadableConfig.isUseRoleForEndpointManagementEnabled() >> true
        def response = service.addEndpointTemplate(headers, uriInfo(), authToken, endpointTemplate).build()

        then:
        response.status == SC_CREATED

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_ENDPOINT_ADMIN.getRoleName());
    }

    def "deleteEndpointTemplate uses identity admin when feature flag is disabled"() {
        given:
        def endpointTemplate = entityFactory.createCloudBaseUrl().with {
            it.enabled = false
            it
        }
        endpointService.checkAndGetEndpointTemplate(_) >> endpointTemplate
        tenantService.getTenantsForEndpoint(_) >> [].asList()

        when:
        reloadableConfig.isUseRoleForEndpointManagementEnabled() >> false
        def response = service.deleteEndpointTemplate(headers, authToken, "endpointId").build()

        then:
        response.status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
    }

    def "deleteEndpointTemplate uses service-admin or rs-endpoint-admin role when feature flag is enabled"() {
        given:
        allowUserAccess()
        def endpointTemplate = entityFactory.createCloudBaseUrl().with {
            it.enabled = false
            it
        }
        endpointService.checkAndGetEndpointTemplate(_) >> endpointTemplate
        tenantService.getTenantsForEndpoint(_) >> [].asList()


        when:
        reloadableConfig.isUseRoleForEndpointManagementEnabled() >> true
        def response = service.deleteEndpointTemplate(headers, authToken, "endpointId").build()

        then:
        response.status == SC_NO_CONTENT

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_ENDPOINT_ADMIN.getRoleName());
    }

    def "updateEndpointTemplate uses service admin when feature flag is disabled"() {
        given:
        mockEndpointConverter(service)
        def endpointTemplateId = "100"
        def endpointTemplate = v1Factory.createEndpointTemplate().with {
            it.id = 100
            it.serviceId = "serviceId"
            it
        }
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createEndpointTemplate(endpointTemplate.name)
        endpointService.checkAndGetEndpointTemplate(_) >> entityFactory.createCloudBaseUrl()

        when:
        reloadableConfig.isUseRoleForEndpointManagementEnabled() >> false
        def response = service.updateEndpointTemplate(headers, uriInfo(), authToken, endpointTemplateId, endpointTemplate).build()

        then:
        response.status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
    }

    def "updateEndpointTemplate uses service-admin or rs-endpoint-admin role when feature flag is enabled"() {
        given:
        allowUserAccess()
        mockEndpointConverter(service)
        def endpointTemplateId = "100"
        def endpointTemplate = v1Factory.createEndpointTemplate().with {
            it.id = 100
            it.serviceId = "serviceId"
            it
        }
        endpointConverter.toCloudBaseUrl(_) >> entityFactory.createEndpointTemplate(endpointTemplate.name)
        endpointService.checkAndGetEndpointTemplate(_) >> entityFactory.createCloudBaseUrl()

        when:
        reloadableConfig.isUseRoleForEndpointManagementEnabled() >> true
        def response = service.updateEndpointTemplate(headers, uriInfo(), authToken, endpointTemplateId, endpointTemplate).build()

        then:
        response.status == SC_OK

        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.SERVICE_ADMIN, IdentityRole.IDENTITY_RS_ENDPOINT_ADMIN.getRoleName());
    }

    def "revokeTokenById: allow user-admin to revoke a federated user's token"() {
        given:
        def tokenId = "fedTokenId"
        def caller = entityFactory.createUser()
        def fedUser = entityFactory.createFederatedUser()

        when:
        def response = service.revokeToken(headers, authToken, tokenId).build()

        then:
        response.status == SC_NO_CONTENT

        1 * securityContext.getEffectiveCallerToken() >> new UserScopeAccess()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * scopeAccessService.getScopeAccessByAccessToken(tokenId) >> entityFactory.createFederatedToken()
        1 * identityUserService.getEndUserById(_) >> fedUser
        1 * authorizationService.verifyDomain(caller, fedUser)
        1 * tokenRevocationService.revokeToken(tokenId)
    }

    def "revokeTokenById: error check"() {
        given:
        def tokenId = "fedTokenId"
        def caller = entityFactory.createUser()
        def fedUser = entityFactory.createFederatedUser()

        when: "revoke token of fed user not within user-admin's domain"
        service.revokeToken(headers, authToken, tokenId)

        then:
        thrown(ForbiddenException)

        1 * securityContext.getEffectiveCallerToken() >> new UserScopeAccess()
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * scopeAccessService.getScopeAccessByAccessToken(tokenId) >> entityFactory.createFederatedToken()
        1 * identityUserService.getEndUserById(_) >> fedUser
        1 * authorizationService.verifyDomain(caller, fedUser) >> {throw new ForbiddenException()}
        0 * tokenRevocationService.revokeToken(tokenId)
    }

    @Unroll
    def "listEndpointTemplates - calls correct services: serviceId = #serviceId"() {
        given:
        def application = entityFactory.createApplication().with {
            it.openStackType = "type"
            it.clientId = serviceId
            it
        }
        def cloudBaseUrl = entityFactory.createCloudBaseUrl()

        when:
        def response = service.listEndpointTemplates(headers, authToken, serviceId)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        if (StringUtils.isBlank(serviceId)) {
            1 * endpointService.getBaseUrls() >> [cloudBaseUrl]
        } else {
            1 * applicationService.checkAndGetApplication(serviceId) >> application
            1 * endpointService.getBaseUrlsByServiceType(application.getOpenStackType()) >> [cloudBaseUrl]
        }

        where:
        serviceId << [null, "", "id"]
    }

    def "listRolesForTenant - calls correct services"(){
        given:
        def tenantId = "tenantId"
        def tenant = entityFactory.createTenant().with {
            it.tenantId = tenantId
            it
        }

        when:
        def response = service.listRolesForTenant(headers, authToken, tenantId, null, null)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * tenantService.checkAndGetTenant(tenantId) >> tenant
        1 * tenantService.getTenantRolesForTenant(tenant.getTenantId()) >> []
    }

    @Unroll
    def "listRolesForUserOnTenant - calls correct services: applyRcnRoles = #applyRcnRoles"(){
        given:
        def tenantId = "tenantId"
        def tenant = entityFactory.createTenant().with {
            it.tenantId = tenantId
            it
        }
        def caller = entityFactory.createUser()
        def userId = "userId"
        def user = entityFactory.createUser().with {
            it.id = userId
            it
        }

        when:
        def response = service.listRolesForUserOnTenant(headers, authToken, tenantId, userId, applyRcnRoles)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * tenantService.checkAndGetTenant(tenantId) >> tenant
        1 * userService.checkAndGetUserById(userId) >> user
        1 * authorizationService.isSelf(caller, user) >> false
        if (applyRcnRoles) {
            1 * tenantService.getEffectiveTenantRolesForUserOnTenantApplyRcnRoles(user, tenant) >> []
        } else {
            1 * tenantService.getEffectiveTenantRolesForUserOnTenant(user, tenant) >> []
        }

        where:
        applyRcnRoles << [true, false]
    }

    @Unroll
    def "listServices - calls correct services: serviceName = #serviceName"(){
        given:
        def paginatorContext = Mock(PaginatorContext)
        def application = entityFactory.createApplication()

        when:
        def response = service.listServices(headers, uriInfo(), authToken, serviceName, offset, limit)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        if (StringUtils.isBlank(serviceName)) {
            1 * applicationService.getOpenStackServices(offset, limit) >> paginatorContext
            1 * applicationPaginator.createLinkHeader(_, paginatorContext)
            1 * paginatorContext.getValueList() >> [application]
        } else {
            1 * applicationService.getByName(serviceName) >> application
        }

        where:
        serviceName << [null, "", "serviceName"]
    }

    def "listGroups - calls correct services"() {
        given:
        def group = entityFactory.createGroup("id", "name", "desc")

        when:
        def response = service.listGroups(headers, authToken, null, offset, limit)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.getGroups(offset, limit) >> [group]
        1 * cloudKsGroupBuilder.build(group) >> v2Factory.createGroup(group.name)
    }

    def "getGroup - calls correct services"() {
        given:
        def group = entityFactory.createGroup("id", "name", "desc")

        when:
        def response = service.getGroup(headers, authToken, group.name)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.getGroupByName(group.name) >> group
        1 * cloudKsGroupBuilder.build(group) >> v2Factory.createGroup(group.name)
    }

    def "deleteDomainPasswordPolicy - calls correct services"() {
        given:
        def domainId = "domainId"
        def domain = entityFactory.createDomain(domainId)
        def caller = entityFactory.createUser().with {
            it.domainId = domainId
            it
        }

        when:
        def response = service.deleteDomainPasswordPolicy(headers, authToken, domainId)

        then:
        response.build().status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN)
        1 * domainService.getDomain(domainId) >> domain
        1 * requestContextHolder.getRequestContext().getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * requestContextHolder.getRequestContext().getEffectiveCaller() >> caller
        1 * domainService.deleteDomainPasswordPolicy(domainId)
    }

    @Unroll
    def "getAccessibleDomains - calls correct services: rcn = #rcn"() {
        given:
        mockDomainPaginator(service)
        mockDomainConverter(service)

        def caller = entityFactory.createUser()
        def paginatorContext = Mock(PaginatorContext)

        when:
        def response = service.getAccessibleDomains(uriInfo(), authToken, offset, limit, rcn)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        if (StringUtils.isNotBlank(rcn)) {
            1 * domainService.getDomainsByRCN(rcn, offset, limit) >> paginatorContext
        } else {
            1 * domainService.getDomains(offset, limit) >> paginatorContext
        }
        1 * domainPaginator.createLinkHeader(_, paginatorContext)
        1 * paginatorContext.getValueList() >> [entityFactory.createDomain()]

        where:
        rcn << [null, "" , "rcn"]
    }

    @Unroll
    def "getAccessibleDomains - domain base access level - calls correct services: rcn = #rcn"() {
        given:
        mockDomainPaginator(service)
        mockDomainConverter(service)

        def caller = entityFactory.createUser()
        def domain = entityFactory.createDomain()
        def otherDomain = entityFactory.createDomain().with {
            it.domainId = "otherDomainId"
            it.rackspaceCustomerNumber = "rcn"
            it
        }
        def tenants = [entityFactory.createTenant()]

        when:
        def response = service.getAccessibleDomains(uriInfo(), authToken, offset, limit, rcn)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.USER_ADMIN
        1 * domainService.getDomain(caller.getDomainId()) >> domain
        1 * tenantService.getTenantsForUserByTenantRoles(caller) >> tenants
        1 * domainService.getDomainsForTenants(tenants) >> [otherDomain]

        and: "verify domains returned"
        domainConverter.toDomains(_) >> { arg ->
            Domains domains = arg[0]
            if (StringUtils.isNotBlank(rcn))
                assert domains.domain.size() == 1
            else
                assert domains.domain.size() == 2
            return new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains()
        }

        where:
        rcn << [null, "" , "rcn"]
    }

    @Unroll
    def "getAccessibleDomainsForUser - calls correct services"() {
        given:
        mockDomainPaginator(service)
        mockDomainConverter(service)

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()
        def domain = entityFactory.createDomain()
        def otherDomain = entityFactory.createDomain().with {
            it.domainId = "otherDomainId"
            it.rackspaceCustomerNumber = "rcn"
            it
        }
        def tenants = [entityFactory.createTenant()]

        when:
        def response = service.getAccessibleDomainsForUser(authToken, userId)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById(userId) >> user
        1 * authorizationService.isSelf(caller, user) >> false
        1 * requestContext.getEffectiveCallerAuthorizationContext().getIdentityUserType() >> callerType
        1 * domainService.getDomain(user.getDomainId()) >> domain
        1 * tenantService.getTenantsForUserByTenantRoles(user) >> tenants
        1 * domainService.getDomainsForTenants(tenants) >> [otherDomain]

        and: "verify services called based on caller"
        if (callerType.isDomainBasedAccessLevel()) {
            1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        } else {
            0 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_)
        }

        and: "verify domains returned"
        domainConverter.toDomains(_) >> { arg ->
            Domains domains = arg[0]
            assert domains.domain.size() == 2
            return new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains()
        }

        where:
        callerType << [IdentityUserTypeEnum.SERVICE_ADMIN, IdentityUserTypeEnum.IDENTITY_ADMIN, IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    def "addRegion - calls correct services"() {
        given:
        mockRegionConverter(service)

        def region = v1Factory.createRegion()

        when:
        def response = service.addRegion(uriInfo(), authToken, region)

        then:
        response.build().status == SC_CREATED

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
        1 * cloudRegionService.addRegion(_)
    }

    def "getRegion - calls correct services"() {
        given:
        mockRegionConverter(service)

        def region = "ORD"

        when:
        def response = service.getRegion(authToken, region)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
        1 * cloudRegionService.checkAndGetRegion(region) >> entityFactory.createRegion()
    }

    def "getRegions - calls correct services"() {
        given:
        mockRegionConverter(service)

        when:
        def response = service.getRegions(authToken)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
        1 * cloudRegionService.getRegions(_) >> [entityFactory.createRegion()]
    }

    def "updateRegion - calls correct services"() {
        given:
        mockRegionConverter(service)
        def region = v1Factory.createRegion()

        when:
        def response = service.updateRegion(authToken, region.name, region)

        then:
        response.build().status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
        1 * cloudRegionService.checkAndGetRegion(region.name)
        1 * cloudRegionService.updateRegion(region.name, _)
    }

    def "deleteRegion - calls correct services"() {
        given:
        mockRegionConverter(service)
        def region = "ORD"

        when:
        def response = service.deleteRegion(authToken, region)

        then:
        response.build().status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
        1 * cloudRegionService.checkAndGetRegion(region)
        1 * cloudRegionService.deleteRegion(region)
    }

    def "getGroupById - calls correct services"() {
        given:
        def group = entityFactory.createGroup("id", "group", "desc")

        when:
        def response = service.getGroupById(headers, authToken, group.groupId)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.checkAndGetGroupById(group.groupId) >> group
    }

    def "addGroup - calls correct services"() {
        given:
        def groupForCreate = v1Factory.createGroup("group", "id", "desc")
        def group = entityFactory.createGroup("id", "group", "desc")

        when:
        def response = service.addGroup(headers, uriInfo(), authToken, groupForCreate)

        then:
        response.build().status == SC_CREATED

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * cloudGroupBuilder.build(groupForCreate) >> group
        1 * groupService.addGroup(_)
        1 * cloudKsGroupBuilder.build(_) >> groupForCreate
    }

    def "updateGroup - calls correct services"() {
        given:
        def groupForUpdate = v1Factory.createGroup("group", "id", "desc")
        def group = entityFactory.createGroup("id", "group", "desc")

        when:
        def response = service.updateGroup(headers, authToken, groupForUpdate.id, groupForUpdate)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * cloudGroupBuilder.build(groupForUpdate) >> group
        1 * groupService.updateGroup(_)
        1 * cloudKsGroupBuilder.build(_) >> groupForUpdate
    }

    def "deleteGroup - calls correct services"() {
        given:
        def groupId = "groupId"

        when:
        def response = service.deleteGroup(headers, authToken, groupId)

        then:
        response.build().status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.deleteGroup(groupId)
    }

    def "addUserToGroup - calls correct services"() {
        given:
        def group = entityFactory.createGroup("groupId", "name", "desc")
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser().with {
            it.id = "callerId"
            it
        }

        when:
        def response = service.addUserToGroup(headers, authToken, group.groupId, user.id)

        then:
        response.build().status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.checkAndGetGroupById(group.groupId) >> group
        1 * identityUserService.checkAndGetUserById(user.id) >> user
        1 * authorizationService.hasDefaultUserRole(user) >> false
        1 * userService.isUserInGroup(user.id, group.groupId) >> false
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        1 * authorizationService.hasUserAdminRole(user) >> false
        1 * identityUserService.addGroupToEndUser(group.groupId, user.id)
        1 * atomHopperClient.asyncPost((User) user, FeedsUserStatusEnum.GROUP, _)
    }

    def "removeUserFromGroup - calls correct services"() {
        given:
        def group = entityFactory.createGroup("groupId", "name", "desc")
        def user = entityFactory.createUser()

        when:
        def response = service.removeUserFromGroup(headers, authToken, group.groupId, user.id)

        then:
        response.build().status == SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.checkAndGetGroupById(group.groupId) >> group
        1 * identityUserService.checkAndGetUserById(user.id) >> user
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.USER_ADMIN
        1 * userService.isUserInGroup(user.id, group.groupId) >> true
        1 * identityUserService.getEndUsersByDomainId(user.domainId, UserType.ALL) >> []
        1 * identityUserService.removeGroupFromEndUser(group.groupId, user.id)
        1 * atomHopperClient.asyncPost((User) user, FeedsUserStatusEnum.GROUP, _)
    }

    def "getUsersForGroup - calls correct services"() {
        given:
        def group = entityFactory.createGroup("id", "name", "desc")

        when:
        def response = service.getUsersForGroup(headers, authToken, group.groupId, offset, limit)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * groupService.checkAndGetGroupById(group.groupId)
        1 * userService.getEnabledUsersByGroupId(group.groupId, offset, limit) >> Mock(PaginatorContext)
    }

    def "setUserEnabled - calls correct services"() {
        given:
        def userForUpdate = v2Factory.createUser()
        def user = entityFactory.createUser()

        when:
        def response = service.setUserEnabled(headers, authToken, userForUpdate.id, userForUpdate)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * userService.checkAndGetUserById(userForUpdate.id) >> user
        1 * userService.updateUser(_)
    }

    def "updateUserApiKeyCredentials - calls correct services"() {
        given:
        def user = entityFactory.createUser()
        def apiKeyCred = v2Factory.createApiKeyCredentials("username", "key")

        when:
        def response = service.updateUserApiKeyCredentials(headers, authToken, user.id, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS, apiKeyCred)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN)
        1 * validator20.validateUsername(apiKeyCred.username)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * userService.updateUser(user)
    }

    @Unroll
    def "resetUserApiKeyCredentials - calls correct services - callerType = #callerType"() {
        given:
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()

        when:
        def response = service.resetUserApiKeyCredentials(headers, authToken, user.id, JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)

        then:
        response.build().status == SC_OK

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        1 * userService.checkAndGetUserById(user.id) >> user
        1 * authorizationContext.getIdentityUserType() >> callerType
        if (callerType == IdentityUserTypeEnum.USER_ADMIN) {
            1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user)
        }
        if (callerType == IdentityUserTypeEnum.IDENTITY_ADMIN) {
            1 * authorizationService.hasServiceAdminRole(user)
        }
        1 * userService.updateUser(user)

        where:
        callerType << IdentityUserTypeEnum.values()
    }

    def "updateUser: update phone pin for Fed User"() {
        given:
        def userId = "userId"
        def phonePin = "786124"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setPhonePin(phonePin)

        FederatedUser federatedUser = entityFactory.createFederatedUser().with {
            it.id = userId
            it
        }

        when: "update federated user for phone pin"
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "test invocation phone pin validator and updateFederatedUser"
        1 * validator20.validatePhonePin(phonePin)
        1 * identityUserService.updateFederatedUser(federatedUser)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> federatedUser
        2 * identityUserService.getEndUserById(userId) >> federatedUser
    }

    def "updateUser: update phone pin for user"() {
        given:
        def userId = "userId"
        def phonePin = "786124"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setPhonePin(phonePin)

        User provisionedUser = entityFactory.createUser().with {
            it.id = userId
            it
        }

        when: "update provisioned user for phone pin"
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "test invocation phone pin validator"
        1 * validator20.validatePhonePin(phonePin)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> provisionedUser
        2 * identityUserService.getEndUserById(userId) >> provisionedUser
    }


    def "updateUser: update phone pin for Provisioned User"() {
        given:
        def userId = "userId"
        def otherUserId = "otherUserId"
        def phonePin = "786124"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setPhonePin(phonePin)

        User user = entityFactory.createUser().with {
            it.id = userId
            it.phonePin = "123786"
            it
        }

        User otherUser = entityFactory.createUser().with {
            it.id = otherUserId
            it
        }

        when: "update user phone pin is requested by user him self with valid pin and feature flag ON"
        def response = service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin is validated and get updated"
        1 * validator20.validatePhonePin(phonePin)
        2 * identityUserService.getEndUserById(userId) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * userService.updateUser(_)

        response.status == SC_OK

        when: "update user phone pin is requested not by him self but by any other user with valid pin"
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin is not validated"
        0 * validator20.validatePhonePin(phonePin)
        2 * identityUserService.getEndUserById(userId) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> otherUser

        when: "update user phone pin is requested by user him self with null pin"
        userForCreate.setPhonePin(null)
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin validation is skipped"
        0 * validator20.validatePhonePin(phonePin)
        2 * identityUserService.getEndUserById(userId) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user

        when: "update user phone pin is requested by impersonated user"
        userForCreate.setPhonePin(null)
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin validation is skipped"
        0 * validator20.validatePhonePin(phonePin)
        2 * identityUserService.getEndUserById(userId) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user

        when: "phone pin passed in request is same as original/existing phone pin"
        userForCreate.setPhonePin("123786")
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin validation is skipped"
        0 * validator20.validatePhonePin(_)
        2 * identityUserService.getEndUserById(userId) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
    }

    def "updateUser: update phone pin for Federated User"() {
        given:
        allowUserAccess()
        def userId = "fedUserId"
        def otherUserId = "otherUserId"
        def phonePin = "786124"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setPhonePin(phonePin)

        User otherUser = entityFactory.createUser().with {
            it.id = otherUserId
            it
        }

        FederatedUser federatedUser = entityFactory.createFederatedUser().with {
            it.id = userId
            it.phonePin = "123786"
            it.phonePinAuthenticationFailureCount = 3
            it
        }

        when: "update federated user for phone pin "
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin validator is invoked and fed user service is invoked"
        1 * validator20.validatePhonePin(phonePin)
        1 * identityUserService.updateFederatedUser(federatedUser)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> federatedUser
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        and: "phone pin failure count is reset to 0"
        federatedUser.phonePinAuthenticationFailureCountNullSafe == 0

        when: "update federated user with phone pin as null"
        userForCreate.setPhonePin(null)
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin validator and updateFederatedUser are not invoked"
        0 * validator20.validatePhonePin(_)
        0 * identityUserService.updateFederatedUser(federatedUser)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> federatedUser
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        when: "update federated user with phone pin by some other user"
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin validator and updateFederatedUser are not invoked"
        0 * validator20.validatePhonePin(phonePin)
        0 * identityUserService.updateFederatedUser(federatedUser)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> otherUser
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        when: "phone pin passed in request is same as original/existing phone pin"
        userForCreate.setPhonePin("123786")
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin validation is skipped"
        0 * validator20.validatePhonePin(_)
        0 * identityUserService.updateFederatedUser(federatedUser)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> otherUser
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        when: "Update just contactId on federated user"
        federatedUser.phonePinAuthenticationFailureCount = 3
        federatedUser.phonePin = "022776"
        userForCreate.setPhonePin(null)
        userForCreate.setContactId("12345")
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "User is updated"
        1 * identityUserService.updateFederatedUser(federatedUser)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> federatedUser
        2 * identityUserService.getEndUserById(userId) >> federatedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        and: "phone pin is not changed"
        federatedUser.phonePin == "022776"
        federatedUser.phonePinAuthenticationFailureCountNullSafe == 3
        federatedUser.contactId == "12345"
    }

    def "updateUser: Impersonated user cannot update phone pin"() {
        given:
        def userId = "userId"
        def phonePin = "786124"
        UserForCreate userForCreate = new UserForCreate()
        userForCreate.setPhonePin(phonePin)

        User user = entityFactory.createUser().with {
            it.id = userId
            it.phonePin = phonePin
            it
        }

        when: "update user phone pin is requested by impersonated user"
        requestContextHolder.getRequestContext().getSecurityContext().isImpersonatedRequest() >> true
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin is validated and get updated"
        0 * validator20.validatePhonePin(phonePin)
        2 * identityUserService.getEndUserById(userId) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * userService.updateUser(_)

        when: "update user phone pin is requested by Racker impersonated user"
        requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest() >> true
        service.updateUser(headers, authToken, userId, userForCreate).build()

        then: "phone pin is validation and update is skipped"
        0 * validator20.validatePhonePin(phonePin)
        2 * identityUserService.getEndUserById(userId) >> user
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled() >> user
        1 * userService.updateUser(_)
    }


    def "unlockPhonePin: test with different callers trying to unlock a phone pin"() {
        given:
        def userId = "userId"
        def otherUserId = "otherUserId"

        User user = entityFactory.createUser().with {
            it.id = userId
            it.phonePin = "123786"
            it
        }
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> user

        when: "user unlocks his own phone pin"
        def response = service.unlockPhonePin(authToken, userId).build()

        then:
        1 * phonePinService.unlockPhonePin(userId)
        response.status == SC_NO_CONTENT

        when: "user without phone pin admin role unlocks other user's phone pin"
        response = service.unlockPhonePin(authToken, otherUserId).build()

        then:
        0 * phonePinService.unlockPhonePin(otherUserId)
        response.status == SC_FORBIDDEN

        when: "user with phone pin admin role unlocks other user's phone pin"
        response = service.unlockPhonePin(authToken, userId).build()

        then:
        1 * phonePinService.unlockPhonePin(userId)
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.IDENTITY_PHONE_PIN_ADMIN.getRoleName()) >> true
        response.status == SC_NO_CONTENT
    }

    def mockServices() {
        mockEndpointConverter(service)
        mockRackerAuthenticationService(service)
        mockAuthorizationService(service)
        mockApplicationService(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockGroupService(service)
        mockUserService(service)
        mockDefaultRegionService(service)
        mockDomainService(service)
        mockCloudRegionService(service)
        mockQuestionService(service)
        mockSecretQAService(service)
        mockEndpointService(service)
        mockFederatedIdentityService(service)
        mockMultiFactorCloud20Service(service)
        mockRoleService(service)
        mockIdentityUserService(service)
        mockIdentityConfig(service)
        mockAuthResponseService(service)
        mockRequestContextHolder(service)
        mockRoleConverter(service)
        mockUserGroupService(service)
        mockRuleService(service)
        mockPhonePinService(service)
        mockDelegationService(service)
        mockFederatedIdentityService(service)
        mockIdentityProviderConverterCloudV20(service)
        mockTokenRevocationService(service)
        mockDomainConverter(service)
    }

    def mockMisc() {
        mockAtomHopperClient(service)
        mockConfiguration(service)
        mockCloudGroupBuilder(service)
        mockCloudKsGroupBuilder(service)
        mockValidator(service)
        mockValidator20(service)
        mockPrecedenceValidator(service)
        mockUserPaginator(service)
        mockApplicationPaginator(service)
        mockEndUserPaginator(service)
        mockAuthWithToken(service)
        mockAuthWithApiKeyCredentials(service)
        mockAuthWithPasswordCredentials(service)
        mockUserConverter(service)
        mockServiceConverter(service)
        mockSamlUnmarshaller(service)
        mockIdmPathUtils(service)
        mockEmailClient(service)
    }

    def createFilter(FilterParam.FilterParamName name, String value) {
        return new FilterParam(name, value)
    }

    def createAuthenticationRequest(boolean useRsaCreds) {
        def mockedElement = Mock(JAXBElement)
        if (useRsaCreds) {
            mockedElement.getValue() >> v1Factory.createRsaCredentials()
        } else {
            mockedElement.getValue() >> v2Factory.createPasswordCredentialsBase()
        }
        new AuthenticationRequest().with {
            it.credential = mockedElement
            return it
        }
    }
}
