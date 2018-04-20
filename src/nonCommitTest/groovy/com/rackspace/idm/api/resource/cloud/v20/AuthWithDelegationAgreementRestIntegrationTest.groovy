package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.collections4.CollectionUtils
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

import static com.rackspace.idm.Constants.ROLE_RBAC1_ID
import static com.rackspace.idm.Constants.ROLE_RBAC2_ID
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

        // created "hidden" tenant in account (faws is configured as such)
        def tenantResponse = cloud20.addTenant(sharedIdentityAdminToken, new Tenant().with {
            it.name = Constants.TENANT_TYPE_FAWS + ":" + RandomStringUtils.randomAlphabetic(5)
            it.id = it.name
            it.domainId = sharedUserAdmin.domainId
            it
        })

        assert tenantResponse.status == SC_CREATED

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
        sharedSubUser2Token = authResponse.getEntity(AuthenticateResponse).value.token.id

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
    }

    /**
     * Verifies the delegation services accessibility is controlled via the high level feature flag. Uses as invalid
     * token to verify. If the services are disabled, the token will never be checked and a 503 will
     * be immediately returned. If the services are enabled, the token will be found invalid and a 401 will be returned.
     */
    @Unroll
    def "Auth with da requires DA services to be enabled: enabled: #enableServices"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, enableServices)

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
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, sharedSubUser2, da)

        cleanup:
        reloadableConfiguration.reset()

        where:
        featureEnabled | mediaType
        true           | MediaType.APPLICATION_XML_TYPE
        false          | MediaType.APPLICATION_XML_TYPE
        true           | MediaType.APPLICATION_JSON_TYPE
        false          | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Valid user delegate auth with da including roles receives da roles, mediaType = #mediaType"() {
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }

        // Give subuser2 access to same domain as subuser
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser2.id)
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(da, assignments, sharedUserAdminToken)

        // Auth as regular subuser under the domain
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, sharedSubUser2, da)

        and: "user received DA roles"
        // Assigned on MOSSO tenant
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == da.domainId} != null
        // Assigned on NAST tenant
        delegateAuthResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId != da.domainId} != null

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
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, sharedSubUser2, da)

        cleanup:
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

        def adapterForFedUser = new User().with {
            it.id = fedUserAuthResponse.user.id
            it.username = fedUserAuthResponse.user.name
            it
        }

        when: "Auth as fed delegate from domain2 under domain1 DA"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponse.token.id, da.id)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, adapterForFedUser, da)

        cleanup:
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
    }

    def "v2.0 API Fed user that is member of delegate user group can auth under DA"() {
        // Create a user group in domain 2
        UserGroup domain2UserGroup = utils.createUserGroup(sharedSubUser2.domainId)

        def samlRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = sharedUserAdmin2.domainId
            it.validitySeconds = 100
            it.brokerIssuer = Constants.DEFAULT_BROKER_IDP_URI
            it.originIssuer = Constants.IDP_V2_DOMAIN_URI
            it.email = Constants.DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = UUID.randomUUID().toString()
            it.roleNames = [] as Set
            it.groupNames = [domain2UserGroup.getName()] as Set
            it
        }

        // Create a fed user through v2 that adds user to group
        AuthenticateResponse fedUserAuthResponse = utils.authenticateV2FederatedUser(samlRequest)

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

        def adapterForFedUser = new User().with {
            it.id = fedUserAuthResponse.user.id
            it.username = fedUserAuthResponse.user.name
            it
        }

        when: "Auth as fed delegate from domain2 under the domain1 DA"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponse.token.id, da.id)

        then: "resultant info is appropriate"
        assertDelegateAuthSameAsSubuser(delegateAuthResponse, realSubUserAuthResponse, adapterForFedUser, da)
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
        assert delegateAuthResponse.user.name == delegateUser.username // User reflects the delegate
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
}