package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.IdentityUserService
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
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

        // Create delegation agreement and give subuser2 access to same domain as subuser
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def sharedDaResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert sharedDaResponse.status == SC_CREATED
        def sharedDa = sharedDaResponse.getEntity(DelegationAgreement)

        cloud20.addUserDelegate(sharedUserAdminToken, sharedDa.id, sharedSubUser2.id)
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
            it
        }
        def sharedDaResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert sharedDaResponse.status == SC_CREATED
        def sharedDa = sharedDaResponse.getEntity(DelegationAgreement)
        utils.addUserDelegate(sharedUserAdminToken, sharedDa.id, sharedSubUser2.id)

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
        utils.deleteDelegationAgreement(sharedUserAdminToken, da)
        reloadableConfiguration.reset()

        where:
        [mediaType, userAdminLookupByDomain] << [[MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE], [true,false]].combinations()
    }

    def "Delegate Token returns 403 non approved services"() {
        // Must allow services in order to get token
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)

        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def sharedDaResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert sharedDaResponse.status == SC_CREATED
        def sharedDa = sharedDaResponse.getEntity(DelegationAgreement)
        utils.addUserDelegate(sharedUserAdminToken, sharedDa.id, sharedSubUser2.id)

        def token = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, sharedDa.id).token.id

        when: "list tenants"
        def response = cloud20.listTenants(token)

        then:
        assert response.status == SC_FORBIDDEN

        when: "list user groups"
        response = cloud20.listUserGroupsForDomain(token, sharedUserAdmin.domainId)

        then:
        assert response.status == SC_FORBIDDEN

        when: "list endpoint assignment rules"
        response = cloud20.listEndpointAssignmentRules(token)

        then:
        assert response.status == SC_FORBIDDEN
    }
    
    def "DA tokens are revoked when the DA is deleted"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def otherUserAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(otherUserAdmin.domainId, domain.rackspaceCustomerNumber)
        def userAdminToken = utils.getToken(userAdmin.username)
        def da = utils.createDelegationAgreementInDomain(userAdminToken, userAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, otherUserAdmin.id)
        def provisionedUserDaToken = utils.getDelegationAgreementToken(otherUserAdmin.username, da.id)
        def federatedUserAuthResponse = utils.authenticateFederatedUser(otherUserAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, federatedUserAuthResponse.user.id)
        def federatedUserDaToken = utils.authenticateTokenAndDelegationAgreement(federatedUserAuthResponse.token.id, da.id).token.id

        when: "validate the DA token for the provisioned user"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then:
        response.status == 200

        when: "validate the DA token for the federated user"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then:
        response.status == 200

        when: "delete the DA and validate the provisioned user DA token again"
        utils.deleteDelegationAgreement(utils.getToken(userAdmin.username), da)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "token has been revoked"
        response.status == 404

        when: "validate the federated user DA token again"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "token has been revoked"
        response.status == 404
    }

    def "DA tokens are revoked for a user when the user is disabled"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def otherUserAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(otherUserAdmin.domainId, domain.rackspaceCustomerNumber)
        def userAdminToken = utils.getToken(userAdmin.username)
        def da = utils.createDelegationAgreementInDomain(userAdminToken, userAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, otherUserAdmin.id)
        def daToken = utils.getDelegationAgreementToken(otherUserAdmin.username, da.id)

        when: "validate the DA token"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), daToken)

        then:
        response.status == 200

        when: "disable the user"
        utils.disableUser(otherUserAdmin)
        response = cloud20.validateToken(utils.getServiceAdminToken(), daToken)

        then: "token has been revoked"
        response.status == 404

        when: "enable the user and validate the token again"
        otherUserAdmin.enabled = true
        utils.updateUser(otherUserAdmin)
        response = cloud20.validateToken(utils.getServiceAdminToken(), daToken)

        then: "the token is still revoked"
        response.status == 404
    }

    def "DA tokens are revoked when a delegate's domain is disabled"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def otherUserAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(otherUserAdmin.domainId, domain.rackspaceCustomerNumber)
        def userAdminToken = utils.getToken(userAdmin.username)
        def da = utils.createDelegationAgreementInDomain(userAdminToken, userAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, otherUserAdmin.id)
        def provisionedUserDaToken = utils.getDelegationAgreementToken(otherUserAdmin.username, da.id)
        def federatedUserAuthResponse = utils.authenticateFederatedUser(otherUserAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, federatedUserAuthResponse.user.id)
        def federatedUserDaToken = utils.authenticateTokenAndDelegationAgreement(federatedUserAuthResponse.token.id, da.id).token.id

        when: "validate the DA token for the provisioned user"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then:
        response.status == 200

        when: "validate the DA token for the federated user"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then:
        response.status == 200

        when: "disable the delegate's domain and validate the provisioned user's token"
        utils.disableDomain(otherUserAdmin.domainId)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "token has been revoked"
        response.status == 404

        when: "validate the federated user's token"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "token has been revoked"
        response.status == 404

        when: "enable the domain and validate the provisioned user's token again"
        utils.updateDomain(otherUserAdmin.domainId, new Domain().with { it.enabled = true; it})
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "the token is still revoked"
        response.status == 404

        when: "enable the domain and validate the federated user's token again"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "the token is still revoked"
        response.status == 404
    }

    def "DA tokens are revoked when the domain for the DA is disabled"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def otherUserAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(otherUserAdmin.domainId, domain.rackspaceCustomerNumber)
        def userAdminToken = utils.getToken(userAdmin.username)
        def da = utils.createDelegationAgreementInDomain(userAdminToken, userAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, otherUserAdmin.id)
        def provisionedUserDaToken = utils.getDelegationAgreementToken(otherUserAdmin.username, da.id)
        def federatedUserAuthResponse = utils.authenticateFederatedUser(otherUserAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, federatedUserAuthResponse.user.id)
        def federatedUserDaToken = utils.authenticateTokenAndDelegationAgreement(federatedUserAuthResponse.token.id, da.id).token.id

        when: "validate the DA provisioned user DA token"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then:
        response.status == 200

        when: "validate the DA federated user DA token"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then:
        response.status == 200

        when: "disable the DA's domain and validate the provisioned user DA token"
        utils.disableDomain(userAdmin.domainId)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "validate the federated user DA token"
        utils.disableDomain(userAdmin.domainId)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "enable the domain and validate provisioned user DA token again"
        utils.updateDomain(userAdmin.domainId, new Domain().with { it.enabled = true; it})
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "the token is valid again"
        response.status == 200

        when: "enable the domain and validate federated user DA token again"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "the token is valid again"
        response.status == 200
    }

    def "DA tokens are revoked when the user is removed as an explicit delegate"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def otherUserAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(otherUserAdmin.domainId, domain.rackspaceCustomerNumber)
        def userAdminToken = utils.getToken(userAdmin.username)
        def da = utils.createDelegationAgreementInDomain(userAdminToken, userAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, otherUserAdmin.id)
        def provisionedUserDaToken = utils.getDelegationAgreementToken(otherUserAdmin.username, da.id)
        def federatedUserAuthResponse = utils.authenticateFederatedUser(otherUserAdmin.domainId)
        utils.addUserDelegate(userAdminToken, da.id, federatedUserAuthResponse.user.id)
        def federatedUserDaToken = utils.authenticateTokenAndDelegationAgreement(federatedUserAuthResponse.token.id, da.id).token.id

        when: "validate the provisioned user DA token"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then:
        response.status == 200

        when: "validate the federated user DA token"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then:
        response.status == 200

        when: "remove the provisioned user as an explicit delegate and validate again"
        utils.deleteUserDelegate(utils.getToken(userAdmin.username), da.id, otherUserAdmin.id)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "remove the federated user as an explicit delegate and validate again"
        utils.deleteUserDelegate(utils.getToken(userAdmin.username), da.id, federatedUserAuthResponse.user.id)
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "add the provisioned user back as an explicit delegate and validate again"
        utils.addUserDelegate(userAdminToken, da.id, otherUserAdmin.id)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "the token is valid again"
        response.status == 200

        when: "add the federated user back as an explicit delegate and validate again"
        utils.addUserDelegate(userAdminToken, da.id, federatedUserAuthResponse.user.id)
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "the token is valid again"
        response.status == 200
    }

    def "DA tokens for a user through user group membership are revoked when the user group is removed from the DA"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def otherUserAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(otherUserAdmin.domainId, domain.rackspaceCustomerNumber)
        def userAdminToken = utils.getToken(userAdmin.username)
        def da = utils.createDelegationAgreementInDomain(userAdminToken, userAdmin.domainId)
        def userGroup = utils.createUserGroup(otherUserAdmin.domainId)
        utils.addUserToUserGroup(otherUserAdmin.id, userGroup)
        utils.addUserGroupDelegate(utils.getToken(userAdmin.username), da.id, userGroup.id)
        def provisionedUserDaToken = utils.getDelegationAgreementToken(otherUserAdmin.username, da.id)
        def federatedUserAuthResponse = utils.authenticateFederatedUser(otherUserAdmin.domainId, [userGroup.name] as Set)
        def federatedUserDaToken = utils.authenticateTokenAndDelegationAgreement(federatedUserAuthResponse.token.id, da.id).token.id

        when: "validate the provisioned user DA token"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then:
        response.status == 200

        when: "validate the federated user DA token"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then:
        response.status == 200

        when: "remove the user group as a delegate and validate the provisioned user DA token again"
        utils.deleteUserGroupDelegate(utils.getToken(userAdmin.username), da.id, userGroup.id)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "validate the federated user DA token again"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "add the user group back as a delegate and validate the provisioned user DA token again"
        utils.addUserGroupDelegate(utils.getToken(userAdmin.username), da.id, userGroup.id)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "the token is valid again"
        response.status == 200

        when: "validate the federated user DA token"
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then:
        response.status == 200
    }

    def "DA tokens for a user with access through user group membership are revoked when the user is removed from the user group"() {
        given:
        def userAdmin = utils.createCloudAccountWithRcn()
        def domain = utils.getDomain(userAdmin.domainId)
        def otherUserAdmin = utils.createCloudAccount()
        utils.domainRcnSwitch(otherUserAdmin.domainId, domain.rackspaceCustomerNumber)
        def userAdminToken = utils.getToken(userAdmin.username)
        def da = utils.createDelegationAgreementInDomain(userAdminToken, userAdmin.domainId)
        def userGroup = utils.createUserGroup(otherUserAdmin.domainId)
        utils.addUserToUserGroup(otherUserAdmin.id, userGroup)
        utils.addUserGroupDelegate(utils.getToken(userAdmin.username), da.id, userGroup.id)
        def provisionedUserDaToken = utils.getDelegationAgreementToken(otherUserAdmin.username, da.id)
        def federatedUserAuthResponse = utils.authenticateFederatedUser(otherUserAdmin.domainId, [userGroup.name] as Set)
        def federatedUserDaToken = utils.authenticateTokenAndDelegationAgreement(federatedUserAuthResponse.token.id, da.id).token.id

        when: "validate the provisioned user DA token"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then:
        response.status == 200

        when: "validate the federated user DA token"
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then:
        response.status == 200

        when: "remove the provisioned user from the user group"
        utils.removeUserFromUserGroup(otherUserAdmin.id, userGroup)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "remove the federated user from the user group"
        utils.authenticateFederatedUser(otherUserAdmin.domainId, [] as Set, [] as Set, federatedUserAuthResponse.user.name)
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "token has been invalidated"
        response.status == 404

        when: "add the provisioned user back to the user group and validate again"
        utils.addUserToUserGroup(otherUserAdmin.id, userGroup)
        response = cloud20.validateToken(utils.getServiceAdminToken(), provisionedUserDaToken)

        then: "the token is valid again"
        response.status == 200

        when: "add the federated user back to the user group and validate again"
        utils.authenticateFederatedUser(otherUserAdmin.domainId, [userGroup.name] as Set, [] as Set, federatedUserAuthResponse.user.name)
        response = cloud20.validateToken(utils.getServiceAdminToken(), federatedUserDaToken)

        then: "the token is valid again"
        response.status == 200
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
