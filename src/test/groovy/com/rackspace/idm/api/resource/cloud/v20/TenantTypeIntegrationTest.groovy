package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.Constants
import com.rackspace.idm.validation.Validator20
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class TenantTypeIntegrationTest extends RootIntegrationTest {

    @Shared def serviceAdminToken
    @Shared def identityAdminToken
    @Shared def userAdminToken
    @Shared def defaultUserToken
    @Shared def userManageToken

    @Shared def domainId
    @Shared def identityAdmin
    @Shared def userAdmin
    @Shared def userManage
    @Shared def defaultUser

    static def DEFAULT_PASSWORD = "Password1"

    def setupSpec() {
        def sharedRandom = UUID.randomUUID().toString().replace('-',"")
        domainId = "domain1$sharedRandom"

        serviceAdminToken = cloud20.authenticatePassword("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        identityAdminToken = cloud20.authenticatePassword("auth", "auth123").getEntity(AuthenticateResponse).value.token.id

        cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("userAdmin1$sharedRandom", "display", "test@rackspace.com", true, null, domainId, "Password1"))
        userAdmin = cloud20.getUserByName(identityAdminToken, "userAdmin1$sharedRandom").getEntity(User).value
        userAdminToken = cloud20.authenticatePassword("userAdmin1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUser1$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUser = cloud20.getUserByName(userAdminToken, "defaultUser1$sharedRandom").getEntity(User).value
        defaultUserToken = cloud20.authenticatePassword("defaultUser1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUserWithManageRole$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        userManage = cloud20.getUserByName(userAdminToken, "defaultUserWithManageRole$sharedRandom").getEntity(User).value
        userManageToken = cloud20.authenticate("defaultUserWithManageRole$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id
    }

    def cleanupSpec() {
        cloud20.destroyUser(serviceAdminToken, userManage.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUser.getId())
        cloud20.destroyUser(serviceAdminToken, userAdmin.getId())
    }

    @Unroll
    def "TenantType name - globally unique across tenant types"() {
        given:
        def name = getRandomUUID("name")[0..15]
        TenantType tenantType = v2Factory.createTenantType(name, "description")

        when:
        utils.getServiceAdminToken()
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)
        TenantType createdTenantType = response.getEntity(TenantType)

        then:
        createdTenantType.name == tenantType.name
        createdTenantType.description == tenantType.description

        when:
        response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)

        then:
        response.status == SC_CONFLICT

        cleanup:
        cloud20.deleteTenantType(serviceAdminToken, name)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "TenantType name required and validate - tenant name = #name, contentType = #contentType, expected status = #expectedStatus"() {
        given:
        TenantType tenantType = v2Factory.createTenantType(name, "description")

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)

        then:
        response.status == expectedStatus

        cleanup:
        if (name != null) {
            cloud20.deleteTenantType(serviceAdminToken, name)
        }

        where:
        contentType                     | name                                              | expectedStatus
        MediaType.APPLICATION_XML_TYPE  | 'ttype101'                                        | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'ttype101'                                        | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'ttype_101'                                       | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'ttype_101'                                       | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'ttype-101'                                       | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'ttype-101'                                       | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | '101ttype'                                        | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | '101ttype'                                        | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'ttype'                                           | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'ttype'                                           | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | '101'                                             | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | '101'                                             | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'thisisok_'                                       | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'thisisok_'                                       | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'thisisok-'                                       | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'thisisok-'                                       | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | '_'                                               | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | '_'                                               | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | '-'                                               | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | '-'                                               | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'in$valid'                                        | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'in$valid'                                        | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | ''                                                | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | ''                                                | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | null                                              | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | null                                              | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'a' * Validator20.TENANT_TYPE_NAME_MAX_LENGTH + 1 | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'a' * Validator20.TENANT_TYPE_NAME_MAX_LENGTH + 1 | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'TTYPE'                                           | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'TTYPE'                                           | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype*101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype*101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype@101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype@101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype!101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype!101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype#101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype#101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype^101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype^101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype&101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype&101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype>101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype>101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype<101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype<101'                                       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttypë'                                           | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttypë'                                           | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | ' ttype_1'                                        | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | ' ttype_1'                                        | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype 1'                                         | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype 1'                                         | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'ttype_1 '                                        | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | 'ttype_1 '                                        | SC_BAD_REQUEST
    }

    @Unroll
    def "TenantType description required - max 255 characters, contentType = #contentType"() {
        given:
        def name = getRandomUUID("name")[0..15]
        TenantType tenantType = v2Factory.createTenantType(name, description)

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)

        then:
        response.status == expectedStatus

        cleanup:
        cloud20.deleteTenantType(serviceAdminToken, name)

        where:
        contentType                     | description     | expectedStatus
        MediaType.APPLICATION_XML_TYPE  | 'description'   | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'description'   | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'a' * 255       | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'a' * 255       | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'a' * 256       | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | 'a' * 256       | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | ''              | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | ''              | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | null            | SC_BAD_REQUEST
        MediaType.APPLICATION_JSON_TYPE | null            | SC_BAD_REQUEST
        MediaType.APPLICATION_XML_TYPE  | '&#abc123'      | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | '&#abc123'      | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | '\u0430'        | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | '\u0430'        | SC_CREATED
        MediaType.APPLICATION_XML_TYPE  | 'description 1' | SC_CREATED
        MediaType.APPLICATION_JSON_TYPE | 'description 1' | SC_CREATED
    }

    @Unroll
    def "tenantType service can only be consumed by service-admins"() {
        given:
        def name = getRandomUUID("name")
        TenantType tenantType = v2Factory.createTenantType(name, "description")

        when:
        def response = cloud20.addTenantType(token, tenantType, contentType, contentType)

        then:
        response.status == SC_FORBIDDEN

        when:
        response = cloud20.getTenantType(token, name)

        then:
        response.status == SC_FORBIDDEN

        when:
        response = cloud20.deleteTenantType(token, name)

        then:
        response.status == SC_FORBIDDEN

        when:
        response = cloud20.listTenantTypes(token, contentType)

        then:
        response.status == SC_FORBIDDEN

        where:
        contentType                     | token
        MediaType.APPLICATION_XML_TYPE  | identityAdminToken
        MediaType.APPLICATION_JSON_TYPE | identityAdminToken
        MediaType.APPLICATION_XML_TYPE  | userAdminToken
        MediaType.APPLICATION_JSON_TYPE | userAdminToken
        MediaType.APPLICATION_XML_TYPE  | defaultUserToken
        MediaType.APPLICATION_JSON_TYPE | defaultUserToken
        MediaType.APPLICATION_XML_TYPE  | userManageToken
        MediaType.APPLICATION_JSON_TYPE | userManageToken
    }

    @Unroll
    def "Create tenantType service response must include name and description"() {
        given:
        def name = getRandomUUID("name")[0..15]
        def description = "description"
        TenantType tenantType = v2Factory.createTenantType(name, description)

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)
        TenantType createdTenantType = response.getEntity(TenantType)

        then:
        createdTenantType.name == name
        createdTenantType.description == description

        cleanup:
        cloud20.deleteTenantType(serviceAdminToken, name)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Get tenantType service response must include name and description"() {
        given:
        def name = getRandomUUID("name")[0..15]
        def description = "description"
        TenantType tenantType = v2Factory.createTenantType(name, description)

        when:
        cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)
        def response = cloud20.getTenantType(serviceAdminToken, name, contentType)
        TenantType createdTenantType = response.getEntity(TenantType)

        then:
        createdTenantType.name == name
        createdTenantType.description == description

        cleanup:
        cloud20.deleteTenantType(serviceAdminToken, name)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "List tenantType service response must include name and description"() {
        given:
        def name = getRandomUUID("name")[0..15]
        def description = "description"
        TenantType tenantType = v2Factory.createTenantType(name, description)

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)
        response = cloud20.listTenantTypes(serviceAdminToken, contentType)
        TenantTypes tenantTypes = response.getEntity(TenantTypes)
        TenantType createdTenantType = tenantTypes.tenantType.findAll { it.name == name }.get(0)

        then:
        createdTenantType.name == name
        createdTenantType.description == description

        cleanup:
        cloud20.deleteTenantType(serviceAdminToken, name)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Delete tenantType service"() {
        given:
        def name = getRandomUUID("name")[0..15]
        TenantType tenantType = v2Factory.createTenantType(name, "description")

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)

        then:
        response.status == SC_CREATED

        when:
        response = cloud20.deleteTenantType(serviceAdminToken, name)

        then: "Return 204 if type is found and deleted"
        response.status == SC_NO_CONTENT

        when:
        response = cloud20.deleteTenantType(serviceAdminToken, name)

        then: "Return 404 if type does not exist"
        response.status == SC_NOT_FOUND

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "Cannot delete tenantType service that is referenced by tenant"() {
        given:
        def tenantTypeName = getRandomUUID("name")[0..15]
        TenantType tenantType = v2Factory.createTenantType(tenantTypeName, "description")

        def tenantId = getRandomUUID("tenant")
        def tenant = v2Factory.createTenant(tenantId, tenantId, [tenantTypeName])
        def contentType = MediaType.APPLICATION_JSON_TYPE

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)

        then:
        response.status == SC_CREATED

        when: "delete tenantType with existing tenant with tenantType reference"
        response = cloud20.addTenant(serviceAdminToken, tenant, contentType, contentType)
        def createdTenant = getEntity(response, Tenant)

        response = cloud20.deleteTenantType(serviceAdminToken, tenantTypeName)

        then:
        response.status == SC_BAD_REQUEST
        String errMsg = "TenantType with name ${tenantType.name} is referenced and cannot be deleted"
        response.getEntity(BadRequestFault).value.message == errMsg

        when: "delete tenantType when tenant with tenantType reference deleted"
        cloud20.deleteTenant(serviceAdminToken, createdTenant.id)
        response = cloud20.deleteTenantType(serviceAdminToken, tenantTypeName)

        then: "Return 204 if type is found and deleted"
        response.status == SC_NO_CONTENT
    }

    def "Cannot delete tenantType service that is referenced by RCN role"() {
        given:
        def tenantTypeName = getRandomUUID("name")[0..15]
        TenantType tenantType = v2Factory.createTenantType(tenantTypeName, "description")

        def serviceId = Constants.IDENTITY_SERVICE_ID
        def roleName = getRandomUUID("role")
        Types types = new Types().with {
            it.type = [tenantTypeName]
            it
        }
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = roleName
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }
        def contentType = MediaType.APPLICATION_JSON_TYPE

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)

        then:
        response.status == SC_CREATED

        when: "delete tenantType with existing role with tenantType reference"
        response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        Role role = getEntity(response, Role)

        response = cloud20.deleteTenantType(serviceAdminToken, tenantTypeName)

        then:
        response.status == SC_BAD_REQUEST
        String errMsg = "TenantType with name ${tenantType.name} is referenced and cannot be deleted"
        response.getEntity(BadRequestFault).value.message == errMsg

        when: "delete tenantType when role with tenantType reference deleted"
        cloud20.deleteRole(identityAdminToken, role.id)
        response = cloud20.deleteTenantType(serviceAdminToken, tenantTypeName)

        then: "Return 204 if type is found and deleted"
        response.status == SC_NO_CONTENT
    }

    def "Cannot delete tenantType service that is referenced by rule"() {
        given:
        def tenantTypeName = getRandomUUID("name")[0..15]
        TenantType tenantType = v2Factory.createTenantType(tenantTypeName, "description")

        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = tenantTypeName
            it.description = "description"
            it
        }
        def contentType = MediaType.APPLICATION_JSON_TYPE

        when:
        def response = cloud20.addTenantType(serviceAdminToken, tenantType, contentType, contentType)

        then:
        response.status == SC_CREATED

        when: "delete tenantType with existing rule with tenantType reference"
        response = cloud20.addEndpointAssignmentRule(identityAdminToken, rule)
        def createdRule = response.getEntity(TenantTypeEndpointRule)

        response = cloud20.deleteTenantType(serviceAdminToken, tenantTypeName)

        then:
        response.status == SC_BAD_REQUEST
        String errMsg = "TenantType with name ${tenantType.name} is referenced and cannot be deleted"
        response.getEntity(BadRequestFault).value.message == errMsg

        when: "delete tenantType when rule with tenantType reference deleted"
        cloud20.deleteEndpointAssignmentRule(identityAdminToken, createdRule.id)
        response = cloud20.deleteTenantType(serviceAdminToken, tenantTypeName)

        then: "Return 204 if type is found and deleted"
        response.status == SC_NO_CONTENT
    }

    @Unroll
    def "tenantType service can not be consumed by invalid token"() {
        given:
        def name = getRandomUUID("name")
        def token = "invalidToken"
        TenantType tenantType = v2Factory.createTenantType(name, "description")

        when:
        def response = cloud20.addTenantType(token, tenantType, contentType, contentType)

        then:
        response.status == SC_UNAUTHORIZED

        when:
        response = cloud20.deleteTenantType(token, name)

        then:
        response.status == SC_UNAUTHORIZED

        when:
        response = cloud20.listTenantTypes(token, contentType)

        then:
        response.status == SC_UNAUTHORIZED

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "tenantType service can not be consumed by revoked token"() {
        given:
        def name = getRandomUUID("name")
        def token = utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        utils.revokeToken(token)
        TenantType tenantType = v2Factory.createTenantType(name, "description")

        when:
        def response = cloud20.addTenantType(token, tenantType, contentType, contentType)

        then:
        response.status == SC_UNAUTHORIZED

        when:
        response = cloud20.deleteTenantType(token, name)

        then:
        response.status == SC_UNAUTHORIZED

        when:
        response = cloud20.listTenantTypes(token, contentType)

        then:
        response.status == SC_UNAUTHORIZED

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "tenantType service can not be consumed by valid token that has been modified"() {
        given:
        def name = getRandomUUID("name")
        def token = utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        if (truncate) {
            token = token[0..-2]
        } else {
            token = token + 'a'
        }
        TenantType tenantType = v2Factory.createTenantType(name, "description")

        when:
        def response = cloud20.addTenantType(token, tenantType, contentType, contentType)

        then:
        response.status == SC_UNAUTHORIZED

        when:
        response = cloud20.deleteTenantType(token, name)

        then:
        response.status == SC_UNAUTHORIZED

        when:
        response = cloud20.listTenantTypes(token, contentType)

        then:
        response.status == SC_UNAUTHORIZED

        where:
        contentType                     | truncate
        MediaType.APPLICATION_XML_TYPE  | false
        MediaType.APPLICATION_JSON_TYPE | false
        MediaType.APPLICATION_XML_TYPE  | true
        MediaType.APPLICATION_JSON_TYPE | true
    }
}
