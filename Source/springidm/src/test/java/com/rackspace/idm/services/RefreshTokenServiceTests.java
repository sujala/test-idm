package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.RefreshToken;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.RefreshTokenService;
import com.rackspace.idm.domain.service.impl.DefaultRefreshTokenService;

public class RefreshTokenServiceTests {

    RefreshTokenService refreshTokenService;

    ScopeAccessDao mockScopeAccessDao;
    UserDao mockUserDao;

    String refreshTokenString = "somerefreshtokenstring";
    String username = "someuser";
    String clientId = "someClientId";
    int defaultTokenExpirationSeconds = 86400;
    String dataCenterPrefix = "DFW";
    
    String accessToken = "XXXX";
    DateTime accessTokenExpiration = new DateTime();
    String clientRCN = "ClientRCN";
    String refreshToken = "YYYY";
    DateTime refreshTokenExpiration = new DateTime();
    String userRCN = "UserRCN";
    
    String uniqueId = "uniqueId";

    @Before
    public void setUp() {
        
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        mockUserDao = EasyMock.createMock(UserDao.class);
        
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();

        refreshTokenService = new DefaultRefreshTokenService(mockScopeAccessDao,
            mockUserDao,appConfig);
    }

    @Test
    public void shouldUpdateRefreshToken() {
        ScopeAccess scopeAccess = getFakeScopeAccess();
        EasyMock.expect(mockScopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId)).andReturn(scopeAccess);
        mockScopeAccessDao.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));
        EasyMock.replay(mockScopeAccessDao);
             
        RefreshToken returnToken = refreshTokenService.createRefreshTokenForUser(
            getFakeUser(), getFakeClient());
        
        Assert.assertNotNull(returnToken);
        EasyMock.verify(mockScopeAccessDao);
    }
    
    @Test(expected=IllegalStateException.class)
    public void shouldThrowErrorForScopeAccessNotPresent() {
        EasyMock.expect(mockScopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId)).andReturn(null);
        EasyMock.replay(mockScopeAccessDao);
        RefreshToken returnToken = refreshTokenService.createRefreshTokenForUser(
            getFakeUser(), getFakeClient());
    }
    
    @Test
    public void shouldGetRefreshTokenByTokenId() {

        ScopeAccess scopeAccess = getFakeScopeAccess();

        EasyMock.expect(mockScopeAccessDao.getScopeAccessByRefreshToken(refreshToken))
            .andReturn(scopeAccess);
        EasyMock.replay(mockScopeAccessDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByTokenString(refreshToken);

        Assert.assertNotNull(token);
        EasyMock.verify(mockScopeAccessDao);
    }

    @Test
    public void shouldReturnNullForInvalidRefreshToken() {

        EasyMock.expect(mockScopeAccessDao.getScopeAccessByRefreshToken(refreshToken))
        .andReturn(null);
    EasyMock.replay(mockScopeAccessDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByTokenString(refreshToken);

        Assert.assertNull(token);
        EasyMock.verify(mockScopeAccessDao);
    }
    
    @Test
    public void shouldGetRefreshTokenByUserAndClient() {
        DateTime currentTime = new DateTime().minusSeconds(100);
        ScopeAccess scopeAccess = getFakeScopeAccess();
        EasyMock.expect(mockScopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId))
            .andReturn(scopeAccess);
        EasyMock.replay(mockScopeAccessDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByUserAndClient(username, clientId, currentTime);

        Assert.assertNotNull(token);
        EasyMock.verify(mockScopeAccessDao);
    }
    
    @Test
    public void shouldReturnNullGetRefreshTokenByUserAndClient() {
        DateTime currentTime = new DateTime().minusSeconds(100);
        EasyMock.expect(mockScopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId))
            .andReturn(null);
        EasyMock.replay(mockScopeAccessDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByUserAndClient(username, clientId, currentTime);

        Assert.assertNull(token);
        EasyMock.verify(mockScopeAccessDao);
    }
    
    @Test
    public void shouldReturnNullForExpiredTokenGetRefreshTokenByUserAndClient() {
        DateTime currentTime = new DateTime().plusSeconds(100);
        ScopeAccess scopeAccess = getFakeScopeAccess();
        EasyMock.expect(mockScopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId))
            .andReturn(scopeAccess);
        EasyMock.replay(mockScopeAccessDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByUserAndClient(username, clientId, currentTime);

        Assert.assertNull(token);
        EasyMock.verify(mockScopeAccessDao);
    }
    
    @Test
    public void shouldResetRefreshTokenExpiration() {
        
        ScopeAccess scopeAccess = getFakeScopeAccess();
        EasyMock.expect(mockScopeAccessDao.getScopeAccessByRefreshToken(refreshToken)).andReturn(scopeAccess);
        mockScopeAccessDao.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));
        EasyMock.replay(mockScopeAccessDao);
        
        RefreshToken token = getFakeRefreshToken();
        refreshTokenService.resetTokenExpiration(token);
        EasyMock.verify(mockScopeAccessDao);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResetRefreshTokenIfNull() {
        
        RefreshToken refreshToken = null;
        refreshTokenService.resetTokenExpiration(refreshToken);
    }
    
    @Test
    public void shouldDeleteAllTokensForUser() {
        List<ScopeAccess> scopes = new ArrayList<ScopeAccess>();
        scopes.add(getFakeScopeAccess());
        
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(getFakeUser());
        EasyMock.replay(mockUserDao);
        
        EasyMock.expect(mockScopeAccessDao.getScopeAccessesByParent(getFakeUser().getUniqueId())).andReturn(scopes);
        mockScopeAccessDao.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));
        EasyMock.replay(mockScopeAccessDao);
        
        refreshTokenService.deleteAllTokensForUser(username);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockScopeAccessDao);
    }
    
    @Test
    public void shouldDeleteTokenForUserByClientId() {
        ScopeAccess scopeAccess = getFakeScopeAccess();
        EasyMock.expect(mockScopeAccessDao.getScopeAccessByUsernameAndClientId(username, clientId)).andReturn(scopeAccess);
        mockScopeAccessDao.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));
        EasyMock.replay(mockScopeAccessDao);
        
        refreshTokenService.deleteTokenForUserByClientId(username, clientId);
        EasyMock.verify(mockScopeAccessDao);
    }

    // helper functions
    private RefreshToken getFakeRefreshToken() {
        RefreshToken token = new RefreshToken(refreshToken,
            new DateTime().plusSeconds(defaultTokenExpirationSeconds),
            username, clientId);
        return token;
    }
    
    private ScopeAccess getFakeScopeAccess() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(clientId);
        sa.setAccessToken(accessToken);
        sa.setAccessTokenExpiration(accessTokenExpiration);
        sa.setClientRCN(clientRCN);
        sa.setRefreshToken(refreshToken);
        sa.setRefreshTokenExpiration(refreshTokenExpiration);
        sa.setUsername(username);
        sa.setUserRCN(userRCN);
        return sa;
    }
    
    private User getFakeUser() {
        User user = new User();
        user.setUniqueId(uniqueId);
        user.setUsername(username);
        user.setCustomerId(userRCN);
        return user;
    }
    
    private BaseClient getFakeClient() {
        return new BaseClient(clientId, clientRCN);
    }
}
