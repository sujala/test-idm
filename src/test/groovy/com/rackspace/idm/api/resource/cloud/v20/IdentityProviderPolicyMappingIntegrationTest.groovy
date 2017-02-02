package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.IdentityPropertyValueType
import com.rackspace.idm.domain.service.FederatedIdentityService
import com.rackspace.idm.validation.Validator20
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class IdentityProviderPolicyMappingIntegrationTest extends RootIntegrationTest {

    @Autowired
    FederatedIdentityService federatedIdentityService

    @Autowired
    IdentityProviderDao identityProviderDao

    @Autowired
    Validator20 validator20

    def "Update and get IDP's policy"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)

        when: "Update IDP's policy"
        resetCloudFeedsMock()
        def policy = '{"policy":{"name":"name"}}'
        def response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, policy)

        then: "Return 204 No Content"
        response.status == SC_NO_CONTENT

        and: "only one event was posted"
        def idpEntity = federatedIdentityService.getIdentityProviderByIssuer(creationResultIdp.issuer)
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "the event was an UPDATE event for the IDP"
        cloudFeedsMock.verify(
                testUtils.createIdpFeedsRequest(idpEntity, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "Getting IDP's policy"
        response = cloud20.getIdentityProviderPolicy(idpManagerToken, creationResultIdp.id)

        then: "Return 200 OK"
        response.status == SC_OK
        def body = response.getEntity(String)
        body == policy

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
    }

    def "Assert error for IDP's policy file exceeding the maximum size"() {
        given:
        def maxSize = 1
        reloadableConfiguration.setProperty(IdentityConfig.IDP_POLICY_MAX_KILOBYTE_SIZE_PROP, maxSize)
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)

        when: "Update IDP's policy exceeding max size restriction"
        resetCloudFeedsMock()
        def policy = '{ "mapping": { "rules": [ { "remote": [ { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:NameID" }, { "name":"email" }, { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:SubjectConfirmation\\/saml2:SubjectConfirmationData\\/@NotOnOrAfter" }, { "name":"domain" }, { "name":"roles", "multiValue":true, "blacklist": ["nova:admin"] }, { "remote": [ { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:NameID" }, { "name":"email" }, { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:SubjectConfirmation\\/saml2:SubjectConfirmationData\\/@NotOnOrAfter" }, { "name":"domain" }, { "name":"roles", "multiValue":true, "blacklist": ["nova:admin"] }, { "remote": [ { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:NameID" }, { "name":"email" }, { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:SubjectConfirmation\\/saml2:SubjectConfirmationData\\/@NotOnOrAfter" }, { "name":"domain" }, { "name":"roles", "multiValue":true, "blacklist": ["nova:admin"] } ], "local": { "user": { "domain":"{3}", "name":"{0}", "email":"{1}", "roles":"{4}", "expire":"{2}" } } } ], "version" : "RAX-1" } }'
        def response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, policy)

        then: "Return 400 BadRequest"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, String.format(Validator20.FEDERATION_IDP_POLICY_MAX_SIZE_EXCEED_ERROR_MESSAGE, maxSize))

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
        reloadableConfiguration.reset()
    }

    def "Error check on Update IDP's policy"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)
        def policy = '{"policy":{"name":"name"}}'
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        resetCloudFeedsMock()

        when: "Missing identity:identity-provider-manager role"
        def token = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        def response = cloud20.updateIdentityProviderPolicy(token, creationResultIdp.id, policy)

        then: "Return 403"
        response.status == SC_FORBIDDEN

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Invalid token"
        response = cloud20.updateIdentityProviderPolicy("invalid", creationResultIdp.id, policy)

        then: "Return 401"
        response.status == SC_UNAUTHORIZED

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Invalid json"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, '{"policy":}')

        then: "Return 400"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, Validator20.FEDERATION_IDP_POLICY_INVALID_JSON_ERROR_MESSAGE)

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Null body"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, null)

        then: "Return 400"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, Validator20.FEDERATION_IDP_POLICY_INVALID_JSON_ERROR_MESSAGE)

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Empty body"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, "")

        then: "Return 400"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, Validator20.FEDERATION_IDP_POLICY_INVALID_JSON_ERROR_MESSAGE)

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Invalid ipd id"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, "invalid", policy)

        then: "Return 404"
        response.status == SC_NOT_FOUND

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Invalid content type"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, policy, MediaType.APPLICATION_XML_TYPE)

        then: "Return 415"
        response.status == SC_UNSUPPORTED_MEDIA_TYPE

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
        utils.deleteUsers(users)
    }

    def "Error check on Get IDP's policy"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        resetCloudFeedsMock()

        when: "Missing identity:identity-provider-manager role"
        def token = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        def response = cloud20.getIdentityProviderPolicy(token, creationResultIdp.id)

        then: "Return 403"
        response.status == SC_FORBIDDEN

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Invalid token"
        response = cloud20.getIdentityProviderPolicy("invalid", creationResultIdp.id)

        then: "Return 401"
        response.status == SC_UNAUTHORIZED

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Invalid ipd id"
        response = cloud20.getIdentityProviderPolicy(idpManagerToken, "invalid")

        then: "Return 404"
        response.status == SC_NOT_FOUND

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        when: "Invalid accept type"
        response = cloud20.getIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, MediaType.APPLICATION_XML_TYPE)

        then: "Return 406"
        response.status == SC_NOT_ACCEPTABLE

        and: "no event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(0)
        )

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
        utils.deleteUsers(users)
    }

    def "IDP with a null policy in the directory returns the default policy"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)
        def idpEntity = identityProviderDao.getIdentityProviderById(creationResultIdp.id)
        idpEntity.setPolicy(null)
        identityProviderDao.updateIdentityProviderAsIs(idpEntity)

        when: "Get IDP's policy"
        def response = cloud20.getIdentityProviderPolicy(idpManagerToken, creationResultIdp.id)

        then: "Return 200"
        response.status == SC_OK
        def body = response.getEntity(String)
        body == utils.getIdentityPropertyByName(IdentityConfig.FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP).value

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
    }

    def "Get IDP's policy with read-only role"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)

        when: "Get IDP's policy"
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_PROVIDER_READ_ONLY_ROLE_ID)
        def token = utils.getToken(identityAdmin.username, Constants.DEFAULT_PASSWORD)
        def response = cloud20.getIdentityProviderPolicy(token, creationResultIdp.id)

        then: "Return 200"
        response.status == SC_OK

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
        utils.deleteUser(identityAdmin)
    }

    def "create IDP sets the default policy on the IDP"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)

        when: "Get IDP's policy"
        def response = cloud20.getIdentityProviderPolicy(idpManagerToken, creationResultIdp.id)

        then: "Return 200"
        response.status == SC_OK
        def body = response.getEntity(String)
        body == utils.getIdentityPropertyByName(IdentityConfig.FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP).value

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
    }

    def "test validation of Identity Provider default policy property"() {
        given:
        // Delete the default policy. This is messy but other tests expect for the policy property to already exist
        def defaultPolicyPropData = utils.getIdentityPropertyByName(IdentityConfig.FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP)
        utils.deleteIdentityProperty(defaultPolicyPropData.id)
        def defaultPolicyProp = new IdentityProperty().with {
            it.name = defaultPolicyPropData.name
            it.description = defaultPolicyPropData.description
            it.value = defaultPolicyPropData.value
            it.valueType = defaultPolicyPropData.valueType
            it.idmVersion = defaultPolicyPropData.versionAdded
            it.reloadable = defaultPolicyPropData.reloadable
            it.searchable = true
            it
        }
        def propData = v2Factory.createIdentityProperty(IdentityConfig.FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP)
        propData.valueType = IdentityPropertyValueType.JSON.typeName
        def jsonTemplate = "{\"prop\" : {\"value\": \"\"} }"

        when: "try to create the property with a value larger than the max allowed identity provider policy size"
        def jsonAttrValue = testUtils.randomAlphaStringWithLengthInBytes(identityConfig.getReloadableConfig().getIdpPolicyMaxSize() * 1024 + 1 - testUtils.getStringLenghtInBytes(jsonTemplate))
        propData.value = "{\"prop\" : {\"value\": \"$jsonAttrValue\"} }"
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propData)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, validator20.idpPolicyMaxSizeExceededErrorMessage)

        when: "try to create the identity property with a value equal to the max allowed identity provider policy size"
        jsonAttrValue = testUtils.randomAlphaStringWithLengthInBytes(identityConfig.getReloadableConfig().getIdpPolicyMaxSize() * 1024 - testUtils.getStringLenghtInBytes(jsonTemplate))
        propData.value = "{\"prop\" : {\"value\": \"$jsonAttrValue\"} }"
        response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propData)

        then: "success"
        response.status == SC_CREATED
        def property = response.getEntity(IdentityProperty)

        when: "try to update the property with a value larger than the max allowed identity provider policy size"
        jsonAttrValue = testUtils.randomAlphaStringWithLengthInBytes(identityConfig.getReloadableConfig().getIdpPolicyMaxSize() * 1024 + 1 - testUtils.getStringLenghtInBytes(jsonTemplate))
        propData.value = "{\"prop\" : {\"value\": \"$jsonAttrValue\"} }"
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propData)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, validator20.idpPolicyMaxSizeExceededErrorMessage)

        when: "try to update the property with a value equal to the max allowed identity provider policy size"
        jsonAttrValue = testUtils.randomAlphaStringWithLengthInBytes(identityConfig.getReloadableConfig().getIdpPolicyMaxSize() * 1024 - testUtils.getStringLenghtInBytes(jsonTemplate))
        propData.value = "{\"prop\" : {\"value\": \"$jsonAttrValue\"} }"
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propData)

        then: "success"
        response.status == SC_OK

        cleanup:
        utils.deleteIdentityProperty(property.id)
        devops.createIdentityProperty(utils.getIdentityAdminToken(), defaultPolicyProp)
    }

    def "test IDP creation is unavailable when IDP default policy property is missing"() {
        given:
        // Delete the default policy. This is messy but other tests expect for the policy property to already exist
        def defaultPolicyPropData = utils.getIdentityPropertyByName(IdentityConfig.FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP)
        utils.deleteIdentityProperty(defaultPolicyPropData.id)
        def idpData = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def defaultPolicyProp = new IdentityProperty().with {
            it.name = defaultPolicyPropData.name
            it.description = defaultPolicyPropData.description
            it.value = defaultPolicyPropData.value
            it.valueType = defaultPolicyPropData.valueType
            it.idmVersion = defaultPolicyPropData.versionAdded
            it.reloadable = defaultPolicyPropData.reloadable
            it.searchable = true
            it
        }

        when: "try to create an IDP without the default policy property existing"
        def response = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData)

        then: "error - unavailable"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_SERVICE_UNAVAILABLE, DefaultCloud20Service.FEDERATION_IDP_CREATION_NOT_AVAILABLE_MISSING_DEFAULT_POLICY_MESSAGE)

        when: "create the default policy property and try to create the IDP again"
        devops.createIdentityProperty(utils.getIdentityAdminToken(), defaultPolicyProp)
        response = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData)

        then: "success"
        response.status == SC_CREATED
        def idp = response.getEntity(IdentityProvider)

        cleanup:
        utils.deleteIdentityProvider(idp)
    }

}
