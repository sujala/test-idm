package com.rackspace.idm.api.resource

import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.User
import org.junit.Test
import spock.lang.Specification

import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.equalToIgnoringCase
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertThat

class IdmPathUtilsTest extends Specification {

    IdmPathUtils idmPathUtils

    def setup() {
        idmPathUtils = new IdmPathUtils()
    }

    def "getLastIndex: the correct last index is returned by getLastIndex"() {
        when:
        def index = idmPathUtils.getLastIndex(records, limit, offset)

        then:
        expected == index

        where:
        expected | offset | limit | records
        98       | 0      | 2     | 100
        98       | 74     | 3     | 100
        93       | 53     | 10    | 100
        99       | 8      | 13    | 100
        13       | 7      | 6     | 14
    }

    def "addComma: Does not add comma if no existing value in string"() {
        StringBuilder builder = new StringBuilder()

        when:
        idmPathUtils.addComma(builder)

        then:
        builder.toString().indexOf(",") == -1
    }

    def "addComma: Does add comma if existing value in string"() {
        StringBuilder builder = new StringBuilder()
        builder.append("this is the first string")

        when:
        idmPathUtils.addComma(builder)

        then:
        builder.toString().indexOf(",") > 0
    }

    def "makeLink: creates link"() {
        when:
        String link = idmPathUtils.makeLink("path", "?query", "first")
        String expected = '<path?query>; rel="first"'

        then:
        link == expected
    }

    def "when on the first page no link is created for 'first' and 'prev'"() {
        given:
        def offset = 7
        def limit = 10
        def totalRecords = 100
        def context = createContext(offset, limit, totalRecords)

        when:
        def result = idmPathUtils.createLinkHeader(uriInfo(), context)

        then:
        for (String link : result.split(",")) {
            assert(!link.matches(".*rel=\"first\".*"))
            assert(!link.matches(".*rel=\"prev\".*"))
        }
    }

    def "when on the last page no link is created for 'last' and 'next'"() {
        given:
        def offset = 95
        def limit = 10
        def totalRecords = 100
        def context = createContext(offset, limit, totalRecords)

        when:
        def result = idmPathUtils.createLinkHeader(uriInfo(), context)

        then:
        for (String link : result.split(",")) {
            assert(!link.matches(".*rel=\"last\".*"))
            assert(!link.matches(".*rel=\"next\".*"))
        }
    }

    def "createLinkHeader: the appropriate links are returned"() {
        when:
        def result = idmPathUtils.createLinkHeader(uriInfo(), createContext(offset, limit, totalRecords))
        def firstLink = "marker=0&limit=$limit>; rel=\"first\""
        def prevLink = "marker=$prev&limit=$limit>; rel=\"prev\""
        def lastLink = "marker=$last&limit=$limit>; rel=\"last\""
        def nextLink = "marker=$next&limit=$limit>; rel=\"next\""

        then:
        result.contains(firstLink)
        result.contains(prevLink)
        result.contains(lastLink)
        result.contains(nextLink)


        where:
        offset | limit | totalRecords | prev | next | last
        17     | 10    | 30           | 7    | 27   | 27
        17     | 10    | 50           | 7    | 27   | 47
        9      | 3     | 20           | 6    | 12   | 18
        20     | 10    | 50           | 10   | 30   | 40
    }

    def "createLinkHeader: returns null with empty context"() {
        PaginatorContext<User> context = new PaginatorContext<User>()

        when:
        String header = idmPathUtils.createLinkHeader(uriInfo(), context)

        then:
        header == null
    }

    def "createLinkHeader: onFirstPage first Page Equals Prev Page"() {
        PaginatorContext<User> context = createContext(10, 10, 100)

        when:
        String header = idmPathUtils.createLinkHeader(uriInfo(), context)
        String[] links = header.split(", ")
        String prevLink = links[1].split(";")[0]
        String firstLink = links[0].split(";")[0]

        then:
        prevLink.equalsIgnoreCase(firstLink)
    }

    def createContext(int offset, int limit, int totalRecords) {
        return new PaginatorContext<Object>().with {
            it.offset = offset
            it.limit = limit
            it.totalRecords = totalRecords
            return it
        }
    }

    def uriInfo() {
        return uriInfo("http://absolute.path/to/resource")
    }

    def uriInfo(String absolutePath) {
        def builderMock = Mock(UriBuilder)
        def uriInfo = Mock(UriInfo)

        builderMock.path(_ as String) >> { String arg1 ->
            absolutePath = absolutePath + "/" + arg1
            return builderMock
        }
        builderMock.path(null) >> builderMock
        builderMock.build() >> {
            try {
                return new URI(absolutePath)
            } catch (Exception ex) {
                return new URI("http://absolute.path/to/resource")
            }
        }
        uriInfo.getRequestUriBuilder() >> builderMock
        uriInfo.getAbsolutePath() >> new URI(absolutePath)

        return uriInfo
    }
}
