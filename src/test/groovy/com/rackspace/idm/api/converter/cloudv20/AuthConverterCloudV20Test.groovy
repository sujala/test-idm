package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Shared
import testHelpers.RootServiceTest

class AuthConverterCloudV20Test extends RootServiceTest{

    @Shared AuthConverterCloudV20 converter

    def setup() {
        converter = new AuthConverterCloudV20()
        mockJAXBObjectFactories(converter)
        mockTokenConverter(converter)
        mockUserConverter(converter)
        mockEndpointConverter(converter)
    }

    def "toAuthenticationResponse using a racker verifies that toUserForAuthenticateResponse uses a Racker as a parameter" () {
        given:
        Racker racker = entityFactory.createRacker()
        def sa = entityFactory.createRackerScopeAccess()
        def tenantRole = entityFactory.createTenantRole()
        def roles = [tenantRole].asList()
        def endpoint = entityFactory.createOpenstackEndpoint()

        when:
        AuthenticateResponse response = converter.toRackerAuthenticationResponse(racker, sa, roles, null)

        then:
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse() >> new AuthenticateResponse()
        1 * userConverter.toRackerForAuthenticateResponse(racker, roles)
        response != null
    }

    def "toAuthenticationResponse using a user - verifies that toUserForAuthenticateResponse uses a User as a parameter" () {
        given:
        User user = entityFactory.createUser()
        def sa = new UserScopeAccess()
        def tenantRole = entityFactory.createTenantRole()
        def roles = [tenantRole].asList()
        def endpoint = entityFactory.createOpenstackEndpoint()
        def endpoints = [endpoint].asList()
        def authTuple = new AuthResponseTuple(user, sa)
        def scInfo = new ServiceCatalogInfo(roles, Collections.EMPTY_LIST, Collections.EMPTY_LIST, IdentityUserTypeEnum.DEFAULT_USER)

        when:
        AuthenticateResponse response = converter.toAuthenticationResponse(authTuple, scInfo)

        then:
        1 * jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse() >> new AuthenticateResponse()
        1 * userConverter.toUserForAuthenticateResponse(user, roles)
        0 * userConverter.toRackerForAuthenticateResponse(entityFactory.createRacker(), roles)
        response != null
    }


}
