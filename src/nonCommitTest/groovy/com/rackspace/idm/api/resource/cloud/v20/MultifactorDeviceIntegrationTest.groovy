package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDevices
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevices
import com.rackspace.idm.JSONConstants
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

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

    @Unroll
    def "test list multi factor devices - MediaType = #mediaType"() {
        given:
        def user = utils.createCloudAccount()
        def userToken = utils.getToken(user.username)

        when:
        def response = cloud20.getMultiFactorDevicesFromUser(utils.getToken(user.username), user.id, mediaType)

        then:
        response.status == 200

        when: "list devices with one mobile phone"
        MobilePhone mobilePhone = utils.addMobilePhoneToUser(userToken, user)
        response = cloud20.getMultiFactorDevicesFromUser(utils.getToken(user.username), user.id, mediaType)
        def devices = getMultiFactorDevicesEntity(response)

        then:
        response.status == HttpStatus.SC_OK

        devices.mobilePhones.mobilePhone != null
        devices.mobilePhones.mobilePhone.size() == 1
        devices.mobilePhones.mobilePhone.get(0).id == mobilePhone.id

        devices.otpDevices.otpDevice != null
        devices.otpDevices.otpDevice.size() == 0

        when: "list devices with one otp device"
        OTPDevice otpDevice = utils.addOtpDeviceToUser(userToken, user)
        response = cloud20.getMultiFactorDevicesFromUser(utils.getToken(user.username), user.id, mediaType)
        devices = getMultiFactorDevicesEntity(response)

        then:
        response.status == HttpStatus.SC_OK

        devices.mobilePhones.mobilePhone != null
        devices.mobilePhones.mobilePhone.size() == 1
        devices.mobilePhones.mobilePhone.get(0).id == mobilePhone.id

        devices.otpDevices.otpDevice != null
        devices.otpDevices.otpDevice.size() == 1
        devices.otpDevices.otpDevice.get(0).id == otpDevice.id

        cleanup:
        utils.deleteUsers(user)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "verify list multi factor devices returns an empty list if no devices are found on user - MediaType = #mediaType"() {
        given:
        def user = utils.createCloudAccount()

        when:
        def response = cloud20.getMultiFactorDevicesFromUser(utils.getToken(user.username), user.id, mediaType)
        def devices = getMultiFactorDevicesEntity(response)

        then:
        response.status == 200

        devices.mobilePhones.mobilePhone != null
        devices.mobilePhones.mobilePhone.size() == 0

        devices.otpDevices.otpDevice != null
        devices.otpDevices.otpDevice.size() == 0

        cleanup:
        utils.deleteUsers(user)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def getMultiFactorDevicesEntity(response) {
        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            return response.getEntity(MultiFactorDevices)
        }

        Object object = new JsonSlurper().parseText(response.getEntity(String))[JSONConstants.RAX_AUTH_MULTIFACTOR_DEVICES]
        MultiFactorDevices multiFactorDevices = new MultiFactorDevices()
        multiFactorDevices.mobilePhones = new MobilePhones()
        multiFactorDevices.mobilePhones.mobilePhone.addAll(object[JSONConstants.MOBILE_PHONES])
        multiFactorDevices.otpDevices = new OTPDevices()
        multiFactorDevices.otpDevices.otpDevice.addAll(object[JSONConstants.OTP_DEVICES])

        return multiFactorDevices
    }

}
