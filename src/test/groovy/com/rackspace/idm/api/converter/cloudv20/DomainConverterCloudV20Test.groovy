package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.IdentityConfig.ReloadableConfig
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.Domains
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

class DomainConverterCloudV20Test extends Specification {

    @Shared DomainConverterCloudV20 converterCloudV20
    @Shared IdentityConfig identityConfig
    @Shared ReloadableConfig reloadableConfig

    def setupSpec() {
        identityConfig = Mock(IdentityConfig)
        reloadableConfig = Mock(ReloadableConfig)

        converterCloudV20 = new DomainConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
        converterCloudV20.identityConfig = identityConfig

        identityConfig.reloadableConfig >> reloadableConfig
        reloadableConfig.getDomainDefaultSessionInactivityTimeout() >> Duration.parse("PT15M")
    }

    def cleanupSpec() {
    }

    def "convert domain from ldap to jersey object"() {
        given:
        Domain domain = domain("domainId", "name", false, "description")

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domainEntity = converterCloudV20.toDomain(domain)

        then:
        domain.domainId == domainEntity.id
        domain.name == domainEntity.name
        domain.enabled == domainEntity.enabled
        domain.description == domainEntity.description
    }

    def "convert domain from jersey object to ldap"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domainEntity = domainEntity("domainId", "name", false, "description")

        when:
        Domain domain = converterCloudV20.fromDomain(domainEntity)

        then:
        domain.domainId == domainEntity.id
        domain.name == domainEntity.name
        domain.enabled == domainEntity.enabled
        domain.description == domainEntity.description
    }

    def "convert domain from jersey object to ldap - should set defaults"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domainEntity = domainEntity("domainId", "name", false, "description")
        domainEntity.enabled = null

        when:
        Domain domain = converterCloudV20.fromDomain(domainEntity)

        then:
        domain.domainId == domainEntity.id
        domain.name == domainEntity.name
        domain.description == domainEntity.description
        domainEntity.enabled == true
    }

    def "convert domains to jersey object" () {
        given:
        Domain domain = domain("domainId", "name", false, "description")
        Domains domains = new Domains()
        domains.getDomain().add(domain)

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domainsEntity = converterCloudV20.toDomains(domains)

        then:
        domains.getDomain().size() == domainsEntity.getDomain().size()
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domainEntity = domainsEntity.domain.get(0)
        domain.domainId == domainEntity.id
        domain.name == domainEntity.name
        domain.enabled == domainEntity.enabled
        domain.description == domainEntity.description
    }

    def "convert jersey domains to domains object" () {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = domainEntity("domainId", "name", false, "description")
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains domains = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains()
        domains.getDomain().add(domain)

        when:
        Domains domainsEntity = converterCloudV20.fromDomains(domains)

        then:
        domains.getDomain().size() == domainsEntity.getDomain().size()
        Domain domainEntity = domainsEntity.domain.get(0)
        domain.id == domainEntity.domainId
        domain.name == domainEntity.name
        domain.enabled == domainEntity.enabled
        domain.description == domainEntity.description
    }

    def domain(String id, String name, boolean enabled, String description) {
        new Domain().with {
            it.domainId = id
            it.name = name
            it.enabled = enabled
            it.description = description
            return it
        }
    }

    def domainEntity(String id, String name, boolean enabled, String description) {
        new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain().with {
            it.id = id
            it.name = name
            it.enabled = enabled
            it.description = description
            return it
        }
    }
}
