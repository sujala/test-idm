package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.api.converter.cloudv20.IdentityProviderConverterCloudV20
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.FederatedIdentityService
import com.rackspace.idm.domain.service.IdentityProviderTypeFilterEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultFederatedIdentityService
import com.rackspace.idm.validation.Validator20
import groovy.json.JsonSlurper
import groovy.xml.XmlUtil
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.mockserver.verify.VerificationTimes
import org.opensaml.saml.saml2.metadata.EntityDescriptor
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorImpl
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.SamlProducer
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import java.util.regex.Pattern

import static org.apache.http.HttpStatus.*
import static org.opensaml.saml.common.xml.SAMLConstants.SAML20MD_NS
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponse
import static testHelpers.IdmAssert.assertOpenStackV2FaultResponseWithErrorCode

class IdentityProviderCRUDIntegrationTest extends RootIntegrationTest {

    @Autowired
    TenantService tenantService

    @Autowired
    TenantDao tenantDao

    @Autowired
    FederatedIdentityService federatedIdentityService

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    UserService userService

    @Autowired
    IdentityProviderDao identityProviderDao

    @Unroll
    def "CRUD a DOMAIN IDP with approvedDomainGroup, but no certs - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "create a DOMAIN IDP with no certs and approvedDomainGroup"
        resetCloudFeedsMock()
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED
        creationResultIdp.enabled
        creationResultIdp.approvedDomainGroup == domainGroupIdp.approvedDomainGroup
        creationResultIdp.approvedDomainIds == null
        creationResultIdp.description == domainGroupIdp.description
        creationResultIdp.issuer == domainGroupIdp.issuer
        creationResultIdp.authenticationUrl == domainGroupIdp.authenticationUrl
        creationResultIdp.publicCertificates == null
        creationResultIdp.federationType == IdentityProviderFederationTypeEnum.DOMAIN
        creationResultIdp.id != null
        creationResultIdp.name != null
        response.headers.getFirst("Location") != null
        response.headers.getFirst("Location").contains(creationResultIdp.id)

        and: "only one event was posted"
        def idpEntity = federatedIdentityService.getIdentityProviderByIssuer(domainGroupIdp.issuer)
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was a CREATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.CREATE),
                VerificationTimes.exactly(1)
        )

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
        getResultIdp.name != null

        when: "update the provider"
        resetCloudFeedsMock()
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

        when: "delete the provider"
        resetCloudFeedsMock()
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT
        cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType).status == SC_NOT_FOUND

        and: "only one event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was a DELETE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.DELETE),
                VerificationTimes.exactly(1)
        )

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
    def "disabling an IDP revokes all tokens for users within the IDP - #creationType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def idpCredential = SamlCredentialUtils.generateX509Credential()
        def samlProducer = new SamlProducer(idpCredential)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(idpCredential.entityCertificate)
        def issuer = UUID.randomUUID().toString()
        def idpUrl = UUID.randomUUID().toString()
        IdentityProvider idp
        if (creationType == IdpCreationType.METADATA) {
            String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, idpUrl, [pubCertPemString])
            idp = cloud20.createIdentityProviderWithMetadata(utils.getToken(userAdmin.username), metadata).getEntity(IdentityProvider)
        } else {
            def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)
            def publicCertificates = v2Factory.createPublicCertificates(pubCerts)

            def idpData = v2Factory.createIdentityProvider(issuer, "blah", idpUrl, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL, null).with {
                it.publicCertificates = publicCertificates
                it
            }
            idp = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData).getEntity(IdentityProvider)
        }
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(idp.issuer, RandomStringUtils.randomAlphanumeric(8), 1000, domainId, null, "${RandomStringUtils.randomAlphanumeric(8)}@example.com", samlProducer)
        def federationResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(federationResponse.user.id)
        def federatedUserToken = federationResponse.token.id
        def idpEntity = identityProviderDao.getIdentityProviderByName(idp.name)
        def brokerCredential = SamlCredentialUtils.generateX509Credential()
        def brokerIdp = cloud20.createIdentityProviderWithCred(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.BROKER, brokerCredential).getEntity(IdentityProvider)
        def v2authRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerCredential, idpCredential)

        when: "disable the IDP"
        resetCloudFeedsMock()
        def idpRequest = new IdentityProvider().with {
            it.enabled = false
            it
        }
        def response = cloud20.updateIdentityProvider(utils.getServiceAdminToken(), idp.id, idpRequest)

        then: "the IDP is disabled"
        response.status == 200
        def updatedIdp = response.getEntity(IdentityProvider)
        !updatedIdp.enabled

        and: "an update event was sent for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        and: "and the user TRR event was sent"
        cloudFeedsMock.verify(
                testUtils.createUserTrrFeedsRequest(federatedUser),
                VerificationTimes.exactly(1)
        )

        when: "get the IDP"
        def getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idp.id)

        then: "still disabled"
        !getIdp.enabled

        when: "validate the token returned prior to disabling the IDP"
        def validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserToken)

        then:
        validateResponse.status == 404

        when: "try to auth against the IDP using v1 fed auth"
        response = cloud20.samlAuthenticate(samlAssertion)

        then:
        response.status == 403

        when: "try to auth against the IDP using v2 fed auth"
        def v2AuthRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = domainId
            it.validitySeconds = 1000
            it.brokerIssuer = brokerIdp.issuer
            it.originIssuer = idp.issuer
            it.email = "${RandomStringUtils.randomAlphanumeric(8)}@example.com"
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = federatedUser.username
            it.roleNames = [] as Set
            it
        }
        def samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        response = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse))

        then:
        response.status == 403

        when: "enable the IDP and auth again"
        idpRequest = new IdentityProvider().with {
            it.enabled = true
            it
        }
        cloud20.updateIdentityProvider(utils.getServiceAdminToken(), idp.id, idpRequest)
        response = cloud20.samlAuthenticate(samlAssertion)

        then:
        response.status == 200

        when: "validate the token returned prior to disabling the IDP, after re-enabling the IDP"
        validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserToken)

        then:
        validateResponse.status == 404

        when: "validate the token returned prior to disabling the IDP, after deleting the IDP"
        utils.deleteIdentityProviderQuietly(utils.getServiceAdminToken(), idp.id)
        validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserToken)

        then:
        validateResponse.status == 404

        cleanup:
        utils.deleteUserQuietly(userAdmin)

        where:
        creationType << IdpCreationType.values()
    }

    def "impersonating a federated user is allowed for disabled IDPs"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def idpCredential = SamlCredentialUtils.generateX509Credential()
        def samlProducer = new SamlProducer(idpCredential)
        IdentityProvider idp = cloud20.createIdentityProviderWithCred(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.DOMAIN, idpCredential).getEntity(IdentityProvider)
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(idp.issuer, RandomStringUtils.randomAlphanumeric(8), 1000, domainId, null, "${RandomStringUtils.randomAlphanumeric(8)}@example.com", samlProducer)
        def response = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(response.user.id)

        when: "impersonate the federated user"
        def impersonationResponse = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then:
        impersonationResponse.status == 200
        def impersonationToken = impersonationResponse.getEntity(ImpersonationResponse).token.id

        when: "disable the IDP"
        resetCloudFeedsMock()
        def idpRequest = new IdentityProvider().with {
            it.enabled = false
            it
        }
        response = cloud20.updateIdentityProvider(utils.getServiceAdminToken(), idp.id, idpRequest)

        then: "the IDP is disabled"
        response.status == 200
        def updatedIdp = response.getEntity(IdentityProvider)
        !updatedIdp.enabled

        and: "and the user TRR event was sent"
        cloudFeedsMock.verify(
                testUtils.createUserTrrFeedsRequest(federatedUser),
                VerificationTimes.exactly(1)
        )

        and: "and no token TRR event was sent"
        cloudFeedsMock.verify(
                testUtils.createTokenFeedsRequest(impersonationToken),
                VerificationTimes.exactly(0)
        )

        when: "validate the impersonation token w/ the IDP disabled"
        def validateResponse = cloud20.validateToken(impersonationToken, impersonationToken)

        then:
        validateResponse.status == 200

        when: "impersonate the federated user again"
        def impersonationResponse2 = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then:
        impersonationResponse2.status == 200
        def impersonationToken2 = impersonationResponse2.getEntity(ImpersonationResponse).token.id

        when: "validate the impersonation token"
        validateResponse = cloud20.validateToken(impersonationToken2, impersonationToken2)

        then:
        validateResponse.status == 200

        when: "impersonate the federated user again, after IDP is deleted"
        utils.deleteIdentityProviderQuietly(utils.getServiceAdminToken(), idp.id)
        def impersonationResponse3 = cloud20.impersonate(utils.getIdentityAdminToken(), federatedUser)

        then:
        impersonationResponse3.status == 404

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

    @Unroll
    def "cannot disable a #idpType IDP"() {
        given:
        def cred = SamlCredentialUtils.generateX509Credential()
        def idp = cloud20.createIdentityProviderWithCred(utils.getServiceAdminToken(), idpType, cred).getEntity(IdentityProvider)

        when:
        def idpRequest = new IdentityProvider().with {
            it.enabled = false
            it
        }
        def response = cloud20.updateIdentityProvider(utils.getServiceAdminToken(), idp.id, idpRequest)

        then:
        response.status == 200
        def updatedIdp = response.getEntity(IdentityProvider)
        updatedIdp.enabled

        when: "get the IDP"
        def getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idp.id)

        then: "still enabled"
        getIdp.enabled

        where:
        idpType << [IdentityProviderFederationTypeEnum.RACKER, IdentityProviderFederationTypeEnum.BROKER]
    }

    def "IDPs with a null enabled attribute still behave as if they were enabled"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def cred = SamlCredentialUtils.generateX509Credential()
        def idpEntityData = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.enabled = null
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL.name()
            it.authenticationUrl = "http://${RandomStringUtils.randomAlphanumeric(8)}.com"
            it.uri = "http://${RandomStringUtils.randomAlphanumeric(8)}.com"
            it.federationType = IdentityProviderFederationTypeEnum.DOMAIN
            it.approvedDomainIds = [domainId] as List
            it.description = RandomStringUtils.randomAlphanumeric(16)
            it.providerId = RandomStringUtils.randomAlphanumeric(36)
            it.addUserCertificate(cred.entityCertificate)
            it
        }
        identityProviderDao.addIdentityProvider(idpEntityData)
        def samlProducer = new SamlProducer(cred)
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(idpEntityData.uri, RandomStringUtils.randomAlphanumeric(8), 1000, domainId, null, "${RandomStringUtils.randomAlphanumeric(8)}@example.com", samlProducer)


        when: "get the IDP"
        def getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idpEntityData.providerId)

        then:
        getIdp.enabled

        when: "try to authenticate to the IDP"
        def federationResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        federationResponse.status == 200
        def fedAuthResponseData = federationResponse.getEntity(AuthenticateResponse).value
        def federatedToken = fedAuthResponseData.token.id
        def federatedUser = utils.getUserById(fedAuthResponseData.user.id)

        when: "disable the IDP"
        resetCloudFeedsMock()
        def idpRequest = new IdentityProvider().with {
            it.enabled = false
            it
        }
        def response = cloud20.updateIdentityProvider(utils.getServiceAdminToken(), getIdp.id, idpRequest)

        then: "the IDP is disabled"
        response.status == 200
        def updatedIdp = response.getEntity(IdentityProvider)
        !updatedIdp.enabled

        and: "an update event was sent for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntityData, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        and: "and the user TRR event was sent"
        cloudFeedsMock.verify(
                testUtils.createUserTrrFeedsRequest(federatedUser),
                VerificationTimes.exactly(1)
        )

        when: "get the IDP"
        getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), getIdp.id)

        then: "still disabled"
        !getIdp.enabled

        when: "validate the token returned prior to disabling the IDP"
        def validateResponse = cloud20.validateToken(utils.getServiceAdminToken(), federatedToken)

        then:
        validateResponse.status == 404

        cleanup:
        utils.deleteIdentityProviderQuietly(utils.getServiceAdminToken(), idpEntityData.providerId)
        utils.deleteUserQuietly(userAdmin)
    }

    @Unroll
    def "can create an IDP with enabled == #enabledValue"() {
        given:
        def idpData = v2Factory.createIdentityProvider(UUID.randomUUID().toString(), "blah", UUID.randomUUID().toString(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.name(), null).with {
            it.publicCertificates = publicCertificates
            it.enabled = enabledValue
            it
        }

        when: "create the IDP"
        def response = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData)

        then:
        response.status == 201
        def idp = response.getEntity(IdentityProvider)
        idp.enabled == (enabledValue == null ? true : enabledValue)

        when: "get the IDP"
        def getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idp.id)

        then:
        getIdp.enabled == (enabledValue == null ? true : enabledValue)

        cleanup:
        utils.deleteIdentityProviderQuietly(utils.getServiceAdminToken(), idp.id)

        where:
        enabledValue << [true, false, null]
    }

    @Unroll
    def "CRUD a DOMAIN IDP with approvedDomainIds, but no certs - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domain = utils.createDomainEntity()

        when: "create a DOMAIN IDP with no certs and approvedDomainGroup"
        resetCloudFeedsMock()
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id].asList())
        def response = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED
        creationResultIdp.approvedDomainGroup == null
        creationResultIdp.approvedDomainIds.approvedDomainId == [domain.id].asList()
        creationResultIdp.description == approvedDomainsIdp.description
        creationResultIdp.issuer == approvedDomainsIdp.issuer
        creationResultIdp.authenticationUrl == approvedDomainsIdp.authenticationUrl
        creationResultIdp.publicCertificates == null
        creationResultIdp.federationType == IdentityProviderFederationTypeEnum.DOMAIN
        creationResultIdp.id != null
        creationResultIdp.name != null
        response.headers.getFirst("Location") != null
        response.headers.getFirst("Location").contains(creationResultIdp.id)

        and: "only one event was posted"
        def idpEntity = federatedIdentityService.getIdentityProviderByIssuer(approvedDomainsIdp.issuer)
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was a CREATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.CREATE),
                VerificationTimes.exactly(1)
        )

        when: "get the DOMAIN group IDP"
        def getIdpResponse = cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType)
        IdentityProvider getResultIdp = getIdpResponse.getEntity(IdentityProvider)

        then: "contains appropriate info"
        getIdpResponse.status == SC_OK
        getResultIdp.approvedDomainGroup == null
        getResultIdp.approvedDomainIds.approvedDomainId == [domain.id].asList()
        getResultIdp.description == approvedDomainsIdp.description
        getResultIdp.issuer == approvedDomainsIdp.issuer
        getResultIdp.authenticationUrl == approvedDomainsIdp.authenticationUrl
        getResultIdp.federationType == IdentityProviderFederationTypeEnum.DOMAIN
        getResultIdp.publicCertificates == null
        getResultIdp.id != null
        getResultIdp.name != null

        when: "update the provider"
        resetCloudFeedsMock()
        def newAuthUrl = RandomStringUtils.randomAlphanumeric(10)
        def domain2 = utils.createDomainEntity()
        IdentityProvider updateApprovedDomainsIdp = new IdentityProvider().with {
            it.authenticationUrl = newAuthUrl
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.approvedDomainId.addAll([domain.id, domain2.id].asList())
            it.approvedDomainIds = approvedDomainIds
            it
        }
        def updateIdpResponse = cloud20.updateIdentityProvider(idpManagerToken, creationResultIdp.id, updateApprovedDomainsIdp, requestContentType, requestContentType)

        then: "idp updated"
        updateIdpResponse.status == SC_OK
        IdentityProvider updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)
        updateResultIdp.authenticationUrl == newAuthUrl
        updateResultIdp.approvedDomainIds.approvedDomainId.size() == 2
        updateResultIdp.approvedDomainIds.approvedDomainId == [domain.id, domain2.id].asList()

        def getAfterUpdateIdpResponse = cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType)
        IdentityProvider getAfterUpdateIdp = getAfterUpdateIdpResponse.getEntity(IdentityProvider)
        getAfterUpdateIdp.authenticationUrl == newAuthUrl
        updateResultIdp.approvedDomainIds.approvedDomainId.size() == 2
        updateResultIdp.approvedDomainIds.approvedDomainId == [domain.id, domain2.id].asList()

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

        when: "delete the provider"
        resetCloudFeedsMock()
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT
        cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType).status == SC_NOT_FOUND

        and: "only one event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was a DELETE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.DELETE),
                VerificationTimes.exactly(1)
        )

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)
        utils.deleteDomain(domain.id)
        utils.deleteDomain(domain2.id)

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
        resetCloudFeedsMock()
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, null)
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
        creationResultIdp.name != null
        creationResultIdp.federationType == IdentityProviderFederationTypeEnum.RACKER
        response.headers.getFirst("Location") != null
        response.headers.getFirst("Location").contains(creationResultIdp.id)

        and: "only one event was posted"
        def idpEntity = federatedIdentityService.getIdentityProviderByIssuer(domainGroupIdp.issuer)
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was a CREATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.CREATE),
                VerificationTimes.exactly(1)
        )

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
        getResultIdp.name != null

        when: "delete the provider"
        resetCloudFeedsMock()
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT
        cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType).status == SC_NOT_FOUND

        and: "only one event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was a DELETE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.DELETE),
                VerificationTimes.exactly(1)
        )

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

    def "Create IDP with name having '_' and ':'" () {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idpName = testUtils.getRandomUUID("test:example_1")

        when: "create a IDP"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(idpName, "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        creationResultIdp.name == idpName

        cleanup:
        utils.deleteUser(idpManager)
        utils.deleteIdentityProvider(creationResultIdp)
    }

    def "Create IDP using Metadata" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "Creating IDP using metadata"
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        Pattern pattern = Pattern.compile(".*/cloud/v2.0/RAX-AUTH/federation/identity-providers/" + idp.id)
        response.headers.get("Location").get(0).toString().matches(pattern)
        idp.issuer == issuer
        idp.authenticationUrl == authenticationUrl
        idp.name == domainId
        idp.approvedDomainIds.approvedDomainId.size() == 1
        idp.approvedDomainIds.approvedDomainId.get(0) == domainId
        idp.description == domainId
        idp.publicCertificates.publicCertificate.get(0).pemEncoded != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
    }

    def "Create IDP using Metadata using dedicated domain format name" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = testUtils.getRandomUUIDOfLength("dedicated:123", 30)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "Creating IDP using metadata"
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        idp.name == domainId.substring(0, IdentityProviderConverterCloudV20.METADATA_IDP_MAX_NAME_SIZE)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
    }

    @Unroll
    def "test update IDP using Metadata supports accept content type #accept" () {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def idp = utils.createIdentityProvider(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.DOMAIN, domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        String issuer = idp.issuer
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)

        when: "update IDP using metadata"
        def response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata, accept)

        then:
        response.status == SC_OK
        def responseIdp
        if (accept == MediaType.APPLICATION_XML_TYPE) {
            responseIdp = response.getEntity(IdentityProvider)
        } else {
            responseIdp = new JsonSlurper().parseText(response.getEntity(String))['RAX-AUTH:identityProvider']
        }
        responseIdp.issuer == issuer
        responseIdp.authenticationUrl == authenticationUrl
        responseIdp.metadata == null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "test update IDP using Metadata user access" () {
        given:
        def domainId1 = utils.createDomain()
        def domainId2 = utils.createDomain()
        def identityAdmin1, userAdmin1, userManager1, defaultUser1, users1
        def identityAdmin2, userAdmin2, userManager2, defaultUser2, users2
        (identityAdmin1, userAdmin1, userManager1, defaultUser1) = utils.createUsers(domainId1)
        users1 = [defaultUser1, userManager1, userAdmin1, identityAdmin1]
        (identityAdmin2, userAdmin2, userManager2, defaultUser2) = utils.createUsers(domainId2)
        users2 = [defaultUser2, userManager2, userAdmin2, identityAdmin2]

        def rcn = testUtils.getRandomRCN()
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = rcn
            it
        }
        utils.updateDomain(domainId1, updateDomainEntity)

        // Create the IDP in domain 1
        def idp = utils.createIdentityProvider(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.DOMAIN, domainId1)

        when: "update IDP using user admin"
        String metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        def response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(userAdmin1.username), idp.id, metadata)

        then:
        response.status == SC_OK

        when: "update IDP using user manager"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(userManager1.username), idp.id, metadata)

        then:
        response.status == SC_OK

        when: "try to update the IDP using the default user"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(defaultUser1.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        when: "add the rcn:admin role to the default user and try again"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        utils.addRoleToUser(defaultUser1, Constants.RCN_ADMIN_ROLE_ID)
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(defaultUser1.username), idp.id, metadata)

        then:
        response.status == SC_OK

        when: "update IDP using user admin in different domain not in RCN"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(userAdmin2.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        when: "update IDP using user manager in different domain not in RCN"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(userManager2.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        when: "try to update the IDP using the default user in different domain not in RCN"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(defaultUser2.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        when: "add the rcn:admin role to the default user and try again in different domain not in RCN"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        utils.addRoleToUser(defaultUser2, Constants.RCN_ADMIN_ROLE_ID)
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(defaultUser2.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        when: "add the domain to the RCN and try to update IDP using user admin in different domain in same RCN"
        utils.updateDomain(domainId2, updateDomainEntity)
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(userAdmin2.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        when: "update IDP using user manager in different domain in same RCN"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(userManager2.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        when: "update IDP using default user with rcn:admin role in different domain in same RCN"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(defaultUser2.username), idp.id, metadata)

        then:
        response.status == SC_OK

        when: "move the rcn:admin to a different RCN and try to update the IDP again"
        rcn = testUtils.getRandomRCN()
        updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = rcn
            it
        }
        utils.updateDomain(domainId2, updateDomainEntity)
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(defaultUser2.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId1)
        utils.deleteDomain(domainId2)
        utils.deleteIdentityProvider(idp)
    }

    enum IdpCreationType { METADATA, EXPLICITLY }

    @Unroll
    def "test update IDP using metadata that was created using #creationType" () {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        def idp
        if (creationType == IdpCreationType.METADATA) {
            String metadata = new SamlFactory().generateMetadataXMLForIDP(testUtils.getRandomUUID("http://example.com/"), testUtils.getRandomUUID("authenticationUrl"))
            def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
            idp = response.getEntity(IdentityProvider)
        } else {
            idp = utils.createIdentityProvider(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.DOMAIN, domainId)
        }

        String issuer = idp.issuer
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)

        when: "update IDP using metadata"
        def response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)
        IdentityProvider responseIdp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        responseIdp.issuer == issuer
        responseIdp.authenticationUrl == authenticationUrl
        responseIdp.approvedDomainIds.approvedDomainId.size() == 1
        responseIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        responseIdp.metadata == null

        when: "get IDP and verify that the values persisted correctly"
        def getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idp.id)

        then:
        getIdp.issuer == issuer
        getIdp.authenticationUrl == authenticationUrl
        getIdp.approvedDomainIds.approvedDomainId.size() == 1
        getIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        getIdp.metadata == null

        when: "get the IDP metadata and verify it was updated"
        def updatedMetadata = cloud20.getIdentityProviderMetadata(userAdminToken, idp.id)

        then:
        // we have to pretty print the xml so we can compare them as strings
        XmlUtil.serialize(updatedMetadata.getEntity(String)) == XmlUtil.serialize(metadata)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)

        where:
        creationType << [IdpCreationType.METADATA, IdpCreationType.EXPLICITLY]
    }

    def "test update IDP with more than 1 approved domain using metadata"() {
        given:
        def domainId = utils.createDomain()
        def domain2 = utils.createDomainEntity()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def idpData = v2Factory.createIdentityProvider(testUtils.getRandomUUID(), testUtils.getRandomUUID("My IDP - "), testUtils.getRandomUUID("http://example.com/"), IdentityProviderFederationTypeEnum.DOMAIN)
        idpData.approvedDomainIds = new ApprovedDomainIds().with {
            it.approvedDomainId = [domainId, domain2.id]
            it
        }
        idpData.approvedDomainGroup = null
        def idp = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData).getEntity(IdentityProvider)

        when:
        String metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, idp.authenticationUrl)
        def response = cloud20.updateIdentityProviderUsingMetadata(utils.getToken(userAdmin.username), idp.id, metadata)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domain2.id)
        utils.deleteIdentityProvider(idp)
    }

    @Unroll
    def "test update, get, and list IDPs using metadata with user in rax restricted group"() {
        given:
        def domainId = utils.createDomain()
        def users = utils.createUsers(domainId)
        def idpData = v2Factory.createIdentityProvider(testUtils.getRandomUUID(), testUtils.getRandomUUID("My IDP - "), testUtils.getRandomUUID("http://example.com/"), IdentityProviderFederationTypeEnum.DOMAIN)
        idpData.approvedDomainIds = new ApprovedDomainIds().with {
            it.approvedDomainId = [domainId]
            it
        }
        idpData.approvedDomainGroup = null
        def idp = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData).getEntity(IdentityProvider)
        def user = users.find { it.username =~ regex }
        def userToken = utils.getToken(user.username)

        when: "update w/o the restricted group"
        String metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        def response = cloud20.updateIdentityProviderUsingMetadata(userToken, idp.id, metadata)

        then: "success"
        response.status == SC_OK

        when: "Get IDP w/o the restricted group"
        response = cloud20.getIdentityProvider(userToken, idp.id)

        then: "Valid"
        response.status == SC_OK

        when: "List IDPs w/ the restricted group"
        response = cloud20.listIdentityProviders(userToken)

        then: "Valid"
        response.status == SC_OK

        when: "update w/ the restricted group to parent user"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(8))
        utils.addUserToGroupWithId(Constants.RAX_STATUS_RESTRICTED_GROUP_ID, users.find { it.username =~ "userAdmin*" })
        userToken = utils.getToken(user.username)
        response = cloud20.updateIdentityProviderUsingMetadata(userToken, idp.id, metadata)

        then: "error"
        response.status == SC_FORBIDDEN

        when: "get w/ the restricted group"
        response = cloud20.getIdentityProvider(userToken, idp.id)

        then: "error"
        response.status == SC_FORBIDDEN

        when: "List IDPs w/ the restricted group"
        response = cloud20.listIdentityProviders(userToken)

        then: "error"
        response.status == SC_FORBIDDEN

        when: "delete w/ the restricted group"
        response = cloud20.deleteIdentityProvider(userToken, idp.id)

        then: "error"
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users.reverse())
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)

        where:
        regex << ["userAdmin*", "userManage*"]
    }

    def "test update IDP error cases" () {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def idp = utils.createIdentityProvider(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.DOMAIN, domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "the request body is invalid xml"
        def response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, "I am not valid xml!!!")

        then:
        response.status == SC_BAD_REQUEST

        when: "the issuer in the metadata is not the issuer for the existing IDP"
        String metadata = new SamlFactory().generateMetadataXMLForIDP(RandomStringUtils.randomAlphabetic(8), idp.authenticationUrl)
        response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)

        then:
        response.status == SC_BAD_REQUEST

        when: "the auth URL is empty"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, "")
        response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)

        then:
        response.status == SC_BAD_REQUEST

        when: "the auth URL is null"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, null)
        response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)

        then:
        response.status == SC_BAD_REQUEST

        when: "the auth URL is longer than the max allowed length"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, RandomStringUtils.randomAlphabetic(Validator20.MAX_IDENTITY_PROVIDER_AUTH_URL + 1))
        response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)

        then:
        response.status == SC_BAD_REQUEST

        when: "provide an invalid cert"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, idp.authenticationUrl, ["invalidCert"])
        response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)

        then:
        response.status == SC_BAD_REQUEST

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
    }

    @Unroll
    def "test Update IDP certs using metadata for IDP created using #creationType" () {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        def idp
        if (creationType == IdpCreationType.METADATA) {
            String metadata = new SamlFactory().generateMetadataXMLForIDP(testUtils.getRandomUUID("http://example.com/"), testUtils.getRandomUUID("authenticationUrl"))
            def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
            idp = response.getEntity(IdentityProvider)
        } else {
            idp = utils.createIdentityProvider(utils.getServiceAdminToken(), IdentityProviderFederationTypeEnum.DOMAIN, domainId)
        }

        def keyPair1 = SamlCredentialUtils.generateKeyPair()
        def cert1 = SamlCredentialUtils.generateCertificate(keyPair1)
        def pubKey1 = SamlCredentialUtils.getCertificateAsPEMString(cert1)
        def keyPair2 = SamlCredentialUtils.generateKeyPair()
        def cert2 = SamlCredentialUtils.generateCertificate(keyPair2)
        def pubKey2 = SamlCredentialUtils.getCertificateAsPEMString(cert2)

        String metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, idp.authenticationUrl, [pubKey1])

        when: "update IDP using metadata w/ key 1"
        def response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)
        IdentityProvider responseIdp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        responseIdp.authenticationUrl == idp.authenticationUrl
        responseIdp.approvedDomainIds.approvedDomainId.size() == 1
        responseIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        responseIdp.publicCertificates.publicCertificate.size() == 1
        responseIdp.publicCertificates.publicCertificate[0].pemEncoded == pubKey1
        responseIdp.metadata == null

        when: "get IDP and verify that the values persisted correctly"
        def getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idp.id)

        then:
        getIdp.authenticationUrl == idp.authenticationUrl
        getIdp.approvedDomainIds.approvedDomainId.size() == 1
        getIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        getIdp.publicCertificates.publicCertificate.size() == 1
        getIdp.publicCertificates.publicCertificate[0].pemEncoded == pubKey1
        getIdp.metadata == null

        when: "update IDP using metadata w/ key 2"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, idp.authenticationUrl, [pubKey2])
        response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)
        responseIdp = response.getEntity(IdentityProvider)

        then: "key 1 is removed and only key 2 is on the IDP"
        response.status == SC_OK
        responseIdp.authenticationUrl == idp.authenticationUrl
        responseIdp.approvedDomainIds.approvedDomainId.size() == 1
        responseIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        responseIdp.publicCertificates.publicCertificate.size() == 1
        responseIdp.publicCertificates.publicCertificate[0].pemEncoded == pubKey2
        responseIdp.metadata == null

        when: "get IDP and verify that the values persisted correctly"
        getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idp.id)

        then:
        getIdp.authenticationUrl == idp.authenticationUrl
        getIdp.approvedDomainIds.approvedDomainId.size() == 1
        getIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        getIdp.publicCertificates.publicCertificate.size() == 1
        getIdp.publicCertificates.publicCertificate[0].pemEncoded == pubKey2
        getIdp.metadata == null

        when: "update IDP using metadata w/ key 1 and key 2"
        metadata = new SamlFactory().generateMetadataXMLForIDP(idp.issuer, idp.authenticationUrl, [pubKey1, pubKey2])
        response = cloud20.updateIdentityProviderUsingMetadata(userAdminToken, idp.id, metadata)
        responseIdp = response.getEntity(IdentityProvider)

        then: "both key 1 and key 2 are on the IDP"
        response.status == SC_OK
        responseIdp.authenticationUrl == idp.authenticationUrl
        responseIdp.approvedDomainIds.approvedDomainId.size() == 1
        responseIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        responseIdp.publicCertificates.publicCertificate.size() == 2
        responseIdp.publicCertificates.publicCertificate.find {it -> it.pemEncoded == pubKey1} != null
        responseIdp.publicCertificates.publicCertificate.find {it -> it.pemEncoded == pubKey2} != null
        responseIdp.metadata == null

        when: "get IDP and verify that the values persisted correctly"
        getIdp = utils.getIdentityProvider(utils.getServiceAdminToken(), idp.id)

        then:
        getIdp.authenticationUrl == idp.authenticationUrl
        getIdp.approvedDomainIds.approvedDomainId.size() == 1
        getIdp.approvedDomainIds.approvedDomainId.get(0) == domainId
        getIdp.publicCertificates.publicCertificate.size() == 2
        responseIdp.publicCertificates.publicCertificate.find {it -> it.pemEncoded == pubKey1} != null
        responseIdp.publicCertificates.publicCertificate.find {it -> it.pemEncoded == pubKey2} != null
        getIdp.metadata == null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)

        where:
        creationType << [IdpCreationType.METADATA, IdpCreationType.EXPLICITLY]
    }

    def "Verify users with roles identity:user-admin, identity:user-manage, rcn:admin can create, get and delete IDP using metadata" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "Creating IDP using userAdmin token"
        def userAdminToken = utils.getToken(userAdmin.username)
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)
        Pattern pattern = Pattern.compile(".*/cloud/v2.0/RAX-AUTH/federation/identity-providers/" + idp.id)

        then:
        response.status == SC_CREATED
        response.headers.get("Location").get(0).toString().matches(pattern)
        idp.issuer == issuer
        idp.authenticationUrl == authenticationUrl

        when: "Getting IDP using userAdmin token"
        response = cloud20.getIdentityProvider(userAdminToken, idp.id)
        idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        idp.issuer == issuer
        idp.authenticationUrl == authenticationUrl
        idp.approvedDomainIds.approvedDomainId.get(0) == domainId

        when: "Deleting IDP using userAdmin token"
        response = cloud20.deleteIdentityProvider(userAdminToken, idp.id)

        then:
        response.status == SC_NO_CONTENT

        when: "Creating IDP using userManage token"
        def userManageToken = utils.getToken(userManage.username)
        issuer = testUtils.getRandomUUID("issuer")
        metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        response = cloud20.createIdentityProviderWithMetadata(userManageToken, metadata)
        def idp2 = response.getEntity(IdentityProvider)
        pattern = Pattern.compile(".*/cloud/v2.0/RAX-AUTH/federation/identity-providers/" + idp2.id)

        then:
        response.status == SC_CREATED
        response.headers.get("Location").get(0).toString().matches(pattern)
        idp2.issuer == issuer
        idp2.authenticationUrl == authenticationUrl

        when: "Getting IDP using userManage token"
        response = cloud20.getIdentityProvider(userManageToken, idp2.id)
        idp2 = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        idp2.issuer == issuer
        idp2.authenticationUrl == authenticationUrl
        idp2.approvedDomainIds.approvedDomainId.get(0) == domainId

        when: "Deleting IDP using userManage token"
        response = cloud20.deleteIdentityProvider(userManageToken, idp2.id)

        then:
        response.status == SC_NO_CONTENT

        when: "Creating IDP using defaultUser token with rcn:admin role"
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        def defaultUserToken = utils.getToken(defaultUser.username)
        issuer = testUtils.getRandomUUID("issuer")
        metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        response = cloud20.createIdentityProviderWithMetadata(defaultUserToken, metadata)
        def idp3 = response.getEntity(IdentityProvider)
        pattern = Pattern.compile(".*/cloud/v2.0/RAX-AUTH/federation/identity-providers/" + idp3.id)

        then:
        response.status == SC_CREATED
        response.headers.get("Location").get(0).toString().matches(pattern)
        idp3.issuer == issuer
        idp3.authenticationUrl == authenticationUrl

        when: "Getting IDP using defaultUser token with rcn:admin role"
        response = cloud20.getIdentityProvider(defaultUserToken, idp3.id)
        idp3 = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        idp3.issuer == issuer
        idp3.authenticationUrl == authenticationUrl
        idp3.approvedDomainIds.approvedDomainId.get(0) == domainId

        when: "Deleting IDP using defaultUser token with rcn:admin role"
        response = cloud20.deleteIdentityProvider(defaultUserToken, idp3.id)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Verify users with roles identity:user-admin, identity:user-manage, and rcn:admin can get IDP" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "Creating IDP using idpManager token"
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId].asList())
        def response = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        idp.issuer == issuer

        when: "Getting IDP using userAdmin token"
        def userAdminToken = utils.getToken(userAdmin.username)
        response = cloud20.getIdentityProvider(userAdminToken, idp.id)
        idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        idp.issuer == issuer

        when: "Getting IDP using userManage token"
        def userManageToken = utils.getToken(userManage.username)
        response = cloud20.getIdentityProvider(userManageToken, idp.id)
        idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        idp.issuer == issuer

        when: "Getting IDP using defaultUser token with rcn:admin role"
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.getIdentityProvider(defaultUserToken, idp.id)
        idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        idp.issuer == issuer

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
    }

    def "Create IDP using Metadata: Verify the name is updated with integer suffixes when IDP name already exists" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "Create IDP using metadata"
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        idp.name == domainId

        when: "Create second IDP using metadata"
        issuer = testUtils.getRandomUUID("issuer2")
        String metadata2 = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata2)
        def idp2 = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        idp2.name == String.format("%s_2", domainId)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
        utils.deleteIdentityProvider(idp2)
    }

    def "Error check create IDP with metadata" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = null;
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "Creating IDP using invalid metadata"
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, "invalid")

        then: "Assert badRequest"
        assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, "Invalid XML")

        when: "Creating IDP when IDPSSPDescriptor is null"
        EntityDescriptor entityDescriptor = new EntityDescriptorImpl(SAML20MD_NS, "EntityDescriptor", "md")
        entityDescriptor.entityID = issuer
        metadata = new SamlFactory().convertEntityDescriptorToString(entityDescriptor)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)

        then: "Assert badRequest"
        assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST,
                String.format("Invalid XML metadata: %s is a required element.",
                IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME))

        when: "Creating IDP when issuer is null"
        String invalidMetadata = new SamlFactory().generateMetadataXMLForIDP(null, authenticationUrl)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, invalidMetadata)

        then: "Assert badRequest"
        assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "Creating IDP when authenticationUrl is null"
        invalidMetadata = new SamlFactory().generateMetadataXMLForIDP(issuer, null)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, invalidMetadata)

        then: "Assert badRequest"
        assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "Creating IDP using same issuer"
        userAdminToken = utils.getToken(userAdmin.username)
        metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)
        assert response.status == SC_CREATED
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)

        then: "Assert badRequest"
        assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault , SC_CONFLICT, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS)

        when: "Exceed number of IDPs for the domain's limit"
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_MAX_IDP_PER_DOMAIN_PROP, 1)
        issuer = testUtils.getRandomUUID("issuer")
        metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)

        then: "Assert forbidden"
        assertOpenStackV2FaultResponseWithErrorCode(response, ForbiddenFault , SC_FORBIDDEN, ErrorCodes.ERROR_CODE_IDP_LIMIT_PER_DOMAIN)

        when: "Creating IDP using identity admin"
        response = cloud20.createIdentityProviderWithMetadata(utils.identityAdminToken, metadata)

        then: "Assert forbidden"
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "Creating IDP using service admin"
        response = cloud20.createIdentityProviderWithMetadata(utils.serviceAdminToken, metadata)

        then: "Assert forbidden"
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "Creating IDP using default user"
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.createIdentityProviderWithMetadata(defaultUserToken, metadata)

        then: "Assert forbidden"
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "Creating IDP using user with no domain"
        def userWithNoDomainToken = utils.getToken(Constants.USER_WITH_NO_DOMAIN_USERNAME)
        response = cloud20.createIdentityProviderWithMetadata(userWithNoDomainToken, metadata)

        then: "Assert forbidden"
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "Creating IDP using user admin user w/ 'rax_status_restricted' group"
        def raxStatusRestrictedGroup = v2Factory.createGroup(Constants.RAX_STATUS_RESTRICTED_GROUP_NAME).with {
            it.id = Constants.RAX_STATUS_RESTRICTED_GROUP_ID
            it
        }
        utils.addUserToGroup(raxStatusRestrictedGroup, userAdmin)
        userAdminToken = utils.getToken(userAdmin.username)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        utils.removeUserFromGroup(raxStatusRestrictedGroup, userAdmin)

        then: "Assert forbidden"
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "Creating IDP using user admin in default domain"
        def user = new User().with {
            it.id = userAdmin.id
            it.domainId = identityConfig.getReloadableConfig().getGroupDefaultDomainId()
            it
        }
        userService.updateUser(user)
        userAdminToken = utils.getToken(userAdmin.username)
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)

        then: "Assert forbidden"
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
        reloadableConfiguration.reset()
    }

    def "Get IDP metadata" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)

        when: "Create IDP using metadata"
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Get IDP's metadata"
        response = cloud20.getIdentityProviderMetadata(userAdminToken, idp.id)
        String metadataXml = response.getEntity(String)

        then:
        response.status == SC_OK
        metadata.replace("\n","") == metadataXml.replace("\n", "")

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
    }

    def "RBAC test for get IDP metadata" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "Create IDP using metadata"
        def userAdminToken = utils.getToken(userAdmin.username)
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Get IDP's metadata using userAdmin token"
        response = cloud20.getIdentityProviderMetadata(userAdminToken, idp.id)
        String metadataXml = response.getEntity(String)

        then:
        response.status == SC_OK
        metadata.replace("\n","") == metadataXml.replace("\n", "")

        when: "Get IDP's metadata using userManage token"
        def userManageToken = utils.getToken(userManage.username)
        response = cloud20.getIdentityProviderMetadata(userManageToken, idp.id)
        metadataXml = response.getEntity(String)

        then:
        response.status == SC_OK
        metadata.replace("\n","") == metadataXml.replace("\n", "")

        when: "Get IDP's metadata using defaulUser with 'identity:identity-provider-manager' role"
        utils.addRoleToUser(defaultUser, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.getIdentityProviderMetadata(defaultUserToken, idp.id)
        metadataXml = response.getEntity(String)
        utils.deleteRoleOnUser(defaultUser, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)

        then:
        response.status == SC_OK
        metadata.replace("\n","") == metadataXml.replace("\n", "")

        when: "Get IDP's metadata using defaultUser with 'identity:identity-provider-read-only' role"
        utils.addRoleToUser(defaultUser, Constants.IDENTITY_PROVIDER_READ_ONLY_ROLE_ID)
        defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.getIdentityProviderMetadata(defaultUserToken, idp.id)
        metadataXml = response.getEntity(String)
        utils.deleteRoleOnUser(defaultUser, Constants.IDENTITY_PROVIDER_READ_ONLY_ROLE_ID)

        then:
        response.status == SC_OK
        metadata.replace("\n","") == metadataXml.replace("\n", "")

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
    }

    def "Get IDP with metadata and Delete IDP using 'rcn:admin' role" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def userAdmin, users, userAdmin2, users2
        (userAdmin, users) = utils.createUserAdmin(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId2)
        // Add domains to same RCN
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = testUtils.getRandomRCN()
            it
        }
        utils.updateDomain(domainId, updateDomainEntity)
        utils.updateDomain(domainId2, updateDomainEntity)

        when: "Create IDP with metadata using userAdmin"
        def userAdminToken = utils.getToken(userAdmin.username)
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Get IDP's metadata using userAdmin2 token"
        def userAdmin2Token = utils.getToken(userAdmin2.username)
        response = cloud20.getIdentityProviderMetadata(userAdmin2Token, idp.id)

        then:
        response.status == SC_FORBIDDEN

        when: "Delete IDP using userAdmin2 token"
        response = cloud20.deleteIdentityProvider(userAdmin2Token, idp.id)

        then:
        response.status == SC_FORBIDDEN

        when: "Get IDP's metadata using userAdmin2 with 'rcn:admin' role"
        utils.addRoleToUser(userAdmin2, Constants.RCN_ADMIN_ROLE_ID)
        userAdmin2Token = utils.getToken(userAdmin2.username)
        response = cloud20.getIdentityProviderMetadata(userAdmin2Token, idp.id)
        String metadataXml = response.getEntity(String)

        then:
        response.status == SC_OK
        metadata.replace("\n","") == metadataXml.replace("\n", "")

        when: "Delete IDP using userAdmin2 token with 'rcn:admin' role"
        response = cloud20.deleteIdentityProvider(userAdmin2Token, idp.id)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId)
    }

    def "Get and Delete IDP using 'rcn:admin' role" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def userAdmin, users, userAdmin2, users2
        (userAdmin, users) = utils.createUserAdmin(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId2)
        // Add domains to same RCN
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = testUtils.getRandomRCN()
            it
        }
        utils.updateDomain(domainId, updateDomainEntity)
        utils.updateDomain(domainId2, updateDomainEntity)

        when: "Create IDP with metadata using userAdmin"
        def userAdminToken = utils.getToken(userAdmin.username)
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Get IDP using userAdmin2 token"
        def userAdmin2Token = utils.getToken(userAdmin2.username)
        response = cloud20.getIdentityProvider(userAdmin2Token, idp.id)

        then:
        response.status == SC_FORBIDDEN

        when: "Delete IDP using userAdmin2 token"
        response = cloud20.deleteIdentityProvider(userAdmin2Token, idp.id)

        then:
        response.status == SC_FORBIDDEN

        when: "Get IDP using userAdmin2 with 'rcn:admin' role"
        utils.addRoleToUser(userAdmin2, Constants.RCN_ADMIN_ROLE_ID)
        userAdmin2Token = utils.getToken(userAdmin2.username)
        response = cloud20.getIdentityProvider(userAdmin2Token, idp.id)
        idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_OK
        idp.authenticationUrl == authenticationUrl
        idp.issuer == issuer
        idp.approvedDomainIds.approvedDomainId.get(0) == domainId

        when: "Delete IDP using userAdmin2 token with 'rcn:admin' role"
        response = cloud20.deleteIdentityProvider(userAdmin2Token, idp.id)

        then:
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
        utils.deleteDomain(domainId)
    }

    def "Error check get and delete IDP" () {
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        def identityAdmin2, userAdmin2, userManage2, defaultUser2
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdmin2, userAdmin2, userManage2, defaultUser2) = utils.createUsers(domainId2)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domain = utils.createDomainEntity()

        when: "Create IDP with multiple approvedDomainIds"
        def userAdminToken = utils.getToken(userAdmin.username)
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId, domain.id].asList())
        def response = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Create IDP using metadata"
        response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider metadataIdp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Create DOMAIN GROUP IDP"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp)
        IdentityProvider domainIdp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Create BROKER IDP"
        IdentityProvider brokerGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.BROKER, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, brokerGroupIdp)
        IdentityProvider brokerIdp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Get invalid IDP id"
        response = cloud20.getIdentityProvider(userAdminToken, "invalid")

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format(DefaultFederatedIdentityService.IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE, "invalid"))

        when: "Delete invalid IDP id"
        response = cloud20.deleteIdentityProvider(userAdminToken, "invalid")

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format(DefaultFederatedIdentityService.IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE, "invalid"))

        when: "User admin try to retrieve a DOMAIN GROUP IDP"
        response = cloud20.getIdentityProvider(userAdminToken, domainIdp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "User admin try to delete a DOMAIN GROUP IDP"
        response = cloud20.deleteIdentityProvider(userAdminToken, domainIdp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "User admin try to retrieve a BROKER IDP"
        response = cloud20.getIdentityProvider(userAdminToken, brokerIdp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "User admin try to delete a BROKER IDP"
        response = cloud20.deleteIdentityProvider(userAdminToken, brokerIdp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "User admin try to retrieve IDP with more than one approvedDomainId"
        response = cloud20.getIdentityProvider(userAdminToken, idp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "User admin try to delete IDP with more than one approvedDomainId"
        response = cloud20.deleteIdentityProvider(userAdminToken, idp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "User admin try to retrieve IDP not part of the approvedDomainIds"
        def userAdmin2Token = utils.getToken(userAdmin2.username)
        response = cloud20.getIdentityProvider(userAdmin2Token, metadataIdp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "User admin try to delete IDP not part of the approvedDomainIds"
        response = cloud20.deleteIdentityProvider(userAdmin2Token, metadataIdp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(defaultUser2, userManage2, userAdmin2, identityAdmin2)
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)
        utils.deleteDomain(domain.id)
        utils.deleteIdentityProvider(idp)
        utils.deleteIdentityProvider(metadataIdp)
        utils.deleteIdentityProvider(domainIdp)
        utils.deleteIdentityProvider(brokerIdp)

    }

    def "Error check get IDP metadata" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        def identityAdmin2, userAdmin2, userManage2, defaultUser2
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdmin2, userAdmin2, userManage2, defaultUser2) = utils.createUsers(domainId2)

        when: "Create IDP using metadata"
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        def userAdminToken = utils.getToken(userAdmin.username)
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Create IDP"
        IdentityProvider identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId].asList())
        response = cloud20.createIdentityProvider(userAdminToken, identityProvider)
        IdentityProvider idp2 = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Get IDP's metadata using defaultUser token"
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.getIdentityProviderMetadata(defaultUserToken, idp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "Get invalid IDP's metadata using defaultUser with 'identity-provider-manager' role"
        utils.addRoleToUser(defaultUser, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.getIdentityProviderMetadata(defaultUserToken, "invalid")

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format(DefaultFederatedIdentityService.IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE, "invalid"))

        when: "Get invalid IDP's metadata using userAdmin"
        defaultUserToken = utils.getToken(userAdmin.username)
        response = cloud20.getIdentityProviderMetadata(defaultUserToken, "invalid")

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format(DefaultFederatedIdentityService.IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE, "invalid"))

        when: "Get IDP's metadata using userAdmin2 token with 'rcn:admin' role, but not part of the RCN"
        utils.addRoleToUser(userAdmin2, Constants.RCN_ADMIN_ROLE_ID)
        def userAdmin2Token = utils.getToken(userAdmin2.username)
        response = cloud20.getIdentityProviderMetadata(userAdmin2Token, idp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "Get IDP with no metadata"
        response = cloud20.getIdentityProviderMetadata(userAdminToken, idp2.id)

        then:
        assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format(DefaultCloud20Service.METADATA_NOT_FOUND_ERROR_MESSAGE, idp2.id))

        when: "Get IDP metadata using 'application/json'"
        response = cloud20.getIdentityProviderMetadata(userAdminToken, idp.id, MediaType.APPLICATION_JSON_TYPE)

        then:
        response.status == SC_NOT_ACCEPTABLE

        when: "Get IDP's metadata using userAdmin token with 'rax_status_restricted' group"
        def raxStatusRestrictedGroup = v2Factory.createGroup(Constants.RAX_STATUS_RESTRICTED_GROUP_NAME).with {
            it.id = Constants.RAX_STATUS_RESTRICTED_GROUP_ID
            it
        }
        utils.addUserToGroup(raxStatusRestrictedGroup, userAdmin)
        userAdminToken = utils.getToken(userAdmin.username)
        response = cloud20.getIdentityProviderMetadata(userAdminToken, idp.id)

        then:
        assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, DefaultCloud20Service.NOT_AUTHORIZED)

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(defaultUser2, userManage2, userAdmin2, identityAdmin2)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
        utils.deleteIdentityProvider(idp2)
    }

    def "test post IDP events to feed feature flag: postEvents = #postEvents"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_POST_IDP_FEED_EVENTS_PROP, postEvents)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        resetCloudFeedsMock()

        when: "create the IDP"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp)

        then: "success"
        response.status == SC_CREATED
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        and: "event was posted if feature is enabled"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(postEvents ? 1 : 0)
        )

        when: "update the IDP"
        resetCloudFeedsMock()
        def newAuthUrl = RandomStringUtils.randomAlphanumeric(10)
        IdentityProvider updateDomainGroupIdp = new IdentityProvider().with {
            it.authenticationUrl = newAuthUrl
            it
        }
        def updateIdpResponse = cloud20.updateIdentityProvider(idpManagerToken, creationResultIdp.id, updateDomainGroupIdp)

        then: "idp updated"
        updateIdpResponse.status == SC_OK

        and: "event was posted if feature is enabled"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(postEvents ? 1 : 0)
        )

        when: "delete the provider"
        resetCloudFeedsMock()
        def deleteIdpResponse = cloud20.deleteIdentityProvider(idpManagerToken, creationResultIdp.id)

        then: "idp deleted"
        deleteIdpResponse.status == SC_NO_CONTENT

        and: "event was posted if feature is enabled"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(postEvents ? 1 : 0)
        )

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUserQuietly(idpManager)
        reloadableConfiguration.reset()

        where:
        postEvents | _
        true       | _
        false      | _
    }

    def "Assert issuer is case sensitive on IDP creation"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def issuer = getRandomUUID('issuer')

        when: "create a IDP"
        def domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID('name'), "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp)

        then: "201 Created"
        response.status == SC_CREATED
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        when: "create a IDP w/ issuer matching existing IDP but all uppercase"
        domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID('name'), "description", issuer.toUpperCase(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp)

        then: "201 Created"
        response.status == SC_CREATED
        IdentityProvider creationResultIdp2 = response.getEntity(IdentityProvider)

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        if (creationResultIdp2) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp2.id)
        }
        utils.deleteUserQuietly(idpManager)
    }

    @Unroll
    def "Error check create IDP with various empty strings - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        resetCloudFeedsMock()

        when: "create IDP with invalid XML"
        def response = cloud20.createIdentityProvider(idpManagerToken, "invalid", MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, "Invalid XML")

        when: "create a DOMAIN IDP with approvedDomainGroup, empty approvedDomainId list"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, Collections.EMPTY_LIST)
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE) //our json reader would send NULL rather than empty array so json would pass (appropriately)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with approvedDomainGroup, empty string approvedDomainId"
        domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, [""])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with empty string approvedDomainGroup, empty string approvedDomainId"
        domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, "", [""])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with empty string approvedDomainGroup and valid approvedDomainId"
        IdentityProvider domainIdIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, "", [domainId])
        response = cloud20.createIdentityProvider(idpManagerToken, domainIdIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a DOMAIN IDP with null approvedDomainGroup, empty string approvedDomainId"
        domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [""])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN)

        when: "create a DOMAIN IDP with null authenticationUrl"
        domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.authenticationUrl = null
            it
        }
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "create a RACKER IDP with null authenticationUrl"
        def rackidp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, null).with {
            it.authenticationUrl = null
            it
        }
        response = cloud20.createIdentityProvider(idpManagerToken, rackidp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        and: "no feed event was posted"
        def idpEntity = federatedIdentityService.getIdentityProviderByIssuer(domainGroupIdp.issuer)
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

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
        resetCloudFeedsMock()

        when: "create a IDP with approvedDomainGroup, null list"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "create a IDP with null approvedDomainGroup, entry in list"
        domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, [domainId])
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "bad request"
        response.status == SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        and: "no feed event was posted"
        def idpEntity = federatedIdentityService.getIdentityProviderByIssuer(domainGroupIdp.issuer)
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

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
        def domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
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
    def "Try to update IDP without specifying authenticationUrl or name - request: #requestContentType"() {
        given:
        def idpManagerToken = utils.getServiceAdminToken()
        def idpName = getRandomUUID("name")
        def domainGroupIdp = v2Factory.createIdentityProvider(idpName, "", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        def createdIdp = response.getEntity(IdentityProvider)
        def updateIdp = new IdentityProvider()

        when: "create a DOMAIN IDP with approvedDomainGroup, empty string description"
        def updateIdpResponse = cloud20.updateIdentityProvider(idpManagerToken, createdIdp.id, updateIdp)
        def updateIdpEntity = updateIdpResponse.getEntity(IdentityProvider)

        then: "successful"
        updateIdpResponse.status == SC_OK
        updateIdpEntity.name == idpName
        updateIdpEntity.authenticationUrl == "http://random.url"

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Error if try to update IDP with any value other than authenticationUrl or name - property: #prop "() {
        given:
        def idpManagerToken = utils.getServiceAdminToken()

        when: "create a DOMAIN IDP with approvedDomainGroup, empty string description"
        def response = cloud20.createIdentityProvider(idpManagerToken, idp)

        then: "successful"
        response.status == SC_BAD_REQUEST

        where:
        prop                  | idp
        "id"                  | new IdentityProvider().with {it.id = "as"; it}
        "issuer"              | new IdentityProvider().with {it.issuer = "as"; it}
        "federationType"      | new IdentityProvider().with {it.federationType = IdentityProviderFederationTypeEnum.DOMAIN; it}
        "description"         | new IdentityProvider().with {it.description = "as"; it}
        "publicCertificates"  | new IdentityProvider().with {it.publicCertificates = new PublicCertificates(); it}
        "approvedDomainGroup" | new IdentityProvider().with {it.approvedDomainGroup = "as"; it}
        "approvedDomainIds"   | new IdentityProvider().with {it.approvedDomainIds = new ApprovedDomainIds(); it}
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
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
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
        creationResultIdp.name != null
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
        getResultIdp.name != null

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
        assertOpenStackV2FaultResponse(deleteIdpResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND,
                String.format(DefaultFederatedIdentityService.IDENTITY_PROVIDER_NOT_FOUND_ERROR_MESSAGE, "non-existant-idp-provider"))

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
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
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
        def idp1Name = getRandomUUID("idp1")
        IdentityProvider idp1ToCreate = v2Factory.createIdentityProvider(idp1Name, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp1 = utils.createIdentityProvider(idpManagerToken, idp1ToCreate)

        //create a tenant and add it to domain 1
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)

        def domainId2 = UUID.randomUUID().toString()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId2, domainId2))

        def idp2Name = getRandomUUID("idp2")
        IdentityProvider idp2ToCreate = v2Factory.createIdentityProvider(idp2Name, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId2]).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp2 = utils.createIdentityProvider(idpManagerToken, idp2ToCreate)

        def idp3Name = getRandomUUID("idp3")
        IdentityProvider idp3ToCreate = v2Factory.createIdentityProvider(idp3Name, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp3 = utils.createIdentityProvider(idpManagerToken, idp3ToCreate)

        def idp4Name = getRandomUUID("idp4")
        IdentityProvider idp4ToCreate = v2Factory.createIdentityProvider(idp4Name, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, null, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        IdentityProvider idp4 = utils.createIdentityProvider(idpManagerToken, idp4ToCreate)

        when: "get all idp"
        def allIdp = cloud20.listIdentityProviders(idpManagerToken, null, null, null, null, null, acceptContentType)

        then:
        allIdp.status == SC_OK
        IdentityProviders providers = allIdp.getEntity(IdentityProviders.class)
        providers != null
        providers.identityProvider.size() >= 4
        providers.identityProvider.find{it.id == idp1.id} != null
        providers.identityProvider.find{it.name == idp1Name} != null
        providers.identityProvider.find{it.id == idp2.id} != null
        providers.identityProvider.find{it.name == idp2Name} != null
        providers.identityProvider.find{it.id == idp3.id} != null
        providers.identityProvider.find{it.name == idp3Name} != null
        providers.identityProvider.find{it.id == idp4.id} != null
        providers.identityProvider.find{it.name == idp4Name} != null

        when: "get all idps for specific domain"
        def domainSpecificIdpResponse = cloud20.listIdentityProviders(idpManagerToken, null, null, domainId, null, null, acceptContentType)

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
        def onlyExplicitResponse = cloud20.listIdentityProviders(idpManagerToken, null, null, null, IdentityProviderTypeFilterEnum.EXPLICIT.name(), null, acceptContentType)

        then: "all idps with an EXPLICIT domain mapping are returned"
        onlyExplicitResponse.status == SC_OK
        def onlyExplicitIdps = onlyExplicitResponse.getEntity(IdentityProviders.class)
        onlyExplicitIdps != null
        onlyExplicitIdps.identityProvider.find{it.id == idp1.id} != null
        onlyExplicitIdps.identityProvider.find{it.id == idp2.id} != null
        onlyExplicitIdps.identityProvider.find{it.id == idp3.id} == null
        onlyExplicitIdps.identityProvider.find{it.id == idp4.id} == null

        when: "get all idps that have an EXPLICIT domain mapping and for a specific domain"
        def domainAndExplicitResponse = cloud20.listIdentityProviders(idpManagerToken, null, null, domainId, IdentityProviderTypeFilterEnum.EXPLICIT.name(), null, acceptContentType)

        then: "all idps with an EXPLICIT domain mapping are returned"
        domainAndExplicitResponse.status == SC_OK
        def domainAndExplicitIdps = domainAndExplicitResponse.getEntity(IdentityProviders.class)
        domainAndExplicitIdps != null
        domainAndExplicitIdps.identityProvider.find{it.id == idp1.id} != null
        domainAndExplicitIdps.identityProvider.find{it.id == idp2.id} == null
        domainAndExplicitIdps.identityProvider.find{it.id == idp3.id} == null
        domainAndExplicitIdps.identityProvider.find{it.id == idp4.id} == null

        when: "get all idps that have a DOMAIN mapping and for a specific TENANT"
        def tenantResponse = cloud20.listIdentityProviders(idpManagerToken, null, null, null, null, tenant.id, acceptContentType)

        then: "all idps with a domain mapping for the tenant are returned"
        tenantResponse.status == SC_OK
        def tenantIdps = tenantResponse.getEntity(IdentityProviders.class)
        tenantIdps != null
        tenantIdps.identityProvider.find{it.id == idp1.id} != null
        tenantIdps.identityProvider.find{it.id == idp2.id} == null
        tenantIdps.identityProvider.find{it.id == idp3.id} != null
        tenantIdps.identityProvider.find{it.id == idp4.id} == null

        when: "get all idps that have an EXPLICIT DOMAIN mapping and for a specific TENANT"
        def tenantAndExplicitResponse = cloud20.listIdentityProviders(idpManagerToken, null, null, null, IdentityProviderTypeFilterEnum.EXPLICIT.name(), tenant.id, acceptContentType)

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
        acceptContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "list IDPs by type filter returns 400 for any value of 'idpType' other than EXPLICIT"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, null, "invalidTypeFilter")

        then:
        response.status == 400
    }

    def "list IDPs returns 400 is tenant and domain IDs are given as filters"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, "someDomain", null, "someTenant")

        then:
        response.status == 400
    }

    @Unroll
    def "list IDPs returns empty list if the tenant being filtered by does not exist, accept = #accept"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when:
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, null, null, "someTenant", accept)

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
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, "domainDoesNotExist", null, null, accept)

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
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, null, null, tenant.id)

        then:
        response.status == 400
    }

    @Unroll
    def "list IDPs with parameters - name = #name, includeDomain = #includeDomain, idpType = #idpType, includeTenant = #includeTenant, accept = #accept"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domain = utils.createDomainEntity(UUID.randomUUID().toString())
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domain.id, tenant.id)
        IdentityProvider idpToCreate = v2Factory.createIdentityProvider(name, "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id]).with {
            it.publicCertificates = publicCertificates
            it
        }
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, idpToCreate)

        when:
        def response
        if (includeDomain && includeTenant) {
            response = cloud20.listIdentityProviders(idpManagerToken, name, null, domain.id, idpType, tenant.id, accept)
        } else if (includeDomain) {
            response = cloud20.listIdentityProviders(idpManagerToken, name, null, domain.id, idpType, null, accept)
        } else if (includeTenant) {
            response = cloud20.listIdentityProviders(idpManagerToken, name, null, null, idpType, tenant.id, accept)
        } else {
            response = cloud20.listIdentityProviders(idpManagerToken, name, null, null, idpType, null, accept)
        }
        IdentityProviders providers = response.getEntity(IdentityProviders.class)

        then:
        response.status == SC_OK
        providers.identityProvider.size() == 1

        cleanup:
        try {
            utils.deleteDomain(domain.id)
            utils.deleteTenant(tenant.id)
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
            utils.deleteUserQuietly(idpManager)
        } catch (Exception ex) {
            // Eat. We're just cleaning up
        }
        
        where:
        name                 | includeDomain | idpType                                        | includeTenant | accept
        getRandomUUID("idp") | false         | null                                           | false         | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp") | false         | null                                           | false         | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("idp") | true          | null                                           | false         | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp") | true          | null                                           | false         | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("idp") | false         | null                                           | true          | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp") | false         | null                                           | true          | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("idp") | false         | IdentityProviderTypeFilterEnum.EXPLICIT.name() | false         | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp") | false         | IdentityProviderTypeFilterEnum.EXPLICIT.name() | false         | MediaType.APPLICATION_JSON_TYPE
    }

    def "Check for list IDP"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def users = utils.createUsers(domainId)
        def users2 = utils.createUsers(domainId2)

        def rcn = testUtils.getRandomRCN()
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = rcn
            it
        }
        utils.updateDomain(domainId, updateDomainEntity)
        utils.updateDomain(domainId2, updateDomainEntity)

        def dummyDomain = utils.createDomainEntity()

        def userAdmin = users.find { it.username =~ "userAdmin*" }
        def userAdminToken = utils.getToken(userAdmin.username)
        def userAdmin2 = users2.find { it.username =~ "userAdmin*" }
        def userAdmin2Token = utils.getToken(userAdmin2.username)

        when: "Create IDP using metadata"
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def response = cloud20.createIdentityProviderWithMetadata(userAdmin2Token, metadata)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Create IDP with multiple approvedDomainIds"
        IdentityProvider idpToCreate = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [dummyDomain.id, domainId].asList())
        response = cloud20.createIdentityProvider(idpManagerToken, idpToCreate)
        def idpMultipleApprovedDomainIds = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        idpMultipleApprovedDomainIds.approvedDomainIds.approvedDomainId.size() == 2

        when: "Create IDP with multiple approvedDomainIds"
        idpToCreate = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [dummyDomain.id].asList())
        response = cloud20.createIdentityProvider(idpManagerToken, idpToCreate)
        def idpNotInUsersDomain = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        idpNotInUsersDomain.approvedDomainIds.approvedDomainId.size() == 1

        when: "List IDPs using userAdmin"
        response = cloud20.listIdentityProviders(userAdminToken)
        def listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 0

        when: "List IDPs with 'rcn:admin' user"
        def defaultUser = users.find { it.username =~ "defaultUser*" }
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.listIdentityProviders(defaultUserToken)
        listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 1

        when: "List IDPs with query params"
        response = cloud20.listIdentityProviders(userAdmin2Token, idp.name, idp.issuer, domainId2)
        listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 1

        when: "List IDPs with invalid query params"
        response = cloud20.listIdentityProviders(userAdmin2Token, "invalid")
        listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 0

        cleanup:
        utils.deleteUsers(users.reverse())
        utils.deleteUsers(users2.reverse())
        utils.deleteDomain(domainId)
        utils.deleteDomain(domainId2)
        utils.deleteDomain(dummyDomain.id)
        utils.deleteIdentityProvider(idp)
        utils.deleteIdentityProvider(idpNotInUsersDomain)
        utils.deleteIdentityProvider(idpMultipleApprovedDomainIds)
    }

    @Unroll
    def "Valid list IDPs with query parameters - name = #name, issuer = #issuer, domainId = #domainId, idpType = #idpType, tenantName = #tenantName, accept = #accept"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        // Auto generate IDP attributes if null
        def domain
        if (domainId == null) {
            domain = utils.createDomainEntity()
        } else {
            domain = utils.createDomainEntity(domainId)
        }

        def tenant
        if (tenantName == null) {
            tenant = utils.createTenant()
        } else {
            tenant = utils.createTenant(tenantName)
        }
        utils.addTenantToDomain(domain.id, tenant.id)

        IdentityProvider idpToCreate
        if (name == null && issuer == null) {
            idpToCreate = v2Factory.createIdentityProvider(getRandomUUID("name"), "description", getRandomUUID("issuer"), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        } else if (name == null && issuer != null) {
            idpToCreate = v2Factory.createIdentityProvider(getRandomUUID("name"), "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        } else if (name != null && issuer == null) {
            idpToCreate = v2Factory.createIdentityProvider(name, "description", getRandomUUID("issuer"), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        } else {
            idpToCreate = v2Factory.createIdentityProvider(name, "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        }
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, idpToCreate)

        when: "get Idp from query parameters"
        def response = cloud20.listIdentityProviders(idpManagerToken, name, issuer, domainId, idpType, tenantName, accept)
        IdentityProviders providers = response.getEntity(IdentityProviders.class)

        then: "Assert only one IDP is returned"
        response.status == SC_OK
        providers.identityProvider.size() == 1

        cleanup:
        utils.deleteDomain(domain.id)
        utils.deleteTenant(tenant.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUserQuietly(idpManager)

        where:
        name                  | issuer                  | domainId                | idpType                                        | tenantName              | accept
        getRandomUUID("name") | null                    | null                    | null                                           | null                    | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("name") | null                    | null                    | null                                           | null                    | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("name") | getRandomUUID("issuer") | null                    | null                                           | null                    | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("name") | getRandomUUID("issuer") | null                    | null                                           | null                    | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("name") | getRandomUUID("issuer") | getRandomUUID("domain") | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("name") | getRandomUUID("issuer") | getRandomUUID("domain") | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("name") | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | getRandomUUID("tenant") | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("name") | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | getRandomUUID("tenant") | MediaType.APPLICATION_JSON_TYPE

        null                  | getRandomUUID("issuer") | null                    | null                                           | null                    | MediaType.APPLICATION_XML_TYPE
        null                  | getRandomUUID("issuer") | null                    | null                                           | null                    | MediaType.APPLICATION_JSON_TYPE

        null                  | getRandomUUID("issuer") | getRandomUUID("domain") | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_XML_TYPE
        null                  | getRandomUUID("issuer") | getRandomUUID("domain") | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_JSON_TYPE

        null                  | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | getRandomUUID("tenant") | MediaType.APPLICATION_XML_TYPE
        null                  | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | getRandomUUID("tenant") | MediaType.APPLICATION_JSON_TYPE
    }

    def "Test list of IDPs created by metadata" () {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        def identityAdmin2, userAdmin2, userManage2, defaultUser2
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        (identityAdmin2, userAdmin2, userManage2, defaultUser2) = utils.createUsers(domainId2)
        // Add domains to same RCN
        def rcn = testUtils.getRandomRCN()
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = rcn
            it
        }
        utils.updateDomain(domainId, updateDomainEntity)
        utils.updateDomain(domainId2, updateDomainEntity)

        when: "Create IDP using metadata"
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def userAdminToken = utils.getToken(userAdmin.username)
        def response = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata)
        IdentityProvider idpWithMetadata = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "Create IDP within same RCN"
        issuer = testUtils.getRandomUUID("issuer")
        def userAdmin2Token = utils.getToken(userAdmin2.username)
        metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        response = cloud20.createIdentityProviderWithMetadata(userAdmin2Token, metadata)
        IdentityProvider idpWithSameRcn = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "List IDPs with userAdmin token"
        response = cloud20.listIdentityProviders(userAdminToken)
        IdentityProviders identityProviders = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        identityProviders.identityProvider.size() == 1
        identityProviders.identityProvider.get(0).id == idpWithMetadata.id

        when: "List IDPs with userAdmin token"
        def userManageToken = utils.getToken(userManage.username)
        response = cloud20.listIdentityProviders(userManageToken)
        identityProviders = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        identityProviders.identityProvider.size() == 1
        identityProviders.identityProvider.get(0).id == idpWithMetadata.id

        when: "List IDPs with user that has the rcn:admin role"
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.listIdentityProviders(defaultUserToken)
        identityProviders = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        identityProviders.identityProvider.size() == 2
        identityProviders.identityProvider.find {it.id == idpWithMetadata.id} != null
        identityProviders.identityProvider.find {it.id == idpWithSameRcn.id} != null

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUsers(defaultUser2, userManage2, userAdmin2, identityAdmin2)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idpWithMetadata)
        utils.deleteIdentityProvider(idpWithSameRcn)
    }

    @Unroll
    def "Invalid list IDPs with query parameters - name = #name, issuer = #issuer, domainId = #domainId, idpType = #idpType, tenantName = #tenantName, accept = #accept"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        // Auto generate IDP attributes if null
        def domain
        if (domainId == null || domainId == "invalid") {
            domain = utils.createDomainEntity()
        } else {
            domain = utils.createDomainEntity(domainId)
        }

        def tenant
        if (tenantName == null || tenantName == "invalid") {
            tenant = utils.createTenant()
        } else {
            tenant = utils.createTenant(tenantName)
        }
        utils.addTenantToDomain(domain.id, tenant.id)

        IdentityProvider idpToCreate
        if (name == "invalid" && issuer == "invalid") {
            idpToCreate = v2Factory.createIdentityProvider(getRandomUUID("name"), "description", getRandomUUID("issuer"), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        } else if (name == "invalid" && issuer != "invalid") {
            idpToCreate = v2Factory.createIdentityProvider(getRandomUUID("name"), "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        } else if (name != "invalid" && issuer == "invalid") {
            idpToCreate = v2Factory.createIdentityProvider(name, "description", getRandomUUID("issuer"), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        } else {
            idpToCreate = v2Factory.createIdentityProvider(name, "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id])
        }
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, idpToCreate)

        when: "get Idp from query parameters"
        def response = cloud20.listIdentityProviders(idpManagerToken, name, issuer, domainId, idpType, tenantName, accept)
        IdentityProviders providers = response.getEntity(IdentityProviders.class)

        then: "Assert no IDP is returned"
        response.status == SC_OK
        providers.identityProvider.size() == 0

        cleanup:
        utils.deleteDomain(domain.id)
        utils.deleteTenant(tenant.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUserQuietly(idpManager)

        where:
        name                  | issuer                  | domainId                | idpType                                        | tenantName              | accept
        getRandomUUID("name") | getRandomUUID("issuer") | "invalid"               | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("name") | getRandomUUID("issuer") | "invalid"               | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("name") | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | "invalid"               | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("name") | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | "invalid"               | MediaType.APPLICATION_JSON_TYPE

        "invalid"             | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_XML_TYPE
        "invalid"             | getRandomUUID("issuer") | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_JSON_TYPE

        getRandomUUID("name") | "invalid"               | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("name") | "invalid"               | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_JSON_TYPE

        "invalid"             | "invalid"               | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_XML_TYPE
        "invalid"             | "invalid"               | null                    | IdentityProviderTypeFilterEnum.EXPLICIT.name() | null                    | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "list IDPs with parameters invalid cases - invalidName = #invalidName, invalidDomain = #invalidDomain, invalidTenant = #invalidTenant , accept = #accept"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domain = utils.createDomainEntity(UUID.randomUUID().toString())
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domain.id, tenant.id)
        def idpName = getRandomUUID("idp")
        IdentityProvider idpToCreate = v2Factory.createIdentityProvider(idpName, "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id]).with {
            it.publicCertificates = publicCertificates
            it
        }
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, idpToCreate)

        when:
        def response
        if (invalidDomain) {
            response = cloud20.listIdentityProviders(idpManagerToken, idpName, null, "invalid", null, null, accept)
        } else if (invalidTenant) {
            response = cloud20.listIdentityProviders(idpManagerToken, idpName, null, null, null, "invalid", accept)
        } else if (invalidName) {
            response = cloud20.listIdentityProviders(idpManagerToken, "invalid", null, null, null, null, accept)
        }
        IdentityProviders providers = response.getEntity(IdentityProviders.class)

        then:
        response.status == SC_OK
        providers.identityProvider.size() == 0

        cleanup:
        utils.deleteDomain(domain.id)
        utils.deleteTenant(tenant.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUserQuietly(idpManager)

        where:
        invalidName | invalidDomain | invalidTenant | accept
        true        | false         | false         | MediaType.APPLICATION_XML_TYPE
        true        | false         | false         | MediaType.APPLICATION_JSON_TYPE

        false       | false         | true          | MediaType.APPLICATION_XML_TYPE
        false       | false         | true          | MediaType.APPLICATION_JSON_TYPE

        false       | true          | false         | MediaType.APPLICATION_XML_TYPE
        false       | true          | false         | MediaType.APPLICATION_JSON_TYPE
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
        def response = cloud20.listIdentityProviders(idpManagerToken, null, null, null, null, tenant.id)

        then:
        response.status == 400
    }

    def "list IDPs with 'issuer' query param"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idpName = getRandomUUID('name')
        def idpIssuer = getRandomUUID('issuer')
        IdentityProvider identityProvider = v2Factory.createIdentityProvider(idpName, "blah", idpIssuer, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def createResponse = cloud20.createIdentityProvider(idpManagerToken, identityProvider)
        def creationResultIdp = createResponse.getEntity(IdentityProvider)

        when: "only issuer"
        def response = cloud20.listIdentityProviders(idpManagerToken, null, idpIssuer, null, null, null)
        def listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 1

        when: "issuer and name query param"
        response = cloud20.listIdentityProviders(idpManagerToken, idpName, idpIssuer, null, null, null)
        listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 1

        when: "invalid issuer"
        response = cloud20.listIdentityProviders(idpManagerToken, null, idpIssuer.toUpperCase(), null, null, null)
        listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 0

        when: "invalid issuer and valid name query param"
        response = cloud20.listIdentityProviders(idpManagerToken, idpName, idpIssuer.toUpperCase(), null, null, null)
        listIdps = response.getEntity(IdentityProviders)

        then:
        response.status == SC_OK
        listIdps.identityProvider.size() == 0

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUserQuietly(idpManager)
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
        IdentityProvider validIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(utils.getIdentityAdminToken(), validIdp)

        then: "403"
        response.status == SC_FORBIDDEN

        when: "Domain IDP without either approvedDomains or approvedDomainGroup"
        IdentityProvider invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "Domain IDP with both approvedDomains and approvedDomainGroup"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, [domainId])
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "Racker IDP with approvedDomainGroup"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.RACKER, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        when: "Domain IDP with invalid approvedDomainGroup"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, "Invalid", null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_GROUP)

        when: "Domain IDP with invalid approvedDomains"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, ["non-existent-domain"])
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN)

        when: "IDP with issuer already exists"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", Constants.DEFAULT_IDP_URI, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "409"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_CONFLICT, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS)

        when: "IDP with issuer length exceeded 255"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", RandomStringUtils.randomAlphabetic(256), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)

        when: "IDP with missing issuer"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah", null, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "IDP with missing fed type"
        invalid = v2Factory.createIdentityProvider(getRandomUUID(), "blah",  getRandomUUID(), null, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "IDP with name length exceeded 255"
        invalid = v2Factory.createIdentityProvider(RandomStringUtils.randomAlphabetic(256), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)

        when: "IDP with name having special characters"
        invalid = v2Factory.createIdentityProvider(getRandomUUID("@"), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE)

        when: "IDP with no name"
        invalid = v2Factory.createIdentityProvider(null, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)

        when: "IDP with empty name"
        invalid = v2Factory.createIdentityProvider("", "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE)
    }

    def "Update Identity Provider returns errors appropriately"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domain = utils.createDomainEntity(UUID.randomUUID().toString())
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domain.id, tenant.id)
        def idpName = getRandomUUID("idp")
        def idpAuthenticationUrl = getRandomUUID()
        IdentityProvider idpToCreate = v2Factory.createIdentityProvider(idpName, "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id]).with {
            it.publicCertificates = publicCertificates
            it
        }
        def idp = utils.createIdentityProvider(idpManagerToken, idpToCreate)

        when: "IDP with name already exist"
        def existingIdpName = getRandomUUID("existingIdp")
        IdentityProvider existingIdp = v2Factory.createIdentityProvider(existingIdpName, "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id]).with {
            it.publicCertificates = publicCertificates
            it
        }
        def existingIdpEntity  = utils.createIdentityProvider(idpManagerToken, existingIdp)
        IdentityProvider invalid = new IdentityProvider().with {
            it.name = existingIdpName
            it
        }
        def response = cloud20.updateIdentityProvider(idpManagerToken, idp.id, invalid)

        then: "400"
        def errorMsg = String.format(Validator20.DUPLICATE_IDENTITY_PROVIDER_NAME_ERROR_MSG, existingIdpName)
        assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, String.format("Error code: '%s'; %s", ErrorCodes.ERROR_CODE_IDP_NAME_ALREADY_EXISTS, errorMsg))

        when: "Cannot unset name for IDP"
        invalid.name = ""
        response = cloud20.updateIdentityProvider(idpManagerToken, idp.id, invalid)
        invalid.name = idpName

        then: "400"
        assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, String.format(Validator20.EMPTY_ATTR_MESSAGE, "name"))

        when: "Cannot unset authenticationUrl for IDP"
        invalid.authenticationUrl = ""
        response = cloud20.updateIdentityProvider(idpManagerToken, idp.id, invalid)
        invalid.authenticationUrl = idpAuthenticationUrl

        then: "400"
        assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, String.format(Validator20.EMPTY_ATTR_MESSAGE, "authenticationUrl"))

        when: "IDP with name length exceeded 255"
        invalid.name = RandomStringUtils.randomAlphabetic(256)
        response = cloud20.updateIdentityProvider(idpManagerToken, idp.id, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED)

        when: "IDP with name having special characters"
        invalid.name = getRandomUUID("@")
        response = cloud20.updateIdentityProvider(idpManagerToken, idp.id, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE)

        when: "IDP with empty approvedDomainIds"
        ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
        invalid.setApprovedDomainIds(approvedDomainIds)
        invalid.name = null
        response = cloud20.updateIdentityProvider(idpManagerToken, idp.id, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_EMPTY_APPROVED_DOMAIN)

        when: "IDP with invalid approvedDomainIds"
        approvedDomainIds.approvedDomainId.add("invalid")
        invalid.setApprovedDomainIds(approvedDomainIds)
        response = cloud20.updateIdentityProvider(idpManagerToken, idp.id, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN)

        when: "Update IDP with approvedDomainIds when approvedDomainGroup is already set"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        def domainGroupIdpEntity  = utils.createIdentityProvider(idpManagerToken, domainGroupIdp)
        approvedDomainIds.approvedDomainId.add(domain.id)
        invalid.setApprovedDomainIds(approvedDomainIds)
        response = cloud20.updateIdentityProvider(idpManagerToken, domainGroupIdpEntity.id, invalid)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_EXISTING_APPROVED_DOMAIN_GROUP)

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, idp.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, existingIdpEntity.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, domainGroupIdpEntity.id)
        utils.deleteUserQuietly(idpManager)
    }

    def "Update Identity provider with duplicate approved domain Ids" () {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domain = utils.createDomainEntity()
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id].asList()).with {
            it.publicCertificates = publicCertificates
            it
        }
        def approvedDomainsIdpEntity  = utils.createIdentityProvider(idpManagerToken, approvedDomainsIdp)

        when: "Updating Idp with duplicate approved domain Ids"
        IdentityProvider duplicateApprovedDomainIds = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.approvedDomainId.addAll([domain.id, domain.id].asList())
            it.approvedDomainIds = approvedDomainIds
            it
        }
        def response = cloud20.updateIdentityProvider(idpManagerToken, approvedDomainsIdpEntity.id, duplicateApprovedDomainIds)

        then:
        response.status == SC_OK
        IdentityProvider updateResultIdp = response.getEntity(IdentityProvider)
        updateResultIdp.approvedDomainIds.approvedDomainId.size() == 1
        updateResultIdp.approvedDomainIds.approvedDomainId == [domain.id].asList()

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, approvedDomainsIdpEntity.id)
        utils.deleteUserQuietly(idpManager)
        utils.deleteDomain(domain.id)
    }

    def "Attempt to update Identity provider's approved domain Ids with feature flag feature.allow.updating.approved.domain.ids.for.idp = false" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ALLOW_UPDATING_APPROVED_DOMAIN_IDS_FOR_IDP_PROP, false)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domain = utils.createDomainEntity()
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id].asList()).with {
            it.publicCertificates = publicCertificates
            it
        }
        def approvedDomainsIdpEntity  = utils.createIdentityProvider(idpManagerToken, approvedDomainsIdp)

        when: "Updating Idp's approvedDomainIds"
        IdentityProvider duplicateApprovedDomainIds = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.approvedDomainId.addAll([domain.id].asList())
            it.approvedDomainIds = approvedDomainIds
            it
        }
        def response = cloud20.updateIdentityProvider(idpManagerToken, approvedDomainsIdpEntity.id, duplicateApprovedDomainIds)

        then:
        response.status == SC_BAD_REQUEST

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, approvedDomainsIdpEntity.id)
        utils.deleteUserQuietly(idpManager)
        utils.deleteDomain(domain.id)
        reloadableConfiguration.reset()
    }

    def "Updating Identity provider approved domain Ids"() {
        given: "A new identity provider"
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        // Create new admin users for domain
        def domain = utils.createDomainEntity()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domain.id)
        def domain2 = utils.createDomainEntity()
        def userAdmin2, users2
        (userAdmin2, users2) = utils.createUserAdmin(domain2.id)

        def issuer = getRandomUUID("issuer")
        def keyPair1 = SamlCredentialUtils.generateKeyPair()
        def cert1 = SamlCredentialUtils.generateCertificate(keyPair1)
        def pubCertPemString1 = SamlCredentialUtils.getCertificateAsPEMString(cert1)
        def pubCerts1 = v2Factory.createPublicCertificate(pubCertPemString1)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)
        def samlProducerForSharedIdp = new SamlProducer(SamlCredentialUtils.generateX509Credential(cert1, keyPair1))

        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, null, [domain.id, domain2.id].asList()).with {
            it.publicCertificates = publicCertificates
            it
        }
        def approvedDomainsIdpEntity  = utils.createIdentityProvider(idpManagerToken, approvedDomainsIdp)

        when: "Create new federated users"
        def username = testUtils.getRandomUUID("fedUser")
        def username2 = testUtils.getRandomUUID("fedUser2")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(issuer, username, expSecs, domain.id, null, email, samlProducerForSharedIdp);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(issuer, username2, expSecs, domain2.id, null, email, samlProducerForSharedIdp);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)

        then: "Verify successful authentication"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        samlResponse2.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value

        when: "Validating tokens"
        def validateResponse = cloud20.validateToken(utils.identityAdminToken, authResponse.token.id)
        def validate2Response = cloud20.validateToken(utils.identityAdminToken, authResponse2.token.id)

        then: "Assert successful validation"
        validateResponse.status == SC_OK
        validate2Response.status == SC_OK

        when: "Retrieve users from backend"
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)
        FederatedUser fedUser2 = federatedUserRepository.getUserById(authResponse2.user.id)


        then: "Verify users exist"
        fedUser.id == authResponse.user.id
        fedUser.username == username
        fedUser2.id == authResponse2.user.id
        fedUser2.username == username2

        when: "Updating approved domains on IDP"
        IdentityProvider updateIdentityProvider = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.approvedDomainId.addAll([domain2.id].asList())
            it.approvedDomainIds = approvedDomainIds
            it
        }
        def updateResponse = cloud20.updateIdentityProvider(idpManagerToken, approvedDomainsIdpEntity.id, updateIdentityProvider)

        then: "Assert update IDP response"
        updateResponse.status == SC_OK

        when: "Retrieve users and validate tokens"
        fedUser = federatedUserRepository.getUserById(authResponse.user.id)
        fedUser2 = federatedUserRepository.getUserById(authResponse2.user.id)
        validateResponse = cloud20.validateToken(utils.identityAdminToken, authResponse.token.id)
        validate2Response = cloud20.validateToken(utils.identityAdminToken, authResponse2.token.id)

        then: "Assert users and token status"
        fedUser == null
        validateResponse.status == SC_NOT_FOUND
        fedUser2 != null
        validate2Response.status == SC_OK

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, approvedDomainsIdpEntity.id)
        utils.deleteUserQuietly(idpManager)
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
        utils.deleteDomain(domain.id)
        utils.deleteDomain(domain2.id)
    }

    @Unroll
    def "Create Identity provider with name #name - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "Create IDP"
        IdentityProvider identityProvider = v2Factory.createIdentityProvider(name, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, identityProvider)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "Assert created idp"
        response.status == org.springframework.http.HttpStatus.CREATED.value()
        creationResultIdp.name == name

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)

        where:
        name                            | content                         | accept
        getRandomUUID("idp")            | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp")            | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        getRandomUUID("idp.other-name") | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp.other-name") | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Update Identity provider with name #name - content-type = #content, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        IdentityProvider identityProvider = v2Factory.createIdentityProvider(getRandomUUID("idp"), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, identityProvider)
        IdentityProvider createdIdp = response.getEntity(IdentityProvider)

        when: "Update IDP"
        IdentityProvider idpForUpdate = new IdentityProvider().with {
            it.name = name
            it
        }
        response = cloud20.updateIdentityProvider(idpManagerToken, createdIdp.id, idpForUpdate)
        IdentityProvider updatedIdp = response.getEntity(IdentityProvider)

        then: "Assert updated idp"
        response.status == org.springframework.http.HttpStatus.OK.value()
        updatedIdp.name == name

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, createdIdp.id)
        utils.deleteUserQuietly(idpManager)

        where:
        name                            | content                         | accept
        getRandomUUID("idp")            | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp")            | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        getRandomUUID("idp.other-name") | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        getRandomUUID("idp.other-name") | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def "Identity provider name must be unique"() {
        given:
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def idpName = getRandomUUID("idp")

        when: "Create IDP"
        IdentityProvider identityProvider = v2Factory.createIdentityProvider(idpName, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, identityProvider)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "Assert created idp"
        response.status == org.springframework.http.HttpStatus.CREATED.value()
        creationResultIdp.name == idpName

        when: "Create IDP with existing name"
        IdentityProvider invalid = v2Factory.createIdentityProvider(idpName, "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "Assert conflict"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_CONFLICT, ErrorCodes.ERROR_CODE_IDP_NAME_ALREADY_EXISTS)

        when: "Create IDP with existing name - upper case"
        invalid = v2Factory.createIdentityProvider(idpName.toUpperCase(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, invalid)

        then: "Assert conflict"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_CONFLICT, ErrorCodes.ERROR_CODE_IDP_NAME_ALREADY_EXISTS)

        cleanup:
        if (creationResultIdp) {
            utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        }
        utils.deleteUserQuietly(idpManager)
    }

    def "Create domain with dup domainIds ignores dups"() {
        given:
        def domainId = utils.createDomain()
        cloud20.addDomain(utils.getServiceAdminToken(), v2Factory.createDomain(domainId, domainId))
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "create a DOMAIN IDP with single certs and approvedDomains"
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId, domainId])
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
        IdentityProvider idp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId]).with {
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
        def idpRequest = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN)

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

    @Unroll
    def "CRUD a BROKER IDP - request: #requestContentType"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "Create IDP can set federationType of 'BROKER'"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.BROKER, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED
        creationResultIdp.approvedDomainGroup == ApprovedDomainGroupEnum.GLOBAL.storedVal
        creationResultIdp.approvedDomainIds == null
        creationResultIdp.description == domainGroupIdp.description
        creationResultIdp.issuer == domainGroupIdp.issuer
        creationResultIdp.authenticationUrl == domainGroupIdp.authenticationUrl
        creationResultIdp.publicCertificates == null
        creationResultIdp.id != null
        creationResultIdp.name != null
        creationResultIdp.federationType == IdentityProviderFederationTypeEnum.BROKER
        response.headers.getFirst("Location") != null
        response.headers.getFirst("Location").contains(creationResultIdp.id)

        when: "GET/LIST IDPs must return BROKER as appropriate"
        def getIdpResponse = cloud20.getIdentityProvider(idpManagerToken, creationResultIdp.id, requestContentType, requestContentType)
        IdentityProvider getResultIdp = getIdpResponse.getEntity(IdentityProvider)

        then: "contains appropriate info"
        getIdpResponse.status == SC_OK
        creationResultIdp.approvedDomainGroup == ApprovedDomainGroupEnum.GLOBAL.storedVal
        getResultIdp.approvedDomainIds == null
        getResultIdp.description == domainGroupIdp.description
        getResultIdp.issuer == domainGroupIdp.issuer
        getResultIdp.authenticationUrl == domainGroupIdp.authenticationUrl
        getResultIdp.federationType == IdentityProviderFederationTypeEnum.BROKER
        getResultIdp.publicCertificates == null
        getResultIdp.id != null
        getResultIdp.name != null

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

    def "When BROKER is specified, the approvedDomainGroup must be set, and specified as GLOBAL"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "Create IDP with missing approvedDomainGroup"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.BROKER, null, null)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)


        when: "Create IDP with non GLOBAL approvedDomainGroup"
        domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.BROKER, "NONE", null)
        response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)

        then: "400"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_IDP_INVALID_APPROVED_DOMAIN_OPTIONS)

        where:
        requestContentType              | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "When a v1 federation auth request is made w/ a SAML Response issued by a BROKER IDP, a 403 must be returned."() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)

        when: "Create IDP can set federationType of 'BROKER'"
        def keyPair = SamlCredentialUtils.generateKeyPair()
        def certificate = SamlCredentialUtils.generateCertificate(keyPair)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(certificate)
        def publicCertificate = v2Factory.createPublicCertificate(pubCertPemString)
        def publicCertificates = v2Factory.createPublicCertificates(publicCertificate)
        def samlProducerForSharedIdp = new SamlProducer(SamlCredentialUtils.generateX509Credential(certificate, keyPair))

        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.BROKER, ApprovedDomainGroupEnum.GLOBAL.storedVal, null).with {
            it.publicCertificates = publicCertificates
            it
        }

        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "federate with BROKER IDP"
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def email = "fedIntTest@invalid.rackspace.com"
        def assertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(domainGroupIdp.issuer, username, 5000, domainId, null, email, samlProducerForSharedIdp);
        response = cloud20.federatedAuthenticate(assertion)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, "Error code: 'FED-000'; v1 Authentication is not supported for this IDP")

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

    def "test that a metadata IDP cannot have certs updated outside of updating the metadata" () {
        given:
        String issuer = testUtils.getRandomUUID("issuer")
        String authenticationUrl = testUtils.getRandomUUID("authenticationUrl")
        String metadata = new SamlFactory().generateMetadataXMLForIDP(issuer, authenticationUrl)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        IdentityProvider idp = cloud20.createIdentityProviderWithMetadata(userAdminToken, metadata).getEntity(IdentityProvider)
        def keyPair = SamlCredentialUtils.generateKeyPair()
        def cert = SamlCredentialUtils.generateCertificate(keyPair)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(cert)
        def pubCertEntity = v2Factory.createPublicCertificate(pubCertPemString)

        when: "add IDP cert"
        def response = cloud20.createIdentityProviderCertificates(utils.getServiceAdminToken(), idp.id, pubCertEntity)

        then:
        assertOpenStackV2FaultResponseWithErrorCode(response, ForbiddenFault, SC_FORBIDDEN, ErrorCodes.ERROR_CODE_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP)

        when: "delete IDP cert"
        response = cloud20.deleteIdentityProviderCertificates(utils.getServiceAdminToken(), idp.id, "1")

        then:
        assertOpenStackV2FaultResponseWithErrorCode(response, ForbiddenFault, SC_FORBIDDEN, ErrorCodes.ERROR_CODE_IDP_CANNOT_MANUALLY_UPDATE_CERTS_ON_METADATA_IDP)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteIdentityProvider(idp)
    }

    @Unroll
    def "Update IDP supports end user modification - request: #requestContentType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP, true)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        def otherDomainId = utils.createDomain()
        def otherDomain = utils.createDomainEntity(otherDomainId)
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def userAdminToken = utils.getToken(userAdmin.username)
        def userManageToken = utils.getToken(userManage.username)
        def defaultUserToken = utils.getToken(defaultUser.username)

        def rackspaceCustomerNumber  = testUtils.getRandomRCN()

        // Add domains to same RCN
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = rackspaceCustomerNumber
            it
        }
        utils.updateDomain(domainId, updateDomainEntity)
        utils.updateDomain(otherDomainId, updateDomainEntity)

        def thirdDomainId = utils.createDomain()
        def thirddomain = utils.createDomainEntity(thirdDomainId)


        when: "Create IDP can set federationType of 'DOMAIN' in same domain"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId])
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "Create IDP can set federationType of 'DOMAIN' in other domain"
        IdentityProvider outsideDomainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [otherDomainId])
        response = cloud20.createIdentityProvider(idpManagerToken, outsideDomainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdpWithOtherDomain = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "Create IDP can set federationType of 'DOMAIN' with domain without rcn"
        IdentityProvider nonRcnDomainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [thirdDomainId])
        response = cloud20.createIdentityProvider(idpManagerToken, nonRcnDomainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdpWithThirdDomain = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "Create IDP can set federationType of 'DOMAIN' with multiple domains including user's domain"
        IdentityProvider multipleDomainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId, otherDomainId])
        response = cloud20.createIdentityProvider(idpManagerToken, multipleDomainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdpWithMultipleDomain = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "Create IDP can set federationType of 'DOMAIN' with multiple domains without user's domain"
        IdentityProvider multipleOtherDomainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId, otherDomainId])
        response = cloud20.createIdentityProvider(idpManagerToken, multipleOtherDomainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdpWithOtherMultipleDomain = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "Create IDP can set federationType of 'DOMAIN' without domains"
        IdentityProvider emptyDomainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        response = cloud20.createIdentityProvider(idpManagerToken, emptyDomainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdpWithoutDomain = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "User-admin try to update IdP's name whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        def name = RandomStringUtils.randomAlphanumeric(10)
        IdentityProvider updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        def updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        IdentityProvider updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 w/ updated name in response"
        updateIdpResponse.status == SC_OK
        updateResultIdp.name == name

        when: "User-admin try to update IdP's enabled attr whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        updateDomainGroupIdp = new IdentityProvider().with {
            it.enabled = !updateResultIdp.enabled
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then:
        updateIdpResponse.status == SC_OK
        updateResultIdp.enabled == updateDomainGroupIdp.enabled

        when: "User-manage try to update IdP's name whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 w/ updated name in response"
        updateIdpResponse.status == SC_OK
        updateResultIdp.name == name

        when: "User-manage try to update IdP's enabled attr whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        updateDomainGroupIdp = new IdentityProvider().with {
            it.enabled = !updateResultIdp.enabled
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then:
        updateIdpResponse.status == SC_OK
        updateResultIdp.enabled == updateDomainGroupIdp.enabled

        when: "User-admin try to update IdP's approvedDomainId whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([otherDomainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }

        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-admin try to update IdP's authenticationUrl whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.authenticationUrl = "some-voodoo-string"
            it
        }

        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP's authenticationUrl whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.authenticationUrl = "some-voodoo-string"
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-admin try to update IdP's name whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP's name whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-admin try to update IdP's description whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        def description = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 w/ updated description in response"
        updateIdpResponse.status == SC_OK
        updateResultIdp.description == description

        when: "User-manage try to update IdP's description whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        description = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 w/ updated description in response"
        updateIdpResponse.status == SC_OK
        updateResultIdp.description == description

        when: "User-admin try to update IdP's description whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        description = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP's description whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        description = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-admin try to update IdP's name for which their domain is one of the approved domains"
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdpWithMultipleDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP's name for which their domain is one of the approved domains"
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdpWithMultipleDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-admin try to update IdP's description for which their domain is one of the approved domains"
        resetCloudFeedsMock()
        description = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdpWithMultipleDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP's description for which their domain is one of the approved domains"
        resetCloudFeedsMock()
        description = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdpWithMultipleDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-admin try to update IdP for which `approvedDomainGroup` attribute is not None"
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdpWithoutDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP for which `approvedDomainGroup` attribute is not None"
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdpWithoutDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP's approvedDomainId whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([otherDomainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "rcn:admin try to update IdP's name whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        // add rcn:admin role to defaultUser
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 w/ updated name in response"
        updateIdpResponse.status == SC_OK
        updateResultIdp.name == name

        when: "rcn:admin try to update IdP's enabled attr whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        updateDomainGroupIdp = new IdentityProvider().with {
            it.enabled = !updateResultIdp.enabled
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 w/ updated name in response"
        updateIdpResponse.status == SC_OK
        updateResultIdp.enabled == updateDomainGroupIdp.enabled

        when: "rcn:admin try to update IdP's approvedDomainId whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([domainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200"
        updateIdpResponse.status == SC_OK
        updateResultIdp.approvedDomainIds.approvedDomainId.contains(domainId)
        updateResultIdp.approvedDomainIds.approvedDomainId.size() == 1

        when: "rcn:admin try to update IdP's approvedDomainId to the same approvedDomainId."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([otherDomainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200"
        updateIdpResponse.status == SC_OK
        updateResultIdp.approvedDomainIds.approvedDomainId.contains(otherDomainId)
        updateResultIdp.approvedDomainIds.approvedDomainId.size() == 1

        when: "rcn:admin try to update IdP's description whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        description = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 w/ updated description in response"
        updateIdpResponse.status == SC_OK
        updateResultIdp.description == description

        when: "rcn:admin try to update IdP's authenticationUrl whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.authenticationUrl = "some-voodoo-string"
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 but not an updated url"
        updateIdpResponse.status == SC_OK
        updateResultIdp.authenticationUrl == "http://random.url"

        when: "rcn:admin with identity-provider-manager try to update IdP's authenticationUrl whose approvedDomainId doesn't match their own domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        utils.addRoleToUser(defaultUser, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.authenticationUrl = "some-voodoo-string"
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)
        updateResultIdp = updateIdpResponse.getEntity(IdentityProvider)

        then: "Expect a 200 with an updated url"
        updateIdpResponse.status == SC_OK
        updateResultIdp.authenticationUrl == "some-voodoo-string"

        when: "rcn-admin try to update IdP's approvedDomainId to a list of approved domain ids."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([otherDomainId, domainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "rcn:admin try to update IdP's approvedDomainId whose approvedDomainId does not have RCN with approvedDomainId that has rcn."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([domainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdpWithThirdDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdpWithThirdDomain.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdpWithoutDomain.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdpWithOtherDomain.id)
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdpWithMultipleDomain.id)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteDomain(otherDomainId)
        utils.deleteDomain(thirdDomainId)
        utils.deleteUserQuietly(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "CID-948 - Update IDP supports end user modification for rcn - request: #requestContentType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP, true)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        def otherDomainId = utils.createDomain()
        def otherDomain = utils.createDomainEntity(otherDomainId)
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUserToken = utils.getToken(defaultUser.username)

        def rackspaceCustomerNumber  = testUtils.getRandomRCN()

        // Add domains to same RCN
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = rackspaceCustomerNumber
            it
        }
        utils.updateDomain(domainId, updateDomainEntity)
        utils.updateDomain(otherDomainId, updateDomainEntity)

        def thirdDomainId = utils.createDomain()
        def thirddomain = utils.createDomainEntity(thirdDomainId)


        when: "Create IDP can set federationType of 'DOMAIN' in same domain"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId])
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "Create IDP can set federationType of 'DOMAIN' in other domain"
        IdentityProvider outsideDomainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [otherDomainId])
        response = cloud20.createIdentityProvider(idpManagerToken, outsideDomainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdpWithOtherDomain = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "rcn:admin try to update IdP's approvedDomainId whose approvedDomainId is in a different rcn."
        resetCloudFeedsMock()
        def name = RandomStringUtils.randomAlphanumeric(10)
        IdentityProvider updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([thirdDomainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }
        def updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdpWithOtherDomain.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdpWithOtherDomain.id)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteDomain(otherDomainId)
        utils.deleteDomain(thirdDomainId)
        utils.deleteUserQuietly(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Attempt to update IDP with feature disabled - request: #requestContentType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP, false)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        def otherDomainId = utils.createDomain()
        def otherDomain = utils.createDomainEntity(otherDomainId)
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def userAdminToken = utils.getToken(userAdmin.username)
        def userManageToken = utils.getToken(userManage.username)
        def defaultUserToken = utils.getToken(defaultUser.username)

        def rackspaceCustomerNumber = testUtils.getRandomRCN()

        // Add domains to same RCN
        def updateDomainEntity = new Domain().with {
            it.rackspaceCustomerNumber = rackspaceCustomerNumber
            it
        }
        utils.updateDomain(domainId, updateDomainEntity)
        utils.updateDomain(otherDomainId, updateDomainEntity)


        when: "Create IDP can set federationType of 'DOMAIN' in same domain"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId])
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, requestContentType, requestContentType)
        IdentityProvider creationResultIdp = response.getEntity(IdentityProvider)

        then: "created successfully"
        response.status == SC_CREATED

        when: "User-admin try to update IdP's name whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        def name = RandomStringUtils.randomAlphanumeric(10)
        IdentityProvider updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it.description = description
            it
        }
        def updateIdpResponse = cloud20.updateIdentityProvider(userAdminToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "User-manage try to update IdP's name whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it.description = description
            it
        }
        updateIdpResponse = cloud20.updateIdentityProvider(userManageToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "rcn:admin try to update IdP's name whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it
        }
        // add rcn:admin role to defaultUser
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        when: "rcn:admin try to update IdP's approvedDomainId whose approvedDomainId matches their own domain & is the only approved domain."
        resetCloudFeedsMock()
        name = RandomStringUtils.randomAlphanumeric(10)
        updateDomainGroupIdp = new IdentityProvider().with {
            ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
            approvedDomainIds.getApprovedDomainId().addAll([otherDomainId])
            it.approvedDomainIds = approvedDomainIds
            it
        }
        // add rcn:admin role to defaultUser
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        updateIdpResponse = cloud20.updateIdentityProvider(defaultUserToken, creationResultIdp.id, updateDomainGroupIdp, requestContentType, requestContentType)

        then: "Expect a 403"
        updateIdpResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteDomain(otherDomainId)
        utils.deleteUserQuietly(idpManager)

        where:
        requestContentType | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    private IdentityProvider getUpdateDomainGroupIdp(name, description, domainId=null) {
        IdentityProvider updateDomainGroupIdp = new IdentityProvider().with {
            it.name = name
            it.description = description
            if (domainId) {
                ApprovedDomainIds approvedDomainIds = new ApprovedDomainIds()
                approvedDomainIds.getApprovedDomainId().addAll([domainId])
                it.approvedDomainIds = approvedDomainIds
            }
            it
        }
        updateDomainGroupIdp
    }

    def "Test delete IDP with roles identity:user-admin, identity:user-manage and rcn:admin" () {

        given:
        String issuer = testUtils.getRandomUUID("issuer")
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
                (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "Creating IDP using idpManager token"
        IdentityProvider approvedDomainsIdp = v2Factory.createIdentityProvider(getRandomUUID(), "description", issuer, IdentityProviderFederationTypeEnum.DOMAIN, null, [domainId].asList())
        def response = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp)
        IdentityProvider idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED
        idp.issuer == issuer

        when: "Deleting IDP using userAdmin token"
        def userAdminToken = utils.getToken(userAdmin.username)
        response = cloud20.deleteIdentityProvider(userAdminToken, idp.id)

        then:
        response.status == SC_NO_CONTENT

        when: "Deleting IDP using userManage token"
        def response1 = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp)
        IdentityProvider idp1 = response1.getEntity(IdentityProvider)
        def userManageToken = utils.getToken(userManage.username)
        response1 = cloud20.deleteIdentityProvider(userManageToken, idp1.id)

        then:
        response1.status == SC_NO_CONTENT

        when: "Deleting IDP using defaultUser token w/o rcn:admin role"
        def response2 = cloud20.createIdentityProvider(idpManagerToken, approvedDomainsIdp)
        IdentityProvider idp2 = response2.getEntity(IdentityProvider)
        def defaultUserToken = utils.getToken(defaultUser.username)
        response2 = cloud20.deleteIdentityProvider(defaultUserToken, idp2.id)

        then:
        response2.status == SC_FORBIDDEN

        when: "Deleting IDP using defaultUser token with rcn:admin role"
        utils.addRoleToUser(defaultUser, Constants.RCN_ADMIN_ROLE_ID)
        def defaultUserTokenWithRcnRole = utils.getToken(defaultUser.username)
        response2 = cloud20.deleteIdentityProvider(defaultUserTokenWithRcnRole, idp2.id)

        then:
        response2.status == SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }
}
