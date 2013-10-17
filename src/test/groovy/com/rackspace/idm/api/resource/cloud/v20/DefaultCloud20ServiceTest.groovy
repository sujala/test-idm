package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.api.common.fault.v1.ItemNotFoundFault
import com.rackspace.idm.GlobalConstants
<<<<<<< HEAD
import com.rackspace.idm.JSONConstants
=======
import com.rackspace.idm.api.converter.cloudv20.AuthConverterCloudV20
>>>>>>> B-58104 Identity Federation Accept SAML Response and Generate Token
import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.PolicyConverterCloudV20
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.exception.*
import com.unboundid.ldap.sdk.ReadOnlyEntry
import org.dozer.DozerBeanMapper
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.UserList
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response
import javax.xml.bind.JAXBElement

class DefaultCloud20ServiceTest extends RootServiceTest {

    @Shared JAXBObjectFactories objFactories
    @Shared DefaultCloud20Service service

    @Shared ScopeAccess scopeAccessMock

    @Shared def offset = 0
    @Shared def limit = 25
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
        service = new DefaultCloud20Service()

        exceptionHandler = new ExceptionHandler()
        objFactories = new JAXBObjectFactories()

        exceptionHandler.objFactories = objFactories

        service.exceptionHandler = exceptionHandler
        service.objFactories = objFactories
    }

    def setup() {
        mockServices()
        mockMisc()

        def weights = [ "0", "100", "500", "1000", "2000" ].asList()
        config.getList("cloudAuth.allowedRoleWeights") >> weights

        headers = Mock()
        jaxbMock = Mock(JAXBElement)
    }

    def "question create verifies Identity admin level access and adds Question"() {
        given:
        mockQuestionConverter(service)
        allowUserAccess()

        when:
        def response = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion()).build()

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
        1 * questionService.addQuestion(_) >> "questionId"
        response.getStatus() == 201
        response.getMetadata().get("location")[0] != null
    }

    def "question create handles exceptions"() {
        given:
        mockQuestionConverter(service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]

        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        questionService.addQuestion(_) >> { throw new BadRequestException() }

        when:
        def response1 = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())
        def response2 = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())
        def response3 = service.addQuestion(uriInfo(), authToken, entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401
        response2.build().status == 403
        response3.build().status == 400
    }

    def "question delete verifies Identity admin level access and deletes question"() {
        given:
        mockQuestionConverter(service)
        allowUserAccess()

        when:
        def response = service.deleteQuestion(authToken, questionId).build()

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
        1 * questionService.deleteQuestion(questionId)

        response.getStatus() == 204
    }

    def "question delete handles exceptions"() {
        given:
        mockQuestionConverter(service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        questionService.deleteQuestion(questionId) >> { throw new NotFoundException() }

        when:
        def response1 = service.deleteQuestion(authToken, questionId).build()
        def response2 = service.deleteQuestion(authToken, questionId).build()
        def response3 = service.deleteQuestion(authToken, questionId).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 404

    }

    def "question update verifies Identity admin level access"() {
        given:
        mockQuestionConverter(service)
        allowUserAccess()

        when:
        def response = service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion())

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_)
        response.build().status == 204
    }

    def "question update updates question"() {
        given:
        mockQuestionConverter(service)
        allowUserAccess()

        when:
        def response = service.updateQuestion(authToken, questionId, entityFactory.createJAXBQuestion()).build()

        then:
        1 * questionService.updateQuestion(questionId, _)
        response.status == 204
    }

    def "question update handles exceptions"() {
        given:
        mockQuestionConverter(service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]

        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        questionService.updateQuestion(sharedRandom, _) >> { throw new BadRequestException() }
        questionService.updateQuestion("1$sharedRandom", _) >> { throw new NotFoundException() }

        when:
        def response1 = service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response2 = service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response3 = service.updateQuestion(authToken, sharedRandom, entityFactory.createJAXBQuestion())
        def response4 = service.updateQuestion(authToken, "1$sharedRandom", entityFactory.createJAXBQuestion())

        then:
        response1.build().status == 401
        response2.build().status == 403
        response3.build().status == 400
        response4.build().status == 404
    }

    def "question(s) get verifies user level access"() {
        given:
        mockQuestionConverter(service)
        allowUserAccess()

        when:
        service.getQuestion(authToken, questionId)
        service.getQuestions(authToken)

        then:
        2 * authorizationService.verifyUserLevelAccess(_)
    }

    def "question(s) get gets question and returns it (them)"() {
        given:
        mockQuestionConverter(service)
        allowUserAccess()

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

    def "question(s) get handles exceptions"() {
        given:
        mockQuestionConverter(service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(authToken) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyUserLevelAccess(mock) >> { throw new ForbiddenException() }
        questionService.getQuestion("1$questionId") >> {throw new NotFoundException() }

        def secondMock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken("1$authToken") >>> [ null, secondMock ]
        authorizationService.verifyUserLevelAccess(secondMock) >> { throw new ForbiddenException() }

        when:
        def questionResponse1 = service.getQuestion(authToken, questionId).build()
        def questionResponse2 = service.getQuestion(authToken, questionId).build()
        def questionResponse3 = service.getQuestion(authToken, "1$questionId").build()

        def questionsResponse1 = service.getQuestions("1$authToken").build()
        def questionsResponse2 = service.getQuestions("1$authToken").build()

        then:
        questionResponse1.status == 401
        questionResponse2.status == 403
        questionResponse3.status == 404

        questionsResponse1.status == 401
        questionsResponse2.status == 403
    }

    def "updateCapabilities verifies identity admin level access"() {
        given:
        mockCapabilityConverter(service)
        allowUserAccess()

        when:
        service.updateCapabilities(authToken, v1Factory.createCapabilities(), "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "updateCapabilities updates capability"() {
        given:
        mockCapabilityConverter(service)
        allowUserAccess()

        when:
        def response = service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest","1").build()

        then:
        response.status == 204
    }

    def "updateCapabilities handles exceptions"() {
        given:
        mockCapabilityConverter(service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.updateCapabilities(_, _, _) >> { throw new BadRequestException() }

        when:
        def response1 = service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()
        def response2 = service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()
        def response3 = service.updateCapabilities(authToken, v1Factory.createCapabilities(),"computeTest", "1").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "capabilites get verifies identity admin level access"() {
        given:
        mockCapabilityConverter(service)
        allowUserAccess()

        when:
        service.getCapabilities(authToken, "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "Capabilities get gets and returns capabilities"() {
        given:
        mockCapabilityConverter(service)
        allowUserAccess()

        jaxbMock.getValue() >> v1Factory.createCapabilities([ v1Factory.createCapability("1", "capability") ].asList())

        when:
        def response = service.getCapabilities(authToken,"computeTest","1").build()

        then:
        1 * capabilityService.getCapabilities(_, _)
        def com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities entity = response.getEntity()
        entity.getCapability().get(0).id.equals("1")
        entity.getCapability().get(0).name.equals("capability")
        response.status == 200
    }

    def "capabilities get handles exceptions"() {
        given:
        mockCapabilityConverter(service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.getCapabilities(_, _) >> { throw new BadRequestException() }

        when:
        def response1 = service.getCapabilities("badToken","computeTest","1").build()
        def response2 = service.getCapabilities("badToken","computeTest","1").build()
        def response3 = service.getCapabilities("badToken","computeTest","1").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "deleteCapabilities verifies identity admin level access"() {
        given:
        mockCapabilityConverter(service)
        allowUserAccess()

        when:
        service.removeCapabilities(authToken, "type", "version")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "deleteCapabilities deletes capability"() {
        given:
        mockCapabilityConverter(service)
        allowUserAccess()

        when:
        def response = service.removeCapabilities(authToken , "computeTest", "1").build()

        then:
        response.status == 204
        1 * capabilityService.removeCapabilities("computeTest", "1")
    }

    def "deleteCapabilities handles exceptions"() {
        given:
        mockCapabilityConverter(service)

        def mock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock, Mock(ScopeAccess) ]
        authorizationService.verifyIdentityAdminLevelAccess(mock) >> { throw new ForbiddenException() }
        capabilityService.removeCapabilities(_, _) >> { throw new BadRequestException() }

        when:
        def response1 = service.removeCapabilities(authToken, null, null).build()
        def response2 = service.removeCapabilities(authToken, null, null).build()
        def response3 = service.removeCapabilities(authToken, null, null).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "addUser verifies user admin access level and validates user"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        when:
        def response = service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()

        then:
        1 * authorizationService.verifyUserManagedLevelAccess(_)
        1 * validator.validate20User(_)
        1 * validator.validatePasswordForCreateOrUpdate(_) >> { throw new BadRequestException() }
        response.status == 400
    }

    def "addUser determines if caller is user-admin, admin, or service admin"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        when:
        service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate("username", null, null))

        then:
        2 * authorizationService.authorizeCloudUserAdmin(_)
        2 * authorizationService.authorizeCloudIdentityAdmin(_)
        2 * authorizationService.authorizeCloudServiceAdmin(_)
    }

    def "addUser creates domain"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        authorizationService.authorizeCloudUserAdmin(_) >>> [ false, false ]
        authorizationService.authorizeCloudIdentityAdmin(_) >>> [ true, false ]
        authorizationService.authorizeCloudServiceAdmin(_) >>> [ false, false ]

        when:
        service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate())

        then:
        1 * defaultRegionService.validateDefaultRegion(_)
        1 * domainService.createNewDomain(_)
    }

    def "addUser sets roles"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        authorizationService.authorizeCloudUserAdmin(_) >>> [ false, false ]
        authorizationService.authorizeCloudIdentityAdmin(_) >>> [ true, true ]
        authorizationService.authorizeCloudServiceAdmin(_) >>> [ false, false ]

        applicationService.getClientRoleByClientIdAndRoleName(_, _) >> entityFactory.createClientRole()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        userService.checkAndGetUserById(_) >> entityFactory.createUser()

        when:
        service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate())

        then:
        1 * tenantService.addTenantRoleToUser(_, _)

    }

    def "addUser caller is userAdmin without domain throws BadRequestException"() {
        given:
        allowUserAccess()
        mockUserConverter(service)
        def caller = entityFactory.createUser().with {
            domainId = null
            return it
        }

        userService.getUserByScopeAccess(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        def response = service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()

        then:
        response.status == 400
    }

    def "addUser caller is userAdmin with too many subUsers throws BadRequestException"() {
        given:
        allowUserAccess()
        def converterMock = Mock(UserConverterCloudV20)
        service.userConverterCloudV20 = converterMock

        mockUserConverter(service)
        def caller = entityFactory.createUser()
        def users = [ entityFactory.createUser() ].asList()

        userService.getUserByScopeAccess(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.getUsersWithDomain(_) >> users
        config.getInt("maxNumberOfUsersInDomain") >> 0

        userService.getUserByAuthToken(_) >>> [
                entityFactory.createUser("username1", "id", null, "region"),
                entityFactory.createUser("username2", "id", "one", "region"),
                entityFactory.createUser("username3", "id", "two", "region"),
                entityFactory.createUser("username4", "id", null, "region"),
        ]

        converterMock.fromUser(_) >>> [
                entityFactory.createUser("userDO1", null, null, "region"),
                entityFactory.createUser("userDO2", null, null, "region"),
                entityFactory.createUser("userDO3", null, "domain", "region"),
                entityFactory.createUser("userDO4", null, null, "region"),
                entityFactory.createUser("userDO5", null, "domain", "region")
        ]

        when:
        def response = service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()

        then:
        response.status == 400
    }

    def "addUser caller is user admin and user being added does not have a domain throws BadRequestException"() {
        given:
        allowUserAccess()
        mockUserConverter(service)
        def caller = Mock(User)
        def users = [ entityFactory.createUser() ].asList()

        caller.getDomainId() >>> [ "domainId", "domainId", null]
        userService.getUserByScopeAccess(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.getUsersWithDomain(_) >> users
        config.getInt("maxNumberOfUsersInDomain") >> 5
        domainService.getDomainAdmins(_) >> [].asList()

        when:
        def response = service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()

        then:
        response.status == 400
    }

    def "addUser caller is service admin and created user has domain throws BadRequestException"() {
        given:
        allowUserAccess()
        def converter = Mock(UserConverterCloudV20)
        def mockedUser = Mock(User)
        def caller = entityFactory.createUser()

        mockedUser.getDomainId() >> "domainId"
        converter.fromUser(_) >> mockedUser
        service.userConverterCloudV20 = converter

        userService.getUserByScopeAccess(_) >> caller
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        when:
        def response = service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()

        then:
        response.status == 400
    }

    def "addUser caller is identity admin and created user has no domain throws BadRequestException"() {
        given:
        allowUserAccess()
        def converter = Mock(UserConverterCloudV20)
        def mockedUser = Mock(User)
        def caller = entityFactory.createUser()

        mockedUser.getDomainId() >> ""
        converter.fromUser(_) >> mockedUser
        service.userConverterCloudV20 = converter

        userService.getUserByScopeAccess(_) >> caller
        authorizationService.authorizeCloudIdentityAdmin(_) >> true

        when:
        def response = service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate()).build()

        then:
        response.status == 400
    }

    def "addUser when caller is user-admin adds callers tenantRoles to user being added"() {
        given:
        mockUserConverter(service)

        allowUserAccess()

        def caller = entityFactory.createUser()

        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.getUsersWithDomain(_) >> [].asList()
        domainService.getDomainAdmins(_) >> [].asList()

        when:
        service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate())

        then:
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * tenantService.addCallerTenantRolesToUser(caller, _)
    }

    def "addUser when caller is user-admin adds callers groups to user being added"() {
        given:
        mockUserConverter(service)

        allowUserAccess()

        def caller = entityFactory.createUser()
        def groups = entityFactory.createGroups()
        groups.add(entityFactory.createGroup("1","groupId1","group description"))
        groups.add(entityFactory.createGroup("2","groupId2","group description"))

        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.getUsersWithDomain(_) >> [].asList()
        userService.getGroupsForUser(_) >> groups
        domainService.getDomainAdmins(_) >> [].asList()

        when:
        service.addUser(headers, uriInfo(), authToken, v1Factory.createUserForCreate())

        then:
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * tenantService.addCallerTenantRolesToUser(caller, _)
        2 * userService.addGroupToUser(_, _)
    }

    def "setDomainId sets users domain to callers domain"() {
        given:
        def caller = entityFactory.createUser().with {
            it.domainId = "callersDomain"
            it
        }
        def user = entityFactory.createUser()

        when:
        service.assignUserToCallersDomain(caller, user)

        then:
        user.getDomainId() == caller.getDomainId()
    }

    def "setDomainId throws BadRequestException if caller does not have a domain"() {
        given:
        def caller = entityFactory.createUser().with {
            it.domainId = null
            return it
        }
        def user = entityFactory.createUser()

        when:
        service.assignUserToCallersDomain(caller, user)

        then:
        thrown(BadRequestException)
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
        1 * tenantService.addTenantRoleToUser(user, _)
    }

    def "listUsers gets caller"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        when:
        service.listUsers(headers, uriInfo(), authToken, null, null)

        then:
        1 * userService.getUser(_) >> entityFactory.createUser()
    }

    def "listUsers (defaultUser caller) succeeds"() {
        given:
        mockUserService(service)
        allowUserAccess()

        authorizationService.authorizeCloudUser(_) >> true

        when:
        def response = service.listUsers(headers, uriInfo(), authToken, null, null).build()

        then:
        response.status == 200
    }

    def "listUsers (caller is admin or service admin) gets enabled paged users and returns list"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        authorizationService.authorizeCloudUser(_) >> false
        authorizationService.authorizeCloudIdentityAdmin(_) >>> [ true ]
        authorizationService.authorizeCloudServiceAdmin(_) >>> [ false ] >> true

        def userContextMock = Mock(PaginatorContext)
        userContextMock.getValueList() >> [].asList()

        userService.getUser(_) >> entityFactory.createUser()

        userPaginator.createLinkHeader(_, _) >> "link header"

        when:
        def response1 = service.listUsers(headers, uriInfo(), authToken, offset, limit).build()
        def response2 = service.listUsers(headers, uriInfo(), authToken, offset, limit).build()

        then:
        2 * userService.getAllEnabledUsersPaged(_, _) >> userContextMock

        response1.status == 200
        response2.status == 200
        response1.getMetadata().get("Link") != null
        response2.getMetadata().get("Link") != null
    }

    def "listUsers (caller is userAdmin) gets paged users with domain filter and returns list"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        authorizationService.authorizeCloudUser(_) >> false
        authorizationService.authorizeCloudIdentityAdmin(_) >> false
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        def userContextMock = Mock(PaginatorContext)
        userContextMock.getValueList() >> [].asList()

        userService.getUser(_) >> entityFactory.createUser()

        userPaginator.createLinkHeader(_, _) >> "link header"

        when:
        def response1 = service.listUsers(headers, uriInfo(), authToken, offset, limit).build()

        then:
        userService.getAllUsersPagedWithDomain(_, _, _) >> { arg1, arg2, arg3 ->
            assert(arg1 == "domainId")
            return userContextMock
        }

        response1.status == 200
        response1.getMetadata().get("Link") != null
    }

    def "listUsers handles exceptions"() {
        given:
        mockUserConverter(service)

        def mock = Mock(ScopeAccess)
        mock.getLDAPEntry() >> createLdapEntry()
        scopeAccessMock = Mock()
        scopeAccessMock.getLDAPEntry() >> createLdapEntry()

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mock ] >> scopeAccessMock
        authorizationService.verifyUserAdminLevelAccess(mock) >> { throw new ForbiddenException() }

        userService.getUser(_) >>> [
                entityFactory.createUser(),
                entityFactory.createUser("username", null, null, "region")
        ]

        when:
        def response1 = service.listUsers(headers, uriInfo(), authToken, null, null).build()
        def response2 = service.listUsers(headers, uriInfo(), authToken, null, null).build()
        def response3 = service.listUsers(headers, uriInfo(), authToken, null, null).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
    }

    def "listUsersWithRole verifies admin level access"() {
        given:
        mockUserConverter(service)
        allowUserAccess()

        def contextMock = Mock(PaginatorContext)
        contextMock.getValueList() >> [].asList()

        userService.getUsersWithRole(_, _, _, _) >> contextMock
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit)

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "listUsersWithRole (user admin caller) filters by domain and gets users"() {
        given:
        mockUserConverter(service)
        mockUserPaginator(service)
        allowUserAccess()
        def domain = "callerDomain"

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()
        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.getUserByScopeAccess(_) >> entityFactory.createUser("caller", null, domain, "region")
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
        authorizationService.authorizeCloudUserAdmin(_) >> false
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

        def scopeAccessMock = Mock(ScopeAccess)
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, scopeAccessMock ] >> Mock(ScopeAccess)
        authorizationService.verifyUserAdminLevelAccess(scopeAccessMock) >> { throw new ForbiddenException() }

        applicationService.getClientRoleById(_) >>> [ null ] >> entityFactory.createClientRole()

        authorizationService.authorizeCloudUserAdmin(_) >>> [ true ] >> false

        userService.getUserByScopeAccess(_) >> entityFactory.createUser("caller", null, null, "region")


        when:
        def response1 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()
        def response2 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()
        def response3 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()
        def response4 = service.listUsersWithRole(headers, uriInfo(), authToken, roleId, offset, limit).build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 404
        response4.status == 400
    }

    def "addUserRole verifies user-admin access and role to add"() {
        given:
        allowUserAccess()

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * authorizationService.verifyUserManagedLevelAccess(_)
        1 * applicationService.getClientRoleById(roleId) >> entityFactory.createClientRole(null)
    }

    def "addUserRole verifies caller precedence over user to modify and role to add"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, roleToAdd)
    }

    def "addUserRole checks for existing identity:* role"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "somethingdifferentfromcaller"
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * tenantService.getGlobalRolesForUser(_)

    }

    def "addUserRole verifies user is within caller domain when caller is user-admin"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = Mock(User)
        def roleToAdd = entityFactory.createClientRole(null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        service.addUserRole(headers, authToken, userId, roleId)

        then:
        1 * caller.getDomainId() >> user.getDomainId()
    }

    def "addUserRole adds role"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "somethingdifferentfromcaller"
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(null)

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        def response = service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        1 * tenantService.addTenantRoleToUser(user, _)
        response.status == 200
    }

    def "addUserRole not allowed without scopeAccess"() {
        given:
        1 * scopeAccessService.getScopeAccessByAccessToken(_) >> null

        when:
        def response = service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        response.status == 401
    }

    def "addUserRole not allowed without precedence"() {
        given:
        def mockedScopeAccess = Mock(ScopeAccess)
        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def roleToAdd = entityFactory.createClientRole(null)

        scopeAccessService.getScopeAccessByAccessToken(_) >> mockedScopeAccess

        precedenceValidator.verifyCallerPrecedenceOverUser(caller, user) >> { throw new ForbiddenException() }

        applicationService.getClientRoleById(roleId) >> roleToAdd
        userService.checkAndGetUserById(userId) >> user
        userService.getUserByAuthToken(authToken) >> caller

        when:
        def response = service.addUserRole(headers, authToken, userId, roleId).build()

        then:
        response.status == 403
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
        given:
        allowUserAccess()

        when:
        service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * authorizationService.verifyUserManagedLevelAccess(_)
    }

    def "deleteUserRole verifies user to modify is within callers domain when caller is user-admin"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = Mock(User)

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * caller.getDomainId() >> user.getDomainId()
    }

    def "deleteUserRole gets users globalRoles"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "someotherid"
        def caller = entityFactory.createUser()

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> false

        when:
        service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * tenantService.getGlobalRolesForUser(user) >> [ entityFactory.createTenantRole("name") ].asList()

    }

    def "deleteUserRole prevents a user from deleting their own identity:* role"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole("identity:serviceAdmin").with {
            it.roleRsId = roleId
            return it
        }

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> user
        authorizationService.authorizeCloudUserAdmin(_) >> false

        tenantService.getGlobalRolesForUser(_) >> [ tenantRole ].asList()

        when:
        def response = service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        response.status == 403
    }

    def "deleteUserRole verifies callers precedence over user and role to be deleted"() {
        given:
        allowUserAccess()
        def tenantRole = entityFactory.createTenantRole("name").with {
            it.roleRsId = roleId
            return it
        }

        def user = entityFactory.createUser()
        user.id = "differentid"

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> entityFactory.createUser()

        tenantService.getGlobalRolesForUser(_) >> [ tenantRole ].asList()

        when:
        service.deleteUserRole(headers, authToken, userId, roleId)

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_, _)
        1 * precedenceValidator.verifyCallerRolePrecedence(_, _)
    }

    def "deleteUserRole deletes role"() {
        given:
        allowUserAccess()

        def tenantRole = entityFactory.createTenantRole().with {
            it.roleRsId = roleId
            return it
        }

        def user = entityFactory.createUser()
        user.id = "differentid"

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> entityFactory.createUser()

        tenantService.getGlobalRolesForUser(_) >> [ tenantRole ].asList()

        when:
        def response = service.deleteUserRole(headers, authToken, userId, roleId).build()

        then:
        1 * tenantService.deleteTenantRoleForUser(_, _)
        response.status == 204
    }

    def "deleteUserRole handles exceptions"() {
        given:
        def mockedScopeAccess = Mock(ScopeAccess)
        def user1 = entityFactory.createUser()
        user1.id = "someotherid"
        def user2 = entityFactory.createUser("user2", null, "domain2", "region")
        def user3 = entityFactory.createUser()
        user3.id = "someotherid"
        def caller1 = entityFactory.createUser()
        def caller2 = entityFactory.createUser("caller2", null, "domain", "region")
        def caller3 = user3
        def tenantRole = entityFactory.createTenantRole("identity:role").with {
            it.roleRsId = roleId
            return it
        }

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, mockedScopeAccess ] >> Mock(ScopeAccess)
        authorizationService.verifyUserManagedLevelAccess(mockedScopeAccess) >> { throw new ForbiddenException() }
        userService.checkAndGetUserById("1$userId") >> { throw new NotFoundException() }

        userService.checkAndGetUserById(_) >>> [ user2, user1, user3 ]
        userService.getUserByAuthToken(_) >>> [ caller2, caller1, caller3 ]

        authorizationService.authorizeCloudUserAdmin(_) >> [ true ] >> false

        tenantService.getGlobalRolesForUser(_) >>> [
                [].asList(),
                [ tenantRole ].asList()
        ]

        when:
        def response1 = service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response2 = service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response3 = service.deleteUserRole(headers, authToken, "1$userId", roleId).build()
        def response4 = service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response5 = service.deleteUserRole(headers, authToken, userId, roleId).build()
        def response6 = service.deleteUserRole(headers, authToken, userId, roleId).build()

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
        mockRoleConverter(service)
        allowUserAccess()

        when:
        service.addRole(headers, uriInfo(), authToken, v2Factory.createRole())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
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
        1 * scopeAccessService.getScopeAccessByAccessToken(_) >> sa
        1 * authorizationService.verifyIdentityAdminLevelAccess(_) >> { throw new ForbiddenException() }
    }

    def "addRole sets serviceId"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()

        def roleMock = Mock(Role)
        roleMock.getServiceId() == ''
        roleMock.getName() >> "name"

        when:
        service.addRole(headers, uriInfo(), authToken, roleMock)

        then:
        1 * roleMock.setServiceId(_)
    }

    def "addRole verifies service admin access when adding identity:* roles"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()

        when:
        service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", "serviceId", null))

        then:
        1 * authorizationService.authorizeCloudServiceAdmin(_)
    }

    def "addRole gets service"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        when:
        service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", "serviceId", null))

        then:
        1 * applicationService.checkAndGetApplication(_)
    }

    def "addRole adds role and returns location"() {
        given:
        mockRoleConverter(service)
        allowUserAccess()
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        applicationService.checkAndGetApplication(_) >> entityFactory.createApplication()

        when:
        def response = service.addRole(headers, uriInfo(), authToken, v2Factory.createRole("identity:role", null, null)).build()

        then:
        1 * applicationService.addClientRole(_)
        response.status == 201
        response.getMetadata().get("location") != null
    }

    def "addRole verifies role is not null"() {
        given:
        allowUserAccess()
        def scopeAccessMock = Mock(ScopeAccess)
        def role = v2Factory.createRole("role", "service", null)
        def namelessRole = v2Factory.createRole(null, null, null)
        def identityRole = v2Factory.createRole("identity:role", null, null)
        def roleWithService = v2Factory.createRole("role", "serviceId", null)
        config.getString("cloudAuth.clientId") >> "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        config.getString("idm.clientId") >> "18e7a7032733486cd32f472d7bd58f709ac0d221"
        config.getString("cloudAuth.globalRoles.clientId") >> "6ae999552a0d2dca14d62e2bc8b764d377b1dd6c"

        when:
        def response = service.addRole(headers, uriInfo(), authToken, null).build()

        then:
        response.status == 400
    }

    def "addRole verifies that name is not blank"() {
        given:
        allowUserAccess()
        def role = v2Factory.createRole("", "serviceId", "tenantId")

        when:
        def response = service.addRole(headers, uriInfo(), authToken, role).build()

        then:
        response.status == 400
    }

    def "deleteRole verifies admin level access"() {
        given:
        allowUserAccess()

        when:
        service.deleteRole(headers, authToken, roleId)

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "deleteRole verifies callers precedence over role to be deleted"() {
        given:
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

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = service.deleteRole(headers, authToken, roleId).build()

        then:
        1 * applicationService.deleteClientRole(_)
        response.status == 204
    }

    def "deleteRole handles exceptions"() {
        given:
        def scopeAccessMock = Mock(ScopeAccess)
        def role = entityFactory.createClientRole("identity:role").with {
            it.id = "unique"
            return it
        }

        scopeAccessService.getScopeAccessByAccessToken(_) >>> [ null, scopeAccessMock ] >> Mock(ScopeAccess)
        authorizationService.verifyIdentityAdminLevelAccess(scopeAccessMock) >> { throw new ForbiddenException() }

        applicationService.getClientRoleById("unique") >> role

        when:
        def response1 = service.deleteRole(headers, authToken, roleId).build()
        def response2 = service.deleteRole(headers, authToken, roleId).build()
        def response3 = service.deleteRole(headers, authToken, null).build()
        def response4 = service.deleteRole(headers, authToken, "unique").build()

        then:
        response1.status == 401
        response2.status == 403
        response3.status == 400
        response4.status == 403
    }

    def "addRolesToUserOnTenant verifies user-admin level access"() {
        given:
        allowUserAccess()

        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_)
    }

    def "addRolesToUserOnTenant verifies tenant access"() {
        given:
        allowUserAccess()

        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * authorizationService.verifyTokenHasTenantAccess(_, _)
    }

    def "addRolesToUserOnTenant verifies tenant"() {
        given:
        allowUserAccess()

        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * tenantService.checkAndGetTenant(_)
    }

    def "addRolesToUserOnTenant verifies user"() {
        given:
        allowUserAccess()

        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenantId", "userId", "roleId")

        then:
        1 * userService.checkAndGetUserById(_)
        1 * userService.getUserByAuthToken(_)
    }

    def "addRolesToUserOnTenant verifies that user to modify is within callers domain when caller is user-admin"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = Mock(User)

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        def response = service.addRolesToUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * caller.getDomainId() >> entityFactory.createUser("username", null, "unique", "region")
        response.status == 403
    }

    def "addRolesToUserOnTenant verifies role"() {
        given:
        allowUserAccess()

        when:
        def response = service.addRolesToUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * applicationService.getClientRoleById(_) >> entityFactory.createClientRole("identity:role")
        response.status == 403
    }

    def "addRolesToUserOnTenant verifies callers precedence"() {
        given:
        allowUserAccess()

        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        service.addRolesToUserOnTenant(headers, authToken, "tenatn1", "user1", "role1").build()

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(_, _)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(_, _)
    }

    def "addRolesToUserOnTenant adds role to user on tenant"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()

        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> entityFactory.createUser()
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = service.addRolesToUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * tenantService.addTenantRoleToUser(user, _)
        response.status == 200
    }

    def "deleteRoleFromUserOnTenant verifies user admin level access"() {
        given:
        allowUserAccess()

        when:
        def response1 = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()
        def response2 = service.deleteRoleFromUserOnTenant(headers, "1$authToken", "tenant1", "user1", "role1").build()

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authToken) >> { throw new NotAuthorizedException() }
        response1.status == 401

        then:
        1 * authorizationService.verifyUserAdminLevelAccess(_) >> { throw new ForbiddenException() }
        response2.status == 403
    }

    def "deleteRoleFromUserOnTenant verifies tenant access"() {
        given:
        allowUserAccess()

        when:
        service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1")

        then:
        1 * authorizationService.verifyTokenHasTenantAccess(_, _)
    }

    def "deleteRoleFromUserOnTenant verifies user"() {
        given:
        allowUserAccess()

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * userService.checkAndGetUserById("user1") >> { throw new NotFoundException() }
        response.status == 404
    }

    def "deleteRoleFromUserOnTenant verifies tenant"() {
        given:
        allowUserAccess()

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * tenantService.checkAndGetTenant(_) >> { throw new NotFoundException() }
        response.status == 404
    }

    def "deleteRoleFromUserOnTenant verifies user belongs to callers domain when caller is user admin"() {
        given:
        allowUserAccess()

        def caller = Mock(User)
        def user = entityFactory.createUser()

        authorizationService.authorizeCloudUserAdmin(_) >> true
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * caller.getDomainId() >> "1$user.domainId"
        response.status == 403
    }

    def "deleteRoleFromUserOnTenant verifies role"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * applicationService.getClientRoleById(_) >> { throw new NotFoundException() }
        response.status == 404
    }

    def "deleteRoleFromUserOnTenant verifies caller precedence"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * precedenceValidator.verifyCallerPrecedenceOverUser(caller, user)
        1 * precedenceValidator.verifyCallerRolePrecedenceForAssignment(caller, _)
    }

    def "deleteRoleFromUserOnTenant deletes role from user"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        tenantService.checkAndGetTenant(_) >> entityFactory.createTenant()
        userService.checkAndGetUserById(_) >> user
        userService.getUserByAuthToken(_) >> caller
        applicationService.getClientRoleById(_) >> entityFactory.createClientRole()

        when:
        def response = service.deleteRoleFromUserOnTenant(headers, authToken, "tenant1", "user1", "role1").build()

        then:
        1 * tenantService.deleteTenantRoleForUser(user, _)
        response.status == 204
    }

    def "getSecretQA returns 200"() {
        given:
        mockSecretQAConverter(service)
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "id"

        userService.getUser(_) >> user
        userService.checkAndGetUserById(_) >> user

        when:
        def response = service.getSecretQAs(authToken,"id").build()

        then:
        1 * secretQAService.getSecretQAs(_) >> entityFactory.createSecretQAs()
        response.status == 200
    }

    def "createSecretQA returns 200"() {
        given:
        mockSecretQAConverter(service)
        allowUserAccess()

        def user = entityFactory.createUser()
        user.id = "id"

        userService.getUser(_) >> user
        userService.checkAndGetUserById(_) >> user

        when:
        def response = service.createSecretQA(authToken,"1", v1Factory.createSecretQA()).build()

        then:
        1 * secretQAService.addSecretQA("1", _)
        response.status == 200
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
        def userScopeAccess = createUserScopeAccess("tokenString", "userRsId", "clientId", new Date())

        scopeAccessMock = Mock()
        scopeAccessMock.getLDAPEntry() >> createLdapEntry()
        scopeAccessService.getScopeAccessByAccessToken(_) >>> [
                scopeAccessMock,
                userScopeAccess
        ]

        userService.getUser(_) >> entityUser
        tenantService.getGlobalRolesForUser(entityUser) >> [
                entityFactory.createTenantRole("identity:default")
        ].asList()

        scopeAccessService.getMostRecentDirectScopeAccessForUserByClientId(_, _) >> userScopeAccess

        when:
        def responseBuilder = service.impersonate(headers, authToken, impRequest)

        then:
        1 * scopeAccessService.updateExpiredUserScopeAccess(userScopeAccess, true) >> userScopeAccess
        responseBuilder.build().status == 200
    }

    def "setDomainId throws bad request"() {
        given:
        def caller = entityFactory.createUser().with {
            it.domainId = null
            return it
        }
        def user = entityFactory.createUser()

        when:
        service.assignUserToCallersDomain(caller, user)

        then:
        thrown(BadRequestException)
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
        1 * domainConverter.fromDomain(domain)
        1 * authenticationService.authenticateDomainUsernamePassword(_, _, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> createRackerScopeAcccss()
        1 * tenantService.getTenantRolesForUser(racker)
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
        1 * domainConverter.fromDomain(domain)
        1 * authenticationService.authenticateDomainRSA(_, _, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> createRackerScopeAcccss()
        1 * tenantService.getTenantRolesForUser(racker)
    }

    def "getUserCredential verifies access token"() {
        when:
        service.getUserPasswordCredentials(headers, authToken, "userId")

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(_)
    }

    def "getUserCredential verifies user level access"() {
        given:
        allowUserAccess()

        when:
        service.getUserPasswordCredentials(headers, authToken, "userId")

        then:
        0 * authorizationService.verifyUserLevelAccess(_)
    }

    def "identity:admin should not get other users password credentials"() {
        given:
        allowUserAccess()
        def user = entityFactory.createUser("user", "userId", "123456", "DFW")
        def caller = entityFactory.createUser("caller", "callerId", null, "DFW")

        userService.getUserById("userId") >> user
        userService.getUser(_) >> caller

        when:
        def response = service.getUserPasswordCredentials(headers, authToken, "userId").build()

        then:
        1 * authorizationService.verifyServiceAdminLevelAccess(_) >> {throw new ForbiddenException()}

        then:
        response.status == 403
    }

    def "getUserCredential gets user"() {
        given:
        allowUserAccess()

        when:
        service.getUserPasswordCredentials(headers, authToken, "userId")

        then:
        1 * userService.getUserById("userId")
    }

    def "getUserCredential verifies user is in callers domain when caller is user-admin"() {
        given:
        allowUserAccess()

        def user = Mock(User)
        def caller = Mock(User)

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller
        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        service.getUserApiKeyCredentials(headers, authToken, "userId")

        then:
        1 * authorizationService.verifyDomain(caller, user)
    }

    def "getUserCredential verifies caller is user when caller is default user"() {
        given:
        allowUserAccess()

        def user = Mock(User)
        def caller = Mock(User)
        caller.getId() >> "unique"

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller
        authorizationService.verifyServiceAdminLevelAccess(_) >> {throw new ForbiddenException()}

        when:
        def result = service.getUserPasswordCredentials(headers, authToken, "userId")

        then:
        result.build().status == 403
    }

    def "getUserCredential verifies user with id exists"() {
        given:
        allowUserAccess()

        authorizationService.authorizeCloudServiceAdmin(_) >> false
        authorizationService.authorizeCloudUserAdmin(_) >> false

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, "userId")

        then:
        result.build().status == 404
    }

    def "service-admin can get user password credential"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser().with {
            it.password = "Password1"
            return it
        }
        def caller = entityFactory.createUser()

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        when:
        def response2 = service.getUserPasswordCredentials(headers, authToken, "userId")

        then:
        response2.build().status == 200
    }

    def "service-admin can get user apikey credentials"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser().with {
            it.apiKey = "apiKey"
            return it
        }
        def caller = entityFactory.createUser()

        userService.getUserById(_) >> user
        userService.getUser(_) >> caller
        authorizationService.authorizeCloudServiceAdmin(_) >> true

        when:
        def response2 = service.getUserApiKeyCredentials(headers, authToken, "userId")

        then:
        response2.build().status == 200
    }

    def "addDomain verifies access level"() {
        given:
        allowUserAccess()
        mockDomainConverter(service)

        when:
        service.addDomain(authToken, uriInfo(), v1Factory.createDomain())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "addDomain verifies domain has name"() {
        given:
        allowUserAccess()
        def domain = v1Factory.createDomain("id", null)

        when:
        def response = service.addDomain(authToken, uriInfo(), domain)

        then:
        response.build().status == 400
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

    def "getEndpointsByDomainId verifies admin access"() {
        given:
        allowUserAccess()
        mockEndpointConverter(service)

        domainService.checkAndGetDomain(_) >> entityFactory.createDomain()

        when:
        service.getEndpointsByDomainId(authToken, "domainId")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
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

    def "removeTenantFromDomain verifies access level"() {
        given:
        allowUserAccess()

        when:
        service.removeTenantFromDomain(authToken, "domainId", "tenantId")

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
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

    def "getServiceApis verifies access level"() {
        given:
        allowUserAccess()
        mockCapabilityConverter(service)

        when:
        service.getServiceApis(authToken)

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "getServiceApis gets service api's"() {
        given:
        allowUserAccess()
        mockCapabilityConverter(service)

        when:
        def response = service.getServiceApis(authToken)

        then:
        1 * capabilityService.getServiceApis()
        response.build().status == 200
    }

    def "getPolicies verifies access level"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        service.getPolicies(authToken)

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "getPolicies gets policies"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        def response = service.getPolicies(authToken)

        then:
        1 * policyService.getPolicies()

        response.build().status == 200
    }

    def "add policy verifies access level"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "add policy validates policy name"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        def response = service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())

        then:
        1 * policyValidator.validatePolicyName(_) >> { throw new BadRequestException() }
        response.build().status == 400
    }

    def "add policy adds policy with duplicate exception and success"() {
        given:
        allowUserAccess()

        def policy1 = Mock(Policy)
        def policy2 = Mock(Policy)

        policyConverter = Mock(PolicyConverterCloudV20)
        policyConverter.fromPolicy(_) >>> [
                policy1,
                policy2
        ]

        service.policyConverterCloudV20 = policyConverter

        when:
        def response1 = service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())
        def response2 = service.addPolicy(uriInfo(), authToken, v1Factory.createPolicy())

        then:
        1 * policyService.addPolicy(policy1) >> { throw new DuplicateException() }
        1 * policyService.addPolicy(policy2)

        response1.build().status == 409
        response2.build().status == 201
    }

    def "updatePolicy verifies access level"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        service.updatePolicy(authToken,"policyId", v1Factory.createPolicy())

        then:
        1 * authorizationService.verifyIdentityAdminLevelAccess(_)
    }

    def "updatePolicy verifies policy"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        def response = service.updatePolicy(authToken, "policyId", v1Factory.createPolicy())

        then:
        1 * policyService.checkAndGetPolicy("policyId") >> { throw new NotFoundException() }
        response.build().status == 404
    }

    def "updatePolicy verifies policy name"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        service.updatePolicy(authToken, "policyId", v1Factory.createPolicy())

        then:
        1 * policyValidator.validatePolicyName(_)
    }

    def "updatePolicy updates policy"() {
        given:
        allowUserAccess()
        mockPolicyConverter(service)

        when:
        def response = service.updatePolicy(authToken, "policyId", v1Factory.createPolicy())

        then:
        1 * policyService.updatePolicy(_,_)
        response.build().status == 204
    }

    def "checkDomainFromAuthRequest returns domain"() {
        given:
        def domain = v1Factory.createDomain()
        def list = [ domain ].asList()
        def authRequest = v2Factory.createAuthenticationRequest("tenantId", "tenantName", null).with {
            it.any = list
            return it
        }

        when:
        def response = service.checkDomainFromAuthRequest(authRequest)

        then:
        response.description.equals(domain.getDescription())
        response.id.equals(domain.getId())
        response.name.equals(domain.getName())
    }

    def "method getAccessibleDomainsEndpointsForUser gets endpoints by user"() {
        given:
        allowUserAccess()
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
        authorizationService.authorizeCloudUser(_) >> true
        1 * userService.checkAndGetUserById(_) >> user
        1 * domainService.checkAndGetDomain(_) >> domain
        0 * scopeAccessService.getScopeAccessForUser(_)
        1 * scopeAccessService.getOpenstackEndpointsForUser(_) >> endpoints
        1 * tenantService.getTenantsByDomainId(_) >> tenants
        1 * userService.getUserByScopeAccess(_) >> user
        1 * tenantService.getTenantRolesForUser(_) >> tenantRoles
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

    def "authenticate without tenantAccess returns 401 (notAuthenticated)"() {
        given:
        def authRequestWithTenantName = v2Factory.createAuthenticationRequest("tokenString", "", "tenantName")
        def authRequestWithTenantId = v2Factory.createAuthenticationRequest("tokenString", "tenantId", "")

        authWithToken.authenticate(_) >> new AuthResponseTuple().with {
            it.user = entityFactory.createUser()
            return it
        }

        when:
        def response1 = service.authenticate(headers, authRequestWithTenantName).build()
        def response2 = service.authenticate(headers, authRequestWithTenantId).build()

        then:
        1 * tenantService.hasTenantAccess(_, "tenantName") >> false
        1 * tenantService.hasTenantAccess(_, "tenantId") >> false
        response1.status == 401
        response2.status == 401
    }

    def "authenticate with tenantAccess and all credential types succeeds"() {
        given:
        mockTokenConverter(service)
        mockAuthConverterCloudV20(service)

        def passwordCred = v2Factory.createJAXBPasswordCredentialsBase("username", "Password1")
        def apiKeyCred = v1Factory.createJAXBApiKeyCredentials("username", "apiKey")

        def passwordAuthRequest = v2Factory.createAuthenticationRequest("", "", passwordCred)
        def apiKeyAuthRequest = v2Factory.createAuthenticationRequest("", "", apiKeyCred)
        def tokenAuthRequest = v2Factory.createAuthenticationRequest("tokenString", "", "")

        def authResponseTuple = new AuthResponseTuple().with {
            it.user = entityFactory.createUser()
            it.userScopeAccess = createUserScopeAccess()
            return it
        }

        scopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> [].asList()

        when:
        def res1 = service.authenticate(headers, tokenAuthRequest).build()
        def res2 = service.authenticate(headers, passwordAuthRequest).build()
        def res3 = service.authenticate(headers, apiKeyAuthRequest).build()

        then:
        1 * authWithToken.authenticate(_) >> authResponseTuple
        1 * authWithPasswordCredentials.authenticate(_) >> authResponseTuple
        1 * authWithApiKeyCredentials.authenticate(_) >> authResponseTuple

        res1.status == 200
        res2.status == 200
        res3.status == 200
    }

    def "buildAuthResponse gets User's tenantRoles from the User"() {
        given:
        mockAuthConverterCloudV20(service)
        mockTokenConverter(service)

        def userScopeAccess = createUserScopeAccess()
        def user = entityFactory.createUser()
        def authRequest = v2Factory.createAuthenticationRequest("token", "", "")

        scopeAccessService.getOpenstackEndpointsForScopeAccess(_) >> [].asList()

        when:
        service.buildAuthResponse(userScopeAccess, null, user, authRequest)

        then:
        1 * tenantService.getTenantRolesForUser(user)
    }

    def "validateToken when caller token is racker token gets tenantRoles and rackerRoles by user"() {
        given:
        mockTokenConverter(service)
        mockUserConverter(service)
        allowRackerAccess()

        def racker = entityFactory.createRacker()

        when:
        service.validateToken(headers, authToken, "tokenId", "tenantId")

        then:
        1 * userService.getRackerByRackerId(_) >> racker
        1 * tenantService.getTenantRolesForUser(racker) >> [].asList()
        1 * userService.getRackerRoles(_) >> [].asList()
    }

    def "validateToken when caller token is user token gets tenantRoles by user"() {
        given:
        mockTokenConverter(service)
        mockUserConverter(service)
        allowUserAccess()

        def user = entityFactory.createUser()

        when:
        service.validateToken(headers, authToken, "tokenId", "tenantId")

        then:
        1 * userService.getUserByScopeAccess(_) >> user
        1 * tenantService.getTenantRolesForUser(user) >> [].asList()
    }

    def "validateToken when caller token is impersonated gets tenantRoles for user being impersonated"() {
        given:
        mockTokenConverter(service)
        mockUserConverter(service)
        allowImpersonatedAccess()

        def impersonator = entityFactory.createUser()
        def user = entityFactory.createUser()

        when:
        service.validateToken(headers, authToken, "tokenId", "tenantId")

        then:
        1 * userService.getUserByScopeAccess(_) >> impersonator
        1 * userService.getUser(_) >> user
        1 * tenantService.getTenantRolesForUser(user) >> [].asList()
        1 * tenantService.getGlobalRolesForUser(impersonator) >> [].asList()
    }

    def "listCredentials verifies user level access"() {
        when:
        def result = service.listCredentials(headers, authToken, "userId", null, null).build()

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authToken) >> createUserScopeAccess()
        1 * authorizationService.verifyUserLevelAccess(_) >> { throw new NotAuthorizedException() }
        result.status == 401
    }

    def "listCredentials gets caller from scopeAccess and user from path parameter"() {
        given:
        allowUserAccess()

        when:
        service.listCredentials(headers, authToken, "userId", null, null)

        then:
        1 * userService.getUserByScopeAccess(_)
        1 * userService.checkAndGetUserById("userId")
    }

    def "listCredentials verifies self when caller is user-admin"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole("identity:user-admin").with {
            it.roleRsId = "roleRsId"
            it.userId = "userId"
            return it
        }
        def userAdminTenantList = [ tenantRole ].asList()

        tenantService.getTenantRolesForUser(_) >>  userAdminTenantList

        when:
        service.listCredentials(headers, authToken, "userId", null, null)

        then:
        1 * userService.checkAndGetUserById("userId") >> user
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * authorizationService.verifySelf(caller, user) >> { throw new NotAuthorizedException() }
    }

    def "listCredentials verifies self when caller is default user"() {
        given:
        allowUserAccess()

        def user = entityFactory.createUser()
        def caller = entityFactory.createUser()
        def tenantRole = entityFactory.createTenantRole("identity:default").with {
            it.roleRsId = "roleRsId"
            it.userId = "userId"
            return it
        }
        def defaultuserTenantList = [ tenantRole ].asList()

        tenantService.getTenantRolesForUser(_) >>> [
                [].asList(),
                defaultuserTenantList
        ]

        when:
        service.listCredentials(headers, authToken, "userId", null, null)

        then:
        1 * userService.checkAndGetUserById("userId") >> user
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * authorizationService.verifySelf(caller, user) >> { throw new NotAuthorizedException() }
    }

    def "listCredentials authorizes service admin and checks for password"() {
        given:
        allowUserAccess()

        def user = Mock(User)
        def caller = entityFactory.createUser()
        def tenantList = [ entityFactory.createTenantRole() ].asList()

        tenantService.getTenantRolesForUser(_) >> tenantList
        userService.checkAndGetUserById("userId") >> user
        userService.getUserByScopeAccess(_) >> caller

        when:
        def result = service.listCredentials(headers, authToken, "userId", null, null).build()

        then:
        result.status == 200
        1 * user.getApiKey() >> ""
        2 * user.getPassword() >> "Password"
        1 * user.getUsername() >> "username"
        1 * authorizationService.authorizeCloudServiceAdmin(_) >> true
    }

    def "listCredentials checks for apikey"() {
        given:
        allowUserAccess()

        def user = Mock(User)
        def caller = entityFactory.createUser()
        def tenantList = [ entityFactory.createTenantRole() ].asList()

        tenantService.getTenantRolesForUser(_) >> tenantList
        userService.checkAndGetUserById("userId") >> user
        userService.getUserByScopeAccess(_) >> caller
        authorizationService.authorizeCloudServiceAdmin(_) >> false

        when:
        def result = service.listCredentials(headers, authToken, "userId", null, null).build()

        then:
        result.status == 200
        2 * user.getApiKey() >> "apiKey"
        1 * user.getUsername() >> "username"
    }

    def "isUserAdmin gets tenantRoles by user if role parameter is null and returns false"() {
        given:
        def user = entityFactory.createUser()
        def roleList = [ entityFactory.createTenantRole() ].asList()

        when:
        def result = service.isUserAdmin(user)

        then:
        1 * tenantService.getTenantRolesForUser(user) >> roleList
        result == false
    }

    def "isUserAdmin checks if user-admin role is in roleList"() {
        given:
        def user = entityFactory.createUser()
        def roleList = [ entityFactory.createTenantRole("identity:user-admin") ].asList()

        when:
        def result = service.isUserAdmin(user)

        then:
        1 * tenantService.getTenantRolesForUser(user) >> roleList
        result == true
    }

    def "isDefaultUser gets tenantRoles by user if role parameter is null and returns false"() {
        given:
        def user = entityFactory.createUser()
        def roleList = [ entityFactory.createTenantRole() ].asList()

        when:
        def result = service.isDefaultUser(user)

        then:
        1 * tenantService.getTenantRolesForUser(user) >> roleList
        result == false
    }

    def "isDefaultUser checks if user-admin role is in roleList"() {
        given:
        def user = entityFactory.createUser()
        def roleList = [ entityFactory.createTenantRole("identity:default") ].asList()

        when:
        def result = service.isDefaultUser(user)

        then:
        1 * tenantService.getTenantRolesForUser(user) >> roleList
        result == true
    }

    def "addService checks if caller is service admin"() {
        when:
        def result = service.addService(headers, uriInfo(), authToken, null).build()

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authToken) >> createUserScopeAccess("token", "1", "clientid", new Date().minus(1))
        result.status == 401
    }

     def "deleteUserFromSoftDeleted checks if caller is service admin"() {
        when:
        def result = service.deleteUserFromSoftDeleted(headers, authToken, "userId")

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authToken) >> createUserScopeAccess()
        1 * authorizationService.verifyServiceAdminLevelAccess(_) >> { throw new ForbiddenException() }
        result.status == 403
    }

    def "listTenants gets user from scopeAccess and tenants from user"() {
        given:
        allowUserAccess()
        mockTenantConverter(service)
        def user = entityFactory.createUser()

        when:
        def result = service.listTenants(headers, authToken, null, null).build()

        then:
        1 * userService.getUserByScopeAccess(_) >> user
        1 * tenantService.getTenantsForUserByTenantRoles(user)  >> [].asList()
        result.status == 200
    }

    def "user accesslevel is verified by getAdminsForDefaultUser"() {
        given:
        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        domainService.getEnabledDomainAdmins(_, _) >> [].asList()

        when:
        service.getUserAdminsForUser(authToken, "userId")

        then:
        1 * scopeAccessService.getScopeAccessByAccessToken(authToken) >> createUserScopeAccess()
        1 * authorizationService.verifyUserLevelAccess(_)
    }

    def "when caller is defaultuser getAdminsForDefaultUser verifies caller is user"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        authorizationService.authorizeCloudUser(_) >> true

        when:
        service.getUserAdminsForUser(authToken, "userId")

        then:
        1 * userService.getUser(_) >> caller
        1 * userService.checkAndGetUserById(_) >> user

        then:
        thrown(ForbiddenException)
    }

    def "when caller is userAdmin getAdminsForDefaultUser verifies user is in callers domain"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "callerDomain", "REGION")
        def user = entityFactory.createUser("user", "userId", "userDomain", "REGION")

        authorizationService.authorizeCloudUserAdmin(_) >> true

        when:
        service.getUserAdminsForUser(authToken, "userId")

        then:
        1 * userService.getUser(_) >> caller
        1 * userService.checkAndGetUserById(_) >> user

        then:
        thrown(ForbiddenException)
    }

    def "a list of enabled admins is returned by getAdminsForDefaultUser"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", "domainId", "REGION")
        def user = entityFactory.createUser("user", "userId", "domainId", "REGION")

        userService.getUser(_) >> caller
        userService.checkAndGetUserById(_) >> user

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        1 * domainService.getEnabledDomainAdmins("domainId")
        response.status == 200
    }

    def "A list of admins is not retrieve if the user has no domain"() {
        given:
        allowUserAccess()
        def caller = entityFactory.createUser("caller", "callerId", null, "REGION")
        def user = entityFactory.createUser("user", "userId", null, "REGION")

        userService.getUser(_) >> caller
        userService.checkAndGetUserById(_) >> user

        when:
        def response = service.getUserAdminsForUser(authToken, "userId").build()

        then:
        response.status == 200
        0 * domainService.getEnabledDomainAdmins(_, _)
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
        allowUserAccess()

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, userId).build()

        then:
        result.status == 200
        userService.getUserById(userId) >> user
        userService.getUser(_) >> caller
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
        userService.getUserById(_) >> user
        userService.getUser(_) >> caller
        authorizationService.authorizeCloudUser(_) >> true
        authorizationService.authorizeCloudUserAdmin(_) >> false
    }

    def "isRoleWeightAllowed verifies that the specified weight is allowed"() {
        given:
        def weights = [ "0", "100", "500", "1000", "2000" ].asList()

        when:
        service.isRoleWeightValid(500)
        service.isRoleWeightValid(3)

        then:
        notThrown(BadRequestException)

        then:
        thrown(BadRequestException)
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
        1 * authenticationService.authenticateDomainUsernamePassword(_, _, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> { arg1, arg2, List<String> arg3 ->
            assert (arg3.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD))
            createRackerScopeAcccss()
        }
        1 * authConverter.toAuthenticationResponse(_, _, _, _) >> { arg1, ScopeAccess arg2, arg3, arg4 ->
            assert (arg2.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD))
            new AuthenticateResponse()
        }
    }

    def "authenticateFederatedDomain sets token authenticatedBy with rsa credentials"() {
        given:
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
        1 * authenticationService.authenticateDomainRSA(_, _, _) >> authResult
        1 * scopeAccessService.getValidRackerScopeAccessForClientId(_, _, _) >> { arg1, arg2, List<String> arg4 ->
            assert (arg4.contains(GlobalConstants.AUTHENTICATED_BY_RSAKEY))
            createRackerScopeAcccss()
        }
        1 * authConverter.toAuthenticationResponse(_, _, _, _) >> { arg1, ScopeAccess arg2, arg3, arg4 ->
            assert (arg2.authenticatedBy.contains(GlobalConstants.AUTHENTICATED_BY_RSAKEY))
            new AuthenticateResponse()
        }
    }

    def "calling getUserByEmail returns the user"() {
        given:
        def user = entityFactory.createUser()
        allowUserAccess()

        when:
        def result = service.getUsersByEmail(headers, authToken, "email@email.com").build()

        then:
        userService.getUsersByEmail(_) >> [user].asList()

        then:
        result.status == 200
    }

    def "userAdmin calling getUsersByEmail filters subUsers by domain"() {
        given:
        allowUserAccess()
        def converter = new UserConverterCloudV20()
        converter.mapper = new DozerBeanMapper()
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

        when:
        def result = service.getUsersByEmail(headers, authToken, email).build()

        then:
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * userService.getUsersByEmail(_) >> [subUser1, subUser2].asList()
        1 * authorizationService.authorizeCloudUserAdmin(_) >> true
        1 * authorizationService.hasSameDomain(caller, subUser1) >> true
        1 * authorizationService.hasSameDomain(caller, subUser2) >> false

        then:
        result.status == 200
        UserList users = result.entity
        users.user.size() == 1
    }

    def "Disabling a enabled user sends an atom feed"(){
        given:
        allowUserAccess()
        def converter = new UserConverterCloudV20()
        converter.mapper = new DozerBeanMapper()
        service.userConverterCloudV20 = converter
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
        userService.getUserByAuthToken(_) >> entityFactory.createUser()
        userService.isUsernameUnique(_) >> true

        when:
        service.updateUser(headers, authToken, "2", user)

        then:
        1 * atomHopperClient.asyncPost(_,_)
    }

    def "Disabling a disabled user does not send an atom feed"(){
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
        0 * atomHopperClient.asyncPost(_,_)
    }

    def "validate role for create - throws BadRequestException"(){
        when:
        def role = null;
        if (name != null || serviceId != null){
            role = v2Factory.createRole(name, serviceId, null)
        }
        service.validateRole(role)

        then:
        thrown(ex)

        where:
        name    | serviceId | ex
        null    | null      | BadRequestException
        null    | "service" | BadRequestException
    }

    @Unroll
    def "delete api key credential"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        def userInDomain = entityFactory.createUser("caller", "userId2", "domainId", "region")
        userInDomain.apiKey = "key"
        def userOutOfDomain = entityFactory.createUser("caller", "userId2", "domainId2", "region")
        userOutOfDomain.apiKey = "key"

        when:
        def inDomainResult = service.deleteUserCredential(headers, authToken, "1", JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)
        def outDomainResult = service.deleteUserCredential(headers, authToken, "1", JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)

        then:
        2 * userService.checkAndGetUserById(_) >>> [ userInDomain, userOutOfDomain ]
        2 * userService.getUserByAuthToken(_) >>> [ caller, caller, caller, caller ]
        4 * authorizationService.hasServiceAdminRole(_) >>> [ callerSA, userSA, callerSA, userSA ]
        4 * authorizationService.hasIdentityAdminRole(_) >>> [ callerIA, userIA, callerIA, userIA ]
        4 * authorizationService.hasUserAdminRole(_) >>> [ callerUA, userUA, callerUA, userUA ]
        4 * authorizationService.hasUserManageRole(_) >>> [ callerUM, userUM, callerUM, userUM ]
        if (precedenceForbidden) {
            2 * precedenceValidator.verifyCallerPrecedenceOverUser(_,_) >> {throw new ForbiddenException()}
        }


        inDomainResult.status == inDomain
        outDomainResult.status == outDomain

        where:
        callerSA | callerIA | callerUA | callerUM | userSA | userIA | userUA | userUM | inDomain | outDomain | precedenceForbidden
        // identity:service-admin calls
        true     | false    | false    | false    | true   | false  | false  | false  | 403      | 403       | false
        true     | false    | false    | false    | false  | true   | false  | false  | 204      | 204       | false
        true     | false    | false    | false    | false  | false  | true   | false  | 204      | 204       | false
        true     | false    | false    | false    | false  | false  | false  | true   | 204      | 204       | false
        true     | false    | false    | false    | false  | false  | false  | false  | 204      | 204       | false
        // identity:admin calls
        false    | true     | false    | false    | true   | false  | false  | false  | 403      | 403       | true
        false    | true     | false    | false    | false  | true   | false  | false  | 403      | 403       | false
        false    | true     | false    | false    | false  | false  | true   | false  | 204      | 204       | false
        false    | true     | false    | false    | false  | false  | false  | true   | 204      | 204       | false
        false    | true     | false    | false    | false  | false  | false  | false  | 204      | 204       | false
        // identity:user-admin calls
        false    | false    | true     | false    | true   | false  | false  | false  | 403      | 403       | true
        false    | false    | true     | false    | false  | true   | false  | false  | 403      | 403       | true
        false    | false    | true     | false    | false  | false  | true   | false  | 403      | 403       | false
        false    | false    | true     | false    | false  | false  | false  | true   | 204      | 403       | false
        false    | false    | true     | false    | false  | false  | false  | false  | 204      | 403       | false
        // identity:user-manage calls
        false    | false    | false    | true     | true   | false  | false  | false  | 403      | 403       | true
        false    | false    | false    | true     | false  | true   | false  | false  | 403      | 403       | true
        false    | false    | false    | true     | false  | false  | true   | false  | 403      | 403       | true
        false    | false    | false    | true     | false  | false  | false  | true   | 403      | 403       | false
        false    | false    | false    | true     | false  | false  | false  | false  | 204      | 403       | false
        // identity:default calls
        false    | false    | false    | false    | true   | false  | false  | false  | 403      | 403       | true
        false    | false    | false    | false    | false  | true   | false  | false  | 403      | 403       | true
        false    | false    | false    | false    | false  | false  | true   | false  | 403      | 403       | true
        false    | false    | false    | false    | false  | false  | false  | true   | 403      | 403       | true
        false    | false    | false    | false    | false  | false  | false  | false  | 403      | 403       | false
    }

    @Unroll
    def "self delete api key credentials"() {
        given:
        allowUserAccess()

        def caller = entityFactory.createUser("caller", "userId", "domainId", "region")
        caller.apiKey = "key"

        when:
        def result = service.deleteUserCredential(headers, authToken, "1", JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS)

        then:
        1 * userService.checkAndGetUserById(_) >> caller
        1 * userService.getUserByAuthToken(_) >> caller
        2 * authorizationService.hasServiceAdminRole(_) >> callerSA
        2 * authorizationService.hasIdentityAdminRole(_) >> callerIA
        2 * authorizationService.hasUserAdminRole(_) >> callerUA
        2 * authorizationService.hasUserManageRole(_) >>> callerUM

        result.status == expectedResult

        where:
        where:
        callerSA | callerIA | callerUA | callerUM | expectedResult
        true     | false    | false    | false    | 204
        false    | true     | false    | false    | 204
        false    | false    | true     | false    | 403
        false    | false    | false    | true     | 204
        false    | false    | false    | false    | 204
    }

    def "validate role for create - if service id is null set it"(){
        given:
        def role = v2Factory.createRole("name", null, null)
        allowUserAccess()

        when:
        service.addRole(headers,uriInfo(),authToken, role)

        then:
        1 * config.getString("cloudAuth.globalRoles.clientId") >> "123"
        role.serviceId == "123"
    }

    def "Add Group to user"() {
        given:
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        userService.getSubUsers(_) >> [].asList()

        when:
        Response.ResponseBuilder response = service.addUserToGroup(headers, authToken, "1", "2")

        then:
        1 * userService.addGroupToUser(_, _)
        1 * atomHopperClient.asyncPost(_, _)
        response.build().status == 204
    }

    def "Add Group to user with subUsers sends an atom feed for each user"() {
        given:
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        userService.getSubUsers(_) >> [entityFactory.createUser()].asList()

        when:
        Response.ResponseBuilder response = service.addUserToGroup(headers, authToken, "1", "2")

        then:
        2 * userService.addGroupToUser(_, _)
        2 * atomHopperClient.asyncPost(_, _)
        response.build().status == 204
    }

    def "Add an existing Group to user"() {
        given:
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        userService.isUserInGroup(_, _) >> true
        userService.getSubUsers(_) >> [].asList()

        when:
        Response.ResponseBuilder response = service.addUserToGroup(headers, authToken, "1", "2")

        then:
        0 * userService.addGroupToUser(_, _)
        0 * atomHopperClient.asyncPost(_, _)
        response.build().status == 204
    }

    def "Delete Group from user"() {
        given:
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        userService.getSubUsers(_) >> [].asList()
        userService.isUserInGroup(_, _) >> true

        when:
        Response.ResponseBuilder response = service.removeUserFromGroup(headers, authToken, "1", "2")

        then:
        1 * userService.deleteGroupFromUser(_, _)
        1 * atomHopperClient.asyncPost(_, _)
        response.build().status == 204
    }

    def "Delete Group from user with subUsers sends an atom feed for each user"(){
        given:
        allowUserAccess()
        groupService.checkAndGetGroupById(_) >> entityFactory.createGroup("1", "nameone", "desc")
        userService.checkAndGetUserById(_) >> entityFactory.createUser()
        authorizationService.hasDefaultUserRole(_) >> false
        authorizationService.hasUserAdminRole(_) >> true
        userService.getSubUsers(_) >> [entityFactory.createUser()].asList()
        userService.isUserInGroup(_, _) >> true

        when:
        Response.ResponseBuilder response = service.removeUserFromGroup(headers, authToken, "1", "2")

        then:
        2 * userService.deleteGroupFromUser(_, _)
        2 * atomHopperClient.asyncPost(_, _)
        response.build().status == 204
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

    def "Only service-admin are allow" () {
        given:
        allowUserAccess()
        def user = entityFactory.createUser()
        user.password = "Password1"

        when:
        def response = service.getUserPasswordCredentials(headers, authToken, "1")

        then:
        1 * authorizationService.verifyServiceAdminLevelAccess(_)
        1 * userService.getUserById(_) >> user

        then:
        response.build().status == 200
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
        userService.getUserByAuthToken(authToken) >> caller
        tenantService.getGlobalRolesForUser(_) >> userRoles
        authorizationService.hasDefaultUserRole(_) >> true

        then:
        1 * tenantService.addTenantRoleToUser(_, _)
    }

    def "User with user-manage role can create new user" () {
        given:
        allowUserAccess()
        def user = v1Factory.createUserForCreate()
        def caller = entityFactory.createUser().with {
            it.username = "caller"
            return it
        }

        when:
        service.addUser(headers, uriInfo(), authToken, user)

        then:
        1 * authorizationService.verifyUserManagedLevelAccess(_)
        2 * authorizationService.authorizeUserManageRole(_) >> true
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * userService.getUsersWithDomain(_) >> [].asList()
        1 * domainService.getDomainAdmins(_) >> [].asList()
        notThrown(BadRequestException)
        1 * defaultRegionService.validateDefaultRegion(_)
        1 * userService.addUser(_)
        1 * userService.getGroupsForUser(_) >> [].asList()
    }

    def "User with user-manage role can delete user" () {
        given:
        allowUserAccess()
        def user = entityFactory.createUser()

        when:
        service.deleteUser(headers, authToken, "userId")

        then:
        1 * authorizationService.verifyUserManagedLevelAccess(_)
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.authorizeUserManageRole(_) >> true
        1 * authorizationService.verifyDomain(_, _)
        1 * userService.deleteUser(_)
    }

    def "User with user-manage role cannot delete user with user-manage role" () {
        given:
        allowUserAccess()
        def user = entityFactory.createUser()

        when:
        def result = service.deleteUser(headers, authToken, "userId")

        then:
        1 * authorizationService.verifyUserManagedLevelAccess(_)
        1 * userService.checkAndGetUserById(_) >> user
        1 * authorizationService.authorizeUserManageRole(_) >> true
        1 * authorizationService.hasUserManageRole(_) >> true
        result.build().status == 401
    }

    @Unroll("User with user-manage role can update different user with roles updatingServiceAdmin=#updatingServiceAdmin;updatingIdentityAdmin=#updatingIdentityAdmin;updatingUserAdmin=#updatingUserAdmin;updatingManagedUser=#updatingManagedUser;updatingCloudUser=#updatingCloudUser")
    def "User with user-manage role can update different user with roles"() {
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

        userService.getUserByAuthToken(_) >> caller
        userService.checkAndGetUserById(_) >> retrievedUser

        //mock caller roles
        authorizationService.authorizeCloudIdentityAdmin(_) >> false
        authorizationService.authorizeCloudUserAdmin(_) >> false
        authorizationService.authorizeUserManageRole(_) >> true
        authorizationService.authorizeCloudUser(_) >> false

        //mock user being updated roles
        authorizationService.hasServiceAdminRole(_) >> updatingServiceAdmin
        authorizationService.hasIdentityAdminRole(_) >> updatingIdentityAdmin
        authorizationService.hasUserAdminRole(_) >> updatingUserAdmin
        authorizationService.hasUserManageRole(_) >> updatingManagedUser
        authorizationService.hasDefaultUserRole(_) >> updatingCloudUser

        when:
        def result = service.updateUser(headers, authToken, "1", user)

        then:
        //Despise this hack here, but code expects the authorization service to throw this to verify user managers/admins can not update service/identity admins.
        //verifyDomain is tested in separate tests so just simulating what verifyDomain would do in this particular case.
        if (updatingServiceAdmin || updatingIdentityAdmin) {
            1 * authorizationService.verifyDomain(_,_) >> {throw new ForbiddenException()}
        }

        result.status == expectedStatus

        //Application logic assumption is that only ONE of the following roles service/identity/useradmin/usermanage
        where:
        updatingServiceAdmin    | updatingIdentityAdmin | updatingUserAdmin | updatingManagedUser | updatingCloudUser   | expectedStatus
        true                    | false                 | false             | false               | false               | 403
        false                   | true                  | false             | false               | false               | 403
        false                    | false                | true              | false               | false               | 403
        false                    | false                | false             | true                | true                | 200
        false                    | false                | false             | false               | true                | 200
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

        when:
        def result = service.addUserRole(headers, authToken, "abc", "123")

        then:
        1 * applicationService.getClientRoleById(_) >> clientRole
        1 * userService.checkAndGetUserById(_) >> user
        1 * userService.getUserByAuthToken(_) >> caller
        1 * authorizationService.authorizeCloudUserAdmin(_) >> true
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
        User user = entityFactory.createUser().with {
            it.domainId = "1"
            return it
        }
        User caller = entityFactory.createUser().with {
            it.domainId = "2"
            return it
        }

        when:
        def result = service.addUserRole(headers, authToken, "abc", "123")

        then:
        1 * applicationService.getClientRoleById(_) >> clientRole
        1 * userService.checkAndGetUserById(_) >> user
        1 * userService.getUserByAuthToken(_) >> caller
        result.build().status == 403
    }

    def "Add user-manage role to default user" () {
        given:
        allowUserAccess()
        ClientRole clientRole = new ClientRole().with {
            it.name = "identity:user-manage"
            return it
        }
        User user = entityFactory.createUser()
        user.id = "something different from caller"
        User caller = entityFactory.createUser()

        when:
        def result = service.addUserRole(headers, authToken, "abc", "123")

        then:
        1 * applicationService.getClientRoleById(_) >> clientRole
        1 * userService.checkAndGetUserById(_) >> user
        1 * userService.getUserByAuthToken(_) >> caller
        1 * authorizationService.authorizeCloudUserAdmin(_) >> true
        1 * authorizationService.hasDefaultUserRole(_) >> true
        result.build().status == 200
    }

    def "User with user-manage role can get user by ID" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()
        User caller = entityFactory.createUser()

        when:
        def result = service.getUserById(headers, authToken, "abc123")

        then:
        1 * userService.getUser(_) >> caller
        1 * authorizationService.authorizeCloudUser(_) >> true
        1 * authorizationService.hasUserManageRole(_) >> true
        1 * userService.getUserById(_) >> user
        1 * authorizationService.authorizeUserManageRole(_) >> true
        1 * authorizationService.verifyDomain(_, _)
        result.build().status == 200
    }

    def "User with user-manage role can get user by name" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()
        User caller = entityFactory.createUser()

        when:
        def result = service.getUserByName(headers, authToken, "testName")

        then:
        1 * userService.getUser(_) >> user
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * authorizationService.authorizeUserManageRole(_) >> true
        1 * authorizationService.verifyDomain(_, _)
        result.build().status == 200
    }

    def "User with user-manage role can get user by email" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()
        def users = [user].asList()
        User caller = entityFactory.createUser()

        when:
        def result = service.getUsersByEmail(headers, authToken, "test@rackspace.com")

        then:
        1 * userService.getUsersByEmail(_) >> users
        1 * userService.getUserByScopeAccess(_) >> caller
        1 * authorizationService.authorizeUserManageRole(_) >> true
        result.build().status == 200
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
        1 * userService.getUserById(_) >> user
        1 * userService.getUser(_) >> caller
        1 * authorizationService.authorizeCloudUser(_) >> false
        1 * authorizationService.authorizeUserManageRole(_) >> true
        result.build().status == 200
    }

    def "User with user-manage role cannot get User Admin's API key" () {
        given:
        allowUserAccess()
        User user = entityFactory.createUser()

        when:
        def result = service.getUserApiKeyCredentials(headers, authToken, "abc123")

        then:
        1 * authorizationService.verifyUserLevelAccess(_)
        1 * userService.getUserById(_) >> user
        1 * authorizationService.authorizeUserManageRole(_) >> true
        1 * authorizationService.hasUserAdminRole(_) >> true
        result.build().status == 403
    }

<<<<<<< HEAD
    def "Create user will generate an apiKey - generate.apiKey.userForCreate=true" () {
        given:
        allowUserAccess()
        def user = v1Factory.createUserForCreate()

        when:
        service.addUser(headers, uriInfo(), authToken, user)

        then:
        1 * config.getBoolean("generate.apiKey.userForCreate") >> true
        1 * userService.addUser(_) >> {User u ->
            assert(u.apiKey != null)
        }
    }

    def "Create user will not generate an apiKey - generate.apiKey.userForCreate=false" () {
        given:
        allowUserAccess()
        def user = v1Factory.createUserForCreate()

        when:
        service.addUser(headers, uriInfo(), authToken, user)

        then:
        1 * config.getBoolean("generate.apiKey.userForCreate") >> false
        1 * userService.addUser(_) >> {User u ->
            assert(u.apiKey == null)
        }
    }

    def "checkMaxNumberOfUsersInDomain does not allow creating more than maxNumberOfUsersInDomain"() {
        given:
        config.getInt("maxNumberOfUsersInDomain") >> maxNumberOfUsersInDomain

        List<User> users = new ArrayList<>()
        for (int i = 0; i < numberOfUsers; i++) {
            def user = entityFactory.createUser().with {
                it.username = "username$i"
                it
            }
            users.add(user)
        }

        when:
        boolean result = false
        try {
            service.checkMaxNumberOfUsersInDomain(users)
        } catch (BadRequestException e) {
            result = true
        }

        then:
        result == badRequest

        where:
        maxNumberOfUsersInDomain | numberOfUsers | badRequest
        2                        | 2             | true
        2                        | 1             | false
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
=======
    def "Validate a saml response" () {
        given:
        def samlResponse = Mock(org.opensaml.saml2.core.Response)
        def mockAuthData = Mock(AuthData)
        def responseBuilder = Mock(javax.ws.rs.core.Response.ResponseBuilder)
        AuthenticateResponse mockAuthenticateResponse = Mock()
        AuthConverterCloudV20 authConverter = Mock()
        service.authConverterCloudV20 = authConverter;

        and:
        defaultFederatedIdentityService.generateAuthenticationInfo() >> mockAuthData
        authConverter.toAuthenticationResponse(_) >> mockAuthenticateResponse

        when: "saml response is validated"
        def result = service.validateSamlResponse(headers, samlResponse)

        then:
        result.build().status == 200
    }

    def "List tenants for federated token"() {
        given:
        allowFederatedTokenAccess()
        mockTenantConverter(service)

        when:
        def result = service.listTenants(headers, authToken, null, null).build()

        then:
        1 * tenantService.getTenantsForFederatedTokenByTenantRoles(_)  >> [].asList()
        result.status == 200
>>>>>>> B-58104 Identity Federation Accept SAML Response and Generate Token
    }

    def mockServices() {
        mockAuthenticationService(service)
        mockAuthorizationService(service)
        mockApplicationService(service)
        mockScopeAccessService(service)
        mockTenantService(service)
        mockGroupService(service)
        mockUserService(service)
        mockDefaultRegionService(service)
        mockDomainService(service)
        mockPolicyService(service)
        mockCapabilityService(service)
        mockCloudRegionService(service)
        mockQuestionService(service)
        mockSecretQAService(service)
        mockEndpointService(service)
        mockFederatedIdentityService(service);
    }

    def mockMisc() {
        mockAtomHopperClient(service)
        mockConfiguration(service)
        mockCloudGroupBuilder(service)
        mockCloudKsGroupBuilder(service)
        mockValidator(service)
        mockValidator20(service)
        mockPolicyValidator(service)
        mockPrecedenceValidator(service)
        mockUserPaginator(service)
        mockAuthWithToken(service)
        mockAuthWithApiKeyCredentials(service)
        mockAuthWithPasswordCredentials(service)
        mockUserConverter(service)
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
            mockedElement.getValue() >> v2Factory.createPasswordCredentialsBase()
        }
        new AuthenticationRequest().with {
            it.credential = mockedElement
            return it
        }
    }
}
