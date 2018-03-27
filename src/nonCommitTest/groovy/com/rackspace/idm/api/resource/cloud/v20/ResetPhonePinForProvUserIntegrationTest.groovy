package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE
import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE_PASSWORD
import static org.apache.http.HttpStatus.SC_OK

class ResetPhonePinForProvUserIntegrationTest extends RootIntegrationTest {

    @Shared
    def identityAdmin, userAdmin, userManage, defaultUser

    @Unroll
    def "Reset phone pin for identityAdmin, userAdmin, userManage, defaultUser users; media = #accept" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "Service admin token will not be able to reset the pin for any user"

        def response1 = cloud20.resetPhonePin(utils.getServiceAdminToken(), identityAdmin.id)
        def response2 = cloud20.resetPhonePin(utils.getServiceAdminToken(), userAdmin.id)
        def response3 = cloud20.resetPhonePin(utils.getServiceAdminToken(), userManage.id)
        def response4 = cloud20.resetPhonePin(utils.getServiceAdminToken(), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response4, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "Identity admin token able to reset the pin for any user"

        def pinIA1 = utils.getPhonePin(identityAdmin.id, utils.getToken(identityAdmin.username)).pin
        response1 = cloud20.resetPhonePin(utils.getIdentityAdminToken(), identityAdmin.id)
        def pinFromResponse1 = response1.getEntity(PhonePin).pin
        def pinIA2 = utils.getPhonePin(identityAdmin.id, utils.getToken(identityAdmin.username)).pin

        def pinUA1 = utils.getPhonePin(userAdmin.id, utils.getToken(userAdmin.username)).pin
        response2 = cloud20.resetPhonePin(utils.getIdentityAdminToken(), userAdmin.id)
        def pinFromResponse2 = response2.getEntity(PhonePin).pin
        def pinUA2 = utils.getPhonePin(userAdmin.id, utils.getToken(userAdmin.username)).pin

        def pinUM1 = utils.getPhonePin(userManage.id, utils.getToken(userManage.username)).pin
        response3 = cloud20.resetPhonePin(utils.getIdentityAdminToken(), userManage.id)
        def pinFromResponse3 = response3.getEntity(PhonePin).pin
        def pinUM2 = utils.getPhonePin(userManage.id, utils.getToken(userManage.username)).pin

        def pinDU1 = utils.getPhonePin(defaultUser.id, utils.getToken(defaultUser.username)).pin
        response4 = cloud20.resetPhonePin(utils.getIdentityAdminToken(), defaultUser.id)
        def pinFromResponse4 = response4.getEntity(PhonePin).pin
        def pinDU2 = utils.getPhonePin(defaultUser.id, utils.getToken(defaultUser.username)).pin

        then: "phone pin before and after reset are different"
        assert response1.status == SC_OK
        assert pinFromResponse1 != pinIA1
        assert pinFromResponse1 == pinIA2

        assert response2.status == SC_OK
        assert pinFromResponse2 != pinUA1
        assert pinFromResponse2 == pinUA2

        assert response3.status == SC_OK
        assert pinFromResponse3 != pinUM1
        assert pinFromResponse3 == pinUM2

        assert response4.status == SC_OK
        assert pinFromResponse4 != pinDU1
        assert pinFromResponse4 == pinDU2

        when: "User admin token able to reset the pin only for self"

        response1 = cloud20.resetPhonePin(utils.getToken(userAdmin.username), identityAdmin.id)

        pinUA2 = utils.getPhonePin(userAdmin.id, utils.getToken(userAdmin.username)).pin
        response2 = cloud20.resetPhonePin(utils.getToken(userAdmin.username), userAdmin.id)
        pinFromResponse2 = response2.getEntity(PhonePin).pin
        pinUA1 = utils.getPhonePin(userAdmin.id, utils.getToken(userAdmin.username)).pin

        response3 = cloud20.resetPhonePin(utils.getToken(userAdmin.username), userManage.id)
        response4 = cloud20.resetPhonePin(utils.getToken(userAdmin.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        assert response2.status == SC_OK
        assert pinFromResponse2 != pinUA2
        assert pinFromResponse2 == pinUA1

        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response4, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "User manage token able to reset the pin only for self"

        response1 = cloud20.resetPhonePin(utils.getToken(userManage.username), identityAdmin.id)
        response2 = cloud20.resetPhonePin(utils.getToken(userManage.username), userAdmin.id)

        response3 = cloud20.resetPhonePin(utils.getToken(userManage.username), userManage.id)
        pinFromResponse3 = response3.getEntity(PhonePin).pin
        pinUM1 = utils.getPhonePin(userManage.id, utils.getToken(userManage.username)).pin

        response4 = cloud20.resetPhonePin(utils.getToken(userManage.username), defaultUser.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        assert response3.status == SC_OK
        assert pinFromResponse3 != pinUM2
        assert pinFromResponse3 == pinUM1

        IdmAssert.assertOpenStackV2FaultResponse(response4, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "Default user token able to reset the pin only for self"

        response1 = cloud20.resetPhonePin(utils.getToken(defaultUser.username), identityAdmin.id)
        response2 = cloud20.resetPhonePin(utils.getToken(defaultUser.username), userAdmin.id)
        response3 = cloud20.resetPhonePin(utils.getToken(defaultUser.username), userManage.id)

        response4 = cloud20.resetPhonePin(utils.getToken(defaultUser.username), defaultUser.id)
        pinFromResponse4 = response4.getEntity(PhonePin).pin
        pinDU2 = utils.getPhonePin(defaultUser.id, utils.getToken(defaultUser.username)).pin

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response1, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")
        IdmAssert.assertOpenStackV2FaultResponse(response3, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        assert response4.status == SC_OK
        assert pinFromResponse4 != pinDU1
        assert pinFromResponse4 == pinDU2

        when: "Racker impersonated token cannot be used to reset the phone pin"
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
        response2 = cloud20.resetPhonePin(utils.impersonateWithToken(rackerToken, userAdmin).token.id, userAdmin.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response2, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Impersonation tokens cannot be used to reset the phone PIN.")

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Reset phone pin returns 404 when phone pin feature is disabled" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, false)

        when:
        def response = cloud20.resetPhonePin(utils.getIdentityAdminToken(), "anyUserId", MediaType.APPLICATION_JSON_TYPE)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "Service Not Found")

        cleanup:
        staticIdmConfiguration.reset()
    }
}
