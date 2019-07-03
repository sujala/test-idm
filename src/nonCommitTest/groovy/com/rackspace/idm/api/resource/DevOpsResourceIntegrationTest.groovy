package com.rackspace.idm.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.joda.time.DateTime
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
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import static com.rackspace.idm.Constants.DEFAULT_BROKER_IDP_URI
import static com.rackspace.idm.Constants.IDP_V2_DOMAIN_ID
import static com.rackspace.idm.Constants.IDP_V2_DOMAIN_URI
import static com.rackspace.idm.Constants.IDP_V2_RACKER_PRIVATE_KEY
import static com.rackspace.idm.Constants.IDP_V2_RACKER_PUBLIC_KEY
import static com.rackspace.idm.Constants.IDP_V2_RACKER_URI
import static com.rackspace.idm.Constants.RACKER
import static com.rackspace.idm.Constants.getDEFAULT_BROKER_IDP_PRIVATE_KEY
import static com.rackspace.idm.Constants.getDEFAULT_BROKER_IDP_PUBLIC_KEY
import static com.rackspace.idm.Constants.getDEFAULT_BROKER_IDP_URI
import static com.rackspace.idm.Constants.getDEFAULT_PASSWORD
import static com.rackspace.idm.Constants.getIDP_V2_DOMAIN_PRIVATE_KEY
import static com.rackspace.idm.Constants.getIDP_V2_DOMAIN_PUBLIC_KEY
import static com.rackspace.idm.Constants.getIDP_V2_DOMAIN_URI
import static com.rackspace.idm.Constants.getSCOPE_SETUP_MFA

class DevOpsResourceIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    UserDao userRepository

    @Autowired
    DomainService domainService

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

        def user = utils.createUserAdminWithoutIdentityAdmin()
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

        def userAdmin = utils.createCloudAccount()
        def federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_DOMAIN_PUBLIC_KEY, IDP_V2_DOMAIN_PRIVATE_KEY)
        def fedRequest = utils.createFedRequest(userAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def federationResponse = samlResponse.getEntity(AuthenticateResponse).value
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
        entity.user.domain == userAdmin.domainId
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
        entity.impersonatedUser.domain == userAdmin.domainId
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
        utils.deleteFederatedUserQuietly(fedRequest.username)
    }

    @Unroll
    def "test analyze token by disabling a federated user"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)
        def brokerCred = SamlCredentialUtils.generateX509Credential()
        def brokerIdp = utils.createIdentityProviderWithCred(utils.identityAdminToken, IdentityProviderFederationTypeEnum.BROKER, brokerCred)
        def originCred = SamlCredentialUtils.generateX509Credential()
        def originIdp = utils.createIdentityProviderWithCred(utils.identityAdminToken, IdentityProviderFederationTypeEnum.DOMAIN, originCred)

        def userAdmin = utils.createCloudAccount()
        def federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(brokerCred, originCred)
        def fedRequest = utils.createFedRequest(userAdmin, brokerIdp.issuer, originIdp.issuer)
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def federationResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedUser = utils.getUserById(federationResponse.user.id)
        def federatedUserToken = federationResponse.token.id

        def idpRequest = new IdentityProvider().with {
            it.enabled = false
            it
        }
        cloud20.updateIdentityProvider(utils.getServiceAdminToken(), originIdp.id, idpRequest)

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
        entity.user.domain == userAdmin.domainId
        entity.user.enabled == true
        entity.user.domainEnabled == true

        entity.trrs.size == 1
        entity.trrs[0].identityProviderId == originIdp.id

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUser(identityAdmin)
        utils.deleteFederatedUserQuietly(fedRequest.username, originIdp.id)
        utils.deleteIdentityProviderQuietly(utils.serviceAdminToken, originIdp.id)
        utils.deleteIdentityProviderQuietly(utils.serviceAdminToken, brokerIdp.id)
    }

    @Unroll
    def "test analyze token for a federated racker token"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def analyzeAdminToken = utils.getToken(identityAdmin.username)
        utils.addRoleToUser(identityAdmin, Constants.IDENTITY_ANALYZE_TOKEN_ROLE_ID)

        def federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, IDP_V2_RACKER_PUBLIC_KEY, IDP_V2_RACKER_PRIVATE_KEY)
        def fedRequest = new FederatedDomainAuthGenerationRequest().with {
            it.validitySeconds = 100
            it.brokerIssuer = DEFAULT_BROKER_IDP_URI
            it.originIssuer = IDP_V2_RACKER_URI
            it.email = Constants.DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = RACKER
            it.roleNames = [] as Set
            it
        }
        def samlAssertion = federatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)
        def samlResponse = cloud20.samlAuthenticate(federatedDomainAuthRequestGenerator.convertResponseToString(samlAssertion))
        def federationResponse = samlResponse.getEntity(AuthenticateResponse).value
        def federatedRackerToken = federationResponse.token.id

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

        entity.user.username == federationResponse.user.name
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

    def "migrateDomainAdmin: test domain's user-admin gets successfully migrated to domain"() {
        given:
        def caller = utils.createCloudAccount()
        utils.addRoleToUser(caller, Constants.IDENTITY_MIGRATE_DOMAIN_ADMIN)

        when: "domain with no user-admins"
        def domain = utils.createDomainEntity()
        def response = devops.migrateDomainAdmin(utils.getToken(caller.username), domain.id)
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).domain

        then:
        response.status == HttpStatus.SC_OK

        entity.id == domain.id
        entity.userAdminDN == ""
        entity.previousUserAdminDN == ""

        when: "domain with user-admin"
        def userAdmin = utils.createCloudAccount()
        def userEntity = userRepository.getUserById(userAdmin.id)
        domainService.removeDomainUserAdminDN(userEntity)

        response = devops.migrateDomainAdmin(utils.getToken(caller.username), userAdmin.domainId)
        entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).domain

        then:
        response.status == HttpStatus.SC_OK

        entity.id == userAdmin.domainId
        entity.userAdminDN == userEntity.getDn().toString()
        entity.previousUserAdminDN == ""


        cleanup:
        utils.deleteUserQuietly(caller)
        utils.deleteUserQuietly(userAdmin)
    }

    def "migrateDomainAdmin: error check"() {
        given:
        def caller = utils.createCloudAccount()
        utils.addRoleToUser(caller, Constants.IDENTITY_MIGRATE_DOMAIN_ADMIN)
        def domain = utils.createDomainEntity()

        when: "domain does not exist"
        def response = devops.migrateDomainAdmin(utils.getToken(caller.username), "invalid")

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        when: "invalid token"
        response = devops.migrateDomainAdmin("invalid", domain.id)

        then:
        response.status == HttpStatus.SC_UNAUTHORIZED

        when: "forbidden token"
        response = devops.migrateDomainAdmin(utils.getServiceAdminToken(), domain.id)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        cleanup:
        utils.deleteUserQuietly(caller)
    }
}
