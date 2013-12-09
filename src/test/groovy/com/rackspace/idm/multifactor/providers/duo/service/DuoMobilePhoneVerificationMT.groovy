package com.rackspace.idm.multifactor.providers.duo.service

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.idm.domain.config.PropertyFileConfiguration
import com.rackspace.idm.multifactor.domain.Pin
import com.rackspace.idm.multifactor.providers.MobilePhoneVerification
import org.apache.commons.configuration.Configuration
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

/**
 * This is a manual integration test between the idm duo provider integration and Duo Security. It expects the appropriate IDM configuration to be loadable
 * via the standard PropertyFileConfiguration. This method of loading the configuration information is used to allow the sensitive integration keys to
 * be encrypted via the standard mechanism in the idm.secrets file.
 *
 * <b>The test is a Manual Test (MT) with the @Ignore annotation because it causes a REAL SMS message to be sent to a REAL phone which costs REAL money for rackspace.</b>
 */
@Ignore
class DuoMobilePhoneVerificationMT extends Specification{

    /**
     * This property must be populated with a valid telephone number (e.g. 5126667777) before running this test.
     */
    private static final String TEST_PHONE = "";

    @Shared PropertyFileConfiguration pfConfig;

    @Shared Phonenumber.PhoneNumber phoneToText = PhoneNumberUtil.getInstance().parse(TEST_PHONE, "US")

    def setupSpec() {
       pfConfig = new PropertyFileConfiguration();
    }

    def "send verification code returns 4 digit pin"() {
        MobilePhoneVerification mobilePhoneVerification = new DuoMobilePhoneVerification(getDefaultConfiguration());

        when: "request pin to be sent"
            Pin pin  = mobilePhoneVerification.sendPin(phoneToText)

        then: "returns 4 digit numeric pin"
            assertPinValid(pin)
    }

    def "send verification with special characters"() {
        String specialMessage = "-_.`!@#\$%^&*()=+|[]{}';:<>?\\,\\ ~ <pin>"; //note - <pin> must be part of the message or duo will reject it
        Configuration config = getDefaultConfiguration();
        config.setProperty(DuoMobilePhoneVerification.PHONE_VERIFICATION_MESSAGE_PROP_NAME, specialMessage)

        MobilePhoneVerification mobilePhoneVerification = new DuoMobilePhoneVerification(config);

        when: "request pin to be sent"
            Pin pin = mobilePhoneVerification.sendPin(phoneToText)

        then: "returns 4 digit numeric pin"
            assertPinValid(pin)
    }

    def void assertPinValid(Pin pin) {
        assert pin.getPin().length() == 4
        assert pin.getPin().toInteger() >= 0
    }

    def Configuration getDefaultConfiguration() {
        Configuration testConfiguration = pfConfig.getConfig();
         return testConfiguration;
    }
}
