package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import junit.framework.Assert;
import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScopeAccessServiceTests extends ServiceTestsBase {

    UserService userService;
    LdapScopeAccessRepository scopeAccessDao;
    ApplicationService applicationService;
    TenantService tenantService;
    EndpointService endpointService;
    AuthHeaderHelper authHeaderHelper;
    ScopeAccessService scopeAccessService;
    AtomHopperClient atomHopperClient;

    @Before
    public void setUp() throws Exception {
        Configuration appConfig = new PropertyFileConfiguration().getConfig();
        authHeaderHelper = new AuthHeaderHelper();

        userService = EasyMock.createMock(UserService.class);
        applicationService = EasyMock.createMock(ApplicationService.class);
        scopeAccessDao = EasyMock.createMock(LdapScopeAccessRepository.class);
        endpointService = EasyMock.createMock(EndpointService.class);
        tenantService = EasyMock.createMock(TenantService.class);
        atomHopperClient = EasyMock.createMock(AtomHopperClient.class);
        userService = EasyMock.createMock(DefaultUserService.class);
        scopeAccessService = new DefaultScopeAccessService();
        scopeAccessService.setUserService(userService);
        scopeAccessService.setApplicationService(applicationService);
        scopeAccessService.setScopeAccessDao(scopeAccessDao);
        scopeAccessService.setTenantService(tenantService);
        scopeAccessService.setEndpointService(endpointService);
        scopeAccessService.setAuthHeaderHelper(authHeaderHelper);
        scopeAccessService.setAppConfig(appConfig);
        scopeAccessService.setAtomHopperClient(atomHopperClient);
        scopeAccessService.setUserService(userService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotUpdateScopeAccessIfScopeAccessIsNull() {
        scopeAccessService.updateScopeAccess(null);
    }

    private List<ScopeAccess> getScopeAccessList(UserScopeAccess sa) {
        List<ScopeAccess> saList = new ArrayList<ScopeAccess>();
        saList.add(sa);
        return saList;
    }

    @Test
    public void shouldGetScopeAccessByRefreshToken() {
        UserScopeAccess usao = getFakeUserScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByRefreshToken("refreshToken"))
            .andReturn(usao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getScopeAccessByRefreshToken("refreshToken");
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetScopeAccessByAccessToken() {
        UserScopeAccess usao = getFakeUserScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByAccessToken("accessToken"))
            .andReturn(usao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getScopeAccessByAccessToken("accessToken");
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetAccessTokenByAuthHeader() {
        ScopeAccess fakeScopeAccess = getFakeScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByAccessToken("accessToken"))
            .andReturn(fakeScopeAccess);
        EasyMock.replay(scopeAccessDao);
        ScopeAccess sa = scopeAccessService
            .getAccessTokenByAuthHeader("accessToken");
        EasyMock.verify(scopeAccessDao);
        Assert.assertEquals(fakeScopeAccess, sa);
    }

    @Test
    public void shouldAuthenticateAccessToken() {
        UserScopeAccess sao = getFakeUserScopeAccess();
        String accessTokenStr = "accessTokenString";
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByAccessToken(accessTokenStr))
            .andReturn(sao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.authenticateAccessToken(accessTokenStr);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetDelegatedScopeAccessByUsername() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        List<DelegatedClientScopeAccess> list = new ArrayList<DelegatedClientScopeAccess>();
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        list.add(delegatedClientScopeAccess);
        EasyMock.expect(scopeAccessDao.getDelegatedClientScopeAccessByUsername(user.getUsername())).andReturn(list);
        EasyMock.replay(scopeAccessDao);
        
        List<DelegatedClientScopeAccess> retVal = scopeAccessService.getDelegatedUserScopeAccessForUsername(user.getUsername());
        Assert.assertNotNull(retVal);
        Assert.assertEquals(delegatedClientScopeAccess.getAccessTokenString(), retVal.get(0).getAccessTokenString());
        
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotGetDelegatedScopeAccessByUsername() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        List<DelegatedClientScopeAccess> list = new ArrayList<DelegatedClientScopeAccess>();
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        
        EasyMock.expect(scopeAccessDao.getDelegatedClientScopeAccessByUsername(user.getUsername())).andReturn(null);
        EasyMock.replay(scopeAccessDao);
        
        List<DelegatedClientScopeAccess> retVal = scopeAccessService.getDelegatedUserScopeAccessForUsername(user.getUsername());
    }   
    
    @Test
    public void shouldGetDelegatedScopeAccessByAccessToken() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        delegatedClientScopeAccess.setUsername(user.getUsername());
        EasyMock.expect(scopeAccessDao.getScopeAccessByRefreshToken(accessToken)).andReturn(delegatedClientScopeAccess);
        EasyMock.replay(scopeAccessDao);
        
        DelegatedClientScopeAccess retVal = scopeAccessService.getDelegatedScopeAccessByRefreshToken(user, accessToken);
        Assert.assertNotNull(retVal);
        Assert.assertEquals(delegatedClientScopeAccess.getAccessTokenString(), retVal.getAccessTokenString());
        
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test
    public void shouldReturnNullForNonExistentTokenGetDelegatedScopeAccessByAccessToken() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        delegatedClientScopeAccess.setUsername(user.getUsername());
        EasyMock.expect(scopeAccessDao.getScopeAccessByRefreshToken(accessToken)).andReturn(null);
        EasyMock.replay(scopeAccessDao);
        
        DelegatedClientScopeAccess retVal = scopeAccessService.getDelegatedScopeAccessByRefreshToken(user, accessToken);
        Assert.assertNull(retVal);
        
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test
    public void shouldReturnNullForNonDelegateTokenGetDelegatedScopeAccessByAccessToken() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString(accessToken);
        userScopeAccess.setUsername(user.getUsername());
        EasyMock.expect(scopeAccessDao.getScopeAccessByRefreshToken(accessToken)).andReturn(userScopeAccess);
        EasyMock.replay(scopeAccessDao);
        
        DelegatedClientScopeAccess retVal = scopeAccessService.getDelegatedScopeAccessByRefreshToken(user, accessToken);
        Assert.assertNull(retVal);
        
        EasyMock.verify(scopeAccessDao);
    }
}
