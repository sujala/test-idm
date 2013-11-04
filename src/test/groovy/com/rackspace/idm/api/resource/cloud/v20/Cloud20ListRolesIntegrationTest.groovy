package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import org.springframework.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 11/4/13
 * Time: 1:35 PM
 * To change this template use File | Settings | File Templates.
 */
class Cloud20ListRolesIntegrationTest extends RootIntegrationTest{

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId

    def "List roles"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def serviceName = utils.getRandomUUID("listRoleService")
        def service = v1Factory.createService(serviceName, serviceName)
        def roleName = utils.getRandomUUID("role")
        def role = v1Factory.createRole(roleName)

        when:
        def identityAdminToken = utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)
        def createService = utils.createService(utils.getServiceAdminToken(), service)
        role.serviceId = createService.id
        def createRole = utils.createRole(identityAdminToken, role)
        def listRoles = utils.listRoles(identityAdminToken, createService.id, "0", "1")

        then:
        createService != null
        createRole != null
        listRoles != null
        listRoles.role.size() == 1

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteRole(createRole)
        utils.deleteService(createService)
    }

    def "Allow pagination on list roles"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def serviceName = utils.getRandomUUID("listRoleService")
        def service = v1Factory.createService(serviceName, serviceName)
        def listRoles = new ArrayList()

        when:
        def identityAdminToken = utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)
        def createService = utils.createService(utils.getServiceAdminToken(), service)
        for(def i = 0; i < roleList; i ++){
            def role = v1Factory.createRole(utils.getRandomUUID("role".concat(i.toString())))
            role.serviceId = createService.id
            def createRole = utils.createRole(identityAdminToken, role)
            listRoles.add(createRole)
        }
        def roles = utils.listRoles(identityAdminToken, createService.id, marker.toString(), limit.toString())

        then:
        createService != null
        roles != null
        roles.role.size() == size

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        for(def role : listRoles){
            utils.deleteRole(role)
        }
        utils.deleteService(createService)

        where:
        roleList | marker | limit | size
        2        | 0      | 2     | 2
        3        | 1      | 2     | 2
        4        | 1      | 3     | 3
        5        | 3      | 2     | 2
        5        | 4      | 2     | 1
    }
}
