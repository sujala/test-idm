package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/21/12
 * Time: 9:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultAuthorizationServiceTestOld {

    DefaultAuthorizationService defaultAuthorizationService;
    ScopeAccessService scopeAccessService = mock(ScopeAccessService.class);
    ApplicationService applicationService = mock(ApplicationService.class);
    Configuration config = mock(Configuration.class);
    TenantService tenantSerivce = mock(TenantService.class);
    UserService userService = mock(UserService.class);

    @Before
    public void setUp() throws Exception {
        defaultAuthorizationService = new DefaultAuthorizationService();
        defaultAuthorizationService.setConfig(config);
        defaultAuthorizationService.setTenantService(tenantSerivce);
        defaultAuthorizationService.setScopeAccessService(scopeAccessService);
        defaultAuthorizationService.setApplicationService(applicationService);
        defaultAuthorizationService.setUserService(userService);

        when(applicationService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(new ClientRole());
        defaultAuthorizationService.retrieveAccessControlRoles();
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdMatchesAndTargetNotInstanceOfClientScopeAccessOrUserScopeAccessOrRackerScopeAccess_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new ScopeAccess();
        targetScopeAccess.setClientId("abc");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("ABC");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfUserScopeAccessAndUsernamesMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new UserScopeAccess();
        targetScopeAccess.setClientId("123");
        ((UserScopeAccess) targetScopeAccess).setUserRsId("jsmith");
        ((UserScopeAccess) targetScopeAccess).setUsername("jsmith");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        ((UserScopeAccess) requestingScopeAccess).setUserRsId("jsmith");
        ((UserScopeAccess) requestingScopeAccess).setUsername("jsmith");
        requestingScopeAccess.setClientId("123");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("456");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("123");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        ((RackerScopeAccess)requestingScopeAccess).setRackerId("123");
        requestingScopeAccess.setClientId("456");
        assertThat("boolean", defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess, requestingScopeAccess), equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsMatch_returnsTrue() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("4");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("123");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        ((RackerScopeAccess)requestingScopeAccess).setRackerId("123");
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(true));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfClientScopeAccessAndClientIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ClientScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetNotInstanceOfClientScopeAccessOrUserScopeAccessOrRackerScopeAccess_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new ClientScopeAccess();
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("456");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("rclements");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((RackerScopeAccess) requestingScopeAccess).setRackerId("jsmith");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfClientScopeAccessAndClientIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ClientScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        requestingScopeAccess.setClientId("456");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfUserScopeAccessAndUsernamesDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new UserScopeAccess();
        targetScopeAccess.setClientId("4");
        ((UserScopeAccess) targetScopeAccess).setUserRsId("rclements");
        ((UserScopeAccess) targetScopeAccess).setUsername("rclements");
        ScopeAccess requestingScopeAccess = new UserScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((UserScopeAccess) requestingScopeAccess).setUserRsId("jsmith");
        ((UserScopeAccess) requestingScopeAccess).setUsername("jsmith");
        assertThat("boolean", defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess, requestingScopeAccess), equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetInstanceOfRackerScopeAccessAndRackerIdsDoNotMatch_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new RackerScopeAccess();
        targetScopeAccess.setClientId("4");
        ((RackerScopeAccess) targetScopeAccess).setRackerId("rclements");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((RackerScopeAccess) requestingScopeAccess).setRackerId("jsmith");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void authorizeAsRequestorOrOwner_requestorNotInstanceOfClientScopeAccessAndClientIdsDoNotMatchAndTargetNotInstanceOfClientScopeAccessOrUserScopeAccessOrRackerScopeAccess_returnsFalse() throws Exception {
        ScopeAccess targetScopeAccess = new ScopeAccess();
        targetScopeAccess.setClientId("4");
        ScopeAccess requestingScopeAccess = new RackerScopeAccess();
        requestingScopeAccess.setClientId("456");
        ((RackerScopeAccess) requestingScopeAccess).setRackerId("jsmith");
        assertThat("boolean",defaultAuthorizationService.authorizeAsRequestorOrOwner(targetScopeAccess,requestingScopeAccess),equalTo(false));
    }

    @Test
    public void verifySelf_sameUsernameAndUniqueId_succeeds() throws Exception {
            User user1 = new User();
            user1.setUniqueId("foo");
            user1.setId("foo");
            user1.setUsername("foo");
            User user2 = new User();
            user2.setUniqueId("foo");
            user2.setId("foo");
            user2.setUsername("foo");
            defaultAuthorizationService.verifySelf(user1, user2);
    }

    @Test
    public void verifySelf_differentUsername_throwsForbiddenException() throws Exception {
       try{
           User user1 = new User();
           user1.setId("foo");
           user1.setUsername("foo");
           User user2 = new User();
           user2.setId("foo");
           user2.setUsername("!foo");
           defaultAuthorizationService.verifySelf(user1, user2);
           assertTrue("should throw exception", false);
       }catch (ForbiddenException ex){
           assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
       }
    }

    @Test
    public void verifySelf_differentUniqueId_throwsForbiddenException() throws Exception {
        try{
            User user1 = new User();
            user1.setUniqueId("foo1");
            user1.setId("foo");
            user1.setUsername("foo");
            User user2 = new User();
            user1.setUniqueId("foo2");
            user2.setId("foo");
            user2.setUsername("foo");
            defaultAuthorizationService.verifySelf(user1, user2);
            assertTrue("should throw exception", false);
        }catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not Authorized"));
        }

    }

    @Test
    public void verifySelf_differentUsernameAndDifferentUniqueId_throwsForbiddenException() throws Exception {
        try{
            User user1 = new User();
            user1.setId("foo");
            user1.setUsername("foo");
            User user2 = new User();
            user2.setId("foo");
            user2.setUsername("!foo");
            defaultAuthorizationService.verifySelf(user1, user2);
            assertTrue("should throw exception", false);
        }catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyDomain_domainIdIsNull_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            caller.setId("1");
            User retrievedUser = new User();
            retrievedUser.setId("2");
            retrievedUser.setDomainId("domainId");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
            assertTrue("should throw exception", false);
        } catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyDomain_domainIdsAreNull_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            User retrievedUser = new User();
            caller.setId("1");
            retrievedUser.setId("2");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
            assertTrue("should throw exception", false);
        } catch (ForbiddenException ex){
            assertThat("exception message", ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void verifyDomain_callerEqualsRetrievedUser_doNothing() throws Exception {
        try{
            User caller = new User();
            User retrievedUser = new User();
            caller.setId("1");
            retrievedUser.setId("1");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
        } catch (ForbiddenException ex){
            assertTrue("should not throw exception", false);
        }
    }

    @Test
    public void verifyDomain_callerDomainIdNotMatchRetrievedUserDomainId_throwsForbiddenException() throws Exception {
        try{
            User caller = new User();
            caller.setDomainId("notSame");
            User retrievedUser = new User();
            retrievedUser.setDomainId("domainId");
            caller.setId("1");
            retrievedUser.setId("2");
            defaultAuthorizationService.verifyDomain(retrievedUser, caller);
            assertTrue("should throw exception",false);
        }catch (ForbiddenException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Not Authorized"));
        }
    }

    @Test
    public void checkAuthAndHandleFailure_isAuthorized_doesNothing() throws Exception {
        defaultAuthorizationService.checkAuthAndHandleFailure(true,null);
    }
}
