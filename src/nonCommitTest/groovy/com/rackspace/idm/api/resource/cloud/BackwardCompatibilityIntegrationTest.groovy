package com.rackspace.idm.api.resource.cloud

import spock.lang.Shared
import testHelpers.RootIntegrationTest

class BackwardCompatibilityIntegrationTest extends RootIntegrationTest {

    @Shared def userAdmin, users

    def "Verify that the 'create one user' call is backwards compatible with v11 for user-admins" () {
        given:
        def domainId = utils.createDomain()

        when:
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def user = utils11.getUserByName(userAdmin.username)
        String token = utils.getToken(userAdmin.username)
        def endpoints = utils.getEndpointsForToken(token)

        then:
        user.id == userAdmin.username
        user.mossoId.toString() == userAdmin.domainId
        user.nastId == utils.getNastTenant(domainId)
        user.enabled == userAdmin.enabled

        endpoints != null
        user.baseURLRefs.baseURLRef.size() == endpoints.endpoint.size()
        for(String id : endpoints.endpoint.id){
            assert (user.baseURLRefs.baseURLRef.id.contains(Integer.valueOf(id)) == true)
        }

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(userAdmin.domainId)
    }

    def "Verify that the 'create one user' sets all v1Defaults" () {
        given:
        def domainId = utils.createDomain()

        when:
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def user = utils11.getUserByName(userAdmin.username)

        then:
        user != null
        utils11.validateV1Default(user.baseURLRefs.baseURLRef)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(userAdmin.domainId)
    }
}
