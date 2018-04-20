package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.*

class ValidateDelegateTokenRestIntegrationTest extends RootIntegrationTest {

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
    IdentityUserService identityUserService;

    def setupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)

        def authResponse = cloud20.authenticatePassword(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedServiceAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        authResponse = cloud20.authenticatePassword(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Create a cloud account
        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        authResponse = cloud20.authenticatePassword(sharedUserAdmin.username, DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedUserAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedSubUser = cloud20.createSubUser(sharedUserAdminToken)
        authResponse = cloud20.authenticatePassword(sharedSubUser.username, DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedSubUserToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Create a second cloud account
        sharedUserAdmin2 = cloud20.createCloudAccount(sharedIdentityAdminToken)

        authResponse = cloud20.authenticatePassword(sharedUserAdmin2.username, DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedUserAdmin2Token = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedSubUser2 = cloud20.createSubUser(sharedUserAdmin2Token)
        authResponse = cloud20.authenticatePassword(sharedSubUser2.username, DEFAULT_PASSWORD)
        assert authResponse.status == SC_OK
        sharedSubUser2Token = authResponse.getEntity(AuthenticateResponse).value.token.id

        // Update domains to have same ID
        def rcnSwitchResponse = cloud20.domainRcnSwitch(sharedServiceAdminToken, sharedUserAdmin.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(sharedServiceAdminToken, sharedUserAdmin2.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)

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
    def "Validate with da requires DA services to be enabled: enabled: #enableServices"() {
        // Must allow services in order to get token
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)

        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it.delegateId = sharedSubUser2.id
            it
        }
        def sharedDaResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert sharedDaResponse.status == SC_CREATED
        def sharedDa = sharedDaResponse.getEntity(DelegationAgreement)

        def token = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, sharedDa.id).token.id

        // Now enable/disable for test
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, enableServices)

        when: "Validate auth token"
        def response = cloud20.validateToken(token, token)

        then:
        if (enableServices) {
            assert response.status == SC_OK
        } else {
            assert response.status == SC_SERVICE_UNAVAILABLE
        }

        where:
        enableServices << [true, false]
    }

    /**
     *  A vanilla DA with no roles assigned causes a delegate to have same roles and information as a
     *  vanilla subuser within the domain for which apply_rcn_roles was applied. Since this is standard use case doing
     *  multiple permutations of format and feature flags.
     */
    @Unroll
    def "Validating a DA token for a delegate returns same roles as subuser's validated token and all roles on DA: #mediaType, feature.enable.user.admin.look.up.by.domain: #userAdminLookupByDomain"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, userAdminLookupByDomain)

        UserGroup domain2UserGroup = utils.createUserGroup(sharedSubUser2.domainId)

        AuthenticateResponse realSubUserValidateResponse = utils.validateTokenApplyRcnRoles(sharedSubUserToken)

        // Create some fed users; one belonging to user group
        AuthenticateResponse fedUserAuthResponse = authBasicFedUser()
        AuthenticateResponse fedUserAuthResponse2 = authBasicFedUser()
        AuthenticateResponse fedUserAuthResponseGroupMember = authBasicFedUser([domain2UserGroup.getName()] as Set)

        def mossoTenantId = sharedUserAdmin.domainId
        def nastTenantId = realSubUserValidateResponse.user.roles.role.find {it.name == DEFAULT_OBJECT_STORE_ROLE}.tenantId

        // Create a DA with subuser2, fed user, and user group as delegates
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser2.id)
        utils.addUserDelegate(sharedUserAdminToken, da.id, fedUserAuthResponse.user.id)
        utils.addUserDelegate(sharedUserAdminToken, da.id, fedUserAuthResponse2.user.id)
        utils.addUserGroupDelegate(sharedUserAdminToken, da.id, domain2UserGroup.id)

        // Grant the roles to the DA
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [mossoTenantId]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(da, assignments, sharedUserAdminToken)

        // Auth as users to get delegation auth responses to compare to validate responses
        AuthenticateResponse userDelegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id, mediaType)
        AuthenticateResponse fedUserDelegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponse.token.id, da.id, mediaType)
        AuthenticateResponse userGroupDelegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(fedUserAuthResponseGroupMember.token.id, da.id, mediaType)

        when: "validate as user delegate"
        AuthenticateResponse delegateValidateResponse = utils.validateToken(userDelegateAuthResponse.token.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateValidateSameAsSubuser(delegateValidateResponse, realSubUserValidateResponse, userDelegateAuthResponse.user, da)

        and: "user received DA role 1 on all tenants"
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == mossoTenantId} != null
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == nastTenantId} != null

        and: "user received DA role 2 on mosso tenant only"
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == mossoTenantId} != null
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == nastTenantId} == null

        when: "validate as fed user delegate"
        delegateValidateResponse = utils.validateToken(fedUserDelegateAuthResponse.token.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateValidateSameAsSubuser(delegateValidateResponse, realSubUserValidateResponse, fedUserDelegateAuthResponse.user, da)

        and: "user received DA role 1 on all tenants"
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == mossoTenantId} != null
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == nastTenantId} != null

        and: "user received DA role 2 on mosso tenant only"
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == mossoTenantId} != null
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == nastTenantId} == null

        when: "validate as user group delegate"
        delegateValidateResponse = utils.validateToken(userGroupDelegateAuthResponse.token.id, mediaType)

        then: "resultant info is appropriate"
        assertDelegateValidateSameAsSubuser(delegateValidateResponse, realSubUserValidateResponse, userGroupDelegateAuthResponse.user, da)

        and: "user received DA role 1 on all tenants"
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == mossoTenantId} != null
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC1_ID && it.tenantId == nastTenantId} != null

        and: "user received DA role 2 on mosso tenant only"
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == mossoTenantId} != null
        delegateValidateResponse.user.roles.role.find {it.id == ROLE_RBAC2_ID && it.tenantId == nastTenantId} == null

        cleanup:
        reloadableConfiguration.reset()

        where:
        [mediaType, userAdminLookupByDomain] << [[MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE], [true,false]].combinations()
    }

    void assertDelegateValidateSameAsSubuser(AuthenticateResponse delegateAuthResponse, AuthenticateResponse realSubUserAuthResponse, def delegateUser, DelegationAgreement da) {
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

        // Delegate information is populated
        assert delegateAuthResponse.user.delegationAgreementId == da.id

        // User identifying information reflects user authenticating
        assert delegateAuthResponse.user.id == delegateUser.id // User reflects the delegate
        assert delegateAuthResponse.user.name == delegateUser.name // User reflects the delegate

        // service catalog is empty (like for all validates)
        assert (delegateAuthResponse.serviceCatalog == null || delegateAuthResponse.serviceCatalog.service.size() == 0)
    }

    AuthenticateResponse authBasicFedUser(Set<String> groupNames = [] as Set) {
        def samlRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = sharedUserAdmin2.domainId
            it.validitySeconds = 100
            it.brokerIssuer = DEFAULT_BROKER_IDP_URI
            it.originIssuer = IDP_V2_DOMAIN_URI
            it.email = DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = UUID.randomUUID().toString()
            it.roleNames = [] as Set
            it.groupNames = groupNames
            it
        }

        return utils.authenticateV2FederatedUser(samlRequest)
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