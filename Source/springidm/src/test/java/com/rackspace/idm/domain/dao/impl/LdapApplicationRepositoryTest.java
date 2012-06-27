package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.unboundid.ldap.sdk.Attribute;
import org.apache.commons.configuration.Configuration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/27/12
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapApplicationRepositoryTest {
    private LdapApplicationRepository ldapApplicationRepository;
    private LdapApplicationRepository spy;

    @Before
    public void setUp() throws Exception {
        ldapApplicationRepository = new LdapApplicationRepository(mock(LdapConnectionPools.class), mock(Configuration.class));
        spy = spy(ldapApplicationRepository);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addClient_clientIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addClient(null);
    }

    @Test (expected = IllegalStateException.class)
    public void addClient_getAddAttributesForClient_throwsGeneralSecurityException() throws Exception {
        Application client = new Application();
        doThrow(new GeneralSecurityException()).when(spy).getAddAttributesForClient(client);
        spy.addClient(client);
    }

    @Test (expected = IllegalStateException.class)
    public void addClient_getAddAttributesForClient_throwsInvalidCipherTextException() throws Exception {
        Application client = new Application();
        doThrow(new InvalidCipherTextException()).when(spy).getAddAttributesForClient(client);
        spy.addClient(client);
    }

    @Test
    public void addClient_callsAddEntry() throws Exception {
        Application client = new Application();
        client.setClientId("id");
        client.setClientSecretObj(ClientSecret.newInstance("secret"));
        doReturn(new Attribute[0]).when(spy).getAddAttributesForClient(client);
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addClient(client);
        verify(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addClientGroup_clientGroupIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addClientGroup(null, null);
    }

    @Test (expected = DuplicateClientGroupException.class)
    public void addClientGroup_groupExists_throwsDuplicateClientGroupException() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setCustomerId("customerId");
        clientGroup.setClientId("clientId");
        clientGroup.setName("name");
        doReturn(clientGroup).when(spy).getClientGroup("customerId", "clientId", "name");
        spy.addClientGroup(clientGroup, "uniqueId");
    }

    @Test
    public void addClientGroup_callsAddEntry() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setCustomerId("customerId");
        clientGroup.setClientId("clientId");
        clientGroup.setName("name");
        doReturn(null).when(spy).getClientGroup("customerId", "clientId", "name");
        doReturn(new Attribute[0]).when(spy).getAddAttributesForClientGroup(clientGroup);
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
        spy.addClientGroup(clientGroup, "uniqueId");
        verify(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addUserToclientGroup_uniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addUserToClientGroup("", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addUserToclientGroup_clientGropuIsNUll_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addUserToClientGroup("uniqueId", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addUserToclientGroup_clientGropuUniqueIdIsNUll_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addUserToClientGroup("uniqueId", new ClientGroup());
    }

    @Test
    public void authenticate_clientIsNull_returnsNewClientAuthenticationResultWithNullClient() throws Exception {
        doReturn(null).when(spy).getClientByClientId("clientId");
        ClientAuthenticationResult result = spy.authenticate("clientId", "secret");
        assertThat("client authenticate result", result.getClient(), equalTo(null));
    }

    @Test
    public void deleteClient_clientNotNull_callsDeleteEntryAndSubtree() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setUniqueId("uniqueId");
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
        spy.deleteClient(client);
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
    }

    @Test
    public void deleteClient_clientIsNull_doesNothing() throws Exception {
        ldapApplicationRepository.deleteClient(null);
    }

    @Test
    public void deleteClientGroup_callsDeleteEntryAndSubtree() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setName("name");
        clientGroup.setUniqueId("uniqueId");
        doNothing().when(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
        spy.deleteClientGroup(clientGroup);
        verify(spy).deleteEntryAndSubtree(eq("uniqueId"), any(Audit.class));
    }
}
