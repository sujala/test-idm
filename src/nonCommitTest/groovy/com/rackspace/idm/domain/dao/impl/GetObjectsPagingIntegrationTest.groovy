package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.TenantDao
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

/**
 * Created with IntelliJ IDEA
 * User: jorge
 * Date: 11/19/13
 * Time: 5:01 PM
 * To change this template use File | Settings | File Templates.
 */
@DirtiesContext
class GetObjectsPagingIntegrationTest extends RootIntegrationTest{

    @Shared def defaultUser, users
    @Shared def domainId

    @Autowired TenantDao tenantDao
    @Autowired Configuration config

    def cleanup() {
        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_DEFAULT_VALUE)
    }

    @Unroll
    def "Get paged or unpaged as appropriate: useVlvAndSssControl = #useVlvAndSssControl" () {
        given:
        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, useVlvAndSssControl)
        def totalTenants = tenantDao.getTenantCount()

        when: "set the max page size to one more than the current number of tenants"
        reloadableConfiguration.setProperty(IdentityConfig.MAX_CA_DIRECTORY_PAGE_SIZE_PROP, totalTenants + 1)
        def tenants = tenantDao.getTenants()

        then:
        if (useVlvAndSssControl) {
            assertIterableTypeWithCount(tenants, List, totalTenants)
        } else {
            assertIterableTypeWithCount(tenants, LdapPagingIterator, totalTenants)
        }

        when: "set the max page size to be equal to the total number of tenants"
        reloadableConfiguration.setProperty(IdentityConfig.MAX_CA_DIRECTORY_PAGE_SIZE_PROP, totalTenants)
        tenants = tenantDao.getTenants()

        then:
        if (useVlvAndSssControl) {
            assertIterableTypeWithCount(tenants, List, totalTenants)
        } else {
            assertIterableTypeWithCount(tenants, LdapPagingIterator, totalTenants)
        }

        when: "set the max page size to be equal to 1 minus the total number of tenants"
        reloadableConfiguration.setProperty(IdentityConfig.MAX_CA_DIRECTORY_PAGE_SIZE_PROP, totalTenants - 1)
        tenants = tenantDao.getTenants()

        then:
        assertIterableTypeWithCount(tenants, LdapPagingIterator, totalTenants)

        cleanup:
        reloadableConfiguration.reset()

        where:
        useVlvAndSssControl | _
        true                | _
        false               | _
    }

    def void assertIterableTypeWithCount(Iterable iterator, expectedIteratorType, expectedItemCount) {
        def allItems = iterator.iterator().collect()
        def totalItems = allItems.size()

        //no duplicates in list
        allItems.each {
            allItems.count(it.tenantId) == 1
        }

        assert List.isInstance(iterator) || LdapPagingIterator.isInstance(iterator)
        assert expectedIteratorType.isInstance(iterator)
        assert totalItems > expectedItemCount - 2 && totalItems < expectedItemCount + 2
        assert totalItems == expectedItemCount
    }

    def countEntriesInIterable(Iterable iterator) {
        if (iterator instanceof Collection) {
            return ((Collection)iterator).size()
        }

        def i=0
        for (Object x : iterator) {
            i++
        }
        return i;
    }

}
