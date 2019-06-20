package com.rackspace.idm.api.resource.cloud

import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.http.HttpStatus
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class CloudVersionIntegrationTest extends RootIntegrationTest {

    def setup() {
        reloadableConfiguration.reset()
    }

    def "When feature.return.json.specific.cloud.version enabled, the custom versions.json is returned when request json"() {
        when: "use json feature and get json"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP, true)
        def response = cloud.getVersions(MediaType.APPLICATION_JSON_TYPE)

        then: "get json specific version"
        response.status == HttpStatus.SC_OK
        response.getHeaders().getFirst("content-type") == "application/json"

        //for this test just verify the content contains the v3 data (which is only part of versions.json)
        String val = response.getEntity(String)
        val.contains("https://identity.api.rackspacecloud.com/v3")
    }

    def "When feature.return.json.specific.cloud.version disabled, the transformed versions.xml is returned when request json"() {
        when: "use json feature and get json"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP, false)
        def response = cloud.getVersions(MediaType.APPLICATION_JSON_TYPE)

        then: "get json specific version"
        response.status == HttpStatus.SC_OK
        response.getHeaders().getFirst("content-type") == "application/json"

        //for this test just verify the content does not contain the v3 data (which is not part of the versions.xml)
        String val = response.getEntity(String)
        !val.contains("https://identity.api.rackspacecloud.com/v3")
        val.contains("http://docs.rackspacecloud.com/auth/api/v1.1/auth.wadl")
    }

    @Unroll
    def "Versions.xml returned regardless of feature.return.json.specific.cloud.version set to #value when request xml"() {
        when: "get xml with feature"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RETURN_JSON_SPECIFIC_CLOUD_VERSION_PROP, value)
        def response = cloud.getVersions(MediaType.APPLICATION_XML_TYPE)

        then: "get xml specific version"
        response.status == HttpStatus.SC_OK
        response.getHeaders().getFirst("content-type") == "application/xml"

        //for this test just verify the content does not contain the v3 data (which is not part of the versions.xml)
        String val = response.getEntity(String)
        !val.contains("https://identity.api.rackspacecloud.com/v3")
        val.contains("<atom:link")

        where:
        value | _
        true | _
        false | _
    }

    @Unroll
    def "Returning Versions.xml returns when feature.reuse.jaxb.context set to #value"() {
        when: "get xml with feature"
        def response = cloud.getVersions(MediaType.APPLICATION_XML_TYPE)

        then: "get xml specific version"
        response.status == HttpStatus.SC_OK
        response.getHeaders().getFirst("content-type") == "application/xml"

        //for this test just verify the content does not contain the v3 data (which is not part of the versions.xml)
        String val = response.getEntity(String)
        !val.contains("https://identity.api.rackspacecloud.com/v3")
        val.contains("<atom:link")

    }

    def "Returning Versions20.xml returns same value regardless of feature.reuse.jaxb.context"() {
        when: "get xml with feat"
        def responseTrue = cloud20.getVersion(MediaType.APPLICATION_XML_TYPE)

        def responseFalse = cloud20.getVersion(MediaType.APPLICATION_XML_TYPE)

        then: "get json specific version"
        responseTrue.status == HttpStatus.SC_OK
        responseFalse.status == HttpStatus.SC_OK
        responseTrue.getEntity(String) == responseFalse.getEntity(String)
    }

    def "Returning Versions11.xml returns same value regardless of feature.reuse.jaxb.context"() {
        when: "get xml with feat"
        def responseTrue = cloud11.getVersion()

        def responseFalse = cloud11.getVersion()

        then: "get json specific version"
        responseTrue.status == HttpStatus.SC_OK
        responseFalse.status == HttpStatus.SC_OK
        responseTrue.getEntity(String) == responseFalse.getEntity(String)
    }
}
