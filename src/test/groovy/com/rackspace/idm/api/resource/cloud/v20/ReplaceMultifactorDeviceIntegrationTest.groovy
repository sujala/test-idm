package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.multifactor.PhoneNumberGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly


@ContextConfiguration(locations = ["classpath:app-config.xml"
        , "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml"])
class ReplaceMultifactorDeviceIntegrationTest extends RootIntegrationTest {

    @Autowired
    MobilePhoneDao mobilePhoneRepository;

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired
    private IdentityUserService userService

    @Override
    public void doSetupSpec() {
        ClassPathResource resource = new ClassPathResource("/com/rackspace/idm/api/resource/cloud/v20/keys");
        resource.exists()
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml")
    }
    
    def "user can replace their mobile phone device when mfa is disabled"() {
        setup:
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()
        def userAdminToken = utils.getToken(userAdmin.username)
        def userScopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken)
        def originalPhone = setUpAndEnableMultiFactor(userAdminToken, userAdmin)
        def phoneToAdd = v2Factory.createMobilePhone()
        resetTokenExpiration(userScopeAccess)

        when: "try to replace the device on the user with mfa enabled"
        def response = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, phoneToAdd)

        then: "an error is returned"
        response.status == 400

        and: "the phone sent in the request was not added to the directory"
        mobilePhoneRepository.getByTelephoneNumber(phoneToAdd.number) == null

        when: "disable mfa and try to replace the phone"
        utils.updateMultiFactor(userAdminToken, userAdmin.id, v2Factory.createMultiFactorSettings(false))
        response = cloud20.addPhoneToUser(userAdminToken, userAdmin.id, phoneToAdd)

        then: "the request was successful"
        response.status == 201

        and: "the original phone was deleted from the directory"
        mobilePhoneRepository.getByTelephoneNumber(originalPhone.number) == null

        and: "the user's phone-related attributes have been cleared out"
        def userEntity = userService.getProvisionedUserById(userAdmin.id)
        !userEntity.multiFactorDeviceVerified
        userEntity.multiFactorDevicePin == null
        userEntity.multiFactorDevicePinExpiration == null

        and: "the new phone is now in the directory with the user as a member"
        def phoneEntity = mobilePhoneRepository.getByTelephoneNumber(phoneToAdd.number)
        phoneEntity != null
        phoneEntity.members.contains(userEntity.getUniqueId())

        cleanup:
        utils.deleteUsers(users)
    }

    def "user can replace their mobile phone device when mfa is enabled for OTP"() {
        setup:
        def users
        def userAdmin
        (userAdmin, users) = utils.createUserAdmin()
        String userAdminToken = utils.getToken(userAdmin.username) //pwdToken

        OTPDevice device = utils.setUpAndEnableUserForMultiFactorOTP(userAdminToken, userAdmin)
        userAdminToken = utils.authenticateWithOTPDevice(userAdmin, device)

        when: "add a verified phone to the user"
        MobilePhone phone1 = utils.addVerifiedMobilePhoneToUser(userAdminToken, userAdmin)

        then: "can add while MFA OTP is enabled"
        phone1 != null

        when: "replace the phone with a new one"
        MobilePhone phone2 = utils.addVerifiedMobilePhoneToUser(userAdminToken, userAdmin)

        then: "can replace while MFA OTP is enabled"
        phone2 != null

        cleanup:
        utils.deleteUsers(users)
    }

    def "unlinked device is deleted if user replacing device is last owner of device"() {
        setup:
        def users1
        def users2
        def userAdmin1, userAdmin2
        (userAdmin1, users1) = utils.createUserAdmin()
        (userAdmin2, users2) = utils.createUserAdmin()
        def userAdminToken1 = utils.getToken(userAdmin1.username)
        def userScopeAccess1 = scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken1)
        def originalPhoneNumber = PhoneNumberGenerator.randomUSNumberAsString()
        setUpAndEnableMultiFactor(userAdminToken1, userAdmin1, originalPhoneNumber)
        resetTokenExpiration(userScopeAccess1)
        def userAdminToken2 = utils.getToken(userAdmin2.username)
        def userScopeAccess2 = scopeAccessRepository.getScopeAccessByAccessToken(userAdminToken2)
        setUpAndEnableMultiFactor(userAdminToken2, userAdmin2, originalPhoneNumber)
        resetTokenExpiration(userScopeAccess2)
        def phoneToAdd = v2Factory.createMobilePhone()

        when: "disable mfa and replace the phone on the first user"
        utils.updateMultiFactor(userAdminToken1, userAdmin1.id, v2Factory.createMultiFactorSettings(false))
        def response = cloud20.addPhoneToUser(userAdminToken1, userAdmin1.id, phoneToAdd)

        then: "the request was successful"
        response.status == 201

        and: "the original phone was not deleted from the directory but only contains the other user"
        def phoneEntity = mobilePhoneRepository.getByTelephoneNumber(originalPhoneNumber)
        def userAdminEntity1 = userService.getProvisionedUserById(userAdmin1.id)
        def userAdminEntity2 = userService.getProvisionedUserById(userAdmin2.id)
        phoneEntity != null
        !phoneEntity.members.contains(userAdminEntity1.getUniqueId())
        phoneEntity.members.contains(userAdminEntity2.getUniqueId())

        when: "disable mfa and replace the phone on the other user"
        utils.updateMultiFactor(userAdminToken2, userAdmin2.id, v2Factory.createMultiFactorSettings(false))
        response = cloud20.addPhoneToUser(userAdminToken2, userAdmin2.id, phoneToAdd)

        then: "the request was successful"
        response.status == 201

        and: "the phone was deleted from the directory"
        mobilePhoneRepository.getByTelephoneNumber(originalPhoneNumber) == null

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
    }

    def setUpAndEnableMultiFactor(token, user, phoneNumber = PhoneNumberGenerator.randomUSNumberAsString()) {
        def phone = setUpMultiFactorWithoutEnable(token, user, phoneNumber)
        utils.updateMultiFactor(token, user.id, v2Factory.createMultiFactorSettings(true))
        return phone
    }

    def setUpMultiFactorWithoutEnable(token, user, phoneNumber) {
        def phone = setUpMultiFactorWithUnverifiedPhone(token, user, phoneNumber)
        utils.sendVerificationCodeToPhone(token, user.id, phone.id)
        def constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
        utils.verifyPhone(token, user.id, phone.id, constantVerificationCode)
        return phone
    }

    def setUpMultiFactorWithUnverifiedPhone(token, user, phoneNumber) {
        def phone
        if(phoneNumber != null) {
            phone = utils.addPhone(token, user.id, v2Factory.createMobilePhone(phoneNumber))
        } else {
            phone = utils.addPhone(token, user.id)
        }
        return phone
    }

    def void resetTokenExpiration(scopeAccessToReset) {
        Date now = new Date()
        Date future = new Date(now.year + 1, now.month, now.day)
        scopeAccessToReset.setAccessTokenExp(future)
        scopeAccessRepository.updateScopeAccess(scopeAccessToReset)
    }

}
