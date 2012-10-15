package com.rackspace.idm.api.resource.cloud.v20;

import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.test.framework.JerseyTest
import spock.lang.Shared
import spock.lang.Specification

import static javax.ws.rs.core.MediaType.APPLICATION_JSON

class Cloud20IntegrationTest extends Specification {
    @Shared def jerseyTest = new JerseyTest(['com.rackspace.idm.api.resource'] as String[]) {}
    @Shared def resource = jerseyTest.resource()
    def randomness = UUID.randomUUID()

    def setupSpec() {
        jerseyTest.setUp()
    }

    def cleanupSpec() {
        jerseyTest.tearDown()
    }

    def 'hello'() {
        when:
            def hello = ''

        then:
            hello.length() == 0
    }
}
