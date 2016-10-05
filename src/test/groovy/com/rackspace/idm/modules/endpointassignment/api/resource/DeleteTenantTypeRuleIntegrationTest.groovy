package com.rackspace.idm.modules.endpointassignment.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.modules.endpointassignment.dao.GlobalRuleDao
import com.rackspace.idm.modules.endpointassignment.dao.LdapTenantTypeRuleRepository
import com.rackspace.idm.modules.endpointassignment.entity.Rule
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType
import java.util.regex.Pattern

class DeleteTenantTypeRuleIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    GlobalRuleDao globalRuleDao

    @Autowired
    LdapTenantTypeRuleRepository ldapTenantTypeRuleRepository;

    def setup() {
        cloud20.addUserRole(specificationServiceAdminToken, specificationIdentityAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
    }

    def "Delete Tenant Type Rule: requires role"() {
        def newAdmin = createIdentityAdmin()
        def token = authenticate(newAdmin.username)
        def nonexistantRule =  testUtils.getRandomUUID()

        when: "delete non-existant rule w/o role"
        def response = cloud20.deleteEndpointAssignmentRule(token, nonexistantRule)

        then: "Forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "delete non-existant rule w/ role"
        cloud20.addUserRole(specificationServiceAdminToken, newAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
        def response2 = cloud20.deleteEndpointAssignmentRule(token, nonexistantRule)

        then: "Not Found"
        IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern(response2, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, Pattern.compile("Object .* not found"))

        cleanup:
        deleteUserQuietly(newAdmin)
    }

    def "Delete Tenant Type Rule"() {
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.description = "a description"
            it
        }

        //create a rule to delete
        def resultRule = utils.addEndpointTemplateAssignmentRule(specificationIdentityAdminToken, rule)
        assert ldapTenantTypeRuleRepository.getById(resultRule.id) != null

        when: "delete rule"
        def response = cloud20.deleteEndpointAssignmentRule(specificationIdentityAdminToken, resultRule.id)

        then: "rule deleted"
        response.status == HttpStatus.SC_NO_CONTENT

        and: "rule no longer exists in backend"
        ldapTenantTypeRuleRepository.getById(resultRule.id) == null
    }
}
