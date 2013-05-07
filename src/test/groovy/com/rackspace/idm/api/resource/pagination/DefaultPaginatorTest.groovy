package com.rackspace.idm.api.resource.pagination

import com.rackspace.idm.domain.entity.User
import com.unboundid.asn1.ASN1OctetString
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.ws.rs.core.UriInfo

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 3/6/13
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultPaginatorTest extends RootServiceTest {

    @Shared Paginator paginator

    def setupSpec() {
        paginator = new DefaultPaginator<User>()
    }

    def setup() {
    }

    def "when on the first page no link is created for 'first' and 'prev'"() {
        given:
        def offset = 7
        def limit = 10
        def totalRecords = 100
        def context = createContext(offset, limit, totalRecords)

        when:
        def result = paginator.createLinkHeader(uriInfo(), context)

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
        def result = paginator.createLinkHeader(uriInfo(), context)

        then:
        for (String link : result.split(",")) {
            assert(!link.matches(".*rel=\"last\".*"))
            assert(!link.matches(".*rel=\"next\".*"))
        }
    }

    def "the appropriate links are returned by createLinkHeader"() {
        when:
        def result = paginator.createLinkHeader(uriInfo(), createContext(offset, limit, totalRecords))
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

    def "the correct last index is returned by getLastIndex"() {
        when:
        def index = paginator.getLastIndex(records, limit, offset)

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

    def "createSearchRequest sets the contextId to null if not provided"() {
        given:
        def searchRequest = createSearchRequest()

        when:
        paginator.createSearchRequest("rsId", searchRequest, 0, 25)
        def contextId = searchRequest.getControl(VirtualListViewRequestControl.VIRTUAL_LIST_VIEW_REQUEST_OID).contextID

        then:
        contextId == null
    }

    def "createSearchRequest sets the contextId"() {
        when:
        paginator.createSearchRequest("rsId", searchRequest, contextId, 0, 25)

        then:
        def value = searchRequest.getControl(VirtualListViewRequestControl.VIRTUAL_LIST_VIEW_REQUEST_OID).contextID
        value.equals(expected)

        where:
        searchRequest           | contextId               | expected
        createSearchRequest()   | null                    | null
        createSearchRequest()   | createContextId("1234") | createContextId("1234")

    }

    def createSearchRequest() {
        return new SearchRequest("cn=base", SearchScope.BASE, "(objectClass=*)", "rsId")
    }

    def createContextId(String value) {
        return new ASN1OctetString(value)
    }

    def createContext(int offset, int limit, int totalRecords) {
        return new PaginatorContext<Object>().with {
            it.offset = offset
            it.limit = limit
            it.totalRecords = totalRecords
            return it
        }
    }
}
