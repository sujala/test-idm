package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.converter.cloudv20.QuestionConverterCloudV20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.dao.impl.LdapQuestionRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessPeristenceRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.validation.RolePrecedenceValidator
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.impl.*
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capability
import com.rackspace.idm.domain.dao.impl.LdapCapabilityRepository
import com.rackspace.idm.api.converter.cloudv20.CapabilityConverterCloudV20

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20
import com.rackspace.idm.api.resource.pagination.PaginatorContext

import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.rackspace.idm.api.resource.pagination.Paginator
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.dao.impl.LdapApplicationRepository
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.domain.dao.impl.LdapTenantRepository
import com.rackspace.idm.domain.dao.impl.LdapApplicationRoleRepository

import javax.ws.rs.core.HttpHeaders
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.dao.impl.LdapTenantRoleRepository
import org.openstack.docs.identity.api.v2.Role
import com.rackspace.idm.exception.ForbiddenException
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.joda.time.DateTime
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA;

/*
 This class uses the application context but mocks the ldap interactions
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultCloud20ServiceIntegrationTest extends Specification {

    @Autowired DefaultUserService userService
    @Autowired Configuration configuration
    @Autowired DefaultCloud20Service cloud20Service
    @Autowired DefaultQuestionService questionService
    @Autowired QuestionConverterCloudV20 questionConverter
    @Autowired DefaultCapabilityService capabilityService
    @Autowired CapabilityConverterCloudV20 capabilityConverter
    @Autowired UserConverterCloudV20 userConverterCloudV20
    @Autowired Paginator<User> userPaginator
    @Autowired ApplicationService clientService
    @Autowired TenantService tenantService

    @Shared RolePrecedenceValidator precedenceValidator
    @Shared DefaultScopeAccessService scopeAccessService
    @Shared DefaultAuthorizationService authorizationService
    @Shared DefaultCloudRegionService cloudRegionService
    @Shared LdapUserRepository userDao
    @Shared LdapScopeAccessPeristenceRepository scopeAccessDao
    @Shared LdapQuestionRepository questionDao
    @Shared LdapCapabilityRepository capabilityDao
    @Shared LdapApplicationRepository clientDao
    @Shared LdapTenantRepository tenantDao
    @Shared LdapApplicationRoleRepository clientRoleDao
    @Shared LdapTenantRoleRepository tenantRoleDao


    @Shared HttpHeaders headers
    @Shared def authToken = "token"
    @Shared def offset = "0"
    @Shared def limit = "25"
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def questionId = "id"
    @Shared JAXBObjectFactories objFactories;
    @Shared def roleId = "roleId"

    @Shared def adminRole
    @Shared def userAdminRole
    @Shared def defaultUserRole
    @Shared def specialRole
    @Shared def tenantRoleList
    @Shared def serviceAdminRole
    @Shared def genericRole
    @Shared def userAdmin1
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

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
        objFactories = new JAXBObjectFactories()
    }

    def cleanupSpec() {
    }

    def "create a user sets the default region"() {
        given:
        createMocks()
        allowAccess()

        userDao.isUsernameUnique(_) >> true

        cloudRegionService.getDefaultRegion("US") >> region()

        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        when:
        userService.addUser(user)

        then:
        user.region == "DFW"
    }

    def "create user without default region throws bad request"() {
        given:
        createMocks()
    allowAccess()

        userDao.isUsernameUnique(_) >> true

        cloudRegionService.getDefaultRegion("US") >> null

        def user = user("user$sharedRandom", "user@email.com", true, "user$sharedRandom")

        when:
        userService.addUser(user)

        then:
        thrown(BadRequestException)
    }

    def "question create verifies Identity admin level access"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.addQuestion(uriInfo(), authToken, jaxbQuestion())

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_);
    }

    def "question update verifies Identity admin level access"() {
        given:
        createMocks()
        allowAccess()

        questionDao.getObject(_) >> question()

        when:
        cloud20Service.updateQuestion(authToken, questionId, jaxbQuestion())

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_);
    }

    def "question delete verifies Identity admin level access"() {
        given:
        createMocks()
        allowAccess()

        questionDao.getObject(_) >> question()

        when:
        cloud20Service.deleteQuestion(authToken, questionId)

        then:
        1 *  authorizationService.verifyIdentityAdminLevelAccess(_);
    }

    def "get question verifies default and user admin"() {
        given:
        createMocks()
        allowAccess()
        def questions = new ArrayList<Question>()
        def question = question()
        questions.add(question)

        questionDao.getObject(_) >> question
        questionDao.getObjects(_) >> questions

        when:
        cloud20Service.getQuestion(authToken, questionId)
        cloud20Service.getQuestions(authToken)

        then:
        2 *  authorizationService.verifyUserLevelAccess(_);
    }

    def "add question calls ldap"() {
        given:
        createMocks()
        allowAccess()

        when:
        cloud20Service.addQuestion(uriInfo(), authToken, jaxbQuestion())

        then:
        1 * questionDao.addQuestion(_)
    }

    def "add question returns 200 on success and location header"() {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.addQuestion(uriInfo(), authToken, jaxbQuestion())

        then:
        Response response = responseBuilder.build()
        response.status == 201
        response.getMetadata().get("location").get(0) != null
    }

    // this is for release (1.0.12 or seomthing) migration related 12/03/2012
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

        when:
        def response = cloud20Service.addUser(headers, uriInfo(), authToken, user)

        then:
        response.build().status == 201
    }

    def "get question returns 200 and question"() {
        given:
        createMocks()
        allowAccess()

        questionDao.getQuestion(_) >> question()

        when:
        def responseBuilder = cloud20Service.getQuestion(authToken, questionId)

        then:
        Response response = responseBuilder.build()
        response.status == 200
        response.entity != null
    }

    def "get questions returns 200 and questions"() {
        given:
        createMocks()
        allowAccess()
        def questions = new ArrayList<Question>()

        questionDao.getQuestions() >> questions

        when:
        def responseBuilder = cloud20Service.getQuestions(authToken)

        then:
        Response response = responseBuilder.build()
        response.status == 200
        response.entity != null

    }

    def "update question calls ldap update"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getQuestion(_) >> question()

        when:
        cloud20Service.updateQuestion(authToken, questionId, jaxbQuestion())

        then:
        1 * questionDao.updateQuestion(_)
    }

    def "update question returns 204"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getQuestion(_) >> question()

        when:
        def responseBuilder = cloud20Service.updateQuestion(authToken, questionId, jaxbQuestion())

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "delete question calls ldap"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getQuestion(_) >> question()

        when:
        cloud20Service.deleteQuestion(authToken, questionId)

        then:
        1 * questionDao.deleteQuestion(_)
    }

    def "delete question returns 204"() {
        given:
        createMocks()
        allowAccess()
        questionDao.getQuestion(_) >> question()

        when:
        def responseBuilder = cloud20Service.deleteQuestion(authToken, questionId)

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "updateCapabilities return 204" (){
        given:
        createMocks()
        allowAccess()
        Capabilities capabilities = new Capabilities();
        capabilities.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null))
        List<com.rackspace.idm.domain.entity.Capability> capabilitiesDO = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getNextCapabilityId() >> "123321"
        capabilityDao.getObjects(_) >> capabilitiesDO

        when:
        def responseBuilder = cloud20Service.updateCapabilities(authToken,capabilities,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "updateCapabilities return 400" (){
        given:
        createMocks()
        allowAccess()
        Capabilities capabilities = new Capabilities();
        capabilities.capability.add(getCapability(null, "get_server", "get_server", "description", "http://someUrl", null))
        capabilityDao.getNextCapabilityId() >> "123321"
        List<com.rackspace.idm.domain.entity.Capability> capabilities2 = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getObjects(_) >> capabilities2
        when:
        def responseBuilder = cloud20Service.updateCapabilities(authToken,capabilities,"computeTest","1")
        def responseBuilder2 = cloud20Service.updateCapabilities(authToken,capabilities,null,"1")
        def responseBuilder3 = cloud20Service.updateCapabilities(authToken,null,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 400
        Response response2 = responseBuilder2.build()
        response2.status == 400
        Response response3 = responseBuilder3.build()
        response3.status == 400
    }

    def "updateCapabilities return 401" (){
        given:
        createMocks()
        Capabilities capabilities = new Capabilities();

        when:
        def responseBuilder = cloud20Service.updateCapabilities("badToken",capabilities,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 401
    }

    def "getCapabilities returns 200" () {
        given:
        createMocks()
        allowAccess()
        List<com.rackspace.idm.domain.entity.Capability> capabilities = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getObjects(_) >> capabilities
        when:
        def responseBuilder = cloud20Service.getCapabilities(authToken,"computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 200
    }

    def "getCapabilities returns 401" () {
        given:
        createMocks()

        when:
        def responseBuilder = cloud20Service.getCapabilities("badToken","computeTest","1")

        then:
        Response response = responseBuilder.build()
        response.status == 401
    }

    def "getCapabilities returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.getCapabilities(authToken, null , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "getCapabilities null version returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.getCapabilities(authToken, "computeTest" , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "deleteCapabilities returns 204" () {
        given:
        createMocks()
        allowAccess()
        List<com.rackspace.idm.domain.entity.Capability> capabilities = new ArrayList<com.rackspace.idm.domain.entity.Capability>();
        capabilityDao.getObjects(_) >> capabilities

        when:
        def responseBuilder = cloud20Service.removeCapabilities(authToken , "computeTest", "1")

        then:
        Response response = responseBuilder.build()
        response.status == 204
    }

    def "deleteCapabilities returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.removeCapabilities(authToken, null , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "deleteCapabilities with null version returns 400" () {
        given:
        createMocks()
        allowAccess()

        when:
        def responseBuilder = cloud20Service.removeCapabilities(authToken, "computeTest" , null)

        then:
        Response response = responseBuilder.build()
        response.status == 400
    }

    def "deleteCapabilities returns 401" () {
        given:
        createMocks()

        when:
        def responseBuilder = cloud20Service.removeCapabilities("badToken", "computeTest" , null)

        then:
        Response response = responseBuilder.build()
        response.status == 401
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
        def limit = cloud20Service.validateLimit(null)

        then:
        limit == configuration.getInt("ldap.paging.limit.default")
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
        def limit = cloud20Service.validateLimit("0")

        then:
        limit == configuration.getInt("ldap.paging.limit.default")
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
                clientRole("role", 750),
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
}
