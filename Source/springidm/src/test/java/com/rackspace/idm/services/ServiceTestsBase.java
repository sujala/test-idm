package com.rackspace.idm.services;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Test;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.PermissionEntity;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public class ServiceTestsBase {

    String clientId = "ClientId";
    ClientSecret clientSecret = ClientSecret.newInstance("Secret");
    String name = "Name";
    String inum = "Inum";
    String iname = "Iname";
    String customerId = "CustomerId";
    ClientStatus status = ClientStatus.ACTIVE;
    String seeAlso = "SeeAlso";
    String owner = "Owner";

    String customerName = "Name";
    String customerInum = "Inum";
    String customerIname = "Iname";
    CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    String customerSeeAlso = "SeeAlso";
    String customerOwner = "Owner";
    String customerCountry = "USA";

    String resourceId = "resource";
    String resourceValue = "resourceValue";

    String groupName = "groupName";
    String groupType = "groupType";

    String userDN = "userDN";
    String groupDN = "groupDN";
    String username = "username";

    String uniqueId = "uniqueId";
    
    @Test
    public void BlankTest() {}

    protected Client getFakeClient() {
        Client client = new Client(clientId, clientSecret, name, inum, iname,
            customerId, status);

        client.setUniqueId(uniqueId);
        return client;
    }

    protected Customer getFakeCustomer() {
        return new Customer(customerId, customerInum, customerIname,
            customerStatus, customerSeeAlso, owner);
    }

    protected User getFakeUser() {
        User user = new User();
        user.setUsername(username);
        user.setUniqueId(userDN);
        return user;
    }

    protected PermissionEntity getFakePermission(String permissionId) {
        PermissionEntity res = EasyMock.createNiceMock(PermissionEntity.class);

        res.setDescription("description");
        res.setEnabled(true);
        res.setGrantedByDefault(false);
        res.setValue(resourceValue);
        res.setPermissionType("type");
        res.setResourceGroup("resourceGroup");
        res.setTitle("title");

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
