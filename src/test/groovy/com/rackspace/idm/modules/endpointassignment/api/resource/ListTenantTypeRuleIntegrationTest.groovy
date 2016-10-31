package com.rackspace.idm.modules.endpointassignment.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointAssignmentRules
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.modules.endpointassignment.dao.GlobalRuleDao
import com.rackspace.idm.modules.endpointassignment.dao.LdapTenantTypeRuleRepository
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.IdentityFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType

/**
 * This test assumes the standard Identity Admin (auth) has the appropriate authorization to manage endpoint rules and
 * that the base endpoints 1003 and 1026 exist in the base data set.
 */
class ListTenantTypeRuleIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    GlobalRuleDao globalRuleDao

    @Autowired
    LdapTenantTypeRuleRepository ldapTenantTypeRuleRepository;

    def setup() {
        cloud20.addUserRole(specificationServiceAdminToken, specificationIdentityAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
    }

    def "List Tenant Type Rule: requires caller to have specific role"() {
        def newAdmin = createIdentityAdmin()
        def token = authenticate(newAdmin.username)

        when: "get rule list by caller w/o role"
        def response = cloud20.listEndpointAssignmentRules(token)

        then: "Forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "get rule list by caller w/ role"
        cloud20.addUserRole(specificationServiceAdminToken, newAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
        def response2 = cloud20.listEndpointAssignmentRules(token)

        then: "OK"
        response2.status == HttpStatus.SC_OK

        cleanup:
        deleteUserQuietly(newAdmin)
    }

    @Unroll
    def "List Tenant Type Rule: # rules returned depends on configuration request: #request, accept: #accept"() {
        def rule1 = new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.description = "hi"
            it.endpointTemplates = new EndpointTemplateList()
            it.endpointTemplates.endpointTemplate = [v1Factory.createEndpointTemplate("1003", "whatever"), v1Factory.createEndpointTemplate("1026", "whatever")] as List<EndpointTemplate>
            it
        }
        def createdRule1 = utils.addEndpointTemplateAssignmentRule(specificationIdentityAdminToken, rule1)

        def rule2 = new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.description = "hi"
            it.endpointTemplates = new EndpointTemplateList()
            it.endpointTemplates.endpointTemplate = [v1Factory.createEndpointTemplate("1003", "whatever"), v1Factory.createEndpointTemplate("1026", "whatever")] as List<EndpointTemplate>
            it
        }
        def createdRule2 = utils.addEndpointTemplateAssignmentRule(specificationIdentityAdminToken, rule2)

        when: "set max result < known number of rules"
        reloadableConfiguration.setProperty(IdentityConfig.MAX_CA_DIRECTORY_PAGE_SIZE_PROP, 1)
        def invalidResponse = cloud20.listEndpointAssignmentRules(specificationIdentityAdminToken, request, accept)

        then: "get 503"
        IdmAssert.assertOpenStackV2FaultResponse(invalidResponse, IdentityFault, HttpStatus.SC_SERVICE_UNAVAILABLE, "The search resulted in too many results")

        when: "get the rules w/ max size > max number rules"
        reloadableConfiguration.setProperty(IdentityConfig.MAX_CA_DIRECTORY_PAGE_SIZE_PROP, 1000)
        def response = cloud20.listEndpointAssignmentRules(specificationIdentityAdminToken, request, accept)

        then: "contains all the rules"
        response.status == HttpStatus.SC_OK
        def ruleList = response.getEntity(EndpointAssignmentRules)
        ruleList.getTenantTypeEndpointRules() != null
        ruleList.getTenantTypeEndpointRules().tenantTypeEndpointRule.size() >= 2 //would be >2 if pre-existing rules

        and: "contains rule 1 info similar to what's returned after creating rule"
        def foundRule1 = ruleList.getTenantTypeEndpointRules().tenantTypeEndpointRule.find {it.id == createdRule1.id}
        foundRule1 != null
        foundRule1.id == createdRule1.id
        foundRule1.description == createdRule1.description
        foundRule1.tenantType == createdRule1.tenantType
        foundRule1.endpointTemplates == null //templates not returned

        and: "contains rule 2 "
        ruleList.getTenantTypeEndpointRules().tenantTypeEndpointRule.find {it.id == createdRule2.id} != null

        cleanup:
        deleteRuleQuietly(createdRule1)
        deleteRuleQuietly(createdRule2)

        where:
        request                         | accept
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
    }

    def deleteRuleQuietly(rule) {
        try {
            globalRuleDao.deleteEndpointAssignmentRule(rule.id)
        } catch (Exception ex) {
            // Eat the exception since just cleaning up
        }
    }
}
