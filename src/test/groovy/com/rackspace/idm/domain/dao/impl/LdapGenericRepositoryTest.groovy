package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.UniqueId
import com.unboundid.ldap.sdk.SearchResultEntry
import org.junit.Rule
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.junit.ConditionalIgnoreRule
import testHelpers.junit.IgnoreByRepositoryProfile

@IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
class LdapGenericRepositoryTest extends Specification {

    @Shared DummyRepository genericRepository

    @Rule
    public ConditionalIgnoreRule role = new ConditionalIgnoreRule()


    def "processSearchResults does not add null values"() {
        given:
        genericRepository = new DummyRepository()
        def List<SearchResultEntry> searchResultList = new ArrayList<SearchResultEntry>()
        searchResultList.add(null)

        when:
        def result = genericRepository.processSearchResult(searchResultList)

        then:
        result.size() == 0
    }

    public class DummyRepository extends LdapGenericRepository<Dummy> {
        private List<Dummy> processSearchResult(List<SearchResultEntry> searchResultList) {
            return super.processSearchResult(searchResultList);
        }
    }

    public class Dummy implements UniqueId {
        @Override
        String getUniqueId() {
            return '';
        }
        @Override
        void setUniqueId(String uniqueId) {
        }
    }
}
