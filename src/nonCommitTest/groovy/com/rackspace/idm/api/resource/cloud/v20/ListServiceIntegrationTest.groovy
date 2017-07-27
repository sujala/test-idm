package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.ApplicationDao
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList
import org.openstack.docs.identity.api.v2.IdentityFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType


class ListServiceIntegrationTest extends RootIntegrationTest {

    @Autowired ApplicationDao applicationDao;

    @Unroll
    def "List services"() {
        when: "Retrieving all services"
        def response = cloud20.listServices(utils.getServiceAdminToken(), acceptContentType)

        then: "Returns all services"
        response.status == 200
        def list = getServiceListFromResponse(response, acceptContentType)
        assert assertServiceListSize(list, 1, true)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Get service by name"() {
        given: "A new created service"
        def adminToken = utils.getServiceAdminToken()
        def service = v1Factory.createService()
        service.name = testUtils.getRandomUUID()
        def serviceResponse = cloud20.createService(adminToken, service)
        def serviceId = getServiceIdFromLocationHeader(serviceResponse.location.toString())

        when: "Retrieving service by name"
        def response = cloud20.listServices(adminToken, acceptContentType, service.name)

        then: "Return a list with a single service"
        response.status == 200
        def list = getServiceListFromResponse(response, acceptContentType)
        assert assertServiceListSize(list, 1)
        assert assertServiceListContainsServiceByName(list, service.name)

        cleanup:
        cloud20.deleteService(adminToken, serviceId)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Test get service by name with invalid name"() {
        given:
        def adminToken = utils.getServiceAdminToken()

        when: "Retrieving service by name with invalid data"
        def response = cloud20.listServices(adminToken, acceptContentType, testUtils.getRandomUUID())

        then: "Returns an empty list"
        response.status == 200
        def list = getServiceListFromResponse(response, acceptContentType)
        assert assertServiceListSize(list, 0)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Test get service by name with empty name"() {
        given:
        def adminToken = utils.getServiceAdminToken()

        when: "Retrieving service by name with invalid data"
        def response = cloud20.listServices(adminToken, acceptContentType, "")

        then: "Returns all services"
        response.status == 200
        def list = getServiceListFromResponse(response, acceptContentType)
        assert assertServiceListSize(list, 1, true)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Assert name query parameter is case-insensitive"() {
        given: "A new created service"
        def adminToken = utils.getServiceAdminToken()
        def service = v1Factory.createService()
        service.name = testUtils.getRandomUUID()
        def serviceResponse = cloud20.createService(adminToken, service)
        def serviceId = getServiceIdFromLocationHeader(serviceResponse.location.toString())

        when: "Retrieving service by name"
        def response = cloud20.listServices(adminToken, acceptContentType, service.name.toUpperCase())

        then: "Return a list with a single service"
        response.status == 200
        def list = getServiceListFromResponse(response, acceptContentType)
        assert assertServiceListSize(list, 1)
        assert assertServiceListContainsServiceByName(list, service.name)

        cleanup:
        cloud20.deleteService(adminToken, serviceId)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "Assert IdentityFault if two service exist with the same name"() {
        given:
        def service1 = utils.createService()
        def service2 = utils.createService()
        //update service2 to have the same name as service 1
        //have to use the dao directly b/c the API and service layers prevent this
        def service2Entity = applicationDao.getApplicationByName(service2.getName())
        service2Entity.name = service1.name
        applicationDao.updateApplication(service2Entity)

        when:
        def response = cloud20.listServices(utils.getServiceAdminToken(), MediaType.APPLICATION_XML_TYPE, service1.name)

        then:
        response.status == 500
        response.getEntity(IdentityFault).value.message == DefaultCloud20Service.DUPLICATE_SERVICE_ERROR_MESSAGE

        cleanup:
        utils.deleteService(service2)
        utils.deleteService(service1)
    }

    def getServiceIdFromLocationHeader(location) {
        String[] bits = location.split("/")
        return bits[bits.length-1]
    }

    def getServiceListFromResponse(response, acceptContentType) {
        def list = new ArrayList()
        if (acceptContentType == MediaType.APPLICATION_XML_TYPE) {
            def serviceList = response.getEntity(ServiceList).value
            list = serviceList.service
        } else {
            def seviceList = new JsonSlurper().parseText(response.getEntity(String))
            list = seviceList["OS-KSADM:services"]
        }
        return list
    }

    def assertServiceListSize(list, size, greaterThan=false) {
        if (greaterThan){
            return list.size() > size
        }
        return list.size() == size
    }

    def assertServiceListContainsServiceByName(list, name) {
        for(def service : list){
           if (name == service.name) {
               return true
            }
        }
        return false
    }
}
