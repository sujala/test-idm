package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.http.HttpStatus
import groovy.json.JsonSlurper
import org.apache.http.client.utils.URLEncodedUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import javax.ws.rs.core.MediaType

import static com.rackspace.idm.domain.service.IdentityUserTypeEnum.*

class DefaultMultiFactorCloud20ServiceIntegrationTest extends RootIntegrationTest {

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

    def "test OTP feature flag"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        getReloadableConfiguration().setProperty("feature.otp.create.enabled.flag", "false")

        when:
        def name = "test"
        OTPDevice request = new OTPDevice()
        request.setName(name)
        def response = cloud20.addOTPDeviceToUser(userAdminToken, userAdmin.id, request)

        then:
        response.status == 404

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()
        getReloadableConfiguration().reset()
    }

    @Unroll
    def "test get OTP device for user, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def otpRequest = new OTPDevice().with {
            it.name = "myOtp"
            it
        }
        def device = utils.addOTPDevice(utils.getToken(userAdmin.username), userAdmin.id, otpRequest)

        when:
        def response = cloud20.getOTPDeviceFromUser(utils.getToken(userAdmin.username), userAdmin.id, device.id, accept)

        then:
        response.status == 200
        def otpResponse
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            otpResponse = response.getEntity(OTPDevice)
            assert otpResponse.qrcode == null
            assert otpResponse.keyUri == null
        } else {
            otpResponse = new JsonSlurper().parseText(response.getEntity(String))['RAX-AUTH:otpDevice']
            assert !otpResponse.hasProperty('qrcode')
            assert !otpResponse.hasProperty('keyUri')
        }
        otpResponse.verified == false
        otpResponse.name == otpRequest.name
        otpResponse.id == device.id

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "get OTP device with invalid user ID returns 404"() {
        when:
        def response = cloud20.getOTPDeviceFromUser(utils.getIdentityAdminToken(), "invalidId", "invalidId")

        then:
        response.status == 404
    }

    def "get OTP device with invalid device ID returns 404"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def otpRequest = new OTPDevice().with {
            it.name = "myOtp"
            it
        }
        utils.addOTPDevice(utils.getToken(userAdmin.username), userAdmin.id, otpRequest)

        when:
        def response = cloud20.getOTPDeviceFromUser(utils.getIdentityAdminToken(), userAdmin.id, "invalidId")

        then:
        response.status == 404
    }

    @Unroll
    def "test security for get device for user, caller = #caller, targetUser = #targetUser, differentDomain = #differentDomain"() {
        given:
        def domainId1 = utils.createDomain()
        def domainId2 = utils.createDomain()
        def identityAdmin1, userAdmin1, userManage1, defaultUser1
        def identityAdmin2, userAdmin2, userManage2, defaultUser2
        (identityAdmin1, userAdmin1, userManage1, defaultUser1) = utils.createUsers(domainId1)
        (identityAdmin2, userAdmin2, userManage2, defaultUser2) = utils.createUsers(domainId2)
        def otpRequest = new OTPDevice().with {
            it.name = "myOtp"
            it
        }
        def defaultUserDevice = utils.addOTPDevice(utils.getToken(defaultUser1.username), defaultUser1.id, otpRequest)
        def userAdminDevice = utils.addOTPDevice(utils.getToken(userAdmin1.username), userAdmin1.id, otpRequest)
        def token, targetUserId, deviceId
        switch (caller) {
            case SERVICE_ADMIN:
                token = utils.getServiceAdminToken()
                break;
            case IDENTITY_ADMIN:
                token = utils.getIdentityAdminToken()
                break;
            case USER_ADMIN:
                if(differentDomain) {
                    token = utils.getToken(userAdmin2.username)
                } else {
                    token = utils.getToken(userAdmin1.username)
                }
                break;
            case USER_MANAGER:
                if(differentDomain) {
                    token = utils.getToken(userManage2.username)
                } else {
                    token = utils.getToken(userManage1.username)
                }
                break;
            case DEFAULT_USER:
                if(differentDomain) {
                    token = utils.getToken(defaultUser2.username)
                } else {
                    token = utils.getToken(defaultUser1.username)
                }
                break;
        }
        switch (targetUser) {
            case USER_ADMIN:
                targetUserId = userAdmin1.id
                deviceId = userAdminDevice.id
                break;
            case DEFAULT_USER:
                targetUserId = defaultUser1.id
                deviceId = defaultUserDevice.id
                break;
        }

        when:
        def response = cloud20.getOTPDeviceFromUser(token, targetUserId, deviceId)

        then:
        response.status == expectedResponse

        cleanup:
        utils.deleteUsers(defaultUser1, userManage1, userAdmin1, identityAdmin1)
        utils.deleteUsers(defaultUser2, userManage2, userAdmin2, identityAdmin2)
        utils.deleteDomain(domainId1)
        utils.deleteDomain(domainId2)

        where:
        caller         | targetUser   | differentDomain | expectedResponse
        SERVICE_ADMIN  | DEFAULT_USER | false           | 200
//        IDENTITY_ADMIN | DEFAULT_USER | false           | 200
//        USER_ADMIN     | DEFAULT_USER | false           | 200
//        USER_MANAGER   | DEFAULT_USER | false           | 200
//        DEFAULT_USER   | DEFAULT_USER | false           | 200
//        USER_ADMIN     | DEFAULT_USER | true            | 403
//        USER_MANAGER   | DEFAULT_USER | true            | 403
//        DEFAULT_USER   | DEFAULT_USER | true            | 403
//        USER_ADMIN     | USER_ADMIN   | false           | 200
//        USER_MANAGER   | USER_ADMIN   | false           | 403
//        DEFAULT_USER   | USER_ADMIN   | false           | 403
//        USER_ADMIN     | USER_ADMIN   | true            | 403
//        USER_MANAGER   | USER_ADMIN   | true            | 403
//        DEFAULT_USER   | USER_ADMIN   | true            | 403
    }

}
