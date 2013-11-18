package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import org.dozer.DozerBeanMapper
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList
import org.openstack.docs.identity.api.v2.Endpoint
import org.openstack.docs.identity.api.v2.EndpointList
import org.openstack.docs.identity.api.v2.ServiceCatalog
import org.openstack.docs.identity.api.v2.VersionForService
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 8/20/13
 * Time: 2:28 PM
 * To change this template use File | Settings | File Templates.
 */
class EndpointConverterCloudV20Test extends Specification {
    @Shared EndpointConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new EndpointConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            it.sf = new OpenStackServiceCatalogFactory()
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert CloudBaseUrl from ldap to EndpointTemplate jersey object"() {
        given:
        CloudBaseUrl baseUrl = baseurl()

        when:
        EndpointTemplate endpointTemplate = converterCloudV20.toEndpointTemplate(baseUrl)

        then:
        VersionForService version = endpointTemplate.getVersion()
        baseUrl.adminUrl == endpointTemplate.adminURL
        baseUrl.baseUrlId == endpointTemplate.id.toString()
        baseUrl.enabled == endpointTemplate.enabled
        baseUrl.global == endpointTemplate.global
        baseUrl.internalUrl == endpointTemplate.internalURL
        baseUrl.openstackType == endpointTemplate.type
        baseUrl.publicUrl == endpointTemplate.publicURL
        baseUrl.region == endpointTemplate.region
        baseUrl.versionId == version.id
        baseUrl.versionInfo == version.info
        baseUrl.versionList == version.list
    }

    def "convert CloudBaseUrl from ldap to Endpoint jersey object"() {
        given:
        CloudBaseUrl baseUrl = baseurl()

        when:
        Endpoint endpoint = converterCloudV20.toEndpoint(baseUrl)

        then:
        VersionForService version = endpoint.getVersion()
        baseUrl.adminUrl == endpoint.adminURL
        baseUrl.baseUrlId == endpoint.id.toString()
        baseUrl.internalUrl == endpoint.internalURL
        baseUrl.openstackType == endpoint.type
        baseUrl.publicUrl == endpoint.publicURL
        baseUrl.region == endpoint.region
        baseUrl.versionId == version.id
        baseUrl.versionInfo == version.info
        baseUrl.versionList == version.list
    }

    def "convert EndpointTemplate jersey object to ldap CloudBaseurl"() {
        given:
        EndpointTemplate endpointTemplate = endpointTemplate()

        when:
        CloudBaseUrl baseUrl = converterCloudV20.toCloudBaseUrl(endpointTemplate)

        then:
        VersionForService version = endpointTemplate.getVersion()
        baseUrl.adminUrl == endpointTemplate.adminURL
        baseUrl.baseUrlId == endpointTemplate.id.toString()
        baseUrl.enabled == endpointTemplate.enabled
        baseUrl.global == endpointTemplate.global
        baseUrl.internalUrl == endpointTemplate.internalURL
        baseUrl.openstackType == endpointTemplate.type
        baseUrl.publicUrl == endpointTemplate.publicURL
        baseUrl.region == endpointTemplate.region
        baseUrl.versionId == version.id
        baseUrl.versionInfo == version.info
        baseUrl.versionList == version.list
    }

    def "convert EndpointTemplate jersey object to ldap CloudBaseurl - no version"() {
        given:
        EndpointTemplate endpointTemplate = endpointTemplate()
        endpointTemplate.version = null

        when:
        CloudBaseUrl baseUrl = converterCloudV20.toCloudBaseUrl(endpointTemplate)

        then:
        baseUrl.adminUrl == endpointTemplate.adminURL
        baseUrl.baseUrlId == endpointTemplate.id.toString()
        baseUrl.enabled == endpointTemplate.enabled
        baseUrl.global == endpointTemplate.global
        baseUrl.internalUrl == endpointTemplate.internalURL
        baseUrl.openstackType == endpointTemplate.type
        baseUrl.publicUrl == endpointTemplate.publicURL
        baseUrl.region == endpointTemplate.region
        baseUrl.versionId == null
        baseUrl.versionInfo == null
        baseUrl.versionList == null
    }

    def "convert CloudBaseUrl list from ldap to EndpointTemplateList jersey object"() {
        given:
        CloudBaseUrl baseUrl = baseurl()
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>()
        baseUrls.add(baseUrl)

        when:
        EndpointTemplateList endpointTemplates = converterCloudV20.toEndpointTemplateList(baseUrls)

        then:
        endpointTemplates.getEndpointTemplate().size() == baseUrls.size()
        EndpointTemplate endpointTemplate = endpointTemplates.getEndpointTemplate().get(0)
        VersionForService version = endpointTemplate.getVersion()
        baseUrl.adminUrl == endpointTemplate.adminURL
        baseUrl.baseUrlId == endpointTemplate.id.toString()
        baseUrl.enabled == endpointTemplate.enabled
        baseUrl.global == endpointTemplate.global
        baseUrl.internalUrl == endpointTemplate.internalURL
        baseUrl.openstackType == endpointTemplate.type
        baseUrl.publicUrl == endpointTemplate.publicURL
        baseUrl.region == endpointTemplate.region
        baseUrl.versionId == version.id
        baseUrl.versionInfo == version.info
        baseUrl.versionList == version.list
    }

    def "convert CloudBaseUrl list from ldap to EndpointTemplateList jersey object - null list"() {

        when:
        EndpointTemplateList endpointTemplates = converterCloudV20.toEndpointTemplateList(null)

        then:
        endpointTemplates.getEndpointTemplate().size() == 0
    }

    def "convert CloudBaseUrl list from ldap to EndpointTemplateList jersey object - empty list"() {
        given:
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>()

        when:
        EndpointTemplateList endpointTemplates = converterCloudV20.toEndpointTemplateList(baseUrls)

        then:
        endpointTemplates.getEndpointTemplate().size() == 0
    }

    def "convert CloudBaseUrl list from ldap to EndpointList jersey object"() {
        given:
        CloudBaseUrl baseUrl = baseurl()
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>()
        baseUrls.add(baseUrl)

        when:
        EndpointList endpoints = converterCloudV20.toEndpointListFromBaseUrls(baseUrls)

        then:
        endpoints.getEndpoint().size() == baseUrls.size()
        Endpoint endpoint = endpoints.getEndpoint().get(0)
        VersionForService version = endpoint.getVersion()
        baseUrl.adminUrl == endpoint.adminURL
        baseUrl.baseUrlId == endpoint.id.toString()
        baseUrl.internalUrl == endpoint.internalURL
        baseUrl.openstackType == endpoint.type
        baseUrl.publicUrl == endpoint.publicURL
        baseUrl.region == endpoint.region
        baseUrl.versionId == version.id
        baseUrl.versionInfo == version.info
        baseUrl.versionList == version.list
    }

    def "convert CloudBaseUrl list from ldap to EndpointList jersey object - null list"() {

        when:
        EndpointList endpointTemplates = converterCloudV20.toEndpointListFromBaseUrls(null)

        then:
        endpointTemplates.getEndpoint().size() == 0
    }

    def "convert CloudBaseUrl list from ldap to EndpointList jersey object - empty list"() {
        given:
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>()

        when:
        EndpointList endpointTemplates = converterCloudV20.toEndpointListFromBaseUrls(baseUrls)

        then:
        endpointTemplates.getEndpoint().size() == 0
    }

    def "convert OpenstackEndpoint list from ldap to EndpointList jersey object"() {
        given:
        CloudBaseUrl baseUrl = baseurl()
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>()
        baseUrls.add(baseUrl)
        OpenstackEndpoint osEndpoint = new OpenstackEndpoint()
        osEndpoint.baseUrls = baseUrls
        List<OpenstackEndpoint> osEndpoints = new ArrayList<OpenstackEndpoint>()
        osEndpoints.add(osEndpoint)

        when:
        EndpointList endpoints = converterCloudV20.toEndpointList(osEndpoints)

        then:
        endpoints.getEndpoint().size() == baseUrls.size()
        Endpoint endpoint = endpoints.getEndpoint().get(0)
        VersionForService version = endpoint.getVersion()
        baseUrl.adminUrl == endpoint.adminURL
        baseUrl.baseUrlId == endpoint.id.toString()
        baseUrl.internalUrl == endpoint.internalURL
        baseUrl.openstackType == endpoint.type
        baseUrl.publicUrl == endpoint.publicURL
        baseUrl.region == endpoint.region
        baseUrl.versionId == version.id
        baseUrl.versionInfo == version.info
        baseUrl.versionList == version.list
        osEndpoint.tenantId == endpoint.tenantId
    }

    def "convert OpenstackEndpoint list from ldap to EndpointList jersey object - null list"() {

        when:
        EndpointList endpoints = converterCloudV20.toEndpointList(null)

        then:
        endpoints.getEndpoint().size() == 0
    }

    def "convert OpenstackEndpoint list from ldap to EndpointList jersey object - empty list"() {
        given:
        List<OpenstackEndpoint> osEndpoints = new ArrayList<OpenstackEndpoint>()

        when:
        EndpointList endpoints = converterCloudV20.toEndpointList(osEndpoints)

        then:
        endpoints.getEndpoint().size() == 0
    }

    def "convert OpenstackEndpoint list from ldap to ServiceCatalog jersey object"() {
        given:
        CloudBaseUrl baseUrl = baseurl()
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>()
        baseUrls.add(baseUrl)
        OpenstackEndpoint osEndpoint = new OpenstackEndpoint()
        osEndpoint.baseUrls = baseUrls
        List<OpenstackEndpoint> osEndpoints = new ArrayList<OpenstackEndpoint>()
        osEndpoints.add(osEndpoint)

        when:
        ServiceCatalog catalog = converterCloudV20.toServiceCatalog(osEndpoints)

        then:
        catalog.getService().size() == baseUrls.size()
    }

    def "convert OpenstackEndpoint list from ldap to ServiceCatalog jersey object - null list"() {

        when:
        ServiceCatalog catalog = converterCloudV20.toServiceCatalog(null)

        then:
        catalog.getService().size() == 0
    }

    def "convert OpenstackEndpoint list from ldap to ServiceCatalog jersey object - empty list"() {
        given:
        List<OpenstackEndpoint> osEndpoints = new ArrayList<OpenstackEndpoint>()

        when:
        ServiceCatalog catalog = converterCloudV20.toServiceCatalog(osEndpoints)

        then:
        catalog.getService().size() == 0
    }

    def "convert EndpointTemplate to CloudBaseUrl sets the baseUrlType"() {
        given:
        EndpointTemplate endpointTemplate = endpointTemplate().with {
            it.type = type
            it
        }

        when:
        CloudBaseUrl baseUrl = converterCloudV20.toCloudBaseUrl(endpointTemplate)

        then:
        baseUrl.baseUrlType == expectedBaseUrlType

        where:
        type            | expectedBaseUrlType
        "object-store"  | "NAST"
        "compute"       | "MOSSO"
        "monitoring"    | "MOSSO"
        null            | null
    }

    def baseurl() {
        new CloudBaseUrl().with {
            it.adminUrl = "http://admin.com"
            it.baseUrlId = 1
            it.baseUrlType = "type"
            it.enabled = false
            it.global = false
            it.internalUrl = "http://internal.com"
            it.openstackType = "openstacktype"
            it.publicUrl = "http://public.com"
            it.region = "region"
            it.serviceName = "service"
            it.versionId = "versionId"
            it.versionInfo = "versionInfo"
            it.versionList = "versionList"
            it.v1Default = false
            return it
        }
    }

    def versionForService() {
        new VersionForService().with {
            it.id = "versionId"
            it.info = "versionInfo"
            it.list = "versionList"
            return it
        }
    }

    def endpoint() {
        new Endpoint().with {
            it.adminURL = "http://admin.com"
            it.id = 1
            it.internalURL = "http://internal.com"
            it.name = "name"
            it.publicURL = "http://public.com"
            it.region = "region"
            it.type = "openstacktype"
            it.version = versionForService()
            return it
        }
    }

    def endpointTemplate() {
        new EndpointTemplate().with {
            it.adminURL = "http://admin.com"
            it.enabled = false
            it.global = false
            it.id = 1
            it.internalURL = "http://internal.com"
            it.name = "name"
            it.publicURL = "http://public.com"
            it.region = "region"
            it.type = "openstacktype"
            it.version = versionForService()
            return it
        }
    }
}
