package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.any;
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

    @Test
    public void getAllClients_callsGetMultipleEntries() throws Exception {
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        spy.getAllClients();
        verify(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
    }

    @Test
    public void getAllClients_addClientsToList_returnsClients() throws Exception {
        Application client = new Application();
        ArrayList<SearchResultEntry> resultEntries = new ArrayList<SearchResultEntry>();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0], new Control[0]);
        resultEntries.add(searchResultEntry);
        doReturn(resultEntries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        doReturn(client).when(spy).getClient(searchResultEntry);
        List<Application> result = spy.getAllClients();
        assertThat("client", result.get(0), equalTo(client));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientByClientId_clientIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientByClientId("");
    }

    @Test
    public void getClientByClientId_foundClient_returnsClient() throws Exception {
        Application client = new Application();
        doReturn(client).when(spy).getSingleClient(any(Filter.class));
        Application result = spy.getClientByClientId("clientId");
        assertThat("client", result, equalTo(client));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientByClientName_clientNameIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientByClientname("   ");
    }

    @Test
    public void getClientByClientName_foundClient_returnsClient() throws Exception {
        Application client = new Application();
        doReturn(client).when(spy).getSingleClient(any(Filter.class));
        Application result = spy.getClientByClientname("clientName");
        assertThat("client", result, equalTo(client));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientByCustomerIdAndClientId_clientIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientByCustomerIdAndClientId("customerId", "   ");
    }

    @Test
    public void getClientByCustomerIdAndClientId_foundClient_returnsClient() throws Exception {
        Application client = new Application();
        doReturn(client).when(spy).getSingleClient(any(Filter.class));
        Application result = spy.getClientByCustomerIdAndClientId("customerId", "clientId");
        assertThat("client", result, equalTo(client));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientById("");
    }

    @Test
    public void getClientById_foundClient_returnsClient() throws Exception {
        Application client = new Application();
        doReturn(client).when(spy).getSingleClient(any(Filter.class));
        Application result = spy.getClientById("clientId");
        assertThat("client", result, equalTo(client));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientByScope_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientByScope("");
    }

    @Test
    public void getClientByScope_foundClient_returnsClient() throws Exception {
        Application client = new Application();
        doReturn(client).when(spy).getSingleClient(any(Filter.class));
        Application result = spy.getClientByScope("scope");
        assertThat("client", result, equalTo(client));
    }

    @Test
    public void getClientGroup_didNotFindGroup_returnsNull() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_NAME, "groupName")
                .addEqualAttribute(LdapRepository.ATTR_CLIENT_ID, "clientId")
                .addEqualAttribute(LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, "customerId")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_CLIENTGROUP)
                .build();
        doReturn(null).when(spy).getSingleEntry(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter, LdapRepository.ATTR_GROUP_SEARCH_ATTRIBUTES);
        ClientGroup result = spy.getClientGroup("customerId", "clientId", "groupName");
        assertThat("client group", result, equalTo(null));
    }

    @Test
    public void getClientGroup_didFindGroup_returnsGroup() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_NAME, "groupName")
                .addEqualAttribute(LdapRepository.ATTR_CLIENT_ID, "clientId")
                .addEqualAttribute(LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER, "customerId")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_CLIENTGROUP)
                .build();
        ClientGroup group = new ClientGroup();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, searchFilter, LdapRepository.ATTR_GROUP_SEARCH_ATTRIBUTES);
        doReturn(group).when(spy).getClientGroup(searchResultEntry);
        ClientGroup result = spy.getClientGroup("customerId", "clientId", "groupName");
        assertThat("client group", result, equalTo(group));
    }

    @Test
    public void getClientGroupByUniqueId_didNotFindGroup_returnsNull() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder().addEqualAttribute(
                LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_CLIENTGROUP).build();
        doReturn(null).when(spy).getSingleEntry("uniqueId", SearchScope.BASE, searchFilter, LdapRepository.ATTR_GROUP_SEARCH_ATTRIBUTES);
        ClientGroup result = spy.getClientGroupByUniqueId("uniqueId");
        assertThat("client group", result, equalTo(null));
    }

    @Test
    public void getClientGroupByUniqueId_didFindGroup_returnsGroup() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder().addEqualAttribute(
                LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_CLIENTGROUP).build();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ClientGroup group = new ClientGroup();
        doReturn(searchResultEntry).when(spy).getSingleEntry("uniqueId", SearchScope.BASE, searchFilter, LdapRepository.ATTR_GROUP_SEARCH_ATTRIBUTES);
        doReturn(group).when(spy).getClientGroup(searchResultEntry);
        ClientGroup result = spy.getClientGroupByUniqueId("uniqueId");
        assertThat("client group", result, equalTo(group));
    }

    @Test (expected = NotFoundException.class)
    public void getClientGroupsByClientId_clientIsNull_throwsNotFound() throws Exception {
        doReturn(null).when(spy).getClientByClientId("clientId");
        spy.getClientGroupsByClientId("clientId");
    }

    @Test
    public void getClientGroupsByClientId_doesNotFindGroups_returnsEmptyList() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder().addEqualAttribute(
                LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_CLIENTGROUP).build();
        Application client = new Application();
        client.setUniqueId("uniqueId");
        doReturn(client).when(spy).getClientByClientId("clientId");
        doReturn(new ArrayList<SearchResultEntry>()).when(spy).getMultipleEntries("uniqueId", SearchScope.ONE, searchFilter, LdapRepository.ATTR_NAME, LdapRepository.ATTR_GROUP_SEARCH_ATTRIBUTES);
        List<ClientGroup> result = spy.getClientGroupsByClientId("clientId");
        assertThat("group list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getClientGroupsByClientId_doesFindGroups_returnsGroupList() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder().addEqualAttribute(
                LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_CLIENTGROUP).build();
        ClientGroup group = new ClientGroup();
        Application client = new Application();
        client.setUniqueId("uniqueId");
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        ArrayList<SearchResultEntry> resultList = new ArrayList<SearchResultEntry>();
        resultList.add(searchResultEntry);
        doReturn(client).when(spy).getClientByClientId("clientId");
        doReturn(resultList).when(spy).getMultipleEntries("uniqueId", SearchScope.ONE, searchFilter, LdapRepository.ATTR_NAME, LdapRepository.ATTR_GROUP_SEARCH_ATTRIBUTES);
        doReturn(group).when(spy).getClientGroup(searchResultEntry);
        List<ClientGroup> result = spy.getClientGroupsByClientId("clientId");
        assertThat("group list", result.get(0), equalTo(group));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientsByCustomerId_customerIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientsByCustomerId("  ", 1, 1);
    }

    @Test
    public void getClientsByCustomerId_foundClients_returnsClients() throws Exception {
        Applications clients = new Applications();
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1));
        Applications result = spy.getClientsByCustomerId("customerId", 1, 1);
        assertThat("clients", result, equalTo(clients));
    }

    @Test
    public void getAllClients_filtersNotNull_returnsApplications() throws Exception {
        FilterParam filterParam = new FilterParam(FilterParam.FilterParamName.APPLICATION_NAME, "1");
        List<FilterParam> filters = new ArrayList<FilterParam>();
        filters.add(filterParam);
        Applications clients = new Applications();
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1));
        Applications result = spy.getAllClients(filters, 1, 1);
        assertThat("clients", result, equalTo(clients));
    }

    @Test
    public void getAllClients_filterIsNull_returnsApplications() throws Exception {
        Applications clients = new Applications();
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1));
        Applications result = spy.getAllClients(null, 1, 1);
        assertThat("clients", result, equalTo(clients));
    }

    @Test
    public void isUserInClientGroup_foundEntry_returnsTrue() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .addEqualAttribute(LdapRepository.ATTR_MEMBER_OF, "groupDN").build();
        doReturn(new SearchResultEntry("", new Attribute[0])).when(spy).getSingleEntry(LdapRepository.USERS_BASE_DN, SearchScope.ONE, searchFilter, LdapRepository.ATTR_NO_ATTRIBUTES);
        boolean result = spy.isUserInClientGroup("username", "groupDN");
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void isUserInClientGroup_notFoundEntry_returnsFalse() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .addEqualAttribute(LdapRepository.ATTR_MEMBER_OF, "groupDN").build();
        doReturn(null).when(spy).getSingleEntry(LdapRepository.USERS_BASE_DN, SearchScope.ONE, searchFilter, LdapRepository.ATTR_NO_ATTRIBUTES);
        boolean result = spy.isUserInClientGroup("username", "groupDN");
        assertThat("boolean", result, equalTo(false));
    }

    @Test (expected = IllegalArgumentException.class)
    public void removeUserFromGroup_userUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.removeUserFromGroup("  ", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void removeUserFromGroup_groupIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.removeUserFromGroup("uniqueId", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void removeUserFromGroup_groupUniqueIdIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.removeUserFromGroup("uniqueId", new ClientGroup());
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClient_clientIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateClient(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClient_clientIdIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateClient(new Application());
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClient_oldClientIsNull_throwsIllegalArgument() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        doReturn(null).when(spy).getClientByClientId("clientId");
        spy.updateClient(client);
    }

    @Test
    public void updateClient_callsGetModification() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        Application oldClient = client;
        doReturn(new ArrayList<Modification>()).when(spy).getModifications(oldClient, client);
        doReturn(oldClient).when(spy).getClientByClientId("clientId");
        spy.updateClient(client);
        verify(spy).getModifications(oldClient, client);
    }

    @Test
    public void updateClient_callsUpdateEntry() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setUniqueId("uniqueId");
        Application oldClient = client;
        ArrayList<Modification> modificationList = new ArrayList<Modification>();
        Modification modification = new Modification(ModificationType.ADD, "ADD");
        modificationList.add(modification);
        doReturn(modificationList).when(spy).getModifications(oldClient, client);
        doReturn(oldClient).when(spy).getClientByClientId("clientId");
        doNothing().when(spy).updateEntry(eq("uniqueId"), eq(modificationList), any(Audit.class));
        spy.updateClient(client);
        verify(spy).updateEntry(eq("uniqueId"), eq(modificationList), any(Audit.class));
    }

    @Test (expected = IllegalStateException.class)
    public void updateClient_updateEntry_throwsGeneralSecurityException() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setUniqueId("uniqueId");
        Application oldClient = client;
        ArrayList<Modification> modificationList = new ArrayList<Modification>();
        Modification modification = new Modification(ModificationType.ADD, "ADD");
        modificationList.add(modification);
        doReturn(oldClient).when(spy).getClientByClientId("clientId");
        doThrow(new GeneralSecurityException()).when(spy).getModifications(oldClient, client);
        spy.updateClient(client);
    }

    @Test (expected = IllegalStateException.class)
    public void updateClient_updateEntry_throwsInvalidCipherTextException() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setUniqueId("uniqueId");
        Application oldClient = client;
        ArrayList<Modification> modificationList = new ArrayList<Modification>();
        Modification modification = new Modification(ModificationType.ADD, "ADD");
        modificationList.add(modification);
        doReturn(oldClient).when(spy).getClientByClientId("clientId");
        doThrow(new InvalidCipherTextException()).when(spy).getModifications(oldClient, client);
        spy.updateClient(client);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClientGroup_groupIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateClientGroup(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClientGroup_groupUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateClientGroup(new ClientGroup());
    }

    @Test
    public void updateClientGroup_callsGetClientGroupByUniqueId() throws Exception {
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        clientGroup.setType("same");
        doReturn(clientGroup).when(spy).getClientGroupByUniqueId("uniqueId");
        spy.updateClientGroup(clientGroup);
        verify(spy).getClientGroupByUniqueId("uniqueId");
    }

    @Test
    public void updateClientGroup_getTypeNotNullAndBlank_addsDeleteMod() throws Exception {
        ArgumentCaptor<ArrayList> argumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        clientGroup.setType("");
        ClientGroup oldClientGroup = new ClientGroup();
        oldClientGroup.setUniqueId("notUniqueAtAll");
        oldClientGroup.setType("oldType");
        doReturn(oldClientGroup).when(spy).getClientGroupByUniqueId("uniqueId");
        doNothing().when(spy).updateEntry(eq("notUniqueAtAll"), any(ArrayList.class), any(Audit.class));
        spy.updateClientGroup(clientGroup);
        verify(spy).updateEntry(eq("notUniqueAtAll"), argumentCaptor.capture(), any(Audit.class));
        ArrayList<Modification> value = argumentCaptor.getValue();
        assertThat("modification type", value.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void updateClientGroup_getTypeNotNullAndNotBlank_addsReplaceMod() throws Exception {
        ArgumentCaptor<ArrayList> argumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
        ClientGroup clientGroup = new ClientGroup();
        clientGroup.setUniqueId("uniqueId");
        clientGroup.setType("newType");
        ClientGroup oldClientGroup = new ClientGroup();
        oldClientGroup.setUniqueId("notUniqueAtAll");
        oldClientGroup.setType("oldType");
        doReturn(oldClientGroup).when(spy).getClientGroupByUniqueId("uniqueId");
        doNothing().when(spy).updateEntry(eq("notUniqueAtAll"), any(ArrayList.class), any(Audit.class));
        spy.updateClientGroup(clientGroup);
        verify(spy).updateEntry(eq("notUniqueAtAll"), argumentCaptor.capture(), any(Audit.class));
        ArrayList<Modification> value = argumentCaptor.getValue();
        assertThat("modification type", value.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }
}
