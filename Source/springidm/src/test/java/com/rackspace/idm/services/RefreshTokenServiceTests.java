package com.rackspace.idm.services;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.entities.RefreshTokenDefaultAttributes;
import com.rackspace.idm.test.stub.StubLogger;

public class RefreshTokenServiceTests {

    RefreshTokenDao mockRefreshTokenDao;
    RefreshTokenService refreshTokenService;
    RefreshTokenDefaultAttributes defaultAttributes;

    String refreshTokenString = "somerefreshtokenstring";
    String username = "someuser";
    String clientId = "someClientId";
    int defaultTokenExpirationSeconds = 86400;
    String dataCenterPrefix = "DFW";

    @Before
    public void setUp() {
        
        mockRefreshTokenDao = EasyMock.createMock(RefreshTokenDao.class);

        defaultAttributes = new RefreshTokenDefaultAttributes(
            defaultTokenExpirationSeconds, dataCenterPrefix);

        refreshTokenService = new DefaultRefreshTokenService(defaultAttributes,
            mockRefreshTokenDao);
    }

    @Test
    public void shouldCreateRefreshToken() {
        
        RefreshToken refreshToken = getFakeRefreshToken();
        
        mockRefreshTokenDao.save(refreshToken);        
        RefreshToken returnToken = refreshTokenService.createRefreshTokenForUser(
            refreshToken.getOwner(), refreshToken.getRequestor());
        
        Assert.assertNotNull(returnToken);
    }
    
    @Test
    public void shouldGetRefreshTokenByTokenId() {

        RefreshToken refreshToken = getFakeRefreshToken();

        EasyMock.expect(
            mockRefreshTokenDao.findByTokenString(refreshTokenString))
            .andReturn(refreshToken);
        EasyMock.replay(mockRefreshTokenDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByTokenString(refreshTokenString);

        Assert.assertNotNull(token);
        EasyMock.verify(mockRefreshTokenDao);
    }

    @Test
    public void shouldReturnNullForInvalidRefreshToken() {

        EasyMock.expect(
            mockRefreshTokenDao.findByTokenString(refreshTokenString))
            .andReturn(null);
        EasyMock.replay(mockRefreshTokenDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByTokenString(refreshTokenString);

        Assert.assertNull(token);
        EasyMock.verify(mockRefreshTokenDao);
    }
    
    @Test
    public void shouldGetRefreshTokenByUserAndClient() {

        RefreshToken refreshToken = getFakeRefreshToken();
        DateTime currentTime = new DateTime();
        
        EasyMock.expect(
            mockRefreshTokenDao.findTokenForOwner(username, clientId, currentTime))
            .andReturn(refreshToken);
        EasyMock.replay(mockRefreshTokenDao);

        RefreshToken token = refreshTokenService
            .getRefreshTokenByUserAndClient(username, clientId, currentTime);

        Assert.assertNotNull(token);
        EasyMock.verify(mockRefreshTokenDao);
    }
    
    @Test
    public void shouldResetRefreshTokenExpiration() {
        
        RefreshToken refreshToken = getFakeRefreshToken();
        
        mockRefreshTokenDao.updateToken(refreshToken);
        EasyMock.replay(mockRefreshTokenDao);
        
        refreshTokenService.resetTokenExpiration(refreshToken);
        EasyMock.verify(mockRefreshTokenDao);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResetRefreshTokenIfNull() {
        
        RefreshToken refreshToken = null;
        refreshTokenService.resetTokenExpiration(refreshToken);
    }

    // helper functions
    private RefreshToken getFakeRefreshToken() {
        RefreshToken token = new RefreshToken(refreshTokenString,
            new DateTime().plusSeconds(defaultTokenExpirationSeconds),
            username, clientId);
        return token;
    }
}
