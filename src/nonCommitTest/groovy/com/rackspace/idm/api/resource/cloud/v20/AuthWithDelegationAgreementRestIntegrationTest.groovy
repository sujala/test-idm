package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlAttributeFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.*

class AuthWithDelegationAgreementRestIntegrationTest extends RootIntegrationTest {

    @Shared def sharedServiceAdminToken
    @Shared def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared def sharedUserAdminToken

    @Shared User sharedSubUser
    @Shared def sharedSubUserToken

    @Shared User sharedUserAdmin2
    @Shared def sharedUserAdmin2Token

    @Shared User sharedSubUser2
    @Shared AuthenticateResponse sharedSubUser2AuthResponse
    @Shared def sharedSubUser2Token

    @Shared String commonRcn = "RCN-234-567-654"

    @Shared EndpointTemplate endpointTemplate

    @Autowired
    AETokenService aeTokenService

    @Autowired
    IdentityUserService identityUserService

    @Shared FederatedDomainAuthRequestGenerator federatedDomainAuthRequestGenerator

    @Shared String serviceAdminToken

    def setupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedServiceAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Create a cloud account
        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        authResponse = cloud20.authenticatePassword(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedUserAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedSubUser = cloud20.createSubUser(sharedUserAdminToken)
        authResponse = cloud20.authenticatePassword(sharedSubUser.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedSubUserToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Create a second cloud account
        sharedUserAdmin2 = cloud20.createCloudAccount(sharedIdentityAdminToken)

        authResponse = cloud20.authenticatePassword(sharedUserAdmin2.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedUserAdmin2Token = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedSubUser2 = cloud20.createSubUser(sharedUserAdmin2Token)
        authResponse = cloud20.authenticatePassword(sharedSubUser2.username, Constants.DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedSubUser2AuthResponse = authResponse.getEntity(AuthenticateResponse).value
        sharedSubUser2Token = sharedSubUser2AuthResponse.token.id

        // Update domains to have same RCN
        def rcnSwitchResponse = cloud20.domainRcnSwitch(sharedServiceAdminToken, sharedUserAdmin.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(sharedServiceAdminToken, sharedUserAdmin2.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)

        // Created v2 fed request generator for test. User groups can only be added via v2 IDPs
        federatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(Constants.DEFAULT_BROKER_IDP_PUBLIC_KEY, Constants.DEFAULT_BROKER_IDP_PRIVATE_KEY, Constants.IDP_V2_DOMAIN_PUBLIC_KEY, Constants.IDP_V2_DOMAIN_PRIVATE_KEY)
    }

    /**
     * By default for these tests open up DAs to all RCNs. Tests that verify limiting the availability will need to
     * reset these properties.
     *
     * @return
     */
    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AUTHENTICATION_PROP, true)
    }

    /**
     * Verifies the delegation services accessibility is controlled via the high level feature flag. Uses as invalid
     * token to verify. If the services are disabled, the token will never be checked and a 503 will
     * be immediately returned. If the services are enabled, the token will be found invalid and a 401 will be returned.
     */
    @Unroll
    def "Auth with da requires auth with DA to be enabled: enabled: #enableServices"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AUTHENTICATION_PROP, enableServices)

        when: "Auth"
        def response = cloud20.authenticateTokenAndDelegationAgreement("invalid", "invalid")

        then:
        if (enableServices) {
            assert response.status == SC_UNAUTHORIZED
        } else {
            assert response.status == SC_SERVICE_UNAVAILABLE
        }

        where:
        enableServices << [true, false]
    }

    @Unroll
    def "Error: Auth with da using undecryptable token: mediaType: #mediaType"() {
        when: "Auth"
        def response = cloud20.authenticateTokenAndDelegationAgreement("invalid", "invalid")

        then:
        assert response.status == SC_UNAUTHORIZED
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, SC_UNAUTHORIZED, AuthWithDelegationCredentials.ERROR_MSG_INVALID_TOKEN)

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }

    @Unroll
    def "Error: Auth with da using expired token: mediaType: #mediaType"() {
        def sharedAdminEntity = identityUserService.getProvisionedUserById(sharedUserAdmin.id)
        ScopeAccess sa = aeTokenService.unmarshallToken(sharedUserAdminToken)
        sa.setAccessTokenExp(new DateTime().minusDays(1).toDate())
        def expiredToken = aeTokenService.marshallTokenForUser(sharedAdminEntity, sa)

        when: "Auth"
        def response = cloud20.authenticateTokenAndDelegationAgreement(expiredToken, "invalid")

        then:
        assert response.status == SC_UNAUTHORIZED
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, SC_UNAUTHORIZED, AuthWithDelegationCredentials.ERROR_MSG_INVALID_TOKEN)

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }

    @Unroll
    def "Error: Auth with da using scoped token: mediaType: #mediaType"() {
        def sharedAdminEntity = identityUserService.getProvisionedUserById(sharedUserAdmin.id)
        ScopeAccess sa = aeTokenService.unmarshallToken(sharedUserAdminToken)
        sa.setScope(TokenScopeEnum.MFA_SESSION_ID.scope)
        def scopedToken = aeTokenService.marshallTokenForUser(sharedAdminEntity, sa)

        when: "Auth"
        def response = cloud20.authenticateTokenAndDelegationAgreement(scopedToken, "invalid")

        then:
        assert response.status == SC_FORBIDDEN
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }

    def "Error: Auth with da using racker token"() {
        def token = utils.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD).token.id

        when: "Auth with racker"
        def response = cloud20.authenticateTokenAndDelegationAgreement(token, "invalid")

        then:
        assert response.status == SC_FORBIDDEN
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, AuthWithDelegationCredentials.ERROR_MSG_INVALID_TOKEN)
    }

    def "Error: Auth with da using impersonation token"() {
        def token = utils.impersonateWithToken(sharedIdentityAdminToken, sharedUserAdmin).token.id

        when: "Auth with impersonation token"
        def response = cloud20.authenticateTokenAndDelegationAgreement(token, "invalid")

        then:
        assert response.status == SC_FORBIDDEN
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, AuthWithDelegationCredentials.ERROR_MSG_INVALID_TOKEN)
    }

    /**
     *  A vanilla DA with no roles assigned causes a delegate to have same roles and service catalog as a
     *  vanilla subuser within the domain for which apply_rcn_roles was applied.
     */
    @Unroll
    def "Valid user delegate auth with da receives token: feature.enable.user.admin.look.up.by.domain = #featureEnabled, mediaType = #mediaType"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, featureEnabled)
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }

        // Give subuser2 access to same domain as subuser
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser2.id)

        // Auth as regular subuser under the domain
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, sharedSubUser2AuthResponse.user, da)

        cleanup:
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)
        reloadableConfiguration.reset()

        where:
        featureEnabled | mediaType
        true           | MediaType.APPLICATION_XML_TYPE
        false          | MediaType.APPLICATION_XML_TYPE
        true           | MediaType.APPLICATION_JSON_TYPE
        false          | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Verify delegate auth returns appropriate DA assigned roles for each possible delegate type, mediaType = #mediaType"() {
        UserGroup domain2UserGroup = utils.createUserGroup(sharedSubUser2.domainId)

        // Create 2 fed users through v2; one of which is member of group
        AuthenticateResponse fedUserAuthResponse = authBasicFedUser()
        AuthenticateResponse fedUserAuthResponseGroupMember = authBasicFedUser([domain2UserGroup.getName()] as Set)

        // Auth as regular subuser under the domain
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        def mossoTenantId = sharedUserAdmin.domainId
        def nastTenantId = realSubUserAuthResponse.user.roles.role.find {it.name == DEFAULT_OBJECT_STORE_ROLE}.tenantId

        // Create a DA with subuser2, fed user, and user group as delegates
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser2.id)
        utils.addUserDelegate(sharedUserAdminToken, da.id, fedUserAuthResponse.user.id)
        utils.addUserGroupDelegate(sharedUserAdminToken, da.id, domain2UserGroup.id)

        // Grant the roles to the DA
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [mossoTenantId])) // mosso tenant
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(da, assignments, sharedUserAdminToken)

        when: "Auth as user delegate"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, sharedSubUser2AuthResponse.user, da)

        and: "user received DA role 1 on all tenants"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == nastTenantId} != null

        and: "user received DA role 2 on mosso tenant only"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == nastTenantId} == null

        when: "Auth as fed user delegate"
        delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponse.token.id, da.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, fedUserAuthResponse.user, da)

        and: "user received DA role 1 on all tenants"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == nastTenantId} != null

        and: "user received DA role 2 on mosso tenant only"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == nastTenantId} == null

        when: "Auth as user group member delegate"
        delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponseGroupMember.token.id, da.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, fedUserAuthResponseGroupMember.user, da)

        and: "user received DA role 1 on all tenants"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == nastTenantId} != null

        and: "user received DA role 2 on mosso tenant only"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == nastTenantId} == null

        cleanup:
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Verify delegate auth returns appropriate DA assigned roles when all DA assigned roles are domain roles, mediaType = #mediaType"() {
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }

        // Auth as regular subuser under the domain
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        def mossoTenantId = sharedUserAdmin.domainId
        def nastTenantId = realSubUserAuthResponse.user.roles.role.find {it.name == DEFAULT_OBJECT_STORE_ROLE}.tenantId

        // Give subuser2 access to same domain as subuser
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser2.id)
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(da, assignments, sharedUserAdminToken)

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, sharedSubUser2AuthResponse.user, da)

        and: "user received DA role 1 on all tenants"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == nastTenantId} != null

        and: "user received DA role 2 on all tenants"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == mossoTenantId} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == nastTenantId} != null

        cleanup:
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Verify delegate auth returns roles on hidden tenants appropriately"() {
        // Create 2 domains in same RCN
        def ua1 = utils.createCloudAccount()
        def ua2 = utils.createGenericUserAdmin()
        utils.domainRcnSwitch(ua1.domainId, commonRcn)
        utils.domainRcnSwitch(ua2.domainId, commonRcn)

        // Create 3 faws tenants in d1
        def faws1 = createFawsTenant(ua1.domainId)
        def faws2 = createFawsTenant(ua1.domainId)
        def faws3 = createFawsTenant(ua1.domainId)

        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }

        def ua1Token = utils.getToken(ua1.username)

        // Auth as ua2
        AuthenticateResponse ua2AuthResponse = utils.authenticate(ua2)

        // Give ua2 access to d1
        def da = utils.createDelegationAgreement(ua1Token, daToCreate)
        utils.addUserDelegate(ua1Token, da.id, ua2.id)
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [faws2.id, faws3.id]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(da, assignments, ua1Token)

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(ua2AuthResponse.token.id, da.id)

        then: "Delegate gets role 1 on faws2 and faw3, but not faws1"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == faws1.id} == null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == faws2.id} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == faws3.id} != null

        and: "Delegate gets role 2 on faws2 and faw3, but not faws1"
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == faws1.id} == null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == faws2.id} != null
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == faws3.id} != null

        cleanup:
        utils.deleteDelegationAgreement(ua1Token, da)
    }

    @Unroll
    def "Verify delegate auth does not return any roles if the DA domain does not contain tenants; mediaType=#mediaType"() {
        // Create 2 domains in same RCN without any tenants
        def ua1 = utils.createGenericUserAdmin()
        def ua2 = utils.createGenericUserAdmin()
        utils.domainRcnSwitch(ua1.domainId, commonRcn)
        utils.domainRcnSwitch(ua2.domainId, commonRcn)

        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }

        def ua1Token = utils.getToken(ua1.username)

        // Auth as ua2
        AuthenticateResponse ua2AuthResponse = utils.authenticate(ua2)

        // Give ua2 access to d1
        def da = utils.createDelegationAgreement(ua1Token, daToCreate)
        utils.addUserDelegate(ua1Token, da.id, ua2.id)
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(da, assignments, ua1Token)

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(ua2AuthResponse.token.id, da.id, mediaType)

        then: "Delegate receives no roles"
        // Existing auth code has discrepancy where xml returns wrapper object, but json doesn't. Must account
        // for this in test
        if (mediaType == MediaType.APPLICATION_XML_TYPE) {
            assert delegateAuthResponse.user.roles.role.size() == 0
        } else {
            assert delegateAuthResponse.user.roles == null
        }

        cleanup:
        utils.deleteDelegationAgreement(ua1Token, da)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Valid user group delegate auth with da receives token"() {
        // Create a user group in domain 2 and add user to it
        UserGroup domain2UserGroup = utils.createUserGroup(sharedSubUser2.domainId)
        utils.addUserToUserGroup(sharedSubUser2.id, domain2UserGroup)

        // Create a DA with domain1 useradmin as principal and add domain2 user as delegate
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert cloud20.addUserGroupDelegate(sharedUserAdminToken, da.id, domain2UserGroup.id).status == SC_NO_CONTENT

        // Auth as regular subuser under the domain1 w/ applyrcnrole logic
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        when: "Auth as delegate from domain2 under the domain1"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, sharedSubUser2AuthResponse.user, da)

        cleanup:
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)
        reloadableConfiguration.reset()
    }

    def "Valid fed user created via v1.0 Fed API can auth under DA when explicitly listed as delegate "() {
        // Create a fed user under common global IDP
        def attributes = SamlAttributeFactory.createAttributes(sharedUserAdmin2.domainId, [], Constants.DEFAULT_FED_EMAIL, [])
        AuthenticateResponse fedUserAuthResponse = utils.authenticateV1FederatedUser(attributes, true)

        // Create a DA w/ fed user as delegate
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert cloud20.addUserDelegate(sharedUserAdminToken, da.id, fedUserAuthResponse.user.id).status == SC_NO_CONTENT

        // Auth as regular subuser under the domain1 w/ applyrcnrole logic
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        when: "Auth as fed delegate from domain2 under domain1 DA"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponse.token.id, da.id)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, fedUserAuthResponse.user, da)

        cleanup:
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)
        reloadableConfiguration.reset()
    }

    /**
     * Fed 1.0 API does NOT support setting user groups. Because of this, fed users created under v1 API can not delegate
     * under a DA for which they are an effective delegate only due to user group membership
     * @return
     */
    def "Valid fed user created via v1.0 Fed API can not auth under DA when member of user group that is a delegate"() {
        // Create a user group in domain 2
        UserGroup domain2UserGroup = utils.createUserGroup(sharedSubUser2.domainId)

        // Create a fed user under common global IDP and add to group
        def attributes = SamlAttributeFactory.createAttributes(sharedUserAdmin2.domainId, [], Constants.DEFAULT_FED_EMAIL, [domain2UserGroup.getName()])
        AuthenticateResponse fedUserAuthResponse = utils.authenticateV1FederatedUser(attributes, true)

        // Create a DA w/ user group as delegate
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert cloud20.addUserGroupDelegate(sharedUserAdminToken, da.id, domain2UserGroup.id).status == SC_NO_CONTENT

        when: "Auth as fed delegate from domain2 under the domain1 DA"
        def delegateAuthResponse = cloud20.authenticateTokenAndDelegationAgreement(fedUserAuthResponse.token.id, da.id)

        then: "resultant info is appropriate"
        IdmAssert.assertOpenStackV2FaultResponse(delegateAuthResponse, ItemNotFoundFault, SC_NOT_FOUND, null, AuthWithDelegationCredentials.ERROR_MSG_MISSING_AGREEMENT)

        cleanup:
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)
    }

    def "v2.0 API Fed user that is member of delegate user group can auth under DA"() {
        // Create a user group in domain 2
        UserGroup domain2UserGroup = utils.createUserGroup(sharedSubUser2.domainId)

        // Create a fed user through v2 that adds user to group
        AuthenticateResponse fedUserAuthResponse =authBasicFedUser([domain2UserGroup.getName()] as Set)

        // Create a DA w/ user group as delegate
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert cloud20.addUserGroupDelegate(sharedUserAdminToken, da.id, domain2UserGroup.id).status == SC_NO_CONTENT

        // Auth as regular subuser under the domain1 w/ applyrcnrole logic
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        when: "Auth as fed delegate from domain2 under the domain1 DA"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponse.token.id, da.id)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, fedUserAuthResponse.user, da)

        cleanup:
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)
    }

    def "auth with nested DA is allowed for enabled users"() {
        given:
        def daData = new DelegationAgreement().with {
            it.subAgreementNestLevel = 1
            it.name = "DA for domain ${sharedUserAdmin.domainId}"
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daData)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser.id)
        def nestedDaData = new DelegationAgreement().with {
            it.parentDelegationAgreementId = da.id
            it.name = "Nested DA for domain ${sharedUserAdmin.domainId}"
            it
        }
        def nestedDa = utils.createDelegationAgreement(sharedSubUserToken, nestedDaData)
        def defaultUser = utils.createUser(sharedUserAdminToken)
        utils.addUserDelegate(sharedUserAdminToken, nestedDa.id, defaultUser.id)
        def defaultUserToken = utils.getToken(defaultUser.username)
        def userManager = utils.createUser(sharedUserAdminToken)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID)
        utils.addUserDelegate(sharedUserAdminToken, nestedDa.id, userManager.id)
        def userManagerToken = utils.getToken(userManager.username)
        def fedUserAuthResponse = utils.authenticateFederatedUser(sharedUserAdmin.domainId)
        def fedUser = fedUserAuthResponse.user
        utils.addUserDelegate(sharedUserAdminToken, nestedDa.id, fedUser.id)
        def fedUserToken = fedUserAuthResponse.token.id
        def userGroup = utils.createUserGroup(sharedUserAdmin.domainId)
        def userInUserGroup = utils.createUser(sharedUserAdminToken)
        utils.addUserToUserGroup(userInUserGroup.id, userGroup)
        utils.addUserGroupDelegate(sharedUserAdminToken, nestedDa.id, userGroup.id)
        def userInUserGroupToken = utils.getToken(userInUserGroup.username)
        def tokensInDa = [defaultUserToken, userManagerToken, fedUserToken, userInUserGroupToken]

        when: "auth with DA"
        def authResponses = []
        tokensInDa.each { token ->
            authResponses << cloud20.authenticateTokenAndDelegationAgreement(token, nestedDa.id)
        }

        then:
        authResponses.each { authResponse ->
            assert authResponse.status == 200
        }
    }

    def "auth with nested DA is rejected for disabled users"() {
        given:
        def daData = new DelegationAgreement().with {
            it.subAgreementNestLevel = 1
            it.name = "DA for domain ${sharedUserAdmin.domainId}"
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daData)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser.id)
        def nestedDaData = new DelegationAgreement().with {
            it.parentDelegationAgreementId = da.id
            it.name = "Nested DA for domain ${sharedUserAdmin.domainId}"
            it
        }
        def nestedDa = utils.createDelegationAgreement(sharedSubUserToken, nestedDaData)
        def defaultUser = utils.createUser(sharedUserAdminToken)
        utils.addUserDelegate(sharedUserAdminToken, nestedDa.id, defaultUser.id)
        def defaultUserToken = utils.getToken(defaultUser.username)
        def userManager = utils.createUser(sharedUserAdminToken)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID)
        utils.addUserDelegate(sharedUserAdminToken, nestedDa.id, userManager.id)
        def userManagerToken = utils.getToken(userManager.username)
        def userGroup = utils.createUserGroup(sharedUserAdmin.domainId)
        def userInUserGroup = utils.createUser(sharedUserAdminToken)
        utils.addUserToUserGroup(userInUserGroup.id, userGroup)
        utils.addUserGroupDelegate(sharedUserAdminToken, nestedDa.id, userGroup.id)
        def userInUserGroupToken = utils.getToken(userInUserGroup.username)
        def usersAndTokensInDa = [[defaultUser, defaultUserToken], [userManager, userManagerToken], [userInUserGroup, userInUserGroupToken]]

        when: "auth with DA"
        def authResponses = []
        usersAndTokensInDa.each { userAndToken ->
            authResponses << cloud20.authenticateTokenAndDelegationAgreement(userAndToken[1], nestedDa.id)
        }

        then: "the users are able to auth with DA"
        authResponses.each { authResponse ->
            assert authResponse.status == 200
        }

        when: "disable the user and auth with DA again"
        authResponses = []
        usersAndTokensInDa.each { userAndToken ->
            utils.disableUser(userAndToken[0])
            authResponses << cloud20.authenticateTokenAndDelegationAgreement(userAndToken[1], nestedDa.id)
        }

        then: "the users are no longer able to auth with DA"
        authResponses.each { authResponse ->
            assert authResponse.status == 401
        }
    }

    def "auth with nested DA is allowed for DAs when the parent DA principal is disabled"() {
        given:
        def rcn = "RCN-${RandomStringUtils.randomNumeric(3)}-${RandomStringUtils.randomNumeric(3)}-${RandomStringUtils.randomNumeric(3)}"
        def userAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(userAdmin.domainId, rcn)
        def userAdminToken = utils.getToken(userAdmin.username)
        def rcnAdmin = utils.createUser(userAdminToken)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdmin.username)
        def subUser = utils.createUser(userAdminToken)
        def subUserToken = utils.getToken(subUser.username)
        def daData = new DelegationAgreement().with {
            it.subAgreementNestLevel = 1
            it.name = "DA for domain ${userAdmin.domainId}"
            it
        }
        def da = utils.createDelegationAgreement(rcnAdminToken, daData)
        utils.addUserDelegate(rcnAdminToken, da.id, subUser.id)
        def nestedDaData = new DelegationAgreement().with {
            it.parentDelegationAgreementId = da.id
            it.name = "Nested DA for domain ${userAdmin.domainId}"
            it
        }
        def nestedDa = utils.createDelegationAgreement(subUserToken, nestedDaData)
        def otherSubUser = utils.createUser(userAdminToken)
        def otherSubUserToken = utils.getToken(otherSubUser.username)
        utils.addUserDelegate(userAdminToken, nestedDa.id, otherSubUser.id)

        when: "auth with nested DA w/ enabled principal of parent DA"
        def authResponse = cloud20.authenticateTokenAndDelegationAgreement(otherSubUserToken, nestedDa.id)

        then:
        authResponse.status == 200

        when: "auth with nested DA w/ disabled principal of parent DA"
        utils.disableUser(rcnAdmin)
        authResponse = cloud20.authenticateTokenAndDelegationAgreement(otherSubUserToken, nestedDa.id)

        then:
        authResponse.status == 200
    }

    void assertDelegateAuthSameAsSubuser(AuthenticateResponse delegateAuthResponse, AuthenticateResponse realSubUserAuthResponse, def delegateUser, DelegationAgreement da) {
        // Token info same (though not id..)
        assert delegateAuthResponse.token.tenant.name == realSubUserAuthResponse.token.tenant.name
        assert delegateAuthResponse.token.tenant.id == realSubUserAuthResponse.token.tenant.id
        assert delegateAuthResponse.token.id != realSubUserAuthResponse.token.tenant.id

        // Token reflects delegate authentication
        assert delegateAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())
        assert delegateAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.DELEGATE.getValue())

        // Domain based user info is same
        assert delegateAuthResponse.user.domainId == realSubUserAuthResponse.user.domainId
        assert delegateAuthResponse.user.defaultRegion == realSubUserAuthResponse.user.defaultRegion
        assert delegateAuthResponse.user.sessionInactivityTimeout == realSubUserAuthResponse.user.sessionInactivityTimeout

        // Delegate has all the same roles as the subuser (may have extra based on DA roles assigned)
        assert delegateAuthResponse.user.roles.role.size() >= realSubUserAuthResponse.user.roles.role.size()
        realSubUserAuthResponse.user.roles.role.each { subuserRoleId ->
            def matchingDelegateRole = delegateAuthResponse.user.roles.role.find {it.id == subuserRoleId.id && it.tenantId == subuserRoleId.tenantId}
            assert matchingDelegateRole != null
            assert subuserRoleId.name == matchingDelegateRole.name
        }

        // User identifying information reflects user authenticating
        assert delegateAuthResponse.user.id == delegateUser.id // User reflects the delegate
        assert delegateAuthResponse.user.name == delegateUser.name // User reflects the delegate
        if (da != null) {
            assert delegateAuthResponse.user.delegationAgreementId == da.id
        }

        // service catalogs are the same
        delegateAuthResponse.serviceCatalog.service.size() == realSubUserAuthResponse.serviceCatalog.service.size()
        delegateAuthResponse.serviceCatalog.service.each {dServiceForCatalog ->
            ServiceForCatalog matchingSubUserServiceForCatalog = realSubUserAuthResponse.serviceCatalog.service.find {it.name == dServiceForCatalog.name}
            assert matchingSubUserServiceForCatalog != null

            // now iterate through endpoints
            assert dServiceForCatalog.endpoint.size() == matchingSubUserServiceForCatalog.endpoint.size()
            dServiceForCatalog.endpoint.each {dEndpointForService ->
                EndpointForService matchingSubUserEndpointForService = matchingSubUserServiceForCatalog.endpoint.find {
                    it.tenantId == dEndpointForService.tenantId && it.publicURL == dEndpointForService.publicURL && it.adminURL == dEndpointForService.adminURL && it.region == dEndpointForService.region
                }
                assert matchingSubUserEndpointForService != null
            }
        }
    }

    TenantAssignment createTenantAssignment(String roleId, List<String> tenants) {
        def assignment = new TenantAssignment().with {
            ta ->
                ta.onRole = roleId
                ta.forTenants.addAll(tenants)
                ta
        }
        return assignment
    }

    AuthenticateResponse authBasicFedUser(Set<String> groupNames = [] as Set) {
        def samlRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = sharedUserAdmin2.domainId
            it.validitySeconds = 100
            it.brokerIssuer = Constants.DEFAULT_BROKER_IDP_URI
            it.originIssuer = Constants.IDP_V2_DOMAIN_URI
            it.email = Constants.DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = UUID.randomUUID().toString()
            it.roleNames = [] as Set
            it.groupNames = groupNames
            it
        }

        return utils.authenticateV2FederatedUser(samlRequest)
    }

    // created "hidden" tenant in account (faws is configured as such)
    Tenant createFawsTenant(String domainId) {
        def tenantResponse = cloud20.addTenant(sharedIdentityAdminToken, new Tenant().with {
            it.name = Constants.TENANT_TYPE_FAWS + ":" + RandomStringUtils.randomAlphabetic(5)
            it.id = it.name
            it.domainId = domainId
            it
        })
        utils.createTenant()

        assert tenantResponse.status == SC_CREATED
        return tenantResponse.getEntity(Tenant).value
    }
}