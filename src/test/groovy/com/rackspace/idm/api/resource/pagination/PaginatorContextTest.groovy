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
}
