package com.rackspace.idm.domain.service.impl;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 5/31/12
 * Time: 10:30 AM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultScopeAccessServiceTest {

    @InjectMocks
    DefaultScopeAccessService defaultScopeAccessService = new DefaultScopeAccessService();
    DefaultScopeAccessService spy;
    @Mock
    ScopeAccessDao scopeAccessDao;
    @Mock
    Configuration configuration;
    @Mock
    private UserDao userDao;
    @Mock
    private ApplicationDao clientDao;
    @Mock
    private TenantDao tenantDao;
    @Mock
    private EndpointDao endpointDao;
    @Mock
    private AuthHeaderHelper authHeaderHelper;

    ImpersonationRequest impersonationRequest;

    @Before
    public void setUp() throws Exception {
        impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setUsername("impersonatedUser");
        impersonationRequest.setUser(user);
        when(configuration.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        when(configuration.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        when(configuration.getInt("token.refreshWindowHours")).thenReturn(12);

        spy = spy(defaultScopeAccessService);

    }

    @Test
    public void getScopeAccessByUserId_callsScopeAccessDao_getScopeAccessByUserId() throws Exception {
        defaultScopeAccessService.getScopeAccessByUserId("userId");
        verify(scopeAccessDao).getScopeAccessByUserId("userId");
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tokenInstanceOfDelegatedClientScopeAccess_setsParentUniqueId() throws Exception {
        Attribute attribute = new Attribute("name");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry("test",attribute);
        DelegatedClientScopeAccess token = new DelegatedClientScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantDao.getTenantRolesByParent("test")).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantDao).getTenantRolesByParent("test");
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tokenNotInstanceOfDelegatedClientScopeAccess_setsParentUniqueId() throws Exception {
        DN dn = new DN("cn=rdn,dc=parent");
        Attribute attribute = new Attribute("name");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(dn,attribute,attribute);
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantDao.getTenantRolesByParent("dn:=Tim Jones")).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantDao).getTenantRolesByParent("dc=parent");
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tokenNotInstanceOfDelegatedClientScopeAccessAndFails_stillPasses() throws Exception {
        Entry entry = new Entry("junk");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(entry);
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantDao).getTenantRolesByParent(null);
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_rolesNull_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(null);
        List<OpenstackEndpoint> endpoints = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size", endpoints.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_rolesEmpty_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(new ArrayList<TenantRole>());
        List<OpenstackEndpoint> endpoints = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size", endpoints.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_roleHasTenantIdAndIdValid_addsTenantToList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        String[] tenantIds = {"123"};
        role.setTenantIds(tenantIds);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        Tenant tenant = new Tenant();
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        when(tenantDao.getTenant("123")).thenReturn(tenant);
        when(endpointDao.getOpenstackEndpointsForTenant(tenant)).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(endpointDao).getOpenstackEndpointsForTenant(tenant);
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_roleHasTenantIdAndIdNotValid_doesNotAddTenantToList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        String[] tenantIds = {"123"};
        role.setTenantIds(tenantIds);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        when(tenantDao.getTenant("123")).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(endpointDao,never()).getOpenstackEndpointsForTenant(any(Tenant.class));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_noTenantIds_doesNotCallTenantDaoMethod() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        String[] tenantIds = {};
        role.setTenantIds(tenantIds);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantDao,never()).getTenant(anyString());
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tenantIdsNull_doesNotCallTenantDaoMethod() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantDao,never()).getTenant(anyString());
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_rolesEmpty_doesNotCallTenantDaoMethod() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantDao,never()).getTenant(anyString());
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_endPointsExist_returnsListWithEndpoint() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);

        TenantRole role = new TenantRole();
        String[] tenantIds = {"123"};
        role.setTenantIds(tenantIds);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        Tenant tenant = new Tenant();

        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        endpoint.getBaseUrls().add(new CloudBaseUrl());

        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        when(tenantDao.getTenant("123")).thenReturn(tenant);
        when(endpointDao.getOpenstackEndpointsForTenant(tenant)).thenReturn(endpoint);

        List<OpenstackEndpoint> endpointList = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size",endpointList.size(),equalTo(1));
        assertThat("endpoint",endpointList.get(0),equalTo(endpoint));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_endpointNull_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);

        TenantRole role = new TenantRole();
        String[] tenantIds = {"123"};
        role.setTenantIds(tenantIds);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        Tenant tenant = new Tenant();

        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        endpoint.getBaseUrls().add(new CloudBaseUrl());

        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        when(tenantDao.getTenant("123")).thenReturn(tenant);
        when(endpointDao.getOpenstackEndpointsForTenant(tenant)).thenReturn(null);

        List<OpenstackEndpoint> endpointList = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size",endpointList.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_baseUrlsListEmpty_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);

        TenantRole role = new TenantRole();
        String[] tenantIds = {"123"};
        role.setTenantIds(tenantIds);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        Tenant tenant = new Tenant();

        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());

        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        when(tenantDao.getTenant("123")).thenReturn(tenant);
        when(endpointDao.getOpenstackEndpointsForTenant(tenant)).thenReturn(endpoint);

        List<OpenstackEndpoint> endpointList = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size",endpointList.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tenantsEmpty_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);

        TenantRole role = new TenantRole();
        String[] tenantIds = {"123"};
        role.setTenantIds(tenantIds);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        when(tenantDao.getTenantRolesByParent(null)).thenReturn(roles);
        when(tenantDao.getTenant("123")).thenReturn(null);

        List<OpenstackEndpoint> endpointList = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size",endpointList.size(),equalTo(0));
    }

    @Test
    public void addDelegateScopeAccess_scopeAccessNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.addDelegateScopeAccess(null,null);
            assertTrue("should throw exception",false);
        } catch (IllegalArgumentException ex)
        {
            assertThat("exception message", ex.getMessage(), equalTo("Null argument passed in."));
        }
    }

    @Test
    public void addDelegateScopeAccess_scopeAccessNotNull_callsScopeAccessDaoMethod() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        defaultScopeAccessService.addDelegateScopeAccess(null, scopeAccess);
        verify(scopeAccessDao).addDelegateScopeAccess(null,scopeAccess);
    }

    @Test
    public void addDelegateScopeAccess_scopeAccessNotNull_returnsNewScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessDao.addDelegateScopeAccess(null,scopeAccess)).thenReturn(scopeAccess);
        assertThat("returned scope access", defaultScopeAccessService.addDelegateScopeAccess(null, scopeAccess), equalTo(scopeAccess));
    }

    @Test
    public void addImpersonatedScopeAccess_TokenDoesNotExists_callsScopeAccessDao_addImpersonatedScopeAccess() throws Exception {
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "impToken", impersonationRequest);
        verify(scopeAccessDao).addImpersonatedScopeAccess(anyString(), any(ScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_whenScopeAccessDoesNotExist_callsSetImpersonatedScopeAccess() throws Exception {
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        User user = new User();
        spy.addImpersonatedScopeAccess(user, null, null, impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_existingAccessIsNullAndExpireInIsNullAndCallerIsServiceUser_setsExpirationToDefault() throws Exception {
        ArgumentCaptor<ImpersonatedScopeAccess> argument = ArgumentCaptor.forClass(ImpersonatedScopeAccess.class);
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        when(scopeAccessDao.addImpersonatedScopeAccess(anyString(), argument.capture())).thenReturn(null);
        when(configuration.getInt("token.impersonatedByServiceDefaultSeconds")).thenReturn(3600);
        defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "impToken", impersonationRequest);
        DateTime dateTime = new DateTime().plusSeconds(3600);
        assertThat("expiration date", argument.getValue().getAccessTokenExp().getTime(), greaterThan(dateTime.getMillis() - 60000L));
        assertThat("expiration date", argument.getValue().getAccessTokenExp().getTime(), lessThan(dateTime.getMillis() + 60000L));
    }

    @Test
    public void addImpersonatedScopeAccess_TokenExistsAndIsExpiredAndImpersonatingTokenIsNull_callsScopeAccessDao_updateScopeAccess() throws Exception {
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ImpersonatedScopeAccess());
        defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "impToken", impersonationRequest);
        verify(scopeAccessDao).updateScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_whenScopeAccessExistsAndTokenExpiredAndImpersonatedTokenNull_callsSetImpersonatedScopeAccess() throws Exception {

        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ImpersonatedScopeAccess());
        User user = new User();
        spy.addImpersonatedScopeAccess(user, null, null, impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_tokenExistsAndIsNotExpiredAndImpersonatingTokenNotNullAndImpersonatingTokenEqualsParameter_returnsSameAccessToken() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(100).toDate());
        String token = "abc";
        String impToken = "imp";
        impersonatedScopeAccess.setAccessTokenString(token);
        impersonatedScopeAccess.setImpersonatingToken(impToken);
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(impersonatedScopeAccess);
        ImpersonatedScopeAccess returnedImpersonatedScopeAccess = defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "imp", impersonationRequest);
        assertThat("impersonated token", returnedImpersonatedScopeAccess.getAccessTokenString(), equalTo("abc"));
    }

    @Test
    public void addImpersonatedScopeAccess_tokenExistsAndIsNotExpiredAndImpersonatingTokenNotNullAndImpersonatingTokenNotEqualToParameter_callsSetImpersonatedScopeAccess() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(100).toDate());
        impersonatedScopeAccess.setAccessTokenString("foo");
        impersonatedScopeAccess.setImpersonatingToken("token");
        User user = new User();
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(null,"impersonatedUser")).thenReturn(impersonatedScopeAccess);
        spy.addImpersonatedScopeAccess(user, null, "foo", impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_tokenExistsAndIsNotExpiredAndImpersonatingTokenNull_callsSetImpersonatedScopeAccess() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(100).toDate());
        impersonatedScopeAccess.setAccessTokenString("foo");
        User user = new User();
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(null,"impersonatedUser")).thenReturn(impersonatedScopeAccess);
        spy.addImpersonatedScopeAccess(user, null, "foo", impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_tokenExistsAndIsExpiredAndImpersonatingTokenNotNullAndImpersonatingTokenEqualToParameter_callsSetImpersonatedScopeAccess() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setImpersonatingToken("token");
        User user = new User();
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(null,"impersonatedUser")).thenReturn(impersonatedScopeAccess);
        spy.addImpersonatedScopeAccess(user, null, "token", impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_tokenExistsAndIsExpiredAndImpersonatingTokenNotNullAndImpersonatingTokenNotEqualToParameter_callsSetImpersonatedScopeAccess() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setImpersonatingToken("token");
        User user = new User();
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(null,"impersonatedUser")).thenReturn(impersonatedScopeAccess);
        spy.addImpersonatedScopeAccess(user, null, "foo", impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void validateExpireInElement_ExpireInIsNull_succeeds() throws Exception {
        defaultScopeAccessService.validateExpireInElement(new User(), new ImpersonationRequest());
    }

    @Test
    public void setImpersonatedScopeAccess_callerIsRacker_setsRackerId() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("foo");
        ImpersonatedScopeAccess impersonatedScopeAccess = defaultScopeAccessService.setImpersonatedScopeAccess(racker, impersonationRequest, new ImpersonatedScopeAccess());
        assertThat("racker id", impersonatedScopeAccess.getRackerId(), equalTo("foo"));
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsRacker_setsExpiration() throws Exception {
        DateTime expectedExpirationTime = new DateTime().plusSeconds(10000);
        impersonationRequest.setExpireInSeconds(10000);
        when(configuration.getInt("token.impersonatedByRackerMaxSeconds")).thenReturn(10800);
        ImpersonatedScopeAccess impersonatedScopeAccess = defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), greaterThan(expectedExpirationTime.getMillis() - 60000L));
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), lessThan(expectedExpirationTime.getMillis() + 60000L));
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsServiceUser_setsExpiration() throws Exception {
        DateTime expectedExpirationTime = new DateTime().plusSeconds(10000);
        impersonationRequest.setExpireInSeconds(10000);
        when(configuration.getInt("token.impersonatedByServiceMaxSeconds")).thenReturn(10800);
        ImpersonatedScopeAccess impersonatedScopeAccess = defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), greaterThan(expectedExpirationTime.getMillis() - 60000L));
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), lessThan(expectedExpirationTime.getMillis() + 60000L));
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInGreaterThanMaxAndCallerIsRacker_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(10800000);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInGreaterThanMaxAndCallerIsServiceUser_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(108000000);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInLessThan1AndCallerIsRacker_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(0);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInLessThan1AndCallerIsServiceUser_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(0);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsRacker_checksMaxTime() throws Exception {
        impersonationRequest.setExpireInSeconds(10800);
        when(configuration.getInt("token.impersonatedByRackerMaxSeconds")).thenReturn(10800);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByRackerMaxSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsServiceUser_checksMaxTime() throws Exception {
        impersonationRequest.setExpireInSeconds(10800);
        when(configuration.getInt("token.impersonatedByServiceMaxSeconds")).thenReturn(10800);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByServiceMaxSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNullAndCallerIsRacker_setsExpirationToRackerDefault() throws Exception {
        impersonationRequest.setExpireInSeconds(null);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByRackerDefaultSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNullAndCallerIsServiceUser_setsExpirationToServiceDefault() throws Exception {
        impersonationRequest.setExpireInSeconds(null);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByServiceDefaultSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_CallerIsRacker_setsExpirationToRackerDefault() throws Exception {
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByRackerDefaultSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_CallerIsServiceUser_setsExpirationToServiceDefault() throws Exception {
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByServiceDefaultSeconds");
    }

    @Test
    public void addDirectScopeAccess_scopeAccessIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.addDirectScopeAccess(null,null);
            assertTrue("illegalArgumentException expected",false);
        }catch (IllegalArgumentException ex)
        {
            assertThat("exception message", ex.getMessage(),equalTo("Null argument passed in."));
        }
    }

    @Test
    public void addScopeAccess_nullScopeAccess_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.addScopeAccess(null, null);
            assertTrue("illegalArgumentException expected",false);
        }catch (IllegalArgumentException ex)
        {
            assertThat("exception message", ex.getMessage(),equalTo("Null argument passed in."));
        }
    }

    @Test
    public void addScopeAccess_scopeAccessNotNull_callsScopeAccessDaoMethod() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccess1 = new ScopeAccess();
        when(scopeAccessDao.addScopeAccess(null,scopeAccess)).thenReturn(scopeAccess1);
        assertThat("scope access",defaultScopeAccessService.addScopeAccess(null,scopeAccess),equalTo(scopeAccess1));
    }

    @Test
    public void authenticateAccessToken_scopeAccessInstanceOfHasAccessTokenAndTokenNotExpired_authenticatedIsTrue() throws Exception {
        ScopeAccess scopeAccess = new ImpersonatedScopeAccess();
        ((HasAccessToken) scopeAccess).setAccessTokenString("foo");
        ((HasAccessToken) scopeAccess).setAccessTokenExp(new DateTime().plusMinutes(5).toDate());
        when(scopeAccessDao.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        assertThat("boolean", defaultScopeAccessService.authenticateAccessToken(null), equalTo(true));
    }

    @Test
    public void authenticateAccessToken_scopeAccessInstanceOfHasAccessTokenAndTokenNotExpired_getsAuditContext() throws Exception {
        ScopeAccess scopeAccess = mock(ImpersonatedScopeAccess.class);
        when(((HasAccessToken) scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessDao.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(scopeAccess.getAuditContext()).thenReturn("foo");
        defaultScopeAccessService.authenticateAccessToken(null);
        verify(scopeAccess).getAuditContext();
    }

    @Test
    public void authenticateAccessToken_scopeAccessNotInstanceOfHasAccessToken_authenticatedIsFalse() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessDao.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        assertThat("boolean", defaultScopeAccessService.authenticateAccessToken(null), equalTo(false));
    }

    @Test
    public void delegatePermission_returnsDelegatedPermission() throws Exception {
        DelegatedPermission delegatedPermission = new DelegatedPermission();
        when(scopeAccessDao.delegatePermission(null,null)).thenReturn(delegatedPermission);
        assertThat("delegated permission",defaultScopeAccessService.delegatePermission(null,null),equalTo(delegatedPermission));
    }

    @Test
    public void deleteScopeAccess_scopeAccessNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.deleteScopeAccess(null);
            assertTrue("should throw exception",false);
        }catch (Exception ex){
            assertThat("exception type", ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument passed in."));
        }
    }

    @Test
    public void deleteDelegatedToken_userNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.deleteDelegatedToken(null,null);
            assertTrue("should throw exception",false);
        }catch (Exception ex){
            assertThat("exception type", ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument passed in"));
        }
    }

    @Test
    public void deleteDelegatedToken_scopeAccessListNotNullAndEmpty_throwsNotFoundException() throws Exception {
        try{
            doReturn(new ArrayList<DelegatedClientScopeAccess>()).when(spy).getDelegatedUserScopeAccessForUsername(null);
            spy.deleteDelegatedToken(new User(), null);
            assertTrue("should throw exception",false);
        }catch (Exception ex){
            assertThat("exception type", ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("No delegated access tokens available for the user null"));
        }
    }

    @Test
    public void deleteDelegatedToken_scopeAccessListHasDelegatedClientScopeAccessAndGetRefreshTokenStringDoesNotEqualTokenString_throwsNotFoundException() throws Exception {
        try{
            DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
            delegatedClientScopeAccess.setRefreshTokenString("foo");
            List<DelegatedClientScopeAccess> scopeAccessList = new ArrayList<DelegatedClientScopeAccess>();
            scopeAccessList.add(delegatedClientScopeAccess);
            doReturn(scopeAccessList).when(spy).getDelegatedUserScopeAccessForUsername(null);
            spy.deleteDelegatedToken(new User(), null);
            assertTrue("should throw exception",false);
        }catch (Exception ex){

            assertThat("exception type", ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Token not found : null"));
        }
    }

    @Test
    public void deleteDelegatedToken_scopeAccessListHasDelegatedClientScopeAccessAndGetRefreshTokenStringNull_throwsNotFoundException() throws Exception {
        try{
            DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
            List<DelegatedClientScopeAccess> scopeAccessList = new ArrayList<DelegatedClientScopeAccess>();
            scopeAccessList.add(delegatedClientScopeAccess);
            doReturn(scopeAccessList).when(spy).getDelegatedUserScopeAccessForUsername(null);
            spy.deleteDelegatedToken(new User(),null);
            assertTrue("should throw exception",false);
        }catch (Exception ex){

            assertThat("exception type", ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Token not found : null"));
        }
    }

    @Test
    public void doesAccessTokenHavePermission_permissionIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesAccessTokenHavePermission(null,null);
            assertTrue("should throw exception",false);
        }catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument passed in."));
        }
    }

    @Test
    public void doesAccessTokenHaveService_tokenInstanceOfDelegatedClientScopeAccess_getsUniqueId() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry("dn=string",new Attribute("name"));
        ScopeAccess token = new DelegatedClientScopeAccess();
        token.setLdapEntry(ldapEntry);
        defaultScopeAccessService.doesAccessTokenHaveService(token,null);
        verify(scopeAccessDao).doesParentHaveScopeAccess(eq("dn=string"), any(ScopeAccess.class));
    }

    @Test
    public void doesAccessTokenHaveService_tokenNotInstanceOfDelegatedClientScopeAccess_getsParentDN() throws Exception {
        DN dn = new DN("cn=rdn,dc=parent");
        Attribute attribute = new Attribute("name");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(dn,attribute,attribute);
        ScopeAccess token = new ScopeAccess();
        token.setLdapEntry(ldapEntry);
        defaultScopeAccessService.doesAccessTokenHaveService(token,null);
        verify(scopeAccessDao).doesParentHaveScopeAccess(eq("dc=parent"), any(ScopeAccess.class));
    }

    @Test
    public void doesAccessTokenHaveService_tokenNotInstanceOfDelegatedClientScopeAccessAndThrowsLDAPException_stillSucceeds() throws Exception {
        Entry entry = new Entry("junk");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(entry);
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        defaultScopeAccessService.doesAccessTokenHaveService(token,null);
        verify(scopeAccessDao).doesParentHaveScopeAccess((String) eq(null), any(ScopeAccess.class));
    }

    @Test
    public void doesAccessTokenHaveService_returnsBoolean() throws Exception {
        Entry entry = new Entry("junk");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(entry);
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(scopeAccessDao.doesParentHaveScopeAccess((String) eq(null),any(ScopeAccess.class))).thenReturn(true);
        assertTrue("should return boolean from scopeAccessDao", defaultScopeAccessService.doesAccessTokenHaveService(token, null));
    }

    @Test
    public void doesUserHavePermissionForClient_nullParameters_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesUserHavePermissionForClient(null,null,null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type", ex.getClass().getName(), equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument(s) passed in."));
        }
    }

    @Test
    public void doesUserHavePermissionForClient_nullUserAndNullPermission_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesUserHavePermissionForClient(null,null,new Application());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument(s) passed in."));
        }
    }

    @Test
    public void doesUserHavePermissionForClient_nullUserAndNullClient_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesUserHavePermissionForClient(null,new Permission(),null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument(s) passed in."));
        }
    }

    @Test
    public void doesUserHavePermissionForClient_nullUser_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesUserHavePermissionForClient(null,new Permission(),new Application());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument(s) passed in."));
        }
    }


    @Test
    public void doesUserHavePermissionForClient_nullPermissionAndNullClient_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesUserHavePermissionForClient(new User(),null,null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument(s) passed in."));
        }
    }

    @Test
    public void doesUserHavePermissionForClient_nullPermission_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesUserHavePermissionForClient(new User(),null,new Application());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument(s) passed in."));
        }
    }

    @Test
    public void doesUserHavePermissionForClient_nullClient_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.doesUserHavePermissionForClient(new User(),new Permission(),null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(),equalTo("Null argument(s) passed in."));
        }
    }

    @Test
    public void doesUserHavePermissionForClient_setsClientId() throws Exception {
        Application client = mock(Application.class);
        doReturn(null).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        spy.doesUserHavePermissionForClient(new User(), new Permission(), client);
        verify(client).getClientId();
    }

    @Test
    public void doesUserHavePermissionForClient_setsPermissionId() throws Exception {
        Permission permission = mock(Permission.class);
        doReturn(null).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        spy.doesUserHavePermissionForClient(new User(), permission, new Application());
        verify(permission).getPermissionId();
    }

    @Test
    public void doesUserHavePermissionForClient_callsGetPermissionForParent() throws Exception {
        doReturn(null).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        spy.doesUserHavePermissionForClient(new User(), new Permission(), new Application());
        verify(spy).getPermissionForParent((String) eq(null), any(Permission.class));
    }

    @Test
    public void doesUserHavePermissionForClient_definedPermissionIsNull_returnsFalse() throws Exception {
        doReturn(null).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        assertThat("boolean", spy.doesUserHavePermissionForClient(new User(), new Permission(), new Application()), equalTo(false));
    }

    @Test
    public void doesUserHavePermissionForClient_definedPermissionNotEnabled_returnsFalse() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setEnabled(false);
        doReturn(definedPermission).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        assertThat("boolean",spy.doesUserHavePermissionForClient(new User(), new Permission(), new Application()),equalTo(false));
    }

    @Test
    public void doesUserHavePermissionForClient_definedPermissionGrantedByDefaultAndProvisioned_returnsTrue() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(true);
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(null,null)).thenReturn(new ScopeAccess());
        doReturn(definedPermission).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        assertThat("boolean",spy.doesUserHavePermissionForClient(new User(), new Permission(), new Application()),equalTo(true));
    }

    @Test
    public void doesUserHavePermissionForClient_definedPermissionGrantedByDefaultAndNotProvisioned_returnsFalse() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(true);
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(null,null)).thenReturn(null);
        doReturn(definedPermission).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        assertThat("boolean",spy.doesUserHavePermissionForClient(new User(), new Permission(), new Application()),equalTo(false));
    }

    @Test
    public void doesUserHavePermissionForClient_definedPermissionNotGrantedByDefaultAndGrantedPermission_returnsTrue() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(false);
        when(scopeAccessDao.getPermissionByParentAndPermission((String) eq(null),any(GrantedPermission.class))).thenReturn(new Permission());
        doReturn(definedPermission).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        assertThat("boolean",spy.doesUserHavePermissionForClient(new User(), new Permission(), new Application()),equalTo(true));
    }

    @Test
    public void doesUserHavePermissionForClient_definedPermissionNotGrantedByDefaultAndNotGrantedPermission_returnsFalse() throws Exception {
        DefinedPermission definedPermission = new DefinedPermission();
        definedPermission.setEnabled(true);
        definedPermission.setGrantedByDefault(false);
        when(scopeAccessDao.getPermissionByParentAndPermission((String) eq(null),any(GrantedPermission.class))).thenReturn(null);
        doReturn(definedPermission).when(spy).getPermissionForParent((String) eq(null),any(Permission.class));
        assertThat("boolean",spy.doesUserHavePermissionForClient(new User(), new Permission(), new Application()),equalTo(false));
    }

    @Test
    public void expireAccessToken_scopeAccessIsNull_doesNothingWithNoExceptions() throws Exception {
        defaultScopeAccessService.expireAccessToken(null);
    }

    @Test
    public void expireAccessToken_scopeAccessNotInstanceOfHasAccessToken_doesNotCallUpdateMethod() throws Exception {
        when(scopeAccessDao.getScopeAccessByAccessToken(null)).thenReturn(new ScopeAccess());
        defaultScopeAccessService.expireAccessToken(null);
        verify(scopeAccessDao,never()).updateScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void expireAllTokensForClient_clientIsNull_doesNothingWithNoException() throws Exception {
        defaultScopeAccessService.expireAllTokensForClient(null);
        verify(scopeAccessDao,never()).getScopeAccessesByParent(anyString());
    }

    @Test
    public void expireAllTokensForClient_scopeAccessNotInstanceOfHasAccessToken_doesNotCallUpdateMethod() throws Exception {
        List<ScopeAccess> saList = new ArrayList<ScopeAccess>();
        saList.add(new ScopeAccess());
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(scopeAccessDao.getScopeAccessesByParent(null)).thenReturn(saList);
        defaultScopeAccessService.expireAllTokensForClient(null);
        verify(scopeAccessDao,never()).updateScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void expireAllTokensForUser_userIsNull_doesNothingWithNoException() throws Exception {
        defaultScopeAccessService.expireAllTokensForUser(null);
        verify(scopeAccessDao,never()).getScopeAccessesByParent(anyString());
    }

    @Test
    public void expireAllTokensForUser_scopeAccessNotInstanceOfHasAccessToken_doesNotCallUpdateMehod() throws Exception {
        List<ScopeAccess> saList = new ArrayList<ScopeAccess>();
        saList.add(new ScopeAccess());
        when(userDao.getUserByUsername(null)).thenReturn(new User());
        when(scopeAccessDao.getScopeAccessesByParent(null)).thenReturn(saList);
        defaultScopeAccessService.expireAllTokensForUser(null);
        verify(scopeAccessDao,never()).updateScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void getDelegateScopeAccessesForParent_callsScopeAccessDaoMethod() throws Exception {
        defaultScopeAccessService.getDelegateScopeAccessesForParent(null);
        verify(scopeAccessDao).getDelegateScopeAccessesByParent(null);
    }

    @Test
    public void getDelegateScopeAccessesForParent_returnsList() throws Exception {
        List<ScopeAccess> sa = new ArrayList<ScopeAccess>();
        when(scopeAccessDao.getDelegateScopeAccessesByParent(null)).thenReturn(sa);
        assertThat("list",defaultScopeAccessService.getDelegateScopeAccessesForParent(null),equalTo(sa));
    }

    @Test
    public void getDelegateScopeAccessForParentByClientId_returnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessDao.getDelegateScopeAccessForParentByClientId(null,null)).thenReturn(scopeAccess);
        assertThat("scope access",defaultScopeAccessService.getDelegateScopeAccessForParentByClientId(null,null), equalTo(scopeAccess));
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_userNull_throwsIllegalArgument() throws Exception {
        try{
            defaultScopeAccessService.getOrCreatePasswordResetScopeAccessForUser(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message",ex.getMessage(),equalTo("Null argument passed in."));
        }
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessIsNull_getsDataFromUsers() throws Exception {
        User user = mock(User.class);
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("token").when(spy).generateToken();
        spy.getOrCreatePasswordResetScopeAccessForUser(user);
        verify(user).getId();
        verify(user, atLeastOnce()).getUsername();
        verify(user).getCustomerId();
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessIsNull_getsDefaultExpirationSeconds() throws Exception {
        User user = mock(User.class);
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("token").when(spy).generateToken();
        spy.getOrCreatePasswordResetScopeAccessForUser(user);
        verify(spy).getDefaultTokenExpirationSeconds();
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessIsNull_generatesToken() throws Exception {
        User user = mock(User.class);
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("token").when(spy).generateToken();
        spy.getOrCreatePasswordResetScopeAccessForUser(user);
        verify(spy).generateToken();
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessIsNull_addsDirectScopeAccess() throws Exception {
        User user = new User();
        user.setUniqueId("123");
        doReturn(100).when(spy).getDefaultTokenExpirationSeconds();
        doReturn("token").when(spy).generateToken();
        spy.getOrCreatePasswordResetScopeAccessForUser(user);
        verify(scopeAccessDao).addDirectScopeAccess(eq("123"),any(PasswordResetScopeAccess.class));
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessNotExpired_doesNotSetAccessTokenExp() throws Exception {
        User user = new User();
        PasswordResetScopeAccess prsa = mock(PasswordResetScopeAccess.class);
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(null,"PASSWORDRESET")).thenReturn(prsa);
        when(prsa.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        defaultScopeAccessService.getOrCreatePasswordResetScopeAccessForUser(user);
        verify(prsa,never()).setAccessTokenExp(any(Date.class));
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessNotExpired_doesNotSetAccessTokenString() throws Exception {
        User user = new User();
        PasswordResetScopeAccess prsa = mock(PasswordResetScopeAccess.class);
        when(prsa.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        defaultScopeAccessService.getOrCreatePasswordResetScopeAccessForUser(user);
        verify(prsa,never()).setAccessTokenString(anyString());
    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessNotExpired_doesNotUpdateScopeAccess() throws Exception {
        User user = new User();
        PasswordResetScopeAccess prsa = mock(PasswordResetScopeAccess.class);
        when(prsa.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        defaultScopeAccessService.getOrCreatePasswordResetScopeAccessForUser(user);
        verify(scopeAccessDao,never()).updateScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void getPermissionForParent_permissionIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.getPermissionForParent(null,null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type", ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null scope access object instance."));
        }
    }

    @Test
    public void getPermissionsForParent_permissionIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.getPermissionsForParent(null,null);
            assertTrue("should throw exception",false);
        } catch (Exception ex) {
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null scope access object instance."));
        }
    }

    @Test
    public void getPermissionsForParent_permissionIsNotNull_callsScopeAccessDaoMethod() throws Exception {
        Permission permission = new Permission();
        defaultScopeAccessService.getPermissionsForParent(null,permission);
        verify(scopeAccessDao).getPermissionsByParentAndPermission(null,permission);
    }

    @Test
    public void getPermissionsForParent_permissionIsNotNull_returnsList() throws Exception {
        List<Permission> list = new ArrayList<Permission>();
        Permission permission = new Permission();
        when(scopeAccessDao.getPermissionsByParentAndPermission(null,permission)).thenReturn(list);
        assertThat("list",defaultScopeAccessService.getPermissionsForParent(null,permission),equalTo(list));
    }

    @Test
    public void getPermissionsForParent_callsScopeAccessMethod() throws Exception {
        defaultScopeAccessService.getPermissionsForParent(null);
        verify(scopeAccessDao).getPermissionsByParent(null);
    }

    @Test
    public void getPermissionsForParent_returnsList() throws Exception {
        List<Permission> list = new ArrayList<Permission>();
        when(scopeAccessDao.getPermissionsByParent(null)).thenReturn(list);
        assertThat("list", defaultScopeAccessService.getPermissionsForParent(null), equalTo(list));
    }

    @Test
    public void getScopeAccessByAccessToken_accessTokenNull_throwsNotFoundException() throws Exception {
        try{
            defaultScopeAccessService.getScopeAccessByAccessToken(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Invalid accessToken; Token cannot be null"));
        }
    }


    @Test
    public void getScopeAccessByUserId_nullUserId_throwsNotFoundException() throws Exception {
        try{
            defaultScopeAccessService.getScopeAccessByUserId(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Invalid user id; user id cannot be null"));
        }
    }

    @Test
    public void loadScopeAccessByAccessToken_scopeAccessNull_throwsNotFoundException() throws Exception {
        try{
            doReturn(null).when(spy).getScopeAccessByAccessToken(null);
            spy.loadScopeAccessByAccessToken(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Token not found : null"));
        }
    }

    @Test
    public void loadScopeAccessByAccessToken_scopeAccessInstanceOfHasAccessTokenAndIsExpired_throwsNotFoundException() throws Exception {
        try{
            UserScopeAccess userScopeAccess = new UserScopeAccess();
            userScopeAccess.setAccessTokenExpired();
            doReturn(userScopeAccess).when(spy).getScopeAccessByAccessToken(null);
            spy.loadScopeAccessByAccessToken(null);
            assertTrue("should throw exception", false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Token expired : null"));
        }
    }

    @Test
    public void loadScopeAccessByAccessToken_scopeAccessInstanceOfHasAccessTokenAndNotExpired_returnsScopeAccess() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().plusMinutes(5).toDate());
        userScopeAccess.setAccessTokenString("token");
        doReturn(userScopeAccess).when(spy).getScopeAccessByAccessToken(null);
        assertThat("scope access", spy.loadScopeAccessByAccessToken(null), equalTo((ScopeAccess) userScopeAccess));
    }

    @Test
    public void loadScopeAccessByAccessToken_scopeAccessNotInstanceOfHasAccessToken_returnsScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessByAccessToken(null);
        assertThat("scope access",spy.loadScopeAccessByAccessToken(null),equalTo(scopeAccess));
    }

    @Test
    public void getDelegatedScopeAccessByRefreshToken_scopeAccessInstanceOfDelegatedClientScopeAccessAndUsernamesNotEqual_returnsNull() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setUsername("user");
        User user = new User();
        user.setUsername("jsmith");
        when(scopeAccessDao.getScopeAccessByRefreshToken(null)).thenReturn(delegatedClientScopeAccess);
        assertThat("returns null", defaultScopeAccessService.getDelegatedScopeAccessByRefreshToken(user, null), nullValue());
    }

    @Test
    public void getScopeAccessByAuthCode_returnsDelegatedClientScopeAccess() throws Exception {
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        when(scopeAccessDao.getScopeAccessByAuthorizationCode(null)).thenReturn(delegatedClientScopeAccess);
        assertThat("delegated client scope access", defaultScopeAccessService.getScopeAccessByAuthCode(null), equalTo(delegatedClientScopeAccess));
    }

    @Test
    public void getScopeAccessesForParentByClientId_returnsScopeAccessList() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        list.add(scopeAccess);
        when(scopeAccessDao.getScopeAccessesByParentAndClientId(null, null)).thenReturn(list);
        assertThat("returns list", defaultScopeAccessService.getScopeAccessesForParentByClientId(null, null),equalTo(list));
    }

    @Test
    public void updateUserScopeAccessTokenForClientIdByUser_userIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.updateUserScopeAccessTokenForClientIdByUser(null,null,null,null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null user object instance."));
        }
    }

    @Test
    public void updateUserScopeAccessTokenForClientIdByUser_scopeAccessIsNull_doesNotUpdate() throws Exception {
        doReturn(null).when(spy).getUserScopeAccessForClientId(null,null);
        spy.updateUserScopeAccessTokenForClientIdByUser(new User(),null,null,null);
        verify(scopeAccessDao,never()).updateScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void updateUserScopeAccessTokenForClientIdByUser_scopeAccessIsNotNull_correctlySetsScopeAccess() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        Date expires = new Date();
        doReturn(scopeAccess).when(spy).getUserScopeAccessForClientId(null,null);
        spy.updateUserScopeAccessTokenForClientIdByUser(new User(), null, "token", expires);
        assertThat("token", scopeAccess.getAccessTokenString(), equalTo("token"));
        assertThat("expires date", scopeAccess.getAccessTokenExp(), equalTo(expires));
    }

    @Test
    public void updateUserScopeAccessTokenForClientIdByUser_scopeAccessIsNotNull_callsUpdateMethod() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(scopeAccess).when(spy).getUserScopeAccessForClientId(null, null);
        spy.updateUserScopeAccessTokenForClientIdByUser(new User(), null, "token", null);
        verify(scopeAccessDao).updateScopeAccess(scopeAccess);
    }

    @Test
    public void grantPermissionToClient_permissionIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.grantPermissionToClient(null, null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null argument passed in."));
        }
    }

    @Test
    public void grantPermissionToClient_permissionNotAssociatedWithClient_throwsNotFoundException() throws Exception {
        try{
            when(clientDao.getClientByClientId(null)).thenReturn(new Application());
            defaultScopeAccessService.grantPermissionToClient(null,new GrantedPermission());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Permission null not found for client null"));
        }
    }

    @Test
    public void grantPermissionToClient_noScopeAccessForParent_getsDataFromPermission() throws Exception {
        Permission perm = mock(Permission.class);
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(scopeAccessDao.getPermissionByParentAndPermission((String) eq(null), any(DefinedPermission.class))).thenReturn(perm);
        doReturn(null).when(spy).getDirectScopeAccessForParentByClientId(null, null);
        doReturn(new ScopeAccess()).when(spy).addDirectScopeAccess((String) eq(null), any(ScopeAccess.class));
        spy.grantPermissionToClient(null, new GrantedPermission());
        verify(perm,atLeastOnce()).getClientId();
        verify(perm,atLeastOnce()).getCustomerId();
    }

    @Test
    public void grantPermissionToClient_noScopeAccessForParent_addsDirectScopeAccessForParent() throws Exception {
        Permission perm = mock(Permission.class);
        when(clientDao.getClientByClientId(null)).thenReturn(new Application());
        when(scopeAccessDao.getPermissionByParentAndPermission((String) eq(null), any(DefinedPermission.class))).thenReturn(perm);
        doReturn(null).when(spy).getDirectScopeAccessForParentByClientId(null, null);
        doReturn(new ScopeAccess()).when(spy).addDirectScopeAccess((String) eq(null), any(ScopeAccess.class));
        spy.grantPermissionToClient(null, new GrantedPermission());
        verify(spy).addDirectScopeAccess((String) eq(null), any(ScopeAccess.class));
    }

    @Test
    public void grantPermissionToUser_nullPermissionAndNullUser_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.grantPermissionToUser(null, null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null argument passed in."));
        }
    }

    @Test
    public void grantPermissionToUser_nullPermissionAndUserExists_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.grantPermissionToUser(new User(), null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null argument passed in."));
        }
    }

    @Test
    public void grantPermissionToUser_PermissionExistsAndNullUser_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.grantPermissionToUser(null, new GrantedPermission());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null argument passed in."));
        }
    }

    @Test
    public void grantPermissionToUser_nullClient_throwsNotFoundException() throws Exception {
        try{
            when(clientDao.getClientByCustomerIdAndClientId(null, null)).thenReturn(null);
            defaultScopeAccessService.grantPermissionToUser(new User(), new GrantedPermission());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Client null not found"));
        }
    }

    @Test
    public void grantPermissionToUser_permissionNotAssociatedWithClient_throwsNotFoundException() throws Exception {
        try{
            when(clientDao.getClientByCustomerIdAndClientId(null,null)).thenReturn(new Application());
            defaultScopeAccessService.grantPermissionToUser(new User(), new GrantedPermission());
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Permission null not found for client null"));
        }
    }

    @Test
    public void grantPermissionToUser_noScopeAccessForParent_getsDataFromPermission() throws Exception {
        Permission perm = mock(Permission.class);
        when(clientDao.getClientByCustomerIdAndClientId(null,null)).thenReturn(new Application());
        when(scopeAccessDao.getPermissionByParentAndPermission((String) eq(null), any(DefinedPermission.class))).thenReturn(perm);
        doReturn(null).when(spy).getDirectScopeAccessForParentByClientId(null, null);
        doReturn(new UserScopeAccess()).when(spy).addDirectScopeAccess((String) eq(null), any(ScopeAccess.class));
        spy.grantPermissionToUser(new User(), new GrantedPermission());
        verify(perm,atLeastOnce()).getClientId();
        verify(perm,atLeastOnce()).getCustomerId();
    }

    @Test
    public void grantPermissionToUser_noScopeAccessForParent_getsDataFromUser() throws Exception {
        Permission perm = mock(Permission.class);
        User user = mock(User.class);
        when(clientDao.getClientByCustomerIdAndClientId(null,null)).thenReturn(new Application());
        when(scopeAccessDao.getPermissionByParentAndPermission((String) eq(null), any(DefinedPermission.class))).thenReturn(perm);
        doReturn(null).when(spy).getDirectScopeAccessForParentByClientId(null, null);
        doReturn(new UserScopeAccess()).when(spy).addDirectScopeAccess((String) eq(null), any(ScopeAccess.class));
        spy.grantPermissionToUser(user, new GrantedPermission());
        verify(user, atLeastOnce()).getUsername();
        verify(user,atLeastOnce()).getCustomerId();
        verify(user,atLeastOnce()).getId();
        verify(user,atLeastOnce()).getUniqueId();
    }

    @Test
    public void grantPermissionToUser_noScopeAccessForParent_addsDirectScopeAccessForParent() throws Exception {
        Permission perm = mock(Permission.class);
        when(clientDao.getClientByCustomerIdAndClientId(null, null)).thenReturn(new Application());
        when(scopeAccessDao.getPermissionByParentAndPermission((String) eq(null), any(DefinedPermission.class))).thenReturn(perm);
        doReturn(null).when(spy).getDirectScopeAccessForParentByClientId(null, null);
        doReturn(new UserScopeAccess()).when(spy).addDirectScopeAccess((String) eq(null), any(ScopeAccess.class));
        spy.grantPermissionToUser(new User(), new GrantedPermission());
        verify(spy).addDirectScopeAccess((String) eq(null), any(ScopeAccess.class));
    }

    @Test
    public void removePermission_permissionIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.removePermission(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null scope access object instance."));
        }
    }

    @Test
    public void updatePermission_permissionIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.updatePermission(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("java.lang.IllegalArgumentException"));
            assertThat("exception message", ex.getMessage(), equalTo("Null scope access object instance."));
        }
    }

    @Test
    public void deleteScopeAccessesForParentByApplicationId_listPopulated_callsDeleteScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        list.add(scopeAccess);
        doReturn(list).when(spy).getScopeAccessesForParentByClientId(null, null);
        doNothing().when(spy).deleteScopeAccess(scopeAccess);
        spy.deleteScopeAccessesForParentByApplicationId(null,null);
        verify(spy).deleteScopeAccess(scopeAccess);
    }

    @Test
    public void deleteScopeAccessesForParentByApplicationId_listEmpty_doesNotCallDeleteScopeAccess() throws Exception {
        List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        doReturn(list).when(spy).getScopeAccessesForParentByClientId(null, null);
        spy.deleteScopeAccessesForParentByApplicationId(null,null);
        verify(spy,never()).deleteScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void updateExpiredUserScopeAccess_isExpiredAndWithinRefreshWindow_updatesScopeAccess() throws Exception {
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        doReturn(100).when(spy).getRefreshTokenWindow();
        when(scopeAccess.isAccessTokenWithinRefreshWindow(100)).thenReturn(true);
        doReturn(100).when(spy).getDefaultCloudAuthTokenExpirationSeconds();
        doReturn("foo").when(spy).generateToken();
        spy.updateExpiredUserScopeAccess(scopeAccess);
        verify(scopeAccessDao).updateScopeAccess(scopeAccess);
    }

    @Test
    public void updateExpiredUserScopeAccess_isExpiredAndNotWithinRefreshWindow_updatesScopeAccess() throws Exception {
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(true);
        doReturn(100).when(spy).getRefreshTokenWindow();
        when(scopeAccess.isAccessTokenWithinRefreshWindow(100)).thenReturn(false);
        doReturn(100).when(spy).getDefaultCloudAuthTokenExpirationSeconds();
        doReturn("foo").when(spy).generateToken();
        spy.updateExpiredUserScopeAccess(scopeAccess);
        verify(scopeAccessDao).updateScopeAccess(scopeAccess);
    }

    @Test
    public void updateExpiredUserScopeAccess_isNotExpiredAndWithinRefreshWindow_updatesScopeAccess() throws Exception {
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        doReturn(100).when(spy).getRefreshTokenWindow();
        when(scopeAccess.isAccessTokenWithinRefreshWindow(100)).thenReturn(true);
        doReturn(100).when(spy).getDefaultCloudAuthTokenExpirationSeconds();
        doReturn("foo").when(spy).generateToken();
        spy.updateExpiredUserScopeAccess(scopeAccess);
        verify(scopeAccessDao).updateScopeAccess(scopeAccess);
    }

    @Test
    public void updateExpiredUserScopeAccess_isNotExpiredAndNotWithinRefreshWindow_doesNotUpdateScopeAccess() throws Exception {
        UserScopeAccess scopeAccess = mock(UserScopeAccess.class);
        when(scopeAccess.isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        doReturn(100).when(spy).getRefreshTokenWindow();
        when(scopeAccess.isAccessTokenWithinRefreshWindow(100)).thenReturn(false);
        spy.updateExpiredUserScopeAccess(scopeAccess);
        verify(scopeAccessDao,never()).updateScopeAccess(scopeAccess);
    }

    @Test
    public void updateExpiredUserScopeAccess() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        when(scopeAccessDao.updateScopeAccess(userScopeAccess)).thenReturn(true);
        defaultScopeAccessService.updateExpiredUserScopeAccess(userScopeAccess);
        assertThat("updatesExpiredUserScopeAccess", userScopeAccess.isAccessTokenExpired(new DateTime()), equalTo(false));
    }

    @Test(expected = NotFoundException.class)
    public void getUserScopeAccessForClientId_withNonExistentUser_throwsNotFoundException(){
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(),anyString())).thenReturn(null);
        defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
    }

    @Test
    public void getUserScopeAccessForClientId_withExpiredScopeAccess_returnsNewToken(){
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().minusDays(2).toDate());
        userScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(scopeAccessDao.updateScopeAccess(userScopeAccess)).thenReturn(true);
        UserScopeAccess uas = defaultScopeAccessService.getValidUserScopeAccessForClientId("12345", "12345");
        uas.isAccessTokenExpired(new DateTime());
        assertThat("newUserScopeAccess", uas.getAccessTokenString(), not("1234567890"));
    }

    @Test
    public void getUserScopeAccessForClientId_withinRefreshWindow_returnsNewToken(){
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().plusHours(5).toDate());
        userScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(scopeAccessDao.updateScopeAccess(userScopeAccess)).thenReturn(true);
        UserScopeAccess uas = defaultScopeAccessService.getValidUserScopeAccessForClientId("12345", "12345");
        assertThat("newUserScopeAccessWithinWindow", uas.getAccessTokenString(), not("1234567890"));
    }

    @Test
    public void getUserScopeAccessForClientId_withNonExpiredScopeAccess_returnsSameToken(){
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        userScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        UserScopeAccess uas = defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
        assertThat("newUserScopeAccessNoneExpired", uas.getAccessTokenString(), equalTo("1234567890"));
    }

    @Test
    public void getRackerScopeAccessForClientId_withExpiredScopeAccess_returnsNewToken(){
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenExp(new DateTime().minusDays(2).toDate());
        rackerScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(rackerScopeAccess);
        when(scopeAccessDao.updateScopeAccess(rackerScopeAccess)).thenReturn(true);
        RackerScopeAccess rsa = defaultScopeAccessService.getValidRackerScopeAccessForClientId("12345", "12345", "12345");
        rsa.isAccessTokenExpired(new DateTime());
        assertThat("newRackerScopeAccess", rsa.getAccessTokenString(), not("1234567890"));
    }

    @Test
    public void getRackerScopeAccessForClientId_withNullScopeAccess_returnsNewToken(){
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenExp(new DateTime().minusDays(2).toDate());
        rackerScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        when(scopeAccessDao.updateScopeAccess(rackerScopeAccess)).thenReturn(true);
        RackerScopeAccess rsa = defaultScopeAccessService.getValidRackerScopeAccessForClientId("12345", "12345", "12345");
        verify(scopeAccessDao).addDirectScopeAccess(anyString(), Matchers.<ScopeAccess>anyObject());
        rsa.isAccessTokenExpired(new DateTime());
        assertThat("newRackerScopeAccess", rsa.getAccessTokenString(), not("1234567890"));
    }

    @Test (expected = NotAuthenticatedException.class)
    public void handleApiKeyUsernameAuthenticationFailure_notAuthenticated_throwsNotAuthenticated() throws Exception {
        UserAuthenticationResult result = new UserAuthenticationResult(new User(), false);
        defaultScopeAccessService.handleApiKeyUsernameAuthenticationFailure("username", result);
    }

    @Test
    public void handleApiKeyUsernameAuthenticationFailure_authenticated_doesNothing() throws Exception {
        UserAuthenticationResult result = new UserAuthenticationResult(new User(), true);
        defaultScopeAccessService.handleApiKeyUsernameAuthenticationFailure("username", result);
    }

    @Test (expected = NotAuthenticatedException.class)
    public void getUserScopeAccessForClientIdByUsernameAndApiCredentials_notAuthenticated_throwsNotAuthenticated() throws Exception {
        when(userDao.authenticateByAPIKey("username", "apiKey")).thenReturn(new UserAuthenticationResult(new User(), false));
        defaultScopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials("username", "apiKey", "clientId");
    }

    @Test
    public void getDefaultImpersonatedTokenExpirationSeconds_returnsInt() throws Exception {
        when(configuration.getInt("token.impersonatedExpirationSeconds")).thenReturn(100);
        assertThat("int", defaultScopeAccessService.getDefaultImpersonatedTokenExpirationSeconds(), equalTo(100));
    }

    @Test
    public void handleAuthenticationFailure_notAuthenticated_throwsNotAuthenticatedException() throws Exception {
        try{
            UserAuthenticationResult result = new UserAuthenticationResult(null,false);
            result.isAuthenticated();
            defaultScopeAccessService.handleAuthenticationFailure(null,result);
            assertTrue("should throw exception",false);
        } catch (NotAuthenticatedException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Unable to authenticate user with credentials provided."));
        }
    }

    @Test (expected = NotFoundException.class)
    public void getScopeAccessListByUserId_userIdIsNull_throwsNotFoundException() throws Exception {
        defaultScopeAccessService.getScopeAccessListByUserId(null);
    }

    @Test
    public void getScopeAccessListByUserId_callsScopeAccessDao_getScopeAccessListByUserId() throws Exception {
        defaultScopeAccessService.getScopeAccessListByUserId("userId");
        verify(scopeAccessDao).getScopeAccessListByUserId("userId");
    }

    @Test
    public void getScopeAccessListByUserId_returnsScopeAccessList() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<ScopeAccess> scopeAccessList = new ArrayList<ScopeAccess>();
        scopeAccessList.add(scopeAccess);
        when(scopeAccessDao.getScopeAccessListByUserId("userId")).thenReturn(scopeAccessList);
        List<ScopeAccess> result = defaultScopeAccessService.getScopeAccessListByUserId("userId");
        assertThat("scope access", result.get(0), equalTo(scopeAccess));
        assertThat("list", result.size(), equalTo(1));
    }
}
