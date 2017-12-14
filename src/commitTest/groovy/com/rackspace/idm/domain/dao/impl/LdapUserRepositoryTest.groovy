package com.rackspace.idm.domain.dao.impl

import com.unboundid.ldap.sdk.*
import testHelpers.RootServiceTest

import java.lang.reflect.Field

import static com.rackspace.idm.domain.dao.impl.LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_DEFAULT_VALUE
import static com.rackspace.idm.domain.dao.impl.LdapGenericRepository.USE_VLV_SSS_OPTIMIZATION_PROP_NAME
import static com.rackspace.idm.domain.dao.impl.LdapRepository.*

class LdapUserRepositoryTest extends RootServiceTest {
    LdapUserRepository dao

    LdapConnectionPools ldapConnectionPools
    LDAPInterface ldapInterface

    def setup() {
        dao = new LdapUserRepository()

        mockIdentityConfig(dao)
        mockConfiguration(dao)

        dao.identityConfig = identityConfig
        dao.config = config

        ldapConnectionPools = Mock()
        dao.connPools = ldapConnectionPools

        ldapInterface = Mock()
        ldapConnectionPools.getAppConnPoolInterface() >> ldapInterface
    }

    def "getEnabledUsersByContactId: Get enabled users by contactId filter"() {
        given:
        def contactId = "contactId"

        when:
        dao.getEnabledUsersByContactId(contactId)

        then:
        2 * identityConfig.getReloadableConfig().getMaxDirectoryPageSize() >> 1000

        1 * config.getBoolean(USE_VLV_SSS_OPTIMIZATION_PROP_NAME, USE_VLV_SSS_OPTIMIZATION_DEFAULT_VALUE) >> true

        1 * ldapInterface.search(_ as SearchRequest) >> { args ->
            SearchRequest request = (SearchRequest) args[0]
            assert request.baseDN == USERS_BASE_DN
            assert request.scope == SearchScope.SUB
            assert request.filter == new LdapSearchBuilder()
                    .addEqualAttribute(ATTR_CONTACT_ID, contactId)
                    .addEqualAttribute(ATTR_ENABLED, Boolean.toString(true).toUpperCase())
                    .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACEPERSON)
                    .build()
            List<SearchResultEntry> searchResultList = Arrays.asList()
            return new SearchResult(1, null, null, null, new String[0], searchResultList, Collections.emptyList(), 2, 0)
        }
    }
}
