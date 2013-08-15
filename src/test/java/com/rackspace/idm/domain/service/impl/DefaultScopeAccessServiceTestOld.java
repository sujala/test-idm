package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
public class DefaultScopeAccessServiceTestOld {

    @InjectMocks
    DefaultScopeAccessService defaultScopeAccessService = new DefaultScopeAccessService();
    DefaultScopeAccessService spy;
    @Mock
    ScopeAccessDao scopeAccessDao;
    @Mock
    Configuration configuration;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private TenantService tenantService;
    @Mock
    private EndpointService endpointService;
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
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantService).getTenantRolesForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tokenNotInstanceOfDelegatedClientScopeAccess_setsParentUniqueId() throws Exception {
        DN dn = new DN("cn=rdn,dc=parent");
        Attribute attribute = new Attribute("name");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(dn,attribute,attribute);
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantService).getTenantRolesForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tokenNotInstanceOfDelegatedClientScopeAccessAndFails_stillPasses() throws Exception {
        Entry entry = new Entry("junk");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(entry);
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantService).getTenantRolesForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_rolesNull_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(null);
        List<OpenstackEndpoint> endpoints = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size", endpoints.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_rolesEmpty_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(new ArrayList<TenantRole>());
        List<OpenstackEndpoint> endpoints = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size", endpoints.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_roleHasTenantIdAndIdValid_addsTenantToList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        role.getTenantIds().add("123");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        Tenant tenant = new Tenant();
        when(tenantService.getTenantRolesForScopeAccess(any(ScopeAccess.class))).thenReturn(roles);
        when(tenantService.getTenant("123")).thenReturn(tenant);
        when(endpointService.getOpenStackEndpointForTenant(tenant)).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(endpointService).getOpenStackEndpointForTenant(tenant);
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_roleHasTenantIdAndIdNotValid_doesNotAddTenantToList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        role.getTenantIds().add("123");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(roles);
        when(tenantService.getTenant("123")).thenReturn(null);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(endpointService,never()).getOpenStackEndpointForTenant(any(Tenant.class));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_noTenantIds_doesNotCallTenantServiceMethod() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        role.getTenantIds().clear();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(roles);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantService,never()).getTenant(anyString());
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tenantIdsNull_doesNotCallTenantServiceMethod() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        TenantRole role = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(roles);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantService,never()).getTenant(anyString());
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_rolesEmpty_doesNotCallTenantServiceMethod() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(roles);
        defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        verify(tenantService,never()).getTenant(anyString());
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_endPointsExist_returnsListWithEndpoint() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);

        TenantRole role = new TenantRole();
        role.getTenantIds().add("123");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        Tenant tenant = new Tenant();

        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        endpoint.getBaseUrls().add(new CloudBaseUrl());

        when(tenantService.getTenantRolesForScopeAccess(any(ScopeAccess.class))).thenReturn(roles);
        when(tenantService.getTenant("123")).thenReturn(tenant);
        when(endpointService.getOpenStackEndpointForTenant(tenant)).thenReturn(endpoint);

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
        role.getTenantIds().add("123");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        Tenant tenant = new Tenant();

        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        endpoint.getBaseUrls().add(new CloudBaseUrl());

        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(roles);
        when(tenantService.getTenant("123")).thenReturn(tenant);
        when(endpointService.getOpenStackEndpointForTenant(tenant)).thenReturn(null);

        List<OpenstackEndpoint> endpointList = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size",endpointList.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_baseUrlsListEmpty_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);

        TenantRole role = new TenantRole();
        role.getTenantIds().add("123");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        Tenant tenant = new Tenant();

        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());

        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(roles);
        when(tenantService.getTenant("123")).thenReturn(tenant);
        when(endpointService.getOpenStackEndpointForTenant(tenant)).thenReturn(endpoint);

        List<OpenstackEndpoint> endpointList = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size",endpointList.size(),equalTo(0));
    }

    @Test
    public void getOpenStackEndpointsForScopeAccess_tenantsEmpty_returnsEmptyList() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(new Entry("junk"));
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);

        TenantRole role = new TenantRole();
        role.getTenantIds().add("123");
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(role);

        when(tenantService.getTenantRolesForScopeAccess(null)).thenReturn(roles);
        when(tenantService.getTenant("123")).thenReturn(null);

        List<OpenstackEndpoint> endpointList = defaultScopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        assertThat("size",endpointList.size(),equalTo(0));
    }

    @Test
    public void addImpersonatedScopeAccess_tokenExistsAndIsExpiredAndImpersonatingTokenNotNullAndImpersonatingTokenEqualToParameter_callsSetImpersonatedScopeAccess() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setImpersonatingToken("token");
        User user = new User();
        when(scopeAccessDao.getAllImpersonatedScopeAccessForUser(null)).thenReturn(new ArrayList<ScopeAccess>());
        spy.addImpersonatedScopeAccess(user, null, "token", impersonationRequest);
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
            defaultScopeAccessService.addUserScopeAccess(null, null);
            assertTrue("illegalArgumentException expected",false);
        }catch (IllegalArgumentException ex)
        {
            assertThat("exception message", ex.getMessage(),equalTo("Null argument passed in."));
        }
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
    public void doesAccessTokenHaveService_tokenInstanceOfDelegatedClientScopeAccess_getsUniqueId() throws Exception {
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry("dn=string",new Attribute("name"));
        ScopeAccess token = new DelegatedClientScopeAccess();
        token.setLdapEntry(ldapEntry);
        defaultScopeAccessService.doesAccessTokenHaveService(token,null);
        verify(scopeAccessDao).doesParentHaveScopeAccess(eq("dn=string"), any(ScopeAccess.class));
    }

    @Test
    public void doesAccessTokenHaveService_returnsBoolean() throws Exception {
        Entry entry = new Entry("junk");
        ReadOnlyEntry ldapEntry = new ReadOnlyEntry(entry);
        ScopeAccess token = new UserScopeAccess();
        token.setLdapEntry(ldapEntry);
        when(scopeAccessDao.doesParentHaveScopeAccess((String) eq(null),any(ScopeAccess.class))).thenReturn(true);
        assertTrue("should return boolean from scopeAccessService", defaultScopeAccessService.doesAccessTokenHaveService(token, null));
    }

    @Test
    public void expireAccessToken_scopeAccessIsNull_doesNothingWithNoExceptions() throws Exception {
        defaultScopeAccessService.expireAccessToken(null);
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
    public void getOrCreatePasswordResetScopeAccessForUser_passwordResetScopeAccessNotExpired_doesNotSetAccessTokenExp() throws Exception {
        User user = new User();
        PasswordResetScopeAccess prsa = mock(PasswordResetScopeAccess.class);
        when(scopeAccessDao.getMostRecentScopeAccessByClientId(null, "PASSWORDRESET")).thenReturn(prsa);
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
        when(scopeAccessDao.getDelegatedScopeAccessByAuthorizationCode(null)).thenReturn(delegatedClientScopeAccess);
        assertThat("delegated client scope access", defaultScopeAccessService.getScopeAccessByAuthCode(null), equalTo(delegatedClientScopeAccess));
    }

    @Test
    public void getScopeAccessesForParentByClientId_returnsScopeAccessList() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        list.add(scopeAccess);
        when(scopeAccessDao.getScopeAccessesByParentAndClientId(null, null)).thenReturn(list);
        assertThat("returns list", defaultScopeAccessService.getScopeAccessesForUserByClientId(null, null),equalTo(list));
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
    public void deleteScopeAccessesForParentByApplicationId_listPopulated_callsDeleteScopeAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        list.add(scopeAccess);
        doReturn(list).when(spy).getScopeAccessesForUserByClientId(null, null);
        doNothing().when(spy).deleteScopeAccess(scopeAccess);
        spy.deleteScopeAccessesForApplication(null, null);
        verify(spy).deleteScopeAccess(scopeAccess);
    }

    @Test
    public void deleteScopeAccessesForParentByApplicationId_listEmpty_doesNotCallDeleteScopeAccess() throws Exception {
        List<ScopeAccess> list = new ArrayList<ScopeAccess>();
        doReturn(list).when(spy).getScopeAccessesForUserByClientId(null, null);
        spy.deleteScopeAccessesForApplication(null, null);
        verify(spy,never()).deleteScopeAccess(any(ScopeAccess.class));
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
        when(userService.authenticateWithApiKey("username", "apiKey")).thenReturn(new UserAuthenticationResult(new User(), false));
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
