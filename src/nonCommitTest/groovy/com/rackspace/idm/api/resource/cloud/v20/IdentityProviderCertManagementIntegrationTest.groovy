package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.service.FederatedIdentityService
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.mockserver.verify.VerificationTimes
import org.opensaml.security.credential.Credential
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.SamlProducer
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

class IdentityProviderCertManagementIntegrationTest extends RootIntegrationTest {

    @Autowired
    FederatedIdentityService federatedIdentityService

    @Unroll
    def "test full key rotation for identity provider - request: #requestContentType"() {
        given:

        //add identity provider w/ 2 keys
        Credential cred1 = SamlCredentialUtils.generateX509Credential()
        Credential cred2 = SamlCredentialUtils.generateX509Credential()
        IdentityProvider originIdp = createIdentityProvider(IdentityProviderFederationTypeEnum.DOMAIN)

        // Add Origin with certs
        def pubCertPemStringOrigin1 = SamlCredentialUtils.getCertificateAsPEMString(cred1.entityCertificate)
        def pubCertsOrigin1 = v2Factory.createPublicCertificate(pubCertPemStringOrigin1)

        def pubCertPemStringOrigin2 = SamlCredentialUtils.getCertificateAsPEMString(cred2.entityCertificate)
        def pubCertsOrigin2 = v2Factory.createPublicCertificate(pubCertPemStringOrigin2)

        // Add broker with certs
        Credential brokerIdpCredential = SamlCredentialUtils.generateX509Credential()
        IdentityProvider brokerIdp = createIdentityProvider(IdentityProviderFederationTypeEnum.BROKER)
        def pubCertPemStringBroker = SamlCredentialUtils.getCertificateAsPEMString(brokerIdpCredential.entityCertificate)
        def pubCertsBroker = v2Factory.createPublicCertificate(pubCertPemStringBroker)
        response = cloud20.createIdentityProviderCertificates(utils.serviceAdminToken, brokerIdp.id, pubCertsBroker)
        assert response.status == HttpStatus.SC_NO_CONTENT

        def userAdmin = utils.createCloudAccount()
        def fedRequest = utils.createFedRequest(userAdmin, brokerIdp.issuer, originIdp.issuer)

        when: "add the first cert to the IDP"
        resetCloudFeedsMock()
        def response = cloud20.createIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, pubCertsOrigin1, requestContentType)

        then: "success"
        response.status == HttpServletResponse.SC_NO_CONTENT

        and: "only one event was posted"
        def idpEntity = federatedIdentityService.getIdentityProvider(originIdp.id)
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was an UPDATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "get the IDP"
        def identityProviderResponse = utils.getIdentityProvider(utils.serviceAdminToken, originIdp.id)

        then: "the cert is shown on the IDP and has a generated ID"
        def createdCert = identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemStringOrigin1}
        createdCert != null
        createdCert.id != null

        when: "auth user for IDP"
        FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerIdpCredential, cred1)
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "success"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "add the second cert to the IDP"
        resetCloudFeedsMock()
        response = cloud20.createIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, pubCertsOrigin2)

        then: "success"
        response.status == HttpServletResponse.SC_NO_CONTENT

        and: "only one event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was an UPDATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "get the IDP"
        identityProviderResponse = utils.getIdentityProvider(utils.serviceAdminToken, originIdp.id)

        then: "the both certs are shown on the IDP and both have generated IDs"
        def createdCert1 = identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemStringOrigin1}
        def createdCert2 = identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemStringOrigin2}
        createdCert1 != null
        createdCert.id != null
        createdCert2 != null
        createdCert.id != null

        when: "auth user for IDP using second cert"
        federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerIdpCredential, cred2)
        samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "success"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "delete the first cert"
        resetCloudFeedsMock()
        def deleteCertsResponse = cloud20.deleteIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, createdCert1.id, requestContentType)

        then: "success"
        deleteCertsResponse.status == HttpServletResponse.SC_NO_CONTENT

        and: "only one event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was an UPDATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "get the IDP"
        identityProviderResponse = utils.getIdentityProvider(utils.serviceAdminToken, originIdp.id)

        then: "the first cert is no longer on the IDP"
        identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemStringOrigin1} == null

        and: "the second cert is still on the IDP"
        identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemStringOrigin2} != null

        when: "auth user for IDP using first cert"
        federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerIdpCredential, cred1)
        samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "error, invalid cert"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        when: "auth user for IDP using second cert"
        federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerIdpCredential, cred2)
        samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "success"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "delete the second cert"
        resetCloudFeedsMock()
        deleteCertsResponse = cloud20.deleteIdentityProviderCertificates(utils.serviceAdminToken, originIdp.id, createdCert2.id, requestContentType)

        then: "success"
        deleteCertsResponse.status == HttpServletResponse.SC_NO_CONTENT

        and: "only one event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was an UPDATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "get the IDP"
        identityProviderResponse = utils.getIdentityProvider(utils.serviceAdminToken, originIdp.id)

        then: "the IDP no longer has any certs"
        identityProviderResponse.publicCertificates == null

        when: "auth user for IDP using second cert"
        federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerIdpCredential, cred2)
        samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))

        then: "error, invalid cert"
        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        utils.deleteIdentityProviderQuietly(utils.serviceAdminToken, originIdp.id)
        utils.deleteIdentityProviderQuietly(utils.serviceAdminToken, brokerIdp.id)
        utils.deleteUserQuietly(userAdmin)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test add duplicate certificate to identity provider - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idp = utils.createIdentityProvider(idpManagerToken)
        def keyPair = SamlCredentialUtils.generateKeyPair()
        def cert = SamlCredentialUtils.generateCertificate(keyPair)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(cert)
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)

        when: "add cert to the IDP"
        def response = cloud20.createIdentityProviderCertificates(idpManagerToken, idp.id, pubCerts, requestContentType)

        then: "success"
        response.status == 204

        when: "get the IDP"
        def identityProviderResponse = utils.getIdentityProvider(idpManagerToken, idp.id)

        then: "the cert is shown on the IDP and has a generated ID"
        def createdCert = identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemString}
        createdCert != null
        createdCert.id != null

        when: "add the same cert to the IDP again"
        response = cloud20.createIdentityProviderCertificates(idpManagerToken, idp.id, pubCerts, requestContentType)

        then: "success"
        response.status == 409

        when: "get the IDP again"
        identityProviderResponse = utils.getIdentityProvider(idpManagerToken, idp.id)
        createdCert = identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemString}

        then: "the IDP still only has 1 cert"
        createdCert != null
        createdCert.id != null

        cleanup:
        utils.deleteIdentityProvider(idp)
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test add certificate to identity provider that does not exist - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def keyPair = SamlCredentialUtils.generateKeyPair()
        def cert = SamlCredentialUtils.generateCertificate(keyPair)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(cert)
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)

        when: "add cert to the IDP"
        def response = cloud20.createIdentityProviderCertificates(idpManagerToken, "doesNotExist", pubCerts, requestContentType)

        then: "success"
        response.status == 404

        cleanup:
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test add bad certificate to identity provider, garbage data for PEM string - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idp = utils.createIdentityProvider(idpManagerToken)
        def pubCertPemString = testUtils.getRandomUUID("badPemString")
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)

        when: "add cert to the IDP"
        def response = cloud20.createIdentityProviderCertificates(idpManagerToken, idp.id, pubCerts, requestContentType)

        then: "success"
        response.status == 400

        cleanup:
        utils.deleteIdentityProvider(idp)
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test add blank cert to identity provider - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idp = utils.createIdentityProvider(idpManagerToken)
        def pubCertPemString = "   "
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)

        when: "add cert to the IDP"
        def response = cloud20.createIdentityProviderCertificates(idpManagerToken, idp.id, pubCerts, requestContentType)

        then: "success"
        response.status == 400

        cleanup:
        utils.deleteIdentityProvider(idp)
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test add invalid certificate to identity provider, cert is expired - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idp = utils.createIdentityProvider(idpManagerToken)
        def keyPair = SamlCredentialUtils.generateKeyPair()
        def cert = SamlCredentialUtils.generateCertificate(keyPair, new DateTime().minusYears(1).toDate(), new DateTime().minusDays(1).toDate())
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(cert)
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)

        when: "add cert to the IDP"
        def response = cloud20.createIdentityProviderCertificates(idpManagerToken, idp.id, pubCerts, requestContentType)

        then: "success"
        response.status == 400

        cleanup:
        utils.deleteIdentityProvider(idp)
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test add certificate to identity provider, cert is not yet valid (becomes valid in the future) - request: #requestContentType"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idp = utils.createIdentityProvider(idpManagerToken)
        def keyPair = SamlCredentialUtils.generateKeyPair()
        def cert = SamlCredentialUtils.generateCertificate(keyPair, new DateTime().plusDays(1).toDate(), new DateTime().plusYears(1).toDate())
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(cert)
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlProducer = new SamlProducer(SamlCredentialUtils.generateX509Credential(cert, keyPair))

        when: "add cert to the IDP"
        def response = cloud20.createIdentityProviderCertificates(idpManagerToken, idp.id, pubCerts, requestContentType)

        then: "success"
        response.status == HttpServletResponse.SC_NO_CONTENT

        //TODO: uncomment this once the story to prevent auth with future certs is implemented
//        when: "auth user for IDP using cert"
//        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(idp.issuer, username, expSecs, domainId, null, email, samlProducer);
//        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
//
//        then: "success"
//        samlResponse.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        utils.deleteIdentityProvider(idp)
        utils.deleteUser(idpManager)
        utils.deleteUsers(users)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "test add certificate to identity provider without correct role - request: #requestContentType"() {
        given:
        def idpNonManager = utils.createIdentityAdmin()
        def idpNonManagerToken = utils.getToken(idpNonManager.username)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idp = utils.createIdentityProvider(idpManagerToken)
        def keyPair = SamlCredentialUtils.generateKeyPair()
        def cert = SamlCredentialUtils.generateCertificate(keyPair)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(cert)
        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)

        when: "add cert to the IDP with the non-manager"
        def response = cloud20.createIdentityProviderCertificates(idpNonManagerToken, idp.id, pubCerts, requestContentType)

        then: "not authorized"
        response.status == 403

        when: "get idp"
        def identityProviderResponse = utils.getIdentityProvider(idpManagerToken, idp.id)

        then: "the cert was not added"
        identityProviderResponse.publicCertificates == null

        when: "add cert to the IDP with the manager"
        response = cloud20.createIdentityProviderCertificates(idpManagerToken, idp.id, pubCerts, requestContentType)

        then: "success"
        response.status == 204

        when: "get the IDP"
        identityProviderResponse = utils.getIdentityProvider(idpManagerToken, idp.id)

        then: "the cert is shown on the IDP"
        def createdCert = identityProviderResponse.publicCertificates.publicCertificate.find {it.pemEncoded == pubCertPemString}

        when: "delete the cert using the non-manager"
        def deleteCertsResponse = cloud20.deleteIdentityProviderCertificates(idpNonManagerToken, idp.id, createdCert.id, requestContentType)

        then: "not authorized"
        deleteCertsResponse.status == 403

        cleanup:
        utils.deleteIdentityProvider(idp)
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def createIdentityProvider(IdentityProviderFederationTypeEnum type) {
        def idp = v2Factory.createIdentityProvider(UUID.randomUUID().toString(), "blah", UUID.randomUUID().toString(), type, ApprovedDomainGroupEnum.GLOBAL, null)
        utils.createIdentityProvider(utils.serviceAdminToken, idp)
    }

}
