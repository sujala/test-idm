package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.opensaml.security.credential.Credential
import org.springframework.test.context.ContextConfiguration
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.servlet.http.HttpServletResponse

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederationRotatingKeyIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger LOG = Logger.getLogger(FederationRotatingKeyIntegrationTest.class)

    /**
     * Tests that samlResponses that validate against different certs on the same provider can be processed successfully.
     * Solely tests that the appropriate http status code is returned - relying on other existing tests to verify that the actual
     * backend processing (and response body) is correct when the samlResponse can be property verified.
     */
    def "verify key rotation"() {
        //add identity provider w/ 2 keys
        Credential cred1 = SamlCredentialUtils.generateX509Credential()
        Credential cred2 = SamlCredentialUtils.generateX509Credential()
        IdentityProvider originIdp = createIdentityProvider(IdentityProviderFederationTypeEnum.DOMAIN)

        // Add Origin with certs
        def pubCertPemStringOrigin1 = SamlCredentialUtils.getCertificateAsPEMString(cred1.entityCertificate)
        def pubCertsOrigin1 = v2Factory.createPublicCertificate(pubCertPemStringOrigin1)
        def response = cloud20.createIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, pubCertsOrigin1)
        assert response.status == HttpStatus.SC_NO_CONTENT

        def pubCertPemStringOrigin2 = SamlCredentialUtils.getCertificateAsPEMString(cred2.entityCertificate)
        def pubCertsOrigin2 = v2Factory.createPublicCertificate(pubCertPemStringOrigin2)
        response = cloud20.createIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, pubCertsOrigin2)
        assert response.status == HttpStatus.SC_NO_CONTENT

        // Add broker with certs
        Credential brokerIdpCredential = SamlCredentialUtils.generateX509Credential()
        IdentityProvider brokerIdp = createIdentityProvider(IdentityProviderFederationTypeEnum.BROKER)
        def pubCertPemStringBroker = SamlCredentialUtils.getCertificateAsPEMString(brokerIdpCredential.entityCertificate)
        def pubCertsBroker = v2Factory.createPublicCertificate(pubCertPemStringBroker)
        response = cloud20.createIdentityProviderCertificates(utils.serviceAdminToken, brokerIdp.id, pubCertsBroker)
        assert response.status == HttpStatus.SC_NO_CONTENT

        def userAdmin = utils.createCloudAccount()
        def fedRequest = utils.createFedRequest(userAdmin, brokerIdp.issuer, originIdp.issuer)

        when: "Generate samlResponse using first certificate"
        FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerIdpCredential, cred1)
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "Was processed successfully"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "Generate samlResponse using second certificate"
        FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator2 = new FederatedDomainAuthRequestGenerator(brokerIdpCredential, cred2)
        def samlAssertion2 = federatedDomainAuthRequestGenerator2.createSignedSAMLResponse(fedRequest)
        def samlResponse2 = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator2.convertResponseToString(samlAssertion2))

        then: "Was processed successfully"
        samlResponse2.status == HttpServletResponse.SC_OK

        when: "Remove first certificate"
        originIdp = utils.getIdentityProvider(utils.serviceAdminToken, originIdp.id)
        def originCert1 = originIdp.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemStringOrigin1}
        cloud20.deleteIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, originCert1.id)
        def samlResponseInvalid = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlResponse2Again = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator2.convertResponseToString(samlAssertion2))

        then: "Can not use samlResponse generated for first cert, but can still verify samlResponse for second certificate"
        samlResponseInvalid.status == HttpServletResponse.SC_BAD_REQUEST
        samlResponse2Again.status == HttpServletResponse.SC_OK

        when: "Remove remaining certificate"
        def originCert2 = originIdp.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemStringOrigin2}
        cloud20.deleteIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, originCert2.id)
        def samlResponseInvalid2 = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def samlResponse2Invalid = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator2.convertResponseToString(samlAssertion2))

        then: "Can not validate either samlResponses"
        samlResponseInvalid2.status == HttpServletResponse.SC_BAD_REQUEST
        samlResponse2Invalid.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username)
        utils.deleteIdentityProviderQuietly(utils.serviceAdminToken, originIdp.id)
        utils.deleteIdentityProviderQuietly(utils.serviceAdminToken, brokerIdp.id)
    }

    def createIdentityProvider(IdentityProviderFederationTypeEnum type) {
        def idp = v2Factory.createIdentityProvider(UUID.randomUUID().toString(), "blah", UUID.randomUUID().toString(), type, ApprovedDomainGroupEnum.GLOBAL, null)
        utils.createIdentityProvider(utils.serviceAdminToken, idp)
    }
}
