package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspacecloud.docs.auth.api.v1.User
import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultTenantServiceIntegrationTest extends Specification {

    @Autowired
    DefaultTenantService defaultTenantService

    def setupSpec(){
    }

    def "tenant roles for scopeAccess with null scopeAccess returns IllegalState" () {
        when:
        defaultTenantService.getTenantRolesForScopeAccess(null)

        then:
        thrown(IllegalStateException)
    }

    def "delete Tenant role For Application with null user returns IllegalState" () {
        given:
        Application application = new Application()

        when:
        defaultTenantService.deleteTenantRoleForApplication(application, null)

        then:
        thrown(IllegalStateException)
    }

    def "delete Tenant role For Application with null application returns IllegalState" () {
        given:
        TenantRole tenantRole = new TenantRole()

        when:
        defaultTenantService.deleteTenantRoleForApplication(null, tenantRole)

        then:
        thrown(IllegalStateException)
    }
}
