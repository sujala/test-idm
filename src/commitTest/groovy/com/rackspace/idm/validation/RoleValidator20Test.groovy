package com.rackspace.idm.validation

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory
import testHelpers.V2Factory

class RoleValidator20Test extends Specification {
    @Shared Validator20 validator20 = new Validator20()
    @Shared V2Factory v2Factory = new V2Factory()
    @Shared EntityFactory entityFactory = new EntityFactory()

    @Shared RoleService roleService
    @Shared RequestContextHolder requestContextHolder
    @Shared RequestContext requestContext

    Configuration config

    def setup() {
        config = Mock()
        roleService = Mock()
        requestContextHolder = Mock()
        requestContext = Mock()
        requestContextHolder.getRequestContext() >> requestContext

        validator20.config = config
        validator20.roleService = roleService
    }

    def "validateRoleForCreation: Role can't be null"() {
        when:
        validator20.validateRoleForCreation(null)

        then:
        BadRequestException e = thrown()
        e.getErrorCode() == ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE
    }


    def "validateRoleForCreation: Role name is required"() {
        when:
        validator20.validateRoleForCreation(v2Factory.createRole().with {
            it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
            it.name = null
            it
        })

        then:
        BadRequestException e = thrown()
        e.getErrorCode() == ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE
    }

    def "validateRoleForCreation: Tests for duplicate role name"() {
        def role = v2Factory.createRole().with {
            it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
            it
        }

        when:
        validator20.validateRoleForCreation(role)

        then:
        1 * roleService.getRoleByName(role.name) >> entityFactory.createClientRole(role.name)
        BadRequestException e = thrown()
        e.getMessage().contains("already exists")
    }

    def "validate role for create - if service id is null set it"(){
        given:
        def role = v2Factory.createRole("name", null, null).with {
            it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
            it
        }

        when:
        validator20.validateRoleForCreation(role)

        then:
        1 * config.getString("cloudAuth.globalRoles.clientId") >> "123"
        role.serviceId == "123"
    }
}
