package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.PaginatorDao
import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.User
import com.unboundid.asn1.ASN1OctetString
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl
import spock.lang.Shared
import testHelpers.RootServiceTest

class LdapPaginatorRepositoryTest extends RootServiceTest {
    @Shared PaginatorDao paginator

    def setupSpec() {
        paginator = new LdapPaginatorRepository<User>()
    }

    def setup() {
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

    def "createSearchRequest sets the contextId and contentCount"() {
        when:
        paginator.createSearchRequest("rsId", searchRequest, contextId, 100, 0, 25)

        then:
        def control = searchRequest.getControl(VirtualListViewRequestControl.VIRTUAL_LIST_VIEW_REQUEST_OID)
        control.contextID.equals(expected)
        control.contentCount == 100

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
}
