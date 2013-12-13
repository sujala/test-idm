package com.rackspace.idm.domain.service.impl
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.ApplicationService
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/28/13
 * Time: 1:21 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultRoleServiceTest extends Specification {

    @Shared DefaultRoleService service

    @Shared ApplicationService mockApplicationService
    @Shared ApplicationRoleDao mockApplicationRoleDao
    @Shared ClientRole mockClientRole
    @Shared Application mockApplication
    @Shared Configuration mockConfig;

    def setupSpec() {
        service = new DefaultRoleService()
    }

    def setup() {
        mockApplicationService()
        mockApplicationRoleDao()
        mockConfig()
        mockClientRole = Mock()
        mockApplication = Mock()
    }

    def "Get role by name" () {
        given:
        def roleName = "roleName"

        mockApplicationRoleDao.getRoleByName(roleName) >> mockClientRole

        when:
        def result = service.getRoleByName(roleName)

        then:
        result == mockClientRole
    }

    def "Get super user admin role" () {
        given:
        mockConfig.getString("cloudAuth.serviceAdminRole") >> "identity:service-admin"
        mockApplicationRoleDao.getRoleByName("identity:service-admin") >> mockClientRole

        when:
        def result = service.getSuperUserAdminRole()

        then:
        result == mockClientRole
    }

    def "Get identity admin role" () {
        given:
        mockConfig.getString("cloudAuth.adminRole") >> "identity:admin"
        mockApplicationRoleDao.getRoleByName("identity:admin") >> mockClientRole

        when:
        def result = service.getIdentityAdminRole()

        then:
        result == mockClientRole
    }

    def "Get user admin role" () {
        given:
        mockConfig.getString("cloudAuth.userAdminRole") >> "identity:user-admin"
        mockApplicationRoleDao.getRoleByName("identity:user-admin") >> mockClientRole

        when:
        def result = service.getUserAdminRole()

        then:
        result == mockClientRole
    }

    def "Get user manage role" () {
        given:
        mockConfig.getString("cloudAuth.userManagedRole") >> "identity:user-manage"
        mockApplicationRoleDao.getRoleByName("identity:user-manage") >> mockClientRole

        when:
        def result = service.getUserManageRole()

        then:
        result == mockClientRole
    }

    def "Get default role" () {
        given:
        mockConfig.getString("cloudAuth.userRole") >> "identity:default"
        mockApplicationRoleDao.getRoleByName("identity:default") >> mockClientRole

        when:
        def result = service.getDefaultRole()

        then:
        result == mockClientRole
    }

    def "Get compute:default role" () {
        given:
        mockConfig.getString("serviceName.cloudServers") >> "compute"
        mockApplicationService.getByName("compute") >> mockApplication
        mockApplication.getOpenStackType() >> "compute"
        mockApplicationRoleDao.getRoleByName("compute:default") >> mockClientRole

        when:
        def result = service.getComputeDefaultRole()

        then:
        result == mockClientRole
    }

    def "Get object-store:default role" () {
        given:
        mockConfig.getString("serviceName.cloudFiles") >> "object-store"
        mockApplicationService.getByName("object-store") >> mockApplication
        mockApplication.getOpenStackType() >> "object-store"
        mockApplicationRoleDao.getRoleByName("object-store:default") >> mockClientRole

        when:
        def result = service.getObjectStoreDefaultRole()

        then:
        result == mockClientRole
    }

    def mockApplicationService() {
        mockApplicationService = Mock()
        service.applicationService = mockApplicationService
    }

    def mockApplicationRoleDao() {
        mockApplicationRoleDao = Mock()
        service.applicationRoleDao = mockApplicationRoleDao
    }

    def mockConfig() {
        mockConfig = Mock()
        service.config = mockConfig
    }
}
