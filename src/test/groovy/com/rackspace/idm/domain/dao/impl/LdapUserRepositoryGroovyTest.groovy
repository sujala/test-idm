package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.audit.Audit
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.ResultCode
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 5/3/13
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
class LdapUserRepositoryGroovyTest extends RootServiceTest{
    @Shared
    LdapUserRepository ldapUserRepository

    def setup(){
        ldapUserRepository = new LdapUserRepository()
        mockConfiguration(ldapUserRepository)
    }
}
