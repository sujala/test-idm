package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types
import org.apache.commons.configuration.PropertiesConfiguration;
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class RoleConverterCloudV20Test extends RootServiceTest {
    @Shared RoleConverterCloudV20 converter

    def setupSpec() {
        converter = new RoleConverterCloudV20()
        converter.objFactories = new JAXBObjectFactories()
        converter.mapper = new DozerBeanMapper()
        def reloadableConfig= new PropertiesConfiguration();
        def staticConfig = new PropertiesConfiguration();
        converter.identityConfig = new IdentityConfig(staticConfig, reloadableConfig)
    }

    def setup() {
        mockConfiguration(converter)
    }

    def "can convert tenantRole to jaxb role"() {
        given:
        def roleType = RoleTypeEnum.PROPAGATE
        def serviceId = "serviceId"
        def tenantRole = entityFactory.createTenantRole().with {
            it.roleType = roleType
            it.clientId = serviceId
            return it
        }

        when:
        def result = converter.toRole(tenantRole)

        then:
        result.roleType == roleType
        result.serviceId == serviceId
    }

    def "can convert clientRole to jaxb role"() {
        given:
        def weight = 100
        def serviceId = "serviceId"
        def assignment = RoleAssignmentEnum.TENANT.value()
        def roleType = RoleTypeEnum.RCN.value()
        def tenantTypes = ['type']

        def clientRole = entityFactory.createClientRole().with {
            it.rsWeight = weight
            it.clientId = serviceId
            it.assignmentType = assignment
            it.roleType = roleType
            it.tenantTypes = tenantTypes
            return it
        }

        when:
        def result = converter.toRoleFromClientRole(clientRole)

        then:
        result.propagate == false
        result.serviceId == serviceId
        result.assignment == RoleAssignmentEnum.fromValue(assignment)
        result.roleType == RoleTypeEnum.valueOf(roleType)
        result.types.type == tenantTypes
    }

    def "can convert jaxb role to clientRole"() {
        given:
        def propagate = false
        def serviceId = "serviceId"
        def tenantTypes = ['type']

        def jaxbRole = v2Factory.createRole()
        jaxbRole.propagate = propagate
        jaxbRole.serviceId = serviceId
        jaxbRole.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
        jaxbRole.assignment = RoleAssignmentEnum.TENANT
        jaxbRole.roleType = RoleTypeEnum.RCN
        Types type = new Types()
        type.type.addAll(tenantTypes)
        jaxbRole.setTypes(type)

        when:
        def result = converter.fromRole(jaxbRole, serviceId);

        then:
        result.rsWeight == RoleLevelEnum.LEVEL_1000.levelAsInt
        result.propagate == propagate
        result.clientId == serviceId
        result.assignmentType == RoleAssignmentEnum.TENANT.value()
        result.roleType == RoleTypeEnum.RCN
        result.tenantTypes as HashSet == tenantTypes as HashSet
    }

    @Unroll
    def "jaxb sets weight to #expectedWeight when administrator role set to #adminRole when converting from role to clientRole"() {
        given:
        def jaxbRole = v2Factory.createRole().with {it.administratorRole = adminRole; it}

        when:
        def result = converter.fromRole(jaxbRole, "clientId");

        then:
        result.rsWeight == expectedWeight.levelAsInt

        where:
        adminRole | expectedWeight
        IdentityUserTypeEnum.SERVICE_ADMIN.roleName | RoleLevelEnum.LEVEL_50
        IdentityUserTypeEnum.IDENTITY_ADMIN.roleName | RoleLevelEnum.LEVEL_500
        IdentityUserTypeEnum.USER_MANAGER.roleName | RoleLevelEnum.LEVEL_1000
    }

    def "convert jaxb RoleList to TenantRoles"() {
        given:
        def jaxbRole = v2Factory.createRole("roleName")
        def jaxbRoleList = v2Factory.createRoleList()
        jaxbRoleList.getRole().add(jaxbRole)

        when:
        def result = converter.toTenantRoles(jaxbRoleList)

        then:
        result.size() == 1
        result.get(0).getName() == "roleName"
    }

    def "convert jaxb RoleList to TenantRoles when RoleList is null"() {
        when:
        def result = converter.toTenantRoles(null)

        then:
        result == null
    }
}
