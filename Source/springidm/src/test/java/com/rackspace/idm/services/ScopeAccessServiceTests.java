package com.rackspace.idm.services;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.util.AuthHeaderHelper;

public class ScopeAccessServiceTests extends ServiceTests {

    UserDao mockUserDao;
    ScopeAccessObjectDao scopeAccessDao;
    ClientDao mockClientDao;
    AuthHeaderHelper authHeaderHelper;
    ScopeAccessService scopeAccessService;
    
    @Before
    public void setUp() throws Exception {
        Configuration appConfig = new PropertyFileConfiguration()
        .getConfigFromClasspath();
        authHeaderHelper = new AuthHeaderHelper();
        
        mockUserDao = EasyMock.createMock(UserDao.class);
        mockClientDao = EasyMock.createMock(ClientDao.class);
        scopeAccessDao = EasyMock.createMock(ScopeAccessObjectDao.class);
        scopeAccessService = new DefaultScopeAccessService(mockUserDao, mockClientDao, 
            scopeAccessDao, authHeaderHelper, appConfig);
    }
    
    @Test
    public void shouldAddPermission() {
       Client client = getFakeClient();
       ScopeAccessObject sa = getFakeScopeAccess();
       PermissionObject perm = getFakePermission();
       
       EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId())).andReturn(client);
       
       EasyMock.expect(scopeAccessDao.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId())).andReturn(sa);
       EasyMock.expect(scopeAccessDao.getPermissionByParentAndPermissionId(sa.getUniqueId(), perm)).andReturn(perm);
       
       EasyMock.expect(scopeAccessDao.grantPermission(sa.getUniqueId(), perm)).andReturn(perm);
      
       EasyMock.replay(scopeAccessDao, mockClientDao);
       
       scopeAccessService.addPermissionToScopeAccess(sa.getUniqueId(), perm);
       
       EasyMock.verify(scopeAccessDao); 
    }
}
