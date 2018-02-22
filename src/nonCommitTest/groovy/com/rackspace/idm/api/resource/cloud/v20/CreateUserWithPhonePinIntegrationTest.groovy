package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class CreateUserWithPhonePinIntegrationTest extends RootIntegrationTest {

    @Shared
    def identityAdmin, userAdmin, userManage, defaultUser
    @Shared
    def domainId

    @Autowired
    UserService userService

    @Unroll
    def "Create identityAdmin, userAdmin, userManage, defaultUser with phone PIN - accept == #accept, featureEnabled == #featureEnabled" () {
        given:
        def domainId = utils.createDomain()
        def pinLength = 4

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER, featureEnabled)
        reloadableConfiguration.setProperty(IdentityConfig.USER_PHONE_PIN_SIZE, pinLength)

        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def identityAdminUserEntity = userService.getUserById(userAdmin.id)
        def userAdminUserEntity = userService.getUserById(userAdmin.id)
        def userManageUserEntity = userService.getUserById(userAdmin.id)
        def defaultUserUserEntity = userService.getUserById(userAdmin.id)

        then:

        if (featureEnabled) {
            identityAdminUserEntity.phonePin != null
            identityAdminUserEntity.encryptedPhonePin != null
            identityAdminUserEntity.phonePin.size() == pinLength
            identityAdminUserEntity.phonePin.isNumber()

            userAdminUserEntity.phonePin != null
            userAdminUserEntity.encryptedApiKey != null
            userAdminUserEntity.phonePin.size() == pinLength
            userAdminUserEntity.phonePin.isNumber()

            userManageUserEntity.phonePin != null
            userManageUserEntity.encryptedApiKey != null
            userManageUserEntity.phonePin.size() == pinLength
            userManageUserEntity.phonePin.isNumber()

            defaultUserUserEntity.phonePin != null
            defaultUserUserEntity.encryptedApiKey != null
            defaultUserUserEntity.phonePin.size() == pinLength
            defaultUserUserEntity.phonePin.isNumber()
        } else {
            identityAdminUserEntity.phonePin == null
            userAdminUserEntity.phonePin == null
            userManageUserEntity.phonePin == null
            defaultUserUserEntity.phonePin == null
        }

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        [accept, featureEnabled] << [[MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE], [true, false]].combinations()
    }

    @Unroll
    def "Create userAdmin with phone PIN of different length - pinLength == #pinLength" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER, true)
        reloadableConfiguration.setProperty(IdentityConfig.USER_PHONE_PIN_SIZE, pinLength)

        def user = utils.createCloudAccount()

        when:
        def userEntity = userService.getUserById(user.id)

        then:

        if (pinLength == 10) {
            userEntity.phonePin != null
            userEntity.encryptedApiKey != null
            userEntity.phonePin.size() == pinLength
            userEntity.phonePin.isNumber()
        } else if (pinLength == 4) {
            userEntity.phonePin != null
            userEntity.encryptedApiKey != null
            userEntity.phonePin.size() == pinLength
            userEntity.phonePin.isNumber()
        } else if (pinLength == 0) {
            userEntity.phonePin != null
            userEntity.encryptedApiKey != null
            userEntity.phonePin.size() == pinLength
            userEntity.phonePin.isEmpty()
        }

        cleanup:
        utils.deleteUser(user)

        where:
        [pinLength] << [[10,4,0]].combinations()
    }
}

