package com.rackspace.idm.domain.dao.impl

import com.unboundid.ldap.sdk.Filter
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 19/12/12
 * Time: 15:01
 * To change this template use File | Settings | File Templates.
 */
class LdapApplicationRoleRepositoryTest extends Specification {

    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom
    @Shared Configuration config
    @Shared LdapApplicationRoleRepository repo

    @Shared defaultWeightFilterString = "(&(objectClass=clientRole)(rsWeight>=2000))"
    @Shared userAdminWeightFilterString = "(&(objectClass=clientRole)(rsWeight>=1000))"
    @Shared specialWeightFilterString = "(&(objectClass=clientRole)(rsWeight>=500))"
    @Shared adminWeightFilterString = "(&(objectClass=clientRole)(rsWeight>=100))"
    @Shared serviceAdminWeightFilterString = "(&(objectClass=clientRole)(rsWeight>=0))"

    def setupSpec() {
        repo = new LdapApplicationRoleRepository()
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        config = Mock()
        repo.config = config
    }

    def "searchFilter_availableClientRoles returns appropriate search filter"() {
        when:
        def defaultWeightFilter = repo.searchFilter_availableClientRoles(2000)
        def userAdminWeightFilter = repo.searchFilter_availableClientRoles(1000)
        def specialWeightFilter = repo.searchFilter_availableClientRoles(500)
        def adminWeightFilter = repo.searchFilter_availableClientRoles(100)
        def serviceAdminWeightFilter = repo.searchFilter_availableClientRoles(0)

        then:
        defaultWeightFilter.toString().equals(defaultWeightFilterString)
        userAdminWeightFilter.toString().equals(userAdminWeightFilterString)
        specialWeightFilter.toString().equals(specialWeightFilterString)
        adminWeightFilter.toString().equals(adminWeightFilterString)
        serviceAdminWeightFilter.toString().equals(serviceAdminWeightFilterString)
    }

    def "searchFilter by applicationId and available returns filter"() {
        when:
        def filter = repo.searchFilter_availableRolesByApplicationId("applicationId", 2000)

        then:
        filter.toString().equals("(&(objectClass=clientRole)(clientId=applicationId)(rsWeight>=2000))")
    }
}
