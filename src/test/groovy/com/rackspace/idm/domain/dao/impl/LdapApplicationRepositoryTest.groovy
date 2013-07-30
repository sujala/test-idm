package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.audit.Audit
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.entity.Applications
import com.rackspace.idm.domain.entity.ClientAuthenticationResult
import com.rackspace.idm.domain.entity.ClientSecret
import com.rackspace.idm.domain.entity.FilterParam
import com.rackspace.idm.domain.service.PropertiesService
import com.rackspace.idm.util.CryptHelper
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.Control
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.LDAPSearchException
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import com.unboundid.ldap.sdk.ReadOnlyEntry
import com.unboundid.ldap.sdk.ResultCode
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchScope
import org.apache.commons.configuration.Configuration

import junit.framework.Assert
import org.bouncycastle.crypto.InvalidCipherTextException
import org.easymock.EasyMock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.test.context.ContextConfiguration

import java.security.GeneralSecurityException

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Matchers.any
import static org.mockito.Matchers.anySet
import static org.mockito.Matchers.anyString
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.doNothing
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 7/26/13
 * Time: 1:43 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
@RunWith(MockitoJUnitRunner.class)
class LdapApplicationRepositoryTest {

    String clientId = "clientId"
    ClientSecret clientSecret = ClientSecret.newInstance("Secret")
    String name = "name"
    String customerId = "customerId"
    String salt = "a1 b1"
    String version = "0"
    String dn = "clientId=clientId,ou=applications,o=rackspace,dc=rackspace,dc=com"

    @InjectMocks
    private LdapApplicationRepository ldapApplicationRepository = new LdapApplicationRepository();
    @Mock
    private LdapConnectionPools ldapConnectionPools;
    @Mock
    private CryptHelper cryptHelper;

    @Mock
    private PropertiesService propertiesService;

    @Mock
    protected Configuration config;

    private LdapApplicationRepository spy;
    private LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
        ldapInterface = mock(LDAPInterface.class);

        ldapApplicationRepository.setCryptHelper(cryptHelper);
        spy = spy(ldapApplicationRepository);

        when(cryptHelper.encrypt(anyString(), anyString(), anyString())).thenReturn(new byte[0]);
        when(cryptHelper.decrypt(any(byte[].class), anyString(), anyString())).thenReturn("someString");
        when(propertiesService.getValue(anyString())).thenReturn("0");
        when(config.getString(anyString())).thenReturn("a1 b1");

        doReturn(ldapInterface).when(spy).getAppInterface();
    }

    @Test (expected = IllegalArgumentException.class)
    public void addClient_clientIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.addClient(null)
    }

    @Test
    public void addClient_callsAddEntry() throws Exception {
        Application client = getFakeApp()
        doNothing().when(spy).addEntry(anyString(), any(Attribute[].class), any(Audit.class))
        spy.addClient(client)
        verify(spy).addObject(any(Application.class))
    }

    @Test
    public void authenticate_clientIsNull_returnsNewClientAuthenticationResultWithNullClient() throws Exception {
        doReturn(null).when(spy).getApplicationByClientId("clientId")
        ClientAuthenticationResult result = spy.authenticate("clientId", "secret")
        assertThat("client authenticate result", result.getClient(), equalTo(null))
    }

    @Test
    public void deleteClient_clientNotNull_callsDeleteEntryAndSubtree() throws Exception {
        Application client = getFakeApp()
        doNothing().when(spy).deleteEntryAndSubtree(any(String.class), any(Audit.class))
        spy.deleteApplication(client)
        verify(spy).deleteEntryAndSubtree(any(String.class), any(Audit.class))
    }

    @Test
    public void deleteClient_clientIsNull_doesNothing() throws Exception {
        ldapApplicationRepository.deleteApplication(null)
    }

    @Test
    public void getAllClients_addClientsToList_returnsClients() throws Exception {
        Application application = getFakeApp()
        List<Application> apps = new ArrayList<Application>()
        apps.add(application)
        doReturn(apps).when(spy).getObjects(any(Filter.class))
        List<Application> result = spy.getAllApplications()
        assertThat("client", result.get(0), equalTo(application))
    }

    @Test
    public void getClientByClientId_clientIdIsBlank_returnsNull() throws Exception {
        Application result = ldapApplicationRepository.getApplicationByClientId("")
        assertThat("client", result, equalTo(null))
    }

    @Test
    public void getClientByClientId_foundClient_returnsClient() throws Exception {
        Application application = getFakeApp()
        doReturn(application).when(spy).getObject(any(Filter.class))
        Application result = spy.getApplicationByClientId("clientId")
        assertThat("client", result, equalTo(application))
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientByClientName_clientNameIsBlank_throwsIllegalArgument() throws Exception {
        Application result = spy.getApplicationByName("")
    }

    @Test
    public void getClientByClientName_foundClient_returnsClient() throws Exception {
        Application client = getFakeApp()
        doReturn(client).when(spy).getObject(any(Filter.class))
        Application result = spy.getApplicationByName("clientName")
        assertThat("client", result, equalTo(client))
    }

    @Test
    public void getClientByCustomerIdAndClientId_clientIdIsBlank_returnsNull() throws Exception {
        Application result = ldapApplicationRepository.getApplicationByCustomerIdAndClientId("customerId", "   ")
        assertThat("client", result, equalTo(null))
    }

    @Test
    public void getClientByCustomerIdAndClientId_foundClient_returnsClient() throws Exception {
        Application application = getFakeApp()
        doReturn(application).when(spy).getObject(any(Filter.class))
        Application result = spy.getApplicationByCustomerIdAndClientId("customerId", "clientId")
        assertThat("client", result, equalTo(application))
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getApplicationById("")
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientByScope_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getApplicationByScope("")
    }

    @Test
    public void getClientById_foundClient_returnsClient() throws Exception {
        Application application = getFakeApp()
        doReturn(application).when(spy).getObject(any(Filter.class))
        Application result = spy.getApplicationById("clientId")
        assertThat("client", result, equalTo(application))
    }

    @Test
    public void getClientByScope_foundClient_returnsClient() throws Exception {
        Application application = getFakeApp()
        doReturn(application).when(spy).getObject(any(Filter.class))
        Application result = spy.getApplicationByScope("scope")
        assertThat("client", result, equalTo(application))
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientsByCustomerId_customerIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getClientsByCustomerId("  ", 1, 1)
    }

    @Test
    public void getClientsByCustomerId_foundClients_returnsClients() throws Exception {
        Applications clients = new Applications()
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1))
        Applications result = spy.getClientsByCustomerId("customerId", 1, 1)
        assertThat("clients", result, equalTo(clients))
    }

    @Test
    public void getAllClients_filtersNotNullAndParamNameIsApplicationName_returnsApplications() throws Exception {
        FilterParam filterParam = new FilterParam(FilterParam.FilterParamName.APPLICATION_NAME, "1")
        List<FilterParam> filters = new ArrayList<FilterParam>()
        filters.add(filterParam)
        Applications clients = new Applications()
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1))
        Applications result = spy.getAllApplications(filters, 1, 1)
        assertThat("clients", result, equalTo(clients))
    }


    @Test
    public void getAllClients_filtersNotNullAndParamNameIsNotApplicationName_onlyAddsOneFilter() throws Exception {
        ArgumentCaptor<Filter> argumentCapture = ArgumentCaptor.forClass(Filter.class)
        FilterParam filterParam = new FilterParam(FilterParam.FilterParamName.ROLE_NAME, "1")
        List<FilterParam> filters = new ArrayList<FilterParam>()
        filters.add(filterParam)
        Applications clients = new Applications()
        doReturn(clients).when(spy).getMultipleClients(argumentCapture.capture(), eq(1), eq(1))
        spy.getAllApplications(filters, 1, 1)
        Filter[] filtersCaptured = argumentCapture.getValue().getComponents()
        assertThat("only one filter was added, so no components",filtersCaptured.length,equalTo(0))
    }

    @Test
    public void getAllClients_filterIsNull_returnsApplications() throws Exception {
        Applications clients = new Applications()
        doReturn(clients).when(spy).getMultipleClients(any(Filter.class), eq(1), eq(1))
        Applications result = spy.getAllApplications(null, 1, 1)
        assertThat("clients", result, equalTo(clients))
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClient_clientIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateApplication(null)
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClient_clientIdIsNull_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.updateApplication(new Application())
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateClient_oldClientIsNull_throwsIllegalArgument() throws Exception {
        Application client = getFakeApp()
        doReturn(null).when(spy).getApplicationByClientId("clientId")
        spy.updateApplication(client)
    }

    @Test
    public void updateClient_callsUpdateEntry() throws Exception {
        Application client = getFakeApp()
        Application oldClient = client
        ArrayList<Modification> modificationList = new ArrayList<Modification>()
        Modification modification = new Modification(ModificationType.ADD, "ADD")
        modificationList.add(modification)
        doReturn(modificationList).when(spy).getModifications(oldClient, client)
        doReturn(oldClient).when(spy).getApplicationByClientId("clientId")
        doNothing().when(spy).updateEntry(eq("uniqueId"), eq(modificationList), any(Audit.class))
        spy.updateApplication(client)
        verify(spy).updateEntry(eq("uniqueId"), eq(modificationList), any(Audit.class))
    }

    @Test (expected = IllegalStateException.class)
    public void updateClient_updateEntry_throwsGeneralSecurityException() throws Exception {
        Application client = getFakeApp()
        Application oldClient = client
        ArrayList<Modification> modificationList = new ArrayList<Modification>()
        Modification modification = new Modification(ModificationType.ADD, "ADD")
        modificationList.add(modification)
        doReturn(oldClient).when(spy).getApplicationByClientId("clientId")
        doThrow(new GeneralSecurityException()).when(spy).getModifications(oldClient, client)
        spy.updateApplication(client)
    }

    @Test (expected = IllegalStateException.class)
    public void updateClient_updateEntry_throwsInvalidCipherTextException() throws Exception {
        Application client = getFakeApp()
        Application oldClient = client
        ArrayList<Modification> modificationList = new ArrayList<Modification>()
        Modification modification = new Modification(ModificationType.ADD, "ADD")
        modificationList.add(modification)
        doReturn(oldClient).when(spy).getApplicationByClientId("clientId")
        doThrow(new InvalidCipherTextException()).when(spy).getModifications(oldClient, client)
        spy.updateApplication(client)
    }

    @Test
    public void getAvailableScopes_foundClient_returnsClientList() throws Exception {
        Application application = getFakeApp()
        List<Application> apps = new ArrayList<Application>()
        apps.add(application)
        doReturn(apps).when(spy).getObjects(any(Filter.class))
        List<Application> result = spy.getAvailableScopes()
        assertThat("client", result.get(0), equalTo(application))
    }

    @Test
    public void getAvailableScopes_ZeroEntries_returnsEmptyClientList() throws Exception {
        List<Application> apps = new ArrayList<Application>()
        doReturn(apps).when(spy).getObjects(any(Filter.class))
        List<Application> result = spy.getAvailableScopes()
        assertThat("client", result.isEmpty(), equalTo(true))
    }

    @Test
    public void getMultipleClients_contentCountLessThanOffset_setClientsToEmptyList() throws Exception {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>()
        doReturn(1).when(spy).getLdapPagingLimitMax()
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, null, null)
        Applications result = spy.getMultipleClients(null, 2, 2)
        assertThat("client", result.getClients().isEmpty(), equalTo(true))
    }

    @Test
    public void getMultipleClients_subListIsEmpty_setClientsToEmptyList() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0])
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>()
        searchResultEntryList.add(searchResultEntry)
        doReturn(0).when(spy).getLdapPagingOffsetDefault()
        doReturn(0).when(spy).getLdapPagingLimitDefault()
        doReturn(3).when(spy).getLdapPagingLimitMax()
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, null, null)
        Applications result = spy.getMultipleClients(null, -1, 0)
        assertThat("client", result.getClients().isEmpty(), equalTo(true))
    }

    @Test
    public void getMultipleClients_callsGetClient_returnsClientsWithAddedClient() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0])
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>()
        searchResultEntryList.add(searchResultEntry)
        searchResultEntryList.add(searchResultEntry)
        doReturn(3).when(spy).getLdapPagingLimitMax()
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(LdapRepository.APPLICATIONS_BASE_DN, SearchScope.SUB, null, null)
        Applications result = spy.getMultipleClients(null, 1, 2)
        assertThat("client", result.getClients().size(), equalTo(1))
    }

    @Test
    public void getSingleClient_notFoundClient_returnsNull() throws Exception {
        doReturn(null).when(spy).getObject(any(Filter.class))
        Application result = spy.getSingleClient(null)
        assertThat("client", result, equalTo(null))
    }

    @Test
    public void getSingleClient_foundClient_returnsClient() throws Exception {
        Application client = new Application()
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0])
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class))
        doReturn(client).when(spy).getClient(searchResultEntry)
        Application result = spy.getSingleClient(null)
        assertThat("client", result, equalTo(client))
    }

    @Test
    public void getSingleSoftDeletedClient_notFoundClient_returnsNull() throws Exception {
        doReturn(null).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class))
        Application result = spy.getSingleSoftDeletedClient(null)
        assertThat("client", result, equalTo(null))
    }

    @Test
    public void getSingleSoftDeletedClient_foundClient_returnsClient() throws Exception {
        Application application = getFakeApp()
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0])
        doReturn(application).when(spy).getObject(any(Filter.class), anyString())
        Application result = spy.getSingleSoftDeletedClient(null)
        assertThat("client", result, equalTo(application))
    }

    @Test (expected = IllegalStateException.class)
    public void softDeleteApplication_callsLDAPInterfaceModify_throwsIllegalStateException() throws Exception {
        Application application = getFakeApp()
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modifyDN(anyString(), anyString(), eq(true), anyString())
        spy.softDeleteApplication(application)
    }

    @Test
    public void softDeleteApplications_callsLDAPInterface_modify() throws Exception {
        Application application = getFakeApp()
        doReturn(new LDAPResult(1, ResultCode.SUCCESS)).when(ldapInterface).modifyDN(anyString(), anyString(), eq(true), anyString())
        spy.softDeleteApplication(application)
        verify(ldapInterface).modifyDN(anyString(), anyString(), eq(true), anyString())
    }

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedApplicationById_idIsBlank_throwsIllegalArgument() throws Exception {
        ldapApplicationRepository.getSoftDeletedApplicationById("")
    }

    @Test
    public void getSoftDeletedApplicationById_foundApplication_returnsApplication() throws Exception {
        Application application = getFakeApp()
        doReturn(application).when(spy).getObject(any(Filter.class), anyString())
        Application result = spy.getSoftDeletedApplicationById("id")
        assertThat("application", result, equalTo(application))
    }

    @Test (expected = IllegalArgumentException.class)
    public void getSoftDeletedClientByName_clientNameIsBlank_throwsIllegalArgument() throws Exception {
        spy.getSoftDeletedClientByName("")
    }

    @Test
    public void getSoftDeletedClientByName_foundApplication_returnsApplication() throws Exception {
        Application application = new Application()
        doReturn(application).when(spy).getSingleSoftDeletedClient(any(Filter.class))
        Application result = spy.getSoftDeletedClientByName("clientName")
        assertThat("application", result, equalTo(application))
    }

    @Test (expected = IllegalStateException.class)
    public void unSoftDeleteApplication_callsLDAPInterfaceModify_throwsIllegalStateException() throws Exception {
        Application application = getFakeApp()
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modifyDN(anyString(), anyString(), eq(true), anyString())
        spy.unSoftDeleteApplication(application)
    }

    @Test
    public void unSoftDeleteApplications_callsLDAPInterface_modify() throws Exception {
        Application application = getFakeApp()
        doReturn(new LDAPResult(1, ResultCode.SUCCESS)).when(ldapInterface).modifyDN(anyString(), anyString(), eq(true), anyString())
        spy.unSoftDeleteApplication(application)
        verify(ldapInterface).modifyDN(anyString(), anyString(), eq(true), anyString())
    }

    private Application getFakeApp() {
        Entry entry = new Entry(dn)
        Application app = new Application(clientId, clientSecret, name, customerId)
        app.ldapEntry = new ReadOnlyEntry(entry)
        app.salt = salt
        app.encryptionVersion = version
        return app
    }

    private List<Application> getFakeAppList() {
        List<Application> apps = new ArrayList<Application>()
        apps.add(getFakeApp())
        return apps
    }
}
