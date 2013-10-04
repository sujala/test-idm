package com.rackspace.idm.api.converter.cloudv11

import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.OpenstackEndpoint
import com.rackspace.idm.domain.entity.User
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList
import org.dozer.DozerBeanMapper
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.datatype.DatatypeFactory

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 8/15/13
 * Time: 1:40 PM
 * To change this template use File | Settings | File Templates.
 */
class UserConverterCloudV11Test extends Specification {
    @Shared UserConverterCloudV11 converterCloudV11
    @Shared EndpointConverterCloudV11 endpointConverterCloudV11

    def href = "http://something"
    def id = 1
    def v1Default = true

    def setupSpec() {
        converterCloudV11 = new UserConverterCloudV11()
    }

    def cleanupSpec() {
    }

    def "convert user from ldap to jersey object"() {
        given:
        User user = user()

        when:
        com.rackspacecloud.docs.auth.api.v1.User userEntity = converterCloudV11.toCloudV11User(user)

        then:
        user.username == userEntity.id
        user.apiKey == userEntity.key
        user.mossoId == userEntity.mossoId
        user.nastId == userEntity.nastId
        user.enabled == userEntity.enabled
        userEntity.created.equals(createdXML())
        userEntity.updated.equals(createdXML())
    }

    def "convert user from ldap to jersey object - with null endpoints"() {
        given:
        User user = user()

        when:
        com.rackspacecloud.docs.auth.api.v1.User userEntity = converterCloudV11.toCloudV11User(user, null)

        then:
        user.username == userEntity.id
        user.apiKey == userEntity.key
        user.mossoId == userEntity.mossoId
        user.nastId == userEntity.nastId
        user.enabled == userEntity.enabled
        userEntity.created.equals(createdXML())
        userEntity.updated.equals(createdXML())
    }

    def "convert user from ldap to jersey object - with endpoints"() {
        given:
        User user = user()
        EndpointConverterCloudV11 endpointConverterCloudV11 = Mock()
        converterCloudV11.enpointConverterCloudV11 = endpointConverterCloudV11
        endpointConverterCloudV11.openstackToBaseUrlRefs(_) >>  baseUrlRefs()

        when:
        com.rackspacecloud.docs.auth.api.v1.User userEntity = converterCloudV11.toCloudV11User(user, endpoints())

        then:
        user.username == userEntity.id
        user.apiKey == userEntity.key
        user.mossoId == userEntity.mossoId
        user.nastId == userEntity.nastId
        user.enabled == userEntity.enabled
        userEntity.baseURLRefs.baseURLRef.size() == 1
        userEntity.baseURLRefs.baseURLRef.get(0).href == href
        userEntity.baseURLRefs.baseURLRef.get(0).id == id
        userEntity.baseURLRefs.baseURLRef.get(0).v1Default == v1Default

    }

    def "convert user from ldap to openstack user jersey object - with endpoints"() {
        given:
        User user = user()
        EndpointConverterCloudV11 endpointConverterCloudV11 = Mock()
        converterCloudV11.enpointConverterCloudV11 = endpointConverterCloudV11
        endpointConverterCloudV11.openstackToBaseUrlRefs(_) >>  baseUrlRefs()

        when:
        com.rackspacecloud.docs.auth.api.v1.User userEntity = converterCloudV11.openstackToCloudV11User(user, endpoints())

        then:
        user.username == userEntity.id
        user.apiKey == userEntity.key
        user.mossoId == userEntity.mossoId
        user.nastId == userEntity.nastId
        user.enabled == userEntity.enabled
        userEntity.baseURLRefs.baseURLRef.size() == 1
        userEntity.baseURLRefs.baseURLRef.get(0).href == href
        userEntity.baseURLRefs.baseURLRef.get(0).id == id
        userEntity.baseURLRefs.baseURLRef.get(0).v1Default == v1Default
    }

    def "convert user from ldap to openstack userWithOnlyEnabled jersey object - with endpoints"() {
        given:
        User user = user()
        EndpointConverterCloudV11 endpointConverterCloudV11 = Mock()
        converterCloudV11.enpointConverterCloudV11 = endpointConverterCloudV11
        endpointConverterCloudV11.openstackToBaseUrlRefs(_) >>  baseUrlRefs()

        when:
        com.rackspacecloud.docs.auth.api.v1.User userEntity = converterCloudV11.toCloudV11UserWithOnlyEnabled(user, endpoints())

        then:
        user.username == userEntity.id
        user.apiKey == userEntity.key
        user.mossoId == userEntity.mossoId
        user.nastId == userEntity.nastId
        user.enabled == userEntity.enabled
        userEntity.baseURLRefs.baseURLRef.size() == 1
        userEntity.baseURLRefs.baseURLRef.get(0).href == href
        userEntity.baseURLRefs.baseURLRef.get(0).id == id
        userEntity.baseURLRefs.baseURLRef.get(0).v1Default == v1Default
    }

    def "convert user from ldap to openstack userWithId jersey object - with endpoints"() {
        given:
        User user = user()

        when:
        com.rackspacecloud.docs.auth.api.v1.UserWithId userEntity = converterCloudV11.toCloudV11UserWithId(user)

        then:
        user.username == userEntity.id
    }

    def "convert user from ldap to openstack userWithOnlyKey jersey object - with endpoints"() {
        given:
        User user = user()
        EndpointConverterCloudV11 endpointConverterCloudV11 = Mock()
        converterCloudV11.enpointConverterCloudV11 = endpointConverterCloudV11
        endpointConverterCloudV11.openstackToBaseUrlRefs(_) >>  baseUrlRefs()

        when:
        com.rackspacecloud.docs.auth.api.v1.User userEntity = converterCloudV11.toCloudV11UserWithOnlyKey(user, endpoints())

        then:
        user.username == userEntity.id
        user.apiKey == userEntity.key
        user.mossoId == userEntity.mossoId
        user.nastId == userEntity.nastId
        user.enabled == userEntity.enabled
        userEntity.baseURLRefs.baseURLRef.size() == 1
        userEntity.baseURLRefs.baseURLRef.get(0).href == href
        userEntity.baseURLRefs.baseURLRef.get(0).id == id
        userEntity.baseURLRefs.baseURLRef.get(0).v1Default == v1Default
    }

    def "convert user from jersey object to ldap"() {
        given:
        com.rackspacecloud.docs.auth.api.v1.User userEntity = userEntity()

        when:
        User user = converterCloudV11.fromUser(userEntity)

        then:
        user.username == userEntity.id
        user.apiKey == userEntity.key
        user.mossoId == userEntity.mossoId
        user.nastId == userEntity.nastId
        user.enabled == userEntity.enabled
    }

    def user() {
        user("id", "key", 1, "nast", false)
    }

    def user(String id, String key, int mossoId, String nastId, boolean enabled) {
        new User().with {
            it.username = id
            it.apiKey = key
            it.mossoId = mossoId
            it.nastId = nastId
            it.enabled = enabled
            it.created = created()
            it.updated = created()
            return it
        }
    }

    def userEntity() {
        userEntity("id", "key", 1, "nast", false)
    }

    def userEntity(String id, String key, int mossoId, String nastId, boolean enabled) {
        new com.rackspacecloud.docs.auth.api.v1.User().with {
            it.id = id
            it.key = key
            it.mossoId = mossoId
            it.nastId = nastId
            it.enabled = enabled
            it.created = createdXML()
            it.updated = createdXML()
            return it
        }
    }

    def baseUrlRefs() {
        com.rackspacecloud.docs.auth.api.v1.BaseURLRef urlRef = new BaseURLRef()
        urlRef.href = href
        urlRef.id = id
        urlRef.v1Default = v1Default
        com.rackspacecloud.docs.auth.api.v1.BaseURLRefList urlRefList= new BaseURLRefList()
        urlRefList.baseURLRef.add(urlRef)
        urlRefList
    }

    def baseUrl() {
        CloudBaseUrl baseUrl = new CloudBaseUrl()
        baseUrl.baseUrlId = id
        baseUrl.v1Default = false
    }

    def endpoints() {
        OpenstackEndpoint endpoint = new OpenstackEndpoint()
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>()
        baseUrls.add(baseUrl())
        endpoint.baseUrls = baseUrls
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>()
        endpoints.add(endpoint)
        endpoints
    }

    def created() {
        new Date(2013,1,1)
    }

    def createdXML() {
        GregorianCalendar gc = new GregorianCalendar()
        DateTime created = new DateTime(created())
        gc.setTime(created.toDate())
        DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)
    }
}
