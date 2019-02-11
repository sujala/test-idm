package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/28/13
 * Time: 1:21 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultRoleServiceTest extends RootServiceTest {

    @Shared DefaultRoleService service

    @Shared ApplicationService mockApplicationService
    @Shared ApplicationRoleDao mockApplicationRoleDao
    @Shared ClientRole mockClientRole
    @Shared Application mockApplication
    @Shared Configuration mockConfig

    def setupSpec() {
        service = new DefaultRoleService()
    }

    def setup() {
        mockApplicationService()
        mockApplicationRoleDao()
        mockConfig()
        mockClientRole = Mock()
        mockApplication = Mock()
        identityConfig = Mock(IdentityConfig)
        staticConfig = Mock(IdentityConfig.StaticConfig)
        identityConfig.getStaticConfig() >> staticConfig
        service.identityConfig = identityConfig
    }

    def "Get role by name" () {
        given:
        def roleName = "roleName"

        when:
        def result = service.getRoleByName(roleName)

        then:
        1 * mockApplicationRoleDao.getRoleByName(roleName) >> mockClientRole
        result == mockClientRole
    }

    def "Get super user admin role" () {
        when:
        def result = service.getSuperUserAdminRole()

        then:
        1 * mockApplicationRoleDao.getRoleByName(IdentityUserTypeEnum.SERVICE_ADMIN.roleName) >> mockClientRole
        result == mockClientRole
    }

    def "Get identity admin role" () {
        when:
        def result = service.getIdentityAdminRole()

        then:
        1 * mockApplicationRoleDao.getRoleByName(IdentityUserTypeEnum.IDENTITY_ADMIN.roleName) >> mockClientRole
        result == mockClientRole
    }

    def "Get user admin role" () {
        when:
        def result = service.getUserAdminRole()

        then:
        1 * mockApplicationRoleDao.getRoleByName(IdentityUserTypeEnum.USER_ADMIN.roleName) >> mockClientRole
        result == mockClientRole
    }

    def "Get user manage role" () {
        when:
        def result = service.getUserManageRole()

        then:
        1 * mockApplicationRoleDao.getRoleByName(IdentityUserTypeEnum.USER_MANAGER.roleName) >> mockClientRole
        result == mockClientRole
    }

    def "Get default role" () {
        when:
        def result = service.getDefaultRole()

        then:
        1 * mockApplicationRoleDao.getRoleByName(IdentityUserTypeEnum.DEFAULT_USER.roleName) >> mockClientRole
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

    def "check if role is assigned: #numberOfRoleAssociation" () {
        given:
        def roleId = "roleId"
        mockTenantRoleDao(service)
        tenantRoleDao.getCountOfTenantRoleAssignmentsByRoleId(roleId) >> numberOfRoleAssociation

        when:
        def result = service.isRoleAssigned(roleId)

        then:
        result == expectedResult

        where:
        numberOfRoleAssociation | expectedResult
        2                       |    true
        0                       |    false
        5                       |    true
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
