package com.rackspace.idm.domain.service

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.DelegationAgreementRoleSearchParams
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.impl.DefaultIdentityUserService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import com.rackspace.idm.util.CryptHelper
import org.apache.commons.collections4.IteratorUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.ROLE_RBAC1_ID
import static com.rackspace.idm.Constants.ROLE_RBAC2_ID

class DefaultUserServiceIntegrationTest extends RootIntegrationTest {

    @Autowired
    DefaultUserService userService

    @Autowired
    DefaultIdentityUserService identityUserService

    @Autowired
    CryptHelper cryptHelper

    @Autowired
    UserDao userDao

    @Autowired
    UserGroupService userGroupService

    @Autowired
    DelegationService delegationService

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def "Password history is stored when enabled; even when other password policy features are disabled"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, false)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, true)

        User user = utils.createGenericUserAdmin()

        def newPassword1 = "newPassword1"
        com.rackspace.idm.domain.entity.User userUpdate1 = new com.rackspace.idm.domain.entity.User().with {
            it.id = user.id
            it.username = user.username
            it.password = newPassword1
            it
        }

        def newPassword2 = "newPassword1"
        com.rackspace.idm.domain.entity.User userUpdate2 = new com.rackspace.idm.domain.entity.User().with {
            it.id = user.id
            it.username = user.username
            it.password = newPassword2
            it
        }

        when: "get password history for newly created user"
        def userEntity = identityUserService.getProvisionedUserByIdWithPwdHis(user.id)
        List<String> pwdHistory = userEntity.getPasswordHistory()

        then: "history contains initial password"
        pwdHistory.size() == 1
        cryptHelper.verifyLegacySHA(Constants.DEFAULT_PASSWORD, pwdHistory.get(0))

        when: "User changes password"
        userService.updateUser(userUpdate1)
        userEntity = identityUserService.getProvisionedUserByIdWithPwdHis(user.id)
        pwdHistory = userEntity.getPasswordHistory()

        then: "2 passwords in history with first being the oldest"
        pwdHistory.size() == 2
        cryptHelper.verifyLegacySHA(Constants.DEFAULT_PASSWORD, pwdHistory.get(0))
        cryptHelper.verifyLegacySHA(newPassword1, pwdHistory.get(1))

        when: "User changes password"
        userService.updateUser(userUpdate2)
        userEntity = identityUserService.getProvisionedUserByIdWithPwdHis(user.id)
        pwdHistory = userEntity.getPasswordHistory()

        then: "3 passwords in history with first being the oldest"
        pwdHistory.size() == 3
        cryptHelper.verifyLegacySHA(Constants.DEFAULT_PASSWORD, pwdHistory.get(0))
        cryptHelper.verifyLegacySHA(newPassword1, pwdHistory.get(1))
        cryptHelper.verifyLegacySHA(newPassword2, pwdHistory.get(2))

        when: "User changes password to same thing"
        userService.updateUser(userUpdate2)
        userEntity = identityUserService.getProvisionedUserByIdWithPwdHis(user.id)
        pwdHistory = userEntity.getPasswordHistory()

        then: "4 passwords in history with last 2 being the same password, but with different hashes"
        pwdHistory.size() == 4
        cryptHelper.verifyLegacySHA(Constants.DEFAULT_PASSWORD, pwdHistory.get(0))
        cryptHelper.verifyLegacySHA(newPassword1, pwdHistory.get(1))
        cryptHelper.verifyLegacySHA(newPassword2, pwdHistory.get(2))
        cryptHelper.verifyLegacySHA(newPassword2, pwdHistory.get(3))
        pwdHistory.get(2) != pwdHistory.get(3)
    }

    @Unroll
    def "The number of previous passwords stored in password history depends on config property: setting: #maxHistory"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, false)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.PASSWORD_HISTORY_MAX_PROP, maxHistory)

        User user = utils.createGenericUserAdmin()
        com.rackspace.idm.domain.entity.User userUpdate = new com.rackspace.idm.domain.entity.User().with {
            it.id = user.id
            it.username = user.username
            it
        }
        def newPasswordPrefix = "newPassword"

        def totalPasswordChanges = maxHistory + 1 // Add one more due to current password being stored in history.
        when: "update password totalPasswordChanges times"
        for (i in 1..totalPasswordChanges) {
            userUpdate.password = newPasswordPrefix + i
            userService.updateUser(userUpdate)
        }
        def userEntity = identityUserService.getProvisionedUserByIdWithPwdHis(user.id)
        List<String> pwdHistory = userEntity.getPasswordHistory()

        then: "history is full and original password not included"
        pwdHistory.size() == totalPasswordChanges //stores maxHistory + current password

        and: "all the passwords are newly set passwords in order"
        for (i in 1..totalPasswordChanges) {
            def pwd = newPasswordPrefix + i
            assert cryptHelper.verifyLegacySHA(pwd, pwdHistory.get(i-1))
        }

        where:
        maxHistory | _
        0 | _
        3 | _
        5 | _
    }

    def "History is not added to, but not nulled out, if maintain history property is disabled"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, false)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.PASSWORD_HISTORY_MAX_PROP, 5)

        User user = utils.createGenericUserAdmin()
        com.rackspace.idm.domain.entity.User userUpdate = new com.rackspace.idm.domain.entity.User().with {
            it.id = user.id
            it.username = user.username
            it.password = "newPassword1"
            it
        }

        when: "get password history for newly created user"
        def userEntity = identityUserService.getProvisionedUserByIdWithPwdHis(user.id)
        List<String> pwdHistory = userEntity.getPasswordHistory()

        then: "history contains initial password"
        pwdHistory.size() == 1
        cryptHelper.verifyLegacySHA(Constants.DEFAULT_PASSWORD, pwdHistory.get(0))

        when: "Disable history and change password"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, false)
        userService.updateUser(userUpdate)
        userEntity = identityUserService.getProvisionedUserByIdWithPwdHis(user.id)
        pwdHistory = userEntity.getPasswordHistory()

        then: "history remains at 1 w/ original password"
        pwdHistory.size() == 1
        cryptHelper.verifyLegacySHA(Constants.DEFAULT_PASSWORD, pwdHistory.get(0))
    }

    def "getUserById loading without password history"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_POLICY_SERVICES_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_HISTORY_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENFORCE_PASSWORD_POLICY_EXPIRATION_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_MAINTAIN_PASSWORD_HISTORY_PROP, true)

        User user = utils.createGenericUserAdmin()
        com.rackspace.idm.domain.entity.User userUpdate = new com.rackspace.idm.domain.entity.User().with {
            it.id = user.id
            it.username = user.username
            it.password = "newPassword1"
            it
        }

        when:
        def userEntity = userDao.getUserById(user.id)

        then:
        userEntity.getPasswordHistory() == null
    }

    def "getUsersByTenantId: Does not return user group tenant roles"() {
        setup:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        User user = utils.createCloudAccount()
        UserGroup group = utils.createUserGroup(user.domainId)

        // Assign role to both user group and user on mosso tenant
        utils.addRoleToUserOnTenantId(user, user.domainId, Constants.ROLE_RBAC1_ID)

        TenantAssignment ta = new TenantAssignment().with {
            ta ->
                ta.onRole = ROLE_RBAC1_ID
                ta.forTenants = [user.domainId]
                ta
        }
        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments tas = new TenantAssignments()
            it.tenantAssignments = tas
            tas.tenantAssignment.add(ta)
            it
        }
        def response = cloud20.grantRoleAssignmentsOnUserGroup(utils.getIdentityAdminToken(), group, assignments1)
        assert response.status == HttpStatus.SC_OK

        when:
        Iterable<User> users = userService.getUsersByTenantId(user.domainId)

        then:
        List<User> userList = IteratorUtils.toList(users.iterator())
        userList.size() == 1
        userList.get(0).id == user.id

        when: "retrieve user group assignment"
        List<TenantRole> userGroupRoles = userGroupService.getRoleAssignmentsOnGroup(group.id)

        then: "no user ids"
        userGroupRoles.size() == 1
        userGroupRoles[0].userId == null
    }

    def "getUsersByTenantId: Does not return delegation agreement tenant roles"() {
        setup:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)

        User user = utils.createCloudAccount()
        def userToken = utils.getToken(user.username)
        DelegationAgreement delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = user.domainId
            it
        }
        DelegationAgreement createdDA = utils.createDelegationAgreement(userToken, delegationAgreement)

        // Assign role to user on mosso tenant
        utils.addRoleToUserOnTenantId(user, user.domainId, Constants.ROLE_RBAC1_ID)

        TenantAssignment ta = new TenantAssignment().with {
            ta ->
                ta.onRole = Constants.ROLE_RBAC2_ID
                ta.forTenants = [user.domainId]
                ta
        }
        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments tas = new TenantAssignments()
            it.tenantAssignments = tas
            tas.tenantAssignment.add(ta)
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userToken, createdDA, assignments1)
        assert response.status == HttpStatus.SC_OK

        when:
        Iterable<User> users = userService.getUsersByTenantId(user.domainId)

        then:
        List<User> userList = IteratorUtils.toList(users.iterator())
        userList.size() == 1
        userList.get(0).id == user.id

        when: "retrieve delegation agreement assignment"
        def daEntity = delegationService.getDelegationAgreementById(createdDA.id)
        PaginatorContext<TenantRole> daRoles = delegationService.getRoleAssignmentsOnDelegationAgreement(daEntity, new DelegationAgreementRoleSearchParams(new PaginationParams()))

        then: "no user ids"
        daRoles.searchResultEntryList.size() == 1
        daRoles.searchResultEntryList.get(0).getAttribute("roleRsId").values[0].toString() == ROLE_RBAC2_ID
        daRoles.searchResultEntryList.get(0).getAttribute("userId") == null
    }
}