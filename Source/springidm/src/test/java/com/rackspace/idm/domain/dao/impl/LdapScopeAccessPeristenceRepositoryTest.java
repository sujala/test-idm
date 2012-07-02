package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.naming.directory.SearchResult;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 5:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapScopeAccessPeristenceRepositoryTest {

    Configuration config = mock(Configuration.class);
    LdapConnectionPools ldapConnectionPools = mock(LdapConnectionPools.class);
    LdapScopeAccessPeristenceRepository ldapScopeAccessPeristenceRepository;
    LdapScopeAccessPeristenceRepository spy;

    @Before
    public void setUp() throws Exception {
        ldapScopeAccessPeristenceRepository = new LdapScopeAccessPeristenceRepository(ldapConnectionPools,config);
        spy = spy(ldapScopeAccessPeristenceRepository);
    }

    @Test
    public void addDelegateScopeAccess_callsGetContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(new ScopeAccess()).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDelegateScopeAccess(null,scopeAccess);
        verify(spy).getContainer(null, "DELEGATE TOKENS");
    }

    @Test
    public void addDelegateScopeAccess_entryNotNull_doesNotCallAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(new ScopeAccess()).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDelegateScopeAccess(null,scopeAccess);
        verify(spy,never()).addContainer(anyString(), anyString());
    }

    @Test
    public void addDelegateScopeAccess_entryNull_callsAddContainer() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(new ScopeAccess()).when(spy).addScopeAccess("uniqueId",scopeAccess);
        spy.addDelegateScopeAccess(null,scopeAccess);
        verify(spy).addContainer(null, "DELEGATE TOKENS");
    }

    @Test
    public void addDelegateScopeAccess_callsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess2 = new ScopeAccess();
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", new Attribute[0]);
        doReturn(null).doReturn(searchResultEntry).when(spy).getContainer(null, "DELEGATE TOKENS");
        doNothing().when(spy).addContainer(null,"DELEGATE TOKENS");
        doReturn(scopeAccess2).when(spy).addScopeAccess("uniqueId",scopeAccess);
        assertThat("scope access",spy.addDelegateScopeAccess(null, scopeAccess),equalTo(scopeAccess2));
    }

    @Test
    public void testAddImpersonatedScopeAccess() throws Exception {

    }

    @Test
    public void testAddDirectScopeAccess() throws Exception {

    }

    @Test
    public void testDefinePermission() throws Exception {

    }

    @Test
    public void testDelegatePermission() throws Exception {

    }

    @Test
    public void testDeleteScopeAccess() throws Exception {

    }

    @Test
    public void testDoesAccessTokenHavePermission() throws Exception {

    }

    @Test
    public void testDoesParentHaveScopeAccess() throws Exception {

    }

    @Test
    public void testGetDelegateScopeAccessForParentByClientId() throws Exception {

    }

    @Test
    public void testGetImpersonatedScopeAccessForParentByClientId() throws Exception {

    }

    @Test
    public void testGetDirectScopeAccessForParentByClientId() throws Exception {

    }

    @Test
    public void testGetPermissionByParentAndPermission() throws Exception {

    }

    @Test
    public void testGetPermissionsByParentAndPermission() throws Exception {

    }

    @Test
    public void testGetPermissionsByParent() throws Exception {

    }

    @Test
    public void testGetPermissionsByPermission() throws Exception {

    }

    @Test
    public void testGetScopeAccessByAccessToken() throws Exception {

    }

    @Test
    public void testGetScopeAccessByUserId() throws Exception {

    }

    @Test
    public void testGetScopeAccessByAuthorizationCode() throws Exception {

    }

    @Test
    public void testGetScopeAccessByRefreshToken() throws Exception {

    }

    @Test
    public void testGetScopeAccessByUsernameAndClientId() throws Exception {

    }

    @Test
    public void testGetDelegatedClientScopeAccessByUsername() throws Exception {

    }

    @Test
    public void testGetScopeAccessesByParent() throws Exception {

    }

    @Test
    public void testGetScopeAccessByParentAndClientId() throws Exception {

    }

    @Test
    public void testGetScopeAccessesByParentAndClientId() throws Exception {

    }

    @Test
    public void testGetDelegateScopeAccessesByParent() throws Exception {

    }

    @Test
    public void testGrantPermission() throws Exception {

    }

    @Test
    public void testRemovePermissionFromScopeAccess() throws Exception {

    }

    @Test
    public void testUpdatePermissionForScopeAccess() throws Exception {

    }

    @Test
    public void testUpdateScopeAccess() throws Exception {

    }

    @Test
    public void testAddScopeAccess() throws Exception {

    }
}
