package com.rackspace.idm.validation

import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import com.rackspace.idm.domain.dao.impl.LdapApplicationRoleRepository
import com.rackspace.idm.domain.dao.impl.LdapApplicationRepository
import com.rackspace.idm.domain.dao.impl.LdapTenantRepository
import com.rackspace.idm.domain.dao.impl.LdapTenantRoleRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.domain.service.impl.DefaultApplicationService
import com.rackspace.idm.domain.entity.Application

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/27/12
 * Time: 1:41 PM
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class PrecedenceValidatorTest extends Specification {

    @Autowired private PrecedenceValidator validator
    @Autowired private DefaultUserService userService
    @Autowired private DefaultApplicationService applicationService
    @Autowired private Configuration configuration

    @Shared LdapApplicationRoleRepository applicationRoleDao
    @Shared LdapApplicationRepository applicationDao
    @Shared LdapTenantRepository tenantDao
    @Shared LdapTenantRoleRepository tenantRoleDao
    @Shared LdapUserRepository userDao

    @Shared def defaultRole
    @Shared def userAdminRole
    @Shared def specialRole
    @Shared def adminRole
    @Shared def serviceAdminRole
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
    @Shared def random

    def setupSpec() {
        random = ("$randomness").replace('-', "")
    }

    def "verifyCallerPrecedence throws forbidden exception"() {
        given:
        createMocks()
        setRoles()

        def user = user("user")
        def tenantRole = tenantRole("tenantRole", "roleRsId")

        // getUserIdentityRole
        applicationDao.getClientByClientId(_) >> new Application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles
        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                defaultTenantRole, userAdminTenantRole, userAdminTenantRole, adminTenantRole,
                defaultTenantRole, userAdminTenantRole, userAdminTenantRole, adminTenantRole,
        ]

        // tenantRole -> applicationRole
        applicationRoleDao.getClientRole("role") >>> [
                userAdminRole, specialRole, userAdminRole, adminRole
        ]

        when:
        validator.verifyCallerRolePrecedence(user, userAdminRole)
        validator.verifyCallerRolePrecedence(user, specialRole)
        validator.verifyCallerRolePrecedence(user, adminRole)
        validator.verifyCallerRolePrecedence(user, serviceAdminRole)
        validator.verifyCallerRolePrecedence(user, tenantRole("tenantRole1", "role"))
        validator.verifyCallerRolePrecedence(user, tenantRole("tenantRole2", "role"))
        validator.verifyCallerRolePrecedence(user, tenantRole("tenantRole3", "role"))
        validator.verifyCallerRolePrecedence(user, tenantRole("tenantRole4", "role"))

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerPrecedence does not throw exception"() {
        given:
        createMocks()
        setRoles()

        def user1 = user("user")
        def tenantRole = tenantRole("tenantRole", "role")

        // getUserIdentityRole
        applicationDao.getClientByClientId(_) >> new Application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles
        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                userAdminTenantRole, userAdminTenantRole,
                adminTenantRole, adminTenantRole, adminTenantRole,
                serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole,
                userAdminTenantRole, userAdminTenantRole,
                adminTenantRole, adminTenantRole, adminTenantRole,
                serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole
        ]

        // tenantRole -> applicationRole
        applicationRoleDao.getClientRole(_) >>> [
                defaultRole, userAdminRole,
                defaultRole, userAdminRole, adminRole,
                defaultRole, userAdminRole, adminRole, serviceAdminRole
        ]

        when:
        validator.verifyCallerRolePrecedence(user1, defaultRole)
        validator.verifyCallerRolePrecedence(user1, userAdminRole)

        validator.verifyCallerRolePrecedence(user1, defaultRole)
        validator.verifyCallerRolePrecedence(user1, userAdminRole)
        validator.verifyCallerRolePrecedence(user1, adminRole)

        validator.verifyCallerRolePrecedence(user1, defaultRole)
        validator.verifyCallerRolePrecedence(user1, userAdminRole)
        validator.verifyCallerRolePrecedence(user1, adminRole)
        validator.verifyCallerRolePrecedence(user1, serviceAdminRole)

        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)
        validator.verifyCallerRolePrecedence(user1, tenantRole)

        then:
        notThrown(ForbiddenException)
    }

    def "verify caller precedence over user throws exception"() {
        given:
        createMocks()
        setRoles()

        // getUserIdentityRole
        applicationDao.getClientByClientId(_) >> new Application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles
        // user and caller will alternate (caller first)
        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                defaultTenantRole, userAdminTenantRole,
                defaultTenantRole, adminTenantRole,
                defaultTenantRole, serviceAdminTenantRole,
                userAdminTenantRole, adminTenantRole,
                userAdminTenantRole, serviceAdminTenantRole,
                adminTenantRole, serviceAdminTenantRole
        ]

        when:
        validator.verifyCallerPrecedenceOverUser(user(), user())
        validator.verifyCallerPrecedenceOverUser(user(), user())
        validator.verifyCallerPrecedenceOverUser(user(), user())
        validator.verifyCallerPrecedenceOverUser(user(), user())
        validator.verifyCallerPrecedenceOverUser(user(), user())
        validator.verifyCallerPrecedenceOverUser(user(), user())

        then:
        thrown(ForbiddenException)
    }

    def "verify caller precedence over user does not throw exception"() {
        given:
        createMocks()
        setRoles()

        def callingUser = user("calling")
        def calledUser = user("called")

        // getUserIdentityRole
        applicationDao.getClientByClientId(_) >> new Application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(callingUser, identityRoles) >>> [
                userAdminTenantRole, adminTenantRole, adminTenantRole,
                serviceAdminTenantRole, serviceAdminTenantRole, serviceAdminTenantRole
        ]

        tenantRoleDao.getTenantRoleForUser(calledUser, identityRoles) >>> [
                defaultTenantRole, defaultTenantRole, userAdminTenantRole,
                defaultTenantRole, userAdminTenantRole, adminTenantRole
        ]

        when:
        validator.verifyCallerPrecedenceOverUser(callingUser, calledUser)
        validator.verifyCallerPrecedenceOverUser(callingUser, calledUser)
        validator.verifyCallerPrecedenceOverUser(callingUser, calledUser)
        validator.verifyCallerPrecedenceOverUser(callingUser, calledUser)
        validator.verifyCallerPrecedenceOverUser(callingUser, calledUser)
        validator.verifyCallerPrecedenceOverUser(callingUser, calledUser)

        then:
        notThrown(ForbiddenException)
    }

    def "verify caller role precedence for assignment throws exception"() {
        given:
        createMocks()
        setRoles()

        def user1 = user("user")

        applicationDao.getClientByClientId(_) >> new Application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                userAdminTenantRole, userAdminTenantRole,
                adminTenantRole
        ]

        when:
        //validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:admin", 100, "1"))
        //validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:service-admin", 0, "2"))
        validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:service-admin", 0, "2"))

        then:
        thrown(ForbiddenException)

    }

    def "verify caller role precedence for assignment does not throw exception"() {
        given:
        createMocks()
        setRoles()

        def user1 = user("user")

        applicationDao.getClientByClientId(_) >> new Application()
        applicationRoleDao.getIdentityRoles(_, _) >> identityRoles

        tenantRoleDao.getTenantRoleForUser(_, _) >>> [
                userAdminTenantRole, userAdminTenantRole, userAdminTenantRole,
                adminTenantRole, adminTenantRole,
                serviceAdminTenantRole
        ]

        when:
        validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:default", 2000, "1"))
        validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:special", 1000, "3"))
        validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:admin", 1000, "3"))
        validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:special", 1000, "3"))
        validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:admin", 1000, "3"))
        validator.verifyCallerRolePrecendenceForAssignment(user1, clientRole("identity:service-admin", 0, "4"))

        then:
        notThrown(ForbiddenException)
    }

    def createMocks() {
        applicationDao = Mock()
        applicationRoleDao = Mock()
        tenantDao = Mock()
        tenantRoleDao = Mock()
        userDao = Mock()

        userService.userDao = userDao
        userService.tenantRoleDao = tenantRoleDao
        userService.applicationRoleDao = applicationRoleDao

        applicationService.applicationDao = applicationDao
        applicationService.userDao = userDao
        applicationService.applicationRoleDao = applicationRoleDao
        applicationService.tenantDao = tenantDao
        applicationService.tenantRoleDao = tenantRoleDao
    }

    def setRoles() {
        defaultRole = clientRole("identity:default", configuration.getInt("cloudAuth.defaultUser.rsWeight"), defaultRoleId)
        userAdminRole = clientRole("identity:user-admin", configuration.getInt("cloudAuth.userAdmin.rsWeight"), userAdminRoleId)
        specialRole = clientRole("specialRole", configuration.getInt("cloudAuth.special.rsWeight"), "")
        adminRole = clientRole("identity:admin", configuration.getInt("cloudAuth.admin.rsWeight"), adminRoleId)
        serviceAdminRole = clientRole("identity:service-admin", configuration.getInt("cloudAuth.serviceAdmin.rsWeight"), serviceAdminRoleId)

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

    def clientRole(name, rsWeight, id) {
        new ClientRole().with {
            it.id = id
            it.name = name
            it.rsWeight = rsWeight
            return it
        }
    }
    def tenantRole(name, roleRsId) {
        new TenantRole().with {
            it.name = name
            it.roleRsId = roleRsId
            return it
        }
    }

    def user(name) {
        new User().with {
            it.username = name
            return it
        }
    }
}
