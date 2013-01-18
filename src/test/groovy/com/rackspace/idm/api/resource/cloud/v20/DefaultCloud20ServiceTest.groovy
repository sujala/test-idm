package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.PolicyConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.resource.pagination.PaginatorContext
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.ExceptionHandler
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotAuthorizedException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.domain.entity.*
import com.unboundid.ldap.sdk.ReadOnlyEntry
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.Role
import spock.lang.Shared
import testHelpers.RootServiceTest

import testHelpers.V1Factory
import testHelpers.V2Factory

import testHelpers.EntityFactory

import javax.xml.bind.JAXBElement

class DefaultCloud20ServiceTest extends RootServiceTest {

    @Shared ExceptionHandler exceptionHandler
    @Shared JAXBObjectFactories objFactories

    @Shared ScopeAccess scopeAccessMock

    @Shared def offset = "0"
    @Shared def limit = "25"
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def questionId = "id"
    @Shared def roleId = "roleId"
    @Shared def userId = "userId"

    @Shared def expiredDate
    @Shared def futureDate
    @Shared def refreshDate

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")

        //service being tested
        defaultCloud20Service = new DefaultCloud20Service()

        exceptionHandler = new ExceptionHandler()
        objFactories = new JAXBObjectFactories()

        exceptionHandler.objFactories = objFactories

        defaultCloud20Service.exceptionHandler = exceptionHandler
        defaultCloud20Service.objFactories = objFactories
    }

    def setup() {
        mockServices()
        mockMisc()

        config.getBoolean("useCloudAuth") >> false

        headers = Mock()
        jaxbMock = Mock(JAXBElement)
    }

    def "validateOffset null offset sets offset to 0"() {
        when:
        def offset = defaultCloud20Service.validateOffset(null)

        then:
        offset == 0
    }

    def "validateOffset negative offset throws bad request"() {
        when:
        defaultCloud20Service.validateOffset("-5")

        then:
        thrown(BadRequestException)
    }

    def "validateOffset blank offset throws bad request"() {
        when:
        defaultCloud20Service.validateOffset("")

        then:
        thrown(BadRequestException)
    }

    def "validateOffset valid offset sets offset"() {
        when:
        def offset = defaultCloud20Service.validateOffset("10")

        then:
        offset == 10
    }

    def "validateLimit null limit sets limit to default"() {
        when:
        config.getInt(_) >> 25
        def limit = defaultCloud20Service.validateLimit(null)

        then:
        limit == 25
    }

    def "validateLimit negative limit throws bad request"() {
        when:
        defaultCloud20Service.validateLimit("-5")

        then:
        thrown(BadRequestException)
    }

    def "validateLimit blank limit throws bad request"() {
        when:
        defaultCloud20Service.validateLimit("")

        then:
        thrown(BadRequestException)
    }

    def "validateLimit limit is 0 sets to default"() {
        when:
        config.getInt(_) >> 25
        def limit = defaultCloud20Service.validateLimit("0")

        then:
        limit == 25
    }

    def "validateLimit limit is too large sets to default max"() {
        when:
        config.getInt(_) >> 99
        def value = 100
        def limit = defaultCloud20Service.validateLimit(value.toString())

        then:
        limit == 99
    }

    def "validateLimit limit is valid sets limit"() {
        when:
        config.getInt(_) >> 100
        def value = 99
        def limit = defaultCloud20Service.validateLimit(value.toString())

        then:
        limit == value
    }

    def "question create verifies Identity admin level access and adds Question"() {
        given:
        mockQuestionConverter(defaultCloud20Service)
        allowAccess()


        when:
        def response = defaultCloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion()).build()

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
        1 * questionService.addQuestion(_) >> "questionId"
        response.getStatus() == 201
        response.getMetadata().get("location")[0] != null
    }

    def "question create handles exceptions"() {
        given:
        mockQuestionConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]

        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        questionService.addQuestion(_) >> { throw new BadRequestException() }

        when:
        def response1 = defaultCloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())
        def response2 = defaultCloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())
        def response3 = defaultCloud20Service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401
        response2.build().status == 403
        response3.build().status == 400
    }

    def "question delete verifies Identity admin level access and deletes question"() {
        given:
        mockQuestionConverter(defaultCloud20Service)
        allowAccess()

        when:
        def response = defaultCloud20Service.deleteQuestion(authToken, questionId).build()

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
        1 * questionService.deleteQuestion(questionId)

        response.getStatus() == 204
    }

    def "question delete handles exceptions"() {
        given:
        mockQuestionConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        questionService.deleteQuestion(questionId) >> { throw new NotFoundException() }

        when:
        def response1 = defaultCloud20Service.deleteQuestion(authToken, questionId).build()
        def response2 = defaultCloud20Service.deleteQuestion(authToken, questionId).build()
        def response3 = defaultCloud20Service.deleteQuestion(authToken, questionId).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 404

    }

    def "question update verifies Identity admin level access"() {
        given:
        mockQuestionConverter(defaultCloud20Service)
        allowAccess()

        when:
        def response = defaultCloud20Service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion())

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_)
        response.build().status == 204
    }

    def "question update updates question"() {
        given:
        mockQuestionConverter(defaultCloud20Service)
        allowAccess()

        when:
        def response = defaultCloud20Service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion()).build()

        then:
        1 * questionService.updateQuestion(questionId, _)
        response.status == 204
    }

    def "question update handles exceptions"() {
        given:
        mockQuestionConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]

        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        questionService.updateQuestion(sharedRandom, _) >> { throw new BadRequestException() }
        questionService.updateQuestion("1$sharedRandom", _) >> { throw new NotFoundException() }

        when:
        def response1 = defaultCloud20Service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response2 = defaultCloud20Service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response3 = defaultCloud20Service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response4 = defaultCloud20Service.updateQuestion(authToken, "1$sharedRandom", entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401
        response2.build().status == 403
        response3.build().status == 400
        response4.build().status == 404
    }

    def "question(s) get verifies user level access"() {
        given:
        mockQuestionConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.getQuestion(authToken, questionId)
        defaultCloud20Service.getQuestions(authToken)

        then:
        2 * authorizationService.verifyUserLevelAccess(_)
    }

    def "question(s) get gets question and returns it (them)"() {
        given:
        mockQuestionConverter(defaultCloud20Service)
        allowAccess()

        def questionList = [
                entityFactory.createQuestion("1", "question1"),
                entityFactory.createQuestion("2", "question2")
        ].asList()

        when:
        def response1 = defaultCloud20Service.getQuestion(authToken, questionId).build()
        def response2 = defaultCloud20Service.getQuestions(authToken).build()

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
        mockQuestionConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(authToken) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyUserLevelAccess(mock) >> { throw new ForbiddenException() }
        questionService.getQuestion("1$questionId") >> {throw new NotFoundException() }

        def secondMock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken("1$authToken") >>> [ null, secondMock ]
        authorizationService.verifyUserLevelAccess(secondMock) >> { throw new ForbiddenException() }

        when:
        def questionResponse1 = defaultCloud20Service.getQuestion(authToken, questionId).build()
        def questionResponse2 = defaultCloud20Service.getQuestion(authToken, questionId).build()
        def questionResponse3 = defaultCloud20Service.getQuestion(authToken, "1$questionId").build()

        def questionsResponse1 = defaultCloud20Service.getQuestions("1$authToken").build()
        def questionsResponse2 = defaultCloud20Service.getQuestions("1$authToken").build()

        then:
        questionResponse1.status == 401
        questionResponse2.status == 403
        questionResponse3.status == 404

        questionsResponse1.status == 401
        questionsResponse2.status == 403
    }

    def "updateCapabilities verifies identity admin level access"() {
        given:
        mockCapabilityConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(), "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "updateCapabilities updates capability" (){
        given:
        mockCapabilityConverter(defaultCloud20Service)
        allowAccess()

        when:
        def response = defaultCloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest","1").build()

        then:
        response.status == 204
    }

    def "updateCapabilities handles exceptions" (){
        given:
        mockCapabilityConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.updateCapabilities(_, _, _) >> { throw new BadRequestException() }

        when:
        def response1 = defaultCloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()
        def response2 = defaultCloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()
        def response3 = defaultCloud20Service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "capabilites get verifies identity admin level access"() {
        given:
        mockCapabilityConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.getCapabilities(authToken, "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "Capabilities get gets and returns capabilities" () {
        given:
        mockCapabilityConverter(defaultCloud20Service)
        allowAccess()

        jaxbMock.getValue() >> v1Factory.createCapabilities([ v1Factory.createCapability("1", "capability") ].asList())

        when:
        def response = defaultCloud20Service.getCapabilities(authToken,"computeTest","1").build()

        then:
        1 * capabilityService.getCapabilities(_, _)
        def com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities entity = response.getEntity()
        entity.getCapability().get(0).id.equals("1")
        entity.getCapability().get(0).name.equals("capability")
        response.status == 200
    }

    def "capabilities get handles exceptions" () {
        given:
        mockCapabilityConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.getCapabilities(_, _) >> { throw new BadRequestException() }

        when:
        def response1 = defaultCloud20Service.getCapabilities("badToken","computeTest","1").build()
        def response2 = defaultCloud20Service.getCapabilities("badToken","computeTest","1").build()
        def response3 = defaultCloud20Service.getCapabilities("badToken","computeTest","1").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "deleteCapabilities verifies identity admin level access"() {
        given:
        mockCapabilityConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.removeCapabilities(authToken, "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "deleteCapabilities deletes capability" () {
        given:
        mockCapabilityConverter(defaultCloud20Service)
        allowAccess()

        when:
        def response = defaultCloud20Service.removeCapabilities(authToken , "computeTest", "1").build()

        then:
        response.status == 204
        1 * capabilityService.removeCapabilities("computeTest", "1")
    }

    def "deleteCapabilities handles exceptions"() {
        given:
        mockCapabilityConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.removeCapabilities(_, _) >> { throw new BadRequestException() }

        when:
        def response1 = defaultCloud20Service.removeCapabilities(authToken, null, null).build()
        def response2 = defaultCloud20Service.removeCapabilities(authToken, null, null).build()
        def response3 = defaultCloud20Service.removeCapabilities(authToken, null, null).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "addUser verifies user admin access level and validates user"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate())

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
        1 * validator.validate20User(_)
        1 * validator.validatePasswordForCreateOrUpdate(_)
    }

    def "addUser determines if caller is user-admin, admin, or service admin"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate("username", null, "displayName", null, true))

        then:
        2 * authorizationService.authorizeCloudUserAdmin(_)
        2 * authorizationService.authorizeCloudIdentityAdmin(_)
        2 * authorizationService.authorizeCloudServiceAdmin(_)
    }

    def "addUser creates domain"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        authorizationService.authorizeCloudUserAdmin(_) >>> [ false, false ]
        authorizationService.authorizeCloudIdentityAdmin(_) >>> [ true, false ]
        authorizationService.authorizeCloudServiceAdmin(_) >>> [ false, false ]

        when:
        defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate())

        then:
        1 * defaultRegionService.validateDefaultRegion(_)
        1 * domainService.createNewDomain(_)
    }

    def "addUser sets roles"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        authorizationService.authorizeCloudUserAdmin(_) >>> [ false, false ]
        authorizationService.authorizeCloudIdentityAdmin(_) >>> [ true, true ]
        authorizationService.authorizeCloudServiceAdmin(_) >>> [ false, false ]

        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        userService.checkAndGetUserById(_) >> entityFactory.createUser()

        when:
        defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate())

        then:
        1 * tenantService.addTenantRoleToUser(_, _)

    }

    def "addUser handles exceptions"() {
        given:
        def mockConverter = Mock(UserConverterCloudV20)
        def mock = Mock(ScopeAccess)

        defaultCloud20Service.userConverterCloudV20 = mockConverter

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock ] >> Mock(ScopeAccess)
        authorizationService.verifyUserAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        config.getInt("numberOfSubUsers") >>> [ 0 ] >> 50

        userService.getAllUsers(_) >>> [
                entityFactory.createUsers([entityFactory.createUser()].asList())
        ] >> entityFactory.createUsers()

        userService.getUserByAuthToken(_) >>> [
                entityFactory.createUser("username1", "displayName", "id", null, null, null, null, true),
                entityFactory.createUser("username2", "displayName", "id", "one", null, null, null, true),
                entityFactory.createUser("username3", "displayName", "id", "two", null, null, null, true),
                entityFactory.createUser("username4", "displayName", "id", null, null, null, null, true),
        ]

        mockConverter.toUserDO(_) >>> [
                entityFactory.createUser("userDO1", null, null, null, null, null, null, true),
                entityFactory.createUser("userDO2", null, null, null, null, null, null, true),
                entityFactory.createUser("userDO3", null, null, "domain", null, null, null, true),
                entityFactory.createUser("userDO4", null, null, null, null, null, null, true),
                entityFactory.createUser("userDO5", null, null, "domain", null, null, null, true)
        ]

        authorizationService.authorizeCloudUserAdmin(_) >>> [
                true,
                true,
                false,
                false
        ]

        authorizationService.authorizeCloudIdentityAdmin(_) >>> [
                false,
                false,
                false,
                true
        ]

        authorizationService.authorizeCloudServiceAdmin(_) >>> [
                false,
                false,
                true,
                false
        ]

        userService.addUser(_) >> { throw new DuplicateException() }

        when:
        def response1 = defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()
        def response2 = defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()
        def response3 = defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()
        def response4 = defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()
        def response5 = defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()
        def response6 = defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()
        def response7 = defaultCloud20Service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
        response4.status == 400
        response5.status == 400
        response6.status == 400
        response7.status == 409
    }

    def "listUsers gets caller"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.listUsers(headers, uriInfo(), authToken, null, null)

        then:
        1 * userService.getUser(_) >> entityFactory.createUser()
    }

    def "listUsers (defaultUser caller) succeeds"() {
        given:
        mockUserService(defaultCloud20Service)
        allowAccess()

        authorizationService.authorizeCloudUser(_) >> true

        when:
        def response = defaultCloud20Service.listUsers(headers, uriInfo(), authToken, null, null).build()

        then:
        response.status == 200
    }

    def "listUsers (caller is admin or service admin) gets paged users and returns list"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        authorizationService.authorizeCloudUser(_) >> false
        authorizationService.authorizeCloudIdentityAdmin(_) >>> [ true ]
        authorizationService.authorizeCloudServiceAdmin(_) >>> [ false ] >> true

        def userContextMock = Mock(PaginatorContext)
        userContextMock.getValueList() >> [].asList()

        userService.getUser(_) >> entityFactory.createUser()

        userPaginator.createLinkHeader(_, _) >> "link header"

        when:
        def response1 = defaultCloud20Service.listUsers(headers, uriInfo(), authToken, offset, limit).build()
        def response2 = defaultCloud20Service.listUsers(headers, uriInfo(), authToken, offset, limit).build()

        then:
        2 * userService.getAllUsersPaged(_, _, _) >> userContextMock

        response1.status == 200
        response2.status == 200
        response1.getMetadata().get("Link") != null
        response2.getMetadata().get("Link") != null
    }

    def "listUsers (caller is userAdmin) gets paged users with domain filter and returns list"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        authorizationService.authorizeCloudUser(_) >> false
        authorizationService.authorizeCloudIdentityAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        def userContextMock = Mock(PaginatorContext)
        userContextMock.getValueList() >> [].asList()

        userService.getUser(_) >> entityFactory.createUser()

        userPaginator.createLinkHeader(_, _) >> "link header"

        when:
        def response1 = defaultCloud20Service.listUsers(headers, uriInfo(), authToken, offset, limit).build()

        then:
        userService.getAllUsersPaged(_, _, _) >> { arg1, arg2, arg3 ->
            assert(arg1[0] instanceof FilterParam)
            return userContextMock
        }

        response1.status == 200
        response1.getMetadata().get("Link") != null
    }

    def "listUsers handles exceptions"() {
        given:
        mockUserConverter(defaultCloud20Service)

        def mock = Mock(ScopeAccess)
        mock.getLDAPEntry() >> createLdapEntry()
        scopeAccessMock = Mock()
        scopeAccessMock.getLDAPEntry() >> createLdapEntry()

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock ] >> scopeAccessMock
        authorizationService.verifyUserAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        userService.getUser(_) >>> [
                entityFactory.createUser(),
                entityFactory.createUser("username", null, null, null, null, null, null, true)
        ]

        when:
        def response1 = defaultCloud20Service.listUsers(headers, uriInfo(), authToken, null, null).build()
        def response2 = defaultCloud20Service.listUsers(headers, uriInfo(), authToken, null, null).build()
        def response3 = defaultCloud20Service.listUsers(headers, uriInfo(), authToken, null, null).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "listUsersWithRole verifies admin level access"() {
        given:
        mockUserConverter(defaultCloud20Service)
        allowAccess()

        def contextMock = Mock(PaginatorContext)
        contextMock.getValueList() >> [].asList()

        userService.getUsersWithRole(_, _, _, _) >> contextMock
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        defaultCloud20Service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "listUsersWithRole (user admin caller) filters by domain and gets users"() {
        given:
        mockUserConverter(defaultCloud20Service)
        mockUserPaginator(defaultCloud20Service)
        allowAccess()
        def domain = "callerDomain"

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.getUserByScopeAccess(_) >> entityFactory.createUser("caller", null, null, domain, null, null, null, true)
        userPaginator.createLinkHeader(_, _) >> "link header"

        def contextMock = Mock(PaginatorContext)
        contextMock.getValueList() >> [].asList()

        when:
        def response = defaultCloud20Service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        userService.getUsersWithRole(_, _, _, _) >> { arg1, arg2, arg3, arg4 ->
            assert(arg1.size() == 2)
            assert(arg1[0].value.equals(domain))
            return contextMock
        }

        response.getMetadata().get("Link") != null
        response.status == 200
    }

    def "listUsersWithRole (admin or service admin) filters only by role and gets users"() {
        given:
        mockUserConverter(defaultCloud20Service)
        mockUserPaginator(defaultCloud20Service)
        allowAccess()

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole(roleId, null, null, null)
        authorizationService.authorizeCloudUserAdmin(_) >> false
        userPaginator.createLinkHeader(_, _) >> "link header"

        def contextMock = Mock(PaginatorContext)
        contextMock.getValueList() >> [].asList()

        when:
        def response = defaultCloud20Service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        userService.getUsersWithRole(_, _, _, _) >> { arg1, arg2, arg3, arg4 ->
            assert(arg1.size() == 1)
            assert(arg1[0].value.equals(roleId))
            return contextMock
        }

        response.getMetadata().get("Link") != null
        response.status == 200
    }

    def "listUsersWithRole handles exceptions"() {
        given:
        mockUserConverter(defaultCloud20Service)
        mockUserPaginator(defaultCloud20Service)

        def scopeAccessMock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, scopeAccessMock ] >> Mock(ScopeAccess)
        authorizationService.verifyUserAdminLevelAccess(scopeAccessMock) >> { throw new ForbiddenException() }

        applicationService.getClientRoleById(_) >>> [ null ] >> entityFactory.createClientRole()

        authorizationService.authorizeCloudUserAdmin(_) >>> [ true ] >> false

        userService.getUserByScopeAccess(_) >> entityFactory.createUser("caller", null, null, null, null, null, null, true)


        when:
        def response1 = defaultCloud20Service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()
        def response2 = defaultCloud20Service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()
        def response3 = defaultCloud20Service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()
        def response4 = defaultCloud20Service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 404
        response4.status == 400
    }

    def "addUserRole verifies user-admin access and role to add"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
        1 * applicationService.getClientRoleById(roleId) >> entityFactory.createClientRole(roleId, null, null, null)
    }

    def "addUserRole verifies caller precedence over user to modify and role to add"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(roleId, null, null, null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        defaultCloud20Service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        1 * precedenceValidator.verifyCallerRolePrecendenceForAssignment(caller, roleToAdd)
    }

    def "addUserRole checks for existing identity:* role"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(roleId, null, null, null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        defaultCloud20Service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * tenantService.getGlobalRolesForUser(_)

    }

    def "addUserRole verifies user is within caller domain when caller is user-admin"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()
        def caller = Mock(User)
        def roleToAdd = entityFactory.createClientRole(roleId, null, null, null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        defaultCloud20Service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * caller.getDomainId() >> user.getDomainId()
    }

    def "addUserRole adds role"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(roleId, null, null, null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        def response = defaultCloud20Service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        1 * tenantService.addTenantRoleToUser(user, _)
        response.status == 200
    }

    def "addUserRole handles exceptions"() {
        given:
        def mockedScopeAccess = Mock(ScopeAccess)
        def user1 = entityFactory.createUser()
        def user2 = entityFactory.createUser()
        def user3 = entityFactory.createUser("user3", null, null, "domain3", null, null, null, true)
        def caller1 = entityFactory.createUser()
        def caller2 = entityFactory.createUser()
        def caller3 = entityFactory.createUser("caller3", null, null, "domain1", null, null, null, true)
        def roleToAdd = entityFactory.createClientRole(roleId, null, null, null)

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mockedScopeAccess ] >> Mock(ScopeAccess)
        authorizationService.verifyUserAdminLevelAccess(mockedScopeAccess) >> { throw new ForbiddenException() }
        authorizationService.authorizeCloudUserAdmin(_) >> true

        userService.checkAndGetUserById(("1$userId")) >> { throw new NotFoundException()}

        precedenceValidator.verifyCallerPrecedenceOverUser(caller1, user1) >> { throw new ForbiddenException() }
        precedenceValidator.verifyCallerRolePrecendenceForAssignment(caller2, user2) >> { throw new ForbiddenException() }

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(userId) >>> [ user1, user2, user3 ]
        userService.getUserByAuthToken(authToken) >>> [ caller1, caller2, caller3 ]

        when:
        def response1 = defaultCloud20Service.addUserRole(headers, authToken, userId, roleId).build()
        def response2 = defaultCloud20Service.addUserRole(headers, authToken, userId, roleId).build()
        def response3 = defaultCloud20Service.addUserRole(headers, authToken, "1$userId", roleId).build()
        def response4 = defaultCloud20Service.addUserRole(headers, authToken, userId, roleId).build()
        def response5 = defaultCloud20Service.addUserRole(headers, authToken, userId, roleId).build()
        def response6 = defaultCloud20Service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 404
        response4.status == 403
        response5.status == 403
        response6.status == 403
    }

    def "deleteUserRole verifies userAdmin level access"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "deleteUserRole verifies user to modify is within callers domain when caller is user-admin"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()
        def caller = Mock(User)

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * caller.getDomainId() >> user.getDomainId()
    }

    def "deleteUserRole gets users globalRoles"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> false

        when:
        defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * tenantService.getGlobalRolesForUser(user) >> [ entityFactory.createTenantRole(roleId, null, "name", null, null) ].asList()

    }

    def "deleteUserRole prevents a user from deleting their own identity:* role"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> user
        authorizationService.authorizeCloudUserAdmin(_) >> false

        tenantService.getGlobalRolesForUser(_) >> [ entityFactory.createTenantRole(roleId, null, "identity:serviceAdmin", null, null) ].asList()

        when:
        def response = defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        response.status == 403
    }

    def "deleteUserRole verifies callers precedence over user and role to be deleted"() {
        given:
        allowAccess()

        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        userService.getUserByAuthToken(_) >> entityFactory.createUser()

        tenantService.getGlobalRolesForUser(_) >> [ entityFactory.createTenantRole(roleId, "userRsId", "name", null, null) ].asList()

        when:
        defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_, _)
        1 * precedenceValidator.verifyCallerRolePrecedence(_, _)
    }

    def "deleteUserRole deletes role"() {
        given:
        allowAccess()

        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        userService.getUserByAuthToken(_) >> entityFactory.createUser()

        tenantService.getGlobalRolesForUser(_) >> [ entityFactory.createTenantRole(roleId, userId, null, null, null) ].asList()

        when:
        def response = defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        1 * tenantService.deleteGlobalRole(_)
        response.status == 204
    }

    def "deleteUserRole handles exceptions"() {
        given:
        def mockedScopeAccess = Mock(ScopeAccess)
        def user1 = entityFactory.createUser()
        def user2 = entityFactory.createUser("user2", null, null, "domain2", null, null, null, true)
        def user3 = entityFactory.createUser()
        def caller1 = entityFactory.createUser()
        def caller2 = entityFactory.createUser("caller2", null, null, "domain", null, null, null, true)
        def caller3 = user3

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mockedScopeAccess ] >> Mock(ScopeAccess)
        authorizationService.verifyUserAdminLevelAccess(mockedScopeAccess) >> { throw new ForbiddenException() }
        userService.checkAndGetUserById("1$userId") >> { throw new NotFoundException() }

        userService.checkAndGetUserById(_) >>> [ user2, user1, user3 ]
        userService.getUserByAuthToken(_) >>> [ caller2, caller1, caller3 ]

        authorizationService.authorizeCloudUserAdmin(_) >> [ true ] >> false

        tenantService.getGlobalRolesForUser(_) >>> [
                [].asList(),
                [ entityFactory.createTenantRole(roleId, userId, "identity:role", null, null) ].asList()
        ]

        when:
        def response1 = defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response2 = defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response3 = defaultCloud20Service.deleteUserRole(headers, authToken, "1$userId", roleId).build()
        def response4 = defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response5 = defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response6 = defaultCloud20Service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 404
        response4.status == 403
        response5.status == 404
        response6.status == 403
    }


    def "addRole verifies admin access"() {
        given:
        mockRoleConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.addRole(headers, uriInfo(), authToken, v2Factory.createRole())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "addRole sets serviceId"() {
        given:
        mockRoleConverter(defaultCloud20Service)
        allowAccess()

        def roleMock = Mock(Role)
        roleMock.getServiceId() == ''
        roleMock.getName() >> "name"

        when:
        defaultCloud20Service.addRole(headers, uriInfo(), authToken, roleMock)

        then:
        1 * roleMock.setServiceId(_)
    }

    def "addRole verifies service admin access when adding identity:* roles"() {
        given:
        mockRoleConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", "serviceId", null))

        then:
        1 * authorizationService.verifyServiceAdminLevelAccess(_)
    }

    def "addRole gets service"() {
        given:
        mockRoleConverter(defaultCloud20Service)
        allowAccess()

        when:
        defaultCloud20Service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", "serviceId", null))

        then:
        1 * applicationService.checkAndGetApplication(_)
    }

    def "addRole adds role and returns location"() {
        given:
        mockRoleConverter(defaultCloud20Service)
        allowAccess()

        applicationService.checkAndGetApplication(_) >> entityFactory.createApplication()

        when:
        def response = defaultCloud20Service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", null, null)).build()

        then:
        1 * applicationService.addClientRole(_)
        response.status == 201
        response.getMetadata().get("location") != null
    }

    def "addRole handles exceptions"() {
        given:
        def scopeAccessMock = Mock(ScopeAccess)
        def role = v2Factory.createRole("role", "service", null)
        def namelessRole = v2Factory.createRole(null, null, null)
        def identityRole = v2Factory.createRole("identity:role", null, null)
        def roleWithService = v2Factory.createRole("role", "serviceId", null)

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, scopeAccessMock ] >> Mock(ScopeAccess)
        authorizationService.verifyIdentityAdminLevelAccess(scopeAccessMock) >> { throw new ForbiddenException() }
        authorizationService.verifyServiceAdminLevelAccess(_) >> { throw new ForbiddenException() }
        applicationService.checkAndGetApplication("serviceId") >> { throw new NotFoundException() }
        applicationService.checkAndGetApplication("service") >> entityFactory.createApplication()
        applicationService.addClientRole(_) >> { throw new DuplicateException() }

        when:
        def response1 = defaultCloud20Service.addRole(headers, uriInfo(), authToken, role).build()
        def response2 = defaultCloud20Service.addRole(headers, uriInfo(), authToken, role).build()
        def response3 = defaultCloud20Service.addRole(headers, uriInfo(), authToken, null).build()
        def response4 = defaultCloud20Service.addRole(headers, uriInfo(), authToken, namelessRole).build()
        def response5 = defaultCloud20Service.addRole(headers, uriInfo(), authToken, identityRole).build()
        def response6 = defaultCloud20Service.addRole(headers, uriInfo(), authToken, roleWithService).build()
        def response7 = defaultCloud20Service.addRole(headers, uriInfo(), authToken, role).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
        response4.status == 400
        response5.status == 403
        response6.status == 404
        response7.status == 409
    }

    def "deleteRole verifies admin level access"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.deleteRole(headers, authToken, roleId)

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "deleteRole verifies callers precedence over role to be deleted"() {
        given:
        allowAccess()

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        defaultCloud20Service.deleteRole(headers, authToken, roleId)

        then:
        1 * userService.getUserByAuthToken(_) >> entityFactory.createUser()
        1 * precedenceValidator.verifyCallerRolePrecedence(_, _)
    }

    def "deleteRole deletes role"() {
        given:
        allowAccess()

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = defaultCloud20Service.deleteRole(headers, authToken, roleId).build()

        then:
        1 * applicationService.deleteClientRole(_)
        response.status == 204
    }

    def "deleteRole handles exceptions"() {
        given:
        def scopeAccessMock = Mock(ScopeAccess)

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, scopeAccessMock ] >> Mock(ScopeAccess)
        authorizationService.verifyIdentityAdminLevelAccess(scopeAccessMock) >> { throw new ForbiddenException() }

        applicationService.getClientRoleById("unique") >> entityFactory.createClientRole("unique", "identity:role", null, null)

        when:
        def response1 = defaultCloud20Service.deleteRole(headers, authToken, roleId).build()
        def response2 = defaultCloud20Service.deleteRole(headers, authToken, roleId).build()
        def response3 = defaultCloud20Service.deleteRole(headers, authToken, null).build()
        def response4 = defaultCloud20Service.deleteRole(headers, authToken, "unique").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
        response4.status == 403
    }

    def "addRolesToUserOnTenant verifies user-admin level access"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "addRolesToUserOnTenant verifies tenant access"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * authorizationService.verifyTokenHasTenantAccess(_, _)
    }

    def "addRolesToUserOnTenant verifies tenant"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * tenantService.checkAndGetTenant(_)
    }

    def "addRolesToUserOnTenant verifies user"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * userService.checkAndGetUserById(_)
        1 * userService.getUserByAuthToken(_)
    }

    def "addRolesToUserOnTenant verifies that user to modify is within callers domain when caller is user-admin"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()
        def caller = Mock(User)

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        def response = defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * caller.getDomainId() >> entityFactory.createUser("username", null, null, "unique", null, null, null, true)
        response.status == 403
    }

    def "addRolesToUserOnTenant verifies role"() {
        given:
        allowAccess()

        when:
        def response = defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * applicationService.getClientRoleById(_) >> entityFactory.createClientRole(null, "identity:role", null, null)
        response.status == 403
    }

    def "addRolesToUserOnTenant verifies callers precedence"() {
        given:
        allowAccess()

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenatn1", "user1", "role1").build()

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_, _)
        1 * precedenceValidator.verifyCallerRolePrecendenceForAssignment(_, _)
    }

    def "addRolesToUserOnTenant adds role to user on tenant"() {
        given:
        allowAccess()

        def user = entityFactory.createUser()

        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> entityFactory.createUser()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = defaultCloud20Service.addRolesToUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * tenantService.addTenantRoleToUser(user, _)
        response.status == 200
    }

    def "deleteRoleFromUserOnTenant verifies user admin level access"() {
        given:
        allowAccess()

        when:
        def response1 = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()
        def response2 = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, "1$authToken", "tenant1", "user1", "role1").build()

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authToken) >> { throw new NotAuthorizedException() }
        response1.status == 401

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_) >> { throw new ForbiddenException() }
        response2.status == 403
    }

    def "deleteRoleFromUserOnTenant verifies tenant access"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1")

        then:
        1 * authorizationService.verifyTokenHasTenantAccess(_, _)
    }

    def "deleteRoleFromUserOnTenant verifies user"() {
        given:
        allowAccess()

        when:
        def response = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * userService.checkAndGetUserById("user1") >> { throw new NotFoundException() }
        response.status == 404
    }

    def "deleteRoleFromUserOnTenant verifies tenant"() {
        given:
        allowAccess()

        when:
        def response = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * tenantService.checkAndGetTenant(_) >> { throw new NotFoundException() }
        response.status == 404
    }

    def "deleteRoleFromUserOnTenant verifies user belongs to callers domain when caller is user admin"() {
        given:
        allowAccess()

        def caller = Mock(User)
        def user = entityFactory.createUser()

        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        def response = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * caller.getDomainId() >> "1$user.domainId"
        response.status == 403
    }

    def "deleteRoleFromUserOnTenant verifies role"() {
        given:
        allowAccess()

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        def response = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * applicationService.getClientRoleById(_) >> { throw new NotFoundException() }
        response.status == 404
    }

    def "deleteRoleFromUserOnTenant verifies caller precedence"() {
        given:
        allowAccess()

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        1 * precedenceValidator.verifyCallerRolePrecendenceForAssignment(caller, _)
    }

    def "deleteRoleFromUserOnTenant deletes role from user"() {
        given:
        allowAccess()

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = defaultCloud20Service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * tenantService.deleteTenantRoleForUser(user, _)
        response.status == 204
    }

    def "getSecretQA returns 200" () {
        given:
        mockSecretQAConverter(defaultCloud20Service)
        allowAccess()

        def user = entityFactory.createUser()
        user.id = "id"

        userService.getUser(_) >> user
        userService.checkAndGetUserById(_) >> user

        when:
        def response = defaultCloud20Service.getSecretQAs(authToken,"id").build()

        then:
        1 * secretQAService.getSecretQAs(_) >> entityFactory.createSecretQAs()
        response.status == 200
    }

    def "createSecretQA returns 200" () {
        given:
        mockSecretQAConverter(defaultCloud20Service)
        allowAccess()

        def user = entityFactory.createUser()
        user.id = "id"

        userService.getUser(_) >> user
        userService.checkAndGetUserById(_) >> user

        when:
        def response = defaultCloud20Service.createSecretQA(authToken,"1", v1Factory.createSecretQA()).build()

        then:
        1 * secretQAService.addSecretQA("1", _)
        response.status == 200
    }


    def "Impersonate a disabled user" () {
        given:
        mockAuthConverter(defaultCloud20Service)

        def v20user = v2Factory.createUser()
        v20user.username = "impersonatingUsername"
        def impRequest = v1Factory.createImpersonationRequest(v20user)
        def entityUser = entityFactory.createUser("impersonatedUser", null, null, null, null, null, null, false)
        def userScopeAccess = createUserScopeAccess("tokenString", "userRsId", "clientId", new Date())

        scopeAccessMock = Mock()
        scopeAccessMock.getLDAPEntry() >> createLdapEntry()
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [
                scopeAccessMock,
                userScopeAccess
        ]

        userService.getUser(_) >> entityUser
        tenantService.getGlobalRolesForUser(entityUser) >> [
                entityFactory.createTenantRole(null, null, "identity:default", null, null)
        ].asList()

        scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(_, _) >> userScopeAccess

        when:
        def responseBuilder = defaultCloud20Service.impersonate(headers, authToken, impRequest)

        then:
        1 * scopeAccessService.updateExpiredUserScopeAccess(userScopeAccess, true) >> userScopeAccess
        responseBuilder.build().status == 200
    }

    def "setDomainId throws bad request"() {
        given:
        authorizationService.authorizeCloudUserAdmin(_) >> true
        scopeAccessMock = Mock()
        scopeAccessMock.getLDAPEntry() >> createLdapEntry()

        def mockedUser = Mock(User)
        mockedUser.getDomainId() >> null

        userService.getUser(_) >> mockedUser

        when:
        defaultCloud20Service.setDomainId(scopeAccessMock, Mock(User))

        then:
        thrown(BadRequestException)
    }

    def "authenticateFederatedDomain handles authentication with password credentials"() {
        given:
        mockDomainConverter(defaultCloud20Service)
        mockAuthConverter(defaultCloud20Service)
        def authRequest = createAuthenticationRequest(false)
        def domain = v1Factory.createDomain("id", "RACKSPACE", null, true)
        def racker = entityFactory.createRacker()

        scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> createRackerScopeAcccss()

        when:
        defaultCloud20Service.authenticateFederatedDomain(headers, authRequest, domain)

        then:
        1 * validator20.validatePasswordCredentials(_)
        1 * domainConverter.toDomainDO(domain)
        1 * authenticationService.authenticateDomainUsernamePassword(_, _, _) >> entityFactory.createUserAuthenticationResult(racker, true)
    }

    def "authenticateFederatedDomain handles authentication with RSA credentials"() {
        given:
        mockDomainConverter(defaultCloud20Service)
        mockAuthConverter(defaultCloud20Service)
        def authRequest = createAuthenticationRequest(true)
        def domain = v1Factory.createDomain("id", "RACKSPACE", null, true)
        def racker = entityFactory.createRacker()

        scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> createRackerScopeAcccss()

        when:
        defaultCloud20Service.authenticateFederatedDomain(headers, authRequest, domain)

        then:
        1 * validator20.validateUsername(_)
        1 * domainConverter.toDomainDO(domain)
        1 * authenticationService.authenticateDomainRSA(_, _, _) >> entityFactory.createUserAuthenticationResult(racker, true)
    }

    def "getUserCredential verifies access token"() {
        when:
        defaultCloud20Service.getUserCredential(headers, authToken, "userId", "credentialType")

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(_)
    }

    def "getUserCredential verifies user level access"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.getUserCredential(headers, authToken, "userId", "credentialType")

        then:
        1 * authorizationService.verifyUserLevelAccess(_)
    }

    def "getUserCredential verifies credential type"() {
        given:
        allowAccess()

        when:
        def result = defaultCloud20Service.getUserCredential(headers, authToken, "userId", "invalidCredentialType")

        then:
        result.build().status == 400
    }

    def "getUserCredential gets user"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.PASSWORD_CREDENTIALS)

        then:
        1 * userService.getUserById("userId")
    }

    def "getUserCredential gets caller and checks if caller is user-admin or default user"() {
        given:
        allowAccess()

        userService.getUserById(_) >> entityFactory.createUser()

        when:
        defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.APIKEY_CREDENTIALS)

        then:
        1 * authorizationService.authorizeCloudUser(_) >> false
        1 * authorizationService.authorizeCloudUserAdmin(_) >> false
        1 * userService.getUser(_) >> entityFactory.createUser()
    }

    def "getUserCredential verifies user is in callers domain when caller is user-admin" () {
        given:
        allowAccess()

        def user = Mock(User)
        def caller = Mock(User)

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.APIKEY_CREDENTIALS)

        then:
        1 * authorizationService.verifyDomain(caller, user)
    }

    def "getUserCredential verifies caller is user when caller is default user"() {
        given:
        allowAccess()

        def user = Mock(User)
        def caller = Mock(User)
        caller.getId() >> "unique"

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller
        authorizationService.authorizeCloudUser(_) >> true

        when:
        def result = defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.PASSWORD_CREDENTIALS)

        then:
        result.build().status == 403
    }

    def "getUserCredential verifies user with id exists"() {
        given:
        allowAccess()

        authorizationService.authorizeCloudUser(_) >> false
        authorizationService.authorizeCloudUserAdmin(_) >> false

        when:
        def result = defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.APIKEY_CREDENTIALS)

        then:
        result.build().status == 404
    }

    def "getUserCredential gets user password credentials"() {
        given:
        allowAccess()

        def user = Mock(User)
        user.getPassword() >>> [ null, "Password1" ]
        user.getUsername() >> "username"
        def caller = Mock(User)

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller

        when:
        def response1 = defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.PASSWORD_CREDENTIALS)
        def response2 = defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.PASSWORD_CREDENTIALS)

        then:
        response1.build().status == 404
        response2.build().status == 200
    }

    def "getUserCredential gets user apikey credentials"() {
        given:
        allowAccess()

        def user = Mock(User)
        user.getApiKey() >>> [ null, "apiKey" ]
        user.getUsername() >> "username"
        def caller = Mock(User)

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller

        when:
        def response1 = defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.APIKEY_CREDENTIALS)
        def response2 = defaultCloud20Service.getUserCredential(headers, authToken, "userId", JSONConstants.APIKEY_CREDENTIALS)

        then:
        response1.build().status == 404
        response2.build().status == 200
    }

    def "addDomain verifies access level"() {
        given:
        allowAccess()
        mockDomainConverter(defaultCloud20Service)

        when:
        defaultCloud20Service.addDomain(authToken, uriInfo(), v1Factory.createDomain())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "addDomain verifies domain has name"() {
        given:
        allowAccess()
        def domain = v1Factory.createDomain("id", null, "description", true)

        when:
        def response = defaultCloud20Service.addDomain(authToken, uriInfo(), domain)

        then:
        response.build().status == 400
    }

    def "addDomain adds domain with duplicate exception and success"() {
        given:
        allowAccess()
        domainConverter = Mock(DomainConverterCloudV20)
        defaultCloud20Service.domainConverterCloudV20 = domainConverter

        def domain1 = Mock(Domain)
        def domain2 = Mock(Domain)

        domainConverter.toDomainDO(_) >>> [
                domain1,
                domain2
        ]

        when:
        def response1 = defaultCloud20Service.addDomain(authToken, uriInfo(), v1Factory.createDomain())
        def response2 = defaultCloud20Service.addDomain(authToken, uriInfo(), v1Factory.createDomain())

        then:
        1 * domainService.addDomain(domain1) >> { throw new DuplicateException() }
        1 * domainService.addDomain(domain2)

        response1.build().status == 409
        response2.build().status == 201
    }

    def "getEndpointsByDomainId verifies admin access"() {
        given:
        allowAccess()
        mockEndpointConverter(defaultCloud20Service)

        domainService.checkAndGetDomain(_) >> entityFactory.createDomain()

        when:
        defaultCloud20Service.getEndpointsByDomainId(authToken, "domainId")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "getEndpointsByDomainId verifies domain"() {
        given:
        allowAccess()
        mockEndpointConverter(defaultCloud20Service)

        when:
        defaultCloud20Service.getEndpointsByDomainId(authToken, "domainId")

        then:
        1 * domainService.checkAndGetDomain(_) >> entityFactory.createDomain()
    }

    def "getEndpointsByDomainId gets endpoints for domainId"() {
        given:
        allowAccess()
        mockEndpointConverter(defaultCloud20Service)

        domainService.checkAndGetDomain(_) >> entityFactory.createDomain()

        when:
        def response = defaultCloud20Service.getEndpointsByDomainId(authToken, "domainId")

        then:
        1 * tenantService.getTenantsFromNameList(_)
        1 * endpointService.getEndpointsFromTenantList(_)
        1 * endpointConverter.toEndpointList(_)

        response.build().status == 200
    }

    def "removeTenantFromDomain verifies access level"() {
        given:
        allowAccess()

        when:
        defaultCloud20Service.removeTenantFromDomain(authToken, "domainId", "tenantId")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "removeTenantFromDomain removes tenant from domain"() {
        given:
        allowAccess()

        when:
        def response = defaultCloud20Service.removeTenantFromDomain(authToken, "domainId", "tenantId")

        then:
        1 * domainService.removeTenantFromDomain(_, _)

        response.build().status == 204
    }

    def "getServiceApis verifies access level"() {
        given:
        allowAccess()
        mockCapabilityConverter(defaultCloud20Service)

        when:
        defaultCloud20Service.getServiceApis(authToken)

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "getServiceApis gets service api's"() {
        given:
        allowAccess()
        mockCapabilityConverter(defaultCloud20Service)

        when:
        def response = defaultCloud20Service.getServiceApis(authToken)

        then:
        1 * capabilityService.getServiceApis()
        response.build().status == 200
    }

    def "getPolicies verifies access level"() {
        given:
        allowAccess()
        mockPoliciesConverter(defaultCloud20Service)

        when:
        defaultCloud20Service.getPolicies(authToken)

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "getPolicies gets policies"() {
        given:
        allowAccess()
        mockPoliciesConverter(defaultCloud20Service)

        when:
        def response = defaultCloud20Service.getPolicies(authToken)

        then:
        1 * policyService.getPolicies()

        response.build().status == 200
    }

    def "add policy verifies access level"() {
        given:
        allowAccess()
        mockPolicyConverter(defaultCloud20Service)

        when:
        defaultCloud20Service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "add policy validates policy name"() {
        given:
        allowAccess()
        mockPolicyConverter(defaultCloud20Service)

        when:
        def response = defaultCloud20Service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())

        then:
        1 * policyValidator.validatePolicyName(_) >> { throw new BadRequestException() }
        response.build().status == 400
    }

    def "add policy adds policy with duplicate exception and success"() {
        given:
        allowAccess()

        def policy1 = Mock(Policy)
        def policy2 = Mock(Policy)

        policyConverter = Mock(PolicyConverterCloudV20)
        policyConverter.toPolicyDO(_) >>> [
                policy1,
                policy2
        ]

        defaultCloud20Service.policyConverterCloudV20 = policyConverter

        when:
        def response1 = defaultCloud20Service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())
        def response2 = defaultCloud20Service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())

        then:
        1 * policyService.addPolicy(policy1) >> { throw new DuplicateException() }
        1 * policyService.addPolicy(policy2)

        response1.build().status == 409
        response2.build().status == 201
    }

    def "updatePolicy verifies access level"() {
        given:
        allowAccess()
        mockPolicyConverter(defaultCloud20Service)

        when:
        defaultCloud20Service.updatePolicy(authToken,"policyId", v1Factory.createPolicy())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "updatePolicy verifies policy"() {
        given:
        allowAccess()
        mockPolicyConverter(defaultCloud20Service)

        when:
        def response = defaultCloud20Service.updatePolicy(authToken, "policyId", v1Factory.createPolicy())

        then:
        1 * policyService.checkAndGetPolicy("policyId") >> { throw new NotFoundException() }
        response.build().status == 404
    }

    def "updatePolicy verifies policy name"() {
        given:
        allowAccess()
        mockPolicyConverter(defaultCloud20Service)

        when:
        defaultCloud20Service.updatePolicy(authToken, "policyId", v1Factory.createPolicy())

        then:
        1 * policyValidator.validatePolicyName(_)
    }

    def "updatePolicy updates policy"() {
        given:
        allowAccess()
        mockPolicyConverter(defaultCloud20Service)

        when:
        def response = defaultCloud20Service.updatePolicy(authToken, "policyId", v1Factory.createPolicy())

        then:
        1 * policyService.updatePolicy(_, _)
        response.build().status == 204
    }

    def "checkDomainFromAuthRequest returns domain"() {
        given:
        def domain = v1Factory.createDomain()
        def list = [ domain ].asList()
        def authRequest = v2Factory.createAuthenticationRequest(null, null, null, list)

        when:
        def response = defaultCloud20Service.checkDomainFromAuthRequest(authRequest)

        then:
        response.description.equals(domain.getDescription())
        response.id.equals(domain.getId())
        response.name.equals(domain.getName())

    }

    def "getting identity role names retrieves identity role names from config"() {
        when:
        defaultCloud20Service.getIdentityRoleNames()

        then:
        1 * config.getString("cloudAuth.userRole")
        1 * config.getString("cloudAuth.userAdminRole")
        1 * config.getString("cloudAuth.adminRole")
        1 * config.getString("cloudAuth.serviceAdminRole")
    }

    def mockServices() {
        mockAuthenticationService(defaultCloud20Service)
        mockAuthorizationService(defaultCloud20Service)
        mockApplicationService(defaultCloud20Service)
        mockScopeAccessService(defaultCloud20Service)
        mockTenantService(defaultCloud20Service)
        mockGroupService(defaultCloud20Service)
        mockUserService(defaultCloud20Service)
        mockDelegateCloud20Service(defaultCloud20Service)
        mockDefaultRegionService(defaultCloud20Service)
        mockDomainService(defaultCloud20Service)
        mockPolicyService(defaultCloud20Service)
        mockCapabilityService(defaultCloud20Service)
        mockCloudRegionService(defaultCloud20Service)
        mockQuestionService(defaultCloud20Service)
        mockSecretQAService(defaultCloud20Service)
        mockEndpointService(defaultCloud20Service)
    }

    def mockMisc() {
        mockAtomHopperClient(defaultCloud20Service)
        mockConfiguration(defaultCloud20Service)
        mockCloudGroupBuilder(defaultCloud20Service)
        mockCloudKsGroupBuilder(defaultCloud20Service)
        mockValidator(defaultCloud20Service)
        mockValidator20(defaultCloud20Service)
        mockPolicyValidator(defaultCloud20Service)
        mockPrecedenceValidator(defaultCloud20Service)
        mockUserPaginator(defaultCloud20Service)
    }

    def allowAccess() {
        scopeAccessMock = Mock()
        scopeAccessMock.getLDAPEntry() >> createLdapEntry()
        scopeAccessService.getScopeAccessByAccessToken(_) >> scopeAccessMock
    }

    def createLdapEntry() {
        return createLdapEntry(null)
    }

    def createLdapEntry(String dn) {
        dn = dn ? dn : "accessToken=token,cn=TOKENS,rsId=id,ou=users"

        def mock = Mock(ReadOnlyEntry)
        mock.getDN() >> dn
        mock.getAttributeValue(_) >> { arg ->
            return arg[0]
        }
        return mock
    }

    def createFilter(FilterParam.FilterParamName name, String value) {
        return new FilterParam(name, value)
    }

    def createAuthenticationRequest(boolean useRsaCreds) {
        def mockedElement = Mock(JAXBElement)
        if (useRsaCreds) {
            mockedElement.getValue() >> v1Factory.createRsaCredentials()
        } else {
            mockedElement.getValue() >> v2Factory.createPasswordCredentialsRequiredUsername()
        }
        new AuthenticationRequest().with {
            it.credential = mockedElement
            return it
        }
    }
}
