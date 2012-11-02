package com.rackspace.idm.api.resource.pagination

import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import com.rackspace.idm.domain.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration
import javax.ws.rs.core.UriInfo
import com.unboundid.ldap.sdk.SearchResultEntry
import com.rackspace.idm.exception.BadRequestException

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 29/10/12
 * Time: 15:00
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class PaginatorContextTest extends Specification {

    @Shared PaginatorContext<User> paginatorContext

    def setupSpec() {
        paginatorContext = new PaginatorContext<User>().with() {
            it.setOffset(1)
            it.setLimit(10)
            it.setTotalRecords(25)
            return it
        }
    }

    def cleanupSpec() {
    }

    def "getSearchResultEntryList returns empty list"() {
        when:
        def list = paginatorContext.getSearchResultEntryList()

        then:
        list.size == 0
    }

    def "getValueList returns empty list"() {
        when:
        def list = paginatorContext.getValueList()

        then:
        list.size == 0
    }

    def "getPageLinks returns empty list"() {
        when:
        def list = paginatorContext.getPageLinks()

        then:
        ((HashMap) list).size() == 0
    }

    def "makePageLinks throws bad request"() {
        given:
        paginatorContext.setOffset(100)

        when:
        paginatorContext.makePageLinks()

        then:
        BadRequestException exception = thrown()
        exception.getMessage().equals("Offset greater than total number of records (25)")
    }


    def "makePageLinks within first page"() {
        when:
        paginatorContext.setOffset(0)
        paginatorContext.makePageLinks()

        then:
        paginatorContext.getPageLinks().get("first").equals(paginatorContext.getPageLinks().get("prev"))
    }

    def "makePageLinks within last page"() {
        given:
        paginatorContext.setOffset(20)
        paginatorContext.setLimit(10)

        when:
        paginatorContext.makePageLinks()

        then:
        paginatorContext.getPageLinks().get("last").equals(paginatorContext.getPageLinks().get("next"))
    }

    def "makePageLinks not on first or last page"() {
        given:
        paginatorContext.setOffset(15)
        paginatorContext.setLimit(10)
        paginatorContext.setTotalRecords(50)

        when:
        paginatorContext.makePageLinks()

        then:
        def pageLinks = paginatorContext.getPageLinks()
        pageLinks.get("first").equals("?marker=0&limit=10")
        pageLinks.get("last").equals("?marker=40&limit=10")
        pageLinks.get("prev").equals("?marker=5&limit=10")
        pageLinks.get("next").equals("?marker=25&limit=10")
    }

    def "within first page returns true"() {
        given:
        paginatorContext.setOffset(5)
        paginatorContext.setLimit(10)
        paginatorContext.setTotalRecords(25)

        when:
        def isOnPage = paginatorContext.withinFirstPage()

        then:
        isOnPage
    }

    def "within first page returns false"() {
        given:
        paginatorContext.setOffset(20)
        paginatorContext.setLimit(10)

        when:
        def isOnPage = paginatorContext.withinFirstPage()

        then:
        !isOnPage
    }

    def "within last page returns true"() {
        given:
        paginatorContext.setOffset(20)
        paginatorContext.setLimit(10)

        when:
        def isOnPage = paginatorContext.withinLastPage()

        then:
        isOnPage
    }

    def "within last page returns false"() {
        given:
        paginatorContext.setOffset(5)
        paginatorContext.setLimit(10)

        when:
        def isOnPage = paginatorContext.withinLastPage()

        then:
        !isOnPage
    }

    def  "createLinkHeader returns null"() {
        given:
        paginatorContext.setValueList(new ArrayList<User>())
        paginatorContext.pageLinks = new HashMap<String, String>()

        def uriInfo = Mock(UriInfo.class)
        uriInfo.getAbsolutePath() >> new URI("http://path.to.resource/here")

        when:
        def header = paginatorContext.createLinkHeader(uriInfo)

        then:
        header == null
    }

    def "createLinkHeader returns correct link"() {
        given:
        def userList = new ArrayList<User>()
        userList.add(new User())
        userList.add(new User())

        def pages = new HashMap<String, String>()
        pages.put("first", "?marker=0&limit=2")
        pages.put("last", "?marker=8&limit=2")
        pages.put("next", "?marker=6&limit=2")
        pages.put("prev", "?marker=2&limit=2")
        paginatorContext.setPageLinks(pages)
        paginatorContext.setValueList(userList)

        def uriInfo = Mock(UriInfo.class)
        uriInfo.getAbsolutePath() >> new URI("http://path.to.resource/here")


        when:
        def header = paginatorContext.createLinkHeader(uriInfo)

        then:
        def compareToHeader = "<http://path.to.resource/here?marker=0&limit=2>; rel=\"first\", <http://path.to.resource/here?marker=2&limit=2>; rel=\"prev\", <http://path.to.resource/here?marker=6&limit=2>; rel=\"next\", <http://path.to.resource/here?marker=8&limit=2>; rel=\"last\""
        header.equals(compareToHeader)
    }

    def "addComma adds comma"() {
        given:
        def builder = new StringBuilder()
        builder.append("string")

        when:
        paginatorContext.addComma(builder)

        then:
        builder.indexOf(", ") > 0
    }

    def "addComma does not add comma"() {
        given:
        def builder = new StringBuilder()

        when:
        paginatorContext.addComma(builder)

        then:
        builder.indexOf(", ") == -1
    }

    def "makeLink creates link"() {
        when:
        def link = paginatorContext.makeLink("http://here.to/there", "?marker=5&limit=7", "prev")

        then:
        link.equals("<http://here.to/there?marker=5&limit=7>; rel=\"prev\"")
    }
}
