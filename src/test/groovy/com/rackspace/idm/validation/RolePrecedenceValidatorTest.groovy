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

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/27/12
 * Time: 1:41 PM
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class RolePrecedenceValidatorTest extends Specification {

    @Autowired private RolePrecedenceValidator validator
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

    @Shared def randomness = UUID.randomUUID()
    @Shared def random

    def setupSpec() {
        random = ("$randomness").replace('-', "")
    }

    def "verifyCallerPrecedence throws forbidden exception"() {
        given:
        createMocks()
        setRoles()

        def user = user()
        def tenantRole = tenantRole("tenantRole", "roleRsId")

        // getUserWeight
        tenantRoleDao.getTenantRolesForUser(_, _) >> [ tenantRole ].asList()
        applicationRoleDao.getClientRole("roleRsId") >>> [
                defaultRole, userAdminRole, userAdminRole, adminRole,
                defaultRole, userAdminRole, userAdminRole, adminRole
        ]

        // tenantRole -> applicationRole
        applicationRoleDao.getClientRole("role") >>> [
                userAdminRole, specialRole, userAdminRole, adminRole
        ]

        when:
        validator.verifyCallerPrecedence(user, userAdminRole)
        validator.verifyCallerPrecedence(user, specialRole)
        validator.verifyCallerPrecedence(user, adminRole)
        validator.verifyCallerPrecedence(user, serviceAdminRole)
        validator.verifyCallerPrecedence(user, tenantRole("tenantRole1", "role"))
        validator.verifyCallerPrecedence(user, tenantRole("tenantRole2", "role"))
        validator.verifyCallerPrecedence(user, tenantRole("tenantRole3", "role"))
        validator.verifyCallerPrecedence(user, tenantRole("tenantRole4", "role"))

        then:
        thrown(ForbiddenException)
    }

    def "verifyCallerPrecedence does not throw exception"() {
        given:
        createMocks()
        setRoles()

        def user1 = user()
        def tenantRole1 = tenantRole("tenantRole", "roleRsId")
        def tenantRole2 = tenantRole("tenantRole2", "role")

        // getUserWeight
        tenantRoleDao.getTenantRolesForUser(_, _) >> [ tenantRole1 ].asList()
        applicationRoleDao.getClientRole("roleRsId") >>> [
                userAdminRole, userAdminRole,
                adminRole, adminRole, adminRole,
                serviceAdminRole, serviceAdminRole, serviceAdminRole, serviceAdminRole,
                userAdminRole, userAdminRole,
                adminRole, adminRole, adminRole,
                serviceAdminRole, serviceAdminRole, serviceAdminRole, serviceAdminRole
        ]

        // tenantRole -> applicationRole
        applicationRoleDao.getClientRole("role") >>> [
                defaultRole, userAdminRole,
                defaultRole, userAdminRole, adminRole,
                defaultRole, userAdminRole, adminRole, serviceAdminRole
        ]

        when:
        validator.verifyCallerPrecedence(user1, defaultRole)
        validator.verifyCallerPrecedence(user1, userAdminRole)

        validator.verifyCallerPrecedence(user1, defaultRole)
        validator.verifyCallerPrecedence(user1, userAdminRole)
        validator.verifyCallerPrecedence(user1, adminRole)

        validator.verifyCallerPrecedence(user1, defaultRole)
        validator.verifyCallerPrecedence(user1, userAdminRole)
        validator.verifyCallerPrecedence(user1, adminRole)
        validator.verifyCallerPrecedence(user1, serviceAdminRole)

        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)
        validator.verifyCallerPrecedence(user1, tenantRole2)

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
    }

    def setRoles() {
        defaultRole = clientRole("identity:default", configuration.getInt("cloudAuth.defaultUser.rsWeight"))
        userAdminRole = clientRole("identity:user-admin", configuration.getInt("cloudAuth.userAdmin.rsWeight"))
        specialRole = clientRole("specialRole", configuration.getInt("cloudAuth.special.rsWeight"))
        adminRole = clientRole("identity:admin", configuration.getInt("cloudAuth.admin.rsWeight"))
        serviceAdminRole = clientRole("identity:service-admin", configuration.getInt("cloudAuth.serviceAdmin.rsWeight"))
    }

    def clientRole(name, rsWeight) {
        new ClientRole().with {
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

    def user() {
        new User().with {
            return it
        }
    }
}
