package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.EndpointDao
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.IdentityFault
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class Cloud20BaseURLIntegrationTest extends RootIntegrationTest {

    @Autowired Cloud20Service cloud20Service;

    @Autowired ApplicationDao applicationDao;
    @Autowired EndpointDao endpointDao;

    // Keystone V3 compatibility
    void "Test if 'cloud20Service.addEndpointTemplate(...)' adds all V3 internal attributes"() {
        given:
        def applications = applicationDao.getApplicationByType('object-store')
        def it = applications.iterator()
        it.hasNext() // Force retrieve...
        def application = it.next()
        def templateId = 500 + (int) (Math.random() * 10000000);

        EndpointTemplate template = new EndpointTemplate()
        template.setType(application.getOpenStackType())
        template.setName(application.getName())
        template.setPublicURL('http://localhost')
        template.setInternalURL('http://localhost')
        template.setAdminURL('http://localhost')
        template.setId(templateId)

        when:
        cloud20.addEndpointTemplate(utils.getServiceAdminToken(), template)
        def data = endpointDao.getBaseUrlById(String.valueOf(templateId))

        then:
        data.clientId == application.clientId
        data.publicUrlId != null
        data.internalUrlId != null
        data.adminUrlId != null

        cleanup:
        try { endpointDao.deleteBaseUrl(String.valueOf(templateId)) } catch (Exception e) {}
    }

    void "Test if 'cloud20Service.addEndpointTemplate(...)' returns 404 with invalid service name"() {
        given:
        def templateId = 500 + (int) (Math.random() * 10000000);

        EndpointTemplate template = new EndpointTemplate()
        template.setType(UUID.randomUUID().toString())
        template.setName(UUID.randomUUID().toString())
        template.setPublicURL('http://localhost')
        template.setId(templateId)

        when:
        def response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), template)

        then:
        response.status == 404
    }

    void "Test if 'cloud20Service.addEndpointTemplate(...)' returns 400 for invalid type for service"() {
        given:
        def templateId = 500 + (int) (Math.random() * 10000000);

        EndpointTemplate template = new EndpointTemplate()
        template.setType(UUID.randomUUID().toString())  // Random type
        template.setName("cloudServers")
        template.setPublicURL('http://localhost')
        template.setId(templateId)

        when:
        def response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), template)

        then:
        response.status == 400
    }

    def "creating endpoint template with services with duplicate names returns error"() {
        given:
        def service1 = utils.createService()
        def service2 = utils.createService()
        //update service2 to have the same name as service 1
        //have to use the dao directly b/c the API and service layers prevent this
        def service2Entity = applicationDao.getApplicationByName(service2.getName())
        service2Entity.name = service1.name
        applicationDao.updateApplication(service2Entity)

        when:
        def response = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(testUtils.getRandomInteger().toString(), service1.type, "http://public.url", service1.name))

        then:
        response.status == 500
        response.getEntity(IdentityFault).value.message == DefaultCloud20Service.DUPLICATE_SERVICE_NAME_ERROR_MESSAGE

        cleanup:
        utils.deleteService(service2)
        utils.deleteService(service1)
    }

    def "listing endpoint templates does not return duplicates"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.MAX_CA_DIRECTORY_PAGE_SIZE_PROP, 20)

        when: "list all endpoint templates"
        def listResponse = cloud20.listEndpointTemplates(utils.getServiceAdminToken())
        def totalEndpointTemplates = endpointDao.getBaseUrlCount()
        assert listResponse.status == 200
        def returnedEndpointTemplates = listResponse.getEntity(EndpointTemplateList).value.endpointTemplate
        def returnedEndpointTemplateIds = returnedEndpointTemplates.id

        then: "no duplicates in the returned results"
        returnedEndpointTemplateIds.each {
            assert returnedEndpointTemplateIds.count(it) == 1
        }

        and: "the returned list of base URLs contains the same number of base URLs as stored in the directory"
        returnedEndpointTemplates.size() == totalEndpointTemplates

        cleanup:
        reloadableConfiguration.reset()
    }

    def "v2.0 delete endpoint template deletes the specified endpoint template"() {
        given:
        def endpointTemplate = utils.createEndpointTemplate()

        when: "get the endpoint template"
        def returnedET = utils.getEndpointTemplate(endpointTemplate.id)

        then: "the endpoint template is there"
        returnedET.id == endpointTemplate.id

        when: "disable endpoint template"
        endpointTemplate.enabled = false
        def updateResponse = cloud20.updateEndpointTemplate(utils.getServiceAdminToken(), endpointTemplate.id.toString(), endpointTemplate)

        then:
        updateResponse.status == 200

        when: "delete the endpoint template"
        def deleteResponse = cloud20.deleteEndpointTemplate(utils.getServiceAdminToken(), endpointTemplate.id.toString())
        def getAfterDeleteResponse = cloud20.getEndpointTemplate(utils.getServiceAdminToken(), endpointTemplate.id.toString())

        then: "the endpoint template is no longer there"
        deleteResponse.status == 204
        getAfterDeleteResponse.status == 404
    }

}
