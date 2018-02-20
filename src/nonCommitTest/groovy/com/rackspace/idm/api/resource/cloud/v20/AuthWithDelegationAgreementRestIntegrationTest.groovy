package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationCredentials
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ScopeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointForService
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ServiceForCatalog
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

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
    IdentityUserService identityUserService;

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
    def "Valid auth with da issues token: #mediaType"() {
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it.delegateId = sharedSubUser2.id
            it
        }

        // Give subuser2 access to same domain as subuser
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)

        // Auth as regular subuser under the domain
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id, mediaType)

        then: "resultant info is appropriate"
        // Token info same (though not id..)
        delegateAuthResponse.token.tenant.name == realSubUserAuthResponse.token.tenant.name
        delegateAuthResponse.token.tenant.id == realSubUserAuthResponse.token.tenant.id
        delegateAuthResponse.token.id != realSubUserAuthResponse.token.tenant.id

        // Token reflects delegate authentication
        delegateAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())
        delegateAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.DELEGATE.getValue())

        // Domain based user info is same
        delegateAuthResponse.user.domainId == realSubUserAuthResponse.user.domainId
        delegateAuthResponse.user.defaultRegion == realSubUserAuthResponse.user.defaultRegion
        delegateAuthResponse.user.sessionInactivityTimeout == realSubUserAuthResponse.user.sessionInactivityTimeout

        // Roles are same
        delegateAuthResponse.user.roles.role.size() == realSubUserAuthResponse.user.roles.role.size()
        delegateAuthResponse.user.roles.role.each { delegateRole ->
            def matchingSubuserRole = realSubUserAuthResponse.user.roles.role.find {it.id == delegateRole.id && it.tenantId == delegateRole.tenantId}
            assert matchingSubuserRole != null
            assert delegateRole.name == matchingSubuserRole.name
        }

        // User identifying information reflects user authenticating
        delegateAuthResponse.user.id == sharedSubUser2.id // User reflects the delegate
        delegateAuthResponse.user.name == sharedSubUser2.username // User reflects the delegate

        // service catalogs are the same
        delegateAuthResponse.serviceCatalog.service.size() == realSubUserAuthResponse.serviceCatalog.service.size()
        delegateAuthResponse.serviceCatalog.service.each {dServiceForCatalog ->
            ServiceForCatalog matchingSubUserServiceForCatalog = realSubUserAuthResponse.serviceCatalog.service.find {it.name == dServiceForCatalog.name}
            assert matchingSubUserServiceForCatalog != null

            // now iterate through endpoints
            dServiceForCatalog.endpoint.size() == matchingSubUserServiceForCatalog.endpoint.size()
            dServiceForCatalog.endpoint.each {dEndpointForService ->
                EndpointForService matchingSubUserEndpointForService = matchingSubUserServiceForCatalog.endpoint.find {
                    it.tenantId == dEndpointForService.tenantId && it.publicURL == dEndpointForService.publicURL && it.adminURL == dEndpointForService.adminURL && it.region == dEndpointForService.region
                }
                assert matchingSubUserEndpointForService != null
            }
        }

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }
}