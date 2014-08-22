package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ScopeEnum
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.entity.User
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import testHelpers.V2Factory

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_OK


class AuthScopedTokenIntegrationTest extends RootIntegrationTest {

    @Autowired LdapScopeAccessRepository scopeAccessRepository
    @Autowired LdapUserRepository userRepository

    def "Auth by password with scope creates ScopeAccess with scope separate from regular scopeAccess"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        def scopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(entity.token.id)

        then:
        scopeAccess != null
        scopeAccess.scope == SCOPE_SETUP_MFA
        entity.serviceCatalog == null

        when:
        def regularAuth = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD)
        assert (regularAuth.status == SC_OK)
        def entity2 = regularAuth.getEntity(AuthenticateResponse).value
        assert(entity2 != null)

        then:
        entity.token.id != entity2.token.id

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth by apikey with scope creates ScopeAccess with scope"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        User initialUserAdmin = userRepository.getUserById(userAdmin.id)

        when:
        def response = cloud20.authenticateApiKeyWithScope(userAdmin.username, initialUserAdmin.apiKey, SCOPE_SETUP_MFA)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        def scopeAccess = scopeAccessRepository.getScopeAccessByAccessToken(entity.token.id)

        then:
        scopeAccess != null
        scopeAccess.scope == SCOPE_SETUP_MFA

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if domain attribute present"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def request = v2Factory.createPasswordAuthenticationRequestWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)
        request.domain = v1Factory.createDomain(null, "Domain")

        when:
        def response = cloud20.authenticate(request)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if authing with token"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def request = v2Factory.createAuthenticationRequest()
        request.token = v2Factory.createTokenForAuthenticationRequest()
        request.tenantId = null
        request.tenantName = null
        request.scope = ScopeEnum.fromValue(SCOPE_SETUP_MFA)

        when:
        def response = cloud20.authenticate(request)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if authing with passcode"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def request = v2Factory.createPasscodeAuthenticationRequest("1234")
        request.scope = ScopeEnum.fromValue(SCOPE_SETUP_MFA)

        when:
        def response = cloud20.authenticate(request)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "Auth with scope throws forbidden error if authing with multi-factor enabled"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        def userEntity = userRepository.getUserById(userAdmin.id)
        userEntity.multifactorEnabled = true
        userRepository.updateUserAsIs(userEntity)

        when:
        def response = cloud20.authenticatePasswordWithScope(userAdmin.username, DEFAULT_PASSWORD, SCOPE_SETUP_MFA)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }
}
