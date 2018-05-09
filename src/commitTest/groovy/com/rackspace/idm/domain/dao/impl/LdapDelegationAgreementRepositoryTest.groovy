package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.DelegationPrincipal
import com.unboundid.ldap.sdk.*
import testHelpers.RootServiceTest

class LdapDelegationAgreementRepositoryTest extends RootServiceTest {
    LdapDelegationAgreementRepository dao

    LdapConnectionPools ldapConnectionPools
    LDAPInterface ldapInterface

    def setup() {
        dao = new LdapDelegationAgreementRepository()

        ldapConnectionPools = Mock()
        dao.connPools = ldapConnectionPools

        ldapInterface = Mock()
        ldapConnectionPools.getAppConnPoolInterface() >> ldapInterface
    }

    def "countNumberOfDelegationAgreementsByPrincipal: verify appropriate LDAP search and return value"() {
        given:
        DelegationPrincipal  delegationPrincipal = Mock(DelegationPrincipal)
        String uniqueId = "rsId=1"
        delegationPrincipal.getDn() >> new DN(uniqueId)

        when:
        int count = dao.countNumberOfDelegationAgreementsByPrincipal(delegationPrincipal)

        then:
        count == 1

        1 * ldapInterface.search(dao.getBaseDn(), SearchScope.SUB,  _ as Filter, LdapGenericRepository.ATTRIBUTE_DXENTRYCOUNT) >> { args ->
            Filter filter = args[2]
            assert filter.components.size() == 2
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_RS_PRINCIPAL_DN && it.assertionValue == uniqueId}
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_OBJECT_CLASS && it.assertionValue == LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT}

            Attribute[] attributes = new Attribute[1]
            attributes[0] = new Attribute("dxentrycount", "1")
            new SearchResult(1, null, null, null, new String[0], Collections.singletonList(new SearchResultEntry(dao.getBaseDn(), attributes, new Control[0])), Collections.emptyList(), 1, 0)
        }
    }
}
