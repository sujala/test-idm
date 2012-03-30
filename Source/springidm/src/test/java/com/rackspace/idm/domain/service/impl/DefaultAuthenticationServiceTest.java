package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.util.RSAClient;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 3/1/12
 * Time: 4:07 PM
 */
public class DefaultAuthenticationServiceTest {

    ApplicationDao applicationDao = mock(ApplicationDao.class);
    UserDao userDao = mock(UserDao.class);

    private Configuration config = mock(Configuration.class);
    DefaultAuthenticationService defaultAuthenticationService = new DefaultAuthenticationService(null,null,null,null,applicationDao,config,userDao,null,null, null);
    DefaultAuthenticationService spy;
    RSAClient rsaClient = mock(RSAClient.class);

    @Before
    public void setUp() throws Exception {
        ClientAuthenticationResult value = new ClientAuthenticationResult(new Application(), true);
        when(userDao.getUserByUsername(anyString())).thenReturn(new Racker());
        when(applicationDao.authenticate(anyString(), anyString())).thenReturn(value);
        when(rsaClient.authenticate(anyString(),anyString())).thenReturn(true);
        when(config.getBoolean(anyString(),anyBoolean())).thenReturn(true);
        defaultAuthenticationService.setRsaClient(rsaClient);
        spy = spy(defaultAuthenticationService);
    }

    @Test
    public void authenticate_withRSACredentials_callsAuthenticateRacker() throws Exception {
        RSACredentials rsaCredentials = new RSACredentials();
        rsaCredentials.setUsername("u");
        rsaCredentials.setPassword("p");
        rsaCredentials.setGrantType("password");
        rsaCredentials.setClientId("id");
        doNothing().when(spy).validateCredentials(rsaCredentials);
        doReturn(new RackerScopeAccess()).when(spy).getAndUpdateRackerScopeAccessForClientId(any(Racker.class), any(Application.class));
        spy.authenticate(rsaCredentials);
        verify(spy).authenticateRacker("u", "p", true);
    }

    @Test
    public void authenticateRacker_withFlagSetToTrue_callsClient() throws Exception {
        spy.authenticateRacker("foo", "bar", true);
        verify(rsaClient).authenticate("foo", "bar");
    }
}
