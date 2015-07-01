package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Policies
import com.rackspace.idm.domain.entity.Policy
import com.rackspace.idm.exception.NotFoundException
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootServiceTest

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapPolicyRepositoryIntegrationTest extends RootServiceTest {
    @Autowired
    LdapPolicyRepository repo

    @Shared def randomness = UUID.randomUUID()
    @Shared random

    @Autowired Configuration config


    def setup() {
        random = ("$randomness").replace('-', "")
    }

    def "getNextId returns UUID"() {
        given:
        def success = false
        repo.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",true)

        when:
        def id = repo.getNextId(LdapRepository.NEXT_POLICY_ID)
        try {
            Long.parseLong(id)
        } catch (Exception) {
            success = true
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "getNextId returns Long"() {
        given:
        def success = false
        repo.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",false)

        when:
        def id = repo.getNextId(LdapRepository.NEXT_POLICY_ID)
        try {
            Long.parseLong(id)
            success = true
        } catch (Exception) {
            //no-op
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "Policy create/ retrieve, delete" () {
        given:
        Policy policy = entityFactory.createPolicy("blob", "Policy$random", "defualt", "policy$random")

        when:
        repo.addPolicy(policy)
        Policy addPolicy = repo.getPolicy(policy.policyId)
        repo.deletePolicy(policy.policyId)
        Policy deletePolicy = repo.getPolicy(policy.policyId)

        then:
        addPolicy.equals(addPolicy)
        deletePolicy == null
    }

    def "Get policy with bad id returns null" () {
        when:
        Policy getPolicy = repo.getPolicy("")

        then:
        getPolicy == null
    }

    def "Get policy by name" () {
        given:
        Policy policy = entityFactory.createPolicy("blob", "Policy$random", "default", "policy$random")

        when:
        repo.addPolicy(policy)
        Policy getPolicy = repo.getPolicy(policy.name)
        repo.deletePolicy(policy.policyId)

        then:
        getPolicy.equals(policy)
    }

    def "Get policy with bad name returns null" () {
        when:
        Policy getPolicy = repo.getPolicyByName("")

        then:
        getPolicy == null
    }

    def "Policy update" () {
        given:
        Policy policy = entityFactory.createPolicy("blob", "Policy$random", "default", "policy$random")
        Policy policyForUpdate = entityFactory.createPolicy("newBlob", "NewPolicyName$random", "non-default", policy.policyId)

        when:
        repo.addPolicy(policy)
        policyForUpdate.uniqueId = repo.getPolicy(policy.policyId).uniqueId
        repo.updatePolicy(policyForUpdate)
        Policy updatedPolicy = repo.getPolicy(policy.policyId)
        repo.deletePolicy(policy.policyId)

        then:
        updatedPolicy.equals(policyForUpdate)
    }

    def "Retrieve all policies" () {
        given:
        Policy policy1 = entityFactory.createPolicy("blob", "Policy$random", "default", "policy$random")
        Policy policy2 = entityFactory.createPolicy("blob2", "Policy2$random", "2nddefault", "policy2$random")

        when:
        repo.addPolicy(policy1)
        repo.addPolicy(policy2)
        Policies policies = repo.getPolicies()
        repo.deletePolicy(policy1.policyId)
        repo.deletePolicy(policy2.policyId)
        Policies policiesAfterDelete = repo.getPolicies()

        then:
        policies.getPolicy().contains(policy1)
        policies.getPolicy().contains(policy2)
        !policiesAfterDelete.getPolicy().contains(policy1)
        !policiesAfterDelete.getPolicy().contains(policy2)
    }

    def "Add null policy - throws exception" () {
        when:
        repo.addPolicy(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "Delete policy with bad id - throws exception" () {
        when:
        repo.deletePolicy("")

        then:
        thrown(NotFoundException)
    }

    def "Soft delete policy" () {
        given:
        Policy policy = entityFactory.createPolicy("blob", "Policy$random", "default", "policy$random")

        when:
        repo.addPolicy(policy)
        Policy getPolicy = repo.getPolicy(policy.getPolicyId())
        repo.softDeletePolicy(getPolicy)
        Policy getSoftDeletedPolicy = repo.getPolicy(policy.getPolicyId())

        then:
        policy == getPolicy
        getSoftDeletedPolicy == null
    }

}
