package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.util.JSONReaderForRoles
import org.apache.commons.configuration.Configuration
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE

class Cloud20ListRolesIntegrationTest extends RootIntegrationTest{

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId
    @Shared JSONReaderForRoles readerForRoles = new JSONReaderForRoles()

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
        def response = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, "1000")
        def roleList = response.getEntity(RoleList).value

        then: "found"
        roleList.role.find { it -> it.name == role50.name}.id == role50.id

        when: "identity admin lists roles"
        response = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, "1000")
        roleList = response.getEntity(RoleList).value

        then: "not found"
        roleList.role.find { it -> it.name == role50.name} == null

        when: "user admin lists roles"
        response = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, "1000")
        roleList = response.getEntity(RoleList).value

        then: "not found"
        roleList.role.find { it -> it.name == role50.name} == null

        when: "user manage lists roles"
        response = cloud20.listRoles(utils.getToken(userManage.username), null, null, "1000")
        roleList = response.getEntity(RoleList).value

        then: "not found"
        roleList.role.find { it -> it.name == role50.name} == null

        when: "default user lists roles"
        def responseDefaultUser = cloud20.listRoles(utils.getToken(defaultUser.username), null, null, "1000")

        then: "not found"
        responseDefaultUser.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteRole(role50)
    }

    def "test listing available roles by weight and serviceId"() {
        given:
        def role0 = utils.createRole()
        def roleEntity = roleService.getRoleByName(role0.name)
        roleEntity.rsWeight = 0
        applicationRoleDao.updateClientRole(roleEntity)

        def role50 = utils.createRole()
        roleEntity = roleService.getRoleByName(role50.name)
        roleEntity.rsWeight = 50
        applicationRoleDao.updateClientRole(roleEntity)

        def role100 = utils.createRole()
        roleEntity = roleService.getRoleByName(role100.name)
        roleEntity.rsWeight = 100
        applicationRoleDao.updateClientRole(roleEntity)

        def role500 = utils.createRole()
        roleEntity = roleService.getRoleByName(role500.name)
        roleEntity.rsWeight = 500
        applicationRoleDao.updateClientRole(roleEntity)

        def role750 = utils.createRole()
        roleEntity = roleService.getRoleByName(role750.name)
        roleEntity.rsWeight = 750
        applicationRoleDao.updateClientRole(roleEntity)

        def role900 = utils.createRole()
        roleEntity = roleService.getRoleByName(role900.name)
        roleEntity.rsWeight = 900
        applicationRoleDao.updateClientRole(roleEntity)

        def role1000 = utils.createRole()
        roleEntity = roleService.getRoleByName(role1000.name)
        roleEntity.rsWeight = 1000
        applicationRoleDao.updateClientRole(roleEntity)

        def role2000 = utils.createRole()
        roleEntity = roleService.getRoleByName(role2000.name)
        roleEntity.rsWeight = 2000
        applicationRoleDao.updateClientRole(roleEntity)

        def role2500 = utils.createRole()
        roleEntity = roleService.getRoleByName(role2500.name)
        roleEntity.rsWeight = 2500
        applicationRoleDao.updateClientRole(roleEntity)

        def roles = [role0, role50, role100, role500, role750, role900, role1000, role2000, role2500]
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "service admin lists roles"
        def response = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), IDENTITY_SERVICE_ID, null, "1000")
        def roleList = response.getEntity(RoleList).value

        then:
        roleList.role.find { it -> it.name == role0.name} == null
        roleList.role.find { it -> it.name == role50.name}.id == role50.id
        roleList.role.find { it -> it.name == role100.name}.id == role100.id
        roleList.role.find { it -> it.name == role500.name}.id == role500.id
        roleList.role.find { it -> it.name == role750.name}.id == role750.id
        roleList.role.find { it -> it.name == role900.name}.id == role900.id
        roleList.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList.role.find { it -> it.name == role2500.name}.id == role2500.id

        when: "identity admin lists roles"
        response = cloud20.listRoles(utils.getToken(identityAdmin.username), IDENTITY_SERVICE_ID, null, "1000")
        roleList = response.getEntity(RoleList).value

        then:
        roleList.role.find { it -> it.name == role0.name} == null
        roleList.role.find { it -> it.name == role50.name} == null
        roleList.role.find { it -> it.name == role100.name} == null
        roleList.role.find { it -> it.name == role500.name}.id == role500.id
        roleList.role.find { it -> it.name == role750.name}.id == role750.id
        roleList.role.find { it -> it.name == role900.name}.id == role900.id
        roleList.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList.role.find { it -> it.name == role2500.name}.id == role2500.id

        when: "user admin lists roles"
        response = cloud20.listRoles(utils.getToken(userAdmin.username), IDENTITY_SERVICE_ID, null, "1000")
        roleList = response.getEntity(RoleList).value

        then:
        roleList.role.find { it -> it.name == role0.name} == null
        roleList.role.find { it -> it.name == role50.name} == null
        roleList.role.find { it -> it.name == role100.name} == null
        roleList.role.find { it -> it.name == role500.name} == null
        roleList.role.find { it -> it.name == role750.name} == null
        roleList.role.find { it -> it.name == role900.name}.id == role900.id
        roleList.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList.role.find { it -> it.name == role2500.name}.id == role2500.id

        when: "user manage lists roles"
        response = cloud20.listRoles(utils.getToken(userManage.username), IDENTITY_SERVICE_ID, null, "1000")
        roleList = response.getEntity(RoleList).value

        then:
        roleList.role.find { it -> it.name == role0.name} == null
        roleList.role.find { it -> it.name == role50.name} == null
        roleList.role.find { it -> it.name == role100.name} == null
        roleList.role.find { it -> it.name == role500.name} == null
        roleList.role.find { it -> it.name == role750.name} == null
        roleList.role.find { it -> it.name == role900.name} == null
        roleList.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList.role.find { it -> it.name == role2500.name}.id == role2500.id

        when: "default user lists roles"
        def responseDefaultUser = cloud20.listRoles(utils.getToken(defaultUser.username), IDENTITY_SERVICE_ID, null, "1000")

        then:
        responseDefaultUser.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        for (def role : roles) {
            applicationRoleDao.deleteClientRole(applicationRoleDao.getRoleByName(role.name))
        }
    }

    def "test listing available roles by weight and name"() {
        given:
        def service = utils.createService()

        def role0 = utils.createRole(service)
        def roleEntity = roleService.getRoleByName(role0.name)
        roleEntity.rsWeight = 0
        applicationRoleDao.updateClientRole(roleEntity)

        def role50 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role50.name)
        roleEntity.rsWeight = 50
        applicationRoleDao.updateClientRole(roleEntity)

        def role100 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role100.name)
        roleEntity.rsWeight = 100
        applicationRoleDao.updateClientRole(roleEntity)

        def role500 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role500.name)
        roleEntity.rsWeight = 500
        applicationRoleDao.updateClientRole(roleEntity)

        def role750 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role750.name)
        roleEntity.rsWeight = 750
        applicationRoleDao.updateClientRole(roleEntity)

        def role900 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role900.name)
        roleEntity.rsWeight = 900
        applicationRoleDao.updateClientRole(roleEntity)

        def role1000 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role1000.name)
        roleEntity.rsWeight = 1000
        applicationRoleDao.updateClientRole(roleEntity)

        def role2000 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role2000.name)
        roleEntity.rsWeight = 2000
        applicationRoleDao.updateClientRole(roleEntity)

        def role2500 = utils.createRole(service)
        roleEntity = roleService.getRoleByName(role2500.name)
        roleEntity.rsWeight = 2500
        applicationRoleDao.updateClientRole(roleEntity)

        def roles = [role0, role50, role100, role500, role750, role900, role1000, role2000, role2500]
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        when: "service admin lists roles"
        def response0 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role0.name)
        def roleList0 = response0.getEntity(RoleList).value
        def response50 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role50.name)
        def roleList50 = response50.getEntity(RoleList).value
        def response100 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role100.name)
        def roleList100 = response100.getEntity(RoleList).value
        def response500 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role500.name)
        def roleList500 = response500.getEntity(RoleList).value
        def response750 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role750.name)
        def roleList750 = response750.getEntity(RoleList).value
        def response900 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role900.name)
        def roleList900 = response900.getEntity(RoleList).value
        def response1000 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role1000.name)
        def roleList1000 = response1000.getEntity(RoleList).value
        def response2000 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role2000.name)
        def roleList2000 = response2000.getEntity(RoleList).value
        def response2500 = cloud20.listRoles(utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD), null, null, null, role2500.name)
        def roleList2500 = response2500.getEntity(RoleList).value

        then:
        roleList0.role.find { it -> it.name == role0.name} == null
        roleList0.role.size() == 0
        roleList50.role.find { it -> it.name == role50.name}.id == role50.id
        roleList50.role.size() == 1
        roleList100.role.find { it -> it.name == role100.name}.id == role100.id
        roleList100.role.size() == 1
        roleList500.role.find { it -> it.name == role500.name}.id == role500.id
        roleList500.role.size() == 1
        roleList750.role.find { it -> it.name == role750.name}.id == role750.id
        roleList750.role.size() == 1
        roleList900.role.find { it -> it.name == role900.name}.id == role900.id
        roleList900.role.size() == 1
        roleList1000.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList1000.role.size() == 1
        roleList2000.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList2000.role.size() == 1
        roleList2500.role.find { it -> it.name == role2500.name}.id == role2500.id
        roleList2500.role.size() == 1

        when: "identity admin lists roles"
        response0 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role0.name)
        roleList0 = response0.getEntity(RoleList).value
        response50 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role50.name)
        roleList50 = response50.getEntity(RoleList).value
        response100 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role100.name)
        roleList100 = response100.getEntity(RoleList).value
        response500 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role500.name)
        roleList500 = response500.getEntity(RoleList).value
        response750 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role750.name)
        roleList750 = response750.getEntity(RoleList).value
        response900 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role900.name)
        roleList900 = response900.getEntity(RoleList).value
        response1000 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role1000.name)
        roleList1000 = response1000.getEntity(RoleList).value
        response2000 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role2000.name)
        roleList2000 = response2000.getEntity(RoleList).value
        response2500 = cloud20.listRoles(utils.getToken(identityAdmin.username), null, null, null, role2500.name)
        roleList2500 = response2500.getEntity(RoleList).value

        then:
        roleList0.role.find { it -> it.name == role0.name} == null
        roleList0.role.size() == 0
        roleList50.role.find { it -> it.name == role50.name} == null
        roleList50.role.size() == 0
        roleList100.role.find { it -> it.name == role100.name} == null
        roleList100.role.size() == 0
        roleList500.role.find { it -> it.name == role500.name}.id == role500.id
        roleList500.role.size() == 1
        roleList750.role.find { it -> it.name == role750.name}.id == role750.id
        roleList750.role.size() == 1
        roleList900.role.find { it -> it.name == role900.name}.id == role900.id
        roleList900.role.size() == 1
        roleList1000.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList1000.role.size() == 1
        roleList2000.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList2000.role.size() == 1
        roleList2500.role.find { it -> it.name == role2500.name}.id == role2500.id
        roleList2500.role.size() == 1

        when: "user admin lists roles"
        response0 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role0.name)
        roleList0 = response0.getEntity(RoleList).value
        response50 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role50.name)
        roleList50 = response50.getEntity(RoleList).value
        response100 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role100.name)
        roleList100 = response100.getEntity(RoleList).value
        response500 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role500.name)
        roleList500 = response500.getEntity(RoleList).value
        response750 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role750.name)
        roleList750 = response750.getEntity(RoleList).value
        response900 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role900.name)
        roleList900 = response900.getEntity(RoleList).value
        response1000 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role1000.name)
        roleList1000 = response1000.getEntity(RoleList).value
        response2000 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role2000.name)
        roleList2000 = response2000.getEntity(RoleList).value
        response2500 = cloud20.listRoles(utils.getToken(userAdmin.username), null, null, null, role2500.name)
        roleList2500 = response2500.getEntity(RoleList).value

        then:
        roleList0.role.find { it -> it.name == role0.name} == null
        roleList0.role.size() == 0
        roleList50.role.find { it -> it.name == role50.name} == null
        roleList50.role.size() == 0
        roleList100.role.find { it -> it.name == role100.name} == null
        roleList100.role.size() == 0
        roleList500.role.find { it -> it.name == role500.name} == null
        roleList500.role.size() == 0
        roleList750.role.find { it -> it.name == role750.name} == null
        roleList750.role.size() == 0
        roleList900.role.find { it -> it.name == role900.name}.id == role900.id
        roleList900.role.size() == 1
        roleList1000.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList1000.role.size() == 1
        roleList2000.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList2000.role.size() == 1
        roleList2500.role.find { it -> it.name == role2500.name}.id == role2500.id
        roleList2500.role.size() == 1

        when: "user manage lists roles"
        response0 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role0.name)
        roleList0 = response0.getEntity(RoleList).value
        response50 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role50.name)
        roleList50 = response50.getEntity(RoleList).value
        response100 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role100.name)
        roleList100 = response100.getEntity(RoleList).value
        response500 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role500.name)
        roleList500 = response500.getEntity(RoleList).value
        response750 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role750.name)
        roleList750 = response750.getEntity(RoleList).value
        response900 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role900.name)
        roleList900 = response900.getEntity(RoleList).value
        response1000 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role1000.name)
        roleList1000 = response1000.getEntity(RoleList).value
        response2000 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role2000.name)
        roleList2000 = response2000.getEntity(RoleList).value
        response2500 = cloud20.listRoles(utils.getToken(userManage.username), null, null, null, role2500.name)
        roleList2500 = response2500.getEntity(RoleList).value

        then:
        roleList0.role.find { it -> it.name == role0.name} == null
        roleList0.role.size() == 0
        roleList50.role.find { it -> it.name == role50.name} == null
        roleList50.role.size() == 0
        roleList100.role.find { it -> it.name == role100.name} == null
        roleList100.role.size() == 0
        roleList500.role.find { it -> it.name == role500.name} == null
        roleList500.role.size() == 0
        roleList750.role.find { it -> it.name == role750.name} == null
        roleList750.role.size() == 0
        roleList900.role.find { it -> it.name == role900.name} == null
        roleList900.role.size() == 0
        roleList1000.role.find { it -> it.name == role1000.name}.id == role1000.id
        roleList1000.role.size() == 1
        roleList2000.role.find { it -> it.name == role2000.name}.id == role2000.id
        roleList2000.role.size() == 1
        roleList2500.role.find { it -> it.name == role2500.name}.id == role2500.id
        roleList2500.role.size() == 1

        when: "default user lists roles"
        def responseDefaultUser = cloud20.listRoles(utils.getToken(defaultUser.username))

        then:
        responseDefaultUser.status == 403

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        for (def role : roles) {
            applicationRoleDao.deleteClientRole(applicationRoleDao.getRoleByName(role.name))
        }
        utils.deleteService(service)
    }

    def "list roles returns 400 when you query with serviceId and roleId together"() {
        when:
        def response = cloud20.listRoles(utils.getServiceAdminToken(), IDENTITY_SERVICE_ID, null, null, "roleName")

        then:
        response.status == 400
    }

    @Unroll
    def "service admins can list global roles for self but not other sevice admins"() {
        given:
        def serviceAdminToken = utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)

        when: "list roles for self"
        def response = cloud20.listUserGlobalRoles(serviceAdminToken, Constants.SERVICE_ADMIN_ID)

        then: "success"
        response.status == 200

        when: "list roles for other service admin"
        response = cloud20.listUserGlobalRoles(serviceAdminToken, Constants.SERVICE_ADMIN_ID_2)

        then: "403 Forbidden"
        response.status == 403
    }

    @Unroll
    def "identity admins can list global roles for self but not for other identity admins or service admins"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def identityAdmin2 = utils.createIdentityAdmin()
        def token = utils.getToken(identityAdmin.username)

        when: "list roles for self"
        def response = cloud20.listUserGlobalRoles(token, identityAdmin.id)

        then: "success"
        response.status == 200

        when: "list roles for other identity admin"
        response = cloud20.listUserGlobalRoles(token, identityAdmin2.id)

        then: "403 Forbidden"
        response.status == 403

        when: "list roles for other service admin"
        response = cloud20.listUserGlobalRoles(token, Constants.SERVICE_ADMIN_ID)

        then: "403 Forbidden if feature flag is enabled"
        response.status == 403

        cleanup:
        utils.deleteUsers(identityAdmin, identityAdmin2)
    }

    @Unroll
    def "Get global roles for user with RCN role using query param 'apply_rcn_roles' - apply_rcn_roles = #applyRcnRoles, size = #size, accept = #accept" () {
        given: "A new user admin"
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Update domain to have an RCN
        def domain = v2Factory.createDomain(domainId, domainId, true, null, null, testUtils.getRandomRCN())
        utils.updateDomain(domainId, domain)

        when: "Create new RCN role"
        def role = v2Factory.createRole(false, RoleAssignmentEnum.GLOBAL, RoleTypeEnum.RCN, ["*"]).with {
            it.serviceId = SERVERS_SERVICE_ID
            it.otherAttributes = null
            it
        }
        def createRoleResponse = cloud20.createRole(utils.getIdentityAdminToken(), role)
        def roleEntity = createRoleResponse.getEntity(Role).value

        then: "Assert new created RCN role"
        createRoleResponse.status == HttpStatus.SC_CREATED

        when: "Add global RCN role to user"
        def addRoleToUserResponse = cloud20.addUserRole(utils.getIdentityAdminToken(), userAdmin.id, roleEntity.id)

        then: "Assert role added to user"
        addRoleToUserResponse.status == HttpStatus.SC_OK

        when: "List user's global roles using 'apply_rcn_roles' query param"
        def userGlobalRolesResponse = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), userAdmin.id, null, applyRcnRoles, accept)
        def globalRoles = getRoleListEntity(userGlobalRolesResponse)

        then:
        assert globalRoles.role.find({it.id == USER_ADMIN_ROLE_ID}) != null

        if (!applyRcnRoles) {
            Role rcnRole = globalRoles.role.find({it.id == roleEntity.id})
            assert rcnRole != null
            assert rcnRole.roleType == RoleTypeEnum.RCN
            assert rcnRole.types.type == ["*"]
        }
        globalRoles.role.size == size

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteRole(roleEntity)

        where:
        applyRcnRoles       | size | accept
        true                | 1    | MediaType.APPLICATION_XML_TYPE
        true                | 1    | MediaType.APPLICATION_JSON_TYPE
        "True"              | 1    | MediaType.APPLICATION_XML_TYPE
        "TRUE"              | 1    | MediaType.APPLICATION_XML_TYPE
        false               | 2    | MediaType.APPLICATION_XML_TYPE
        false               | 2    | MediaType.APPLICATION_JSON_TYPE
        "False"             | 2    | MediaType.APPLICATION_XML_TYPE
        "FALSE"             | 2    | MediaType.APPLICATION_XML_TYPE
        "invalid1"          | 2    | MediaType.APPLICATION_XML_TYPE
        "invalid~-_.!*'()," | 2    | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "Get global roles for user with RCN roles using 'serviceId' query param - serviceId = #serviceId size = #size, accept = #accept" () {
        given: "A new user admin"
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Update domain to have an RCN
        def domain = v2Factory.createDomain(domainId, domainId, true, null, null, "RCN-123-123-123")
        utils.updateDomain(domainId, domain)

        when: "Create new RCN role"
        def role = v2Factory.createRole(false, RoleAssignmentEnum.GLOBAL, RoleTypeEnum.RCN, ["*"]).with {
            it.serviceId = SERVERS_SERVICE_ID
            it.otherAttributes = null
            it
        }
        def createRoleResponse = cloud20.createRole(utils.getIdentityAdminToken(), role)
        def roleEntity = createRoleResponse.getEntity(Role).value

        then: "Assert new created RCN role"
        createRoleResponse.status == HttpStatus.SC_CREATED

        when: "Add global RCN role to user"
        def addRoleToUserResponse = cloud20.addUserRole(utils.getIdentityAdminToken(), userAdmin.id, roleEntity.id)

        then: "Assert role added to user"
        addRoleToUserResponse.status == HttpStatus.SC_OK

        when: "List user's global roles"
        def userGlobalRolesResponse = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), userAdmin.id, serviceId, null, accept)
        def globalRoles = getRoleListEntity(userGlobalRolesResponse)

        then:
        if (serviceId == null || serviceId == SERVERS_SERVICE_ID) {
            Role rcnRole = globalRoles.role.find({it.id == roleEntity.id})
            assert rcnRole != null
            assert rcnRole.roleType == RoleTypeEnum.RCN
            assert rcnRole.types.type == ["*"]
        }
        if (serviceId == null || serviceId == IDENTITY_SERVICE_ID) {
            assert globalRoles.role.find({it.id == USER_ADMIN_ROLE_ID}) != null
        }
        globalRoles.role.size == size

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteRole(roleEntity)

        where:
        serviceId           | size | accept
        SERVERS_SERVICE_ID  | 1    | MediaType.APPLICATION_XML_TYPE
        SERVERS_SERVICE_ID  | 1    | MediaType.APPLICATION_JSON_TYPE
        IDENTITY_SERVICE_ID | 1    | MediaType.APPLICATION_XML_TYPE
        IDENTITY_SERVICE_ID | 1    | MediaType.APPLICATION_JSON_TYPE
        null                | 2    | MediaType.APPLICATION_XML_TYPE
        null                | 2    | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Get global roles for user with RCN roles - serviceId = #serviceId, apply_rcn_roles = #applyRcnRoles, size = #size" () {
        given: "A new user admin"
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Update domain to have an RCN
        def domain = v2Factory.createDomain(domainId, domainId, true, null, null, "RCN-123-123-123")
        utils.updateDomain(domainId, domain)

        when: "Create new RCN role"
        def role = v2Factory.createRole(false, RoleAssignmentEnum.GLOBAL, RoleTypeEnum.RCN, ["*"]).with {
            it.serviceId = SERVERS_SERVICE_ID
            it.otherAttributes = null
            it
        }
        def createRoleResponse = cloud20.createRole(utils.getIdentityAdminToken(), role)
        def roleEntity = createRoleResponse.getEntity(Role).value

        then: "Assert new created RCN role"
        createRoleResponse.status == HttpStatus.SC_CREATED

        when: "Add global RCN role to user"
        def addRoleToUserResponse = cloud20.addUserRole(utils.getIdentityAdminToken(), userAdmin.id, roleEntity.id)

        then: "Assert role added to user"
        addRoleToUserResponse.status == HttpStatus.SC_OK

        when: "List user's global roles"
        def userGlobalRolesResponse = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), userAdmin.id, serviceId, applyRcnRoles)
        def globalRoles = getRoleListEntity(userGlobalRolesResponse)

        then:
        if (serviceId == IDENTITY_SERVICE_ID) {
            assert globalRoles.role.find({it.id == USER_ADMIN_ROLE_ID}) != null
        } else if (serviceId == SERVERS_SERVICE_ID && !applyRcnRoles) {
            Role rcnRole = globalRoles.role.find({it.id == roleEntity.id})
            assert rcnRole != null
            assert rcnRole.roleType == RoleTypeEnum.RCN
            assert rcnRole.types.type == ["*"]
        }

        globalRoles.role.size == size

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteRole(roleEntity)

        where:
        serviceId           | applyRcnRoles       | size
        SERVERS_SERVICE_ID  | true                | 0
        IDENTITY_SERVICE_ID | true                | 1
        SERVERS_SERVICE_ID  | false               | 1
        IDENTITY_SERVICE_ID | false               | 1
    }

    @Unroll
    def "Get global roles for user with no RCN roles - serviceId = #serviceId, apply_rcn_roles = #applyRcnRoles" () {
        given: "A new user admin"
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Update domain to have an RCN
        def domain = v2Factory.createDomain(domainId, domainId, true, null, null, "RCN-123-123-123")
        utils.updateDomain(domainId, domain)

        when: "List user's global roles"
        def userGlobalRolesResponse = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), userAdmin.id, serviceId, applyRcnRoles)
        def globalRoles = userGlobalRolesResponse.getEntity(RoleList).value

        then:
        if (size > 0) {
            assert globalRoles.role.find({it.id == USER_ADMIN_ROLE_ID}) != null
        }
        globalRoles.role.size == size

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        serviceId           | applyRcnRoles       | size
        SERVERS_SERVICE_ID  | true                | 0
        IDENTITY_SERVICE_ID | true                | 1
        SERVERS_SERVICE_ID  | false               | 0
        IDENTITY_SERVICE_ID | false               | 1
        null                | true                | 1
        null                | false               | 1
    }

    @Unroll
    def "Get global roles for fed user with no RCN roles - serviceId = #serviceId, apply_rcn_roles = #applyRcnRoles" () {
        given: "A new user admin"
        def userAdmin = cloud20.createCloudAccount(utils.getIdentityAdminToken())
        utils.domainRcnSwitch(userAdmin.domainId, "RCN-123-123-123")

        AuthenticateResponse fedUserAuthResponse = utils.createFederatedUserForAuthResponse(userAdmin.domainId)

        when: "admin list fed user's global roles"
        def userGlobalRolesResponse = cloud20.listUserGlobalRoles(utils.getIdentityAdminToken(), fedUserAuthResponse.user.id, serviceId, applyRcnRoles)
        def globalRoles = userGlobalRolesResponse.getEntity(RoleList).value

        then:
        if (size > 0) {
            assert globalRoles.role.find({it.id == DEFAULT_USER_ROLE_ID}) != null
        }
        globalRoles.role.size == size

        where:
        serviceId           | applyRcnRoles       | size
        SERVERS_SERVICE_ID  | true                | 0
        IDENTITY_SERVICE_ID | true                | 1
        SERVERS_SERVICE_ID  | false               | 0
        IDENTITY_SERVICE_ID | false               | 1
        null                | true                | 1
        null                | false               | 1
    }

    def "Fed user can get own global roles" () {
        given: "A new user admin"
        def userAdmin = cloud20.createCloudAccount(utils.getIdentityAdminToken())

        def expSecs = DEFAULT_SAML_EXP_SECS
        def username = testUtils.getRandomUUID("samlUser")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, userAdmin.domainId, [ROLE_RBAC1_NAME])
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        assert samlResponse.status == HttpStatus.SC_OK
        def fedUserAuthResponse = samlResponse.getEntity(AuthenticateResponse).value

        when: "fed user lists own global roles"
        def userGlobalRolesResponse = cloud20.listUserGlobalRoles(fedUserAuthResponse.token.id, fedUserAuthResponse.user.id)
        def globalRoles = userGlobalRolesResponse.getEntity(RoleList).value

        then:
        globalRoles.role.size() == 2
        assert globalRoles.role.find({it.id == DEFAULT_USER_ROLE_ID}) != null
        assert globalRoles.role.find({it.id == ROLE_RBAC1_ID}) != null

    }

    def getRoleListEntity(response) {
        def entity
        if (response.getType() == MediaType.APPLICATION_JSON_TYPE) {
            InputStream inputStream = IOUtils.toInputStream(response.getEntity(String))
            entity =  readerForRoles.readFrom(Role, null, null, null, null, inputStream)
        } else {
            entity = response.getEntity(RoleList).value
        }

        return entity
    }

}
