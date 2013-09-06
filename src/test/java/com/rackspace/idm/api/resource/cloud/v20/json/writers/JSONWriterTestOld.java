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
        assertThat("string", myOut.toString(), equalTo("{\"baseURLs\":[{\"success\":\"This test worked!\"}]}"));
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
    public void writeTo_BaseURLRefList_returnList() throws Exception {
        BaseURLRefList baseURLRefList = new BaseURLRefList();
        BaseURLRef baseURLRef = new BaseURLRef();
        baseURLRef.setId(1);
        baseURLRef.setHref("http://");
        baseURLRef.setV1Default(false);
        baseURLRefList.getBaseURLRef().add(baseURLRef);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        spy.writeTo(baseURLRefList,null,null,null,null,null,myOut);
        assertThat("string", myOut.toString(), equalTo("{\"baseURLRefs\":[{\"id\":1,\"v1Default\":false,\"href\":\"http:\\/\\/\"}]}"));
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

    @Test
    public void getDomainsWithoutWrapper() throws IOException {
        Domains domains = new Domains();
        Domain domain = new Domain();
        domain.setId("1");
        domain.setDescription("des");
        domain.setEnabled(true);
        domain.setName("name");
        domains.getDomain().add(domain);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();

        writer.writeTo(domains,Domains.class, null, null, null, null, myOut);

        Assert.assertEquals("{\"RAX-AUTH:domains\":[{\"id\":\"1\",\"enabled\":true,\"description\":\"des\",\"name\":\"name\"}]}",myOut.toString());
    }
}
