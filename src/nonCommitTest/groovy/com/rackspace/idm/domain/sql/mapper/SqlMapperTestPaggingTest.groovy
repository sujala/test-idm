package com.rackspace.idm.domain.sql.mapper

import com.rackspace.idm.domain.sql.mapper.test.DummyMapper
import com.rackspace.idm.domain.sql.mapper.test.DummyRepository
import com.rackspace.idm.domain.sql.mapper.test.SqlDummy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification


@ContextConfiguration(locations = "classpath:app-config-h2.xml")
class SqlMapperTestPaggingTest extends Specification{

    @Autowired
    DummyRepository repository

    @Autowired
    DummyMapper mapper

    def setup() {
        for (int i = 0; i < 100; i++) {
            SqlDummy dummy = new SqlDummy()
            dummy.id = i
            dummy.name = "abc" + i
            repository.save(dummy)
        }
    }

    def cleanup() {
        repository.deleteAll()
    }

    def "Paging should return the correct values"() {
        when:
        def page = mapper.getPageRequest(offset, limit)
        while (mapper.fromSQL(repository.findAll(page.pageRequest), page)) {}

        then:
        page.offset == offset
        page.limit == limit

        page.getValueList().get(0).id == offset
        page.getValueList().size() == limit

        where:
        offset  | limit
        0       | 1
        0       | 10
        1       | 10
        1       | 11
        1       | 20
        32      | 12
    }
}
