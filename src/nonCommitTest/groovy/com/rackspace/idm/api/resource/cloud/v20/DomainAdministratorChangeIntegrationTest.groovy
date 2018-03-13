package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainAdministratorChange
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantService
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_OK

class DomainAdministratorChangeIntegrationTest extends RootIntegrationTest {

    @Shared
    String serviceAdminToken

    @Shared
    String identityAdminToken

    @Autowired
    TenantService tenantService

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    UserDao userDao

    @Autowired
    DomainDao domainDao

    void doSetupSpec() {
        def response = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert (response.status == SC_OK)
        serviceAdminToken = response.getEntity(AuthenticateResponse).value.token.id

        response = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert (response.status == SC_OK)
        identityAdminToken = response.getEntity(AuthenticateResponse).value.token.id
    }

    def "Caller must have appropriate role"() {
        given:
        def idAdmin = utils.createIdentityAdmin()
        def idAdminToken = utils.getToken(idAdmin.username)

        // Create invalid request. Will fail for authorized users, but will fail differently than for unauthorized
        def promoteAdminChange = new DomainAdministratorChange()

        when: "Try to change admins w/ non-priv'd identity-admin"
        def response = cloud20.changeDomainAdministrator(idAdminToken, "domainx", promoteAdminChange)

        then: "denied"
        response.status == HttpStatus.SC_FORBIDDEN

        when: "Try to change admins w/ non-priv'd service-admin"
        def response2 = cloud20.changeDomainAdministrator(serviceAdminToken, "domainx", promoteAdminChange)

        then: "denied"
        response2.status == HttpStatus.SC_FORBIDDEN

        when: "assign user-admin appropriate role"
        utils.addRoleToUser(idAdmin, Constants.IDENTITY_CHANGE_DOMAIN_ADMIN_ROLE_ID, serviceAdminToken)
        def response3 = cloud20.changeDomainAdministrator(idAdminToken, "domainx", promoteAdminChange)

        then: "now fails with 404"
        response3.status == HttpStatus.SC_BAD_REQUEST
    }

    @Unroll
    def "Can upgrade user-manager and default users: request: #request; accept: #accept"() {
        given:
        def userAdmin = utils.createCloudAccount(identityAdminToken)
        def domainId = userAdmin.domainId
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManage = utils.createUser(userAdminToken, testUtils.getRandomUUID("userManage"), domainId)
        utils.addRoleToUser(userManage, Constants.USER_MANAGE_ROLE_ID)
        def userManageToken = utils.getToken(userManage.username)

        def userDefault = utils.createUser(userAdminToken, testUtils.getRandomUUID("userDefault"), domainId)
        def userDefaultToken = utils.getToken(userDefault.username)

        when: "upgrade user-manager"
        def promoteUserManagerChange = createAdminChange(userManage.id, userAdmin.id)
        def userManagePromoteResponse = cloud20.changeDomainAdministrator(identityAdminToken, domainId, promoteUserManagerChange, request, accept)

        then: "allowed"
        userManagePromoteResponse.status == HttpStatus.SC_NO_CONTENT

        when: "validate token for new admin includes appropriate user type roles"
        AuthenticateResponse valResponse = utils.validateToken(userManageToken)

        then:
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) != null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) == null

        when: "validate token for old admin includes appropriate user type roles"
        AuthenticateResponse valResponse2 = utils.validateToken(userAdminToken)

        then:
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) != null

        when: "upgrade default user"
        def promoteUserDefaultChange = createAdminChange(userDefault.id, userManage.id)
        def userDefaultPromoteResponse = cloud20.changeDomainAdministrator(identityAdminToken, domainId, promoteUserDefaultChange, request, accept)

        then: "allowed"
        userDefaultPromoteResponse.status == HttpStatus.SC_NO_CONTENT

        when: "validate token for new admin includes appropriate user type roles"
        AuthenticateResponse valResponse3 = utils.validateToken(userDefaultToken)

        then:
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) != null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) == null

        when: "validate token for old admin includes appropriate user type roles"
        AuthenticateResponse valResponse4 = utils.validateToken(userManageToken)

        then:
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_ADMIN.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.USER_MANAGER.roleName)}) == null
        valResponse2.user.roles.role.find({it.name.equalsIgnoreCase(IdentityUserTypeEnum.DEFAULT_USER.roleName)}) != null

        where:
        request | accept
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "user groups are not modified during domain admin change, userAssignment = #userAssignment, groupAssignment = #groupAssignment"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        def userAdmin = utils.createCloudAccount(identityAdminToken)
        def domainId = userAdmin.domainId
        def userAdminToken = utils.getToken(userAdmin.username)
        def userDefault = utils.createUser(userAdminToken, testUtils.getRandomUUID("userDefault"), domainId)
        def userGroup = utils.createUserGroup(domainId)
        def role = utils.createRole(null, RandomStringUtils.randomAlphabetic(8), IdentityUserTypeEnum.USER_MANAGER.roleName)
        def tenant = utils.createTenant(testUtils.getRandomUUID("tenant"), true, testUtils.getRandomUUID("tenant"), domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.addUserToUserGroup(userDefault.id, userGroup)

        if (TenantAssignmentType.GLOBAL == userAssignment) {
            utils.addRoleToUser(userDefault, role.id)
        } else {
            utils.addRoleToUserOnTenant(userDefault, tenant, role.id)
        }

        if (TenantAssignmentType.GLOBAL == groupAssignment) {
            utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(role.id, ['*']))
        } else {
            utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(role.id, [tenant.id]))
        }

        when:
        def adminChange = createAdminChange(userDefault.id, userAdmin.id)
        def adminChangeResp = cloud20.changeDomainAdministrator(identityAdminToken, domainId, adminChange)

        then:
        adminChangeResp.status == HttpStatus.SC_NO_CONTENT
        if (TenantAssignmentType.GLOBAL == groupAssignment) {
            assertGlobalRoleOnUserAfterAdminChange(userDefault, role, userGroup)
            assertGlobalRoleOnUserAfterAdminChange(userAdmin, role, userGroup)
        } else {
            assertTenantedRoleOnUserAfterAdminChange(userDefault, tenant, role, userGroup)
            assertTenantedRoleOnUserAfterAdminChange(userAdmin, tenant, role, userGroup)
        }

        cleanup:
        utils.deleteUserQuietly(userDefault)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteUserGroup(userGroup)
        utils.deleteRole(role)
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()

        where:
        [userAssignment, groupAssignment] << [TenantAssignmentType.values(), TenantAssignmentType.values()].combinations()
    }

    def "user group assignments are not modified during user admin change when user groups have one or more tenant role assignments"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        def userAdmin = utils.createCloudAccount(identityAdminToken)
        def domainId = userAdmin.domainId
        def userAdminToken = utils.getToken(userAdmin.username)
        def userDefault = utils.createUser(userAdminToken, testUtils.getRandomUUID("userDefault"), domainId)
        def userGroup = utils.createUserGroup(domainId)
        def role = utils.createRole(null, RandomStringUtils.randomAlphabetic(8), IdentityUserTypeEnum.USER_MANAGER.roleName)
        def tenant1 = utils.createTenant(testUtils.getRandomUUID("tenant"), true, testUtils.getRandomUUID("tenant"), domainId)
        def tenant2 = utils.createTenant(testUtils.getRandomUUID("tenant"), true, testUtils.getRandomUUID("tenant"), domainId)
        def tenant3 = utils.createTenant(testUtils.getRandomUUID("tenant"), true, testUtils.getRandomUUID("tenant"), domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        utils.addUserToUserGroup(userDefault.id, userGroup)

        when: "promote user when user has a role on tenant3 and group assignment with role on tenant1"
        utils.addRoleToUserOnTenant(userDefault, tenant3, role.id)
        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(role.id, [tenant1.id]))
        def adminChange = createAdminChange(userDefault.id, userAdmin.id)
        def adminChangeResp = cloud20.changeDomainAdministrator(identityAdminToken, domainId, adminChange)

        then:
        adminChangeResp.status == HttpStatus.SC_NO_CONTENT
        assertTenantedRoleOnUserAfterAdminChange(userDefault, tenant1, role, userGroup)
        assertTenantedRoleOnUserAfterAdminChange(userAdmin, tenant1, role, userGroup)
        assertUserDoesNotHaveRoleOnTenant(userDefault, role, tenant3)

        when: "promote user when user has a role on tenant3 and group assignment with role on tenant1 and tenant2"
        // Switch the users so the var names match what they are
        def temp = userDefault
        userDefault = userAdmin
        userAdmin = temp
        utils.addRoleToUserOnTenant(userDefault, tenant3, role.id)
        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(role.id, [tenant1.id, tenant2.id]))
        adminChange = createAdminChange(userDefault.id, userAdmin.id)
        adminChangeResp = cloud20.changeDomainAdministrator(identityAdminToken, domainId, adminChange)

        then:
        adminChangeResp.status == HttpStatus.SC_NO_CONTENT
        assertTenantedRoleOnUserAfterAdminChange(userDefault, tenant1, role, userGroup)
        assertTenantedRoleOnUserAfterAdminChange(userAdmin, tenant1, role, userGroup)
        assertTenantedRoleOnUserAfterAdminChange(userDefault, tenant2, role, userGroup)
        assertTenantedRoleOnUserAfterAdminChange(userAdmin, tenant2, role, userGroup)
        assertUserDoesNotHaveRoleOnTenant(userDefault, role, tenant3)

        cleanup:
        utils.deleteUserQuietly(userDefault)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteUserGroup(userGroup)
        utils.deleteRole(role)
        utils.deleteTenant(tenant1)
        utils.deleteTenant(tenant2)
        utils.deleteTenant(tenant3)
        reloadableConfiguration.reset()
    }

    def "changeDomainAdministrator: promote user updates domain's userAdminDN"() {
        given:
        def userAdmin = utils.createCloudAccount(identityAdminToken)
        def domainId = userAdmin.domainId
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManage = utils.createUser(userAdminToken, testUtils.getRandomUUID("userManage"), domainId)
        utils.addRoleToUser(userManage, Constants.USER_MANAGE_ROLE_ID)

        when: "get userAdmin"
        def userAdminEntity = userDao.getUserById(userAdmin.id)
        def domainEntity = domainDao.getDomain(domainId)

        then: "verify domain's userAdminDN"
        domainEntity.userAdminDN == userAdminEntity.getDn()

        when: "upgrade user-manager"
        def promoteUserManagerChange = createAdminChange(userManage.id, userAdmin.id)
        def userManagePromoteResponse = cloud20.changeDomainAdministrator(identityAdminToken, domainId, promoteUserManagerChange)

        then:
        userManagePromoteResponse.status == HttpStatus.SC_NO_CONTENT

        when: "get promoted user manager"
        def userManagerEntity = userDao.getUserById(userManage.id)
        domainEntity = domainDao.getDomain(domainId)

        then:
        domainEntity.userAdminDN == userManagerEntity.getDn()

        cleanup:
        utils.deleteUserQuietly(userAdmin)
        utils.deleteUserQuietly(userManage)
        utils.deleteTestDomainQuietly(domainId)
    }

    enum TenantAssignmentType { GLOBAL, TENANT }

    /**
     * Given a user used in the domain admin change, verify that the user still has the role on the tenant
     * but not explicitly assigned. Also verify that the user is getting the role from the group.
     */
    void assertTenantedRoleOnUserAfterAdminChange(User user, Tenant tenant, Role role, UserGroup userGroup) {
        // The user still has the role assigned
        def authResp = utils.authenticate(user, Constants.DEFAULT_PASSWORD)
        assert authResp.user.roles.role.find { r -> r.id == role.id && r.tenantId == tenant.id} != null

        // The user does not have the role explicitly assigned
        def endUser = identityUserService.getEndUserById(user.id)
        def userRoles = tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(endUser)
        assert userRoles.find { tenantRole -> tenantRole.roleRsId == role.id && tenantRole.tenantIds.contains(tenant.id) } == null

        // The roles on the group are unmodified
        def roleAssignments = utils.listRoleAssignmentsOnUserGroup(userGroup)
        assert roleAssignments.tenantAssignments.tenantAssignment.size() == 1
        assert roleAssignments.tenantAssignments.tenantAssignment.find { ta -> ta.forTenants.contains(tenant.id) && ta.onRole == role.id} != null
    }

    /**
     * Given a user used in the domain admin change, verify that the user still has the role globally assigned
     * but not explicitly assigned. Also verify that the user is getting the role from the group.
     */
    void assertGlobalRoleOnUserAfterAdminChange(User user, Role role, UserGroup userGroup) {
        // The user still has the role assigned
        def authResp = utils.authenticate(user, Constants.DEFAULT_PASSWORD)
        assert authResp.user.roles.role.find { r -> r.id == role.id && r.tenantId == null } != null

        // The user does not have the role explicitly assigned
        def endUser = identityUserService.getEndUserById(user.id)
        def userRoles = tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(endUser)
        assert userRoles.find { tenantRole -> tenantRole.roleRsId == role.id && tenantRole.tenantIds.isEmpty() } == null

        // The roles on the group are unmodified
        def roleAssignments = utils.listRoleAssignmentsOnUserGroup(userGroup)
        assert roleAssignments.tenantAssignments.tenantAssignment.size() == 1
        assert roleAssignments.tenantAssignments.tenantAssignment.find { ta -> ta.forTenants.contains('*') && ta.onRole == role.id} != null
    }

    /**
     * Given a user, verify that the user does not have a role assigned to the tenant (the user can still have the
     * role assigned globally).
     */
    void assertUserDoesNotHaveRoleOnTenant(User user, Role role, Tenant tenant) {
        // The user still has the role assigned
        def authResp = utils.authenticate(user, Constants.DEFAULT_PASSWORD)
        assert authResp.user.roles.role.find { r -> r.id == role.id && r.tenantId == tenant.id} == null

        // The user does not have the role explicitly assigned
        def endUser = identityUserService.getEndUserById(user.id)
        def userRoles = tenantService.getExplicitlyAssignedTenantRolesForUserPerformant(endUser)
        assert userRoles.find { tenantRole -> tenantRole.roleRsId == role.id && tenantRole.tenantIds.contains(tenant.id) } == null
    }

    def createAdminChange(def promoteUserId, def demoteUserId) {
        new DomainAdministratorChange().with {
            it.promoteUserId = promoteUserId
            it.demoteUserId = demoteUserId
            it
        }
    }

}
