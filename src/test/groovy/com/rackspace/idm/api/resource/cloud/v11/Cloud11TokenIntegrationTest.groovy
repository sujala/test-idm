package com.rackspace.idm.api.resource.cloud.v11

import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.*

class Cloud11TokenIntegrationTest extends RootIntegrationTest {

    def "Authenticate user within a disable domain should return 403" () {
        given:
        def user = utils11.createUser()
        def domainId = String.valueOf(user.mossoId)
        def domain = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }

        when:
        utils11.authenticateWithKey(user.id, user.key)
        utils.updateDomain(domainId, domain)

        def cred = v1Factory.createUserKeyCredentials(user.id, user.key)
        def response = cloud11.authenticate(cred)

        def mossoCred = v1Factory.createMossoCredentials(user.mossoId, user.key)
        def mossoResponse = cloud11.adminAuthenticate(mossoCred)

        def nastCred = v1Factory.createNastCredentials(user.nastId, user.key)
        def nastResponse = cloud11.adminAuthenticate(nastCred)

        then:
        response.status ==  SC_FORBIDDEN
        mossoResponse.status ==  SC_FORBIDDEN
        nastResponse.status ==  SC_FORBIDDEN

        cleanup:
        utils11.deleteUser(user)
        utils.deleteTenantById(String.valueOf(user.mossoId))
        utils.deleteTenantById(user.nastId)
        utils.deleteDomain(domainId)
    }
}
