package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.helpers.Cloud20Utils
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URLEncodedUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.MediaType.APPLICATION_XML

@ContextConfiguration(locations = ["classpath:app-config.xml"])
class DefaultMultiFactorCloud20ServiceIntegrationTest extends RootIntegrationTest {

    @Autowired
    Cloud20Utils utils

    @Autowired
    LdapMobilePhoneRepository mobilePhoneRepository

    @Autowired
    LdapUserRepository userRepository

    @Autowired
    OTPHelper otpHelper

    @Unroll
    def "Get devices for user returns devices accept: #accept contentType: #contentType"() {
        given:
        useMediaType(accept, contentType)

        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def phone = utils.addPhone(userAdminToken, userAdmin.id)

        when:
        MobilePhones phoneList = utils.listDevices(userAdmin)

        then:
        phoneList != null
        phoneList.mobilePhone.size() == 1
        phoneList.mobilePhone[0].isVerified() == false
        phoneList.mobilePhone[0].id == phone.id
        phoneList.mobilePhone[0].number == phone.number

        when:
        def user = userRepository.getUserById(userAdmin.id)
        user.multiFactorDeviceVerified = true
        userRepository.updateUser(user)
        MobilePhones phoneList2 = utils.listDevices(userAdmin)

        then:
        phoneList2 != null
        phoneList2.mobilePhone.size() == 1
        phoneList2.mobilePhone[0].isVerified() == true
        phoneList2.mobilePhone[0].id == phone.id
        phoneList2.mobilePhone[0].number == phone.number

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(phone.id))

        where:
        [ accept, contentType ] << contentTypePermutations()
    }

    def "Get devices for sub-user returns correct value for user"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUserToken = utils.getToken(defaultUser.username)
        utils.addPhone(defaultUserToken, defaultUser.id)
        def user = userRepository.getUserById(defaultUser.id)
        user.multiFactorDeviceVerified = true
        userRepository.updateUser(user)

        when: "list devices for user with self"
        def phoneList = utils.listDevices(defaultUser, defaultUserToken)

        then: "the verified flag is set correctly"
        phoneList != null
        phoneList.mobilePhone.size() == 1
        phoneList.mobilePhone[0].verified == true

        when: "list devices for a user with a different device verified flag"
        phoneList = utils.listDevices(defaultUser, userAdminToken)

        then: "the verified flag is set correctly"
        phoneList != null
        phoneList.mobilePhone.size() == 1
        phoneList.mobilePhone[0].verified == true

        cleanup:
        utils.deleteUsers(users)
    }

    @Unroll
    def "Get devices for user returns empty devices accept: #accept contentType: #contentType"() {
        given:
        useMediaType(accept, contentType)

        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        MobilePhones phoneList = utils.listDevices(userAdmin)

        then:
        phoneList != null
        phoneList.mobilePhone.size() == 0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        [ accept, contentType ] << contentTypePermutations()
    }

    def "Create, verify, and delete OTP device"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "add OTP device"
        def name = "test"
        OTPDevice request = new OTPDevice()
        request.setName(name)
        def device = utils.addOTPDevice(userAdminToken, userAdmin.id, request)

        then: "gets added"
        device.id != null
        device.name == name
        device.verified == false
        device.getKeyUri() != null
        device.getQrcode() != null

        when:
        def secret = Base32.decode(URLEncodedUtils.parse(new URI(device.getKeyUri()), "UTF-8").find { it.name == 'secret' }.value)

        then:
        secret.length == 20

        when: "get device"
        def deviceId = device.id
        device = utils.getOTPDevice(userAdminToken, userAdmin.id, deviceId)

        then: "device is found and values are populated correctly"
        device.id == deviceId
        device.name == name
        device.verified == false
        device.getKeyUri() == null
        device.getQrcode() == null

        when: "verify the device"
        if ((((int) (System.currentTimeMillis() / 1000)) % 30) < 3) {
            Thread.sleep(4000) // avoid race test on the time shift
        }
        def code = new VerificationCode()
        code.setCode(otpHelper.TOTP(secret))
        utils.verifyOTPDevice(userAdminToken, userAdmin.id, deviceId, code)
        device = utils.getOTPDevice(userAdminToken, userAdmin.id, deviceId)

        then: "device is marked as verified"
        device.verified == true

        when: "delete the device"
        def delResponse = cloud20.deleteOTPDeviceFromUser(userAdminToken, userAdmin.id, deviceId)

        then: "device is deleted"
        delResponse.status == HttpStatus.SC_NO_CONTENT

        and:
        cloud20.getOTPDeviceFromUser(userAdminToken, userAdmin.id, deviceId).status == HttpStatus.SC_NOT_FOUND

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
