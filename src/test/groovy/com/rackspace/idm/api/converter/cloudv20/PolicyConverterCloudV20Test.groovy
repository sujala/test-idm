package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.Policies
import com.rackspace.idm.domain.entity.Policy
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 8/12/13
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
class PolicyConverterCloudV20Test extends Specification {

    @Shared PolicyConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new PolicyConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert policy from ldap to jersey object"() {
        given:
        Policy policy = policy("domainId", "name", false, true, "type", "blob", "description")

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policyEntity = converterCloudV20.toPolicy(policy)

        then:
        policy.policyId == policyEntity.id
        policy.name == policyEntity.name
        policy.enabled == policyEntity.enabled
        policy.global == policyEntity.global
        policy.policyType == policyEntity.type
        policy.blob == policyEntity.blob
        policy.description == policyEntity.description
    }

    def "convert policy from jersey object to ldap"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policyEntity = policyEntity("domainId", "name", false, true, "type", "blob", "description")

        when:
        Policy policy = converterCloudV20.fromPolicy(policyEntity)

        then:
        policy.policyId == policyEntity.id
        policy.name == policyEntity.name
        policy.enabled == policyEntity.enabled
        policy.global == policyEntity.global
        policy.policyType == policyEntity.type
        policy.blob == policyEntity.blob
        policy.description == policyEntity.description
    }

    def "convert policy from jersey object to ldap - should set defaults"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policyEntity = policyEntity("domainId", "name", false, true, "type", "blob", "description")
        policyEntity.enabled = null
        policyEntity.global = null

        when:
        Policy policy = converterCloudV20.fromPolicy(policyEntity)

        then:
        policy.policyId == policyEntity.id
        policy.name == policyEntity.name
        policy.policyType == policyEntity.type
        policy.blob == policyEntity.blob
        policy.description == policyEntity.description
        policyEntity.enabled == true
        policyEntity.global == false
    }

    def "convert policy from ldap to jersey object for policies"() {
        given:
        Policy policy = policy("domainId", "name", false, true, "type", "blob", "description")

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policyEntity = converterCloudV20.toPolicyForPolicies(policy)

        then:
        policy.policyId == policyEntity.id
        policy.name == policyEntity.name
        policy.enabled == policyEntity.enabled
        policy.global == policyEntity.global
        policy.policyType == policyEntity.type
        policyEntity.blob == null
        policyEntity.description == null
    }

    def "convert policies to jersey object" () {
        given:
        Policy policy = policy("domainId", "name", false, true, "type", "blob", "description")
        Policies policies = new Policies()
        policies.getPolicy().add(policy)

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies policiesEntity = converterCloudV20.toPolicies(policies)

        then:
        policies.getPolicy().size() == policiesEntity.getPolicy().size()
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policyEntity = policiesEntity.policy.get(0)
        policy.policyId == policyEntity.id
        policy.name == policyEntity.name
        policy.enabled == policyEntity.enabled
        policy.global == policyEntity.global
        policy.policyType == policyEntity.type
    }

    def "convert jersey policies to policies object" () {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy policy = policyEntity("domainId", "name", false, true, "type", "blob", "description")
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies policies = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies()
        policies.getPolicy().add(policy)

        when:
        Policies policiesEntity = converterCloudV20.fromPolicies(policies)

        then:
        policies.getPolicy().size() == policiesEntity.getPolicy().size()
        Policy policyEntity = policiesEntity.policy.get(0)
        policy.id == policyEntity.policyId
        policy.name == policyEntity.name
        policy.enabled == policyEntity.enabled
        policy.global == policyEntity.global
        policy.type == policyEntity.policyType
        policy.blob == policyEntity.blob
        policy.description == policyEntity.description
    }


    def policy(String id, String name, boolean enabled, boolean globalValue, String policyType, String blob, String description) {
        new Policy().with {
            it.policyId = id
            it.name = name
            it.enabled = enabled
            it.global = globalValue
            it.policyType = policyType
            it.blob = blob
            it.description = description
            return it
        }
    }

    def policyEntity(String id, String name, boolean enabled, boolean globalValue, String policyType, String blob, String description) {
        new com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy().with {
            it.id = id
            it.name = name
            it.enabled = enabled
            it.global = globalValue
            it.type = policyType
            it.blob = blob
            it.description = description
            return it
        }
    }
}
