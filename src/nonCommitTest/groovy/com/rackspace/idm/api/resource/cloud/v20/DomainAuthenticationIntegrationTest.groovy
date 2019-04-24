package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty
import com.rackspace.idm.Constants
import org.openstack.docs.identity.api.v2.*
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_OK
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

/**
 * Tests various aspects of authentiacting to a specific domain
 */
class DomainAuthenticationIntegrationTest extends RootIntegrationTest {

    @Shared User sharedUserAdmin
    @Shared String sharedUserAdminToken
    @Shared String sharedIdentityAdminToken

    def setupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        def response = cloud20.addApiKeyToUser(sharedIdentityAdminToken, sharedUserAdmin.id, v2Factory.createApiKeyCredentials(sharedUserAdmin.username, Constants.DEFAULT_API_KEY))
        assert response.status == SC_OK

        sharedUserAdminToken = cloud20.authenticateForToken(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD)
    }

    def cleanupSpec() {
        setDefaultFeatureFlag(false)
        setVerificationFeatureFlag(false)
    }

    @Unroll
    def "authWithPassword: Can authenticate if omit domain or specify own domain regardless of flags: defaultDomainId: #defaultDomain ; verifyDomainId: #verifyDomain"() {
        setDefaultFeatureFlag(defaultDomain)
        setVerificationFeatureFlag(verifyDomain)

        AuthenticationRequest request = v2Factory.createPasswordAuthenticationRequest(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD)

        when: "Auth without domainId"
        def response = cloud20.authenticate(request)

        then: "Valid"
        response.status == SC_OK

        when: "Add domainId to request"
        request.setDomainId(sharedUserAdmin.getDomainId())
        response = cloud20.authenticate(request)

        then: "Valid"
        response.status == SC_OK
        AuthenticateResponse authResponse = response.getEntity(AuthenticateResponse).value
        authResponse.user.domainId == sharedUserAdmin.domainId

        where:
        [defaultDomain, verifyDomain] << [[true, false], [true, false]].combinations()
    }

    @Unroll
    def "authWithX: If provide valid credentials, but domain other than user's, receive appropriate error for credential type: #authRequest.credential.value.getClass().getName()"() {
        setVerificationFeatureFlag(true)

        when:
        def response = cloud20.authenticate(authRequest)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, SC_UNAUTHORIZED, "AUTH-006", "Not authorized for the domain.")

        where:
        authRequest | _
        v2Factory.createPasswordAuthenticationRequest(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD).with {
            it.domainId = "132885"
            it
        } | _
        v2Factory.createApiKeyAuthenticationRequest(sharedUserAdmin.username, Constants.DEFAULT_API_KEY).with {
            it.domainId = "132885"
            it
        } | _
        v2Factory.createTokenAuthenticationRequest(sharedUserAdminToken, "idonotexist", null).with {
            it.domainId = "132885"
            it
        } | _
    }

    @Unroll
    def "authWithX: Credential is verified before domain for credential type: #authRequest.credential.value.getClass().getName()"() {
        setVerificationFeatureFlag(true)

        when:
        def response = cloud20.authenticate(authRequest)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, SC_UNAUTHORIZED, errorCode, errorMessage)

        where:
        authRequest | errorCode | errorMessage
        v2Factory.createPasswordAuthenticationRequest(sharedUserAdmin.username, "invalid").with {
            it.domainId = "132885"
            it
        } | "AUTH-004" | "Unable to authenticate user with credentials provided."
        v2Factory.createApiKeyAuthenticationRequest(sharedUserAdmin.username, "invalid").with {
            it.domainId = "132885"
            it
        } | "AUTH-008" | "Username or api key is invalid."
        v2Factory.createTokenAuthenticationRequest("badtoken", "idonotexist", null).with {
            it.domainId = "132885"
            it
        } | "AUTH-005" | "Token not authenticated."
    }

    @Unroll
    def "authWithX: If provide valid credentials and domain, but bad tenant, receive appropriate error for credential type: #authRequest.credential.value.getClass().getName()"() {
        setVerificationFeatureFlag(true)

        when:
        def response = cloud20.authenticate(authRequest)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, SC_UNAUTHORIZED, "AUTH-007", "Not authorized for the tenant.")

        where:
        authRequest | _
        v2Factory.createPasswordAuthenticationRequest(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD).with {
            it.domainId = sharedUserAdmin.domainId
            it.tenantId = "idonotexist" // Anything the user does not have access to
            it
        } | _
        v2Factory.createApiKeyAuthenticationRequest(sharedUserAdmin.username, Constants.DEFAULT_API_KEY).with {
            it.domainId = sharedUserAdmin.domainId
            it.tenantId = "idonotexist" // Anything the user does not have access to
            it
        } | _
        v2Factory.createTokenAuthenticationRequest(sharedUserAdminToken, "idonotexist", null).with {
            it.domainId = sharedUserAdmin.domainId
            it
        } | _
    }

    @Unroll
    def "authWithApiKey: Can authenticate if omit domain or specify own domain regardless of flags: defaultDomainId: #defaultDomain ; verifyDomainId: #verifyDomain"() {
        setDefaultFeatureFlag(defaultDomain)
        setVerificationFeatureFlag(verifyDomain)

        AuthenticationRequest request = v2Factory.createApiKeyAuthenticationRequest(sharedUserAdmin.username, Constants.DEFAULT_API_KEY)

        when: "Auth without domainId"
        def response = cloud20.authenticate(request)

        then: "Valid"
        response.status == SC_OK

        when: "Add domainId to request"
        request.setDomainId(sharedUserAdmin.getDomainId())
        response = cloud20.authenticate(request)

        then: "Valid"
        response.status == SC_OK

        where:
        [defaultDomain, verifyDomain] << [[true, false], [true, false]].combinations()
    }

    @Unroll
    def "authWithToken: Can authenticate if tenant belongs to user domain - regardless of whether user domain specified or value of flags: defaultDomainId: #defaultDomain ; verifyDomainId: #verifyDomain"() {
        setDefaultFeatureFlag(defaultDomain)
        setVerificationFeatureFlag(verifyDomain)

        String token = utils.getToken(sharedUserAdmin.username, Constants.DEFAULT_PASSWORD)

        AuthenticationRequest request = v2Factory.createTokenAuthenticationRequest(token, sharedUserAdmin.domainId, null)

        when: "Auth without domainId"
        def response = cloud20.authenticate(request)

        then: "Valid"
        response.status == SC_OK

        when: "Add domainId to request"
        request.setDomainId(sharedUserAdmin.getDomainId())
        response = cloud20.authenticate(request)

        then: "Valid"
        response.status == SC_OK

        where:
        [defaultDomain, verifyDomain] << [[true, false], [true, false]].combinations()
    }

    /**
     * This test verifies the case where a user has a direct role on a tenant in another domain and doesn't specific
     * an explicit domainId to auth into. This use case is not
     * a "supported" use case, but one that exists due to shack. The ability to set a role on a user in a different
     * domain should be prevented.
     *
     * @return
     */
    @Unroll
    def "authWithToken: When specified tenant belongs to different domain than user and don't specified explicit domainId. If default=#defaultDomain and verify=#verifyDomain, expect=#expectedResult"() {
        setDefaultFeatureFlag(defaultDomain)
        setVerificationFeatureFlag(verifyDomain)

        User user = utils.createGenericUserAdmin()
        String token = utils.getToken(user.username, Constants.DEFAULT_PASSWORD)

        Tenant tenantInOtherDomain = utils.createTenant()
        utils.addRoleToUserOnTenant(user, tenantInOtherDomain, Constants.ROLE_RBAC1_ID)

        AuthenticationRequest request = v2Factory.createTokenAuthenticationRequest(token, tenantInOtherDomain.id, null)

        when: "Auth without explicit domainId"
        def response = cloud20.authenticate(request)

        then:
        response.status == expectedResult

        where:
        defaultDomain | verifyDomain | expectedResult
        false | false | SC_OK // Currently supported use case.
        false | true | SC_OK // No domain is specified or defaulted, so verification not performed
        true | false | SC_OK // Not verifying auth domain, so should pass
        true | true | SC_UNAUTHORIZED // The user doesn't belong to the tenant's domain, the tenant domain is set as default and being verified
    }

    @Unroll
    def "authWithToken: When existing tenant belongs to different domain than user and specify user's domainId. If default=#defaultDomain and verify=#verifyDomain, expect=#expectedResult"() {
        setDefaultFeatureFlag(defaultDomain)
        setVerificationFeatureFlag(verifyDomain)

        User user = utils.createGenericUserAdmin()
        String token = utils.getToken(user.username, Constants.DEFAULT_PASSWORD)

        Tenant tenantInOtherDomain = utils.createTenant()
        utils.addRoleToUserOnTenant(user, tenantInOtherDomain, Constants.ROLE_RBAC1_ID)

        AuthenticationRequest request = v2Factory.createTokenAuthenticationRequest(token, tenantInOtherDomain.id, null).with {
            it.domainId = user.domainId
            it
        }

        when: "Auth without explicit domainId"
        def response = cloud20.authenticate(request)

        then:
        response.status == expectedResult

        where:
        defaultDomain | verifyDomain | expectedResult
        false         | false        | SC_OK // We don't do any verification around authorized domain id
        false         | true         | SC_UNAUTHORIZED // Tenant doesn't belong to specified authorized domain
        true          | false        | SC_OK  // We don't do any verification around authorized domain id
        true          | true         | SC_UNAUTHORIZED // Tenant doesn't belong to specified authorized domain
    }

    def setDefaultFeatureFlag(boolean value) {
        IdentityProperty prop = new IdentityProperty().with {
            it.value = value.toString()
            it
        }
        devops.updateIdentityProperty(sharedIdentityAdminToken, Constants.REPO_PROP_FEATURE_AUTHORIZATION_DOMAIN_DEFAULT_ID, prop)
    }

    def setVerificationFeatureFlag(boolean value) {
        IdentityProperty prop = new IdentityProperty().with {
            it.value = value.toString()
            it
        }
        devops.updateIdentityProperty(sharedIdentityAdminToken, Constants.REPO_PROP_FEATURE_AUTHORIZATION_DOMAIN_VERIFICATION_ID, prop)
    }
}
