package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser
import com.rackspace.idm.Constants

import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Tests to verify that when a user is deleted through the API,
 * the deleted user's MFA data is also deleted in Duo.
 */
class MultiFactorDeleteUserTest extends RootConcurrentIntegrationTest {

    @Autowired BasicMultiFactorService multiFactorService;

    @Autowired MobilePhoneDao mobilePhoneRepository;

    @Autowired Configuration config

    @Autowired ScopeAccessService scopeAccessService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired UserService userService

    @Autowired UserManagement<DuoUser, DuoPhone> userManagement;


    def "delete MFA user through v2.0 user delete call deletes user's Duo account"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        def userEntity = userService.getUserById(user.id)

        when:
        def response = cloud20.deleteUser(utils.getServiceAdminToken(), user.id)

        then:
        response.status == 204
        Mockito.verify(userManagement.mock).deleteUserById(Constants.MFA_DEFAULT_USER_PROVIDER_ID)

        cleanup:
        mockMobilePhoneVerification.reset()
    }

    def "delete non-MFA user does not return an error through v2.0 delete user call"() {
        given:
        def user = createUserAdmin()

        when:
        def response = cloud20.deleteUser(utils.getServiceAdminToken(), user.id)

        then:
        response.status == 204
    }

    def "delete MFA user through v1.1 user delete call deletes user's Duo account"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        def userEntity = userService.getUserById(user.id)

        when:
        def response = cloud20.deleteUser(utils.getServiceAdminToken(), user.id)

        then:
        response.status == 204
        Mockito.verify(userManagement.mock, Mockito.atLeastOnce()).deleteUserById(Constants.MFA_DEFAULT_USER_PROVIDER_ID)

        cleanup:
        mockMobilePhoneVerification.reset()
    }

    def addPhone(token, user, verify=true) {
        def responsePhone = utils.addPhone(token, user.id)
        utils.sendVerificationCodeToPhone(token, user.id, responsePhone.id)
        if(verify) {
            def constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN);
            utils.verifyPhone(token, user.id, responsePhone.id, constantVerificationCode)
        }
        responsePhone
    }

    def void resetTokenExpiration(tokenString) {
        Date now = new Date()
        Date future = new Date(now.year + 1, now.month, now.day)
        def userScopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenString)
        userScopeAccess.setAccessTokenExp(future)
        scopeAccessRepository.updateScopeAccess(userScopeAccess)
    }

}
