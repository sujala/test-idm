package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.EndpointService
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.Cloud20Methods
import testHelpers.RootIntegrationTest

import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE

class Cloud20UpdateEndpointTemplateIntegrationTest extends RootIntegrationTest {

    @Autowired Cloud20Methods methods
    @Autowired EndpointService endpointService

    def "only service admins can update endpoint templates"() {
        given:
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "compute", publicUrl, "cloudServers", false, "ORD"))
        def endpointForUpdate = v1Factory.createEndpointTemplateForUpdate(true, false, true)
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(utils.createDomain())
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        when: "service admin"
        def updateResponse = cloud20.updateEndpointTemplate(utils.getServiceAdminToken(), endpointTemplateId, endpointForUpdate)

        then: "success"
        updateResponse.status == 200

        when: "identity admin"
        updateResponse = cloud20.updateEndpointTemplate(utils.getToken(identityAdmin.username), endpointTemplateId, endpointForUpdate)

        then: "error"
        updateResponse.status == 403

        when: "user admin"
        updateResponse = cloud20.updateEndpointTemplate(utils.getToken(userAdmin.username), endpointTemplateId, endpointForUpdate)

        then: "error"
        updateResponse.status == 403

        when: "user-manage user"
        updateResponse = cloud20.updateEndpointTemplate(utils.getToken(userManage.username), endpointTemplateId, endpointForUpdate)

        then: "error"
        updateResponse.status == 403

        when: "default user"
        updateResponse = cloud20.updateEndpointTemplate(utils.getToken(defaultUser.username), endpointTemplateId, endpointForUpdate)

        then: "error"
        updateResponse.status == 403

        cleanup:
        endpointService.deleteBaseUrl(endpointTemplateId)
        utils.deleteUsers(users)
    }

    @Unroll
    def "if ID is provided in update endpoint template call, the ID must match the path, accept = #acceptType, request = #requestType"() {
        given:
        def endpointTemplateIdInt = testUtils.getRandomInteger()
        def endpointTemplateId = endpointTemplateIdInt.toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "compute", publicUrl, "cloudServers", false, "ORD"))
        def endpointForUpdate = v1Factory.createEndpointTemplateForUpdate(true, false, true)

        when: "update the endpoint template with matching ID"
        endpointForUpdate.id = endpointTemplateIdInt
        def updateResponse = cloud20.updateEndpointTemplate(utils.getServiceAdminToken(), endpointTemplateId, endpointForUpdate)

        then: "verify that the request was a success"
        updateResponse.status == 200

        when: "update the endpoint template with matching ID"
        endpointForUpdate.id = endpointTemplateIdInt + 1
        updateResponse = cloud20.updateEndpointTemplate(utils.getServiceAdminToken(), endpointTemplateId, endpointForUpdate, acceptType, requestType)

        then: "verify that the request was a success"
        updateResponse.status == 400

        cleanup:
        endpointService.deleteBaseUrl(endpointTemplateId)

        where:
        requestType          | acceptType
        APPLICATION_XML_TYPE | APPLICATION_XML_TYPE
        APPLICATION_XML_TYPE | APPLICATION_JSON_TYPE
        APPLICATION_JSON_TYPE | APPLICATION_XML_TYPE
        APPLICATION_JSON_TYPE | APPLICATION_JSON_TYPE
    }

    def "404 is returned when trying to update an endpoint template that does not exist"() {
        when:
        def endpointForUpdate = v1Factory.createEndpointTemplateForUpdate(true, false, true)
        def updateResponse = cloud20.updateEndpointTemplate(utils.getServiceAdminToken(), testUtils.getRandomInteger().toString(), endpointForUpdate)

        then:
        updateResponse.status == 404
    }

    @Unroll
    def "test update endpoint template attribute combinations: global = #global, enabled = #enabled, default = #_default"() {
        given:
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "compute", publicUrl, "cloudServers", false, "ORD"))

        when: "update the endpoint template with new values"
        def endpointForUpdate = v1Factory.createEndpointTemplateForUpdate(enabled, global, _default)
        def updateResponse = cloud20.updateEndpointTemplate(utils.getServiceAdminToken(), endpointTemplateId, endpointForUpdate)

        then: "verify that the request was a success"
        updateResponse.status == response

        when: "load the endpoint from the directory"
        def endpointTemplateEntity = endpointService.getBaseUrlById(endpointTemplateId)

        then: "verify that the values were updated correctly"
        //only verify that the values were updated if the request was successful
        if(response == 200) {
            endpointTemplateEntity.global == global
            endpointTemplateEntity.enabled == enabled
            endpointTemplateEntity.def == _default
        }

        cleanup:
        endpointService.deleteBaseUrl(endpointTemplateId)

        where:
        global | enabled | _default| response
        true   | true    | true    | 400
        true   | true    | false   | 200
        true   | false   | true    | 400
        true   | false   | false   | 200
        false  | true    | true    | 200
        false  | true    | false   | 200
        false  | false   | true    | 200
        false  | false   | false   | 200
    }

    @Unroll
    def "test update endpoint template using accept = #acceptType, request = #requestType"() {
        given:
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "compute", publicUrl, "cloudServers", false, "ORD"))
        def global = false
        def enabled = true
        def _default = true

        when: "update the endpoint template with new values"
        //change the values so they are different for every iteration of the test
        global = !global
        enabled = !enabled
        _default = !_default
        def endpointForUpdate = v1Factory.createEndpointTemplateForUpdate(enabled, global, _default)
        def updateResponse = cloud20.updateEndpointTemplate(utils.getServiceAdminToken(), endpointTemplateId, endpointForUpdate, acceptType, requestType)

        then: "verify that the request was a success"
        updateResponse.status == 200
        def updatedEndpoint
        if(acceptType == APPLICATION_JSON_TYPE) {
            updatedEndpoint = new JsonSlurper().parseText(updateResponse.getEntity(String))['OS-KSCATALOG:endpointTemplate']
            updatedEndpoint.default == _default
        } else {
            updatedEndpoint = updateResponse.getEntity(EndpointTemplate).value
            updatedEndpoint._default == _default
        }
        updatedEndpoint.enabled == enabled
        updatedEndpoint.global == global

        cleanup:
        endpointService.deleteBaseUrl(endpointTemplateId)

        where:
        requestType          | acceptType
        APPLICATION_XML_TYPE | APPLICATION_XML_TYPE
        APPLICATION_XML_TYPE | APPLICATION_JSON_TYPE
        APPLICATION_JSON_TYPE | APPLICATION_XML_TYPE
        APPLICATION_JSON_TYPE | APPLICATION_JSON_TYPE
    }

    def "cannot set 'default', 'enabled', and 'global' when creating endpoint template"() {
        given:
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        def endpointForCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", publicUrl, "cloudServers", false, "ORD");
        endpointForCreate.enabled = true
        endpointForCreate.global = global
        endpointForCreate._default = _default

        when: "update the endpoint template with new values"
        def response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)

        then: "verify that the request was a success"
        response.status == 201
        def endpointResponse = response.getEntity(EndpointTemplate).value
        endpointResponse.enabled == false
        endpointResponse.global == false
        endpointResponse._default == false

        cleanup:
        endpointService.deleteBaseUrl(endpointTemplateId)

        where:
        global | _default
        true   | true
        true   | false
        false  | true
        false  | false
    }

    def "must specify 'id', 'publicURL', 'type', and 'name' when creating endpoint template"() {
        given:
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        def endpointForCreate = v1Factory.createEndpointTemplate(endpointTemplateId, "compute", publicUrl, "cloudServers", false, "ORD");

        when:
        endpointForCreate.id = null
        def response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)

        then:
        response.status == 400

        when:
        endpointForCreate.publicURL = null
        response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)

        then:
        response.status == 400

        when:
        endpointForCreate.type = null
        response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)

        then:
        response.status == 400

        when:
        endpointForCreate.name = null
        response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)

        then:
        response.status == 400
    }

    def "test endpoint template type mappings"() {
        given:
        def mossoType = "compute"
        def nastType = "object-store"
        def mossoTypeMapping = testUtils.getRandomUUID() + "," + mossoType + "," + testUtils.getRandomUUID()
        def nastTypeMapping = testUtils.getRandomUUID() + "," + nastType + "," + testUtils.getRandomUUID()
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENDPOINT_TEMPLATE_TYPE_USE_MAPPING_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENDPOINT_TEMPLATE_TYPE_MOSSO_MAPPING_PROP, mossoTypeMapping)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENDPOINT_TEMPLATE_TYPE_NAST_MAPPING_PROP, nastTypeMapping)
        def endpointTemplateMossoId = testUtils.getRandomInteger().toString()
        def endpointTemplateNastId = testUtils.getRandomInteger().toString()
        def endpointTemplateFallbackId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")

        when: "test MOSSO type mappings"
        def endpointForCreate = v1Factory.createEndpointTemplate(endpointTemplateMossoId, mossoType, publicUrl, "cloudServers", false, "ORD");
        def response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)
        def endpointEntity = endpointService.getBaseUrlById(endpointTemplateMossoId)

        then:
        response.status == 201
        endpointEntity.openstackType == mossoType
        endpointEntity.baseUrlType == "MOSSO"

        when: "test NAST type mappings"
        endpointForCreate = v1Factory.createEndpointTemplate(endpointTemplateNastId, nastType, publicUrl, "cloudFiles", false, "ORD");
        response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)
        endpointEntity = endpointService.getBaseUrlById(endpointTemplateNastId)

        then:
        response.status == 201
        endpointEntity.openstackType == nastType
        endpointEntity.baseUrlType == "NAST"

        when: "test the fallback type mapping"
        endpointForCreate = v1Factory.createEndpointTemplate(endpointTemplateFallbackId, testUtils.getRandomUUID(), publicUrl, "cloudServers", false, "ORD");
        response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpointForCreate)

        then:
        response.status == 400

        cleanup:
        endpointService.deleteBaseUrl(endpointTemplateMossoId)
        endpointService.deleteBaseUrl(endpointTemplateNastId)
        staticIdmConfiguration.reset()
    }

}
