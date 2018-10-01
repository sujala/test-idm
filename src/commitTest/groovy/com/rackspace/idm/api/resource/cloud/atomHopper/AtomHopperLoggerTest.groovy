package com.rackspace.idm.api.resource.cloud.atomHopper

import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.slf4j.Logger
import spock.lang.Specification

import javax.servlet.http.HttpServletResponse

class AtomHopperLoggerTest extends Specification {

    def "AtomHopperLogger logs the correct values on success"() {
        given:
        def uri = "/identity/events"
        def responseText = "<atom:id>atomId</atom:id>"
        def entity = "<atom:entry></atom:entry>"
        def statusCode = HttpServletResponse.SC_CREATED

        CloseableHttpClient client = Mock(CloseableHttpClient)
        AtomHopperLogger atomHopperLogger = new AtomHopperLogger(client)
        CloseableHttpResponse mockResponse = Mock(CloseableHttpResponse)

        mockResponse.getStatusLine() >> Mock(StatusLine)
        mockResponse.getStatusLine().statusCode >> statusCode

        HttpEntity responseEntity = Mock(HttpEntity)
        mockResponse.getEntity() >> responseEntity
        responseEntity.getContent() >> new ByteArrayInputStream(responseText.getBytes('UTF-8'))

        def mockLogger = Mock(Logger)
        atomHopperLogger.logger = mockLogger

        when:
        HttpPost post = new HttpPost(uri)
        post.setEntity(atomHopperLogger.createRequestEntity(entity))

        def response = atomHopperLogger.execute(post)

        then:
        1 * client.execute(_) >> mockResponse
        1 * mockLogger.info(*_) >> { text, arguments ->
            assert text == "request[method: {} URL: {} payload: {}] response[status: {} atom:id: {}]"
            assert arguments[0] == "POST"
            assert arguments[1] == uri
            assert arguments[2] == entity
            assert arguments[3] == statusCode
            assert arguments[4] == "atomId"
        }
        response.getStatusLine().statusCode == statusCode
        response.getEntity() == responseEntity
    }

    def "AtomHopperLogger logs the correct values on error"() {
        given:
        def uri = "/identity/events"
        def responseText = "Error Message"
        def entity = "<atom:entry></atom:entry>"
        def statusCode = HttpServletResponse.SC_UNAUTHORIZED

        CloseableHttpClient client = Mock(CloseableHttpClient)
        AtomHopperLogger atomHopperLogger = new AtomHopperLogger(client)
        CloseableHttpResponse mockResponse = Mock(CloseableHttpResponse)

        mockResponse.getStatusLine() >> Mock(StatusLine)
        mockResponse.getStatusLine().statusCode >> statusCode

        HttpEntity responseEntity = Mock(HttpEntity)
        mockResponse.getEntity() >> responseEntity
        responseEntity.getContent() >> new ByteArrayInputStream(responseText.getBytes('UTF-8'))

        def mockLogger = Mock(Logger)
        atomHopperLogger.logger = mockLogger

        when:
        HttpPost post = new HttpPost(uri)
        post.setEntity(atomHopperLogger.createRequestEntity(entity))

        def response = atomHopperLogger.execute(post)

        then:
        1 * client.execute(_) >> mockResponse
        1 * mockLogger.info(*_) >> { text, arguments ->
            assert text == "request[method: {} URL: {} payload: {}] response[status: {} error: {}]"
            assert arguments[0] == "POST"
            assert arguments[1] == uri
            assert arguments[2] == entity
            assert arguments[3] == statusCode
            assert arguments[4] == responseText
        }
        response.getStatusLine().statusCode == statusCode
        response.getEntity() == responseEntity
    }

    def "AtomHopperLogger logs the correct values on exception"() {
        given:
        def uri = "/identity/events"
        def entity = "<atom:entry></atom:entry>"
        def errorMessage = "There was an error"

        CloseableHttpClient client = Mock(CloseableHttpClient)
        AtomHopperLogger atomHopperLogger = new AtomHopperLogger(client)

        def mockLogger = Mock(Logger)
        atomHopperLogger.logger = mockLogger

        when:
        HttpPost post = new HttpPost(uri)
        post.setEntity(atomHopperLogger.createRequestEntity(entity))

        atomHopperLogger.execute(post)

        then:
        1 * client.execute(_) >> { throw new IOException(errorMessage) }
        1 * mockLogger.error(*_) >> { text, arguments ->
            assert text == "request[method: {} URL: {} payload: {}] response[exception: {} error: {}]"
            assert arguments[0] == "POST"
            assert arguments[1] == uri
            assert arguments[2] == entity
            assert arguments[3] == IOException.class
            assert arguments[4] == errorMessage
        }

        thrown(IOException)
    }
}
