package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.Validator
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.Region
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/6/12
 * Time: 12:41 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultUserServiceGroovyTest extends Specification{
    @Shared DefaultUserService defaultUserService
    @Shared EndpointService endpointService
    @Shared TenantService tenantService
    @Shared DefaultCloudRegionService cloudRegionService

    @Shared Validator validator

    @Shared Configuration config
    @Shared UserDao userDao
    @Shared ScopeAccessDao scopeAccessDao

    def setupSpec(){
        defaultUserService = new DefaultUserService()
    }

    def "Add BaseUrl to user"() {
        given:
        setupMocks()
        User user = new User()
        user.id = "1"
        user.nastId = "123"
        user.mossoId = 123

        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.enabled = true
        baseUrl.def = false
        baseUrl.baseUrlId = 1
        baseUrl.openstackType = "NAST"

        Tenant tenant = new Tenant()
        tenant.addBaseUrlId("2")
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        defaultUserService.addBaseUrlToUser(1, user)

        then:
        1 * this.tenantService.updateTenant(_)
    }

    def "Add BaseUrl to user - dup baseUrl on tenant"() {
        given:
        setupMocks()
        User user = new User()
        user.id = "1"
        user.nastId = "123"
        user.mossoId = 123

        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.enabled = true
        baseUrl.def = false
        baseUrl.baseUrlId = 1
        baseUrl.openstackType = "NAST"

        Tenant tenant = new Tenant()
        tenant.addBaseUrlId("1")
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        defaultUserService.addBaseUrlToUser(1, user)

        then:
        thrown(BadRequestException)
    }

    def "Add BaseUrl to user - empty baseUrl on tenant"() {
        given:
        setupMocks()
        User user = new User()
        user.nastId = "123"

        CloudBaseUrl baseUrl = new CloudBaseUrl();
        baseUrl.enabled = true
        baseUrl.def = false
        baseUrl.baseUrlId = 1
        baseUrl.openstackType = "NAST"

        Tenant tenant = new Tenant()
        tenant.setTenantId("tenantId")

        endpointService.getBaseUrlById(_) >> baseUrl
        tenantService.getTenant(_) >> tenant

        when:
        defaultUserService.addBaseUrlToUser(1, user)

        then:
        1 * this.tenantService.updateTenant(_)

    }

    def "addUser keeps specified region for user"() {
        given:
        setupMocks()
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("notthesame", "cloud", true)
        userDao.isUsernameUnique(_) >> true

        when:
        defaultUserService.addUser(createUserEntity("region", true, "id", "email@email.com"))

        then:
        userDao.addUser(_) >> { arg1 ->
            assert(arg1.region[0].equals("region"))
        }
    }

    def "addUser adds region to user if not present"() {
        given:
        setupMocks()
        cloudRegionService.getDefaultRegion(_) >> createRegionEntity("region", "cloud", true)
        userDao.isUsernameUnique(_) >> true

        when:
        defaultUserService.addUser(createUserEntity(null, true, "id", "email@email.com"))

        then:
        userDao.addUser(_) >> { arg1 ->
            assert(arg1.region[0].equals("region"))
        }
    }

    def setupMocks(){
        endpointService = Mock()
        tenantService = Mock()
        cloudRegionService = Mock()
        userDao = Mock()
        config = Mock()
        validator = Mock()
        scopeAccessDao = Mock()

        defaultUserService.endpointService = endpointService
        defaultUserService.tenantService = tenantService
        defaultUserService.userDao = userDao
        defaultUserService.cloudRegionService = cloudRegionService
        defaultUserService.config = config
        defaultUserService.validator = validator
        defaultUserService.scopeAccessDao = scopeAccessDao
    }

    def createUserEntity(String region, boolean enabled, String id, String email) {
        new User().with {
            it.region = region
            it.enabled = enabled
            it.id = id
            it.email = email
            return it
        }
    }

    def createRegionEntity(String name, String cloud, boolean isDefault) {
        new Region().with {
            it.name = name
            it.cloud = cloud
            it.isDefault = isDefault
            return it
        }
    }
}
