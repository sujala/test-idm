package com.rackspace.idm.api.resource.cloud.v20
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

/**
 * Tests the multifactor feature and beta flags. In order to accomplish this, the grizzly container must
 * be restarted in order to be placed into the different states for these flags. The states that we are
 * interested in testing here are:
 *     1)  Multifactor Services are turned OFF
 *         Mfa services flag = off
 *         Mfa beta flag = off
 *     2)  Multifactor Services are turned ON and in Beta
 *         Mfa services flag = on
 *         Mfa beta flag = on
 *     3)  Multifactor Services are in ON and NOT in BETA
 *         Mfa services flag = on
 *         Mfa beta flag = off
 * These settings are configured using a Spring BeanPostProcessor that overrides these settings from their
 * default values.
 */
@ContextConfiguration(locations = ["classpath:app-config.xml",
    "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml"])
class MultifactorFeatureFlagIntegrationTest extends RootConcurrentIntegrationTest {

    def static OFF_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagOff.xml"
    def static FULL_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagFull.xml"
    def static BETA_SETTINGS_FILE = "classpath:com/rackspace/idm/multifactor/config/MultifactorFeatureFlagBeta.xml"

    @Autowired BasicMultiFactorService multiFactorService;

    @Autowired LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired Configuration config

    @Autowired SimulatorMobilePhoneVerification simulatorMobilePhoneVerification;

    @Autowired ScopeAccessService scopeAccessService

    @Autowired LdapScopeAccessRepository scopeAccessRepository

    @Autowired RoleService roleService

    @Override
    public void doCleanupSpec() {
        stopGrizzly()
    }

    @Unroll("MFA feature flag for enable MFA API call: requestContentType=#requestContentMediaType ; acceptMediaType=#acceptMediaType ; addMfaRole=#addMfaRole ; flagSettingsFile=#flagSettingsFile")
    def "multifactor feature flag works for enable MFA for user call"() {
        setup:
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml " +
                flagSettingsFile)
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        if(addMfaRole) {
            def mfaBetaRole = roleService.getRoleByName(config.getString("cloudAuth.multiFactorBetaRoleName"))
            cloud20.addUserRole(utils.getServiceAdminToken(), user.id, mfaBetaRole.id)
        }
        def responsePhone
        if(addPhone) {
            responsePhone = addPhone(token, user)
        }

        when:
        def response = cloud20.updateMultiFactorSettings(token, user.id, settings, requestContentMediaType, acceptMediaType)

        then:
        response.status == status

        cleanup:
        if (user != null) {
            if (multiFactorService.removeMultiFactorForUser(user.id))  //remove duo profile
                deleteUserQuietly(user)
        }
        if (responsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhone.getId()))

        where:
        requestContentMediaType | acceptMediaType | addMfaRole | addPhone | flagSettingsFile | status
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | false | OFF_SETTINGS_FILE | 404

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | true | FULL_SETTINGS_FILE | 204

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | false | BETA_SETTINGS_FILE | 401

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true | true | BETA_SETTINGS_FILE | 204
    }

    @Unroll("MFA feature flag for delete MFA API call: requestContentType=#requestContentMediaType ; acceptMediaType=#acceptMediaType ; addMfaRole=#addMfaRole ; flagSettingsFile=#flagSettingsFile")
    def "multifactor feature flag works for delete MFA for user call"() {
        setup:
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml " +
                flagSettingsFile)
        def settings = v2Factory.createMultiFactorSettings(true)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        if(addMfaRole) {
            def role = roleService.getRoleByName(config.getString("cloudAuth.multiFactorBetaRoleName"))
            cloud20.addUserRole(utils.getServiceAdminToken(), user.id, role.id)
        }
        def responsePhone
        if(addPhone) {
            responsePhone = addPhone(token, user)
        }
        if(enableMfa) {
            cloud20.updateMultiFactorSettings(token, user.id, settings, requestContentMediaType, acceptMediaType)
            resetTokenExpiration(token)
        }

        when:
        def response = cloud20.deleteMultiFactor(token, user.id, requestContentMediaType, acceptMediaType)

        then:
        response.status == status

        cleanup:
        if (user != null) {
            if (multiFactorService.removeMultiFactorForUser(user.id))  //remove duo profile
                deleteUserQuietly(user)
        }
        if (responsePhone != null) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhone.getId()))

        where:
        requestContentMediaType | acceptMediaType | addMfaRole | addPhone | enableMfa | flagSettingsFile | status
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | false | false | OFF_SETTINGS_FILE | 404

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | true | true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | true | true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | true | true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | true | true | FULL_SETTINGS_FILE | 204

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | false | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | false | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | false | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | false | false | BETA_SETTINGS_FILE | 401

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | true | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | true | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true | true | true | BETA_SETTINGS_FILE | 204
    }

    @Unroll("MFA feature flag for add phone to user API call: requestContentType=#requestContentMediaType ; acceptMediaType=#acceptMediaType ; addMfaRole=#addMfaRole ; flagSettingsFile=#flagSettingsFile")
    def "multifactor feature flag works for add phone to user call"() {
        setup:
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml " +
                flagSettingsFile)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        if(addMfaRole) {
            def role = roleService.getRoleByName(config.getString("cloudAuth.multiFactorBetaRoleName"))
            cloud20.addUserRole(utils.getServiceAdminToken(), user.id, role.id)
        }

        when:
        def response = cloud20.addPhoneToUser(token, user.id, v2Factory.createMobilePhone())

        then:
        response.status == status

        cleanup:
        if (user != null) {
            if (multiFactorService.removeMultiFactorForUser(user.id))  //remove duo profile
                deleteUserQuietly(user)
        }

        where:
        requestContentMediaType | acceptMediaType | addMfaRole | flagSettingsFile | status
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | OFF_SETTINGS_FILE | 404

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | FULL_SETTINGS_FILE | 201
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | FULL_SETTINGS_FILE | 201
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | FULL_SETTINGS_FILE | 201
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | FULL_SETTINGS_FILE | 201

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | BETA_SETTINGS_FILE | 401

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | true | BETA_SETTINGS_FILE | 201
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | true | BETA_SETTINGS_FILE | 201
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true | BETA_SETTINGS_FILE | 201
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true | BETA_SETTINGS_FILE | 201
    }

    @Unroll("MFA feature flag for send verification code API call: requestContentType=#requestContentMediaType ; acceptMediaType=#acceptMediaType ; addMfaRole=#addMfaRole ; flagSettingsFile=#flagSettingsFile")
    def "multifactor feature flag works for send verification call"() {
        setup:
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml " +
                flagSettingsFile)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        if(addMfaRole) {
            def role = roleService.getRoleByName(config.getString("cloudAuth.multiFactorBetaRoleName"))
            cloud20.addUserRole(utils.getServiceAdminToken(), user.id, role.id)
        }
        def responsePhoneId
        if(addPhone) {
            responsePhoneId = utils.addPhone(token, user.id).id
        } else {
            responsePhoneId = ""
        }

        when:
        def response = cloud20.sendVerificationCode(token, user.id, responsePhoneId)

        then:
        response.status == status

        cleanup:
        if (user != null) {
            if (multiFactorService.removeMultiFactorForUser(user.id))  //remove duo profile
                deleteUserQuietly(user)
        }
        if (!responsePhoneId.isEmpty()) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhoneId))

        where:
        requestContentMediaType | acceptMediaType | addMfaRole | addPhone | flagSettingsFile | status
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | false | OFF_SETTINGS_FILE | 404

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false |  true | FULL_SETTINGS_FILE | 202
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false |  true | FULL_SETTINGS_FILE | 202
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false |  true | FULL_SETTINGS_FILE | 202
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false |  true | FULL_SETTINGS_FILE | 202

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false |  false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false |  false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false |  false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false |  false | BETA_SETTINGS_FILE | 401

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | true |  true | BETA_SETTINGS_FILE | 202
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | true |  true | BETA_SETTINGS_FILE | 202
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true |  true | BETA_SETTINGS_FILE | 202
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true |  true | BETA_SETTINGS_FILE | 202
    }

    @Unroll("MFA feature flag for verify MFA phone API call: requestContentType=#requestContentMediaType ; acceptMediaType=#acceptMediaType ; addMfaRole=#addMfaRole ; flagSettingsFile=#flagSettingsFile")
    def "multifactor feature flag works for verify MFA phone"() {
        setup:
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/multifactor/providers/simulator/SimulatorMobilePhoneVerification-context.xml " +
                flagSettingsFile)
        def user = createUserAdmin()
        def token = authenticate(user.username)
        if(addMfaRole) {
            def role = roleService.getRoleByName(config.getString("cloudAuth.multiFactorBetaRoleName"))
            cloud20.addUserRole(utils.getServiceAdminToken(), user.id, role.id)
        }
        def responsePhoneId
        if(addPhone) {
            responsePhoneId = addPhone(token, user, false).id
        } else {
            responsePhoneId = ""
        }

        when:
        def verificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
        def response = cloud20.verifyVerificationCode(token, user.id, responsePhoneId, verificationCode)

        then:
        response.status == status

        cleanup:
        if (user != null) {
            if (multiFactorService.removeMultiFactorForUser(user.id))  //remove duo profile
                deleteUserQuietly(user)
        }
        if (responsePhoneId != null && !responsePhoneId.isEmpty()) mobilePhoneRepository.deleteObject(mobilePhoneRepository.getById(responsePhoneId))

        where:
        requestContentMediaType | acceptMediaType | addMfaRole | addPhone | flagSettingsFile | status
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | false | OFF_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | false | OFF_SETTINGS_FILE | 404

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false |  true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false |  true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false |  true | FULL_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false |  true | FULL_SETTINGS_FILE | 204

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false |  false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false |  false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false |  false | BETA_SETTINGS_FILE | 401
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false |  false | BETA_SETTINGS_FILE | 401

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | true |  true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | true |  true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true |  true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true |  true | BETA_SETTINGS_FILE | 204
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
