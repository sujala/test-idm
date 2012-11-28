package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.commons.configuration.Configuration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

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
@RunWith(MockitoJUnitRunner.class)
public class LdapApplicationRepositoryTest{

    @InjectMocks 
    private LdapApplicationRepository ldapApplicationRepository = new LdapApplicationRepository();
    @Mock
    private LdapConnectionPools ldapConnectionPools;
    @Mock
    private Configuration configuration;

    private CryptHelper cryptHelper;
    private LdapApplicationRepository spy;
    private LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
        ldapInterface = mock(LDAPInterface.class);
        spy = spy(ldapApplicationRepository);

        cryptHelper = new CryptHelper();
        cryptHelper.setConfiguration(configuration);
        when(configuration.getString("crypto.password")).thenReturn("password");
        when(configuration.getString("crypto.salt")).thenReturn("a1 b1");

        doReturn(ldapInterface).when(spy).getAppInterface();
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

    @Test
    public void getClientByClientId_clientIdIsBlank_returnsNull() throws Exception {
        Application result = ldapApplicationRepository.getClientByClientId("");
        assertThat("client", result, equalTo(null));
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

    @Test
    public void getClientByCustomerIdAndClientId_clientIdIsBlank_returnsNull() throws Exception {
        Application result = ldapApplicationRepository.getClientByCustomerIdAndClientId("customerId", "   ");
        assertThat("client", result, equalTo(null));
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
    public void getAllClients_filtersNotNullAndParamNameIsApplicationName_returnsApplications() throws Exception {
        FilterParam filterParam = new FilterParam(FilterParam.FilterParamName.APPLICATION_NAME, "1");
        List<FilterParam> filters = new ArrayList<FilterParam>();
        filters.add(filterParam);
        Applications clients = new Applications();
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1));
        Applications result = spy.getAllClients(filters, 1, 1);
        assertThat("clients", result, equalTo(clients));
    }


    @Test
    public void getAllClients_filtersNotNullAndParamNameIsNotApplicationName_onlyAddsOneFilter() throws Exception {
        ArgumentCaptor<Filter> argumentCapture = ArgumentCaptor.forClass(Filter.class);
        FilterParam filterParam = new FilterParam(FilterParam.FilterParamName.ROLE_NAME, "1");
        List<FilterParam> filters = new ArrayList<FilterParam>();
        filters.add(filterParam);
        Applications clients = new Applications();
        doReturn(clients).when(spy).getMultipleClients(argumentCapture.capture(), eq(1), eq(1));
        spy.getAllClients(filters, 1, 1);
        Filter[] filtersCaptured = argumentCapture.getValue().getComponents();
        assertThat("only one filter was added, so no components",filtersCaptured.length,equalTo(0));
    }

    @Test
    public void getAllClients_filterIsNull_returnsApplications() throws Exception {
        Applications clients = new Applications();
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1));
        Applications result = spy.getAllClients(null, 1, 1);
        assertThat("clients", result, equalTo(clients));
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

    @Test (expected = IllegalStateException.class)
    public void getAvailableScopes_callsLDAPInterfaceSearch_throwsLDAPSearchException() throws Exception {
        doThrow(new LDAPSearchException(ResultCode.LOCAL_ERROR, "error")).when(ldapInterface).search(anyString(), any(SearchScope.class), any(Filter.class));
        spy.getAvailableScopes();
    }

    @Test
    public void getAvailableScopes_foundClient_returnsClientList() throws Exception {
        Application client = new Application();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        SearchResult searchResult = new SearchResult(1, ResultCode.SUCCESS, "diag", "matchDN", null, searchResultEntryList, null, 1, 1, null);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        doReturn(client).when(spy).getClient(searchResultEntry);
        List<Application> result = spy.getAvailableScopes();
        assertThat("client", result.get(0), equalTo(client));
    }

    @Test
    public void getAvailableScopes_ZeroEntries_returnsEmptyClientList() throws Exception {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        SearchResult searchResult = new SearchResult(1, ResultCode.SUCCESS, "diag", "matchDN", null, searchResultEntryList, null, 1, 1, null);
        when(ldapInterface.search(anyString(), any(SearchScope.class), any(Filter.class))).thenReturn(searchResult);
        List<Application> result = spy.getAvailableScopes();
        assertThat("client", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAddAttributesForClient_addsAllAttributes_returnsArray() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setOpenStackType("openStack");
        client.setName("name");
        client.setRCN("rcn");
        client.setClientSecretObj(ClientSecret.newInstance("secret"));
        client.setEnabled(true);
        client.setTitle("title");
        client.setDescription("description");
        client.setScope("scope");
        client.setCallBackUrl("url");
        client.setUseForDefaultRegion(true);
        Attribute[] result = ldapApplicationRepository.getAddAttributesForClient(client);
        assertThat("client id", result[1].getValue(), equalTo("clientId"));
        assertThat("open stack type", result[2].getValue(), equalTo("openStack"));
        assertThat("name", result[3].getValue(), equalTo("name"));
        assertThat("rcn", result[4].getValue(), equalTo("rcn"));
        assertThat("client secret", result[5].getValue(), equalTo("secret"));
        assertThat("client password", cryptHelper.decrypt(result[6].getValueByteArray()), equalTo("secret"));
        assertThat("enabled", result[7].getValue(), equalTo("TRUE"));
        assertThat("title", result[8].getValue(), equalTo("title"));
        assertThat("description", result[9].getValue(), equalTo("description"));
        assertThat("scope", result[10].getValue(), equalTo("scope"));
        assertThat("url", result[11].getValue(), equalTo("url"));
        assertThat("userForDefaultRegion", result[12].getValue(), equalTo("TRUE"));
    }

    @Test
    public void getAddAttributesForClient_addsNoAttributes_returnsArray() throws Exception {
        Application client = new Application();
        client.setClientSecretObj(ClientSecret.newInstance(null));
        Attribute[] result = ldapApplicationRepository.getAddAttributesForClient(client);
        assertThat("attribute", result.length, equalTo(1));
    }

    @Test
    public void getClient_setupClientAttributes_returnsClient() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setOpenStackType("openStack");
        client.setName("name");
        client.setRCN("rcn");
        client.setClientSecretObj(ClientSecret.newInstance("secret"));
        client.setEnabled(true);
        client.setTitle("title");
        client.setDescription("description");
        client.setScope("scope");
        client.setCallBackUrl("url");
        client.setUseForDefaultRegion(true);
        Attribute[] attributesForClient = ldapApplicationRepository.getAddAttributesForClient(client);
        SearchResultEntry searchResultEntry = new SearchResultEntry("uniqueId", attributesForClient);
        Application result = ldapApplicationRepository.getClient(searchResultEntry);
        assertThat("unique id", result.getUniqueId(), equalTo("uniqueId"));
        assertThat("client id", result.getClientId(), equalTo("clientId"));
        assertThat("open stack type", result.getOpenStackType(), equalTo("openStack"));
        assertThat("name", result.getName(), equalTo("name"));
        assertThat("rcn", result.getRCN(), equalTo("rcn"));
        assertThat("client secret", result.getClientSecret(), equalTo("secret"));
        assertThat("enabled", result.isEnabled(), equalTo(true));
        assertThat("title",result.getTitle(), equalTo("title"));
        assertThat("description", result.getDescription(), equalTo("description"));
        assertThat("scope",result.getScope(), equalTo("scope"));
        assertThat("url", result.getCallBackUrl(), equalTo("url"));
        assertThat("useForDefaultRegion", result.getUseForDefaultRegion(), equalTo(true));
    }

    @Test
    public void getMultipleClients_contentCountLessThanOffset_setClientsToEmptyList() throws Exception {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        doReturn(1).when(spy).getLdapPagingLimitMax();
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, null, null);
        Applications result = spy.getMultipleClients(null, 2, 2);
        assertThat("client", result.getClients().isEmpty(), equalTo(true));
    }

    @Test
    public void getMultipleClients_subListIsEmpty_setClientsToEmptyList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        doReturn(0).when(spy).getLdapPagingOffsetDefault();
        doReturn(0).when(spy).getLdapPagingLimitDefault();
        doReturn(3).when(spy).getLdapPagingLimitMax();
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, null, null);
        Applications result = spy.getMultipleClients(null, -1, 0);
        assertThat("client", result.getClients().isEmpty(), equalTo(true));
    }

    @Test
    public void getMultipleClients_callsGetClient_returnsClientsWithAddedClient() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        searchResultEntryList.add(searchResultEntry);
        doReturn(3).when(spy).getLdapPagingLimitMax();
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, null, null);
        Applications result = spy.getMultipleClients(null, 1, 2);
        assertThat("client", result.getClients().size(), equalTo(1));
    }

    @Test
    public void getSingleClient_notFoundClient_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Application result = spy.getSingleClient(null);
        assertThat("client", result, equalTo(null));
    }

    @Test
    public void getSingleClient_foundClient_returnsClient() throws Exception {
        Application client = new Application();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(client).when(spy).getClient(searchResultEntry);
        Application result = spy.getSingleClient(null);
        assertThat("client", result, equalTo(client));
    }

    @Test
    public void getSingleSoftDeletedClient_notFoundClient_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        Application result = spy.getSingleSoftDeletedClient(null);
        assertThat("client", result, equalTo(null));
    }

    @Test
    public void getSingleSoftDeletedClient_foundClient_returnsClient() throws Exception {
        Application client = new Application();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(client).when(spy).getClient(searchResultEntry);
        Application result = spy.getSingleSoftDeletedClient(null);
        assertThat("client", result, equalTo(client));
    }

    @Test
    public void getModifications_nothingToModify_returnsEmptyList() throws Exception {
        Application client = new Application();
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getModifications_callsCheckForUseForDefaultRegionModification() throws Exception {
        spy.getModifications(new Application(), new Application());
        verify(spy).checkForUseForDefaultRegionModification(any(Application.class), any(Application.class), any(List.class));
    }

    @Test
    public void checkForCallBackUrlModification_addsDeleteMod() throws Exception {
        Application client = new Application();
        client.setCallBackUrl("");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForCallBackUrlModification_addsReplaceMod() throws Exception {
        Application client = new Application();
        client.setCallBackUrl("url");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        Application oldClient = new Application();
        oldClient.setCallBackUrl("differentUrl");
        List<Modification> result = spy.getModifications(oldClient, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForCallBackUrlModification_addsNoMod() throws Exception {
        Application client = new Application();
        client.setCallBackUrl("url");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForScopeModification_addsDeleteMod() throws Exception {
        Application client = new Application();
        client.setScope("");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForScopeModification_addsReplaceMod() throws Exception {
        Application client = new Application();
        client.setScope("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        Application oldClient = new Application();
        oldClient.setScope("different");
        List<Modification> result = spy.getModifications(oldClient, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForScopeModification_addsNoMod() throws Exception {
        Application client = new Application();
        client.setScope("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForDescriptionModification_addsDeleteMod() throws Exception {
        Application client = new Application();
        client.setDescription("");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForDescriptionModification_addsReplaceMod() throws Exception {
        Application client = new Application();
        client.setDescription("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        Application oldClient = new Application();
        oldClient.setDescription("different");
        List<Modification> result = spy.getModifications(oldClient, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForDescriptionModification_addsNoMod() throws Exception {
        Application client = new Application();
        client.setDescription("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForTitleModification_addsDeleteMod() throws Exception {
        Application client = new Application();
        client.setTitle("");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void checkForTitleModification_addsReplaceMod() throws Exception {
        Application client = new Application();
        client.setTitle("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        Application oldClient = new Application();
        oldClient.setTitle("different");
        List<Modification> result = spy.getModifications(oldClient, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForTitleModification_addsNoMod() throws Exception {
        Application client = new Application();
        client.setTitle("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForEnabledStatusModification_enabledNotNullAndNotEqual_addsReplaceMod() throws Exception {
        Application client = new Application();
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        client.setEnabled(true);
        Application oldClient = new Application();
        oldClient.setEnabled(false);
        List<Modification> result = spy.getModifications(oldClient, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForEnabledStatusModification_notNullAndEqual_addsNoMod() throws Exception {
        Application client = new Application();
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        client.setEnabled(true);
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test
    public void checkForClientSecretModification_addsReplaceMod() throws Exception {
        Application client = new Application();
        client.setClientSecretObj(ClientSecret.newInstance("secret"));
        List<Modification> result = spy.getModifications(client, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("mods", result.get(1).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForRCNModification_rcnNotNullAndNotEqual_addsReplaceMod() throws Exception {
        Application client = new Application();
        client.setRCN("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        Application oldClient = new Application();
        oldClient.setRCN("old");
        List<Modification> result = spy.getModifications(oldClient, client);
        assertThat("mods", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test
    public void checkForRCNModification_rcnNullAndEqual_addsNoMod() throws Exception {
        Application client = new Application();
        client.setRCN("new");
        client.setClientSecretObj(ClientSecret.existingInstance(null));
        Application oldClient = new Application();
        oldClient.setRCN("new");
        List<Modification> result = spy.getModifications(oldClient, client);
        assertThat("mods", result.isEmpty(), equalTo(true));
    }

    @Test (expected = IllegalArgumentException.class)
    public void addClientRole_clientUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addClientRole("", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addClientRole_roleIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addClientRole("uniqueId", null);
    }

    @Test (expected = IllegalStateException.class)
    public void addClientRole_entryIsNull_throwsLDAPException() throws Exception {
        ClientRole clientRole = new ClientRole();
        doNothing().when(spy).addContainer("uniqueId", LdapRepository.CONTAINER_ROLES);
        doReturn(null).doReturn(new SearchResultEntry("", new Attribute[0])).when(spy).getContainer("uniqueId", LdapRepository.CONTAINER_ROLES);
        spy.addClientRole("uniqueId", clientRole);
    }

    @Test (expected = IllegalStateException.class)
    public void addClientRole_entryNotNull_throwsLDAPException() throws Exception {
        ClientRole clientRole = new ClientRole();
        doNothing().when(spy).addContainer("uniqueId", LdapRepository.CONTAINER_ROLES);
        doReturn(new SearchResultEntry("", new Attribute[0])).when(spy).getContainer("uniqueId", LdapRepository.CONTAINER_ROLES);
        spy.addClientRole("uniqueId", clientRole);
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteClientRole_roleIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.deleteClientRole(null);
    }

    @Test
    public void deleteClientRole_callsDeleteEntryAndSubtree() throws Exception {
        ClientRole role = new ClientRole();
        doNothing().when(spy).deleteEntryAndSubtree(anyString(), any(Audit.class));
        spy.deleteClientRole(role);
        verify(spy).deleteEntryAndSubtree(anyString(), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientRoleByClientIdAndRoleName_clientIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientRoleByClientIdAndRoleName("", null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientRoleByClientIdAndRoleName_roleNameIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientRoleByClientIdAndRoleName("clientId", null);
    }

    @Test (expected = IllegalStateException.class)
    public void getClientRoleByClientIdAndRoleName_callsSingleClientRole_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getSingleClientRole(anyString(), any(Filter.class));
        spy.getClientRoleByClientIdAndRoleName("clientId", "roleName");
    }

    @Test
    public void getClientRoleByClientIdAndRoleName_foundClientRole_returnsRole() throws Exception {
        ClientRole role = new ClientRole();
        doReturn(role).when(spy).getSingleClientRole(anyString(), any(Filter.class));
        ClientRole result = spy.getClientRoleByClientIdAndRoleName("clientId", "roleName");
        assertThat("client role", result, equalTo(role));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientRolesByClientId_clientIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientRolesByClientId("");
    }

    @Test (expected = IllegalStateException.class)
    public void getClientRolesByClientId_callsGetMultipleClientRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleClientRoles(anyString(), any(Filter.class));
        spy.getClientRolesByClientId("clientId");
    }

    @Test
    public void getClientRolesByClientId_foundClientRoles_returnsRoles() throws Exception {
        ClientRole clientRole = new ClientRole();
        ArrayList<ClientRole> roleList = new ArrayList<ClientRole>();
        roleList.add(clientRole);
        doReturn(roleList).when(spy).getMultipleClientRoles(anyString(), any(Filter.class));
        List<ClientRole> result = spy.getClientRolesByClientId("clientId");
        assertThat("client role", result.get(0), equalTo(clientRole));
    }

    @Test (expected = IllegalStateException.class)
    public void getAllClientRoles_withFiltersParam_CallsGetMultipleClientRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleClientRoles(anyString(), any(Filter.class));
        spy.getAllClientRoles(null);
    }

    @Test
    public void getAllClientRoles_withFiltersParam_emptyFilterList_returnsRoles() throws Exception {
        doReturn(new ArrayList<ClientRole>()).when(spy).getMultipleClientRoles(anyString(), any(Filter.class));
        List<ClientRole> result = spy.getAllClientRoles(new ArrayList<FilterParam>());
        assertThat("roles", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAllClientRoles_withFiltersParam_addsFiltersToSearchBuilder() throws Exception {
        ArgumentCaptor<Filter> argumentCaptor = ArgumentCaptor.forClass(Filter.class);
        List<FilterParam> filterParamList = new ArrayList<FilterParam>();
        filterParamList.add(new FilterParam(FilterParam.FilterParamName.ROLE_NAME, "roleName"));
        filterParamList.add(new FilterParam(FilterParam.FilterParamName.APPLICATION_ID, "applicationId"));
        filterParamList.add(new FilterParam(FilterParam.FilterParamName.TENANT_ID, "tenantId"));
        doReturn(new ArrayList<ClientRole>()).when(spy).getMultipleClientRoles(anyString(), argumentCaptor.capture());
        spy.getAllClientRoles(filterParamList);
        Filter[] result = argumentCaptor.getValue().getComponents();
        assertThat("role name", result[1].getAssertionValue(), equalTo("roleName"));
        assertThat("application id", result[2].getAssertionValue(), equalTo("applicationId"));
        assertThat("filter size", result.length, equalTo(3));
    }

    @Test (expected = IllegalStateException.class)
    public void getAllClientRoles_callsGetMultipleClientRoles_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getMultipleClientRoles(anyString(), any(Filter.class));
        spy.getAllClientRoles();
    }

    @Test
    public void getAllClientRoles_foundRoles_returnClientRoles() throws Exception {
        ClientRole clientRole = new ClientRole();
        ArrayList<ClientRole> roleList = new ArrayList<ClientRole>();
        roleList.add(clientRole);
        doReturn(roleList).when(spy).getMultipleClientRoles(anyString(), any(Filter.class));
        List<ClientRole> result = spy.getAllClientRoles();
        assertThat("client role", result.get(0), equalTo(clientRole));
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClientRole_roleIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateClientRole(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClientRole_roleUniqueIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateClientRole(new ClientRole());
    }

    @Test (expected = IllegalStateException.class)
    public void updateClientRole_callsLDAPPersister_throwsLDAPException() throws Exception {
        ClientRole role = mock(ClientRole.class);
        when(role.getUniqueId()).thenReturn("uniqueId");
        spy.updateClientRole(role);
    }

    @Test
    public void getMultipleClientRoles_addsClientRole_returnsRoleList() throws Exception {
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        entries.add(searchResultEntry);
        ClientRole clientRole = new ClientRole();
        doReturn(clientRole).when(spy).getClientRole(searchResultEntry);
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        List<ClientRole> result = spy.getMultipleClientRoles("", null);
        assertThat("client role", result.get(0), equalTo(clientRole));
    }

    @Test
    public void getMultipleClientRoles_usesCorrectParams_whenCallingGetMultipleEntries() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS,LdapRepository.OBJECTCLASS_CLIENT_ROLE).build();
        doReturn(null).when(spy).getMultipleEntries("base", SearchScope.SUB, searchFilter, null);
        spy.getMultipleClientRoles("base", searchFilter);
        verify(spy).getMultipleEntries("base", SearchScope.SUB, searchFilter, null);
    }

    @Test
    public void getMultipleClientRoles_noEntries_returnsEmptyList() throws Exception {
        ArrayList<SearchResultEntry> entries = new ArrayList<SearchResultEntry>();
        doReturn(entries).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class), anyString());
        List<ClientRole> result = spy.getMultipleClientRoles("", null);
        assertThat("client role", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getSingleClientRole_foundRole_returnsRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry("", SearchScope.SUB, null);
        doReturn(clientRole).when(spy).getClientRole(searchResultEntry);
        ClientRole result = spy.getSingleClientRole("", null);
        assertThat("client role", result, equalTo(clientRole));
    }

    @Test
    public void getClientRole_entryIsNull_returnsNull() throws Exception {
        ClientRole result = ldapApplicationRepository.getClientRole(null);
        assertThat("client role", result, equalTo(null));
    }

    @Test
    public void getClientRole_foundRole_returnsClientRole() throws Exception {
        ClientRole result = ldapApplicationRepository.getClientRole(new SearchResultEntry("uniqueId", new Attribute[0]));
        assertThat("client role", result.getUniqueId(), equalTo("uniqueId"));
    }

    @Test
    public void getNextRoleId_callsGetNextId() throws Exception {
        doReturn("").when(spy).getNextId(LdapRepository.NEXT_ROLE_ID);
        spy.getNextRoleId();
        verify(spy).getNextId(LdapRepository.NEXT_ROLE_ID);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientRoleById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientRoleById("");
    }

    @Test (expected = IllegalStateException.class)
    public void getClientRoleById_callsGetSingleClientRole_throwsLDAPPersistException() throws Exception {
        doThrow(new LDAPPersistException("error")).when(spy).getSingleClientRole(anyString(), any(Filter.class));
        spy.getClientRoleById("id");
    }

    @Test
    public void getClientRoleById_foundRole_returnsClientRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        doReturn(clientRole).when(spy).getSingleClientRole(anyString(), any(Filter.class));
        ClientRole result = spy.getClientRoleById("id");
        assertThat("client role", result, equalTo(clientRole));
    }

    @Test
    public void getOpenStackServices_foundClients_returnsList() throws Exception {
        Applications clients = new Applications();
        Application client = new Application();
        ArrayList<Application> clientList = new ArrayList<Application>();
        clientList.add(client);
        clients.setClients(clientList);
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), anyInt(), anyInt());
        List<Application> result = spy.getOpenStackServices();
        assertThat("client", result.get(0), equalTo(client));
    }

    @Test (expected = IllegalStateException.class)
    public void softDeleteApplication_callsLDAPInterfaceModify_throwsIllegalStateException() throws Exception {
        Application application = new Application();
        application.setUniqueId("uniqueId");
        application.setClientId("clientId");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.softDeleteApplication(application);
    }

    @Test
    public void softDeleteApplications_callsLDAPInterface_modify() throws Exception {
        Application application = new Application();
        application.setUniqueId("uniqueId");
        application.setClientId("clientId");
        doReturn(new LDAPResult(1, ResultCode.SUCCESS)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.softDeleteApplication(application);
        verify(ldapInterface).modify(anyString(), any(Modification.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedApplicationById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getSoftDeletedApplicationById("");
    }

    @Test
    public void getSoftDeletedApplicationById_foundApplication_returnsApplication() throws Exception {
        Application application = new Application();
        doReturn(application).when(spy).getSingleSoftDeletedClient(any(Filter.class));
        Application result = spy.getSoftDeletedApplicationById("id");
        assertThat("application", result, equalTo(application));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedClientByName_clientNameIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getSoftDeletedClientByName("");
    }

    @Test
    public void getSoftDeletedClientByName_foundApplication_returnsApplication() throws Exception {
        Application application = new Application();
        doReturn(application).when(spy).getSingleSoftDeletedClient(any(Filter.class));
        Application result = spy.getSoftDeletedClientByName("clientName");
        assertThat("application", result, equalTo(application));
    }

    @Test (expected = IllegalStateException.class)
    public void unSoftDeleteApplication_callsLDAPInterfaceModify_throwsIllegalStateException() throws Exception {
        Application application = new Application();
        application.setUniqueId("uniqueId");
        application.setClientId("clientId");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.unSoftDeleteApplication(application);
    }

    @Test
    public void unSoftDeleteApplications_callsLDAPInterface_modify() throws Exception {
        Application application = new Application();
        application.setUniqueId("uniqueId");
        application.setClientId("clientId");
        doReturn(new LDAPResult(1, ResultCode.SUCCESS)).when(ldapInterface).modify(anyString(), any(Modification.class));
        spy.unSoftDeleteApplication(application);
        verify(ldapInterface).modify(anyString(), any(Modification.class));
    }
}
