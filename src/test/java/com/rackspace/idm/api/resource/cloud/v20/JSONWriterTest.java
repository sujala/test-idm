package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import junit.framework.Assert;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.sun.jersey.api.json.JSONMarshaller;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.common.api.v1.*;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.ext.os_ksec2.v1.Ec2CredentialsType;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.User;
import org.w3._2005.atom.Link;
import org.w3._2005.atom.Relation;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import java.util.GregorianCalendar;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 1/4/12
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONWriterTest {

    private JSONWriter writer = new JSONWriter();
    private XMLGregorianCalendar calendar;
    JSONWriter spy;

    @Before
    public void setup() throws Exception {
        calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        calendar.setDay(1);
        calendar.setMonth(1);
        calendar.setYear(2012);
        spy = spy(writer);
    }

    @Test
    public void getSize_returnsNegativeOne() throws Exception {
        assertThat("long", writer.getSize(null, null, null, null, null), equalTo(-1L));
    }

    @Test
    public void isWritable_typeIsJAXBElement_returnsFalse() throws Exception {
        assertThat("bool", writer.isWriteable(null, JAXBElement.class, null, null), equalTo(false));
    }

    @Test
    public void isWritable_typeIsNotJAXBElement_returnsTrue() throws Exception {
        assertThat("bool", writer.isWriteable(null, PasswordCredentialsRequiredUsername.class, null, null), equalTo(true));
    }

    @Test
    public void isWritable_typeIsParameterizedTypeWithTwoParameters_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Object.class, Object.class});
        assertThat("bool", writer.isWriteable(null, type, null, null), equalTo(false));
    }

    @Test
    public void isWritable_typeIsParameterizedTypeWithOneCorrectParameters_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Application.class});
        assertThat("bool", writer.isWriteable(null, type, null, null), equalTo(true));
    }

    @Test
    public void writeTo_typeVersionChoice_callsGetVersionChoice() throws Exception {
        VersionChoice versionChoice = new VersionChoice();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getVersionChoice(versionChoice);
        spy.writeTo(versionChoice, VersionChoice.class, null, null, null, null, myOut);
        verify(spy).getVersionChoice(versionChoice);
    }

    @Test
    public void writeTo_typeVersionChoice_writesToOutputStream() throws Exception {
        VersionChoice versionChoice = new VersionChoice();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getVersionChoice(versionChoice);
        spy.writeTo(versionChoice, VersionChoice.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_typeTenants_callsGetTenantWithoutWrapper() throws Exception {
        Tenants tenants = new Tenants();
        Tenant tenant = new Tenant();
        tenants.getTenant().add(tenant);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(tenants, VersionChoice.class, null, null, null, null, myOut);
        doReturn(new JSONObject()).when(spy).getTenantWithoutWrapper(tenant);
        verify(spy).getTenantWithoutWrapper(tenant);
    }

    @Test
    public void writeTo_typeTenants_writesToOutputStream() throws Exception {
        Tenants tenants = new Tenants();
        Tenant tenant = new Tenant();
        tenants.getTenant().add(tenant);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getTenantWithoutWrapper(tenant);
        spy.writeTo(tenants, Tenants.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"tenants\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_typeServiceList_callsGetServiceWithoutWrapper() throws Exception {
        ServiceList serviceList = new ServiceList();
        Service service = new Service();
        serviceList.getService().add(service);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getServiceWithoutWrapper(service);
        spy.writeTo(serviceList, ServiceList.class, null, null, null, null, myOut);
        verify(spy).getServiceWithoutWrapper(service);
    }

    @Test
    public void writeTo_typeServiceList_writesToOutputStream() throws Exception {
        ServiceList serviceList = new ServiceList();
        Service service = new Service();
        serviceList.getService().add(service);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getServiceWithoutWrapper(service);
        spy.writeTo(serviceList, ServiceList.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"OS-KSADM:services\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_typeEndPointList_callsGetEndpoint() throws Exception {
        EndpointList endpointList = new EndpointList();
        Endpoint endpoint = new Endpoint();
        endpointList.getEndpoint().add(endpoint);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getEndpoint(endpoint);
        spy.writeTo(endpointList, EndpointList.class, null, null, null, null, myOut);
        verify(spy).getEndpoint(endpoint);
    }

    @Test
    public void writeTo_typeEndPointList_writesToOutputStream() throws Exception {
        EndpointList endpointList = new EndpointList();
        Endpoint endpoint = new Endpoint();
        endpointList.getEndpoint().add(endpoint);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getEndpoint(endpoint);
        spy.writeTo(endpointList, EndpointList.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"endpoints\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_typeEndPointTemplateListFullyPopulated_writesToOutputStreamAllValues() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setId(123);
        endpointTemplate.setEnabled(false);
        endpointTemplate.setRegion("USA");
        endpointTemplate.setPublicURL("www.publicURL.com");
        endpointTemplate.setGlobal(true);
        endpointTemplate.setName("John Smith");
        endpointTemplate.setAdminURL("www.adminURL.com");
        endpointTemplate.setType("CLOUD");
        endpointTemplate.setInternalURL("www.internalURL.com");
        VersionForService versionForService = new VersionForService();
        versionForService.setId("456");
        endpointTemplate.setVersion(versionForService);
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(endpointTemplateList, EndpointList.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"OS-KSCATALOG:endpointTemplates\":[{\"region\":\"USA\",\"id\":123,\"publicURL\":\"www.publicURL.com\"," +
                "\"enabled\":false,\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"global\":true," +
                "\"versionId\":\"456\",\"type\":\"CLOUD\",\"internalURL\":\"www.internalURL.com\"}]}"));
    }

    @Test
    public void writeTo_typeEndPointTemplateListEmptyTemplate_writesToOutputStreamEmptyList() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setId(123);
        endpointTemplate.setEnabled(false);
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(endpointTemplateList, EndpointList.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"OS-KSCATALOG:endpointTemplates\":[{\"id\":123,\"enabled\":false}]}"));
    }

    @Test
    public void writeTo_typeCredentialTypeSecretQA_callsGetSecretQA() throws Exception {
        CredentialType secretQA = new SecretQA();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getSecretQA((SecretQA) secretQA);
        spy.writeTo(secretQA, SecretQA.class, null, null, null, null, myOut);
        verify(spy).getSecretQA((SecretQA) secretQA);
    }

    @Test
    public void writeTo_typeCredentialTypeSecretQA_writesToOutputStream() throws Exception {
        CredentialType secretQA = new SecretQA();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getSecretQA((SecretQA) secretQA);
        spy.writeTo(secretQA, SecretQA.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_typeCredentialTypePasswordCredentialsRequiredUsername_callsGetPasswordCredentials() throws Exception {
        CredentialType passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getPasswordCredentials((PasswordCredentialsRequiredUsername) passwordCredentialsRequiredUsername);
        spy.writeTo(passwordCredentialsRequiredUsername, CredentialType.class, null, null, null, null, myOut);
        verify(spy).getPasswordCredentials((PasswordCredentialsRequiredUsername) passwordCredentialsRequiredUsername);
    }

    @Test
    public void writeTo_typeCredentialTypePasswordCredentialsRequiredUsername_writesToOutputStream() throws Exception {
        CredentialType passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getPasswordCredentials((PasswordCredentialsRequiredUsername) passwordCredentialsRequiredUsername);
        spy.writeTo(passwordCredentialsRequiredUsername, CredentialType.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_typeCredentialTypePasswordCredentialsBase_callsGetPasswordCredentials() throws Exception {
        CredentialType passwordCredentialsBase = new PasswordCredentialsBase();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getPasswordCredentials((PasswordCredentialsBase) passwordCredentialsBase);
        spy.writeTo(passwordCredentialsBase, CredentialType.class, null, null, null, null, myOut);
        verify(spy).getPasswordCredentials((PasswordCredentialsBase) passwordCredentialsBase);
    }

    @Test
    public void writeTo_typeCredentialTypePasswordCredentialsBase_writesToOutputStream() throws Exception {
        CredentialType passwordCredentialsBase = new PasswordCredentialsBase();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getPasswordCredentials((PasswordCredentialsBase) passwordCredentialsBase);
        spy.writeTo(passwordCredentialsBase, CredentialType.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test(expected = BadRequestException.class)
    public void writeTo_typeEC2CredentialsType_throwException() throws Exception {
        Ec2CredentialsType ec2CredentialsType = new Ec2CredentialsType();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(ec2CredentialsType, CredentialType.class, null, null, null, null, myOut);
    }

    @Test
    public void writeTo_typeGroup_callsGetGroup() throws Exception {
        Group group = new Group();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getGroup(group);
        spy.writeTo(group, Group.class, null, null, null, null, myOut);
        verify(spy).getGroup(group);
    }

    @Test
    public void writeTo_typeGroup_writesToOutputStream() throws Exception {
        Group group = new Group();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getGroup(group);
        spy.writeTo(group, Group.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_typeGroupsList_callsGetGroupsList() throws Exception {
        GroupsList groupsList = new GroupsList();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getGroupsList(groupsList);
        spy.writeTo(groupsList, GroupsList.class, null, null, null, null, myOut);
        verify(spy).getGroupsList(groupsList);
    }

    @Test
    public void writeTo_typeGroupsList_writesToOutputStream() throws Exception {
        GroupsList groupsList = new GroupsList();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getGroupsList(groupsList);
        spy.writeTo(groupsList, GroupsList.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_typeCredentialListTypeNullValue_runsSuccessfully() throws Exception {
        JAXBElement<ApiKeyCredentials> apiKeyCredentialsJAXBElement = new JAXBElement<ApiKeyCredentials>(QName.valueOf("fee"), ApiKeyCredentials.class, null);
        CredentialListType credentialListType = new CredentialListType();
        credentialListType.getCredential().add(apiKeyCredentialsJAXBElement);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(credentialListType, CredentialListType.class, null, null, null, null, myOut);
    }

    @Test
    public void writeTo_typeCredentialListTypeApiKeyCredentials_callsGetApiKeyCredentials() throws Exception {
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        JAXBElement<ApiKeyCredentials> apiKeyCredentialsJAXBElement = new JAXBElement<ApiKeyCredentials>(QName.valueOf("fee"), ApiKeyCredentials.class, apiKeyCredentials);
        CredentialListType credentialListType = new CredentialListType();
        credentialListType.getCredential().add(apiKeyCredentialsJAXBElement);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getApiKeyCredentials(apiKeyCredentials);
        spy.writeTo(credentialListType, CredentialListType.class, null, null, null, null, myOut);
        verify(spy).getApiKeyCredentials(apiKeyCredentials);
    }

    @Test
    public void writeTo_typeCredentialListTypePasswordCredentials_callsGetPasswordCredentials() throws Exception {
        PasswordCredentialsBase passwordCredentialsBase = new PasswordCredentialsBase();
        JAXBElement<PasswordCredentialsBase> passwordCredentialsBaseJAXBElement = new JAXBElement<PasswordCredentialsBase>(QName.valueOf("fee"), PasswordCredentialsBase.class, passwordCredentialsBase);
        CredentialListType credentialListType = new CredentialListType();
        credentialListType.getCredential().add(passwordCredentialsBaseJAXBElement);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getPasswordCredentials(passwordCredentialsBase);
        spy.writeTo(credentialListType, CredentialListType.class, null, null, null, null, myOut);
        verify(spy).getPasswordCredentials(passwordCredentialsBase);
    }

    @Test
    public void writeTo_typeCredentialListTypeEC2CredentialType_writesBlankListToOutputStream() throws Exception {
        Ec2CredentialsType ec2CredentialsType = new Ec2CredentialsType();
        JAXBElement<Ec2CredentialsType> ec2CredentialsTypeJAXBElement = new JAXBElement<Ec2CredentialsType>(QName.valueOf("fee"), Ec2CredentialsType.class, ec2CredentialsType);
        CredentialListType credentialListType = new CredentialListType();
        credentialListType.getCredential().add(ec2CredentialsTypeJAXBElement);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(credentialListType, CredentialListType.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"credentials\":[]}"));
    }

    @Test
    public void writeTo_typeRoleList_callsGetRole() throws Exception {
        Role role = new Role();
        RoleList roleList = new RoleList();
        roleList.getRole().add(role);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getRole(role);
        spy.writeTo(roleList, RoleList.class, null, null, null, null, myOut);
        verify(spy).getRole(role);
    }

    @Test
    public void writeTo_typeRoleList_writesToOutputStream() throws Exception {
        Role role = new Role();
        RoleList roleList = new RoleList();
        roleList.getRole().add(role);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getRole(role);
        spy.writeTo(roleList, RoleList.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"roles\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_typeUserList_callsGetUser() throws Exception {
        User user = new User();
        UserList userList = new UserList();
        userList.getUser().add(user);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getUser(user);
        spy.writeTo(userList, UserList.class, null, null, null, null, myOut);
        verify(spy).getUser(user);
    }

    @Test
    public void writeTo_typeUserList_writesToOutputStream() throws Exception {
        User user = new User();
        UserList userList = new UserList();
        userList.getUser().add(user);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getUser(user);
        spy.writeTo(userList, UserList.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"users\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_typeAuthenticateResponseNoUserNoAny_writesToOutputStream() throws Exception {
        Token token = new Token();
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setToken(token);
        authenticateResponse.setServiceCatalog(serviceCatalog);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        doReturn(jsonObject).when(spy).getToken(token);
        doReturn(jsonArray).when(spy).getServiceCatalog(serviceCatalog);
        spy.writeTo(authenticateResponse, ServiceCatalog.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"access\":{\"token\":{\"success\":\"This test worked!\"},\"serviceCatalog\":[{\"success\":" +
                "\"This test worked!\"}]}}"));
    }

    @Test
    public void writeTo_typeAuthenticateResponseImpersonator_writesImpersonator() throws Exception {
        Token token = new Token();

        Role role = new Role();
        role.setServiceId("123");
        role.setDescription("description");
        role.setName("name");
        role.setId("456");

        RoleList roleList = new RoleList();
        roleList.getRole().add(role);

        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setName("impersonatorName");
        userForAuthenticateResponse.setId("789");
        userForAuthenticateResponse.setRoles(roleList);

        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setToken(token);
        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<UserForAuthenticateResponse> impersonator = objectFactory.createImpersonator(userForAuthenticateResponse);
        authenticateResponse.getAny().add(impersonator);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", "This test worked!");

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);

        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(jsonObject).when(spy).getToken(token);

        spy.writeTo(authenticateResponse, ServiceCatalog.class, null, null, null, null, myOut);

        assertThat("string", myOut.toString(), equalTo("{\"access\":{\"token\":{\"success\":\"This test worked!\"},\"RAX-AUTH:impersonator\":{\"id\":\"789\",\"roles\":[{\"id\":\"456\",\"serviceId\":\"123\",\"description\":\"description\",\"name\":\"name\"}],\"name\":\"impersonatorName\"}}}"));
    }

    @Test
    public void writeTo_typeAuthenticateResponseWithUnusableAny_writesToOutputStream() throws Exception {
        Token token = new Token();
        ServiceCatalog serviceCatalog = new ServiceCatalog();

        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setToken(token);
        authenticateResponse.setServiceCatalog(serviceCatalog);
        authenticateResponse.getAny().add(new Token());
        authenticateResponse.getAny().add(new Token());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", "This test worked!");

        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);

        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(jsonObject).when(spy).getToken(token);
        doReturn(jsonArray).when(spy).getServiceCatalog(serviceCatalog);

        spy.writeTo(authenticateResponse, ServiceCatalog.class, null, null, null, null, myOut);

        assertThat("string", myOut.toString(), equalTo("{\"access\":{\"token\":{\"success\":\"This test worked!\"}," +
                "\"serviceCatalog\":[{\"success\":\"This test worked!\"}]}}"));
    }

    @Test
    public void writeTo_typeImpersonationResponse_callsGetToken() throws Exception {
        Token token = new Token();
        ImpersonationResponse impersonationResponse = new ImpersonationResponse();
        impersonationResponse.setToken(token);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getToken(token);
        spy.writeTo(impersonationResponse, ImpersonationResponse.class, null, null, null, null, myOut);
        verify(spy).getToken(token);
    }

    @Test
    public void writeTo_typeImpersonationRequest_writesToOutputStream() throws Exception {
        Token token = new Token();
        ImpersonationResponse impersonationResponse = new ImpersonationResponse();
        impersonationResponse.setToken(token);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success", "This test worked!");
        doReturn(jsonObject).when(spy).getToken(token);
        spy.writeTo(impersonationResponse, ImpersonationResponse.class, null, null, null, null, myOut);
        assertThat("string", myOut.toString(), equalTo("{\"access\":{\"token\":{\"success\":\"This test worked!\"}}}"));
    }

    @Test
    public void writeTo_typeBaseURLList_callsGetBaseURLList() throws Exception {
        BaseURL baseURL = new BaseURL();
        BaseURLList baseURLList = new BaseURLList();
        baseURLList.getBaseURL().add(baseURL);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        doReturn(new JSONObject()).when(spy).getBaseUrl(baseURL);
        spy.writeTo(baseURLList, BaseURLList.class, null, null, null, null, myOut);
        verify(spy).getBaseUrl(baseURL);
    }

    @Test
    public void writeTo_typeBaseURLList_writesToOutputStream() throws Exception {
        BaseURL baseURL = new BaseURL();
        BaseURLList baseURLList = new BaseURLList();
        baseURLList.getBaseURL().add(baseURL);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success","This test worked!");
        doReturn(jsonObject).when(spy).getBaseUrl(baseURL);
        spy.writeTo(baseURLList, BaseURLList.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"baseURLs\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_typeV1User_writesToOutputStream() throws Exception {
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(798);
        baseURLRef.setHref("101112");
        baseURLRef.setV1Default(true);

        BaseURLRefList baseURLRefList = new BaseURLRefList();
        baseURLRefList.getBaseURLRef().add(baseURLRef);

        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        user.setId("131415");
        user.setEnabled(false);
        user.setKey("key");
        user.setMossoId(123);
        user.setNastId("456");
        user.setBaseURLRefs(baseURLRefList);

        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"user\":{\"id\":\"131415\",\"enabled\":false,\"nastId\":\"456\",\"mossoId\":123,\"baseURLRefs\":[{" +
        "\"id\":798,\"v1Default\":true,\"href\":\"101112\"}],\"key\":\"key\"}}"));
    }

    @Test
    public void writeTo_v1UserNullKey_writesToOutputStreamNoKey() throws Exception {
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(798);
        baseURLRef.setHref("101112");
        baseURLRef.setV1Default(true);

        BaseURLRefList baseURLRefList = new BaseURLRefList();
        baseURLRefList.getBaseURLRef().add(baseURLRef);

        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        user.setId("131415");
        user.setEnabled(false);
        user.setMossoId(123);
        user.setNastId("456");
        user.setBaseURLRefs(baseURLRefList);

        ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        writer.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"user\":{\"id\":\"131415\",\"enabled\":false,\"nastId\":\"456\",\"mossoId\":123,\"baseURLRefs\":[{" +
                "\"id\":798,\"v1Default\":true,\"href\":\"101112\"}]}}"));
    }

    @Test
    public void writeTo_typeV1UserNullMossoId_writesToOutputStreamNoMossoId() throws Exception {
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(798);
        baseURLRef.setHref("101112");
        baseURLRef.setV1Default(true);

        BaseURLRefList baseURLRefList = new BaseURLRefList();
        baseURLRefList.getBaseURLRef().add(baseURLRef);

        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        user.setId("131415");
        user.setKey("key");
        user.setEnabled(false);
        user.setNastId("456");
        user.setBaseURLRefs(baseURLRefList);

        ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        writer.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"user\":{\"id\":\"131415\",\"enabled\":false,\"nastId\":\"456\",\"baseURLRefs\":[{" +
                "\"id\":798,\"v1Default\":true,\"href\":\"101112\"}],\"key\":\"key\"}}"));
    }

    @Test
    public void writeTo_typeV1UserNullNastID_writesToOutputStreamNoNastID() throws Exception {
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(798);
        baseURLRef.setHref("101112");
        baseURLRef.setV1Default(true);

        BaseURLRefList baseURLRefList = new BaseURLRefList();
        baseURLRefList.getBaseURLRef().add(baseURLRef);

        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        user.setId("131415");
        user.setEnabled(false);
        user.setKey("key");
        user.setMossoId(123);
        user.setBaseURLRefs(baseURLRefList);

        ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        writer.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"user\":{\"id\":\"131415\",\"enabled\":false,\"mossoId\":123,\"baseURLRefs\":[{" +
                "\"id\":798,\"v1Default\":true,\"href\":\"101112\"}],\"key\":\"key\"}}"));
    }

    @Test
    public void writeTo_v1User_writesToOutputStreamNoBasURLRefList() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        user.setId("131415");
        user.setEnabled(false);
        user.setKey("key");
        user.setMossoId(123);
        user.setNastId("456");

        ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        writer.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"user\":{\"id\":\"131415\",\"enabled\":false,\"nastId\":\"456\",\"mossoId\":123,\"baseURLRefs\":[]," +
                "\"key\":\"key\"}}"));
    }

    @Test
    public void writeTo_v20user_callsGetUser() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        final User user = new User();
        doReturn(new JSONObject()).when(spy).getUser(user);
        spy.writeTo(user, null, null, null, null, null, myOut);
        verify(spy).getUser(user);
    }

    @Test
    public void writeTo_v20user_writerToOutputStream() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        final User user = new User();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        doReturn(jsonObject).when(spy).getUser(user);
        spy.writeTo(user, null, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"user\":{\"success\":\"This test worked!\"}}"));
    }

    @Test
    public void writeTo_notCaughtByIfStatement_usesMarshaller() throws Exception {
        Token token = new Token();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONMarshaller marshaller = mock(JSONMarshaller.class);
        doReturn(marshaller).when(spy).getMarshaller();

        spy.writeTo(token, null, null, null, null, null, myOut);
        verify(marshaller).marshallToJSON(token,myOut);
    }

    @Test
    public void writeTo_notCaughtByIfStatement_correctJson() throws Exception {
        PasswordCredentialsBase passwordCredentialsBase = new PasswordCredentialsBase();
        passwordCredentialsBase.setUsername("jsmith");
        JAXBElement<PasswordCredentialsBase> passwordCredentialsBaseJAXBElement = new JAXBElement<PasswordCredentialsBase>(QName.valueOf("passwordCredentials"), PasswordCredentialsBase.class, passwordCredentialsBase);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(passwordCredentialsBaseJAXBElement, null, null, null, null, null, myOut);
        assertThat("correct string", myOut.toString(), equalTo("{\"passwordCredentials\":{\"username\":\"jsmith\"}}"));

    }

    @Test (expected = BadRequestException.class)
    public void writeTo_marsahllerFails_throwsBadRequestException() throws Exception {
        Token token = new Token();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONMarshaller marshaller = mock(JSONMarshaller.class);
        doReturn(marshaller).when(spy).getMarshaller();
        doThrow(new JAXBException("bad")).when(marshaller).marshallToJSON(token,myOut);

        spy.writeTo(token, null, null, null, null, null, myOut);
    }

    @Test
    public void getLinks_ListCorrectlyPopulated_returnsJSONArrayWithCorrectValues() throws Exception {
        Relation relation = Relation.AUTHOR;
        Link link = new Link();
        link.setRel(relation);
        link.setType("type");
        link.setHref("href");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        List<Object> list = new ArrayList<Object>();
        list.add(jaxbElement);
        JSONArray jsonArray = JSONWriter.getLinks(list);
        String jsonText = JSONValue.toJSONString(jsonArray);
        myOut.write(jsonText.getBytes());

        assertThat("string",myOut.toString(), equalTo("[{\"rel\":\"author\",\"type\":\"type\",\"href\":\"href\"}]"));
    }

    @Test
    public void getLinks_ListCorrectlyPopulatedNoRelation_returnsJSONArrayWithoutRelation() throws Exception {
        Link link = new Link();
        link.setType("type");
        link.setHref("href");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        List<Object> list = new ArrayList<Object>();
        list.add(jaxbElement);
        JSONArray jsonArray = JSONWriter.getLinks(list);

        String jsonText = JSONValue.toJSONString(jsonArray);
        myOut.write(jsonText.getBytes());

        assertThat("string",myOut.toString(), equalTo("[{\"type\":\"type\",\"href\":\"href\"}]"));
    }

    @Test
    public void getLinks_ListCorrectlyPopulatedNoType_returnsJSONArrayWithoutType() throws Exception {
        Relation relation = Relation.AUTHOR;
        Link link = new Link();
        link.setRel(relation);
        link.setHref("href");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        List<Object> list = new ArrayList<Object>();
        list.add(jaxbElement);
        JSONArray jsonArray = JSONWriter.getLinks(list);

        String jsonText = JSONValue.toJSONString(jsonArray);
        myOut.write(jsonText.getBytes());

        assertThat("string",myOut.toString(), equalTo("[{\"rel\":\"author\",\"href\":\"href\"}]"));
    }

    @Test
    public void getLinks_ListCorrectlyPopulatedNoHRef_returnsJSONArrayWithCorrectValues() throws Exception {
        Relation relation = Relation.AUTHOR;
        Link link = new Link();
        link.setRel(relation);
        link.setType("type");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        List<Object> list = new ArrayList<Object>();
        list.add(jaxbElement);
        JSONArray jsonArray = JSONWriter.getLinks(list);
        String jsonText = JSONValue.toJSONString(jsonArray);
        myOut.write(jsonText.getBytes());

        assertThat("string",myOut.toString(), equalTo("[{\"rel\":\"author\",\"type\":\"type\"}]"));
    }

    @Test
    public void getLinks_ListNoLink_returnsEmptyJSONArray() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<AuthenticateResponse> jaxbElement = new JAXBElement<AuthenticateResponse>(QName.valueOf("foo"),AuthenticateResponse.class,new AuthenticateResponse());
        List<Object> list = new ArrayList<Object>();
        list.add(jaxbElement);
        JSONArray jsonArray = JSONWriter.getLinks(list);

        String jsonText = JSONValue.toJSONString(jsonArray);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("[]"));
    }

    @Test
    public void getLinks_listNotJAXBElements_returnsEmptyJSONArray() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        List<Object> list = new ArrayList<Object>();
        list.add(new Token());
        list.add(new Token());
        list.add(new Token());
        JSONArray jsonArray = JSONWriter.getLinks(list);

        String jsonText = JSONValue.toJSONString(jsonArray);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("[]"));
    }

    @Test
    public void getLinks_listEmpty_returnsEmptyJSONArray() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        List<Object> list = new ArrayList<Object>();
        JSONArray jsonArray = JSONWriter.getLinks(list);

        String jsonText = JSONValue.toJSONString(jsonArray);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("[]"));
    }

    @Test
    public void getVersionChoice_allFieldsPopulated_returnsFullJSONObject() throws Exception {
        Link link = new Link();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        MediaTypeList mediaTypeList = new MediaTypeList();
        mediaTypeList.getMediaType().add(new MediaType());
        VersionStatus versionStatus = VersionStatus.ALPHA;
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");

        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setStatus(versionStatus);
        versionChoice.setUpdated(calendar1);
        versionChoice.getAny().add(jaxbElement);
        versionChoice.setMediaTypes(mediaTypeList);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        JSONObject jsonObject = writer.getVersionChoice(versionChoice);
        String jsonText = JSONValue.toJSONString(jsonObject);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("{\"version\":{\"id\":null,\"updated\":\"2012-03-12T19:23:45\",\"status\":\"ALPHA\",\"links\":[{}]," +
            "\"media-types\":{\"values\":[{\"base\":\"\",\"type\":null}]}}}"));
    }

    @Test
    public void getVersionChoice_noStatus_returnsJSONObjectNoStatus() throws Exception {
        Link link = new Link();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        MediaTypeList mediaTypeList = new MediaTypeList();
        mediaTypeList.getMediaType().add(new MediaType());
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");

        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setUpdated(calendar1);
        versionChoice.getAny().add(jaxbElement);
        versionChoice.setMediaTypes(mediaTypeList);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        JSONObject jsonObject = writer.getVersionChoice(versionChoice);
        String jsonText = JSONValue.toJSONString(jsonObject);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("{\"version\":{\"id\":null,\"updated\":\"2012-03-12T19:23:45\",\"links\":[{}]," +
                "\"media-types\":{\"values\":[{\"base\":\"\",\"type\":null}]}}}"));
    }

    @Test
    public void getVersionChoice_noUpdated_returnsJSONObjectNoUpdatedField() throws Exception {
        Link link = new Link();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        MediaTypeList mediaTypeList = new MediaTypeList();
        mediaTypeList.getMediaType().add(new MediaType());
        VersionStatus versionStatus = VersionStatus.ALPHA;

        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setStatus(versionStatus);
        versionChoice.getAny().add(jaxbElement);
        versionChoice.setMediaTypes(mediaTypeList);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        JSONObject jsonObject = writer.getVersionChoice(versionChoice);
        String jsonText = JSONValue.toJSONString(jsonObject);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("{\"version\":{\"id\":null,\"status\":\"ALPHA\",\"links\":[{}]," +
                "\"media-types\":{\"values\":[{\"base\":\"\",\"type\":null}]}}}"));
    }

    @Test
    public void getVersionChoice_noLinks_returnsJSONObjectNoLinks() throws Exception {
        MediaTypeList mediaTypeList = new MediaTypeList();
        mediaTypeList.getMediaType().add(new MediaType());
        VersionStatus versionStatus = VersionStatus.ALPHA;
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");

        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setStatus(versionStatus);
        versionChoice.setUpdated(calendar1);
        versionChoice.getAny().add(new Link());
        versionChoice.setMediaTypes(mediaTypeList);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        JSONObject jsonObject = writer.getVersionChoice(versionChoice);
        String jsonText = JSONValue.toJSONString(jsonObject);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("{\"version\":{\"id\":null,\"updated\":\"2012-03-12T19:23:45\",\"status\":\"ALPHA\"," +
                "\"media-types\":{\"values\":[{\"base\":\"\",\"type\":null}]}}}"));
    }

    @Test
    public void getVersionChoice_anyFieldEmpty_returnsJSONObjectNoLinks() throws Exception {
        MediaTypeList mediaTypeList = new MediaTypeList();
        mediaTypeList.getMediaType().add(new MediaType());
        VersionStatus versionStatus = VersionStatus.ALPHA;
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");

        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setStatus(versionStatus);
        versionChoice.setUpdated(calendar1);
        versionChoice.setMediaTypes(mediaTypeList);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        JSONObject jsonObject = writer.getVersionChoice(versionChoice);
        String jsonText = JSONValue.toJSONString(jsonObject);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("{\"version\":{\"id\":null,\"updated\":\"2012-03-12T19:23:45\",\"status\":\"ALPHA\"," +
                "\"media-types\":{\"values\":[{\"base\":\"\",\"type\":null}]}}}"));
    }

    @Test
    public void getVersionChoice_MediaListTypeNull_JSONObjectNoMediaTypes() throws Exception {
        Link link = new Link();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        VersionStatus versionStatus = VersionStatus.ALPHA;
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");

        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setStatus(versionStatus);
        versionChoice.setUpdated(calendar1);
        versionChoice.getAny().add(jaxbElement);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        JSONObject jsonObject = writer.getVersionChoice(versionChoice);
        String jsonText = JSONValue.toJSONString(jsonObject);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("{\"version\":{\"id\":null,\"updated\":\"2012-03-12T19:23:45\",\"status\":\"ALPHA\",\"links\":[{}]}}"));
    }

    @Test
    public void getVersionChoice_MediaTypeListEmpty_returnsJSONObjectNoMediaTypes() throws Exception {
        Link link = new Link();
        JAXBElement<Link> jaxbElement = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link);
        MediaTypeList mediaTypeList = new MediaTypeList();
        VersionStatus versionStatus = VersionStatus.ALPHA;
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");
        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setStatus(versionStatus);
        versionChoice.setUpdated(calendar1);
        versionChoice.getAny().add(jaxbElement);
        versionChoice.setMediaTypes(mediaTypeList);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        JSONObject jsonObject = writer.getVersionChoice(versionChoice);
        String jsonText = JSONValue.toJSONString(jsonObject);
        myOut.write(jsonText.getBytes());
        assertThat("string",myOut.toString(), equalTo("{\"version\":{\"id\":null,\"updated\":\"2012-03-12T19:23:45\",\"status\":\"ALPHA\",\"links\":[{}]}}"));
    }

    @Test
    public void getTokenUser_allFieldsPopulated_returnsFullJSONObject() throws Exception {
        Role role1 = new Role();
        Role role2 = new Role();
        RoleList roleList = new RoleList();
        roleList.getRole().add(role1);
        roleList.getRole().add(role2);

        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setName("John Smith");
        userForAuthenticateResponse.setRoles(roleList);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        doReturn(jsonObject).when(spy).getRole(role1);
        doReturn(jsonObject).when(spy).getRole(role2);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getTokenUser(userForAuthenticateResponse);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"roles\":[{\"success\":\"This test worked!\"},{\"success\":\"This test worked!\"}],\"name\":\"John Smith\",\"RAX-AUTH:defaultRegion\":\"\"}"));
    }

    @Test
    public void getTokenUser_nameNull_returnsJSONObjectNoName() throws Exception {
        Role role1 = new Role();
        Role role2 = new Role();
        RoleList roleList = new RoleList();
        roleList.getRole().add(role1);
        roleList.getRole().add(role2);

        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setRoles(roleList);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        doReturn(jsonObject).when(spy).getRole(role1);
        doReturn(jsonObject).when(spy).getRole(role2);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getTokenUser(userForAuthenticateResponse);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"roles\":[{\"success\":\"This test worked!\"},{\"success\":\"This test worked!\"}],\"RAX-AUTH:defaultRegion\":\"\"}"));
    }

    @Test
    public void getTokenUser_emptyRoleList_returnsJSONObjectNoRoles() throws Exception {
        RoleList roleList = new RoleList();

        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setName("John Smith");
        userForAuthenticateResponse.setRoles(roleList);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getTokenUser(userForAuthenticateResponse);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"roles\":[],\"name\":\"John Smith\",\"RAX-AUTH:defaultRegion\":\"\"}"));
    }

    @Test
    public void getTokenUser_nullRoleList_returnsJSONObjectNoRoles() throws Exception {
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setName("John Smith");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getTokenUser(userForAuthenticateResponse);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"roles\":[],\"name\":\"John Smith\",\"RAX-AUTH:defaultRegion\":\"\"}"));
    }

    @Test
    public void getTenantWithoutWrapper_fullyPopulated_returnsFullJSONObject() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName("John Smith");
        tenant.setDescription("this is a description");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getTenantWithoutWrapper(tenant);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"enabled\":true,\"description\":\"this is a description\",\"name\":\"John Smith\"}"));
    }

    @Test
    public void getTenantWithoutWrapper_nullName_returnsJSONObjectNoName() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setDescription("this is a description");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getTenantWithoutWrapper(tenant);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"enabled\":true,\"description\":\"this is a description\"}"));
    }

    @Test
    public void getTenantWithoutWrapper_nullDescription_returnsJSONObjectNoDescription() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName("John Smith");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getTenantWithoutWrapper(tenant);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"enabled\":true,\"name\":\"John Smith\"}"));
    }

    @Test
    public void getServiceCatalog_fullyPopulated_returnsFullJSONObject() throws Exception {
        ServiceForCatalog serviceForCatalog = mock(ServiceForCatalog.class);
        when(serviceForCatalog.getName()).thenReturn("John Smith");
        when(serviceForCatalog.getType()).thenReturn("type");
        when(serviceForCatalog.getEndpoint()).thenReturn(null);
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        serviceCatalog.getService().add(serviceForCatalog);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        doReturn(jsonArray).when(spy).getEndpointsForCatalog(null);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = spy.getServiceCatalog(serviceCatalog);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"name\":\"John Smith\",\"endpoints\":[{\"success\":\"This test worked!\"}],\"type\":\"type\"}]"));
    }

    @Test
    public void getServiceCatalog_NullName_returnsJSONObjectNoName() throws Exception {
        ServiceForCatalog serviceForCatalog = mock(ServiceForCatalog.class);
        when(serviceForCatalog.getType()).thenReturn("type");
        when(serviceForCatalog.getEndpoint()).thenReturn(null);
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        serviceCatalog.getService().add(serviceForCatalog);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        doReturn(jsonArray).when(spy).getEndpointsForCatalog(null);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = spy.getServiceCatalog(serviceCatalog);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"endpoints\":[{\"success\":\"This test worked!\"}],\"type\":\"type\"}]"));
    }

    @Test
    public void getServiceCatalog_NullType_returnsJSONObjectNoType() throws Exception {
        ServiceForCatalog serviceForCatalog = mock(ServiceForCatalog.class);
        when(serviceForCatalog.getName()).thenReturn("John Smith");
        when(serviceForCatalog.getEndpoint()).thenReturn(null);
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        serviceCatalog.getService().add(serviceForCatalog);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        doReturn(jsonArray).when(spy).getEndpointsForCatalog(null);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = spy.getServiceCatalog(serviceCatalog);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"name\":\"John Smith\",\"endpoints\":[{\"success\":\"This test worked!\"}]}]"));
    }

    @Test
    public void getServiceCatalog_nullServiceCatalog_returnsEmptyJSONArray() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getServiceCatalog(null);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[]"));
    }

    @Test
    public void getServiceCatalog11_fullyPopulated_returnsFullJSONObject() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.ServiceCatalog serviceCatalog = new com.rackspacecloud.docs.auth.api.v1.ServiceCatalog();
        com.rackspacecloud.docs.auth.api.v1.Service service = new com.rackspacecloud.docs.auth.api.v1.Service();
        com.rackspacecloud.docs.auth.api.v1.Endpoint endpoint = new com.rackspacecloud.docs.auth.api.v1.Endpoint();
        endpoint.setV1Default(true);
        endpoint.setAdminURL("http://adminUrl");
        endpoint.setInternalURL("http://internal");
        service.getEndpoint().add(endpoint);
        service.setName("cloudFiles");
        serviceCatalog.getService().add(service);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getServiceCatalog11(serviceCatalog);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"cloudFiles\":[{\"publicURL\":null,\"v1Default\":true,\"internalURL\":\"http:\\/\\/internal\"}]}"));
    }

    @Test (expected = BadRequestException.class)
    public void getToken_nullExpired_throwsBadRequestException() throws Exception {
        Token token = new Token();
        writer.getToken(token);
    }

    @Test (expected = BadRequestException.class)
    public void getToken_nullToken_throwsBadRequestException() throws Exception {
        writer.getToken(null);
    }

    @Test
    public void getToken_nullTenant_returnsJSONObjectNoTenant() throws Exception {
        Token token = new Token();
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");
        token.setExpires(calendar1);
        token.setId("123");
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getToken(token);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString().startsWith("{\"id\":\"123\",\"expires\":\"2012-03-12T19:23:45"), equalTo(true));
        assertThat("string", myOut.toString().endsWith("\"}"), equalTo(true));

    }

    @Test
    public void getTenants_twoTenantsInList_returnsJSONObject() throws Exception {
        List<TenantForAuthenticateResponse> list = new ArrayList<TenantForAuthenticateResponse>();
        list.add(new TenantForAuthenticateResponse());
        list.add(new TenantForAuthenticateResponse());
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getTenants(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"id\":null,\"name\":null},{\"id\":null,\"name\":null}]"));
    }

    @Test
    public void getTenants_emptyList_returnsEmptyJSONObject() throws Exception {
        List<TenantForAuthenticateResponse> list = new ArrayList<TenantForAuthenticateResponse>();
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getTenants(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[]"));
    }

    @Test
    public void getEndpointsForCatalog_fullyPopulated_returnsJSONObjectAllFields() throws Exception {
        VersionForService versionForService = new VersionForService();
        EndpointForService endpointForService = new EndpointForService();
        endpointForService.setTenantId("123");
        endpointForService.setPublicURL("www.publicURL.com");
        endpointForService.setInternalURL("www.internalURL.com");
        endpointForService.setRegion("USA");
        endpointForService.setVersion(versionForService);
        List<EndpointForService> list = new ArrayList<EndpointForService>();
        list.add(endpointForService);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"region\":\"USA\",\"tenantId\":\"123\",\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"versionId\":null,\"internalURL\":\"www.internalURL.com\"}]"));

    }

    @Test
    public void getEndpointsForCatalog11_fullyPopulated_returnsJSONObjectAllFields() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.Endpoint endpoint = new com.rackspacecloud.docs.auth.api.v1.Endpoint();
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setRegion("USA");
        endpoint.setV1Default(true);
        List<com.rackspacecloud.docs.auth.api.v1.Endpoint> list = new ArrayList<com.rackspacecloud.docs.auth.api.v1.Endpoint>();
        list.add(endpoint);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog11(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"region\":\"USA\",\"publicURL\":\"www.publicURL.com\",\"v1Default\":true,\"internalURL\":\"www.internalURL.com\"}]"));

    }

    @Test
    public void getEndpointsForCatalog11_noV1Default_returnsJSONObject() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.Endpoint endpoint = new com.rackspacecloud.docs.auth.api.v1.Endpoint();
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setRegion("USA");
        endpoint.setV1Default(false);
        List<com.rackspacecloud.docs.auth.api.v1.Endpoint> list = new ArrayList<com.rackspacecloud.docs.auth.api.v1.Endpoint>();
        list.add(endpoint);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog11(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"region\":\"USA\",\"publicURL\":\"www.publicURL.com\",\"internalURL\":\"www.internalURL.com\"}]"));

    }

    @Test
    public void getEndpointsForCatalog_nullTenantID_returnsJSONObjectNoTenantID() throws Exception {
        VersionForService versionForService = new VersionForService();
        EndpointForService endpointForService = new EndpointForService();
        endpointForService.setPublicURL("www.publicURL.com");
        endpointForService.setInternalURL("www.internalURL.com");
        endpointForService.setRegion("USA");
        endpointForService.setVersion(versionForService);
        List<EndpointForService> list = new ArrayList<EndpointForService>();
        list.add(endpointForService);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"region\":\"USA\",\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"versionId\":null,\"internalURL\":\"www.internalURL.com\"}]"));

    }

    @Test
    public void getEndpointsForCatalog_nullPublicURL_returnsJSONObjectNullPublicURL() throws Exception {
        VersionForService versionForService = new VersionForService();
        EndpointForService endpointForService = new EndpointForService();
        endpointForService.setTenantId("123");
        endpointForService.setInternalURL("www.internalURL.com");
        endpointForService.setRegion("USA");
        endpointForService.setVersion(versionForService);
        List<EndpointForService> list = new ArrayList<EndpointForService>();
        list.add(endpointForService);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"region\":\"USA\",\"tenantId\":\"123\",\"versionInfo\":null," +
                "\"versionList\":null,\"versionId\":null,\"internalURL\":\"www.internalURL.com\"}]"));

    }

    @Test
    public void getEndpointsForCatalog_nullInternalURL_returnsJSONObjectNoInternalURL() throws Exception {
        VersionForService versionForService = new VersionForService();
        EndpointForService endpointForService = new EndpointForService();
        endpointForService.setTenantId("123");
        endpointForService.setPublicURL("www.publicURL.com");
        endpointForService.setRegion("USA");
        endpointForService.setVersion(versionForService);
        List<EndpointForService> list = new ArrayList<EndpointForService>();
        list.add(endpointForService);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"region\":\"USA\",\"tenantId\":\"123\",\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"versionId\":null}]"));

    }

    @Test
    public void getEndpointsForCatalog_nullRegion_returnsJSONObjectNoRegion() throws Exception {
        VersionForService versionForService = new VersionForService();
        EndpointForService endpointForService = new EndpointForService();
        endpointForService.setTenantId("123");
        endpointForService.setPublicURL("www.publicURL.com");
        endpointForService.setInternalURL("www.internalURL.com");
        endpointForService.setVersion(versionForService);
        List<EndpointForService> list = new ArrayList<EndpointForService>();
        list.add(endpointForService);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"tenantId\":\"123\",\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"versionId\":null,\"internalURL\":\"www.internalURL.com\"}]"));

    }

    @Test
    public void getEndpointsForCatalog_nullVersion_returnsJSONObjectNoVersionFields() throws Exception {
        EndpointForService endpointForService = new EndpointForService();
        endpointForService.setTenantId("123");
        endpointForService.setPublicURL("www.publicURL.com");
        endpointForService.setInternalURL("www.internalURL.com");
        endpointForService.setRegion("USA");
        List<EndpointForService> list = new ArrayList<EndpointForService>();
        list.add(endpointForService);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[{\"region\":\"USA\",\"tenantId\":\"123\",\"publicURL\":\"www.publicURL.com\"," +
                "\"internalURL\":\"www.internalURL.com\"}]"));

    }

    @Test
    public void getEndpointsForCatalog_emptyList_returnsEmptyJSONObject() throws Exception {
        List<EndpointForService> list = new ArrayList<EndpointForService>();
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONArray result = writer.getEndpointsForCatalog(list);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("[]"));

    }

    @Test
    public void getSecretQA_returnsJSONObject() throws Exception {
        SecretQA secretQA = new SecretQA();
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getSecretQA(secretQA);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"RAX-KSQA:secretQA\":{\"answer\":null,\"question\":null}}"));
    }

    @Test
    public void getUser_instanceOfUserForCreate_returnsJSONObjectWithPassword() throws Exception {
        User user = new UserForCreate();
        user.setId("10019805");
        user.setUsername("kurt");
        user.setEmail("myEmail");
        ((UserForCreate)user).setPassword("myPassword");
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");
        user.setCreated(calendar1);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getUser(user);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("user", myOut.toString().startsWith("{\"id\":\"10019805\",\"enabled\":true,\"username\":\"kurt\",\"OS-KSADM:password\":\"myPassword\",\"created\":\"2012-03-12T19:23:45"), equalTo(true));
        assertThat("user", myOut.toString().endsWith("\",\"email\":\"myEmail\"}"),equalTo(true));
    }

    @Test
    public void getUser_instanceOfUserForCreate_withOutPassword_returnsJSONObjectWithOutPassword() throws Exception {
        User user = new UserForCreate();
        user.setId("10019805");
        user.setUsername("kurt");
        user.setEmail("myEmail");
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");
        user.setCreated(calendar1);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getUser(user);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("user", myOut.toString().startsWith("{\"id\":\"10019805\",\"enabled\":true,\"username\":\"kurt\",\"created\":\"2012-03-12T19:23:45"), equalTo(true));
        assertThat("user", myOut.toString().endsWith("\",\"email\":\"myEmail\"}"),equalTo(true));
    }

    @Test
    public void getUser_fullyPopulated_returnsJSONObjectWithDefaultRegion() throws Exception {
        final User user = new User();
        user.setId("10019805");
        user.setUsername("kurt");
        user.setEmail("myEmail");
        DatatypeFactory f = DatatypeFactory.newInstance();
        XMLGregorianCalendar calendar1 = f.newXMLGregorianCalendar("2012-03-12T19:23:45");
        user.setCreated(calendar1);
        user.setUpdated(calendar1);
        user.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), "myRegion");
        user.getOtherAttributes().put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), "myPassword");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getUser(user);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("user", myOut.toString().startsWith("{\"id\":\"10019805\",\"enabled\":true," +"\"username\":\"kurt\",\"OS-KSADM:password\":\"myPassword\",\"updated\":\"2012-03-12T19:23:45"), equalTo(true));
        assertThat("user", myOut.toString().endsWith("\",\"email\":\"myEmail\",\"RAX-AUTH:defaultRegion\":\"myRegion\"}"), equalTo(true));
    }

    @Test
    public void getUser_NoDefaultRegion_returnsJSONObject() throws Exception {
        final User user = new User();
        user.setId("10019805");
        user.setUsername("kurt");
        user.setEmail("myEmail");
        user.setCreated(calendar);
        user.setUpdated(calendar);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getUser(user);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("user", myOut.toString().startsWith("{\"id\":\"10019805\",\"enabled\":true,\"username\":\"kurt\",\"updated\":\"2012-01-01"), equalTo(true));
        assertThat("user", myOut.toString().contains("\",\"created\":\"2012-01-01"), equalTo(true));
        assertThat("user", myOut.toString().endsWith("\"email\":\"myEmail\"}"), equalTo(true));
    }

    @Test
    public void getUser_EmptyDefaultRegion_returnsJSONObject() throws Exception {
        final User user = new User();
        user.setId("10019805");
        user.setUsername("kurt");
        user.setEmail("myEmail");
        user.setCreated(calendar);
        user.setUpdated(calendar);
        user.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), "");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getUser(user);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("user", myOut.toString().startsWith("{\"id\":\"10019805\",\"enabled\":true,\"username\":\"kurt\",\"updated\":\"2012-01-01"), equalTo(true));
        assertThat("user", myOut.toString().contains("\",\"created\":\"2012-01-01"), equalTo(true));
        assertThat("user", myOut.toString().endsWith("\"email\":\"myEmail\",\"RAX-AUTH:defaultRegion\":\"\"}"), equalTo(true));
    }

    @Test
    public void getUser_nullCreated_returnsJSONObjectWithDefaultRegion() throws Exception {
        final User user = new User();
        user.setId("10019805");
        user.setUsername("kurt");
        user.setEmail("myEmail");
        user.setUpdated(calendar);
        user.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), "myRegion");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getUser(user);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("user", myOut.toString().startsWith("{\"id\":\"10019805\",\"enabled\":true," +"\"username\":\"kurt\",\"updated\":\"2012-01-01"), equalTo(true));
        assertThat("user", myOut.toString().endsWith("\",\"email\":\"myEmail\",\"RAX-AUTH:defaultRegion\":\"myRegion\"}"), equalTo(true));
    }

    @Test
    public void getUser_nullUpdated_returnsJSONObjectWithDefaultRegion() throws Exception {
        final User user = new User();
        user.setId("10019805");
        user.setUsername("kurt");
        user.setEmail("myEmail");
        user.setCreated(calendar);
        user.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), "myRegion");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getUser(user);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("user", myOut.toString().startsWith("{\"id\":\"10019805\",\"enabled\":true," +"\"username\":\"kurt\",\"created\":\"2012-01-01"), equalTo(true));
        assertThat("user", myOut.toString().endsWith("\",\"email\":\"myEmail\",\"RAX-AUTH:defaultRegion\":\"myRegion\"}"), equalTo(true));
    }

    @Test
    public void getRole_allFieldsPopulated_returnsJSONObject() throws Exception {
        Role role = new Role();
        role.setId("123");
        role.setDescription("this is a description");
        role.setName("John Smith");
        role.setServiceId("456");
        role.setTenantId("789");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getRole(role);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"789\",\"id\":\"123\",\"serviceId\":\"456\",\"description\":\"this is a description\"," +
                "\"name\":\"John Smith\"}"));
    }

    @Test
    public void getRole_nullId_returnsJSONObjectNoId() throws Exception {
        Role role = new Role();
        role.setDescription("this is a description");
        role.setName("John Smith");
        role.setServiceId("456");
        role.setTenantId("789");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getRole(role);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"789\",\"serviceId\":\"456\",\"description\":\"this is a description\"," +
                "\"name\":\"John Smith\"}"));
    }

    @Test
    public void getRole_nullDescription_returnsJSONObjectNoDescription() throws Exception {
        Role role = new Role();
        role.setId("123");
        role.setName("John Smith");
        role.setServiceId("456");
        role.setTenantId("789");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getRole(role);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"789\",\"id\":\"123\",\"serviceId\":\"456\",\"name\":\"John Smith\"}"));
    }

    @Test
    public void getRole_nullName_returnsJSONObjectNoName() throws Exception {
        Role role = new Role();
        role.setId("123");
        role.setDescription("this is a description");
        role.setServiceId("456");
        role.setTenantId("789");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getRole(role);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"789\",\"id\":\"123\",\"serviceId\":\"456\",\"description\":\"this is a description\"}"));
    }

    @Test
    public void getRole_nullServiceId_returnsJSONObjectNoServiceId() throws Exception {
        Role role = new Role();
        role.setId("123");
        role.setDescription("this is a description");
        role.setName("John Smith");
        role.setTenantId("789");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getRole(role);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"789\",\"id\":\"123\",\"description\":\"this is a description\"," +
                "\"name\":\"John Smith\"}"));
    }

    @Test
    public void getRole_nullTenantId_returnsJSONObjectNoTenantId() throws Exception {
        Role role = new Role();
        role.setId("123");
        role.setDescription("this is a description");
        role.setName("John Smith");
        role.setServiceId("456");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getRole(role);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":\"123\",\"serviceId\":\"456\",\"description\":\"this is a description\"," +
                "\"name\":\"John Smith\"}"));
    }

    @Test
    public void getGroups_twoGroups_addsGroupsToArray() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", "This test worked!");
        Group group1 = new Group();
        Group group2 = new Group();
        Groups groups = new Groups();
        groups.getGroup().add(group1);
        groups.getGroup().add(group2);

        doReturn(jsonObject).when(spy).getGroupWithoutWrapper(group1);
        doReturn(jsonObject).when(spy).getGroupWithoutWrapper(group2);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getGroups(groups);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"RAX-KSGRP:groups\":[{\"success\":\"This test worked!\"},{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void getGroupsList_fullyPopulated_returnsJSONObject() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.Group group1 = new com.rackspacecloud.docs.auth.api.v1.Group();
        com.rackspacecloud.docs.auth.api.v1.Group group2 = new com.rackspacecloud.docs.auth.api.v1.Group();
        GroupsList groupsList = new GroupsList();
        groupsList.getGroup().add(group1);
        groupsList.getGroup().add(group2);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        doReturn(jsonObject).when(spy).get11Group(group1);
        doReturn(jsonObject).when(spy).get11Group(group2);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getGroupsList(groupsList);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"groups\":{\"values\":[{\"success\":\"This test worked!\"},{\"success\":\"This test worked!\"}]}}"));
    }

    @Test
    public void getGroupsList_emptyList_returnsJSONObject() throws Exception {
        GroupsList groupsList = new GroupsList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getGroupsList(groupsList);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"groups\":{\"values\":[]}}"));
    }

    @Test
    public void getGroup_callsGetGroupWithoutWrapper() throws Exception {
        Group group = new Group();
        doReturn(new JSONObject()).when(spy).getGroupWithoutWrapper(group);
        spy.getGroup(group);
        verify(spy).getGroupWithoutWrapper(group);
    }

    @Test
    public void getGroup_returnsJSONObject() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        Group group = new Group();
        doReturn(jsonObject).when(spy).getGroupWithoutWrapper(group);
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getGroup(group);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"RAX-KSGRP:group\":{\"success\":\"This test worked!\"}}"));

    }

    @Test
    public void getGroupWithoutWrapper_withDescription_returnsJSONObject() throws Exception {
        Group group = new Group();
        group.setDescription("this is a description");
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getGroupWithoutWrapper(group);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"description\":\"this is a description\",\"name\":null}"));
    }

    @Test
    public void getGroupWithoutWrapper_withoutDescription_returnsJSONObjectNoDescription() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getGroupWithoutWrapper(new Group());
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"name\":null}"));
    }

    @Test
    public void get11Group_withDescription_returnsJSONObject() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.Group group = new com.rackspacecloud.docs.auth.api.v1.Group();
        group.setDescription("this is a description");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.get11Group(group);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"description\":\"this is a description\"}"));
    }

    @Test
    public void get11Group_withoutDescription_returnsJSONObject() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.get11Group(new com.rackspacecloud.docs.auth.api.v1.Group());
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null}"));
    }

    @Test
    public void getServiceWithoutWrapper_nullDescription_returnsJSONObject() throws Exception {
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getServiceWithoutWrapper(new Service());
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":null,\"name\":null,\"type\":null}"));
    }

    @Test
    public void getServiceList_listPopulated_callsGetServiceWithoutWrapper() throws Exception {
        Service service1 = new Service();
        Service service2 = new Service();
        ServiceList serviceList = new ServiceList();
        serviceList.getService().add(service1);
        serviceList.getService().add(service2);

        doReturn(new JSONObject()).when(spy).getServiceWithoutWrapper(service1);
        doReturn(new JSONObject()).when(spy).getServiceWithoutWrapper(service2);

        spy.getServiceList(serviceList);
        verify(spy).getServiceWithoutWrapper(service1);
        verify(spy).getServiceWithoutWrapper(service2);
    }

    @Test
    public void getServiceList_emptyList_returnsJSONObject() throws Exception {

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getServiceList(new ServiceList());
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"OS-KSADM:services\":[]}"));
    }

    @Test
    public void getEndpointTemplateWithoutWrapper_nullAdminURL_returnJSONObject() throws Exception {
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setInternalURL("www.internalURL.com");
        endpointTemplate.setName("John Smith");
        endpointTemplate.setPublicURL("www.publicURL.com");
        endpointTemplate.setType("myType");
        endpointTemplate.setRegion("USA");
        endpointTemplate.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateWithoutWrapper(endpointTemplate);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"region\":\"USA\",\"id\":0,\"enabled\":true,\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"global\":false,\"name\":\"John Smith\",\"versionId\":null,\"type\":\"myType\"," +
                "\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getEndpointTemplateWithoutWrapper_nullInternalURL_returnJSONObject() throws Exception {
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setAdminURL("www.adminURL.com");
        endpointTemplate.setName("John Smith");
        endpointTemplate.setPublicURL("www.publicURL.com");
        endpointTemplate.setType("myType");
        endpointTemplate.setRegion("USA");
        endpointTemplate.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateWithoutWrapper(endpointTemplate);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"region\":\"USA\",\"id\":0,\"enabled\":true,\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"global\":false,\"name\":\"John Smith\",\"adminURL\":\"www.adminURL.com\",\"versionId\":null,\"type\":\"myType\"}"));
    }

    @Test
    public void getEndpointTemplateWithoutWrapper_nullName_returnJSONObject() throws Exception {
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setAdminURL("www.adminURL.com");
        endpointTemplate.setInternalURL("www.internalURL.com");
        endpointTemplate.setPublicURL("www.publicURL.com");
        endpointTemplate.setType("myType");
        endpointTemplate.setRegion("USA");
        endpointTemplate.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateWithoutWrapper(endpointTemplate);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"region\":\"USA\",\"id\":0,\"enabled\":true,\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"global\":false,\"adminURL\":\"www.adminURL.com\",\"versionId\":null,\"type\":\"myType\"," +
                "\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getEndpointTemplateWithoutWrapper_nullPublicURL_returnJSONObject() throws Exception {
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setAdminURL("www.adminURL.com");
        endpointTemplate.setInternalURL("www.internalURL.com");
        endpointTemplate.setName("John Smith");
        endpointTemplate.setType("myType");
        endpointTemplate.setRegion("USA");
        endpointTemplate.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateWithoutWrapper(endpointTemplate);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"region\":\"USA\",\"id\":0,\"enabled\":true,\"versionInfo\":null," +
                "\"versionList\":null,\"global\":false,\"name\":\"John Smith\",\"adminURL\":\"www.adminURL.com\",\"versionId\":null,\"type\":\"myType\"," +
                "\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getEndpointTemplateWithoutWrapper_nullType_returnJSONObject() throws Exception {
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setAdminURL("www.adminURL.com");
        endpointTemplate.setInternalURL("www.internalURL.com");
        endpointTemplate.setName("John Smith");
        endpointTemplate.setPublicURL("www.publicURL.com");
        endpointTemplate.setRegion("USA");
        endpointTemplate.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateWithoutWrapper(endpointTemplate);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"region\":\"USA\",\"id\":0,\"enabled\":true,\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"global\":false,\"name\":\"John Smith\",\"adminURL\":\"www.adminURL.com\",\"versionId\":null," +
                "\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getEndpointTemplateWithoutWrapper_nullRegion_returnJSONObject() throws Exception {
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setAdminURL("www.adminURL.com");
        endpointTemplate.setInternalURL("www.internalURL.com");
        endpointTemplate.setName("John Smith");
        endpointTemplate.setPublicURL("www.publicURL.com");
        endpointTemplate.setType("myType");
        endpointTemplate.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateWithoutWrapper(endpointTemplate);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":0,\"enabled\":true,\"publicURL\":\"www.publicURL.com\",\"versionInfo\":null," +
                "\"versionList\":null,\"global\":false,\"name\":\"John Smith\",\"adminURL\":\"www.adminURL.com\",\"versionId\":null,\"type\":\"myType\"," +
                "\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getEndpointTemplateWithoutWrapper_nullVersionL_returnJSONObject() throws Exception {
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setAdminURL("www.adminURL.com");
        endpointTemplate.setInternalURL("www.internalURL.com");
        endpointTemplate.setName("John Smith");
        endpointTemplate.setPublicURL("www.publicURL.com");
        endpointTemplate.setType("myType");
        endpointTemplate.setRegion("USA");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateWithoutWrapper(endpointTemplate);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"region\":\"USA\",\"id\":0,\"enabled\":true,\"publicURL\":\"www.publicURL.com\"," +
                "\"global\":false,\"name\":\"John Smith\",\"adminURL\":\"www.adminURL.com\",\"type\":\"myType\"," +
                "\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getEndpointTemplateList_fullyPopulated_returnsJSONObject() throws Exception {
        EndpointTemplate endpointTemplate1 = new EndpointTemplate();
        EndpointTemplate endpointTemplate2 = new EndpointTemplate();
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate1);
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate2);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        doReturn(jsonObject).when(spy).getEndpointTemplateWithoutWrapper(endpointTemplate1);
        doReturn(jsonObject).when(spy).getEndpointTemplateWithoutWrapper(endpointTemplate2);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getEndpointTemplateList(endpointTemplateList);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"OS-KSCATALOG:endpointTemplates\":{\"OS-KSCATALOG:endpointTemplate\":[" +
                "{\"success\":\"This test worked!\"},{\"success\":\"This test worked!\"}]}}"));
    }

    @Test
    public void getEndpointTemplateList_emptyList_returnsJSONObject() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpointTemplateList(endpointTemplateList);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"OS-KSCATALOG:endpointTemplates\":{\"OS-KSCATALOG:endpointTemplate\":[]}}"));
    }

    @Test
    public void getBaseURL_fullyPopulated_returnsJSONObject() throws Exception {
        BaseURL baseURL = new BaseURL();
        baseURL.setInternalURL("www.internalURL.com");
        baseURL.setPublicURL("www.publicURL.com");
        baseURL.setRegion("USA");
        baseURL.setServiceName("service");
        baseURL.setUserType(UserType.CLOUD);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getBaseUrl(baseURL);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":0,\"region\":\"USA\",\"publicURL\":\"www.publicURL.com\",\"enabled\":true," +
                "\"default\":false,\"userType\":\"CLOUD\",\"serviceName\":\"service\",\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getBaseURL_nullInternalURL_returnsJSONObject() throws Exception {
        BaseURL baseURL = new BaseURL();
        baseURL.setPublicURL("www.publicURL.com");
        baseURL.setRegion("USA");
        baseURL.setServiceName("service");
        baseURL.setUserType(UserType.CLOUD);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getBaseUrl(baseURL);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":0,\"region\":\"USA\",\"publicURL\":\"www.publicURL.com\",\"enabled\":true," +
                "\"default\":false,\"userType\":\"CLOUD\",\"serviceName\":\"service\"}"));
    }

    @Test
    public void getBaseURL_nullPublicURL_returnsJSONObject() throws Exception {
        BaseURL baseURL = new BaseURL();
        baseURL.setInternalURL("www.internalURL.com");
        baseURL.setRegion("USA");
        baseURL.setServiceName("service");
        baseURL.setUserType(UserType.CLOUD);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getBaseUrl(baseURL);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":0,\"region\":\"USA\",\"enabled\":true," +
                "\"default\":false,\"userType\":\"CLOUD\",\"serviceName\":\"service\",\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getBaseURL_nullRegion_returnsJSONObject() throws Exception {
        BaseURL baseURL = new BaseURL();
        baseURL.setInternalURL("www.internalURL.com");
        baseURL.setPublicURL("www.publicURL.com");
        baseURL.setServiceName("service");
        baseURL.setUserType(UserType.CLOUD);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getBaseUrl(baseURL);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":0,\"publicURL\":\"www.publicURL.com\",\"enabled\":true," +
                "\"default\":false,\"userType\":\"CLOUD\",\"serviceName\":\"service\",\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getBaseURL_nullServiceName_returnsJSONObject() throws Exception {
        BaseURL baseURL = new BaseURL();
        baseURL.setInternalURL("www.internalURL.com");
        baseURL.setPublicURL("www.publicURL.com");
        baseURL.setRegion("USA");
        baseURL.setUserType(UserType.CLOUD);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getBaseUrl(baseURL);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":0,\"region\":\"USA\",\"publicURL\":\"www.publicURL.com\",\"enabled\":true," +
                "\"default\":false,\"userType\":\"CLOUD\",\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getBaseURL_nullUserType_returnsJSONObject() throws Exception {
        BaseURL baseURL = new BaseURL();
        baseURL.setInternalURL("www.internalURL.com");
        baseURL.setPublicURL("www.publicURL.com");
        baseURL.setRegion("USA");
        baseURL.setServiceName("service");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getBaseUrl(baseURL);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"id\":0,\"region\":\"USA\",\"publicURL\":\"www.publicURL.com\",\"enabled\":true," +
                "\"default\":false,\"serviceName\":\"service\",\"internalURL\":\"www.internalURL.com\"}"));
    }

    @Test
    public void getExtension() throws Exception {
        Extension extension = new Extension();
        extension.setAlias("alias");
        extension.setDescription("description");
        extension.setName("name");
        extension.setNamespace("namespace");
        extension.setUpdated(calendar);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(extension, Extension.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"extension\":{\"updated\":\"2012-01-01\",\"alias\":\"alias\",\"description\":\"description\",\"name\":\"name\",\"namespace\":\"namespace\"}}", myOut.toString());
    }

    @Test
    public void getExtensionList_listPopulated_returnsJSONObject() throws Exception {
        Extension extension1 = new Extension();
        Extension extension2 = new Extension();
        Extensions extensions = new Extensions();
        extensions.getExtension().add(extension1);
        extensions.getExtension().add(extension2);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        doReturn(jsonObject).when(spy).getExtensionWithoutWrapper(extension1);
        doReturn(jsonObject).when(spy).getExtensionWithoutWrapper(extension2);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = spy.getExtensionList(extensions);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"extensions\":[{\"success\":\"This test worked!\"},{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void getExtensionList_listEmpty_returnsJSONObject() throws Exception {
        Extensions extensions = new Extensions();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getExtensionList(extensions);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"extensions\":[]}"));
    }

    @Test
    public void getEndpoint_fullyPopulated_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setName("John Smith");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setType("type");
        endpoint.setTenantId("123");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"region\":\"USA\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"versionId\":null," +
                "\"type\":\"type\",\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getEndpoint_nullRegion_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setName("John Smith");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setType("type");
        endpoint.setTenantId("123");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"versionId\":null," +
                "\"type\":\"type\",\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getEndpoint_nullPublicURL_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setName("John Smith");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setType("type");
        endpoint.setTenantId("123");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"region\":\"USA\",\"id\":0," +
                "\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"versionId\":null," +
                "\"type\":\"type\",\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getEndpoint_nullName_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setType("type");
        endpoint.setTenantId("123");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"region\":\"USA\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"versionId\":null," +
                "\"type\":\"type\",\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getEndpoint_nullAdminURL_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setName("John Smith");
        endpoint.setType("type");
        endpoint.setTenantId("123");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"region\":\"USA\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"versionInfo\":null,\"versionList\":null,\"name\":\"John Smith\",\"versionId\":null," +
                "\"type\":\"type\",\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getEndpoint_nullType_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setName("John Smith");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setTenantId("123");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"region\":\"USA\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"versionId\":null," +
                "\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getEndpoint_nullTenantID_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setName("John Smith");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setType("type");
        endpoint.setInternalURL("www.internalURL.com");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"region\":\"USA\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"versionId\":null," +
                "\"type\":\"type\",\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getEndpoint_nullInternalURL_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setName("John Smith");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setType("type");
        endpoint.setTenantId("123");
        endpoint.setVersion(new VersionForService());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"region\":\"USA\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"versionId\":null," +
                "\"type\":\"type\"}"));

    }

    @Test
    public void getEndpoint_nullVersion_returnsJSONObject() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setRegion("USA");
        endpoint.setPublicURL("www.publicURL.com");
        endpoint.setName("John Smith");
        endpoint.setAdminURL("www.adminURL.com");
        endpoint.setType("type");
        endpoint.setTenantId("123");
        endpoint.setInternalURL("www.internalURL.com");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getEndpoint(endpoint);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"tenantId\":\"123\",\"region\":\"USA\",\"id\":0,\"publicURL\":\"www.publicURL.com\"," +
                "\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\"," +
                "\"type\":\"type\",\"internalURL\":\"www.internalURL.com\"}"));

    }

    @Test
    public void getExtensionWithoutWrapper_nullUpdated_returnsJSONObject() throws Exception {
        Extension extension = new Extension();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getExtensionWithoutWrapper(extension);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"alias\":null,\"description\":null,\"name\":null,\"namespace\":null}"));
    }

    @Test
    public void getExtensionWithoutWrapper_anyEmpty_returnsJSONObject() throws Exception {
        Extension extension = new Extension();
        extension.setUpdated(calendar);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getExtensionWithoutWrapper(extension);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString().startsWith("{\"updated\":\"2012-01-01"), equalTo(true));
        assertThat("string", myOut.toString().endsWith("\",\"alias\":null,\"description\":null,\"name\":null,\"namespace\":null}"), equalTo(true));

    }

    @Test
    public void getExtensionWithoutWrapper_anyHasLinks_returnsJSONObject() throws Exception {
        Link link1 = new Link();
        Link link2 = new Link();
        Extension extension = new Extension();

        JAXBElement<Link> jaxbElement1 = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link1);
        JAXBElement<Link> jaxbElement2 = new JAXBElement<Link>(QName.valueOf("foo"),Link.class,link2);

        extension.getAny().add(jaxbElement1);
        extension.getAny().add(jaxbElement2);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getExtensionWithoutWrapper(extension);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"alias\":null,\"description\":null,\"name\":null,\"links\":[{},{}],\"namespace\":null}"));
    }

    @Test
    public void getExtensionWithoutWrapper_anyHasNoLinks_returnsJSONObject() throws Exception {
        Extension extension = new Extension();
        extension.getAny().add(new Token());
        extension.getAny().add(new Token());

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject result = writer.getExtensionWithoutWrapper(extension);
        String jsonText = JSONValue.toJSONString(result);
        myOut.write(jsonText.getBytes());
        assertThat("string", myOut.toString(), equalTo("{\"alias\":null,\"description\":null,\"name\":null,\"namespace\":null}"));
    }

    @Test
    public void getExtensions() throws Exception {
        Extensions extensions = new Extensions();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(extensions, Extensions.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"extensions\":[]}", myOut.toString());
    }

    @Test
    public void getTenants() throws Exception {
        Tenants tenants = new Tenants();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(tenants, Tenants.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"tenants\":[]}", myOut.toString());
    }

    @Test
    public void getService() throws Exception {
        Service service = new Service();
        service.setName("name");
        service.setDescription("description");
        service.setId("id");
        service.setType("type");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(service, Service.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"OS-KSADM:service\":{\"id\":\"id\",\"description\":\"description\",\"name\":\"name\",\"type\":\"type\"}}", myOut.toString());
    }

    @Test
    public void getServiceList() throws Exception {
        ServiceList serviceList = new ServiceList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(serviceList, ServiceList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"OS-KSADM:services\":[]}", myOut.toString());
    }

    @Test
    public void getPasswordCredentialsBase_returnsValidObject() throws Exception {
        PasswordCredentialsBase passwordCredentialsBase = new PasswordCredentialsBase();
        passwordCredentialsBase.setPassword("bananas");
        passwordCredentialsBase.setUsername("jqsmith");
        JSONObject jsonObject = writer.getPasswordCredentials(passwordCredentialsBase);
        JSONObject jsonObject1 = (JSONObject) jsonObject.get("passwordCredentials");
        assertThat("string", jsonObject1.get("username").toString(), equalTo("jqsmith"));
        assertThat("string", jsonObject1.get("password").toString(), equalTo("bananas"));
    }

    @Test
    public void getSecretQA() throws Exception {
        SecretQA secretQA = new SecretQA();
        secretQA.setQuestion("why?");
        secretQA.setAnswer("because!");
        secretQA.setUsername("username");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(secretQA, SecretQA.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"RAX-KSQA:secretQA\":{\"answer\":\"because!\",\"question\":\"why?\"}}", myOut.toString());
    }

    @Test
    public void getEndpointTemplate() throws Exception {
        VersionForService version = new VersionForService();
        version.setId("id");
        version.setInfo("info");
        version.setList("list");

        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setAdminURL("adminurl");
        endpointTemplate.setEnabled(true);
        endpointTemplate.setGlobal(true);
        endpointTemplate.setId(1);
        endpointTemplate.setInternalURL("internalurl");
        endpointTemplate.setName("name");
        endpointTemplate.setPublicURL("publicurl");
        endpointTemplate.setRegion("region");
        endpointTemplate.setType("type");
        endpointTemplate.setVersion(version);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(endpointTemplate, EndpointTemplate.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"OS-KSCATALOG:endpointTemplate\":{\"region\":\"region\",\"id\":1,\"enabled\":true,\"publicURL\":\"publicurl\",\"versionInfo\":\"info\",\"versionList\":\"list\",\"global\":true,\"name\":\"name\",\"adminURL\":\"adminurl\",\"versionId\":\"id\",\"type\":\"type\",\"internalURL\":\"internalurl\"}}", myOut.toString());
    }

    @Test
    public void getEndpoint() throws Exception {
        VersionForService version = new VersionForService();
        version.setId("id");
        version.setInfo("info");
        version.setList("list");

        Endpoint endpoint = new Endpoint();
        endpoint.setAdminURL("adminurl");
        endpoint.setId(1);
        endpoint.setInternalURL("internalurl");
        endpoint.setName("name");
        endpoint.setPublicURL("publicurl");
        endpoint.setRegion("region");
        endpoint.setType("type");
        endpoint.setVersion(version);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(endpoint, Endpoint.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"endpoint\":{\"region\":\"region\",\"id\":1,\"publicURL\":\"publicurl\",\"versionInfo\":\"info\",\"versionList\":\"list\",\"adminURL\":\"adminurl\",\"name\":\"name\",\"versionId\":\"id\",\"type\":\"type\",\"internalURL\":\"internalurl\"}}", myOut.toString());
    }

    @Test
    public void getEndpointList() throws Exception {
        EndpointList endpointList = new EndpointList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(endpointList, EndpointList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"endpoints\":[]}", myOut.toString());
    }

    @Test
    public void getEndpointTemplateList() throws Exception {
        EndpointTemplateList endpointList = new EndpointTemplateList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(endpointList, EndpointTemplateList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"OS-KSCATALOG:endpointTemplates\":[]}", myOut.toString());
    }

    @Test
    public void getApiKeyCredentials() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("apiKey");
        creds.setUsername("username");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(creds, ApiKeyCredentials.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"username\",\"apiKey\":\"apiKey\"}}", myOut.toString());
    }

    @Test
    public void getGroups() throws Exception {
        Groups groups = new Groups();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(groups, Groups.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"RAX-KSGRP:groups\":[]}", myOut.toString());
    }

    @Test
    public void getCredentialListType() throws Exception {
        CredentialListType creds = new CredentialListType();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(creds, CredentialListType.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"credentials\":[]}", myOut.toString());
    }

    @Test
    public void getRoleList() throws Exception {
        RoleList roles = new RoleList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(roles, RoleList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"roles\":[]}", myOut.toString());
    }

    @Test
    public void getDefaultRegionServices() throws Exception {
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        defaultRegionServices.getServiceName().add("cloudFiles");
        defaultRegionServices.getServiceName().add("openstackNova");
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(defaultRegionServices, DefaultRegionServices.class, null, null, null, null, myOut);
        assertThat("services", myOut.toString(), equalTo("{\"RAX-AUTH:defaultRegionServices\":[\"cloudFiles\","+"\"openstackNova\"]}"));
    }

    @Test
    public void getDefaultRegionServices_emptyList() throws Exception {
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(defaultRegionServices, DefaultRegionServices.class, null, null, null, null, myOut);
        assertThat("services", myOut.toString(), equalTo("{\"RAX-AUTH:defaultRegionServices\":[]}"));
    }

    @Test
    public void getUserList() throws Exception {
        UserList users = new UserList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(users, UserList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"users\":[]}", myOut.toString());
    }

    @Test
    public void getAuthenticateResponse() throws Exception {
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();

        TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
        tenant.setId("id");
        tenant.setName("name");

        Token token = new Token();
        token.setExpires(calendar);
        token.setId("id");
        token.setTenant(tenant);

        ServiceCatalog serviceCatalog = new ServiceCatalog();
        authenticateResponse.setServiceCatalog(serviceCatalog);
        authenticateResponse.setToken(token);

        UserForAuthenticateResponse user = new UserForAuthenticateResponse();
        user.setId("id");
        user.setName("name");
        user.setRoles(new RoleList());
        authenticateResponse.setUser(user);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(authenticateResponse, AuthenticateResponse.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"access\":{\"token\":{\"id\":\"id\",\"expires\":\"2012-01-01\",\"tenant\":{\"id\":\"id\",\"name\":\"name\"}},\"serviceCatalog\":[],\"user\":{\"id\":\"id\",\"roles\":[],\"name\":\"name\",\"RAX-AUTH:defaultRegion\":\"\"}}}", myOut.toString());
    }

    @Test
    public void getBaseUrlList() throws Exception {
        BaseURLList baseURLList = new BaseURLList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(baseURLList, BaseURLList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"baseURLs\":[]}", myOut.toString());
    }

    @Test
    public void getV1User() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        user.setCreated(calendar);
        user.setBaseURLRefs(new BaseURLRefList());
        user.setEnabled(true);
        user.setId("id");
        user.setKey("key");
        user.setMossoId(1);
        user.setNastId("nast");
        user.setUpdated(calendar);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(user, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"user\":{\"id\":\"id\",\"enabled\":true,\"nastId\":\"nast\",\"updated\":\"2012-01-01\",\"created\":\"2012-01-01\",\"mossoId\":1,\"baseURLRefs\":[],\"key\":\"key\"}}", myOut.toString());
    }

    @Test
    public void getTokenUser() throws Exception {
        UserForAuthenticateResponse user = new UserForAuthenticateResponse();
        user.setId("id");
        user.setName("name");
        user.setRoles(null);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        writer.writeTo(user, UserForAuthenticateResponse.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"access\":{\"name\":\"name\",\"id\":\"id\"}}", myOut.toString());
    }
}
