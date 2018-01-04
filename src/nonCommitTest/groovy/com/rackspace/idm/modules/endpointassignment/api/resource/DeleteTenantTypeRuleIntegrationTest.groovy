package com.rackspace.idm.modules.endpointassignment.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.modules.endpointassignment.dao.GlobalRuleDao
import com.rackspace.idm.modules.endpointassignment.dao.LdapTenantTypeRuleRepository
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.IdmAssert

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
        IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern(response2, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, Pattern.compile("The specified rule does not exist"))

        cleanup:
        deleteUserQuietly(newAdmin)
    }

    def "Delete Tenant Type Rule"() {
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = Constants.TENANT_TYPE_CLOUD
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

    def "Endpoint templates cannot be deleted while still associated with an assignment rule"() {
        given:
        def endpointTemplate = utils.createEndpointTemplate()
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = Constants.TENANT_TYPE_CLOUD
            it.description = RandomStringUtils.randomAlphanumeric(16)
            it.endpointTemplates = new EndpointTemplateList()
            it.endpointTemplates.endpointTemplate = []
            it.endpointTemplates.endpointTemplate << new EndpointTemplate().with {
                it.id = endpointTemplate.id
                it
            }
            it
        }
        def assignmentRule = utils.addEndpointTemplateAssignmentRule(specificationIdentityAdminToken, rule)
        endpointTemplate.enabled = false
        utils.updateEndpointTemplate(endpointTemplate, endpointTemplate.id.toString())

        when:
        def response = cloud20.deleteEndpointTemplate(utils.getServiceAdminToken(), endpointTemplate.id.toString())

        then:
        response.status == 403
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, 403, DefaultCloud20Service.ERROR_CANNOT_DELETE_ENDPOINT_TEMPLATE_IN_ASSIGNMENT_RULE_MESSAGE)

        when:
        def deleteRuleResponse = cloud20.deleteEndpointAssignmentRule(specificationIdentityAdminToken, assignmentRule.id)
        def deleteEndpointTemplateResponse = cloud20.deleteEndpointTemplate(utils.getServiceAdminToken(), endpointTemplate.id.toString())

        then:
        deleteRuleResponse.status == 204
        deleteEndpointTemplateResponse.status == 204
    }

}
