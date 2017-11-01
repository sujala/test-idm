package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EmailDomains
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.IdentityProvider
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import spock.lang.Specification

class IdentityProviderConverterCloudV20Test extends Specification {

    @Shared IdentityProviderConverterCloudV20 converter

    def setupSpec() {
        converter = new IdentityProviderConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def "toIdentityProvider: emailDomains"() {
        given:
        IdentityProvider identityProvider = new IdentityProvider().with {
            it.emailDomains = ["emailDomain", "emailDomain2"].asList()
            it
        }

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider provider = converter.toIdentityProvider(identityProvider)

        then:
        provider.emailDomains.emailDomain.size() == 2
    }

    def "fromIdentityProvider: avoid duplicate emailDomains"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider identityProvider = new com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider().with {
            it.emailDomains = new EmailDomains().with {
                it.emailDomain = ["emailDomain.com", "emailDomain.com", "EMAILDOMAIN.COM"].asList()
                it
            }
            it
        }

        when:
        IdentityProvider provider = converter.fromIdentityProvider(identityProvider)

        then:
        provider.emailDomains.size() == 1
    }
}
