package com.rackspace.idm.api.resource.cloud.v20;

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

class GetUserByIdWithDelegationAgreementIntegrationTest extends RootIntegrationTest {

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

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
    }

    @Unroll
    def "Support DA tokens for get user by id: #mediaType"() {
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
        delegateAuthResponse.token.tenant.id == realSubUserAuthResponse.token.tenant.id
        def delegateToken = delegateAuthResponse.token.id

        when: "Call 'get user by id' service using the delegation token. But, user ID in the url is of principal user."
        def response = cloud20.getUserById(delegateToken, sharedUserAdmin.id, mediaType)

        then: "Expect a 404 response code"
        response.status == 404

        when: "Call 'get user by id' service using the delegation token. But, the user ID in the url is of a different user in the principal's domain."
        response = cloud20.getUserById(delegateToken, sharedSubUser.id, mediaType)

        then: "Expect a 404 response code"
        response.status == 404

        when: "Call 'get user by id' service using the delegation token with user ID in the url is of delegate user."
        response = cloud20.getUserById(delegateToken, sharedSubUser2.id, mediaType)
        def user = testUtils.getEntity(response, User)

        then: "Expect a 200 response code with delegation"
        response.status == 200

        and: "The user is returned"
        user != null
        user.id == sharedSubUser2.id
        user.username == sharedSubUser2.username

        and: "The information returned must reflect the delegation agreement rather than the user."
        user.domainId == sharedUserAdmin.domainId
        user.defaultRegion == sharedUserAdmin.defaultRegion

        and: "The user information returned must include the additional attribute 'delegationAgreementId'"
        user.delegationAgreementId == da.id

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }

    @Unroll
    def "Call get user by id with delegation token but user id is of principal user belonging to the corresponding DA: #mediaType"() {
        def daToCreate = new DelegationAgreement().with {
            it.name = "a name"
            it.domainId = sharedUserAdmin.domainId
            it
        }

        // Give subuser2 access to same domain as subuser
        def da = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate)
        utils.addUserDelegate(sharedUserAdminToken, da.id, sharedSubUser2.id)

        when: "But, that principal is a delegate for some other DA."
        
        def daToCreate2 = new DelegationAgreement().with {
            it.name = "some name"
            it.domainId = sharedUserAdmin2.domainId
            it
        }

        def da2 = utils.createDelegationAgreement(sharedUserAdminToken, daToCreate2)
        utils.addUserDelegate(sharedUserAdminToken, da2.id, sharedUserAdmin.id)

        then:
        da2 != null

        // Auth as regular subuser under the domain
        AuthenticateResponse realSubUserAuthResponse = utils.authenticate(sharedSubUser.username, Constants.DEFAULT_PASSWORD, "true")

        when: "Auth as delegate from domain2 under the first domain"
        AuthenticateResponse delegateAuthResponse = utils.authenticateTokenAndDelegationAgreement(sharedSubUser2Token, da.id, mediaType)

        then: "resultant info is appropriate"
        delegateAuthResponse.token.tenant.id == realSubUserAuthResponse.token.tenant.id
        def delegateToken = delegateAuthResponse.token.id

        when: "Call 'get user by id' service using the delegation token. But, user ID in the url is of principal user."
        def response = cloud20.getUserById(delegateToken, sharedUserAdmin.id, mediaType)

        then: "Expect a 404 response code"
        response.status == 404

        where:
        mediaType << [MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE]
    }
}
