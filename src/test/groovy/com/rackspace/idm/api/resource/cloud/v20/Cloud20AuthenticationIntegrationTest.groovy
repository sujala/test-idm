package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.User
import org.apache.http.HttpStatus
import org.apache.commons.lang.RandomStringUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import java.util.regex.Pattern

class Cloud20AuthenticationIntegrationTest extends RootIntegrationTest{

    @Autowired
    UserDao userDao

    @Shared def userAdmin, users
    @Shared def domainId
    @Shared def serviceAdminToken

    def void setup() {
        domainId = utils.createDomain()
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        serviceAdminToken = utils.getServiceAdminToken()
    }

    def "Auth with token + tenant returns service catalog appropriately based on state of tenants" () {
        given:
        def token = utils.getToken(userAdmin.username)

        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        def nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !nastAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        nastAuthResponse.getHeaders().getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticateTokenAndTenant(token, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateTokenAndTenant(token, nastTenant.id)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !mossoAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        mossoAuthResponse.getHeaders().getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with pwd + tenant returns service catalog appropriately based on state of tenants" () {
        given:
        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        def mossoAuthRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(userAdmin.username, Constants.DEFAULT_PASSWORD, mossoTenant.id)
        def nastAuthRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(userAdmin.username, Constants.DEFAULT_PASSWORD, nastTenant.id)

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        def nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !nastAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !mossoAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with api key + tenant returns service catalog appropriately based on state of tenants" () {
        given:
        User userEntity = userDao.getUserById(userAdmin.id)

        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        def mossoAuthRequest = v2Factory.createApiKeyAuthenticationRequestWithTenantId(userAdmin.username, userEntity.apiKey, mossoTenant.id)
        def nastAuthRequest = v2Factory.createApiKeyAuthenticationRequestWithTenantId(userAdmin.username, userEntity.apiKey, nastTenant.id)

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        def nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !nastAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticate(mossoAuthRequest)
        nastAuthResponse = cloud20.authenticate(nastAuthRequest)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !mossoAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with OTP MFA returns service catalog appropriately based on state of tenants" () {
        given:
        OTPDevice device = utils.setUpAndEnableUserForMultiFactorOTP(serviceAdminToken, userAdmin)

        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        assert mossoTenant != null
        assert nastTenant != null

        def response = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        assert wwwHeader != null

        String sessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        def otpPasscode = utils.getOTPCodeForDevice(device)

        when: "All tenants enabled"
        def mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        def nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        when: "Disable only NAST tenant and auth"
        utils.updateTenant(nastTenant.id, false)
        mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso successful w/ service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(mossoAuthResponse)

        and: "NAST fails"
        nastAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !nastAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        when: "Disable MOSSO and NAST tenant and auth"
        utils.updateTenant(mossoTenant.id, false)
        mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso successful w/o service catalog"
        mossoAuthResponse.status == HttpStatus.SC_OK
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == mossoTenant.id
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(mossoAuthResponse)

        and: "NAST successful w/o service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertDoesNotHaveServiceCatalog(nastAuthResponse)

        when: "Disable only MOSSO tenant and auth"
        utils.updateTenant(nastTenant.id, true) //need to just re-enable nast
        mossoAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, mossoTenant.id)
        nastAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, otpPasscode, nastTenant.id)

        then: "Mosso fails"
        mossoAuthResponse.status == HttpStatus.SC_UNAUTHORIZED
        !mossoAuthResponse.headers.containsKey(GlobalConstants.X_TENANT_ID)
        mossoAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username

        and: "NAST successful w/ service catalog"
        nastAuthResponse.status == HttpStatus.SC_OK
        nastAuthResponse.headers.getFirst(GlobalConstants.X_TENANT_ID) == nastTenant.id
        nastAuthResponse.headers.getFirst(GlobalConstants.X_USER_NAME) == userAdmin.username
        assertHasServiceCatalog(nastAuthResponse)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with PWD: Returns auto-assigned role and allows auth against tenant w/o role" () {
        given:
        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        // Create a "faws" tenant w/ in domain
        def fawsTenantId = RandomStringUtils.randomAlphanumeric(9)
        def fawsTenantCreate = v2Factory.createTenant(fawsTenantId, fawsTenantId).with {
            it.domainId = mossoTenant.domainId
            it
        }
        def fawsTenant = utils.createTenant(fawsTenantCreate);

        def mossoAuthRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(userAdmin.username, Constants.DEFAULT_PASSWORD, mossoTenant.id)
        def nastAuthRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(userAdmin.username, Constants.DEFAULT_PASSWORD, nastTenant.id)
        def fawsAuthRequest = v2Factory.createPasswordAuthenticationRequestWithTenantId(userAdmin.username, Constants.DEFAULT_PASSWORD, fawsTenant.id)

        when: "Auth w/ pwd w auto assigned enabled"
        def response2 = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)

        then: "Tenant access roles returned for all tenants"
        AuthenticateResponse authResponse2 = response2.getEntity(AuthenticateResponse).value
        def roles2 = authResponse2.user.roles.role
        roles2.size() == 6
        roles2.find {it.id == Constants.MOSSO_ROLE_ID} != null
        roles2.find {it.id == Constants.NAST_ROLE_ID} != null
        roles2.find {it.id == Constants.USER_ADMIN_ROLE_ID} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == mossoTenant.id} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == nastTenant.id} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == fawsTenant.id} != null

        and: "User can auth with mosso/nast tenant"
        cloud20.authenticate(mossoAuthRequest).status == HttpStatus.SC_OK
        cloud20.authenticate(nastAuthRequest).status == HttpStatus.SC_OK

        and: "User can also auth w/ faws tenant"
        cloud20.authenticate(fawsAuthRequest).status == HttpStatus.SC_OK

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenantQuietly(mossoTenant)
        utils.deleteTenantQuietly(nastTenant)
        utils.deleteTenantQuietly(fawsTenant)
        utils.deleteDomain(domainId)
    }

    /**
     * If the role specified as the auto-assigned implicit role to grant on every tenant within user's domain is already
     * assigned as an explicit role on the user for same tenant, the role is only returned once for that tenant
     * @return
     */
    def "Auth with PWD: Auto-assigned role is merged with explicit role" () {
        given:
        //get all tenants (nast + mosso)
        Tenants tenants = cloud20.getDomainTenants(serviceAdminToken, domainId).getEntity(Tenants).value
        def mossoTenant = tenants.tenant.find {
            it.id == domainId
        }
        def nastTenant = tenants.tenant.find() {
            it.id != domainId
        }

        // Create a "faws" tenant w/ in domain
        def fawsTenantId = RandomStringUtils.randomAlphanumeric(9)
        def fawsTenantCreate = v2Factory.createTenant(fawsTenantId, fawsTenantId).with {
            it.domainId = mossoTenant.domainId
            it
        }
        def fawsTenant = utils.createTenant(fawsTenantCreate);

        //create a faws tenant in a different domain
        def externalTenantId = RandomStringUtils.randomAlphanumeric(9)
        def externalDomain = utils.createDomainEntity()
        def externalTenant = v2Factory.createTenant(externalTenantId, externalTenantId).with {
            it.domainId = externalDomain.id
            it
        }
        def externalTenantCreate = utils.createTenant(externalTenant);

        // Explicitly add tenant access role to internal and external tenants
        utils.addRoleToUserOnTenantId(userAdmin, fawsTenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID)
        utils.addRoleToUserOnTenantId(userAdmin, externalTenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID)

        when: "Auth w/ pwd"
        def response2 = cloud20.authenticate(userAdmin.username, Constants.DEFAULT_PASSWORD)

        then: "Tenant access roles returned for all tenants"
        AuthenticateResponse authResponse2 = response2.getEntity(AuthenticateResponse).value
        def roles2 = authResponse2.user.roles.role
        roles2.size() == 7
        roles2.find {it.id == Constants.MOSSO_ROLE_ID} != null
        roles2.find {it.id == Constants.NAST_ROLE_ID} != null
        roles2.find {it.id == Constants.USER_ADMIN_ROLE_ID} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == fawsTenantId} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == externalTenantId} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == mossoTenant.id} != null
        roles2.find {it.id == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && it.tenantId == nastTenant.id} != null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenantQuietly(mossoTenant)
        utils.deleteTenantQuietly(nastTenant)
        utils.deleteTenantQuietly(fawsTenant)
        utils.deleteDomain(domainId)
    }

    def "v2.0 Authenticate returns user's domain" () {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        utils.addApiKeyToUser(userAdmin)
        def cred = utils.getUserApiKey(userAdmin)

        when: "auth w/ api key"
        AuthenticateResponse response = utils.authenticateApiKey(userAdmin, cred.apiKey)

        then: "domainId is returned"
        response.user.domainId == domainId

        when: "auth with pwd"
        response = utils.authenticate(userAdmin)

        then: "domainId is returned"
        response.user.domainId == domainId

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def void assertHasServiceCatalog(response) {
        AuthenticateResponse authResponse = response.getEntity(AuthenticateResponse).value

        assert authResponse.serviceCatalog != null
        assert authResponse.serviceCatalog.service != null
        assert authResponse.serviceCatalog.service.size() > 0
    }

    def void assertDoesNotHaveServiceCatalog(response) {
        AuthenticateResponse authResponse = response.getEntity(AuthenticateResponse).value
        assert authResponse.serviceCatalog == null || authResponse.serviceCatalog.service == null ||
                authResponse.serviceCatalog.service.size() == 0
    }
}
