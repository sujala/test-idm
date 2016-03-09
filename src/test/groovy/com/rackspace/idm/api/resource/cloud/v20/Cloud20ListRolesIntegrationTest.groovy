package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.RoleService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.RoleList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD

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

    @Autowired
    RoleService roleService

    @Autowired
    ApplicationRoleDao applicationRoleDao;

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

    def "only service admins can see roles with weight 50"() {
        given:
        def role50 = utils.createRole()
        def roleEntity = roleService.getRoleByName(role50.name)
        roleEntity.rsWeight = 50
        applicationRoleDao.updateClientRole(roleEntity)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "service admin lists roles"
        def response = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD))
        def roleList = response.getEntity(RoleList).value

        then: "found"
        roleList.role.find { it -> it.name == role50.name}.id == role50.id

        when: "identity admin lists roles"
        response = cloud20.listRoles(utils.getToken(identityAdmin.username))
        roleList = response.getEntity(RoleList).value

        then: "not found"
        roleList.role.find { it -> it.name == role50.name} == null

        when: "user admin lists roles"
        response = cloud20.listRoles(utils.getToken(userAdmin.username))
        roleList = response.getEntity(RoleList).value

        then: "not found"
        roleList.role.find { it -> it.name == role50.name} == null

        when: "user manage lists roles"
        response = cloud20.listRoles(utils.getToken(userManage.username))
        roleList = response.getEntity(RoleList).value

        then: "not found"
        roleList.role.find { it -> it.name == role50.name} == null

        when: "default user lists roles"
        def responseDefaultUser = cloud20.listRoles(utils.getToken(defaultUser.username))

        then: "not found"
        responseDefaultUser.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteRole(role50)
    }

}
