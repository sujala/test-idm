package com.rackspace.idm.api.resource.user

import spock.lang.Specification
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration
import com.rackspace.idm.validation.PrecedenceValidator
import spock.lang.Shared
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultApplicationService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.rackspace.idm.domain.service.impl.DefaultTenantService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.dao.impl.LdapTenantRepository
import com.rackspace.idm.domain.dao.impl.LdapTenantRoleRepository
import com.rackspace.idm.domain.dao.impl.LdapApplicationRepository
import com.rackspace.idm.domain.dao.impl.LdapApplicationRoleRepository
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.domain.entity.ScopeAccess
import com.unboundid.ldap.sdk.Attribute
import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.exception.NotFoundException

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/28/12
 * Time: 7:02 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class UserGlobalRoleResourceGroovyTest extends Specification {

    @Autowired Configuration config
    @Autowired PrecedenceValidator precedenceValidator
    @Autowired UserGlobalRoleResource globalRoleResource
    @Autowired DefaultApplicationService applicationService
    @Autowired DefaultUserService userService
    @Autowired DefaultTenantService tenantService

    @Shared def DefaultScopeAccessService scopeAccessService
    @Shared def DefaultAuthorizationService authorizationService
    @Shared def LdapUserRepository userDao
    @Shared def LdapTenantRepository tenantDao
    @Shared def LdapTenantRoleRepository tenantRoleDao
    @Shared def LdapApplicationRepository applicationDao
    @Shared def LdapApplicationRoleRepository applicationRoleDao

    @Shared def adminRole
    @Shared def userAdminRole
    @Shared def serviceAdminRole
    @Shared def genericRole
    @Shared def defaultRole
    @Shared def specialRole

    @Shared def defaultTenantRole
    @Shared def userAdminTenantRole
    @Shared def adminTenantRole
    @Shared def serviceAdminTenantRole

    @Shared def List<ClientRole> identityRoles

    @Shared def serviceAdminRoleId = "0"
    @Shared def adminRoleId = "1"
    @Shared def userAdminRoleId = "2"
    @Shared def defaultRoleId = "3"

    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def authToken = "authToken"
    @Shared def caller
    @Shared def called

    def setupSpec() {
        sharedRandom = ("$randomness").replace("-", "")
        caller = createUser("caller", "1$sharedRandom")
        called = createUser("called", "2$sharedRandom")
    }

    def "granting a globalRole to a user throws forbidden exception"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        applicationRoleDao.getClientRole(_) >> clientRole("role", 1000, "roleId")
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole("role", 1000, "roleId2")
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                defaultTenantRole, defaultTenantRole, defaultTenantRole,
                userAdminTenantRole, userAdminTenantRole,
                adminTenantRole,
                defaultTenantRole,
                userAdminTenantRole, userAdminTenantRole, userAdminTenantRole,
                adminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole,
                adminTenantRole, serviceAdminTenantRole,
                serviceAdminTenantRole
        ]

        applicationRoleDao.getClientRole(_) >> [
                userAdminRole,
                specialRole, adminRole, serviceAdminRole,
                serviceAdminRole
        ]

        when:
        globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom)

        then:
        thrown(ForbiddenException)
    }

    def "grant user global role throws bad request (identityRole exists)"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        applicationRoleDao.getClientRole(_) >> clientRole("identity:role", 1000, "roleId")
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole("role", 1000, "roleId2")
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                adminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >>> [
                userAdminTenantRole
        ]

        applicationRoleDao.getClientRole(_) >> [
                userAdminRole, // from precedenceValidator
                userAdminRole  // checking to see if user has identity role already
        ]

        when:
        globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom)

        then:
        thrown(BadRequestException)
    }

    def "grant user global role adds role successfully"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        applicationRoleDao.getClientRole(_) >> clientRole("role", 1000, "roleId")
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole("role", 1000, "roleId2")
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >>> [
                defaultTenantRole, userAdminTenantRole, adminTenantRole
        ]

        applicationRoleDao.getClientRole(_) >> [
                defaultRole, genericRole, adminRole
        ]

        when:
        def statuses = []
        statuses.add(globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom).status)
        statuses.add(globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom).status)
        statuses.add(globalRoleResource.grantGlobalRoleToUser(authToken, sharedRandom, sharedRandom).status)

        then:
        for (status in statuses) {
            assert(status == 204)
        }
    }

    def "delete global role from user throws bad request"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        scopeAccessService.getScopeAccessByAccessToken(_) >> new UserScopeAccess()
        applicationRoleDao.getClientRole(_) >> userAdminRole
        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >> adminTenantRole
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >> tenantRole("identity:role", "roleRsId")
        userDao.getUserById(_) >> caller
        userDao.getUserByUsername(_) >> caller

        when:
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)

        then:
        thrown(BadRequestException)
    }

    def "delete global role from user throws forbidden exception"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        applicationRoleDao.getClientRole(_) >> clientRole("role", 1000, "roleId")
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole("role", 1000, "roleId2")
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(called, sharedRandom) >> new TenantRole()

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                defaultTenantRole, defaultTenantRole, defaultTenantRole,
                userAdminTenantRole, userAdminTenantRole,
                adminTenantRole,
                defaultTenantRole,
                userAdminTenantRole, userAdminTenantRole, userAdminTenantRole,
                adminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole,
                adminTenantRole, serviceAdminTenantRole,
                serviceAdminTenantRole
        ]

        applicationRoleDao.getClientRole(_) >> [
                userAdminRole,
                specialRole, adminRole, serviceAdminRole,
                serviceAdminRole
        ]

        when:
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)

        then:
        thrown(ForbiddenException)
    }

    def "delete global role from user throws not found exception"() {
        given:
        createMocks()
        allowAccess()

        userDao.getUserById(_) >> called
        tenantRoleDao.getTenantRoleForUser(called, sharedRandom) >> null

        when:
        globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom)

        then:
        thrown(NotFoundException)

    }

    def "delete global role from user deletes role successfully"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        applicationRoleDao.getClientRole(_) >> clientRole("role", 1000, "roleId")
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole("role", 1000, "roleId2")
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(called, sharedRandom) >> tenantRole("role", "roleRsId")

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >>> [
                defaultTenantRole, userAdminTenantRole, adminTenantRole
        ]

        applicationRoleDao.getClientRole(_) >> [
                defaultRole, genericRole, adminRole
        ]

        when:
        def statuses = []
        statuses.add(globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom).status)
        statuses.add(globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom).status)
        statuses.add(globalRoleResource.deleteGlobalRoleFromUser(authToken, sharedRandom, sharedRandom).status)

        then:
        for (status in statuses) {
            assert(status == 204)
        }
    }

    def "grant tenant role to User throws forbidden exception"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        applicationRoleDao.getClientRole(_) >> clientRole("role", 1000, "roleId")
        tenantDao.getTenant(sharedRandom) >> createTenant(sharedRandom)
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                defaultTenantRole, userAdminTenantRole, adminTenantRole,
                userAdminTenantRole, userAdminTenantRole, userAdminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole,
                defaultTenantRole, defaultTenantRole, defaultTenantRole
        ]

        applicationRoleDao.getClientRole(_) >>> [
                new ClientRole(), new ClientRole(), new ClientRole(),
                new ClientRole(), adminRole,
                new ClientRole(), specialRole,
                new ClientRole(), serviceAdminRole
        ]

        when:
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)

        then:
        thrown(ForbiddenException)
    }

    def "grant global role to user throws bad request (identity role on tenant)"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        tenantDao.getTenant(sharedRandom) >> null >> createTenant(sharedRandom)
        applicationRoleDao.getClientRole(sharedRandom) >>> [
                defaultRole, userAdminRole, adminRole, serviceAdminRole
        ]
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >> userAdminTenantRole

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >> defaultTenantRole

        applicationRoleDao.getClientRole(_) >> null >> defaultRole

        when:
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)
        globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom)

        then:
        thrown(BadRequestException)
    }

    def "grant global role to user on tenant adds role on tenant"() {
        given:
        createMocks()
        setupRoles()
        allowAccess()

        tenantDao.getTenant(sharedRandom) >> createTenant(sharedRandom)
        applicationRoleDao.getClientRole(sharedRandom) >> clientRole("role", 1000, "roleId")
        userDao.getUserById(_) >> called
        userDao.getUserByUsername(_) >> caller

        applicationDao.getClientByClientId(_) >> application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(caller, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(called, identityRoles) >>> [
                defaultTenantRole, userAdminTenantRole, adminTenantRole, serviceAdminTenantRole
        ]

        applicationRoleDao.getClientRole(_) >> defaultRole

        applicationDao.getClientRoleByClientIdAndRoleName(_, _) >> clientRole("role", 1000, "role")

        when:
        def statuses = []
        statuses.add(globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom).status)
        statuses.add(globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom).status)
        statuses.add(globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom).status)
        statuses.add(globalRoleResource.grantTenantRoleToUser(authToken, sharedRandom, sharedRandom, sharedRandom).status)

        then:
        for (status in statuses) {
            assert(status == 204)
        }
    }

    def "createTenantTole throws bad request"() {
        given:
        createMocks()
        applicationRoleDao.getClientRole(_) >> null

        when:
        globalRoleResource.checkAndGetTenantRole("tenantId", "roleId")

        then:
        thrown(BadRequestException)
    }

    def "createTenantRole returns tenantRole"() {
        given:
        createMocks()
        applicationRoleDao.getClientRole(_) >> new ClientRole().with() {
            it.clientId = "clientId"
            it.id = "id"
            it.name = "name"
            return it
        }

        when:
        def tenantRole = globalRoleResource.checkAndGetTenantRole("tenantId", "roleId")

        then:
        tenantRole.clientId == "clientId"
        tenantRole.roleRsId == "id"
        tenantRole.tenantIds == ["tenantId"]
    }

    def createMocks() {
        userDao = Mock()
        tenantDao = Mock()
        tenantRoleDao = Mock()
        applicationDao = Mock()
        applicationRoleDao = Mock()
        authorizationService = Mock()
        scopeAccessService = Mock()

        userService.userDao = userDao
        userService.tenantRoleDao = tenantRoleDao
        userService.applicationRoleDao = applicationRoleDao
        userService.scopeAccessService = scopeAccessService

        tenantService.tenantDao = tenantDao
        tenantService.tenantRoleDao = tenantRoleDao
        tenantService.clientDao = applicationDao

        authorizationService.tenantDao = tenantDao
        authorizationService.applicationDao = applicationDao

        scopeAccessService.tenantDao = tenantDao
        scopeAccessService.applicationDao = applicationDao

        applicationService.applicationDao = applicationDao
        applicationService.applicationRoleDao = applicationRoleDao
        applicationService.tenantDao = tenantDao
        applicationService.tenantRoleDao = tenantRoleDao

        globalRoleResource.setAuthorizationService(authorizationService)
        globalRoleResource.setScopeAccessService(scopeAccessService)
        globalRoleResource.setTenantService(tenantService)
    }

    def setupRoles() {
        defaultRole = clientRole("identity:default", config.getInt("cloudAuth.defaultUser.rsWeight"), defaultRoleId)
        userAdminRole = clientRole("identity:user-admin", config.getInt("cloudAuth.userAdmin.rsWeight"), userAdminRoleId)
        adminRole = clientRole("identity:admin", config.getInt("cloudAuth.admin.rsWeight"), adminRoleId)
        serviceAdminRole = clientRole("identity:service-admin", config.getInt("cloudAuth.serviceAdmin.rsWeight"), serviceAdminRoleId)
        specialRole = clientRole("specialRole", config.getInt("cloudAuth.special.rsWeight"), "")
        genericRole = clientRole("genericRole", 500, "roleId")

        defaultTenantRole = tenantRole("identity:default", defaultRoleId)
        userAdminTenantRole = tenantRole("identity:user-admin", userAdminRoleId)
        adminTenantRole = tenantRole("identity:admin", adminRoleId)
        serviceAdminTenantRole = tenantRole("identity:service-admin", serviceAdminRoleId)

        identityRoles = new ArrayList<ClientRole>()
        identityRoles.add(defaultRole);
        identityRoles.add(userAdminRole);
        identityRoles.add(adminRole);
        identityRoles.add(serviceAdminRole);
    }

    def allowAccess() {
        scopeAccessService.getScopeAccessByAccessToken(_) >> Mock(ScopeAccess).with() {
            def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
            def entry = new ReadOnlyEntry("DN", attribute)
            it.getLDAPEntry() >> entry
            return it
        }
    }

    def clientRole(String name, int rsWeight, String id) {
        new ClientRole().with {
            it.clientId = "1234"
            it.id = id
            it.name = name
            it.rsWeight = rsWeight
            return it
        }
    }

    def createUser(name, id) {
        new User().with {
            it.username = name
            it.id = id
            it.uniqueId = "this is my uniqueId"
            return it
        }
    }

    def application() {
        new Application().with {
            return it
        }
    }

    def tenantRole(String name, String roleId) {
        new TenantRole().with {
            it.name = name
            it.roleRsId = roleId
            return it
        }
    }

    def createTenant(id) {
        new Tenant().with {
            it.tenantId = id
            it.name = "tenantRole"
            return it
        }
    }
}
