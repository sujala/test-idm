package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspacecloud.docs.auth.api.v1.BaseURL
import com.rackspacecloud.docs.auth.api.v1.BaseURLList
import com.rackspacecloud.docs.auth.api.v1.UserType
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

import static org.mockito.Mockito.*;

class Cloud11BaseURLIntegrationTest extends RootIntegrationTest {

    @Autowired Cloud11Service cloud11Service;

    @Autowired ApplicationDao applicationDao;
    @Autowired EndpointDao endpointDao;

    // Keystone V3 compatibility
    void "Test if 'cloud11Service.addBaseURL(...)' adds all V3 internal attributes"() {
        given:
        def request = mock(HttpServletRequest)
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn('Basic YXV0aDphdXRoMTIz')

        def application = applicationDao.getApplicationByName('cloudFiles')
        def baseUrlId = 500 + (int) (Math.random() * 10000000);

        when:
        def BaseURL baseURL = new BaseURL()
        baseURL.setId(baseUrlId)
        baseURL.setServiceName('cloudFiles')
        baseURL.setPublicURL('http://localhost')
        cloud11Service.addBaseURL(request, mock(HttpHeaders), baseURL)

        def data = endpointDao.getBaseUrlById(String.valueOf(baseUrlId))

        then:
        data.clientId == application.clientId
        data.adminUrlId != null
        data.publicUrlId != null
        data.internalUrlId != null

        cleanup:
        try { endpointDao.deleteBaseUrl(String.valueOf(baseUrlId)) } catch (Exception e) {}
    }

    void "Test if 'cloud11Service.addBaseURL(...)' adds V3 internal attributes even if the service lookup fails"() {
        given:
        def request = mock(HttpServletRequest)
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn('Basic YXV0aDphdXRoMTIz')

        def baseUrlId = 500 + (int) (Math.random() * 10000000);

        when:
        def BaseURL baseURL = new BaseURL()
        baseURL.setId(baseUrlId)
        baseURL.setServiceName(UUID.randomUUID().toString()) // Random serviceName
        baseURL.setPublicURL('http://localhost')
        cloud11Service.addBaseURL(request, mock(HttpHeaders), baseURL)

        def data = endpointDao.getBaseUrlById(String.valueOf(baseUrlId))

        then:
        data.adminUrlId != null
        data.publicUrlId != null
        data.internalUrlId != null

        cleanup:
        try { endpointDao.deleteBaseUrl(String.valueOf(baseUrlId)) } catch (Exception e) {}
    }

    @Unroll
    def "test setting unmapped base URL types and viewing them in v1.1, accept = #accept"() {
        given:
        def baseUrlType = "notValidBaseUrlType"
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointTemplateId, baseUrlType, testUtils.getRandomUUID("http://public/"), "name", true, "ORD")
        utils.createAndUpdateEndpointTemplate(endpointTemplate, endpointTemplateId)

        when:
        def response = cloud11.getBaseURLs(null, accept)

        then:
        response.status == 200
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            def baseUrls = response.getEntity(BaseURLList)
            assert baseUrls.baseURL.find({t -> t.id == Integer.valueOf(endpointTemplateId)}).userType == UserType.UNKNOWN
        } else {
            def baseUrls = new JsonSlurper().parseText(response.getEntity(String))
            assert baseUrls.baseURLs.find({t -> t.id == Integer.valueOf(endpointTemplateId)}).userType == UserType.UNKNOWN.toString()
        }

        cleanup:
        utils.deleteEndpointTemplate(endpointTemplate)

        where:
        accept                          | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

}
