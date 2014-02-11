package com.rackspace.idm.api.resource.cloud.v20
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.multifactor.providers.simulator.SimulatorMobilePhoneVerification
import com.rackspace.idm.multifactor.service.BasicMultiFactorService
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly

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

    @Override
    public void doCleanupSpec() {
        startOrRestartGrizzly("classpath:app-config.xml")
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
            cloud20.addUserRole(utils.getServiceAdminToken(), user.id, config.getString("cloudAuth.multiFactorBetaRoleRsId"))
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

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | false | false | BETA_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | false | false | BETA_SETTINGS_FILE | 404
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | false | false | BETA_SETTINGS_FILE | 404
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | false | false | BETA_SETTINGS_FILE | 404

        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE    | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE   | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE   | true | true | BETA_SETTINGS_FILE | 204
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE    | true | true | BETA_SETTINGS_FILE | 204
    }

    def void addPhone(token, user) {
        def responsePhone = utils.addPhone(token, user.id)
        utils.sendVerificationCodeToPhone(token, user.id, responsePhone.id)
        def constantVerificationCode = v2Factory.createVerificationCode(simulatorMobilePhoneVerification.constantPin.pin);
        utils.verifyPhone(token, user.id, responsePhone.id, constantVerificationCode)
        responsePhone
    }

}
