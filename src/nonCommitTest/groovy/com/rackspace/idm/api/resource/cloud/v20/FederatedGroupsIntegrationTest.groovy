package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainAuthRequest
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainRequestHandler
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.V2Factory
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator
import testHelpers.saml.v2.FederatedRackerAuthGenerationRequest
import testHelpers.saml.v2.FederatedRackerAuthRequestGenerator

class FederatedGroupsIntegrationTest extends RootIntegrationTest {

    @Autowired V2Factory v2Factory

    @Shared String sharedServiceAdminToken
    @Shared String sharedIdentityAdminToken

    @Shared String userAdminDomainId
    @Shared User userAdmin
    @Shared FederatedDomainAuthRequestGenerator v2authRequestGenerator

    @Shared IdentityProvider sharedBrokerIdp
    @Shared Credential sharedBrokerIdpCredential
    @Shared IdentityProvider sharedOriginIdp
    @Shared Credential sharedOriginIdpCredential

    void doSetupSpec() {
        sharedServiceAdminToken = cloud20.authenticateForToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        sharedIdentityAdminToken = cloud20.authenticateForToken(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)

        userAdminDomainId = RandomStringUtils.randomAlphanumeric(8)
        def userAdminData = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.email = "${RandomStringUtils.randomAlphanumeric(8)}@rackspace.com"
            it.enabled = true
            it.domainId = userAdminDomainId
            it.password = Constants.DEFAULT_PASSWORD
            it
        }
        userAdmin = cloud20.createUser(sharedIdentityAdminToken, userAdminData).getEntity(User).value

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedBrokerIdp = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential).getEntity(IdentityProvider)
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedOriginIdp = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential).getEntity(IdentityProvider)

        v2authRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
    }

    void doCleanupSpec() {
        cloud20.deleteUser(sharedServiceAdminToken, userAdmin.id)
        cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedBrokerIdp.id)
        cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedOriginIdp.id)
    }

    def "a federated user can be added to a user group"() {
        given:
        def federatedUserUsername = RandomStringUtils.randomAlphanumeric(8)
        def userGroup = utils.createUserGroup(userAdminDomainId)
        def tenantId = RandomStringUtils.randomAlphanumeric(8)
        def tenant = utils.createTenant(tenantId, true, RandomStringUtils.randomAlphanumeric(8), userAdminDomainId)
        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, [tenant.id]))

        when: "auth with the group"
        def v2AuthRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdminDomainId
            it.validitySeconds = 1000
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = "${RandomStringUtils.randomAlphanumeric(8)}@example.com"
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = federatedUserUsername
            it.groupNames = [userGroup.name]
            it
        }
        def samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        def federationResponse = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse))

        then: "successful auth"
        federationResponse.status == 200
        AuthenticateResponse authResp = federationResponse.getEntity(AuthenticateResponse).value

        and: "the role granted by the user group is returned in the auth response"
        authResp.user.roles.role.id.contains(Constants.ROLE_RBAC1_ID)

        when: "list users with the group"
        def listUsersInGroupResp = cloud20.getUsersInUserGroup(utils.getServiceAdminToken(), userAdminDomainId, userGroup.id)

        then: "the federated user is in the list of users in the user group"
        listUsersInGroupResp.status == 200
        UserList usersInGroup = listUsersInGroupResp.getEntity(UserList).value
        usersInGroup.user.id.contains(authResp.user.id)

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteTenant(tenant)
    }

    def "a federated user's user groups reflect the user groups provided in the last successful saml auth for that user"() {
        given:
        def federatedUserUsername = RandomStringUtils.randomAlphanumeric(8)
        def userGroup1 = utils.createUserGroup(userAdminDomainId)
        def userGroup2 = utils.createUserGroup(userAdminDomainId)
        def tenantId = RandomStringUtils.randomAlphanumeric(8)
        def tenant = utils.createTenant(tenantId, true, RandomStringUtils.randomAlphanumeric(8), userAdminDomainId)
        utils.grantRoleAssignmentsOnUserGroup(userGroup1, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, [tenant.id]))
        utils.grantRoleAssignmentsOnUserGroup(userGroup2, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, [tenant.id]))
        def v2AuthRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdminDomainId
            it.validitySeconds = 1000
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = "${RandomStringUtils.randomAlphanumeric(8)}@example.com"
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = federatedUserUsername
            it
        }

        when: "auth with the first user group"
        v2AuthRequest.groupNames = [userGroup1.name]
        def samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        def federationResponse = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse))

        then:
        federationResponse.status == 200
        AuthenticateResponse authResp = federationResponse.getEntity(AuthenticateResponse).value

        when: "list users with the first user group"
        UserList usersInUserGroup = cloud20.getUsersInUserGroup(utils.getServiceAdminToken(), userAdminDomainId, userGroup1.id).getEntity(UserList).value

        then:
        usersInUserGroup.user.id.contains(authResp.user.id)

        when: "list users with the second user group"
        usersInUserGroup = cloud20.getUsersInUserGroup(utils.getServiceAdminToken(), userAdminDomainId, userGroup2.id).getEntity(UserList).value

        then:
        !usersInUserGroup.user.id.contains(authResp.user.id)

        when: "auth with the second user group"
        v2AuthRequest.groupNames = [userGroup2.name]
        samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        federationResponse = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse))

        then:
        federationResponse.status == 200

        when: "list users with the first user group"
        usersInUserGroup = cloud20.getUsersInUserGroup(utils.getServiceAdminToken(), userAdminDomainId, userGroup1.id).getEntity(UserList).value

        then:
        !usersInUserGroup.user.id.contains(authResp.user.id)

        when: "list users with the second user group"
        usersInUserGroup = cloud20.getUsersInUserGroup(utils.getServiceAdminToken(), userAdminDomainId, userGroup2.id).getEntity(UserList).value

        then:
        usersInUserGroup.user.id.contains(authResp.user.id)

        when: "auth with no user groups"
        v2AuthRequest.groupNames = null
        samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        federationResponse = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse))

        then:
        federationResponse.status == 200

        when: "list users with the first user group"
        usersInUserGroup = cloud20.getUsersInUserGroup(utils.getServiceAdminToken(), userAdminDomainId, userGroup1.id).getEntity(UserList).value

        then:
        !usersInUserGroup.user.id.contains(authResp.user.id)

        when: "list users with the second user group"
        usersInUserGroup = cloud20.getUsersInUserGroup(utils.getServiceAdminToken(), userAdminDomainId, userGroup2.id).getEntity(UserList).value

        then:
        !usersInUserGroup.user.id.contains(authResp.user.id)

        cleanup:
        utils.deleteUserGroup(userGroup1)
        utils.deleteUserGroup(userGroup2)
        utils.deleteTenant(tenant)
    }

    def "federated domain user auth does not allow invalid user group names"() {
        given:
        def v2AuthRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdminDomainId
            it.validitySeconds = 1000
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = "${RandomStringUtils.randomAlphanumeric(8)}@example.com"
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it
        }

        when: "auth with an empty string user group"
        v2AuthRequest.groupNames = ["   "]
        def samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        def federationResponse = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(federationResponse, BadRequestFault, 400, ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_GROUP_ASSIGNMENT, String.format(FederatedDomainAuthRequest.INVALID_USER_GROUP_NAME_ERROR_MSG, "null")) // have to use "null" here b/c or xml mashaller converts the "  " value to "null"

        when: "auth with a user group that does not exist"
        v2AuthRequest.groupNames = [RandomStringUtils.randomAlphanumeric(8)]
        samlResponse = v2authRequestGenerator.createSignedSAMLResponse(v2AuthRequest)
        federationResponse = cloud20.federatedAuthenticateV2(v2authRequestGenerator.convertResponseToString(samlResponse))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(federationResponse, BadRequestFault, 400, ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_GROUP_ASSIGNMENT, String.format(FederatedDomainRequestHandler.INVALID_GROUP_FOR_DOMAIN_ERROR_MSG, v2AuthRequest.groupNames[0], v2AuthRequest.domainId))
    }

    def "federated racker auth ignores user groups"() {
        given:
        def rackerIdpCred = SamlCredentialUtils.generateX509Credential()
        def rackerIdp = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, IdentityProviderFederationTypeEnum.RACKER, rackerIdpCred).getEntity(IdentityProvider)
        def rackerAuthRequestGenerator = new FederatedRackerAuthRequestGenerator(sharedBrokerIdpCredential, rackerIdpCred)
        def fedRequest = new FederatedRackerAuthGenerationRequest().with {
            it.validitySeconds = 100
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = rackerIdp.issuer
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = Constants.RACKER_NOGROUP
            it.groupNames = [RandomStringUtils.randomAlphanumeric(8)]
            it
        }
        def samlResponse = rackerAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(rackerAuthRequestGenerator.convertResponseToString(samlResponse))

        then:
        authClientResponse.status == 200

        cleanup:
        utils.deleteIdentityProvider(rackerIdp)
    }

}
