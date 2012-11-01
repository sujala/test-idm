package com.rackspace.idm.domain.config

import org.dozer.Mapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class ExternalBeansConfigurationTest extends Specification {

    @Autowired
    Mapper mapper

    def "dozer bean is being autowried"() {
        expect:
        mapper != null
    }
}
