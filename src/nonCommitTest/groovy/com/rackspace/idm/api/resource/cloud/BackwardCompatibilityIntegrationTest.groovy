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

    def "Verify that create user v1.1 sets correctly the v1Default for v1.0 authentication" () {
        when:
        def user = utils11.createUser()
        org.openstack.docs.identity.api.v2.User user20 = utils.getUserByName(user.id)
        def headers = utils10.authenticate(user.id, user.key)

        def v1DefaultEndpoints = user.baseURLRefs.baseURLRef.findAll{it.v1Default == true}

        def enpoints = [].asList()
        for(String id : v1DefaultEndpoints.id){
            def endpoint = utils11.getBaseURLById(id)
            enpoints.add(endpoint)
        }

        def storage = enpoints.find {it.serviceName == "cloudFiles" }
        def cdn = enpoints.find {it.serviceName == "cloudFilesCDN"}
        def servers = enpoints.find {it.serviceName == "cloudServers"}


        then:
        user != null
        storage.publicURL ==  utils10.removeTenantFromEndpoint(headers["X-Storage-Url"][0])
        cdn.publicURL ==  utils10.removeTenantFromEndpoint(headers["X-CDN-Management-Url"][0])
        servers.publicURL ==  utils10.removeTenantFromEndpoint(headers["X-Server-Management-Url"][0])

        cleanup:
        utils.deleteUser(user20)
        utils.deleteTenant(String.valueOf(user.mossoId))
        utils.deleteTenant(user.nastId)
        utils.deleteDomain(String.valueOf(user.mossoId))
    }
}
