package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.Application
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 8/13/13
 * Time: 11:41 AM
 * To change this template use File | Settings | File Templates.
 */
class ServiceConverterCloudV20Test extends Specification {

    @Shared ServiceConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new ServiceConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert application from ldap to jersey service object"() {
        given:
        Application app = app("id", "name", "type", "description")

        when:
        org.openstack.docs.identity.api.ext.os_ksadm.v1.Service service = converterCloudV20.toService(app)

        then:
        service.id == app.clientId
        service.name == app.name
        service.type == app.openStackType
        service.description == app.description
    }

    def "convert list of applications to jersey services object" () {
        given:
        Application app = app("id", "name", "type", "description")
        List<Application> apps = new ArrayList<Application>()
        apps.add(app)

        when:
        org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList services = converterCloudV20.toServiceList(apps)

        then:
        services.getService().size() == apps.size()
        org.openstack.docs.identity.api.ext.os_ksadm.v1.Service service = services.service.get(0)
        service.id == app.clientId
        service.name == app.name
        service.type == app.openStackType
        service.description == app.description
    }

    def app(String id, String name, String openStackType, String description) {
        new Application().with {
            it.clientId = id
            it.name = name
            it.openStackType = openStackType
            it.description = description
            return it
        }
    }

}
