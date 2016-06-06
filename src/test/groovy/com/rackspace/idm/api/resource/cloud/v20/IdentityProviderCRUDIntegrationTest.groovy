package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.service.IdentityProviderTypeFilterEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.exception.BadRequestException
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.SamlProducer

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse

class IdentityProviderCRUDIntegrationTest extends RootIntegrationTest {

    @Autowired
    TenantService tenantService

    @Autowired
    TenantDao tenantDao

    @Unroll
    def "CRUD a DOMAIN IDP with approvedDomainGroup, but no certs - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "create a DOMAIN IDP with no certs and approvedDomainGroup"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED
        creationResultIdp.approvedDomainGroup == domainGroupIdp.approvedDomainGroup
        creationResultIdp.approvedDomainIds == null
        creationResultIdp.description == domainGroupIdp.description
        creationResultIdp.issuer == domainGroupIdp.issuer
        creationResultIdp.authenticationUrl == domainGroupIdp.authenticationUrl
        creationResultIdp.publicCertificates == null
        creationResultIdp.federationType == IdentityProviderFederationTypeEnum.DOMAIN
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
        getResultIdp.authenticationUrl == domainGroupIdp.authenticationUrl
        getResultIdp.federationType == IdentityProviderFederationTypeEnum.DOMAIN
        getResultIdp.publicCertificates == null
        getResultIdp.id != null

        when: "update the provider"
        def newAuthUrl = RandomStringUtils.randomAlphanumeric(10)
        IdentityProvider updateDomainGroupIdp = new IdentityProvider().with {
            it.authenticationUrl = newAuthUrl
            it
        }
        def updateIdpResponse = cloud20.updateIdentityProvider(idpManagerToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "idp updated"
        updateIdpResponse.status == SC_OK
        IdentityProvider updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)
        updateResultIdp.authenticationUrl == newAuthUrl

        def getAfterUpdateIdpResponse = cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType)
        IdentityProvider getAfterUpdateIdp = getAfterUpdateIdpResponse.getEntity(IdentityProvider)
        getAfterUpdateIdp.authenticationUrl == newAuthUrl

        when: "delete the provider"
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT
        cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType).status == SC_NOT_FOUND

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "CRUD a RACKER IDP with certs - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "create a RACKER IDP with no certs and approvedDomainGroup"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED
        creationResultIdp.approvedDomainGroup == null
        creationResultIdp.approvedDomainIds == null
        creationResultIdp.description == domainGroupIdp.description
        creationResultIdp.issuer == domainGroupIdp.issuer
        creationResultIdp.authenticationUrl == domainGroupIdp.authenticationUrl
        creationResultIdp.publicCertificates == null
        creationResultIdp.id != null
        creationResultIdp.federationType == IdentityProviderFederationTypeEnum.RACKER
        response.headers.getFirst("Location") != null
        response.headers.getFirst("Location").contains(creationResultIdp.id)

        when: "get the DOMAIN group IDP"
        def getIdpResponse = cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType)
        IdentityProvider getResultIdp = getIdpResponse.getEntity(IdentityProvider)

        then: "contains appropriate info"
        getIdpResponse.status == SC_OK
        getResultIdp.approvedDomainGroup == null
        getResultIdp.approvedDomainIds == null
        getResultIdp.description == domainGroupIdp.description
        getResultIdp.issuer == domainGroupIdp.issuer
        getResultIdp.authenticationUrl == domainGroupIdp.authenticationUrl
        getResultIdp.federationType == IdentityProviderFederationTypeEnum.RACKER
        getResultIdp.publicCertificates == null
        getResultIdp.id != null

        when: "delete the provider"
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT
        cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType).status == SC_NOT_FOUND

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }


    @Unroll
    def "Error check create IDP with various empty strings - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))

        when: "create a DOMAIN IDP with approvedDomainGroup, empty approvedDomainId list"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, Collections.EMPTY_LIST)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE) //our json reader would send NULL rather than empty array so json would pass (appropriately)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with approvedDomainGroup, empty string approvedDomainId"
        domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, [""])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with empty string approvedDomainGroup, empty string approvedDomainId"
        domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, "", [""])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with empty string approvedDomainGroup and valid approvedDomainId"
        IdentityProvider domainIdIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, "", [domainId])
        response = cloud20.createIdentityProvider(idpManagerToken, domainIdIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with null approvedDomainGroup, empty string approvedDomainId"
        domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [""])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN)

        when: "create a DOMAIN IDP with null authenticationUrl"
        domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.authenticationUrl = null
            it
        }
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "create a RACKER IDP with null authenticationUrl"
        def rackidp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, null).with {
            it.authenticationUrl = null
            it
        }
        response = cloud20.createIdentityProvider(idpManagerToken, rackidp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        cleanup:
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Error check create Racker IDP with various empty strings - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))

        when: "create a IDP with approvedDomainGroup, null list"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a IDP with null approvedDomainGroup, entry in list"
        domainGroupIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, [domainId])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        cleanup:
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Create IDP with empty description - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "create a DOMAIN IDP with approvedDomainGroup, empty string description"
        def domainGroupIdp = v2Factory.createIdentityProvider("", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "successful"
        response.status == SC_CREATED

        cleanup:
        utils.deleteUser(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Error if try to update IDP without specifying authenticationUrl - request: #requestContentType"() {
        given:
        def idpManagerToken = utils.getServiceAdminToken()
        def domainGroupIdp = v2Factory.createIdentityProvider("", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        def createdIdp = response.getEntity(IdentityProvider)
        def updateIdp = new IdentityProvider()

        when: "create a DOMAIN IDP with approvedDomainGroup, empty string description"
        def updateIdpResponse = cloud20.updateIdentityProvider(idpManagerToken, createdIdp.id, updateIdp)

        then: "successful"
        updateIdpResponse.status == SC_BAD_REQUEST

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Error if try to update IDP with any value other than authenticationUrl - property: #prop "() {
        given:
        def idpManagerToken = utils.getServiceAdminToken()

        when: "create a DOMAIN IDP with approvedDomainGroup, empty string description"
        def response = cloud20.createIdentityProvider(idpManagerToken, idp)

        then: "successful"
        response.status == SC_BAD_REQUEST

        where:
        prop | idp
        "id"  | new IdentityProvider().with {it.authenticationUrl = "url"; it.id = "as"; it}
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
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "Deleting a non-existant domain returns appropriate error"() {
        def idpManagerToken = utils.getServiceAdminToken()

        when:
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, "non-existant-idp-provider")

        then:
        assertOpenStackV2FaultResponse(deleteIdpResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_NOT_FOUND, ErrorCodes.ERROR_MESSAGE_IDP_NOT_FOUND))
    }


    def "Create a DOMAIN IDP with approvedDomains and single cert. Validate can immediately fed auth for user for approvedDomain"() {
        given:
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
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
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(creationResultIdp.issuer, username, expSecs, domainId, null, email, samlProducer1);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "success"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "auth user in different domain for IDP"
        samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(creationResultIdp.issuer, username, expSecs, "otherdomain", null, email, samlProducer1);
        samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "failure"
        samlResponse.status == HttpServletResponse.SC_FORBIDDEN

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)
        utils.deleteUsersQuietly(users)
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

        //create a tenant and add it to domain 1
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)

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
        def allIdp = cloud20.listIdentityProviders(idpManagerToken, null, null, null, requestContentType, requestContentType)

        then:
        allIdp.status == SC_OK
        IdentityProviders providers = allIdp.getEntity(IdentityProviders.class)
        providers != null
        providers.identityProvider.size() >= 4
        providers.identityProvider.find{it.id == idp1.id} != null
        providers.identityProvider.find{it.id == idp2.id} != null
        providers.identityProvider.find{it.id == idp3.id} != null
        providers.identityProvider.find{it.id == idp4.id} != null

        when: "get all idps for specific domain"
        def domainSpecificIdpResponse = cloud20.listIdentityProviders(idpManagerToken, domainId, null, null, requestContentType, requestContentType)

        then: "get all idps for that domain and all global domain idps"
        domainSpecificIdpResponse.status == SC_OK
        IdentityProviders domainSpecificIdps = domainSpecificIdpResponse.getEntity(IdentityProviders.class)
        domainSpecificIdps != null
        domainSpecificIdps.identityProvider.size() >= 4
        domainSpecificIdps.identityProvider.find{it.id == idp1.id} != null
        domainSpecificIdps.identityProvider.find{it.id == idp2.id} == null
        domainSpecificIdps.identityProvider.find{it.id == idp3.id} != null
        domainSpecificIdps.identityProvider.find{it.id == idp4.id} == null

        when: "get all idps that have an EXPLICIT domain mapping"
        def onlyExplicitResponse = cloud20.listIdentityProviders(idpManagerToken, null, IdentityProviderTypeFilterEnum.EXPLICIT.name(), null, requestContentType, requestContentType)

        then: "all idps with an EXPLICIT domain mapping are returned"
        onlyExplicitResponse.status == SC_OK
        def onlyExplicitIdps = onlyExplicitResponse.getEntity(IdentityProviders.class)
        onlyExplicitIdps != null
        onlyExplicitIdps.identityProvider.find{it.id == idp1.id} != null
        onlyExplicitIdps.identityProvider.find{it.id == idp2.id} != null
        onlyExplicitIdps.identityProvider.find{it.id == idp3.id} == null
        onlyExplicitIdps.identityProvider.find{it.id == idp4.id} == null

        when: "get all idps that have an EXPLICIT domain mapping and for a specific domain"
        def domainAndExplicitResponse = cloud20.listIdentityProviders(idpManagerToken, domainId, IdentityProviderTypeFilterEnum.EXPLICIT.name(), null, requestContentType, requestContentType)

        then: "all idps with an EXPLICIT domain mapping are returned"
        domainAndExplicitResponse.status == SC_OK
        def domainAndExplicitIdps = domainAndExplicitResponse.getEntity(IdentityProviders.class)
        domainAndExplicitIdps != null
        domainAndExplicitIdps.identityProvider.find{it.id == idp1.id} != null
        domainAndExplicitIdps.identityProvider.find{it.id == idp2.id} == null
        domainAndExplicitIdps.identityProvider.find{it.id == idp3.id} == null
        domainAndExplicitIdps.identityProvider.find{it.id == idp4.id} == null

        when: "get all idps that have a DOMAIN mapping and for a specific TENANT"
        def tenantResponse = cloud20.listIdentityProviders(idpManagerToken, null, null, tenant.id, requestContentType, requestContentType)

        then: "all idps with a domain mapping for the tenant are returned"
        tenantResponse.status == SC_OK
        def tenantIdps = tenantResponse.getEntity(IdentityProviders.class)
        tenantIdps != null
        tenantIdps.identityProvider.find{it.id == idp1.id} != null
        tenantIdps.identityProvider.find{it.id == idp2.id} == null
        tenantIdps.identityProvider.find{it.id == idp3.id} != null
        tenantIdps.identityProvider.find{it.id == idp4.id} == null

        when: "get all idps that have an EXPLICIT DOMAIN mapping and for a specific TENANT"
        def tenantAndExplicitResponse = cloud20.listIdentityProviders(idpManagerToken, null, IdentityProviderTypeFilterEnum.EXPLICIT.name(), tenant.id, requestContentType, requestContentType)

        then: "all idps with an EXPLICIT domain mapping for the tenant are returned"
        tenantAndExplicitResponse.status == SC_OK
        def tenantAndExplicitIdps = tenantAndExplicitResponse.getEntity(IdentityProviders.class)
        tenantAndExplicitIdps != null
        tenantAndExplicitIdps.identityProvider.find{it.id == idp1.id} != null
        tenantAndExplicitIdps.identityProvider.find{it.id == idp2.id} == null
        tenantAndExplicitIdps.identityProvider.find{it.id == idp3.id} == null
        tenantAndExplicitIdps.identityProvider.find{it.id == idp4.id} == null

        cleanup:
        if (idp1) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, idp1.id)
        }
        if (idp2) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, idp2.id)
        }
        if (idp3) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, idp3.id)
        }
        if (idp4) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, idp4.id)
        }

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "list IDPs by type filter returns 400 for any value of 'idpType' other than EXPLICIT"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, null, "invalidTypeFilter")

        then:
        response.status == 400
    }

    def "list IDPs returns 400 is tenant and domain IDs are given as filters"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, "someDomain", null, "someTenant")

        then:
        response.status == 400
    }

    @Unroll
    def "list IDPs returns empty list if the tenant being filtered by does not exist, accept = #accept"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, "someTenant", accept, accept)

        then:
        response.status == 200
        if (accept == MediaType.APPLICATION_XML_TYPE) {
            def idps = response.getEntity(IdentityProviders.class)
            assert idps.identityProvider.size() == 0
        } else {
            def idps = new JsonSlurper().parseText(response.getEntity(String))['RAX-AUTH:identityProviders']
            assert idps.size() == 0
        }

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "list IDPs returns empty list if the domain being filtered by does not exist, accept = #accept"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, "domainDoesNotExist", null, null, accept, accept)

        then:
        response.status == 200
        if (accept == MediaType.APPLICATION_XML_TYPE) {
            def idps = response.getEntity(IdentityProviders.class)
            assert idps.identityProvider.size() == 0
        } else {
            def idps = new JsonSlurper().parseText(response.getEntity(String))['RAX-AUTH:identityProviders']
            assert idps.size() == 0
        }

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "list IDPs returns 400 if the tenant being filtered by belongs to the default domain"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def tenant = utils.createTenant()

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, tenant.id)

        then:
        response.status == 400
    }

    def "list IDPs returns 400 if the tenant being filtered by has NULL as the associated domain"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def tenant = utils.createTenant()
        def tenantEntity = tenantService.getTenant(tenant.id)
        tenantEntity.domainId = null
        tenantDao.updateTenantAsIs(tenantEntity)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, tenant.id)

        then:
        response.status == 400
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

        when: "Domain IDP with both approvedDomains and approvedDomainGroup"
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

    def "Create domain with dup domainIds ignores dups"() {
        given:
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "create a DOMAIN IDP with single certs and approvedDomains"
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId, domainId])
        def response = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp)

        then: "created successfully"
        response.status == SC_CREATED
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)
        creationResultIdp.approvedDomainIds != null
        creationResultIdp.approvedDomainIds.approvedDomainId.size() == 1
        creationResultIdp.approvedDomainIds.approvedDomainId.get(0) == domainId

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)
    }


    def "Deleting IDP, deletes all its fed users"() {
        given:
        def idpManagerToken = utils.getServiceAdminToken()
        def domainId = utils.createDomain()

        def (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        def username = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"

        //create a new IDP
        def pem = IOUtils.toString(new ClassPathResource(Constants.DEFAULT_IDP_PUBLIC_KEY).getInputStream()).replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "").replace("\n", "")
        def pubCerts1 = v2Factory.createPublicCertificate(pem)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)
        IdentityProvider idp = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.publicCertificates = publicCertificates
            it
        }
        def response = cloud20.createIdentityProvider(idpManagerToken, idp)
        assert response.status == SC_CREATED
        IdentityProvider identityProvider = response.getEntity(IdentityProvider)

        //create a fed user for that IDP
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(idp.issuer, username, expSecs, domainId, null, email);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        assert samlResponse.status == SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def fedUserId = authResponse.user.id
        utils.getUserById(fedUserId, idpManagerToken)

        when: "delete the provider"
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, identityProvider.id)

        then: "All associated fed users are deleted"
        assert cloud20.getUserById(idpManagerToken, fedUserId).status == SC_NOT_FOUND
    }

    def "test get IDP access" () {
        given:
        def user = utils.createIdentityAdmin()
        def token = utils.getToken(user.username)
        def idp = utils.createIdentityProvider()

        when: "try to get IDP w/o any federation access role"
        def response = cloud20.getIdentityProvider(token, idp.id)

        then: "not authorized"
        response.status == 403

        when: "add the IDP manager role to the user"
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        response = cloud20.getIdentityProvider(token, idp.id)

        then: "authorized"
        response.status == 200

        when: "add the IDP read only role to the user"
        utils.deleteRoleOnUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_READ_ONLY_ROLE_ID)
        response = cloud20.getIdentityProvider(token, idp.id)

        then: "authorized"
        response.status == 200

        cleanup:
        utils.deleteUser(user)
        utils.deleteIdentityProvider(idp)
    }

    def "test list IDPs access" () {
        given:
        def user = utils.createIdentityAdmin()
        def token = utils.getToken(user.username)

        when: "try to get IDPs w/o any federation access role"
        def response = cloud20.listIdentityProviders(token)

        then: "not authorized"
        response.status == 403

        when: "add the IDP manager role to the user"
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        response = cloud20.listIdentityProviders(token)

        then: "authorized"
        response.status == 200

        when: "add the IDP read only role to the user"
        utils.deleteRoleOnUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_READ_ONLY_ROLE_ID)
        response = cloud20.listIdentityProviders(token)

        then: "authorized"
        response.status == 200

        cleanup:
        utils.deleteUser(user)
    }

    def "test create IDP access" () {
        given:
        def user = utils.createIdentityAdmin()
        def token = utils.getToken(user.username)
        def idpRequest = v2Factory.createIdentityProvider("blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN)

        when: "try to create the IDP w/o any federation access role"
        def response = cloud20.createIdentityProvider(token, idpRequest)

        then: "not authorized"
        response.status == 403

        when: "add the IDP manager role to the user"
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        response = cloud20.createIdentityProvider(token, idpRequest)

        then: "authorized"
        response.status == 201
        def idp = response.getEntity(IdentityProvider)

        when: "add the IDP read only role to the user"
        utils.deleteRoleOnUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_READ_ONLY_ROLE_ID)
        utils.deleteIdentityProvider(idp)
        response = cloud20.createIdentityProvider(token, idpRequest)

        then: "authorized"
        response.status == 403

        cleanup:
        utils.deleteUser(user)
    }

    def "test delete IDP access"() {
        given:
        def user = utils.createIdentityAdmin()
        def token = utils.getToken(user.username)
        def idp = utils.createIdentityProvider()

        when: "try to delete the IDP w/o any federation access role"
        def response = cloud20.deleteIdentityProvider(token, idp.id)

        then: "not authorized"
        response.status == 403

        when: "add the IDP manager role to the user"
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        response = cloud20.deleteIdentityProvider(token, idp.id)

        then: "authorized"
        response.status == 204

        when: "add the IDP read only role to the user"
        utils.deleteRoleOnUser(user, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        utils.addRoleToUser(user, Constants.IDENTITY_PROVIDER_READ_ONLY_ROLE_ID)
        response = cloud20.deleteIdentityProvider(token, idp.id)

        then: "authorized"
        response.status == 403

        cleanup:
        utils.deleteUser(user)
    }

}
