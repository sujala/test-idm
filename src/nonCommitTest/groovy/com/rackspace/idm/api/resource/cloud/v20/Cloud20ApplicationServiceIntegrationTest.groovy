package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.impl.LdapApplicationRepository
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.exception.BadRequestException
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType;

@ContextConfiguration(locations = "classpath:app-config.xml")
class Cloud20ApplicationServiceIntegrationTest extends RootIntegrationTest {

    @Autowired
    ApplicationService applicationService

    def "list services call with query parameter works as expected"() {
        given:
        def applications = applicationService.getOpenStackServices()
        def numberOfServices = applications.iterator().size()

        when: 'list services with limit=1'
        def response = cloud20.listServices(utils.getServiceAdminToken(), contentType, null, '0', '1')
        def serviceList = getEntity(response, ServiceList)

        then: 'verify response has only 1 service in the list'
        serviceList.service.size() == 1

        when: 'list services with limit=5'
        response = cloud20.listServices(utils.getServiceAdminToken(), contentType, null, '0', '5')
        serviceList = getEntity(response, ServiceList)

        then: 'verify response has only 5 service in the list'
        serviceList.service.size() == 5

        when: 'list services with limit=0'
        response = cloud20.listServices(utils.getServiceAdminToken(), contentType, null, '0', '0')
        serviceList = getEntity(response, ServiceList)

        then: 'verify response defaults to only 25 service in the list'
        serviceList.service.size() == 25

        when: 'list services with limit="null"'
        response = cloud20.listServices(utils.getServiceAdminToken(), contentType, null, '0', null)

        then: 'verify response defaults to only 25 service in the list'
        serviceList.service.size() == 25

        when: 'list services with limit=1000'
        response = cloud20.listServices(utils.getServiceAdminToken(), contentType, null, '0', '1000')
        serviceList = getEntity(response, ServiceList)

        then: 'verify response has all the existing services'
        serviceList.service.size() == numberOfServices

        when: 'list services with limit=""'
        response = cloud20.listServices(utils.getServiceAdminToken(), contentType, null, '0', '')
        serviceList = getEntity(response, ServiceList)

        then: 'verify response defaults to only 25 service in the list'
        serviceList.service.size() == 25

        when: 'list services with limit="badRequest"'
        response = cloud20.listServices(utils.getServiceAdminToken(), contentType, null, '0', 'badRequest')

        then: 'returns bad request'
        response.status == 400

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }
}
