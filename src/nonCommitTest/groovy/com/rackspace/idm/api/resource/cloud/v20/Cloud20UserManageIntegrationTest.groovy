package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.Cloud20Utils
import testHelpers.RootIntegrationTest

class Cloud20UserManageIntegrationTest extends RootIntegrationTest {

    def static DEFAULT_PASSWORD = "Password1"
    def static ROLE_USER_MANAGE_ID = "7"

    @Shared def Cloud20Utils cloud20Utils
    @Shared def sharedRandom = Cloud20Utils.createRandomString()
    @Shared String serviceAdminToken
    @Shared def defaultDomain = "domain$sharedRandom"

    @Shared def identityAdmin
    @Shared def identityAdminToken
    @Shared def userAdmin
    @Shared def userAdminToken
    @Shared def manageUser
    @Shared def manageUserToken
    @Shared def defaultUser
    @Shared def defaultUserToken
    @Shared def globalRole
    @Shared def userAdminDD //different domain
    @Shared def userAdminDDToken
    @Shared def defaultUserSD //same domain
    @Shared def defaultUserSDToken

    def setupSpec() {
        cloud20Utils = new Cloud20Utils(cloud20)
        serviceAdminToken = cloud20Utils.getServiceAdminToken()

        //create identity admin
        def identityAdminUsername = "identityAdmin$sharedRandom"
        identityAdmin = cloud20Utils.createUser(serviceAdminToken, identityAdminUsername, DEFAULT_PASSWORD)
        identityAdminToken = cloud20Utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)

        //create user admin
        def userAdminUsername = "userAdmin$sharedRandom"
        userAdmin = cloud20Utils.createUser(identityAdminToken, userAdminUsername, DEFAULT_PASSWORD, domainId:defaultDomain)
        userAdminToken = cloud20Utils.getToken(userAdmin.username, DEFAULT_PASSWORD)

        //create user manage user
        def userManageUsername = "manageUser$sharedRandom"
        manageUser = cloud20Utils.createUser(userAdminToken, userManageUsername, DEFAULT_PASSWORD)
        manageUserToken = cloud20Utils.getToken(manageUser.username, DEFAULT_PASSWORD)

        //assign user manage role to created default user
        cloud20.addApplicationRoleToUser(userAdminToken, ROLE_USER_MANAGE_ID, manageUser.id)

        //create default user
        def defaultUserUsername = "defaultUser$sharedRandom"
        defaultUser = cloud20Utils.createUser(userAdminToken, defaultUserUsername, DEFAULT_PASSWORD)
        defaultUserToken = cloud20Utils.getToken(defaultUser.username, DEFAULT_PASSWORD)

        //create global role and assign to default user
        //query param for application ID needs to be null in order for the role to be global
        def globalRoleToCreate = v2Factory.createRole("globalRole$sharedRandom", null).with {
            it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
            it
        }
        globalRole = cloud20.createRole(serviceAdminToken, globalRoleToCreate).getEntity(Role).value
        cloud20.addApplicationRoleToUser(manageUserToken, globalRole.id, defaultUser.id)

        //create user admin with different domain
        def userAdminDDUsername = "userManageDD$sharedRandom"
        userAdminDD = cloud20Utils.createUser(identityAdminToken, userAdminDDUsername, DEFAULT_PASSWORD, domainId:"otherDomain$sharedRandom")
        userAdminDDToken = cloud20Utils.getToken(userAdminDD.username, DEFAULT_PASSWORD)
        cloud20.addApplicationRoleToUser(identityAdminToken, ROLE_USER_MANAGE_ID, userAdminDD.id)

        //create default user in same domain
        def defaultUserSDUsername = "defaultUserSD$sharedRandom"
        defaultUserSD = cloud20Utils.createUser(userAdminToken, defaultUserSDUsername, DEFAULT_PASSWORD)
        defaultUserSDToken = cloud20Utils.getToken(defaultUserSD.username, DEFAULT_PASSWORD)
    }

    def cleanupSpec() {
        //TODO: delete all users and roles created
        cloud20.destroyUser(serviceAdminToken, identityAdmin.id)
        cloud20.destroyUser(serviceAdminToken, userAdmin.id)
        cloud20.destroyUser(serviceAdminToken, manageUser.id)
        cloud20.destroyUser(serviceAdminToken, defaultUser.id)
        cloud20.destroyUser(serviceAdminToken, userAdminDD.id)
        cloud20.destroyUser(serviceAdminToken, defaultUserSD.id)
        cloud20.deleteDomain(serviceAdminToken, defaultDomain)
        cloud20.deleteRole(serviceAdminToken, globalRole.id)
    }

    @Unroll
    def "list global roles for default user for user type #userType"() {
        when:
        //list roles using user manage token
        def rolesList = cloud20.listUserGlobalRoles(token, userId.toString())
        def roles = rolesList.getEntity(RoleList).value

        then:
        rolesList.status == status
        roles.role.id.contains(globalRole.id)

        where:
        token               | userId            | status | userType
        manageUserToken     | defaultUser.id    | 200    | "userManager"
        userAdminToken      | defaultUser.id    | 200    | "userAdmin"
        defaultUserToken    | defaultUser.id    | 200    | "defaultUser"
        identityAdminToken  | defaultUser.id    | 200    | "identityAdmin"
        serviceAdminToken   | defaultUser.id    | 200    | "serviceAdmin"
    }

    def "attempt to list global roles for user without proper permissions"() {

        when:
        //list roles using user manage token
        def rolesList = cloud20.listUserGlobalRoles(token, userId.toString())

        then:
        rolesList.status == status

        where:
        token               | userId            | status
        userAdminDDToken    | defaultUser.id    | 403
        defaultUserSDToken  | defaultUser.id    | 403
        userAdminToken      | identityAdmin.id  | 403
        "invalidToken"      | defaultUser.id    | 401
    }

}
