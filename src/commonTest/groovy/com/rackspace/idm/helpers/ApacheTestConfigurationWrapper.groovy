package com.rackspace.idm.helpers

import com.google.i18n.phonenumbers.Phonenumber
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil
import org.apache.commons.configuration.Configuration


class ApacheTestConfigurationWrapper {
    private Configuration configuration;

    /**
     * A property that stores the phone number that should be used to send any SMS messages or other authentication information
     */
    public static final String TEST_PHONE_NUMBER_PROP_NAME = "test.phone.number";

    /**
     * whether the tests that will send SMS messages and otherwise cost rackspace money should be enabled. Requires
     * test.phone.number to be set and duo.telephone.enabled=true
     */
    public static final String TEST_RUN_TELEPHONY="test.run.telephony";

    public ApacheTestConfigurationWrapper(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getTestPhoneNumber() {
        return configuration.getString(TEST_PHONE_NUMBER_PROP_NAME, "");
    }

    public Phonenumber.PhoneNumber getStandardizedTestPhoneNumber() {
        return IdmPhoneNumberUtil.getInstance().parsePhoneNumber(getTestPhoneNumber());
    }

    public boolean getRunTelephonyTests() {
        return configuration.getBoolean(TEST_RUN_TELEPHONY, false);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
