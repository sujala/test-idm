package com.rackspace.idm.multifactor.providers.duo.service

import com.rackspace.idm.domain.config.PropertyFileConfiguration
import com.rackspace.idm.multifactor.providers.ProviderAvailability
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

/**
 * This is an integration test between the IDM Duo provider integration with Duo Security to successfully tell whether due is available.
 *
 * Note -  It expects the appropriate IDM configuration to be loadable
 * via the standard PropertyFileConfiguration. This method of loading the configuration information is used to allow the sensitive integration keys to
 * be encrypted via the standard mechanism in the idm.secrets file.
 */
class DuoAvailabilityIntegrationTest extends Specification {

    @Shared ProviderAvailability duoAvailability;

    def setupSpec() {
        PropertyFileConfiguration pfConfig = new PropertyFileConfiguration();
        Configuration devConfiguration = pfConfig.getConfig();
        duoAvailability = new DuoAvailability(devConfiguration)
    }

    def "get duo availability"() {
        expect: "duo ping returns true"
        duoAvailability.available()
    }

}
