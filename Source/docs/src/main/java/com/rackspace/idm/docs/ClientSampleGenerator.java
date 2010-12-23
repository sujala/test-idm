package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Client;
import com.rackspace.idm.jaxb.ClientCredentials;
import com.rackspace.idm.jaxb.ClientStatus;
import com.rackspace.idm.jaxb.Clients;

public class ClientSampleGenerator extends SampleGenerator {
    private ClientSampleGenerator() {
        super();
    }
    
    public static void main(String[] args) throws JAXBException, IOException {
        ClientSampleGenerator sampleGen = new ClientSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getClient(), "client");
        sampleGen.marshalToFiles(sampleGen.getClients(), "clients");
        sampleGen.marshalToFiles(sampleGen.getClientCredentials(),
            "client_credentials");
    }
    
    private Client getClient() {
        Client client = of.createClient();
        
        client.setClientId("ab4820dhcb39347");
        client.setCustomerId("RCN-000-000-000");
        client.setName("Test Application2");
        client.setIname("@Rackspace*Rackspace*ControlPanel");
        client.setInum("@FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!0001");
        client.setLocked(Boolean.FALSE);
        client.setSoftDeleted(Boolean.FALSE);
        client.setStatus(ClientStatus.ACTIVE);
        
        ClientCredentials creds = of.createClientCredentials();
        creds.setClientSecret("3af738fbeiwu23");
        
        client.setCredentials(creds);
        
        com.rackspace.idm.jaxb.PermissionList permissions = of.createPermissionList();
        
        com.rackspace.idm.jaxb.Permission permission = of.createPermission();
        permission.setPermissionId("addUser");
        permission.setClientId("IDM");
        permissions.getPermissions().add(permission);
        
        client.setPermissions(permissions);
        
        return client;
    }
    
    private Client getClient2() {
        Client client = of.createClient();
        
        client.setClientId("632h389cv902bde");
        client.setCustomerId("RCN-000-000-000");
        client.setName("Test Application2");
        client.setIname("@Rackspace*Rackspace*CloudServers");
        client.setInum("@FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!0002");
        client.setLocked(Boolean.FALSE);
        client.setSoftDeleted(Boolean.FALSE);
        client.setStatus(ClientStatus.ACTIVE);
        
        ClientCredentials creds = of.createClientCredentials();
        creds.setClientSecret("78ndefuy823gvi");
        
        client.setCredentials(creds);
        
        com.rackspace.idm.jaxb.PermissionList permissions = of.createPermissionList();
        
        com.rackspace.idm.jaxb.Permission permission = of.createPermission();
        permission.setPermissionId("addUser");
        permission.setClientId("IDM");
        permissions.getPermissions().add(permission);
        
        client.setPermissions(permissions);
        
        return client;
    }
    
    private Clients getClients() {
        Clients clients = of.createClients();
        Client client = getClient();
        clients.getClients().add(client);
        Client client2 = getClient2();
        clients.getClients().add(client2);
        return clients;
    }
    
    private ClientCredentials getClientCredentials() {
        ClientCredentials creds = of.createClientCredentials();
        creds.setClientSecret("cncv9823823bfb");
        return creds;
    }
}
