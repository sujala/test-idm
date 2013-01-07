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

    @Shared defaultWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=2000)))"
    @Shared userAdminWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=2000)(rsWeight=1000)))"
    @Shared specialWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=2000)(rsWeight=1000)(rsWeight=500)))"
    @Shared adminWeightFilterString =  "(&(objectClass=clientRole)(|(rsWeight=2000)(rsWeight=1000)(rsWeight=500)(rsWeight=100)))"
    @Shared serviceAdminWeightFilterString = "(&(objectClass=clientRole)(|(rsWeight=2000)(rsWeight=1000)(rsWeight=500)(rsWeight=100)(rsWeight=0)))"

    def setupSpec() {
        repo = new LdapApplicationRoleRepository()
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        config = Mock()
        repo.config = config

        config.getInt("cloudAuth.defaultUser.rsWeight") >> 2000
        config.getInt("cloudAuth.userAdmin.rsWeight") >> 1000
        config.getInt("cloudAuth.special.rsWeight") >> 500
        config.getInt("cloudAuth.admin.rsWeight") >> 100
        config.getInt("cloudAuth.serviceAdmin.rsWeight") >> 0
    }

    def "getting roles weights returns all default available weights"() {
        when:
        def weights = repo.getRoleWeights()

        then:
        weights.size() == 5
        weights.contains(2000)
        weights.contains(1000)
        weights.contains(500)
        weights.contains(100)
        weights.contains(0)
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
        listThree.size() == 3
        listFour.size() == 4
        listFive.size() == 5
    }

    def "searchFilter by applicationId and available returns filter"() {
        when:
        def filter = repo.searchFilter_availableRolesByApplicationId("applicationId", 2000)

        then:
        filter.toString().equals("(&(objectClass=clientRole)(clientId=applicationId)(|(rsWeight=2000)))")
    }
}
