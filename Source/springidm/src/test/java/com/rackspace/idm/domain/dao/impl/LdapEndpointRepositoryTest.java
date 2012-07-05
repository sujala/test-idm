package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/3/12
 * Time: 12:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapEndpointRepositoryTest {
    LdapEndpointRepository ldapEndpointRepository;
    LdapEndpointRepository spy;
    LDAPInterface ldapInterface;
    EndPoints endPoints;

    @Before
    public void setUp() throws Exception {
        ldapEndpointRepository = new LdapEndpointRepository(mock(LdapConnectionPools.class), mock(Configuration.class));
        ldapInterface = mock(LDAPInterface.class);

        //setup fields
        endPoints = new EndPoints();
        endPoints.setMossoId(1);
        endPoints.setNastId("nastId");
        endPoints.setUserDN("userDn");
        endPoints.setUsername("username");
        int i = endPoints.hashCode();
        boolean equals = endPoints.equals(endPoints);
        String s = endPoints.toString();
        spy = spy(ldapEndpointRepository);
        doReturn(ldapInterface).when(spy).getAppInterface();
    }

    @Test (expected = IllegalArgumentException.class)
    public void addBaseUrl_baseUrlIsNull_throwsIllegalArgumentException() throws Exception {
        ldapEndpointRepository.addBaseUrl(null);
    }

    @Test
    public void addBaseUrl_addsAllCloudBaseUrlAttributes_succeed() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setGlobal(true);
        when(ldapInterface.add(anyString(), argumentCaptor.capture())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.addBaseUrl(cloudBaseUrl);
        List<Attribute> result = argumentCaptor.getValue();
        assertThat("admin url", result.get(1).getValue(), equalTo("adminUrl"));
        assertThat("base url type", result.get(2).getValue(), equalTo("baseUrlType"));
        assertThat("base url id", result.get(3).getValue(), equalTo("1"));
        assertThat("internal url", result.get(4).getValue(), equalTo("internalUrl"));
        assertThat("publie url", result.get(5).getValue(), equalTo("publicUrl"));
        assertThat("region", result.get(6).getValue(), equalTo("region"));
        assertThat("service name", result.get(7).getValue(), equalTo("serviceName"));
        assertThat("def", result.get(8).getValue(), equalTo("true"));
        assertThat("enabled", result.get(9).getValue(), equalTo("true"));
        assertThat("open stack type", result.get(10).getValue(), equalTo("openStackType"));
        assertThat("version id", result.get(11).getValue(), equalTo("versionId"));
        assertThat("version info", result.get(12).getValue(), equalTo("versionInfo"));
        assertThat("version list", result.get(13).getValue(), equalTo("versionList"));
        assertThat("global", result.get(14).getValue(), equalTo("true"));
    }

    @Test (expected = IllegalStateException.class)
    public void addBaseUrl_callsLDAPInterfaceAdd_throwsIllegalStateException() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setBaseUrlId(0);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.toString();
        cloudBaseUrl.getAuditContext();
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).add(anyString(), any(List.class));
        spy.addBaseUrl(cloudBaseUrl);
    }

    @Test (expected = IllegalStateException.class)
    public void addBaseUrl_resultCodeNotSuccess_throwsIllegalStateException() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        when(ldapInterface.add(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.addBaseUrl(cloudBaseUrl);
    }

    @Test (expected = NotFoundException.class)
    public void addBaseUrlToUser_baseUrlIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getBaseUrlById(1);
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test (expected = BaseUrlConflictException.class)
    public void addBaseUrlToUser_endpointStringContainsDashSign_throwsBaseUrlConflictException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("-1");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        spy.addBaseUrlToUser(1, true, "username");
    }

    @Test (expected = BaseUrlConflictException.class)
    public void addBaseUrlToUser_endpointStringContainsPlusSign_throwsBaseUrlConflictException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("+1");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test (expected = IllegalStateException.class)
    public void addBaseUrlToUser_callsLDAPInterfaceModify_throwsLDAPException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("notExisting");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).modify(anyString(), any(List.class));
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test (expected = IllegalArgumentException.class)
    public void addBaseUrlToUser_resultCodeNotSuccess_throwsIllegalArgumentException() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.addBaseUrlToUser(1, false, "username");
    }

    @Test
    public void addBaseUrlToUser_resultCodeSuccess_urlAddedSuccessfully() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("notExisting");
        endPoints.setEndpoints(endpointList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        when(ldapInterface.modify(anyString(), any(List.class))).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.addBaseUrlToUser(1, false, "username");
        verify(ldapInterface).modify(anyString(), any(List.class));
    }

    @Test (expected = IllegalStateException.class)
    public void deleteBaseUrl_callsLDAPInterfaceDelete_throwsLDAPException() throws Exception {
        doThrow(new LDAPException(ResultCode.LOCAL_ERROR)).when(ldapInterface).delete(anyString());
        spy.deleteBaseUrl(1);
    }

    @Test (expected = IllegalStateException.class)
    public void deleteBaseUrl_resultCodeNotSuccess_throwsIllegalStateException() throws Exception {
        when(ldapInterface.delete(anyString())).thenReturn(new LDAPResult(1, ResultCode.LOCAL_ERROR));
        spy.deleteBaseUrl(1);
    }

    @Test
    public void deleteBaseUrl_resultCodeSuccess_deletesSuccessfully() throws Exception {
        when(ldapInterface.delete(anyString())).thenReturn(new LDAPResult(1, ResultCode.SUCCESS));
        spy.deleteBaseUrl(1);
    }

    @Test
    public void getBaseUrlById_returnsBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        doReturn(searchResultEntry).when(spy).getSingleEntry(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(cloudBaseUrl).when(spy).getBaseUrl(searchResultEntry);
        CloudBaseUrl result = spy.getBaseUrlById(1);
        assertThat("cloud base url", result, equalTo(cloudBaseUrl));
    }



    @Test
    public void getBaseUrlsByService_foundBaseUrl_returnsList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(cloudBaseUrl).when(spy).getBaseUrl(searchResultEntry);
        List<CloudBaseUrl> result = spy.getBaseUrlsByService("service");
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void getBaseUrlsByService_noEntryFound_returnsEmptyList() throws Exception {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        List<CloudBaseUrl> result = spy.getBaseUrlsByService("service");
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getBaseUrls_foundBaseUrl_returnsList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.hashCode();
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        searchResultEntryList.add(searchResultEntry);
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        doReturn(cloudBaseUrl).when(spy).getBaseUrl(searchResultEntry);
        List<CloudBaseUrl> result = spy.getBaseUrls();
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void getBaseUrls_noEntryFound_returnsEmptyList() throws Exception {
        List<SearchResultEntry> searchResultEntryList = new ArrayList<SearchResultEntry>();
        doReturn(searchResultEntryList).when(spy).getMultipleEntries(anyString(), any(SearchScope.class), any(Filter.class));
        List<CloudBaseUrl> result = spy.getBaseUrls();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getEndpointsForUser_endPointIsNull_returnsNull() throws Exception {
        doReturn(null).when(spy).getRawEndpointsForUser("username");
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("list", result, equalTo(null));
    }

    @Test
    public void getEndpointsForUser_endPointsSizeIsZero_returnsNull() throws Exception {
        endPoints.setEndpoints(new ArrayList<String>());
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("list", result, equalTo(null));
    }

    @Test
    public void getEndpointsForUser_baseUrlNotNull_setsUpCloudEndpoint() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        List<String> endPointList = new ArrayList<String>();
        endPointList.add("+1");
        endPoints.setEndpoints(endPointList);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("hash code", result.get(0).hashCode(), equalTo(1998524901));
        assertThat("cloud endpoint", result.get(0).toString(), equalTo("CloudEndpoint [v1preferred=true, baseUrl=baseUrl=null, username=username, nastId=nastId, mossoId=1]"));
        assertThat("cloud base url", result.get(0).getBaseUrl(), equalTo(cloudBaseUrl));
        assertThat("mosso id", result.get(0).getMossoId(), equalTo(1));
        assertThat("username", result.get(0).getUsername(), equalTo("username"));
        assertThat("nast id", result.get(0).getNastId(), equalTo("nastId"));
        assertThat("v1 preferred", result.get(0).isV1preferred(), equalTo(true));
    }

    @Test
    public void getEndpoitnsForUser_baseUrlIsNull_returnsEmptyList() throws Exception {
        List<String> endPointList = new ArrayList<String>();
        endPointList.add("+1");
        endPoints.setEndpoints(endPointList);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doReturn(null).when(spy).getBaseUrlById(1);
        List<CloudEndpoint> result = spy.getEndpointsForUser("username");
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getOpenstackEndpointsForTenant_baseUrlIdIsNull_setsEmptyBaseUrlList() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName("name");
        tenant.setTenantId("tenantId");
        OpenstackEndpoint result = spy.getOpenstackEndpointsForTenant(tenant);
        assertThat("baseurl list", result.getBaseUrls().isEmpty(), equalTo(true));
    }

    @Test
    public void getOpenstackEndpointsForTenant_addsBaseUrlToList_setsBaseUrlList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        String[] baseUrlIds = new String[2];
        baseUrlIds[0] = "1";
        baseUrlIds[1] = "2";
        Tenant tenant = new Tenant();
        tenant.setName("name");
        tenant.setTenantId("tenantId");
        tenant.setBaseUrlIds(baseUrlIds);
        doReturn(null).when(spy).getBaseUrlById(1);
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(2);
        OpenstackEndpoint result = spy.getOpenstackEndpointsForTenant(tenant);
        assertThat("tenant name", result.getTenantName(), equalTo("name"));
        assertThat("tenant id", result.getTenantId(), equalTo("tenantId"));
        assertThat("base url", result.getBaseUrls().get(0), equalTo(cloudBaseUrl));
        assertThat("list", result.getBaseUrls().size(), equalTo(1));
    }

    @Test
    public void appendTenantToBaseUrl_setsAllUrlEndWithSlashWithTenantId() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setPublicUrl("publicUrl/");
        cloudBaseUrl.setAdminUrl("adminUrl/");
        cloudBaseUrl.setInternalUrl("internalUrl/");
        spy.appendTenantToBaseUrl("1", cloudBaseUrl);
        assertThat("public url", cloudBaseUrl.getPublicUrl(), equalTo("publicUrl/1"));
        assertThat("admin url", cloudBaseUrl.getAdminUrl(), equalTo("adminUrl/1"));
        assertThat("internal url", cloudBaseUrl.getInternalUrl(), equalTo("internalUrl/1"));
    }

    @Test
    public void appendTenantToBaseUrl_setsAllUrlWithTenantIdAlsoAddsSlashAtEnd() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setInternalUrl("internalUrl");
        spy.appendTenantToBaseUrl("1", cloudBaseUrl);
        assertThat("public url", cloudBaseUrl.getPublicUrl(), equalTo("publicUrl/1"));
        assertThat("admin url", cloudBaseUrl.getAdminUrl(), equalTo("adminUrl/1"));
        assertThat("internal url", cloudBaseUrl.getInternalUrl(), equalTo("internalUrl/1"));
    }

    @Test (expected = NotFoundException.class)
    public void removeBaseUrlFromUser_baseUrlIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getBaseUrlById(1);
        spy.removeBaseUrlFromUser(1, "username");
    }

    @Test
    public void removeBaseUrlFromUser_endpointSizeIsZero_returns() throws Exception {
        endPoints.setEndpoints(new ArrayList<String>());
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        spy.removeBaseUrlFromUser(1, "username");
        verify(spy).getRawEndpointsForUser("username");
    }

    @Test
    public void removeBaseUrlFromUser_addsDeleteMod() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        ArrayList<String> endpointsList = new ArrayList<String>();
        endpointsList.add("+1");
        endPoints.setEndpoints(endpointsList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doNothing().when(spy).updateEntry(eq("userDn"), argumentCaptor.capture(), any(Audit.class));
        spy.removeBaseUrlFromUser(1, "username");
        List<Modification> result = argumentCaptor.getValue();
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("DELETE"));
    }

    @Test
    public void removeBaseUrlFromUser_addsReplaceMod() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        ArrayList<String> endpointsList = new ArrayList<String>();
        endpointsList.add("+2");
        endPoints.setEndpoints(endpointsList);
        doReturn(new CloudBaseUrl()).when(spy).getBaseUrlById(1);
        doReturn(endPoints).when(spy).getRawEndpointsForUser("username");
        doNothing().when(spy).updateEntry(eq("userDn"), argumentCaptor.capture(), any(Audit.class));
        spy.removeBaseUrlFromUser(1, "username");
        List<Modification> result = argumentCaptor.getValue();
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
    }

    @Test (expected = NotFoundException.class)
    public void setBaseUrlEnabled_baseUrlIsNull_throwsNotFoundException() throws Exception {
        doReturn(null).when(spy).getBaseUrlById(1);
        spy.setBaseUrlEnabled(1, true);
    }

    @Test
    public void setBaseUrlEnabled_callsUpdateEntry() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setUniqueId("uniqueId");
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        doNothing().when(spy).updateEntry(eq("uniqueId"), any(List.class), any(Audit.class));
        spy.setBaseUrlEnabled(1, true);
        verify(spy).updateEntry(eq("uniqueId"), any(List.class), any(Audit.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateCloudBaseUrl_cloudBaseUrlIsNull_throwsIllegalArgumentException() throws Exception {
        spy.updateCloudBaseUrl(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void updateCloudBaseUrl_cloudBaseUrlUniqueIdIsBlank_throwsIllegalArgumentException() throws Exception {
        spy.updateCloudBaseUrl(new CloudBaseUrl());
    }

    @Test
    public void updateCloudBaseUrl_modificationSizeIsZero_doesNotUpdate() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setUniqueId("uniqueId");
        cloudBaseUrl.setBaseUrlId(1);
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        doReturn(new ArrayList<Modification>()).when(spy).getModifications(cloudBaseUrl, cloudBaseUrl);
        spy.updateCloudBaseUrl(cloudBaseUrl);
        verify(spy, never()).updateEntry(anyString(), any(List.class), any(Audit.class));
    }

    @Test
    public void updateCloudBaseUrl_modificationSizeMoreThanZero_callsUpdateEntry() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setUniqueId("uniqueId");
        cloudBaseUrl.setBaseUrlId(1);
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(1);
        Modification modification = new Modification(ModificationType.ADD, "add");
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add(modification);
        doReturn(modifications).when(spy).getModifications(cloudBaseUrl, cloudBaseUrl);
        doNothing().when(spy).updateEntry(anyString(), eq(modifications), any(Audit.class));
        spy.updateCloudBaseUrl(cloudBaseUrl);
        verify(spy).updateEntry(anyString(), eq(modifications), any(Audit.class));
    }

    @Test
    public void getBaseUrl_setsAttributeForBaseUrl_returnsCloudBaseUrl() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        CloudBaseUrl result = spy.getBaseUrl(searchResultEntry);
        assertThat("cloud base url", result.toString(), equalTo("baseUrl=null"));
    }

    @Test
    public void getBaseUrl_WithNullSearchResult_returnsNull() throws Exception {
        CloudBaseUrl result = spy.getBaseUrl(null);
        assertThat("cloud base url", result, nullValue());
    }

    @Test (expected = NotFoundException.class)
    public void getRawEndpointsForUser_searchResultIsNull_throwsNotFoundException() throws Exception {
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .build();
        doReturn(null).when(spy).getSingleEntry(LdapRepository.BASE_DN, SearchScope.SUB, searchFilter, new String[]{LdapRepository.ATTR_ENDPOINT, LdapRepository.ATTR_UID, LdapRepository.ATTR_NAST_ID, LdapRepository.ATTR_MOSSO_ID});
        spy.getRawEndpointsForUser("username");
    }

    @Test
    public void getRawEndpointsForUser_listIsNull_setsEmptyListToEndpoints() throws Exception {
        SearchResultEntry searchResultEntry = new SearchResultEntry("", new Attribute[0]);
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .build();
        doReturn(searchResultEntry).when(spy).getSingleEntry(LdapRepository.BASE_DN, SearchScope.SUB, searchFilter, new String[]{LdapRepository.ATTR_ENDPOINT, LdapRepository.ATTR_UID, LdapRepository.ATTR_NAST_ID, LdapRepository.ATTR_MOSSO_ID});
        EndPoints result = spy.getRawEndpointsForUser("username");
        assertThat("list", result.getEndpoints().isEmpty(), equalTo(true));
    }

    @Test
    public void getRawEndpointsForUser_listLengthIsMoreThanZero_setsListToEndpoints() throws Exception {
        Attribute[] attributes = new Attribute[1];
        attributes[0] = new Attribute(LdapRepository.ATTR_ENDPOINT, "endpoint");
        SearchResultEntry searchResultEntry = new SearchResultEntry("", attributes);
        Filter searchFilter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_UID, "username")
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
                .build();
        doReturn(searchResultEntry).when(spy).getSingleEntry(LdapRepository.BASE_DN, SearchScope.SUB, searchFilter, new String[]{LdapRepository.ATTR_ENDPOINT, LdapRepository.ATTR_UID, LdapRepository.ATTR_NAST_ID, LdapRepository.ATTR_MOSSO_ID});
        EndPoints result = spy.getRawEndpointsForUser("username");
        assertThat("list", result.getEndpoints().get(0), equalTo("endpoint"));
    }

    @Test
    public void getModifications_addsReplaceModOfDef() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setDef(true);
        oCloud.setDef(false);
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_DEF));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfAdminUrl() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setDef(true);
        oCloud.setDef(true);
        nCloud.setAdminUrl("new");
        oCloud.setAdminUrl("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_ADMIN_URL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfBaseUrlType() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setAdminUrl("new");
        oCloud.setAdminUrl("new");
        nCloud.setBaseUrlType("new");
        oCloud.setBaseUrlType("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_BASEURL_TYPE));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfEnabled() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setBaseUrlType("new");
        oCloud.setBaseUrlType("new");
        nCloud.setEnabled(true);
        oCloud.setEnabled(false);
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_ENABLED));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfGlobal() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setEnabled(true);
        oCloud.setEnabled(true);
        nCloud.setGlobal(true);
        oCloud.setGlobal(false);
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_GLOBAL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfInternalUrl() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setGlobal(true);
        oCloud.setGlobal(true);
        nCloud.setInternalUrl("new");
        oCloud.setInternalUrl("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_INTERNAL_URL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfPublicUrl() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setInternalUrl("new");
        oCloud.setInternalUrl("new");
        nCloud.setPublicUrl("new");
        oCloud.setPublicUrl("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_PUBLIC_URL));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfRegion() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setPublicUrl("new");
        oCloud.setPublicUrl("new");
        nCloud.setRegion("new");
        oCloud.setRegion("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_REGION));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfServiceName() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setRegion("new");
        oCloud.setRegion("new");
        nCloud.setServiceName("new");
        oCloud.setServiceName("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_SERVICE));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_addsReplaceModOfOpenStackType() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setServiceName("new");
        oCloud.setServiceName("new");
        nCloud.setOpenstackType("new");
        oCloud.setOpenstackType("old");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("mod", result.get(0).getModificationType().toString(), equalTo("REPLACE"));
        assertThat("attribute", result.get(0).getAttributeName(), equalTo(LdapRepository.ATTR_OPENSTACK_TYPE));
        assertThat("list lize", result.size(), equalTo(1));
    }

    @Test
    public void getModifications_openStackTypeMatch_setsNothing() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        nCloud.setOpenstackType("new");
        oCloud.setOpenstackType("new");
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getModifications_notModsAddedToList_returnsEmptyList() throws Exception {
        CloudBaseUrl oCloud = new CloudBaseUrl();
        CloudBaseUrl nCloud = new CloudBaseUrl();
        List<Modification> result = spy.getModifications(oCloud, nCloud);
        assertThat("list", result.isEmpty(), equalTo(true));
    }
}
