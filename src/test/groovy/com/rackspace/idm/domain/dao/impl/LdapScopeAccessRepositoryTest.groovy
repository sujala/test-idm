package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.ScopeAccess
import com.unboundid.ldap.sdk.ReadOnlyEntry
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: rmlynch
 * Date: 8/20/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
class LdapScopeAccessRepositoryTest extends RootServiceTest {
    @Shared
    ScopeAccessDao scopeAccessDao;

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
}
