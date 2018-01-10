package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import org.openstack.docs.identity.api.v2.BadRequestFault
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

class MultifactorDeviceIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "cannot create OTP device with invalid device name, deviceName = '#deviceName'"() {
        given:
        def domainId = utils.createDomain()
        def users, userAdmin
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def otpDevice = new OTPDevice().with {
            it.name = deviceName
            it
        }

        when:
        def response = cloud20.addOTPDeviceToUser(utils.getToken(userAdmin.username), userAdmin.id, otpDevice)

        then:
        response.status == 400
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, 400, DefaultMultiFactorCloud20Service.BAD_REQUEST_MSG_INVALID_OTP_DEVICE_NAME)

        cleanup:
        utils.deleteUsers(users)

        where:
        deviceName << [null, "", " "]
    }

}
