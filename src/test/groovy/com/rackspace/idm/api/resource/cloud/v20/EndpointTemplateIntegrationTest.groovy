package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EndpointTemplateAssignmentTypeEnum
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.validation.Validator20
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.IdentityFault
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class EndpointTemplateIntegrationTest extends RootIntegrationTest {

    @Unroll
    def "Assert endpoint template attributes on retrieval"() {
        given: "An admin user and a new endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId)
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        cloud20.addEndpointTemplate(adminToken, endpointTemplate, acceptContentType)

        when: "Retrieving the endpoint template"
        def response = cloud20.getEndpointTemplate(adminToken, endpointTemplateId, acceptContentType)

        then: "Assert attributes"
        response.status == 200
        def getEndpointTemplate = getEndpointTemplateFromResponse(response, acceptContentType)
        getEndpointTemplate.id == endpointTemplate.id
        getEndpointTemplate.type == endpointTemplate.type
        getEndpointTemplate.name == endpointTemplate.name
        getEndpointTemplate.publicURL == endpointTemplate.publicURL
        getEndpointTemplate.serviceId == serviceId
        assert assertAssignmentTypeEnum(getEndpointTemplate, EndpointTemplateAssignmentTypeEnum.MOSSO.value(), acceptContentType)

        cleanup:
        cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Assert baseUrl type for list endpoint templates"() {
        given: "An admin user"
        def adminToken = utils.getIdentityAdminToken()

        when: "Retrieving the endpoint templates"
        def response = cloud20.listEndpointTemplates(adminToken, acceptContentType)

        then: "Assert all endpoint template's type"
        response.status == 200
        def listEndpointTemplates = getEndpointTemplateListFromResponse(response, acceptContentType)
        for (def endpointTemplate : listEndpointTemplates) {
            if (acceptContentType == MediaType.APPLICATION_XML_TYPE) {
                assert EndpointTemplateAssignmentTypeEnum.fromValue(endpointTemplate.assignmentType.value()) != null
            } else {
                assert EndpointTemplateAssignmentTypeEnum.fromValue(endpointTemplate["RAX-AUTH:assignmentType"]) != null
            }
        }

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Create endpoint template with service name and type"() {
        given: "Admin user and endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId)
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id

        when: "Create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate, acceptContentType)

        then: "Assert newly created endpoint template"
        response.status == 201
        def createdEndpointTemplate = getEndpointTemplateFromResponse(response, acceptContentType)
        createdEndpointTemplate.id == endpointTemplate.id
        createdEndpointTemplate.type == endpointTemplate.type
        createdEndpointTemplate.name == endpointTemplate.name
        createdEndpointTemplate.publicURL == endpointTemplate.publicURL
        createdEndpointTemplate.serviceId == serviceId
        assert assertAssignmentTypeEnum(createdEndpointTemplate, EndpointTemplateAssignmentTypeEnum.MOSSO.value(), acceptContentType)

        cleanup:
        cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)

        where:
        acceptContentType               | requestContentType              | _
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Create endpoint template with service name and type with no nast/mosso mapping"() {
        given: "Admin user, new service and endpoint template"
        def adminToken = utils.getServiceAdminToken()
        def serviceType = testUtils.getRandomUUID()
        def serviceName = testUtils.getRandomUUID()
        def service = v1Factory.createService(null, serviceName, serviceType)
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, serviceType, "http://publicUrl", serviceName)

        when: "Create a new service"
        def serviceResponse = cloud20.createService(adminToken, service)
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, serviceName)
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id

        then: "Assert created service"
        serviceResponse.status == 201
        listServiceResponse.status == 200
        serviceId != null

        when: "Create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate, acceptContentType)

        then: "Assert newly created endpoint template"
        response.status == 201
        def createdEndpointTemplate = getEndpointTemplateFromResponse(response, acceptContentType)
        createdEndpointTemplate.id == endpointTemplate.id
        createdEndpointTemplate.type == endpointTemplate.type
        createdEndpointTemplate.name == endpointTemplate.name
        createdEndpointTemplate.publicURL == endpointTemplate.publicURL
        assert assertAssignmentTypeEnum(createdEndpointTemplate, EndpointTemplateAssignmentTypeEnum.MANUAL.value(), acceptContentType)

        when: "Get endpoint template"
        def getResponse = cloud20.getEndpointTemplate(adminToken, endpointTemplateId, acceptContentType)

        then: "Assert retrieved endpoint template"
        def getEndpointTemplate = getEndpointTemplateFromResponse(getResponse, acceptContentType)
        getEndpointTemplate.id == endpointTemplate.id
        getEndpointTemplate.type == endpointTemplate.type
        getEndpointTemplate.name == endpointTemplate.name
        getEndpointTemplate.publicURL == endpointTemplate.publicURL
        assert assertAssignmentTypeEnum(getEndpointTemplate, EndpointTemplateAssignmentTypeEnum.MANUAL.value(), acceptContentType)

        cleanup:
        cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)
        cloud20.deleteService(adminToken, serviceId)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Create endpoint template with service id and assignment type"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def serviceType = serviceList.service[0].type
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null,"http://publicUrl", null, true, null, serviceId, assignmentType)

        when: "Create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate, acceptContentType)

        then: "Assert newly created endpoint template"
        response.status == 201
        def createdEndpointTemplate = getEndpointTemplateFromResponse(response, acceptContentType)
        createdEndpointTemplate.id == endpointTemplate.id
        createdEndpointTemplate.type == serviceType
        createdEndpointTemplate.publicURL == endpointTemplate.publicURL
        createdEndpointTemplate.serviceId == serviceId
        assert assertAssignmentTypeEnum(createdEndpointTemplate, assignmentType, acceptContentType)
        createdEndpointTemplate.name == null

        cleanup:
        cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)

        where:
        acceptContentType               | requestContentType              | assignmentType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | "MOSSO"
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | "MOSSO"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | "MOSSO"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | "MOSSO"
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | "NAST"
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | "NAST"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | "NAST"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | "NAST"
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | "MANUAL"
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE  | "MANUAL"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | "MANUAL"
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | "MANUAL"
    }

    def "Endpoint template cannot be created with name, type, service id and assignment type"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", "http://publicUrl", "cloudServers", true, null, serviceId, "MOSSO")

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert BadRequest"
        response.status == 400
        response.getEntity(IdentityFault).value.message == Validator20.ENDPOINT_TEMPLATE_EXTRA_ATTRIBUTES_ERROR_MSG
    }

    def "Endpoint template cannot be created with name, service id and assignment type"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null, "http://publicUrl", "cloudServers", true, null, serviceId, "MOSSO")

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert BadRequest"
        response.status == 400
        response.getEntity(IdentityFault).value.message == Validator20.ENDPOINT_TEMPLATE_EXTRA_ATTRIBUTES_ERROR_MSG
    }

    def "Endpoint template cannot be created with type, service id and assignment type"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", "http://publicUrl", null, true, null, serviceId, "MOSSO")

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert BadRequest"
        response.status == 400
        response.getEntity(IdentityFault).value.message == Validator20.ENDPOINT_TEMPLATE_EXTRA_ATTRIBUTES_ERROR_MSG
    }

    def "Endpoint template cannot be created with only serviceId"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null, "http://publicUrl", null, true, null, serviceId)

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert BadRequest"
        response.status == 400
        response.getEntity(IdentityFault).value.message == String.format(Validator20.ENDPOINT_TEMPLATE_ACCEPTABLE_ASSIGNMENT_TYPE_ERROR_MSG, Arrays.asList(EndpointTemplateAssignmentTypeEnum.values()))
    }

    def "Endpoint template cannot be created with only AssignmentType"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null, "http://publicUrl", null, true, null, null, "MOSSO")

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert BadRequest"
        response.status == 400
        response.getEntity(IdentityFault).value.message == Validator20.ENDPOINT_TEMPLATE_EMPTY_SERVICE_ID_ERROR_MSG
    }

    @Unroll
    def "Endpoint template cannot be created with invalid AssignmentType: XML"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = getEndpointTemplateRequestXML(endpointTemplateId, "serviceId", assignmentType, "http://publicUrl")

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert BadRequest"
        response.status == 400
        def expectedErrorMsg = String.format(Validator20.ENDPOINT_TEMPLATE_ACCEPTABLE_ASSIGNMENT_TYPE_ERROR_MSG, Arrays.asList(EndpointTemplateAssignmentTypeEnum.values()))
        response.getEntity(IdentityFault).value.message == expectedErrorMsg

        where:
        assignmentType | _
        "INVALID"      | _
        ""             | _
        null           | _

    }

    @Unroll
    def "Endpoint template cannot be created with invalid AssignmentType: JSON"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = getEndpointTemplateRequestJSON(endpointTemplateId, "serviceId", assignmentType, "http://publicUrl")

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE)

        then: "Assert BadRequest"
        response.status == 400
        def expectedErrorMsg = String.format(Validator20.ENDPOINT_TEMPLATE_ACCEPTABLE_ASSIGNMENT_TYPE_ERROR_MSG, Arrays.asList(EndpointTemplateAssignmentTypeEnum.values()))
        def entity = new JsonSlurper().parseText(response.getEntity(String))
        entity["badRequest"].message == expectedErrorMsg

        where:
        assignmentType | _
        "INVALID"      | _
        ""             | _
        null           | _
    }

    def "Endpoint template cannot be created with invalid ServiceId"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null, "http://publicUrl", null, true, null, testUtils.getRandomUUID(), "MOSSO")

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert NotFound"
        response.status == 404
    }

    def "Endpoint template cannot be created with service name and type if feature flag: feature.endpoint.template.disable.name.type = true"() {
        given: "An admin user and a endpoint template"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_PROP, true)
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", "http://publicUrl", "cloudServers", true)

        when: "Attempt to create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert BadRequest"
        response.status == 400
        response.getEntity(IdentityFault).value.message == Validator20.ENDPOINT_TEMPLATE_DISABLE_NAME_TYPE_ERROR_MSG

        cleanup:
        reloadableConfiguration.reset()
    }

    def "Delete endpoint template"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getIdentityAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null,"http://publicUrl", null, true, null, serviceId, "MOSSO")

        when: "Create a new endpoint template"
        def response = cloud20.addEndpointTemplate(adminToken, endpointTemplate)

        then: "Assert newly created endpoint template"
        response.status == 201

        when: "Delete endpoint tempalte"
        def deleteResponse = cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)

        then:
        deleteResponse.status == 204
    }

    def "Attempt to delete enabled endpoint template"() {
        given: "An admin user and a endpoint template"
        def adminToken = utils.getServiceAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null,"http://publicUrl", null, true, null, serviceId, "MOSSO")

        when: "Create and enable endpoint template"
        def createdResponse = cloud20.addEndpointTemplate(adminToken, endpointTemplate)
        endpointTemplate.enabled = true
        def updatedResponse = cloud20.updateEndpointTemplate(adminToken, endpointTemplateId, endpointTemplate)

        then: "Assert created and updated responses"
        createdResponse.status == 201
        updatedResponse.status == 200

        when: "Delete endpoint tempalte"
        def deleteResponse = cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)

        then:
        deleteResponse.status == 403

        cleanup:
        endpointTemplate.enabled = false
        cloud20.updateEndpointTemplate(adminToken, endpointTemplateId, endpointTemplate)
        cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)
    }

    def "Attempt to delete an endpoint template associated to a tenant"() {
        given: "An admin user, tenant and endpoint template"
        def adminToken = utils.getServiceAdminToken()
        def endpointTemplateId = testUtils.getRandomIntegerString()
        def listServiceResponse = cloud20.listServices(adminToken, MediaType.APPLICATION_XML_TYPE, "cloudServers")
        def serviceList = listServiceResponse.getEntity(ServiceList).value
        def serviceId = serviceList.service[0].id
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, null,"http://publicUrl", null, true, null, serviceId, "MOSSO")
        def tenantId = testUtils.getRandomIntegerString()
        def tenant = v2Factory.createTenant(tenantId, tenantId)

        when: "Create endpoint template and tenant"
        def createEndpointResponse = cloud20.addEndpointTemplate(adminToken, endpointTemplate)
        def createdTenantResponse = cloud20.addTenant(adminToken, tenant)

        then: "Assert created and updated response"
        createEndpointResponse.status == 201
        createdTenantResponse.status == 201

        when: "Assigning endpoint to tenant"
        def addEndpointResponse = cloud20.addEndpoint(adminToken, tenantId, endpointTemplate)

        then: "Assert adding endpoint to tenant response"
        addEndpointResponse.status == 200

        when: "Attempt to delete endpoint tempalte"
        def deleteResponse = cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)

        then:
        deleteResponse.status == 403

        cleanup:
        cloud20.deleteTenant(adminToken, tenantId)
        cloud20.deleteEndpointTemplate(adminToken, endpointTemplateId)
    }

    def assertAssignmentTypeEnum(endpointTemplate, assignmentType, acceptContentType) {
        def type = null
        if (acceptContentType == MediaType.APPLICATION_XML_TYPE) {
            type = endpointTemplate.assignmentType.value()
        } else {
            type = endpointTemplate["RAX-AUTH:assignmentType"]
        }
        return type == assignmentType
    }

    def getEndpointTemplateFromResponse(response, acceptContentType) {
        def endpointTemplate = null
        if (acceptContentType == MediaType.APPLICATION_XML_TYPE) {
            endpointTemplate = response.getEntity(EndpointTemplate).value
        } else {
            def entity = new JsonSlurper().parseText(response.getEntity(String))
            endpointTemplate = entity["OS-KSCATALOG:endpointTemplate"]
        }
        return endpointTemplate
    }

    def getEndpointTemplateListFromResponse(response, acceptContentType) {
        def list = new ArrayList()
        if (acceptContentType == MediaType.APPLICATION_XML_TYPE) {
            def endpointTemplateList = response.getEntity(EndpointTemplateList).value
            list = endpointTemplateList.endpointTemplate
        } else {
            def endpointTemplateList = new JsonSlurper().parseText(response.getEntity(String))
            list = endpointTemplateList["OS-KSCATALOG:endpointTemplates"]
        }
        return list
    }

    // Methods getEndpointTemplateRequestXML and getEndpointTemplateRequestJSON are needed in order to provide invalid
    // values for assignmentType.

    def getEndpointTemplateRequestXML(id, serviceId, assignmentType, publicUrl){
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
               "<endpointTemplate xmlns=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" " +
               "xmlns:RAX-AUTH=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\" " +
               "id=\"$id\" serviceId=\"$serviceId\" RAX-AUTH:assignmentType=\"$assignmentType\" publicURL=\"$publicUrl\">" +
               "</endpointTemplate>"
    }

    def getEndpointTemplateRequestJSON(id, serviceId, assignmentType, publicUrl) {
        return "{\"OS-KSCATALOG:endpointTemplate\": {" +
                "\"id\":\"$id\"," +
                "\"serviceId\":\"$serviceId\"," +
                "\"RAX-AUTH:assignmentType\":\"$assignmentType\"," +
                "\"publicURL\": \"$publicUrl\"}}"
    }
}
