package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.openstack.docs.identity.api.v2.BadRequestFault
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.SamlProducer

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class IdentityProviderCRUDIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "CRUD a DOMAIN IDP with approvedDomainGroup, but no certs - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "create a DOMAIN IDP with no certs and approvedDomainGroup"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED
        creationResultIdp.approvedDomainGroup == domainGroupIdp.approvedDomainGroup
        creationResultIdp.approvedDomainIds == null
        creationResultIdp.description == domainGroupIdp.description
        creationResultIdp.issuer == domainGroupIdp.issuer
        creationResultIdp.publicCertificates == null
        creationResultIdp.id != null
        response.headers.getFirst("Location") != null
        response.headers.getFirst("Location").contains(creationResultIdp.id)

        when: "get the DOMAIN group IDP"
        def getIdpResponse = cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType)
        IdentityProvider getResultIdp = getIdpResponse.getEntity(IdentityProvider)

        then: "contains appropriate info"
        getIdpResponse.status == SC_OK
        getResultIdp.approvedDomainGroup == domainGroupIdp.approvedDomainGroup
        getResultIdp.approvedDomainIds == null
        getResultIdp.description == domainGroupIdp.description
        getResultIdp.issuer == domainGroupIdp.issuer
        getResultIdp.publicCertificates == null
        getResultIdp.id != null

        when: "delete the provider"
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT
        cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType).status == SC_NOT_FOUND

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "CRUD a DOMAIN IDP with approvedDomains and single cert - request: #requestContentType"() {
        given:
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        def keyPair1 = SamlCredentialUtils.generateKeyPair()
        def cert1 = SamlCredentialUtils.generateCertificate(keyPair1)
        def pubCertPemString1 = SamlCredentialUtils.getCertificateAsPEMString(cert1)
        def pubCerts1 = v2Factory.createPublicCertificate(pubCertPemString1)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)


        when: "create a DOMAIN IDP with single certs and approvedDomains"
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.publicCertificates = publicCertificates
            it
        }

        def response = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED
        creationResultIdp.approvedDomainGroup == null
        creationResultIdp.approvedDomainIds != null
        creationResultIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        creationResultIdp.description == approvedDomainsIdp.description
        creationResultIdp.issuer == approvedDomainsIdp.issuer
        creationResultIdp.publicCertificates != null
        creationResultIdp.publicCertificates.publicCertificate != null
        creationResultIdp.publicCertificates.publicCertificate.size() == 1
        creationResultIdp.publicCertificates.publicCertificate.get(0).pemEncoded == pubCertPemString1
        creationResultIdp.id != null
        response.headers.getFirst("Location") != null
        response.headers.getFirst("Location").contains(creationResultIdp.id)

        when: "get the IDP"
        def getIdpResponse = cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType)
        IdentityProvider getResultIdp = getIdpResponse.getEntity(IdentityProvider)

        then: "contains appropriate info"
        getIdpResponse.status == SC_OK
        getResultIdp.approvedDomainGroup == null
        getResultIdp.approvedDomainIds != null
        getResultIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        getResultIdp.description == approvedDomainsIdp.description
        getResultIdp.issuer == approvedDomainsIdp.issuer
        getResultIdp.publicCertificates != null
        getResultIdp.publicCertificates.publicCertificate != null
        getResultIdp.publicCertificates.publicCertificate.size() == 1
        getResultIdp.publicCertificates.publicCertificate.get(0).pemEncoded == pubCertPemString1
        getResultIdp.id != null

        when: "delete the provider"
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT
        cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType).status == SC_NOT_FOUND

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "Create a DOMAIN IDP with approvedDomains and single cert. Validate can immediately fed auth for user for approvedDomain"() {
        given:
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        def keyPair1 = SamlCredentialUtils.generateKeyPair()
        def cert1 = SamlCredentialUtils.generateCertificate(keyPair1)
        def pubCertPemString1 = SamlCredentialUtils.getCertificateAsPEMString(cert1)
        def pubCerts1 = v2Factory.createPublicCertificate(pubCertPemString1)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)
        def samlProducer1 = new SamlProducer(SamlCredentialUtils.generateX509Credential(cert1, keyPair1))

        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        //create a DOMAIN IDP with single certs and approvedDomains
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider creationResultIdp = utils.createIdentityProvider(idpManagerToken, approvedDomainsIdp)

        when: "auth user in domain with IDP"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(creationResultIdp.issuer, username, expDays, domainId, null, email, samlProducer1);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "success"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "auth user in different domain for IDP"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(creationResultIdp.issuer, username, expDays, "otherdomain", null, email, samlProducer1);
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "failure"
        samlResponse.status == HttpServletResponse.SC_FORBIDDEN

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
        utils.deleteUsers(users)
    }

    @Unroll
    def "Get list of providers returns based on approvedDomains for requestContentType: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        def domainId = UUID.randomUUID().toString()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        IdentityProvider idp1ToCreate = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp1 = utils.createIdentityProvider(idpManagerToken, idp1ToCreate)

        def domainId2 = UUID.randomUUID().toString()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId2, domainId2))
        IdentityProvider idp2ToCreate = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId2]).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp2 = utils.createIdentityProvider(idpManagerToken, idp2ToCreate)

        IdentityProvider idp3ToCreate = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp3 = utils.createIdentityProvider(idpManagerToken, idp3ToCreate)

        IdentityProvider idp4ToCreate = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp4 = utils.createIdentityProvider(idpManagerToken, idp4ToCreate)

        when: "get all idp"
        def allIdp = cloud20.listIdentityProviders(idpManagerToken, null, requestContentType, requestContentType)

        then:
        allIdp.status == SC_OK
        IdentityProviders providers = allIdp.getEntity(IdentityProviders.class)
        providers != null
        providers.identityProvider.size() >= 4
        providers.identityProvider.find{it.id == idp1.id} != null
        providers.identityProvider.find{it.id == idp2.id} != null
        providers.identityProvider.find{it.id == idp3.id} != null
        providers.identityProvider.find{it.id == idp4.id} != null

        when: "get all idps for specific domain "
        def domainSpecificIdpResponse = cloud20.listIdentityProviders(idpManagerToken, domainId, requestContentType, requestContentType)

        then: "get all idps for that domain and all global domain idps"
        allIdp.status == SC_OK
        IdentityProviders domainSpecificIdps = domainSpecificIdpResponse.getEntity(IdentityProviders.class)
        domainSpecificIdps != null
        domainSpecificIdps.identityProvider.size() >= 4
        domainSpecificIdps.identityProvider.find{it.id == idp1.id} != null
        domainSpecificIdps.identityProvider.find{it.id == idp2.id} == null
        domainSpecificIdps.identityProvider.find{it.id == idp3.id} != null
        domainSpecificIdps.identityProvider.find{it.id == idp4.id} == null

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, idp1.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, idp2.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, idp3.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, idp4.id)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _

    }

    def "Create Identity Provider returns errors appropriately"() {
        given:
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        def keyPair1 = SamlCredentialUtils.generateKeyPair()
        def cert1 = SamlCredentialUtils.generateCertificate(keyPair1)
        def pubCertPemString1 = SamlCredentialUtils.getCertificateAsPEMString(cert1)
        def pubCerts1 = v2Factory.createPublicCertificate(pubCertPemString1)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)

        when: "caller doesn't have role"
        IdentityProvider validIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(utils.getIdentityAdminToken(), validIdp)

        then: "403"
        response.status == SC_FORBIDDEN

        when: "Domain IDP without either approvedDomains or approvedDomainGroup"
        IdentityProvider invalid = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "Domain IDP with both approvedDomains or approvedDomainGroup"
        invalid = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, [domainId])
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "Racker IDP with approvedDomainGroup"
        invalid = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "Domain IDP with invalid approvedDomainGroup"
        invalid = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, "Invalid", null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_GROUP)

        when: "Domain IDP with invalid approvedDomains"
        invalid = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, ["non-existent-domain"])
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN)

        when: "IDP with issuer already exists"
        invalid = v2Factory.createIdentityProvider("blah", Constants.DEFAULT_IDP_URI, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "409"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_CONFLICT, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS)

        when: "IDP with issuer length exceeded 255"
        invalid = v2Factory.createIdentityProvider("blah", RandomStringUtils.randomAlphabetic(256), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)

        when: "IDP with missing issuer"
        invalid = v2Factory.createIdentityProvider("blah", null, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "IDP with missing fed type"
        invalid = v2Factory.createIdentityProvider("blah",  getRandomUUID(), null, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

    }

}
