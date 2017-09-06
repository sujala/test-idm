package com.rackspace.idm.domain.dao.impl


import com.rackspace.idm.domain.entity.TenantType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapTenantTypeRepositoryIntegrationTest extends Specification {
    @Autowired
    LdapTenantTypeRepository tenantTypeDao

    @Shared
    def name = UUID.randomUUID().toString()

    def "tenantType crud"() {
        given:
        def tenantType = getTenantType(name)

        when:
        tenantTypeDao.addTenantType(tenantType)
        TenantType createdTenantType = tenantTypeDao.getTenantType(name)

        tenantTypeDao.deleteTenantType(createdTenantType)
        TenantType deletedTenantType = tenantTypeDao.getTenantType(name)

        then:
        tenantType == createdTenantType
        deletedTenantType == null
    }

    def getTenantType(name) {
        new TenantType().with {
            it.name = name
            it.description = "description"
            return it
        }
    }

}
