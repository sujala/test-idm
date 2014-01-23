package com.rackspace.idm.api.resource.cloud.v20

import com.rackspacecloud.docs.auth.api.v1.User
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.EndpointList
import org.openstack.docs.identity.api.v2.Tenant
import spock.lang.Shared
import testHelpers.RootIntegrationTest

/**
 * Created with IntelliJ IDEA
 * User: jorge
 * Date: 11/19/13
 * Time: 5:01 PM
 * To change this template use File | Settings | File Templates.
 */
class Cloud20EndpointIntegrationTest extends RootIntegrationTest{

    @Shared def defaultUser, users
    @Shared def domainId

    def "Get endpoints by domain Id" () {
        given:
        String username = utils.getRandomUUID("endpointUser")
        def mossoId = getRandomNumber(1000000, 2000000);
        def id = String.valueOf(mossoId)
        User userObject = v1Factory.createUser(username, "1234567890", mossoId, null, true)
        def tenantId = getRandomUUID("tenantId")
        Tenant tenantObject = v2Factory.createTenant(tenantId, tenantId)

        when:
        def user = cloud11.createUser(userObject).getEntity(User)
        Tenant tenant = utils.createTenant(tenantObject)
        def endpointId = user.baseURLRefs.baseURLRef.get(0).id
        EndpointTemplate endpointTemplate = v1Factory.createEndpointTemplate(endpointId,String.valueOf(endpointId))
        cloud20.addEndpoint(utils.getServiceAdminToken(), tenant.id, endpointTemplate)
        utils.addTenantToDomain(id, tenant.id)
        EndpointList endpoints = utils.getEndpointsByDomain(id)

        then:
        endpoints != null
        endpoints.endpoint.tenantId.contains(tenantId)

        cleanup:
        cloud11.deleteUser(user.id)
        utils.deleteDomain(id)
        cloud20.deleteTenant(utils.getServiceAdminToken(), user.nastId)
        cloud20.deleteTenant(utils.getServiceAdminToken(), id)
    }
}
