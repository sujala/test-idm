package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.IdentityPropertyValueType
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.AuthenticationRequest
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_OK
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

/**
 * Tests various aspects of authentiacting to a specific domain
 */
class DomainAuthenticationIntegrationTest extends RootIntegrationTest {

    @Shared User sharedUserAdmin
    @Shared String sharedIdentityAdminToken

    def setupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        def response = cloud20.addApiKeyToUser(sharedIdentityAdminToken, sharedUserAdmin.id, v2Factory.createApiKeyCredentials(sharedUserAdmin.username, Constants.DEFAULT_API_KEY))
        assert response.status == SC_OK
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

        then: "Valid"
        response.status == SC_OK

        where:
        [defaultDomain, verifyDomain] << [[true, false], [true, false]].combinations()
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
    def "authWithToken: When existing tenant belongs to different domain than user and don't specified explicit domainId. If default=#defaultDomain and verify=#verifyDomain, expect=#expectedResult"() {
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
        false | true | SC_OK // No domain is specified or defaulted, so verification not relevant
        true | false | SC_OK // Not verifying auth domain
        true | true | SC_UNAUTHORIZED // The user doesn't belong to the tenant's domain
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
