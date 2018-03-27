package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.UserService
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE
import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE_PASSWORD
import static org.apache.http.HttpStatus.SC_OK

@Ignore
class GetPhonePinForProvUserIntegrationTest extends RootIntegrationTest {

    @Shared
    def identityAdmin, userAdmin, userManage, defaultUser

    @Autowired
    UserService userService

    @Unroll
    def "Get phone PIN retrieves the PIN only for the user for self; media = #accept" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "Service admin token cannot retrieve the pin for any user"

        def response1 = cloud20.getPhonePin(utils.getServiceAdminToken(), identityAdmin.id)
        def response2 = cloud20.getPhonePin(utils.getServiceAdminToken(), userAdmin.id)
        def response3 = cloud20.getPhonePin(utils.getServiceAdminToken(), userManage.id)
        def response4 = cloud20.getPhonePin(utils.getServiceAdminToken(), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response4, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "Identity admin token cannot retrieve the pin for any user"

        response1 = cloud20.getPhonePin(utils.getIdentityAdminToken(), identityAdmin.id)
        response2 = cloud20.getPhonePin(utils.getIdentityAdminToken(), userAdmin.id)
        response3 = cloud20.getPhonePin(utils.getIdentityAdminToken(), userManage.id)
        response4 = cloud20.getPhonePin(utils.getIdentityAdminToken(), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response4, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "User admin token able to retrieve the pin only for self and not for sub-users"

        response1 = cloud20.getPhonePin(utils.getToken(userAdmin.username), identityAdmin.id)

        response2 = cloud20.getPhonePin(utils.getToken(userAdmin.username), userAdmin.id)
        def phonePin2 = userService.checkAndGetUserById(userAdmin.id).phonePin

        response3 = cloud20.getPhonePin(utils.getToken(userAdmin.username), userManage.id)
        response4 = cloud20.getPhonePin(utils.getToken(userAdmin.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        assert response2.status == SC_OK
        assert response2.getEntity(PhonePin).pin == phonePin2

        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response4, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "User manage token able to retrieve the pin only for self and not sub-users"

        response1 = cloud20.getPhonePin(utils.getToken(userManage.username), identityAdmin.id)
        response2 = cloud20.getPhonePin(utils.getToken(userManage.username), userAdmin.id)

        response3 = cloud20.getPhonePin(utils.getToken(userManage.username), userManage.id)
        def phonePin3 = userService.checkAndGetUserById(userManage.id).phonePin

        response4 = cloud20.getPhonePin(utils.getToken(userManage.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        assert response3.status == SC_OK
        assert response3.getEntity(PhonePin).pin == phonePin3

        IdmAssert.assertOpenStackV2FaultResponse(response4, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "Default user token able to retrieve the pin only for self"

        response1 = cloud20.getPhonePin(utils.getToken(defaultUser.username), identityAdmin.id)
        response2 = cloud20.getPhonePin(utils.getToken(defaultUser.username), userAdmin.id)
        response3 = cloud20.getPhonePin(utils.getToken(defaultUser.username), userManage.id)

        response4 = cloud20.getPhonePin(utils.getToken(defaultUser.username), defaultUser.id)
        def phonePin4 = userService.checkAndGetUserById(defaultUser.id).phonePin

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        assert response4.status == SC_OK
        assert response4.getEntity(PhonePin).pin == phonePin4

        when: "Racker impersonated token cannot retrieve the phone pin"
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
        response2 = cloud20.getPhonePin(utils.impersonateWithToken(rackerToken, userAdmin).token.id, userAdmin.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Impersonation tokens cannot be used to retrieve the phone PIN.")

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Get phone pin returns 404 when phone pin feature is disabled" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)

        when:
        def response = cloud20.getPhonePin(utils.getServiceAdminToken(), "anyUserId", MediaType.APPLICATION_JSON_TYPE)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "Service Not Found")

        cleanup:
        staticIdmConfiguration.reset()
    }
}

