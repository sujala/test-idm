package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Tenant
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Shared
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

    @Autowired
    LdapTenantRepository repo;

    @Autowired
    Configuration config;

    def cleanup() {
        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_DEFAULT_VALUE)
    }

    def "Get paged or unpaged as appropriate" () {
        given:
        def startCount = getNumberOfPreExistingTenants()

        def totalWantedForLessThanPage = LdapPagingIterator.PAGE_SIZE - 1
        def numToCreateForLessThanPage = totalWantedForLessThanPage - startCount
        assert numToCreateForLessThanPage >= 0

        def totalWantedForEqualPage = LdapPagingIterator.PAGE_SIZE
        def numToCreateForEqualsPage = totalWantedForEqualPage - totalWantedForLessThanPage

        def totalWantedForGreaterThanPage = LdapPagingIterator.PAGE_SIZE + 1
        def numToCreateForGreaterThanPage = totalWantedForGreaterThanPage - totalWantedForEqualPage

        List<Tenant> createdTenants = new ArrayList<Tenant>(totalWantedForGreaterThanPage);

        when: "search result count < page size"
        createdTenants.addAll(addTenants(numToCreateForLessThanPage));

        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, true)
        Iterable<Tenant> lessThanTenantsTrue = repo.getTenants();

        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, false)
        Iterable<Tenant> lessThanTenantsFalse = repo.getTenants();
        List<Tenant> tenantsLessThanPageFalse = new ArrayList<Tenant>(totalWantedForLessThanPage);
        for (Tenant t : lessThanTenantsFalse) {
            tenantsLessThanPageFalse.add(t)
        }


        then: "return LdapPagingIterator when optimization is false, List  when true"
        lessThanTenantsTrue instanceof List
        ((List)lessThanTenantsTrue).size() == totalWantedForLessThanPage

        lessThanTenantsFalse instanceof LdapPagingIterator
        tenantsLessThanPageFalse.size() == totalWantedForLessThanPage

        //equal to page test
        when: "search result count == page size"
        createdTenants.addAll(addTenants(numToCreateForEqualsPage));

        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, true)
        Iterable<Tenant> equalTenantsTrue = repo.getTenants();

        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, false)
        Iterable<Tenant> equalTenantsFalse = repo.getTenants();
        List<Tenant> tenantsEqualsPageFalse = new ArrayList<Tenant>(totalWantedForEqualPage);
        for (Tenant t : equalTenantsFalse) {
            tenantsEqualsPageFalse.add(t)
        }

        then: "return LdapPagingIterator when optimization is false, List when true"
        equalTenantsTrue instanceof List
        ((List)equalTenantsTrue).size() == totalWantedForEqualPage

        equalTenantsFalse instanceof LdapPagingIterator
        tenantsEqualsPageFalse.size() == totalWantedForEqualPage

        when: "search result count > page size"
        createdTenants.addAll(addTenants(numToCreateForGreaterThanPage));

        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, true)
        Iterable<Tenant> greaterThanTenantsTrue = repo.getTenants();
        List<Tenant> tenantsGreaterThanPageTrue = new ArrayList<Tenant>(totalWantedForGreaterThanPage);
        for (Tenant t : greaterThanTenantsTrue) {
            tenantsGreaterThanPageTrue.add(t)
        }

        config.setProperty(LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME, false)
        Iterable<Tenant> greaterThanTenantsFalse = repo.getTenants();
        List<Tenant> tenantsGreaterThanPageFalse = new ArrayList<Tenant>(totalWantedForGreaterThanPage);
        for (Tenant t : greaterThanTenantsFalse) {
            tenantsGreaterThanPageFalse.add(t)
        }

        then: "always returns LdapPagingIterator"
        greaterThanTenantsTrue instanceof LdapPagingIterator
        tenantsGreaterThanPageTrue.size() == totalWantedForGreaterThanPage

        greaterThanTenantsFalse instanceof LdapPagingIterator
        tenantsGreaterThanPageFalse.size() == totalWantedForGreaterThanPage

        cleanup:
        deleteTenants(createdTenants)
    }

    def getNumberOfPreExistingTenants() {
        Integer preExisting = 0
        Iterable<Tenant> existingTenants = repo.getTenants();
        for (Tenant t : existingTenants) {
            preExisting++
        }
        return preExisting
    }

    def List<Tenant> addTenants(int count) {
        List<Tenant> tenantList = new ArrayList<Tenant>(LdapPagingIterator.PAGE_SIZE);
        for (int i=0; i<count; i++) {
            def tenantId = getRandomUUID("tenantId")
            Tenant tenant = getEntityFactory().createTenant(tenantId, tenantId);
            repo.addTenant(tenant)
            tenantList.add(tenant)
        }
        return tenantList;
    }

    def deleteTenants(List<Tenant> tenantList) {
        for (Tenant t : tenantList) {
            repo.deleteObject(t)
        }
    }


}
