package com.rackspace.idm.domain.dao.impl

import org.junit.Ignore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
@ActiveProfiles("SQL")
@Ignore
class SqlBypassDeviceRepositoryTest extends Specification {

    @Autowired
    SqlBypassDeviceRepository sqlBypassDeviceRepository

    // FIXME: Finish the test when Jenkins has MariaDB docker running.
    def "test"() {
        given:
        def a = ""
        when:
        a + ""

        then:
        sqlBypassDeviceRepository != null
    }

}
