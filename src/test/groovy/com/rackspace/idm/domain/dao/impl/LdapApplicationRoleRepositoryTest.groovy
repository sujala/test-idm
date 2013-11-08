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

    @Shared defaultWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=2500)))"
    @Shared userAdminWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=2000)(rsWeight=2500)))"
    @Shared specialWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=750)(rsWeight=900)(rsWeight=1000)(rsWeight=2000)(rsWeight=2500)))"
    @Shared adminWeightFilterString =  "(&(objectClass=clientRole)(|(rsWeight=500)(rsWeight=750)(rsWeight=900)(rsWeight=1000)(rsWeight=2000)(rsWeight=2500)))"
    @Shared serviceAdminWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=100)(rsWeight=500)(rsWeight=750)(rsWeight=900)(rsWeight=1000)(rsWeight=2000)(rsWeight=2500)))"

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

    def "getting role weights or filter returns or filter"() {
        when:
        def listOne = repo.getRoleWeightsOrFilter(2000)
        def listTwo = repo.getRoleWeightsOrFilter(1000)
        def listThree = repo.getRoleWeightsOrFilter(500)
        def listFour = repo.getRoleWeightsOrFilter(100)
        def listFive = repo.getRoleWeightsOrFilter(0)

        then:
        listOne.size() == 1
        listTwo.size() == 2
        listThree.size() == 5
        listFour.size() == 6
        listFive.size() == 7
    }

    def "searchFilter by applicationId and available returns filter"() {
        when:
        def filter = repo.searchFilter_availableRolesByApplicationId("applicationId", 2000)

        then:
        filter.toString().equals("(&(objectClass=clientRole)(clientId=applicationId)(rsWeight>=2000))")
    }
}
