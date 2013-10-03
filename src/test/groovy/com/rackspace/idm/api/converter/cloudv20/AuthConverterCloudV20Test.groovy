package com.rackspace.idm.api.converter.cloudv20

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
    }

    def "toAuthenticationResponse using a racker" () {
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
        1 * userConverter.toUserForAuthenticateResponse(racker, roles)
        response != null
    }

    def "toAuthenticationResponse using a user" () {
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
        0 * userConverter.toUserForAuthenticateResponse(entityFactory.createRacker(), roles)
        response != null
    }

}
