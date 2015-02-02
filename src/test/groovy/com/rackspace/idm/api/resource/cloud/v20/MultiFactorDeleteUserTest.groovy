package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.identity.multifactor.providers.UserManagement
import com.rackspace.identity.multifactor.providers.duo.domain.DuoPhone
import com.rackspace.identity.multifactor.providers.duo.domain.DuoUser
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly

/**
 * Tests to verify that when a user is deleted through the API,
 * the deleted user's MFA data is also deleted in Duo.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml",
    "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class MultiFactorDeleteUserTest extends RootConcurrentIntegrationTest {

    @Autowired BasicMultiFactorService multiFactorService;

    @Autowired LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired Configuration config

    @Autowired SimulatorMobilePhoneVerification simulatorMobilePhoneVerification;

    @Autowired ScopeAccessService scopeAccessService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao scopeAccessRepository

    @Autowired UserService userService

    @Autowired UserManagement<DuoUser, DuoPhone> userManagement;

    @Override
    public void doSetupSpec() {
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml")
    }

    def "delete MFA user through v2.0 user delete call deletes user's Duo account"() {
        given:
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        def responsePhone = addPhone(token, user)
        cloud20.updateMultiFactorSettings(token, user.id, settings)
        def userEntity = userService.getUserById(user.id)
        resetTokenExpiration(token)

        when:
        def response = cloud20.deleteUser(utils.getServiceAdminToken(), user.id)

        then:
        response.status == 204
        userManagement.getUserById(userEntity.getExternalMultiFactorUserId()) == null
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
        resetTokenExpiration(token)

        when:
        def response = cloud11.deleteUser(user.username)

        then:
        response.status == 204
        userManagement.getUserById(userEntity.getExternalMultiFactorUserId()) == null
    }

    def "delete non-MFA user does not return an error through v1.1 delete user call"() {
        given:
        def user = createUserAdmin()

        when:
        def response = cloud11.deleteUser(user.username)

        then:
        response.status == 204
    }

    def addPhone(token, user, verify=true) {
        def responsePhone = utils.addPhone(token, user.id)
        utils.sendVerificationCodeToPhone(token, user.id, responsePhone.id)
        if(verify) {
            def constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
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
