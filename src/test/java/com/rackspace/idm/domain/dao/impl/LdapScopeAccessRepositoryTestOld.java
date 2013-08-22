package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 5:20 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapScopeAccessRepositoryTestOld extends InMemoryLdapIntegrationTest{

    @Mock
    Configuration config;
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @InjectMocks
    LdapScopeAccessRepository ldapScopeAccessPeristenceRepository = new LdapScopeAccessRepository();
    LdapScopeAccessRepository spy;

    @Before
    public void setUp() throws Exception {
        spy = spy(ldapScopeAccessPeristenceRepository);
    }

    @Test
    public void getMultipleEntries_throwsLDAPException_returnsEmptyList() throws Exception {

        final Filter filter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ldapScopeAccessPeristenceRepository.ATTR_OBJECT_CLASS, ldapScopeAccessPeristenceRepository.OBJECTCLASS_SCOPEACCESS)
                .addEqualAttribute(ldapScopeAccessPeristenceRepository.ATTR_CLIENT_ID, "clientId").build();

        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        doReturn(ldapInterface).when(spy).getAppInterface();
        doThrow(new LDAPSearchException(ResultCode.INVALID_DN_SYNTAX,"error")).when(ldapInterface).search(any(SearchRequest.class));
        List<SearchResultEntry> list = spy.getMultipleEntries("baseDN",SearchScope.SUB,filter);
        assertThat("list size",list.size(),equalTo(0));
    }

    @Test
    public void deleteScopeAccess_callsDeleteEntryAndSubtree() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        scopeAccess.setLdapEntry(new ReadOnlyEntry("uniqueId",new Attribute[0]));
        scopeAccess.getUniqueId();
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"),any(Audit.class));
        spy.deleteScopeAccess(scopeAccess);
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"),any(Audit.class));
    }
}
