package com.rackspace.idm.domain.service.impl;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Test;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public class ServiceTestsBase {

    String clientId = "ClientId";
    ClientSecret clientSecret = ClientSecret.newInstance("Secret");
    String name = "Name";
    String customerId = "CustomerId";

    String customerInum = "Inum";

    String userDN = "userDN";
    String username = "username";

    String uniqueId = "uniqueId";
    
    String userRsId = "userRsId";
    
    @Test
    public void BlankTest() {}

    protected Application getFakeClient() {
        Application client = new Application(clientId, clientSecret, name, 
            customerId);

        client.setUniqueId(uniqueId);
        return client;
    }

    protected Customer getFakeCustomer() {
        Customer customer = new Customer();
        customer.setRCN(customerId);
        return customer;
    }

    protected User getFakeUser() {
        User user = new User();
        user.setUsername(username);
        user.setUniqueId(userDN);
        return user;
    }

    protected Permission getFakePermission(String permissionId) {
        Permission res = EasyMock.createNiceMock(Permission.class);

        EasyMock.expect(res.getUniqueId()).andReturn(permissionId).anyTimes();
        EasyMock.expect(res.getClientId()).andReturn(clientId).anyTimes();
        EasyMock.expect(res.getCustomerId()).andReturn(customerId).anyTimes();
        EasyMock.expect(res.getPermissionId()).andReturn(permissionId)
            .anyTimes();

        EasyMock.replay(res);
        return res;
    }

    protected ScopeAccess getFakeScopeAccess() {
        ScopeAccess so = EasyMock.createNiceMock(ScopeAccess.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }

    protected UserScopeAccess getFakeUserScopeAccess() {
        UserScopeAccess so = EasyMock
            .createNiceMock(UserScopeAccess.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);
        so.setUserRsId(userRsId);
        EasyMock.expect(
            so.isAccessTokenExpired(EasyMock.anyObject(DateTime.class)))
            .andReturn(true);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }
    
    protected ClientScopeAccess getFakeClientScopeAccess() {
        ClientScopeAccess so = EasyMock
            .createNiceMock(ClientScopeAccess.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);
        EasyMock.expect(
            so.isAccessTokenExpired(EasyMock.anyObject(DateTime.class)))
            .andReturn(true);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }
   

    protected RackerScopeAccess getFakeRackerScopeAccess() {
        RackerScopeAccess so = EasyMock
            .createNiceMock(RackerScopeAccess.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);
        EasyMock.expect(
            so.isAccessTokenExpired(EasyMock.anyObject(DateTime.class)))
            .andReturn(true);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }

    protected PasswordResetScopeAccess getFakePasswordResetScopeAccessObject() {
        PasswordResetScopeAccess prsa = new PasswordResetScopeAccess();
        prsa.setAccessTokenExp(new DateTime().toDate());
        prsa.setAccessTokenString("passwordResetToken");
        prsa.setClientId("PASSWORDRESET");
        return prsa;
    }
}
