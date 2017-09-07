package com.rackspace.idm.domain.dao.impl


import org.junit.Rule
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: rmlynch
 * Date: 8/20/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
class LdapScopeAccessRepositoryTest extends RootServiceTest {

    def setup () {
        scopeAccessDao = new LdapScopeAccessRepository();
    }

    def "Get the client ID from ScopeAccess DN" () {
        given:
        def id = "456789123"

        when:
        String parsedClientId = scopeAccessDao.parseDNForClientId("clientId=$id,ou=users,o=rackspace,dc=rackspace,dc=com")

        then:
        id == parsedClientId
    }

    def "ensure base dn is set to correct value" () {
        when:
        def baseDn = scopeAccessDao.getBaseDn()

        then:
        baseDn == LdapRepository.SCOPE_ACCESS_BASE_DN
    }
}
