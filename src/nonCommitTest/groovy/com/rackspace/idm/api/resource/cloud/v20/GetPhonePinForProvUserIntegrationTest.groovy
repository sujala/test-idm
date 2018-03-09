package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.UserService
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE
import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE_PASSWORD
import static org.apache.http.HttpStatus.SC_OK

class GetPhonePinForProvUserIntegrationTest extends RootIntegrationTest {

    @Shared
    def identityAdmin, userAdmin, userManage, defaultUser

    @Autowired
    UserService userService

    @Unroll
    def "Get phone pin for identityAdmin, userAdmin, userManage, defaultUser users; media = #accept" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def identityAdminUserEntity = userService.getUserById(identityAdmin.id)
        def userAdminUserEntity = userService.getUserById(userAdmin.id)
        def userManageUserEntity = userService.getUserById(userManage.id)
        def defaultUserEntity = userService.getUserById(defaultUser.id)

        when: "Service admin token able to retrieve the pin for any user"

        def response1 = cloud20.getPhonePin(utils.getServiceAdminToken(), identityAdmin.id)
        def response2 = cloud20.getPhonePin(utils.getServiceAdminToken(), userAdmin.id)
        def response3 = cloud20.getPhonePin(utils.getServiceAdminToken(), userManage.id)
        def response4 = cloud20.getPhonePin(utils.getServiceAdminToken(), defaultUser.id)

        then:
        assert response1.status == SC_OK
        assert response1.getEntity(PhonePin).pin == identityAdminUserEntity.phonePin

        assert response2.status == SC_OK
        assert response2.getEntity(PhonePin).pin == userAdminUserEntity.phonePin

        assert response3.status == SC_OK
        assert response3.getEntity(PhonePin).pin == userManageUserEntity.phonePin

        assert response4.status == SC_OK
        assert response4.getEntity(PhonePin).pin == defaultUserEntity.phonePin

        when: "Identity admin token able to retrieve the pin for any user except another identity admin"

        response1 = cloud20.getPhonePin(utils.getToken(identityAdmin.username), identityAdmin.id)
        response2 = cloud20.getPhonePin(utils.getIdentityAdminToken(), userAdmin.id)
        response3 = cloud20.getPhonePin(utils.getIdentityAdminToken(), userManage.id)
        response4 = cloud20.getPhonePin(utils.getIdentityAdminToken(), defaultUser.id)

        def response5 = cloud20.getPhonePin(utils.getIdentityAdminToken(), identityAdmin.id)

        then:
        assert response1.status == SC_OK
        assert response1.getEntity(PhonePin).pin == identityAdminUserEntity.phonePin

        assert response2.status == SC_OK
        assert response2.getEntity(PhonePin).pin == userAdminUserEntity.phonePin

        assert response3.status == SC_OK
        assert response3.getEntity(PhonePin).pin == userManageUserEntity.phonePin

        assert response4.status == SC_OK
        assert response4.getEntity(PhonePin).pin == defaultUserEntity.phonePin

        IdmAssert.assertOpenStackV2FaultResponse(response5, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "User admin token able to retrieve the pin only for self and sub-users"

        response1 = cloud20.getPhonePin(utils.getToken(userAdmin.username), identityAdmin.id)
        response2 = cloud20.getPhonePin(utils.getToken(userAdmin.username), userAdmin.id)
        response3 = cloud20.getPhonePin(utils.getToken(userAdmin.username), userManage.id)
        response4 = cloud20.getPhonePin(utils.getToken(userAdmin.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        assert response2.status == SC_OK
        assert response2.getEntity(PhonePin).pin == userAdminUserEntity.phonePin

        assert response3.status == SC_OK
        assert response3.getEntity(PhonePin).pin == userManageUserEntity.phonePin

        assert response4.status == SC_OK
        assert response4.getEntity(PhonePin).pin == defaultUserEntity.phonePin


        when: "User manage token able to retrieve the pin only for self and sub-users"

        response1 = cloud20.getPhonePin(utils.getToken(userManage.username), identityAdmin.id)
        response2 = cloud20.getPhonePin(utils.getToken(userManage.username), userAdmin.id)
        response3 = cloud20.getPhonePin(utils.getToken(userManage.username), userManage.id)
        response4 = cloud20.getPhonePin(utils.getToken(userManage.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        assert response3.status == SC_OK
        assert response3.getEntity(PhonePin).pin == userManageUserEntity.phonePin

        assert response4.status == SC_OK
        assert response4.getEntity(PhonePin).pin == defaultUserEntity.phonePin

        when: "Default user token able to retrieve the pin only for self"

        response1 = cloud20.getPhonePin(utils.getToken(defaultUser.username), identityAdmin.id)
        response2 = cloud20.getPhonePin(utils.getToken(defaultUser.username), userAdmin.id)
        response3 = cloud20.getPhonePin(utils.getToken(defaultUser.username), userManage.id)
        response4 = cloud20.getPhonePin(utils.getToken(defaultUser.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Default user (sub-user) token of another user cannot retrieve the phone PIN.")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Default user (sub-user) token of another user cannot retrieve the phone PIN.")
        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Default user (sub-user) token of another user cannot retrieve the phone PIN.")

        assert response4.status == SC_OK
        assert response4.getEntity(PhonePin).pin == defaultUserEntity.phonePin

        when: "User manage token from one domain cannot retrieve the phone PIN of default user on another domain"
        def domain2 = utils.createDomain()
        def (userManage1, users) = utils.createDefaultUser(domain2)
        utils.addRoleToUser(userManage1, Constants.USER_MANAGE_ROLE_ID)
        def response6 = cloud20.getPhonePin(utils.getToken(userManage1.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response6, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; User with user-admin/user-manage token from another domain cannot retrieve the phone PIN.")

        when: "User manage token cannot retrieve phone pin of other user manager under the same domain"
        utils.addRoleToUser(defaultUser, Constants.USER_MANAGE_ROLE_ID)
        def response7 = cloud20.getPhonePin(utils.getToken(defaultUser.username), userManage.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response7, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "User admin token of a different domain cannot retrieve the phone pin"
        def userAdmin1 = cloud20.createCloudAccount(utils.getIdentityAdminToken())
        def response8 = cloud20.getPhonePin(utils.getToken(userAdmin1.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response8, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; User with user-admin/user-manage token from another domain cannot retrieve the phone PIN.")

        when: "Racker impersonated token cannot be used to retrieve the phone pin"
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
        response2 = cloud20.getPhonePin(utils.impersonateWithToken(rackerToken, userAdmin).token.id, userAdmin.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Impersonation tokens cannot be used to retrieve the phone PIN.")

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin, userAdmin1)
        utils.deleteUsers(users)
        utils.deleteDomain(domain2)
        utils.deleteDomain(domainId)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Get phone pin on userAdmin returns 404 when phone pin is not assigned at the time of create user" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)

        def user = utils.createCloudAccount()

        when:

        def response = cloud20.getPhonePin(utils.getServiceAdminToken(), user.id)

        then:

        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "Error code: 'PP-000'; Phone pin not found for userId: '"+user.id+"'")

        cleanup:
        utils.deleteUser(user)
        staticIdmConfiguration.reset()
    }
}

