package com.rackspace.idm.api.resource.roles

import spock.lang.Specification
import spock.lang.Shared
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.api.converter.RolesConverter
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration
import com.rackspace.idm.validation.PrecedenceValidator
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.api.resource.pagination.Paginator
import com.rackspace.idm.domain.service.impl.DefaultApplicationService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.rackspace.idm.validation.InputValidator
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.dao.impl.LdapApplicationRepository
import com.rackspace.idm.domain.dao.impl.LdapTenantRepository
import com.rackspace.idm.domain.dao.impl.LdapApplicationRoleRepository
import com.rackspace.idm.domain.dao.impl.LdapTenantRoleRepository
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.api.idm.v1.Role
import com.rackspace.idm.domain.entity.Application
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.entity.ScopeAccess
import com.unboundid.ldap.sdk.Attribute
import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.ForbiddenException

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/28/12
 * Time: 11:15 PM
 * To change this template use File | Settings | File Templaotes.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class RolesResourceGroovyTest extends Specification {

    @Autowired PrecedenceValidator valdiator
    @Autowired Paginator<ClientRole> paginator
    @Autowired Configuration config
    @Autowired DefaultUserService userService
    @Autowired DefaultApplicationService applicationService

    @Shared RolesResource rolesResource
    @Shared AuthorizationService authorizationService
    @Shared RolesConverter rolesConverter
    @Shared InputValidator inputValidator

    @Shared LdapUserRepository userDao
    @Shared LdapApplicationRepository clientDao
    @Shared LdapTenantRepository tenantDao
    @Shared LdapApplicationRoleRepository clientRoleDao
    @Shared LdapTenantRoleRepository tenantRoleDao
    @Shared ScopeAccessService scopeAccessService

    @Shared def adminRole
    @Shared def userAdminRole
    @Shared def serviceAdminRole
    @Shared def defaultRole

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

    def setup() {
        sharedRandom = ("$randomness").replace("-", "")
        rolesConverter = Mock(RolesConverter)
        authorizationService = Mock(DefaultAuthorizationService)
        inputValidator = new InputValidator()

        rolesResource = new RolesResource(rolesConverter, authorizationService, applicationService, inputValidator)
        rolesResource.setConfig(config)
        rolesResource.setUserService(userService)
        rolesResource.setPrecedenceValidator(valdiator)
    }

    def "addRole sets rsWeight"() {
        given:
        createMocks()

        def role = createRole("name", "appId")
        clientRoleDao.getClientRole(_) >> role
        clientDao.getClientByClientId(_)  >> application()
        clientRoleDao.getNextRoleId()  >> "1234"

        rolesConverter.toClientRole(_) >> createClientRole("role", "id")

        when:
        rolesResource.addRole(authToken, role)

        then:
        clientRoleDao.getClientRoleByApplicationAndName(_, _) >> { arg1, arg2 ->
             assert(arg2.getRsWeight() == config.getInt("cloudAuth.special.rsWeight"))
        }
    }

    def "update Role returns forbidden"() {
        given:
        createMocks()
        setupRoles()

        def role = createRole("name", "id")
        clientDao.getClientByClientId(_)  >> application()
        rolesConverter.toClientRole(_) >> clientRole("role2", 500, "id")
        clientRoleDao.getClientRole(_) >> clientRole("role1", 500, "id")

        scopeAccessService.getScopeAccessByAccessToken(_) >> Mock(ScopeAccess).with() {
            def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
            def entry = new ReadOnlyEntry("DN", attribute)
            it.getLDAPEntry() >> entry
            return it
        }
        def caller = createUser("caller")
        userDao.getUserByUsername(_) >> caller

        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                defaultTenantRole, userAdminTenantRole
        ]

        when:
        rolesResource.updateRole(authToken, sharedRandom, role)
        rolesResource.updateRole(authToken, sharedRandom, role)

        then:
        thrown(ForbiddenException)
    }

    def "updateRole succeeds"() {
        given:
        createMocks()
        setupRoles()

        def role = createRole("name", "id")
        clientDao.getClientByClientId(_)  >> application()
        rolesConverter.toClientRole(_) >> clientRole("role2", 2000, "id")
        clientRoleDao.getClientRole(_) >> clientRole("role1", 2000, "id")

        scopeAccessService.getScopeAccessByAccessToken(_) >> Mock(ScopeAccess).with() {
            def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
            def entry = new ReadOnlyEntry("DN", attribute)
            it.getLDAPEntry() >> entry
            return it
        }
        def caller = createUser("caller")
        userDao.getUserByUsername(_) >> caller

        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole
        ]

        when:
        def statuses = []
        statuses.add(rolesResource.updateRole(authToken, sharedRandom, role).status)
        statuses.add(rolesResource.updateRole(authToken, sharedRandom, role).status)
        statuses.add(rolesResource.updateRole(authToken, sharedRandom, role).status)

        then:
        for (status in statuses) {
            assert(status == 204)
        }
    }

    def "delete role throws forbidden exception"() {
        given:
        createMocks()
        setupRoles()

        def role = createRole("name", "id")
        clientDao.getClientByClientId(_)  >> application()
        rolesConverter.toClientRole(_) >> clientRole("role2", 500, "id")
        clientRoleDao.getClientRole(_) >> clientRole("role1", 500, "id")

        scopeAccessService.getScopeAccessByAccessToken(_) >> Mock(ScopeAccess).with() {
            def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
            def entry = new ReadOnlyEntry("DN", attribute)
            it.getLDAPEntry() >> entry
            return it
        }
        def caller = createUser("caller")
        userDao.getUserByUsername(_) >> caller

        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                defaultTenantRole, userAdminTenantRole
        ]

        when:
        rolesResource.deleteRole(authToken, sharedRandom)
        rolesResource.deleteRole(authToken, sharedRandom)

        then:
        thrown(ForbiddenException)
    }

    def "delete role succeeds"() {
        given:
        createMocks()
        setupRoles()

        def role = createRole("name", "id")
        clientDao.getClientByClientId(_)  >> application()
        clientRoleDao.getClientRole(_) >> clientRole("role1", 2000, "id")

        scopeAccessService.getScopeAccessByAccessToken(_) >> Mock(ScopeAccess).with() {
            def attribute = new Attribute(LdapRepository.ATTR_UID, "username")
            def entry = new ReadOnlyEntry("DN", attribute)
            it.getLDAPEntry() >> entry
            return it
        }
        def caller = createUser("caller")
        userDao.getUserByUsername(_) >> caller

        clientRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                userAdminTenantRole, adminTenantRole, serviceAdminTenantRole
        ]

        tenantDao.getAllTenantRolesForClientRole(_) >> new ArrayList<TenantRole>()

        when:
        def statuses = []
        statuses.add(rolesResource.deleteRole(authToken, sharedRandom).status)
        statuses.add(rolesResource.deleteRole(authToken, sharedRandom).status)
        statuses.add(rolesResource.deleteRole(authToken, sharedRandom).status)

        then:
        for (status in statuses) {
            assert(status == 204)
        }
    }

    def createMocks() {
        userDao = Mock()
        clientDao = Mock()
        tenantDao = Mock()
        clientRoleDao = Mock()
        tenantRoleDao = Mock()
        scopeAccessService = Mock()

        rolesResource.scopeAccessService = scopeAccessService

        userService.userDao = userDao
        userService.applicationRoleDao = clientRoleDao
        userService.tenantRoleDao = tenantRoleDao
        userService.scopeAccessService = scopeAccessService

        applicationService.applicationRoleDao = clientRoleDao
        applicationService.applicationDao = clientDao
        applicationService.tenantRoleDao = tenantRoleDao
        applicationService.tenantDao = tenantDao
    }

    def setupRoles() {
        defaultRole = clientRole("identity:default", config.getInt("cloudAuth.defaultUser.rsWeight"), defaultRoleId)
        userAdminRole = clientRole("identity:user-admin", config.getInt("cloudAuth.userAdmin.rsWeight"), userAdminRoleId)
        adminRole = clientRole("identity:admin", config.getInt("cloudAuth.admin.rsWeight"), adminRoleId)
        serviceAdminRole = clientRole("identity:service-admin", config.getInt("cloudAuth.serviceAdmin.rsWeight"), serviceAdminRoleId)

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

    def createRole(name, applicationId) {
        new Role().with {
            it.name = name
            it.applicationId = applicationId
            return it
        }
    }

    def createClientRole(name, id) {
        new ClientRole().with {
            it.name = name
            it.id = id
            return it
        }
    }

    def application() {
        new Application().with {
            return it
        }
    }

    def createUser(name) {
        new User().with {
            it.username = name
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

    def clientRole(String name, int rsWeight, String id) {
        new ClientRole().with {
            it.clientId = "1234"
            it.id = id
            it.name = name
            it.rsWeight = rsWeight
            return it
        }
    }
}

