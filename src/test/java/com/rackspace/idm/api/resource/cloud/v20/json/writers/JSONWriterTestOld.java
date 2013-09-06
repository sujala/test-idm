package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.sun.jersey.api.json.JSONMarshaller;
import junit.framework.Assert;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.common.api.v1.*;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
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
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
public class JSONWriterTestOld {

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
        assertThat("bool", writer.isWriteable(null, PasswordCredentialsBase.class, null, null), equalTo(true));
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
    public void writeTo_typeCredentialListTypeNullValue_runsSuccessfully() throws Exception {
        JAXBElement<ApiKeyCredentials> apiKeyCredentialsJAXBElement = new JAXBElement<ApiKeyCredentials>(QName.valueOf("fee"), ApiKeyCredentials.class, null);
        CredentialListType credentialListType = new CredentialListType();
        credentialListType.getCredential().add(apiKeyCredentialsJAXBElement);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(credentialListType, CredentialListType.class, null, null, null, null, myOut);
    }

    @Test
    public void writeTo_Policy() throws Exception {
        Policy policy = new Policy();
        policy.setId("1");
        policy.setName("name");
        policy.setDescription("description");
        policy.setBlob("b l o b");
        policy.setEnabled(true);
        policy.setGlobal(true);
        policy.setType("type");
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(policy,null,null,null,null,null,myOut);
        assertThat("string", myOut.toString(), equalTo("{\"RAX-AUTH:policy\":{\"blob\":\"b l o b\",\"id\":\"1\",\"enabled\":true,\"description\":\"description\",\"name\":\"name\",\"global\":true,\"type\":\"type\"}}"));
    }

    @Test
    public void writeTo_Policies() throws Exception {
        Policies policies = new Policies();
        Policy policy = new Policy();
        policy.setId("1");
        policy.setName("name");
        policy.setDescription("description");
        policy.setBlob("b l o b");
        policy.setEnabled(true);
        policy.setGlobal(true);
        policy.setType("type");
        policies.getPolicy().add(policy);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(policies,null,null,null,null,null,myOut);
        assertThat("string", myOut.toString(), equalTo("{\"RAX-AUTH:policies\":{\"RAX-AUTH:policy\":[{\"id\":\"1\",\"enabled\":true,\"global\":true,\"name\":\"name\",\"type\":\"type\"}]}}"));
    }

    @Test
    public void writeTo_AuthData() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.Token token = new com.rackspacecloud.docs.auth.api.v1.Token();
        com.rackspacecloud.docs.auth.api.v1.ServiceCatalog serviceCatalog = new com.rackspacecloud.docs.auth.api.v1.ServiceCatalog();
        com.rackspacecloud.docs.auth.api.v1.Service service = new com.rackspacecloud.docs.auth.api.v1.Service();
        service.setName("name");
        serviceCatalog.getService().add(service);
        token.setId("id");
        token.setExpires(calendar);
        AuthData authData = new AuthData();
        authData.setToken(token);
        authData.setServiceCatalog(serviceCatalog);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(authData,null,null,null,null,null,myOut);
        assertThat("string", myOut.toString(), equalTo("{\"auth\":{\"token\":{\"id\":\"id\",\"expires\":\"2012-01-01\"},\"serviceCatalog\":{\"name\":[]}}}"));
    }

    @Test
    public void writeTo_AuthData_nullServiceCatalog() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.Token token = new com.rackspacecloud.docs.auth.api.v1.Token();
        token.setId("id");
        token.setExpires(calendar);
        AuthData authData = new AuthData();
        authData.setToken(token);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(authData,null,null,null,null,null,myOut);
        assertThat("string", myOut.toString(), equalTo("{\"auth\":{\"token\":{\"id\":\"id\",\"expires\":\"2012-01-01\"}}}"));
    }

    @Test
    public void writeTo_ServiceCatalog() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.ServiceCatalog serviceCatalog = new com.rackspacecloud.docs.auth.api.v1.ServiceCatalog();
        com.rackspacecloud.docs.auth.api.v1.Service service = new com.rackspacecloud.docs.auth.api.v1.Service();
        service.setName("name");
        serviceCatalog.getService().add(service);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(serviceCatalog,null,null,null,null,null,myOut);
        assertThat("string", myOut.toString(), equalTo("{\"serviceCatalog\":{\"name\":[]}}"));
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
