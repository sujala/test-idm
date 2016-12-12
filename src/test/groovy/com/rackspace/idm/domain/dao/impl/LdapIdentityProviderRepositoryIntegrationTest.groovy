package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.IdentityProvider
import com.unboundid.ldap.sdk.DeleteRequest
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.junit.Rule
import org.opensaml.xml.security.credential.Credential
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory
import testHelpers.junit.ConditionalIgnoreRule
import testHelpers.junit.IgnoreByRepositoryProfile
import testHelpers.saml.SamlCredentialUtils

import java.security.cert.X509Certificate

@IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapIdentityProviderRepositoryIntegrationTest extends Specification {
    @Autowired
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    @Autowired
    LdapConnectionPools ldapConnectionPools

    @Rule
    public ConditionalIgnoreRule role = new ConditionalIgnoreRule()

    // These attributes should be loaded in directory via ldif
    @Shared def IDP_ID = "dedicated";
    @Shared def IDP_URI = "http://my.rackspace.com"

    @Shared
    EntityFactory entityFactory = new EntityFactory()

    LDAPInterface con
    LDAPPersister<Application> providerPersister

    def setup() {
        con = ldapConnectionPools.getAppConnPoolInterface()
        providerPersister = LDAPPersister.getInstance(IdentityProvider.class)
    }

    /**
     * Expects the baseline CA directory to have a pre-existing "dedicated"
     * identity provider with a single certificate. This is used to demonstrate loading providers
     * inserted via LDIF.
     */
    def "get external provider by id"() {
        when:
        def provider = ldapIdentityProviderRepository.getIdentityProviderById(IDP_ID)

        then:
        provider.providerId == IDP_ID
        provider.uri == IDP_URI
        provider.description != null
        provider.userCertificates != null
        provider.userCertificates.size() == 1

        //convert the cert to standard x509 object required for signature verification
        provider.getUserCertificatesAsX509() != null
        provider.getUserCertificatesAsX509().size() == provider.userCertificates.size()
    }

    /**
     * Expects the baseline CA directory to have a pre-existing "dedicated"
     * identity provider with a single certificate. This is used to demonstrate loading providers
     * inserted via LDIF.
     */
    def "get external provider by uri"() {
        when:
        def provider = ldapIdentityProviderRepository.getIdentityProviderByUri(IDP_URI)

        then:
        provider.providerId == IDP_ID
        provider.uri == IDP_URI
        provider.description != null
        provider.userCertificates != null
        provider.userCertificates.size() == 1
        provider.getUserCertificatesAsX509() != null
        provider.getUserCertificatesAsX509().size() == provider.userCertificates.size()
    }

    def "addProvider - verify adding new provider"() {
        IdentityProvider provider = entityFactory.createIdentityProviderWithCredential()

        when:
        ldapIdentityProviderRepository.addIdentityProvider(provider)

        then: "Application object should now be populated with ldap entry and entry should exist in ldap"
        provider.getUniqueId() != null
        provider.getUniqueId() == getExpectedProviderDn(provider)

        when: "can retrieve directly from ldap using sdk"
        SearchResultEntry entry = con.getEntry(provider.getUniqueId())

        then:
        entry != null

        when: "retrieve via dao"
        IdentityProvider retrievedProvider = ldapIdentityProviderRepository.getIdentityProviderById(provider.providerId)

        then: "certs retrieved appropriately"
        retrievedProvider != null
        retrievedProvider.userCertificates.size() == provider.userCertificates.size()
        retrievedProvider.getUserCertificatesAsX509().get(0).getEncoded() == provider.getUserCertificatesAsX509().get(0).getEncoded()

        cleanup:
        deleteDirect(provider)
    }

    def "can add multiple user certificates to provider"() {
        Credential cred1 = SamlCredentialUtils.generateX509Credential();
        Credential cred2 = SamlCredentialUtils.generateX509Credential();
        List<X509Certificate> certs = [cred1.entityCertificate, cred2.entityCertificate]
        IdentityProvider provider = entityFactory.createIdentityProviderWithCertificates(certs)

        when:
        ldapIdentityProviderRepository.addObject(provider)

        then: "Application object should now be populated with ldap entry and entry should exist in ldap"
        provider.getUniqueId() != null
        provider.getUniqueId() == getExpectedProviderDn(provider)
        provider.userCertificates.size() == certs.size()

        when: "can retrieve directly from ldap using sdk"
        SearchResultEntry entry = con.getEntry(provider.getUniqueId())

        then:
        entry != null

        when: "retrieve via dao"
        IdentityProvider retrievedProvider = ldapIdentityProviderRepository.getIdentityProviderById(provider.providerId)

        then: "certs retrieved appropriately"
        retrievedProvider != null
        retrievedProvider.userCertificates.size() == certs.size()
        retrievedProvider.getUserCertificatesAsX509().eachWithIndex {cert, index -> cert.getEncoded() == certs.get(index).getEncoded()}

        cleanup:
        deleteDirect(provider)
    }

    def "can create and retrieve provider with no certificates, add certificates, delete certificates"() {
        IdentityProvider provider = entityFactory.createIdentityProviderWithoutCertificate()

        when:
        ldapIdentityProviderRepository.addObject(provider)

        then: "Application object should now be populated with ldap entry and entry should exist in ldap"
        provider.getUniqueId() != null
        provider.getUniqueId() == getExpectedProviderDn(provider)
        provider.userCertificates == null

        when: "can retrieve directly from ldap using sdk"
        SearchResultEntry entry = con.getEntry(provider.getUniqueId())

        then:
        entry != null

        when: "retrieve via dao"
        IdentityProvider retrievedProvider = ldapIdentityProviderRepository.getIdentityProviderById(provider.providerId)

        then: "certs retrieved appropriately"
        retrievedProvider != null
        retrievedProvider.userCertificates == null
        retrievedProvider.getUserCertificatesAsX509().size() == 0

        when: "add cert"
        Credential cred1 = SamlCredentialUtils.generateX509Credential();
        retrievedProvider.addUserCertificate(cred1.entityCertificate)
        ldapIdentityProviderRepository.updateObject(retrievedProvider)
        retrievedProvider = ldapIdentityProviderRepository.getIdentityProviderById(provider.providerId)

        then:
        retrievedProvider.getUserCertificatesAsX509().size() == 1

        when: "add same cert twice"
        retrievedProvider.addUserCertificate(cred1.entityCertificate)
        ldapIdentityProviderRepository.updateObject(retrievedProvider)
        retrievedProvider = ldapIdentityProviderRepository.getIdentityProviderById(provider.providerId)

        then: "doesn't get added again"
        retrievedProvider.getUserCertificatesAsX509().size() == 1

        when: "remove cert"
        retrievedProvider.removeUserCertificate(cred1.entityCertificate)
        ldapIdentityProviderRepository.updateObject(retrievedProvider)
        retrievedProvider = ldapIdentityProviderRepository.getIdentityProviderById(provider.providerId)

        then:
        retrievedProvider.getUserCertificatesAsX509().size() == 0

        cleanup:
        deleteDirect(provider)
    }



    private String getExpectedProviderDn(IdentityProvider provider) {
        new LdapRepository.LdapDnBuilder(LdapRepository.EXTERNAL_PROVIDERS_BASE_DN).addAttribute("ou", provider.providerId).build()
    }

    /**
     * This function uses the unboundid sdk directly to delete data from ldap and bypass all rackspace code
     *
     * @param app - app to be deleted
     * @return
     */
    def deleteDirect(IdentityProvider obj) {
        if (obj != null) {
            deleteDirect(obj.getUniqueId())
        }
    }

    /**
     * This function uses the unboundid sdk directly to delete data from ldap and bypass all rackspace code
     *
     * @param dn - dn of object to be deleted
     * @return
     */
    def deleteDirect(String dn) {
        DeleteRequest deleteRequest = new DeleteRequest(dn);
        deleteRequest.addControl(new SubtreeDeleteRequestControl(true));
        con.delete(deleteRequest);
    }
}