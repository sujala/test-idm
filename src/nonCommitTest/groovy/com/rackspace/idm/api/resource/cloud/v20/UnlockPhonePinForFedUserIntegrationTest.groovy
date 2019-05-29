package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.dao.FederatedUserDao
import org.apache.http.HttpStatus
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_NO_CONTENT
import static org.apache.http.HttpStatus.SC_OK

class UnlockPhonePinForFedUserIntegrationTest extends RootIntegrationTest {

    @Autowired
    FederatedUserDao federatedUserRepository

    @Shared
    String sharedServiceAdminToken
    @Shared
    String sharedIdentityAdminToken
    @Shared
    IdentityProvider sharedBrokerIdp
    @Shared
    Credential sharedBrokerIdpCredential
    @Shared
    IdentityProvider sharedOriginIdp
    @Shared
    Credential sharedOriginIdpCredential
    @Shared
    User sharedUserAdmin
    @Shared
    FederatedDomainAuthRequestGenerator sharedFederatedDomainAuthRequestGenerator

    def setupSpec() {
        sharedServiceAdminToken = cloud20.authenticateForToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        sharedIdentityAdminToken = cloud20.authenticateForToken(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedBrokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        sharedOriginIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)

        sharedFederatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
    }

    def createIdpViaRest(IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, type, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def setup() {
        reloadableConfiguration.reset()
    }

    void doCleanupSpec() {
        cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedBrokerIdp.id)
        cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedOriginIdp.id)
        cloud20.deleteUser(sharedServiceAdminToken, sharedUserAdmin.id)
    }

    def "SAML assertion 2.0 - verify that only phone pin admin or user him self can unlock the phone pin"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        def fedUserId = authResponse.user.id

        then:
        assert authClientResponse.status == SC_OK

        when: "unlock phone pin with default identityAdminToken that has the identity:phone-pin-admin added to it"
        utils.lockPhonepin(fedUserId)
        def response = cloud20.unlockPhonePin(utils.getIdentityAdminToken(), fedUserId)

        then: "expect 204"
        response.status == SC_NO_CONTENT

        when: "unlock phone pin using users token"
        utils.lockPhonepin(fedUserId)
        response = cloud20.unlockPhonePin(authResponse.token.id, fedUserId)

        then: "expect 204"
        response.status == SC_NO_CONTENT

        when: "unlock phone pin with some other user who does not have the identity:phone-pin-admin role"
        utils.lockPhonepin(fedUserId)
        def userAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        response = cloud20.unlockPhonePin(utils.getToken(userAdmin.username), fedUserId)

        then: "expect 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username)
    }


    def "Verify that phone pin can be unlocked only for federated users whose phone pin is in locked state"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        def fedUserId = authResponse.user.id

        then:
        assert authClientResponse.status == SC_OK

        when: "Federated user attempts to unlock his phone pin which is not in locked state"
        def response = cloud20.unlockPhonePin(authResponse.token.id, fedUserId)

        then: "expect 403"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_PHONE_PIN_NOT_LOCKED, ErrorCodes.ERROR_MESSAGE_PHONE_PIN_NOT_LOCKED))

        when: "Federated user attempts to unlock his phone pin which is in locked state"
        utils.lockPhonepin(fedUserId)
        response = cloud20.unlockPhonePin(authResponse.token.id, fedUserId)

        then: "expect 204"
        response.status == SC_NO_CONTENT

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username)
    }
}