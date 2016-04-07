package com.rackspace.idm.validation

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory
import testHelpers.V2Factory

class RoleValidator20Test extends Specification {
    @Shared Validator20 validator20 = new Validator20();
    @Shared V2Factory v2Factory = new V2Factory();
    @Shared EntityFactory entityFactory = new EntityFactory();
    @Shared RoleService roleService;

    Configuration config;

    def setup() {
        config = Mock();
        roleService = Mock();

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
        validator20.validateRoleForCreation(v2Factory.createRole().with {it.name = null; it})

        then:
        BadRequestException e = thrown()
        e.getErrorCode() == ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE
    }

    def "validateRoleForCreation: Tests for duplicate role name"() {
        def role = v2Factory.createRole()

        when:
        validator20.validateRoleForCreation(role)

        then:
        1 * roleService.getRoleByName(role.name) >> entityFactory.createClientRole(role.name)
        BadRequestException e = thrown()
        e.getMessage().contains("already exists")
    }
}
