package com.rackspace.idm.domain.dao.impl

import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.SearchScope
import testHelpers.RootServiceTest

class LdapIdentityProviderRepositoryTest extends RootServiceTest {
    LdapIdentityProviderRepository dao

    LdapConnectionPools ldapConnectionPools
    LDAPInterface ldapInterface

    def setup() {
        dao = new LdapIdentityProviderRepository()

        ldapConnectionPools = Mock()
        dao.connPools = ldapConnectionPools

        ldapInterface = Mock()
        ldapConnectionPools.getAppConnPoolInterface() >> ldapInterface
    }

    def "getIdentityProviderByEmailDomain: Get IDP by email domain filter"() {
        given:
        def emailDomain = "emailDomain"

        when:
        dao.getIdentityProviderByEmailDomain(emailDomain)

        then:
        1 * ldapInterface.searchForEntry(dao.getBaseDn(), SearchScope.ONE, _ as Filter, _) >> { args ->
            Filter filter = args[2]
            assert filter.components.size() == 2
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_EMAIL_DOMAINS && it.assertionValue == emailDomain}
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_OBJECT_CLASS && it.assertionValue == LdapRepository.OBJECTCLASS_EXTERNALPROVIDER}
            null
        }
    }
}
