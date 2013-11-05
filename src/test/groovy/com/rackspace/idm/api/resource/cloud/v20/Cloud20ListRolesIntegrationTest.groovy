package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    Configuration config

    def "List roles"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when:
        def identityAdminToken = utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)
        def service = utils.createService()
        def createRole = utils.createRole(service)
        def listRoles = utils.listRoles(identityAdminToken, service.id, "0", "1")

        then:
        service != null
        createRole != null
        listRoles != null
        listRoles.role.size() == 1

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteRole(createRole)
        utils.deleteService(service)
    }

    def "Allow pagination on list roles"() {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def listRoles = new ArrayList()

        when:
        def identityAdminToken = utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)
        def service = utils.createService()
        for(def i = 0; i < roleList; i ++){
            def createRole = utils.createRole(service)
            listRoles.add(createRole)
        }
        def roles = utils.listRoles(identityAdminToken, service.id, marker.toString(), limit.toString())

        then:
        service != null
        roles != null
        roles.role.size() == size

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        for(def role : listRoles){
            utils.deleteRole(role)
        }
        utils.deleteService(service)

        where:
        roleList | marker | limit | size
        2        | 0      | 2     | 2
        3        | 1      | 2     | 2
        4        | 1      | 3     | 3
        5        | 3      | 2     | 2
        5        | 4      | 2     | 1
    }

    def "User manage should not be allowed to list the Identity:admin role" () {
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        String adminRole = config.getString('cloudAuth.adminRole')

        when:
        def userManageToken = utils.getToken(userManage.username, DEFAULT_PASSWORD)
        def roles = utils.listRoles(userManageToken, null, null, "1000")

        then:
        roles != null
        !roles.role.name.contains(adminRole)

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }
}
