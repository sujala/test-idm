package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.api.resource.pagination.PaginatorContext
import com.rackspace.idm.audit.Audit
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.Applications
import com.rackspace.idm.domain.entity.ClientAuthenticationResult
import com.rackspace.idm.domain.entity.ClientSecret
import com.rackspace.idm.domain.entity.FilterParam
import com.rackspace.idm.domain.service.PropertiesService
import com.rackspace.idm.util.CryptHelper
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.unboundid.ldap.sdk.ResultCode
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchScope
import org.apache.commons.configuration.Configuration
import org.bouncycastle.crypto.InvalidCipherTextException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootServiceTest

import java.security.GeneralSecurityException

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anyString
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.doNothing
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 7/26/13
 * Time: 1:43 PM
 * To change this template use File | Settings | File Templates.
 */
class LdapApplicationRepositoryTest extends RootServiceTest {

    @Shared
    LdapApplicationRepository LdapApplicationRepository

    def setup(){
        LdapApplicationRepository = new LdapApplicationRepository()
        mockConfiguration(LdapApplicationRepository)
    }
}
