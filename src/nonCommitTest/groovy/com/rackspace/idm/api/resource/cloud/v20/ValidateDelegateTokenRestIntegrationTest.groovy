package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.security.AETokenService
import com.rackspace.idm.domain.service.IdentityUserService
import org.joda.time.DateTime
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

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

    @Shared DelegationAgreement sharedDa

    @Shared EndpointTemplate endpointTemplate

    @Autowired
    AETokenService aeTokenService

    @Autowired
    IdentityUserService identityUserService;

    def setupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)

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

        // Create delegation agreement and give subuser2 access to same domain as subuser
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it.delegateId = sharedSubUser2.id
            it
        }
        def sharedDaResponse = cloud20.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        assert sharedDaResponse.status == SC_CREATED
        sharedDa = sharedDaResponse.getEntity(DelegationAgreement)
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
     *  A vanilla DA with no roles assigned causes a delegate to have same roles and service catalog as a
     *  vanilla subuser within the domain for which apply_rcn_roles was applied.
     */
    @Unroll
    def "Valid auth with da issues token: #mediaType"() {
        // Auth as regular subuser under the domain
        AuthenticateResponse realSubUserAuthResponse = utils.validateTokenApplyRcnRoles(sharedSubUserToken)

        def delegateToken = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, sharedDa.id, mediaType).token.id

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateValidateResponse = utils.validateToken(delegateToken, mediaType)

        then: "resultant info is appropriate"
        // Token info same (though not id..)
        delegateValidateResponse.token.tenant.name == realSubUserAuthResponse.token.tenant.name
        delegateValidateResponse.token.tenant.id == realSubUserAuthResponse.token.tenant.id
        delegateValidateResponse.token.id != realSubUserAuthResponse.token.tenant.id

        // Token reflects delegate authentication
        delegateValidateResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())
        delegateValidateResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.DELEGATE.getValue())

        // Domain based user info is same
        delegateValidateResponse.user.domainId == realSubUserAuthResponse.user.domainId
        delegateValidateResponse.user.defaultRegion == realSubUserAuthResponse.user.defaultRegion
        delegateValidateResponse.user.sessionInactivityTimeout == realSubUserAuthResponse.user.sessionInactivityTimeout

        // Roles are same
        delegateValidateResponse.user.roles.role.size() == realSubUserAuthResponse.user.roles.role.size()
        delegateValidateResponse.user.roles.role.each { delegateRole ->
            def matchingSubuserRole = realSubUserAuthResponse.user.roles.role.find {it.id == delegateRole.id && it.tenantId == delegateRole.tenantId}
            assert matchingSubuserRole != null
            assert delegateRole.name == matchingSubuserRole.name
        }

        // User identifying information reflects user authenticating
        delegateValidateResponse.user.id == sharedSubUser2.id
        delegateValidateResponse.user.name == sharedSubUser2.username

        // service catalog is empty (like for all validates)
        delegateValidateResponse.serviceCatalog == null || delegateValidateResponse.serviceCatalog.service.size() == 0

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }
}