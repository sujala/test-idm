package com.rackspace.idm.domain.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultPropertiesServiceIntegrationTest extends Specification {
    @Autowired
    DefaultPropertiesService service

    def "can get string property"() {
        when:
        String versionId = service.getValue("encryptionVersionId")

        then:
        versionId != null
    }

    def "calling get property returns null if not found"() {
        when:
        String notFound = service.getValue("notfoundproperty")

        then:
        notFound == null
    }
}
