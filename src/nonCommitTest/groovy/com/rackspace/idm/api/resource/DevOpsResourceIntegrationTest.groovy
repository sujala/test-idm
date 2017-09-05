package com.rackspace.idm.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.SamlProducer

import static com.rackspace.idm.Constants.getDEFAULT_PASSWORD
import static com.rackspace.idm.Constants.getSCOPE_SETUP_MFA

class DevOpsResourceIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    UserDao userRepository

    def "test federation deletion call"() {
        given:
        def response = devops.getFederationDeletion(utils.getServiceAdminToken())

        when:
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).federatedUsersDeletionResponse

        then:
        response.status == 200
        entity.id != null
    }

    def "test federation deletion call cannot be done by who doesnt have role"() {
        when:
        def response = devops.getFederationDeletion(utils.getIdentityAdminToken())

        then:
        response.status == 403
    }

    @Unroll
    def "test analyze token for a provisioned user token"() {
        def user
        (user) = utils.createUserAdmin()
        def userToken = utils.getToken(user.username)
        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)

        when: "no X-Auth-Token is passed"
        def response = devops.analyzeToken(null, userToken)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

        when: "X-Auth-Token doesn't have valid role (identity:analyze-token) to perform the operation"
        response = devops.analyzeToken(analyzeAdminToken, userToken)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "subject token is empty"
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)
        response = devops.analyzeToken(analyzeAdminToken, null)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Must provide an X-Subject-Token header with the token to analyze")

        when: "invalid token"
        response = devops.analyzeToken(analyzeAdminToken, "invalidToken#!!***")

        then:
        response.status == HttpStatus.SC_OK
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        entity.tokenDecryptable == false
        entity.tokenValid == false
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.decryptionException.message == "Error code: 'AEU-0001'; Error encountered decoding web token"

        when: "error while decrypting the given token"
        response = devops.analyzeToken(analyzeAdminToken, "AAD1PcpjrLcjw8ZeEQ_M5UrZws0QM9hM0_DUnZ7c2s4jqlD5NmC2wDBf7_jePjFbBDI--QV5iOvH5HYMxha2")
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.tokenDecryptable == false
        entity.tokenValid == false
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.decryptionException.message == "Error code: 'AMD-0000'; Error encountered decrypting bytes"

        when: "valid x-auth-token and valid subject token is passed"
        response = devops.analyzeToken(analyzeAdminToken, userToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.tokenDecryptable == true
        entity.tokenValid == true
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.token.token == userToken
        entity.token.type == "USER"

        entity.user.id == user.id
        entity.user.username == user.username
        entity.user.type == "PROVISIONED_USER"
        entity.user.domain == user.domainId
        entity.user.enabled == user.enabled
        entity.user.domainEnabled == true

        entity.trrs == []

        when: "subject token passed is revoked"
        utils.revokeToken(userToken)

        response = devops.analyzeToken(analyzeAdminToken, userToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.token.token == userToken
        entity.token.type == "USER"

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == true
        entity.tokenExpired == false

        entity.trrs.size == 1

        cleanup:
        utils.deleteUser(identityAdmin)
        utils.deleteUser(user)
    }


    @Unroll
    def "test analyze token for racker token"() {
        given:

        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)

        def authResponse = utils.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD)
        def rackerToken = authResponse.token.id

        when: "valid x-auth-token and valid subject token is passed"
        def response = devops.analyzeToken(analyzeAdminToken, rackerToken)

        then:
        response.status == HttpStatus.SC_OK
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        entity.tokenDecryptable == true
        entity.tokenValid == true
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.token.token == rackerToken
        entity.token.type == "RACKER"

        entity.user.id == Constants.RACKER
        entity.user.username == Constants.RACKER
        entity.user.type == "RACKER"
        entity.user.enabled == true

        entity.trrs == []

        when: "subject token passed is revoked"
        utils.revokeToken(rackerToken)
        response = devops.analyzeToken(analyzeAdminToken, rackerToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.token.token == rackerToken
        entity.token.type == "RACKER"

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == true
        entity.tokenExpired == false

        entity.trrs.size == 1

        cleanup:
        utils.deleteUser(identityAdmin)
    }

    @Unroll
    def "test analyze token for a impersonated token"() {
        given:

        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)

        def user
        (user) = utils.createUserAdmin()
        def impersonationToken = utils.getImpersonatedTokenWithToken(analyzeAdminToken, user)

        when: "valid x-auth-token and valid subject token is passed"
        def response = devops.analyzeToken(analyzeAdminToken, impersonationToken)

        then:
        response.status == HttpStatus.SC_OK
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        entity.tokenDecryptable == true
        entity.tokenValid == true
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.token.token == impersonationToken
        entity.token.type == "IMPERSONATION"

        entity.impersonatedUser.id == user.id
        entity.impersonatedUser.username == user.username
        entity.impersonatedUser.type == "PROVISIONED_USER"
        entity.impersonatedUser.domain == user.domainId
        entity.impersonatedUser.enabled == user.enabled
        entity.impersonatedUser.domainEnabled == true

        entity.user.username == identityAdmin.username
        entity.user.id == identityAdmin.id
        entity.user.type == "PROVISIONED_USER"
        entity.user.domain == identityAdmin.domainId
        entity.user.enabled == true
        entity.user.domainEnabled == true

        entity.trrs == []

        when: "subject token passed is revoked"
        utils.revokeToken(impersonationToken)
        response = devops.analyzeToken(analyzeAdminToken, impersonationToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.token.token == impersonationToken
        entity.token.type == "IMPERSONATION"

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == true
        entity.tokenExpired == false

        entity.trrs.size == 1

        cleanup:
        utils.deleteUser(identityAdmin)
        utils.deleteUser(user)
    }

    @Unroll
    def "test analyze token for a federated user token"() {
        given:

        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)

        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def idpCredential = SamlCredentialUtils.generateX509Credential()
        def samlProducer = new SamlProducer(idpCredential)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(idpCredential.entityCertificate)
        def issuer = UUID.randomUUID().toString()
        def idpUrl = UUID.randomUUID().toString()
        IdentityProvider idp

        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts)

        def idpData = v2Factory.createIdentityProvider(issuer, "test", idpUrl, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        idp = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData).getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider)

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(idp.issuer, RandomStringUtils.randomAscii(25), 1000, domainId, null, "${RandomStringUtils.randomAlphanumeric(8)}@example.com", samlProducer)
        def federationResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(federationResponse.user.id)
        def federatedUserToken = federationResponse.token.id

        when: "valid x-auth-token and valid subject token is passed"
        def response = devops.analyzeToken(analyzeAdminToken, federatedUserToken)

        then:
        response.status == HttpStatus.SC_OK
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        entity.tokenDecryptable == true
        entity.tokenValid == true
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.token.token == federatedUserToken
        entity.token.type == "USER"

        entity.user.id == federatedUser.id
        entity.user.username == federatedUser.username
        entity.user.type == "FEDERATED_USER"
        entity.user.domain == domainId
        entity.user.enabled == true
        entity.user.domainEnabled == true
        entity.user.federatedIdp == federatedUser.federatedIdp

        entity.trrs == []

        when: "subject token passed is revoked"
        utils.revokeToken(federatedUserToken)
        response = devops.analyzeToken(analyzeAdminToken, federatedUserToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.token.token == federatedUserToken
        entity.token.type == "USER"

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == true
        entity.tokenExpired == false

        entity.trrs.size == 1

        when: "impersonating a federated user"
        def impersonationToken = utils.getImpersonatedTokenWithToken(analyzeAdminToken, federatedUser)
        response = devops.analyzeToken(analyzeAdminToken, impersonationToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.impersonatedUser.id == federatedUser.id
        entity.impersonatedUser.username == federatedUser.username
        entity.impersonatedUser.type == "FEDERATED_USER"
        entity.impersonatedUser.domain == domainId
        entity.impersonatedUser.enabled == true
        entity.impersonatedUser.domainEnabled == true
        entity.impersonatedUser.federatedIdp == federatedUser.federatedIdp

        entity.user.username == identityAdmin.username
        entity.user.id == identityAdmin.id
        entity.user.type == "PROVISIONED_USER"
        entity.user.domain == identityAdmin.domainId
        entity.user.enabled == true
        entity.user.domainEnabled == true

        entity.trrs == []

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUser(identityAdmin)
        utils.deleteIdentityProvider(idp)
    }

    @Unroll
    def "test analyze token by disabling a federated user"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)

        def domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        def idpCredential = SamlCredentialUtils.generateX509Credential()
        def samlProducer = new SamlProducer(idpCredential)
        def pubCertPemString = SamlCredentialUtils.getCertificateAsPEMString(idpCredential.entityCertificate)
        def issuer = UUID.randomUUID().toString()
        def idpUrl = UUID.randomUUID().toString()
        IdentityProvider idp

        def pubCerts = v2Factory.createPublicCertificate(pubCertPemString)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts)

        def idpData = v2Factory.createIdentityProvider(issuer, "blah", idpUrl, IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        idp = cloud20.createIdentityProvider(utils.getServiceAdminToken(), idpData).getEntity(IdentityProvider)

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(idp.issuer, RandomStringUtils.randomAscii(25), 1000, domainId, null, "${RandomStringUtils.randomAlphanumeric(8)}@example.com", samlProducer)
        def federationResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(federationResponse.user.id)
        def federatedUserToken = federationResponse.token.id

        def idpRequest = new IdentityProvider().with {
            it.enabled = false
            it
        }
        cloud20.updateIdentityProvider(utils.getServiceAdminToken(), idp.id, idpRequest)

        when: "subject token passed is revoked as the idp is disabled"
        def response = devops.analyzeToken(analyzeAdminToken, federatedUserToken)
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.token.token == federatedUserToken
        entity.token.type == "USER"

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == true
        entity.tokenExpired == false

        entity.user.id == federatedUser.id
        entity.user.username == federatedUser.username
        entity.user.type == "FEDERATED_USER"
        entity.user.domain == domainId
        entity.user.enabled == true
        entity.user.domainEnabled == true

        entity.trrs.size == 1
        entity.trrs[0].identityProviderId == idp.id

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUser(identityAdmin)
        utils.deleteIdentityProvider(idp)
    }

    @Unroll
    def "test analyze token for a federated racker token"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)

        def username = Constants.RACKER
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedRacker(Constants.RACKER_IDP_URI, username, 100);

        AuthenticateResponse rackerAuthResponse = utils.authenticateRacker(username, Constants.RACKER_PASSWORD)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        def federatedRackerToken = samlResponse.token.id

        when: "valid x-auth-token and valid subject token is passed"
        def response = devops.analyzeToken(analyzeAdminToken, federatedRackerToken)

        then:
        response.status == HttpStatus.SC_OK

        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        entity.tokenDecryptable == true
        entity.tokenValid == true
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.token.token == federatedRackerToken
        entity.token.type == "RACKER"

        entity.user.username == samlResponse.user.name
        entity.user.type == "RACKER"
        entity.user.enabled == true

        entity.trrs == []

        when: "subject token passed is revoked"
        utils.revokeToken(federatedRackerToken)
        response = devops.analyzeToken(analyzeAdminToken, federatedRackerToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.token.token == federatedRackerToken
        entity.token.type == "RACKER"

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == true
        entity.tokenExpired == false

        entity.trrs.size == 1

        cleanup:
        utils.deleteUser(identityAdmin)
    }

    def "test analyze token for a scoped token"() {
        given:

        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)

        def domainId = utils.createDomain()
        def userAdmin
        (userAdmin) = utils.createUserAdmin(domainId)
        def initialUserAdmin = userRepository.getUserById(userAdmin.id)
        initialUserAdmin.userMultiFactorEnforcementLevel = GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED
        userRepository.updateUserAsIs(initialUserAdmin)
        def scopedAuthResponse = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        def scopedEntity = scopedAuthResponse.getEntity(AuthenticateResponse).value
        def scopedToken = scopedEntity.token.id

        when: "valid x-auth-token and valid subject token is passed"
        def response = devops.analyzeToken(analyzeAdminToken, scopedToken)
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.tokenDecryptable == true
        entity.tokenValid == true
        entity.tokenRevoked == false
        entity.tokenExpired == false

        entity.token.token == scopedToken
        entity.token.type == "USER"
        entity.token.scope == SCOPE_SETUP_MFA

        entity.user.id == userAdmin.id
        entity.user.username == userAdmin.username
        entity.user.type == "PROVISIONED_USER"
        entity.user.domain == userAdmin.domainId
        entity.user.enabled == userAdmin.enabled
        entity.user.domainEnabled == true

        entity.trrs == []

        when: "subject token passed is revoked"
        utils.revokeToken(scopedToken)

        response = devops.analyzeToken(analyzeAdminToken, scopedToken)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.token.token == scopedToken

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == true
        entity.tokenExpired == false

        entity.trrs.size == 1

        cleanup:
        utils.deleteUser(identityAdmin)
        utils.deleteUser(userAdmin)
    }

    @Unroll
    def "test analyze token for an expired token"() {
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)
        def analyzeAdminToken = utils.getToken(identityAdmin.username)


        when: "valid x-auth-token and valid subject token is passed"
        def response = devops.analyzeToken(analyzeAdminToken, "AAD1PcpjE9sqhYK72IWvBfSowjgRuuzEcfhVtgz3QPoDdoPzIRiAMOZ_f37OeCU5LyrjBJU7INor9TZdDsFvIK-mOizUlRf0zDAbu1V-UKEBQtnzX7GsHiWF")
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).tokenAnalysis

        then:
        response.status == HttpStatus.SC_OK

        entity.tokenDecryptable == true
        entity.tokenValid == false
        entity.tokenRevoked == false
        entity.tokenExpired == true

        cleanup:
        utils.deleteUser(identityAdmin)
    }
}
