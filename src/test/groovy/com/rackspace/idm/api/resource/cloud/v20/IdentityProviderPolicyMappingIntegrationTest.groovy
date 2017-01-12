package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import org.openstack.docs.identity.api.v2.BadRequestFault
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class IdentityProviderPolicyMappingIntegrationTest extends RootIntegrationTest {

    def "Update and get IDP's policy"() {
        given:
        def idpManager = utils.createIdentityProviderManager()
        def idpManagerToken = utils.getToken(idpManager.username)
        def identityProvider = v2Factory.createIdentityProvider(getRandomUUID(), "description", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, null)
        def creationResultIdp = utils.createIdentityProvider(idpManagerToken, identityProvider)

        when: "Update IDP's policy"
        def policy = '{"policy":{"name":"name"}}'
        def response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, policy)

        then: "Return 204 No Content"
        response.status == SC_NO_CONTENT

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
        def policy = '{ "mapping": { "rules": [ { "remote": [ { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:NameID" }, { "name":"email" }, { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:SubjectConfirmation\\/saml2:SubjectConfirmationData\\/@NotOnOrAfter" }, { "name":"domain" }, { "name":"roles", "multiValue":true, "blacklist": ["nova:admin"] }, { "remote": [ { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:NameID" }, { "name":"email" }, { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:SubjectConfirmation\\/saml2:SubjectConfirmationData\\/@NotOnOrAfter" }, { "name":"domain" }, { "name":"roles", "multiValue":true, "blacklist": ["nova:admin"] }, { "remote": [ { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:NameID" }, { "name":"email" }, { "path":"\\/saml2p:Response\\/saml2:Assertion\\/saml2:Subject\\/saml2:SubjectConfirmation\\/saml2:SubjectConfirmationData\\/@NotOnOrAfter" }, { "name":"domain" }, { "name":"roles", "multiValue":true, "blacklist": ["nova:admin"] } ], "local": { "user": { "domain":"{3}", "name":"{0}", "email":"{1}", "roles":"{4}", "expire":"{2}" } } } ], "version" : "RAX-1" } }'
        def response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, policy)

        then: "Return 400 BadRequest"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, String.format(DefaultCloud20Service.POLICY_MAX_SIZE_EXCEED_ERROR_MESSAGE, maxSize))

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

        when: "Missing identity:identity-provider-manager role"
        def token = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        def response = cloud20.updateIdentityProviderPolicy(token, creationResultIdp.id, policy)

        then: "Return 403"
        response.status == SC_FORBIDDEN

        when: "Invalid token"
        response = cloud20.updateIdentityProviderPolicy("invalid", creationResultIdp.id, policy)

        then: "Return 401"
        response.status == SC_UNAUTHORIZED

        when: "Invalid json"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, '{"policy":}')

        then: "Return 400"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, DefaultCloud20Service.POLICY_INVALID_JSON_ERROR_MESSAGE)

        when: "Null body"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, null)

        then: "Return 400"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, DefaultCloud20Service.POLICY_INVALID_JSON_ERROR_MESSAGE)

        when: "Empty body"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, "")

        then: "Return 400"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, DefaultCloud20Service.POLICY_INVALID_JSON_ERROR_MESSAGE)

        when: "Invalid ipd id"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, "invalid", policy)

        then: "Return 404"
        response.status == SC_NOT_FOUND

        when: "Invalid content type"
        response = cloud20.updateIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, policy, MediaType.APPLICATION_XML_TYPE)

        then: "Return 415"
        response.status == SC_UNSUPPORTED_MEDIA_TYPE

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

        when: "Missing identity:identity-provider-manager role"
        def token = utils.getToken(userAdmin.username, Constants.DEFAULT_PASSWORD)
        def response = cloud20.getIdentityProviderPolicy(token, creationResultIdp.id)

        then: "Return 403"
        response.status == SC_FORBIDDEN

        when: "Invalid token"
        response = cloud20.getIdentityProviderPolicy("invalid", creationResultIdp.id)

        then: "Return 401"
        response.status == SC_UNAUTHORIZED

        when: "Invalid ipd id"
        response = cloud20.getIdentityProviderPolicy(idpManagerToken, "invalid")

        then: "Return 404"
        response.status == SC_NOT_FOUND

        when: "Invalid accept type"
        response = cloud20.getIdentityProviderPolicy(idpManagerToken, creationResultIdp.id, MediaType.APPLICATION_XML_TYPE)

        then: "Return 406"
        response.status == SC_NOT_ACCEPTABLE

        cleanup:
        utils.deleteIdentityProviderQuietly(idpManagerToken, creationResultIdp.id)
        utils.deleteUser(idpManager)
        utils.deleteUsers(users)
    }

    def "Assert Get IDP's policy returns empty json object"() {
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
        body == "{}"

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
}
