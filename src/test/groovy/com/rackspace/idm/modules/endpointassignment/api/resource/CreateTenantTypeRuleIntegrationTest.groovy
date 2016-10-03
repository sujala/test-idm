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
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType

/**
 * This test presumes the standard Identity Admin (auth) has the appropriate authorization to manage endpoint rules and
 * that the base endpoints 1003 and 1026
 */
class CreateTenantTypeRuleIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    GlobalRuleDao globalRuleDao

    @Autowired
    LdapTenantTypeRuleRepository ldapTenantTypeRuleRepository;

    def setup() {
        cloud20.addUserRole(specificationServiceAdminToken, specificationIdentityAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
    }

    def "Create Tenant Type Rule: requires role"() {
        def newAdmin = createIdentityAdmin()
        def token = authenticate(newAdmin.username)
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.description = "a description"
            it
        }
        when: "create rule w/o role"
        def response = cloud20.addEndpointAssignmentRule(token, rule)

        then: "Forbidden"
        response.status == HttpStatus.SC_FORBIDDEN

        when: "create rule w/ role"
        cloud20.addUserRole(specificationServiceAdminToken, newAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
        def response2 = cloud20.addEndpointAssignmentRule(token, rule)

        then: "Forbidden"
        response2.status == HttpStatus.SC_CREATED

        cleanup:
        deleteRuleQuietly(rule)
        deleteUserQuietly(newAdmin)
    }

    @Unroll
    def "Create Tenant Type Rule: without endpoints; request:#request, accept:#accept"() {
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.description = "a description"
            it
        }

        when: "create rule"
        def response = cloud20.addEndpointAssignmentRule(specificationIdentityAdminToken, rule, request, accept)

        then: "rule created"
        response.status == HttpStatus.SC_CREATED
        def resultRule = response.getEntity(TenantTypeEndpointRule)

        and: "tenant type is lowercased"
        resultRule.tenantType == StringUtils.lowerCase("tenantType")

        and: "other props set correctly"
        resultRule.description == "a description"
        resultRule.endpointTemplates == null

        and: "rule exists in backend"
        TenantTypeRule backendRule = ldapTenantTypeRuleRepository.getById(resultRule.id)
        backendRule != null
        backendRule.tenantType == resultRule.tenantType
        backendRule.description == resultRule.description
        backendRule.endpointTemplateIds.size() == 0

        and: "can get rule via global dao"
        Rule typeAgnosticRule = globalRuleDao.getById(resultRule.id)
        typeAgnosticRule != null
        typeAgnosticRule.id == resultRule.id

        cleanup:
        deleteRuleQuietly(resultRule)

        where:
        request | accept
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "create Tenant Type Rule: with endpoints ignores duplicates and description optional; request:#request, accept:#accept"() {
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.endpointTemplates = new EndpointTemplateList()
            it.endpointTemplates.endpointTemplate = [v1Factory.createEndpointTemplate("1003", "whatever"), v1Factory.createEndpointTemplate("1026", "whatever"), v1Factory.createEndpointTemplate("1026", "another")] as List<EndpointTemplate>
            it
        }

        when: "create rule"
        def response = cloud20.addEndpointAssignmentRule(specificationIdentityAdminToken, rule, request, accept)

        then: "rule created"
        response.status == HttpStatus.SC_CREATED
        def resultRule = response.getEntity(TenantTypeEndpointRule)
        resultRule.tenantType == "tenanttype"

        and: "description can be null"
        resultRule.description == null

        and: "templates set correctly without duplicates"
        resultRule.endpointTemplates != null
        resultRule.endpointTemplates.endpointTemplate.size() == 2
        resultRule.endpointTemplates.endpointTemplate.find {it.id == 1003} != null
        resultRule.endpointTemplates.endpointTemplate.find {it.id == 1026} != null

        and: "rule exists in backend"
        TenantTypeRule backendRule = ldapTenantTypeRuleRepository.getById(resultRule.id)
        backendRule != null
        backendRule.tenantType == resultRule.tenantType
        backendRule.description == resultRule.description
        backendRule.endpointTemplateIds.size() == resultRule.endpointTemplates.endpointTemplate.size()
        backendRule.endpointTemplateIds.find {it == "1003"} != null
        backendRule.endpointTemplateIds.find {it == "1026"} != null

        and: "can get rule via global dao"
        Rule typeAgnosticRule = globalRuleDao.getById(resultRule.id)
        typeAgnosticRule != null
        typeAgnosticRule.id == resultRule.id

        cleanup:
        deleteRuleQuietly(resultRule)

        where:
        request | accept
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
    }

    @Unroll
    def "Create Tenant Type: invalid requests return correct error: #test"() {
        when: "create rule"
        def response = cloud20.addEndpointAssignmentRule(specificationIdentityAdminToken, rule)

        then: "rule created"
        response.status == HttpStatus.SC_BAD_REQUEST
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, expectedErrorMessage)

        where:
        rule | expectedErrorMessage | test
        new TenantTypeEndpointRule().with {
            it.tenantType = null
            it.description = "a description"
            it
        } | "tenantType: may not be null" | "null tenant"
        new TenantTypeEndpointRule().with {
            it.tenantType = ""
            it.description = "a description"
            it
        } | "tenantType: length must be between 1 and 15" | "empty string tenant"
        new TenantTypeEndpointRule().with {
            it.tenantType = "averylongtenantnamegreaterthan15characters"
            it.description = "a description"
            it
        } | "tenantType: length must be between 1 and 15" | "empty string tenant"
        new TenantTypeEndpointRule().with {
            it.tenantType = "\$"
            it.description = "a description"
            it
        } | "tenantType: can only contain alphanumeric characters" | "non alphanumeric tenant"
        new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.description = RandomStringUtils.randomAlphanumeric(256)
            it
        } | "description: length must be between 0 and 255" | "description greater than 255"
    }

    def deleteRuleQuietly(rule) {
        try {
            globalRuleDao.deleteEndpointAssignmentRule(rule.id)
        } catch (Exception ex) {
            // Eat the exception since just cleaning up
        }
    }
}
