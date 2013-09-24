package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.GenericDao
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.PaginatorContext
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.RootServiceTest

class LdapPagingIteratorTest extends Specification {

    def "Iterator works when totalRecords is more than valueList size"() {
        given:
        GenericDao repo = Mock()
        def context = new PaginatorContext<String>();
        context.getValueList().add("obj1")
        context.getValueList().add("obj2")
        context.setTotalRecords(3)
        repo.getObjectsPaged(_, _, _, _,_) >> context

        when:
        LdapPagingIterator<String> pagingIterator = new LdapPagingIterator<String>(repo, null, null, null)

        then:
        for (String string : pagingIterator.iterator()) {
            assert(!string.equals("obj"))
        }
    }
}
