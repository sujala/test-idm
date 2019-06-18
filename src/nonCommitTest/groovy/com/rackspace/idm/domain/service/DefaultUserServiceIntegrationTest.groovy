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



    @Unroll
    def "The number of previous passwords stored in password history depends on config property: setting: #maxHistory"() {
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



    def "getUserById loading without password history"() {

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