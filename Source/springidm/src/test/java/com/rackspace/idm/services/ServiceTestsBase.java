package com.rackspace.idm.services;

import org.easymock.EasyMock;
import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;

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

    protected PermissionObject getFakePermission(String permissionId) {
        PermissionObject res = EasyMock.createNiceMock(PermissionObject.class);

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

    protected ScopeAccessObject getFakeScopeAccess() {
        ScopeAccessObject so = EasyMock.createNiceMock(ScopeAccessObject.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }

    protected UserScopeAccessObject getFakeUserScopeAccess() {
        UserScopeAccessObject so = EasyMock
            .createNiceMock(UserScopeAccessObject.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);
        EasyMock.expect(
            so.isAccessTokenExpired(EasyMock.anyObject(DateTime.class)))
            .andReturn(true);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }
    
    protected ClientScopeAccessObject getFakeClientScopeAccess() {
        ClientScopeAccessObject so = EasyMock
            .createNiceMock(ClientScopeAccessObject.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);
        EasyMock.expect(
            so.isAccessTokenExpired(EasyMock.anyObject(DateTime.class)))
            .andReturn(true);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }
   

    protected RackerScopeAccessObject getFakeRackerScopeAccess() {
        RackerScopeAccessObject so = EasyMock
            .createNiceMock(RackerScopeAccessObject.class);
        so.setClientId(clientId);
        so.setClientRCN(customerInum);
        EasyMock.expect(
            so.isAccessTokenExpired(EasyMock.anyObject(DateTime.class)))
            .andReturn(true);

        EasyMock.expect(so.getUniqueId()).andReturn("soUniqueId").anyTimes();
        EasyMock.replay(so);
        return so;
    }

    protected PasswordResetScopeAccessObject getFakePasswordResetScopeAccessObject() {
        PasswordResetScopeAccessObject prsa = new PasswordResetScopeAccessObject();
        prsa.setAccessTokenExp(new DateTime().toDate());
        prsa.setAccessTokenString("passwordResetToken");
        prsa.setClientId("PASSWORDRESET");
        return prsa;
    }
}
