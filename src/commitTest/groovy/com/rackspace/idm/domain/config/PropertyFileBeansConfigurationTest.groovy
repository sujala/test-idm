package com.rackspace.idm.domain.config

import org.apache.commons.configuration.Configuration
import org.jasypt.encryption.StringEncryptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [PropertyFileConfiguration.class])
class PropertyFileBeansConfigurationTest extends Specification {

    @Autowired
    Configuration configuration

    @Autowired
    StringEncryptor stringEncryptor;

    def "Configuration is being autowired"() {
        expect:
        configuration != null
        configuration instanceof Configuration
    }

    def "String encryptor is being autowired"() {
        expect:
        stringEncryptor != null
    }

}
