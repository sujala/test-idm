package com.rackspace.idm.domain.service.impl

import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultTenantServiceIntegrationTest extends Specification {

    @Autowired
    DefaultTenantService defaultTenantService
}
