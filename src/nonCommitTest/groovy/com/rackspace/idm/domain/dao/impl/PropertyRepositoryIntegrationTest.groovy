package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.PropertyDao
import com.rackspace.idm.domain.entity.Property
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class PropertyRepositoryIntegrationTest extends Specification {
    @Autowired
    PropertyDao propertyDao

    def "can read some property"() {
        when:
        Property property = propertyDao.getProperty("encryptionVersionId")

        then:
        property != null
    }
}
