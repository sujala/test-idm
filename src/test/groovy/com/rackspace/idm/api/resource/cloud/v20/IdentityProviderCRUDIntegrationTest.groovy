package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ApprovedDomainIds
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PublicCertificates
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.service.FederatedIdentityService
import com.rackspace.idm.domain.service.IdentityProviderTypeFilterEnum
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.impl.DefaultFederatedIdentityService
import com.rackspace.idm.validation.Validator20
import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.ServiceUnavailableFault
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

    @Autowired
    FederatedIdentityService federatedIdentityService

    @Autowired
    FederatedUserDao federatedUserRepository

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

        when: "create a DOMAIN IDP with approvedDomainGroup, empty approvedDomainId list"
        IdentityProvider domainGroupIdp = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, Collections.EMPTY_LIST)
        def response = cloud20.createIdentityProvider(idpManagerToken, domainGroupIdp, MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_XML_TYPE) //our json reader would send NULL rather than empty array so json would pass (appropriately)

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

}
