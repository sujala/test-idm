package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.domain.entity.AuthData
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.User
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Shared
import testHelpers.RootServiceTest

class AuthConverterCloudV20Test extends RootServiceTest{
    @Shared AuthConverterCloudV20 converter

    def setup(){
        converter = new AuthConverterCloudV20()
        mockJAXBObjectFactories(converter)
        mockTokenConverter(converter)
        mockUserConverter(converter)
        mockEndpointConverter(converter)
        mockUserService(converter)
    }

    def "toAuthenticationResponse using a racker verifies that toUserForAuthenticateResponse uses a Racker as a parameter" () {
        given:
        Racker racker = entityFactory.createRacker()
        def sa = entityFactory.createScopeAccess()
        def tenantRole = entityFactory.createTenantRole()
        def roles = [tenantRole].asList()
        def endpoint = entityFactory.createOpenstackEndpoint()
        def endpoints = [endpoint].asList()


        when:
        AuthenticateResponse response = converter.toAuthenticationResponse(racker, sa, roles, endpoints)

        then:
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse() >> new AuthenticateResponse()
        1 * userConverter.toRackerForAuthenticateResponse(racker, roles)
        response != null
    }

    def "toAuthenticationResponse using a user - verifies that toUserForAuthenticateResponse uses a User as a parameter" () {
        given:
        User user = entityFactory.createUser()
        def sa = entityFactory.createScopeAccess()
        def tenantRole = entityFactory.createTenantRole()
        def roles = [tenantRole].asList()
        def endpoint = entityFactory.createOpenstackEndpoint()
        def endpoints = [endpoint].asList()

        when:
        AuthenticateResponse response = converter.toAuthenticationResponse(user, sa, roles, endpoints)

        then:
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse() >> new AuthenticateResponse()
        1 * userConverter.toUserForAuthenticateResponse(user, roles)
        0 * userConverter.toRackerForAuthenticateResponse(entityFactory.createRacker(), roles)
        response != null
    }

    def "toAuthenticationResponse using a authData"() {
        given:
        AuthData authData = new AuthData();
        FederatedUser federatedUser = entityFactory.createFederatedUser()
        federatedUser.setRoles([entityFactory.createTenantRole()].asList())
        authData.setUser(federatedUser)
        authData.setEndpoints([entityFactory.createOpenstackEndpoint()].asList())
        authData.setToken(entityFactory.createFederatedToken())

        when:
        AuthenticateResponse response = converter.toAuthenticationResponse(authData)

        then:
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse() >> new AuthenticateResponse()
        1 * tokenConverter.toToken(authData.getToken(), authData.getUser().getRoles())
        1 * userConverter.toUserForAuthenticateResponse(authData.getUser())
        1 * endpointConverter.toServiceCatalog(authData.getEndpoints())
        response != null
    }

}
