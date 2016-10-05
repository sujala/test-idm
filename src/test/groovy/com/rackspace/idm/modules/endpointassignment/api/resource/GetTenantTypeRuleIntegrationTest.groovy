package com.rackspace.idm.modules.endpointassignment.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypeEndpointRule
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.rackspace.idm.modules.endpointassignment.dao.GlobalRuleDao
import com.rackspace.idm.modules.endpointassignment.dao.LdapTenantTypeRuleRepository
import com.rackspace.idm.modules.endpointassignment.entity.Rule
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule
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

/**
 * This test assumes the standard Identity Admin (auth) has the appropriate authorization to manage endpoint rules and
 * that the base endpoints 1003 and 1026 exist in the base data set.
 */
class GetTenantTypeRuleIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    GlobalRuleDao globalRuleDao

    @Autowired
    LdapTenantTypeRuleRepository ldapTenantTypeRuleRepository;

    def setup() {
        cloud20.addUserRole(specificationServiceAdminToken, specificationIdentityAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
    }

    def "Get Tenant Type Rule: requires role"() {
        def newAdmin = createIdentityAdmin()
        def token = authenticate(newAdmin.username)
        def nonexistantRule =  testUtils.getRandomUUID()

        when: "get non-existant rule w/o role"
        def response = cloud20.getEndpointAssignmentRule(token, nonexistantRule)

        then: "Forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "get non-existant rule w/ role"
        cloud20.addUserRole(specificationServiceAdminToken, newAdmin.id, Constants.IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID)
        def response2 = cloud20.getEndpointAssignmentRule(token, nonexistantRule)

        then: "Not Found"
        IdmAssert.assertOpenStackV2FaultResponse(response2, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "The specified rule does not exist")

        cleanup:
        deleteUserQuietly(newAdmin)
    }

    @Unroll
    def "Get Tenant Type Rule: with endpoints returns detail appropriately; detailLevel: #detailLevel, request: #request, accept: #accept"() {
        def rule = new TenantTypeEndpointRule().with {
            it.tenantType = "tenantType"
            it.description = "hi"
            it.endpointTemplates = new EndpointTemplateList()
            it.endpointTemplates.endpointTemplate = [v1Factory.createEndpointTemplate("1003", "whatever"), v1Factory.createEndpointTemplate("1026", "whatever")] as List<EndpointTemplate>
            it
        }

        def createdRule = utils.addEndpointTemplateAssignmentRule(specificationIdentityAdminToken, rule)
        def et1003WebEntity = utils.getEndpointTemplate("1003")
        def et1026WebEntity = utils.getEndpointTemplate("1026")

        when: "get the rule"
        def response = cloud20.getEndpointAssignmentRule(specificationIdentityAdminToken, createdRule.id, detailLevel, request, accept)

        then:
        response.status == HttpStatus.SC_OK
        def getRule = response.getEntity(TenantTypeEndpointRule)
        getRule.id == createdRule.id

        and: "base info correct"
        getRule.tenantType == "tenanttype"
        getRule.description == "hi"

        and: "templates returned correctly"
        getRule.endpointTemplates != null
        getRule.endpointTemplates.endpointTemplate.size() == 2
        def et1003 = getRule.endpointTemplates.endpointTemplate.find {it.id == 1003}
        verifyTemplateData(et1003, et1003WebEntity, detailLevel)

        def et1026 = getRule.endpointTemplates.endpointTemplate.find {it.id == 1026}
        verifyTemplateData(et1026, et1026WebEntity, detailLevel)

        cleanup:
        deleteRuleQuietly(createdRule)

        where:
        detailLevel                             | request                         | accept
        ""                                      | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        null                                    | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        RuleDetailLevelEnum.MINIMUM.getLevel()  | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        RuleDetailLevelEnum.BASIC.getLevel()    | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        ""                                      | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        null                                    | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        RuleDetailLevelEnum.MINIMUM.getLevel()  | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        RuleDetailLevelEnum.BASIC.getLevel()    | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
    }

    def "Get Tenant Type Rule: returns 400 when provide invalid response detail"() {
        when: "try to get the rule with invalid responseDetailLevel param"
        def response = cloud20.getEndpointAssignmentRule(specificationIdentityAdminToken, "randomid", "invalidResponseDetail")

        then: "get 400"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "responseDetail: Invalid value")
    }

    /**
     * Verifies the endpoint template returned by the get call only includes the data
     * @param actual
     * @param expected
     */
    def void verifyTemplateData(actual, expected, detailLevel = null) {
        assert actual != null
        assert actual.id == expected.id

        if (StringUtils.isBlank(detailLevel) || RuleDetailLevelEnum.fromString(detailLevel) == RuleDetailLevelEnum.MINIMUM) {
            assert actual.region == null
            assert actual.enabled == null
            assert actual.publicURL == null
            assert actual.name == null
            assert actual.type == null
        } else if (detailLevel == detailLevel) {
            assert actual.region == expected.region
            assert actual.enabled == expected.enabled
            assert actual.publicURL == expected.publicURL
            assert actual.name == expected.name
            assert actual.type == expected.type
        } else {
            assert 1==0 //just fail
        }

        //these should always be null
        assert actual.adminURL == null
        assert actual.internalURL == null
        assert actual.assignmentType == null
        assert actual.default == null
        assert actual.global == null
        assert actual.serviceId == null
        assert actual.version == null
    }

    def deleteRuleQuietly(rule) {
        try {
            globalRuleDao.deleteEndpointAssignmentRule(rule.id)
        } catch (Exception ex) {
            // Eat the exception since just cleaning up
        }
    }
}
