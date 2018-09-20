package com.rackspace.idm.api.resource.cloud.v11

import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspacecloud.docs.auth.api.v1.BaseURL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.web.WebAppConfiguration
import testHelpers.RootIntegrationTest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.HttpHeaders

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@WebAppConfiguration
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
        baseURL.setInternalURL('http://localhost')
        baseURL.setAdminURL('http://localhost')
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

    void "Test if 'cloud11Service.addBaseURL(...)' returns 404 with an invalid service name"() {
        given:
        def request = mock(HttpServletRequest)
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn('Basic YXV0aDphdXRoMTIz')

        def baseUrlId = 500 + (int) (Math.random() * 10000000);

        when:
        def BaseURL baseURL = new BaseURL()
        baseURL.setId(baseUrlId)
        baseURL.setServiceName(UUID.randomUUID().toString()) // Random serviceName
        baseURL.setPublicURL('http://localhost')
        def response = cloud11Service.addBaseURL(request, mock(HttpHeaders), baseURL).build()

        then:
        response.status == 404
    }

    void "Endpoints created do not display DEFAULT region"() {
        given:
        def request = mock(HttpServletRequest)
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn('Basic YXV0aDphdXRoMTIz')

        def application = applicationDao.getApplicationByName('cloudFiles')
        def baseUrlId = 500 + (int) (Math.random() * 10000000);
        String endpointId = String.valueOf(baseUrlId)

        when:
        def BaseURL baseURL = new BaseURL()
        baseURL.setId(baseUrlId)
        baseURL.setServiceName('cloudFiles')
        baseURL.setPublicURL('http://localhost')
        baseURL.setInternalURL('http://localhost')
        baseURL.setAdminURL('http://localhost')
        baseURL.setRegion("DEFAULT")
        cloud11Service.addBaseURL(request, mock(HttpHeaders), baseURL)

        def data = endpointDao.getBaseUrlById(endpointId)

        def createdEndpoint = cloud11Service.getBaseURLById(request, endpointId, null, mock(HttpHeaders))

        then:
        data.clientId == application.clientId
        data.region == "DEFAULT"
        createdEndpoint.entity.region == null


        cleanup:
        try { endpointDao.deleteBaseUrl(endpointId) } catch (Exception e) {}
    }

}
