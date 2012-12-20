package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.pagination.PaginatorContext
import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.CustomerDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.ApplicationService
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 19/12/12
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */
class DefaultApplicationServiceTest extends Specification {

    @Shared randomness = UUID.randomUUID()
    @Shared sharedRandom

    @Shared ApplicationService service
    @Shared ScopeAccessDao scopeAccessDao
    @Shared ApplicationDao applicationDao
    @Shared CustomerDao customerDao
    @Shared UserDao userDao
    @Shared TenantDao tenantDao
    @Shared ApplicationRoleDao applicationRoleDao
    @Shared TenantRoleDao tenantRoleDao


    def setupSpec() {
        service = new DefaultApplicationService()
        sharedRandom = ("$randomness").replace("-", "")
    }

    def setup() {
        scopeAccessDao = Mock()
        applicationDao = Mock()
        customerDao = Mock()
        userDao = Mock()
        tenantDao = Mock()
        applicationRoleDao = Mock()
        tenantRoleDao = Mock()

        service.scopeAccessDao = scopeAccessDao
        service.applicationDao = applicationDao
        service.customerDao = customerDao
        service.userDao = userDao
        service.tenantDao = tenantDao
        service.applicationRoleDao = applicationRoleDao
        service.tenantRoleDao = tenantRoleDao
    }

    def "getting available clientRoles paged calls applicationRoleDao method"() {
        given:
        def contextMock = Mock(PaginatorContext)

        when:
        service.getAvailableClientRolesPaged("applicationId", 0, 10, 1000)
        service.getAvailableClientRolesPaged(0, 10, 1000)

        then:
        1 * applicationRoleDao.getAvailableClientRolesPaged(0, 10, 1000) >> contextMock
        1 * applicationRoleDao.getAvailableClientRolesPaged("applicationId", 0, 10, 1000) >> contextMock
    }
}
