package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.EndpointDao
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo
import javax.ws.rs.core.HttpHeaders

import static org.mockito.Mockito.*;

class Cloud20BaseURLIntegrationTest extends RootIntegrationTest {

    @Autowired Cloud20Service cloud20Service;

    @Autowired ApplicationDao applicationDao;
    @Autowired EndpointDao endpointDao;

    // Keystone V3 compatibility
    void "Test if 'cloud20Service.addEndpointTemplate(...)' adds all V3 internal attributes"() {
        given:
        def mockUriInfo = mock(UriInfo)
        def mockUriBuilder = mock(UriBuilder)
        when(mockUriInfo.getRequestUriBuilder()).thenReturn(mockUriBuilder)
        when(mockUriBuilder.path(anyString())).thenReturn(mockUriBuilder)
        when(mockUriBuilder.build()).thenReturn(new URI('http://localhost'))

        def applications = applicationDao.getApplicationByType('object-store')
        def it = applications.iterator()
        it.hasNext() // Force retrieve...
        def application = it.next()

        def templateId = 500 + (int) (Math.random() * 10000000);

        when:
        def EndpointTemplate template = new EndpointTemplate()
        template.setType(application.getOpenStackType())
        template.setName(UUID.randomUUID().toString())
        template.setPublicURL('http://localhost')
        template.setId(templateId)
        cloud20Service.addEndpointTemplate(mock(HttpHeaders), mockUriInfo, utils.getServiceAdminToken(), template)

        def data = endpointDao.getBaseUrlById(String.valueOf(templateId))

        then:
        data.clientId == application.clientId
        data.adminUrlId != null
        data.publicUrlId != null
        data.internalUrlId != null

        cleanup:
        try { endpointDao.deleteBaseUrl(String.valueOf(templateId)) } catch (Exception e) {}
    }

    void "Test if 'cloud20Service.addEndpointTemplate(...)' adds V3 internal attributes even if the service lookup fails"() {
        given:
        def mockUriInfo = mock(UriInfo)
        def mockUriBuilder = mock(UriBuilder)
        when(mockUriInfo.getRequestUriBuilder()).thenReturn(mockUriBuilder)
        when(mockUriBuilder.path(anyString())).thenReturn(mockUriBuilder)
        when(mockUriBuilder.build()).thenReturn(new URI('http://localhost'))

        def templateId = 500 + (int) (Math.random() * 10000000);

        when:
        def EndpointTemplate template = new EndpointTemplate()
        template.setType(UUID.randomUUID().toString())  // Random type
        template.setName(UUID.randomUUID().toString())
        template.setPublicURL('http://localhost')
        template.setId(templateId)
        cloud20Service.addEndpointTemplate(mock(HttpHeaders), mockUriInfo, utils.getServiceAdminToken(), template)

        def data = endpointDao.getBaseUrlById(String.valueOf(templateId))

        then:
        data.adminUrlId != null
        data.publicUrlId != null
        data.internalUrlId != null

        cleanup:
        try { endpointDao.deleteBaseUrl(String.valueOf(templateId)) } catch (Exception e) {}
    }

}
