package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Tenant
import com.unboundid.ldap.sdk.ReadOnlyEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/28/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapTenantRepositoryIntegrationTest extends Specification {
    @Autowired
    LdapTenantRepository tenantDao

    @Shared
    def tenantId = UUID.randomUUID().toString()

    def "tenant crud"() {
        given:
        def tenant = getTenant(tenantId)
        def updateTenant = getUpdateTenant(tenantId)

        when:
        tenantDao.addTenant(tenant)
        Tenant createdTenant = tenantDao.getTenant(tenantId)

        updateTenant.uniqueId = createdTenant.uniqueId
        tenantDao.updateTenant(updateTenant);
        Tenant updatedTenant = tenantDao.getTenant(tenantId)

        tenantDao.deleteTenant(tenantId)
        Tenant deletedTenant = tenantDao.getTenant(tenantId)

        then:
        tenant == createdTenant
        updatedTenant == updateTenant
        deletedTenant == null
    }

    def "create tenant without baseUrls or v1Defaults"() {
        given:
        def tenant = getTenant(tenantId)
        tenant.baseUrlIds.clear()
        tenant.v1Defaults.clear()

        when:
        tenantDao.addTenant(tenant)
        Tenant createdTenant = tenantDao.getTenant(tenantId)

        tenantDao.deleteTenant(tenantId)
        Tenant deletedTenant = tenantDao.getTenant(tenantId)

        then:
        tenant == createdTenant
        deletedTenant == null
    }

    def getTenant(tenantId) {
        new Tenant().with {
            it.tenantId = tenantId
            it.enabled = true;
            it.description = "description"
            it.name = "name"
            it.displayName = "displayName"
            it.baseUrlIds.add("1")
            it.baseUrlIds.add("2")
            it.v1Defaults.add("1")
            return it
        }
    }

    def getUpdateTenant(tenantId) {
        new Tenant().with {
            it.tenantId = tenantId
            it.enabled = false;
            it.description = "description2"
            it.name = "name2"
            it.displayName = "displayName2"
            it.baseUrlIds.add("1")
            it.v1Defaults.add("1")
            it.v1Defaults.add("2")
            return it
        }
    }
}
