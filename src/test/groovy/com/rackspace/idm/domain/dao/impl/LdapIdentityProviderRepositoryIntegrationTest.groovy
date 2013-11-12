package com.rackspace.idm.domain.dao.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapIdentityProviderRepositoryIntegrationTest extends Specification {
    @Autowired
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    // These attributes should be loaded in directory via ldif
    @Shared def IDP_NAME = "dedicated";
    @Shared def IDP_URI = "http://my.rackspace.com"

    def "get external provider by name"() {
        when:
        def provider = ldapIdentityProviderRepository.getIdentityProviderByName(IDP_NAME)

        then:
        provider.name == IDP_NAME
        provider.uri == IDP_URI
        provider.description != null
        provider.publicCertificate != null
    }

    def "get external provider by uri"() {
        when:
        def provider = ldapIdentityProviderRepository.getIdentityProviderByUri(IDP_URI)

        then:
        provider.name == IDP_NAME
        provider.uri == IDP_URI
        provider.description != null
        provider.publicCertificate != null
    }
}
