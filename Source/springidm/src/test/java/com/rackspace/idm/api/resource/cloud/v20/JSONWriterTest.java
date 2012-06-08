package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentialsWithOnlyApiKey;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import junit.framework.Assert;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
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
        assertThat("long", writer.getSize(null,null,null,null,null), equalTo(-1L));
    }

    @Test
    public void isWritable_typeIsJAXBElement_returnsTrue() throws Exception {
        assertThat("bool",writer.isWriteable(JAXBElement.class,null,null,null),equalTo(true));
    }

    @Test
    public void isWritable_typeIsNotJAXBElement_returnsFalse() throws Exception {
        assertThat("bool",writer.isWriteable(User.class,null,null,null),equalTo(false));
    }

    @Test
    public void writeTo_JAXBElementTypeVersionChoice_callsGetVersionChoice() throws Exception {
        VersionChoice versionChoice = new VersionChoice();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<VersionChoice> jaxbElement = new JAXBElement<VersionChoice>(QName.valueOf("foo"),VersionChoice.class,versionChoice);
        spy.writeTo(jaxbElement, VersionChoice.class, null, null, null, null, myOut);
        verify(spy).getVersionChoice(versionChoice);
    }

    @Test
    public void writeTo_JAXBElementTypeVersionChoice_writesToOutputStream() throws Exception {
        VersionChoice versionChoice = new VersionChoice();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success","This test worked!");
        JAXBElement<VersionChoice> jaxbElement = new JAXBElement<VersionChoice>(QName.valueOf("foo"),VersionChoice.class,versionChoice);
        doReturn(jsonObject).when(spy).getVersionChoice(versionChoice);
        spy.writeTo(jaxbElement, VersionChoice.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_JAXBElementTypeTenants_callsGetTenantWithoutWrapper() throws Exception {
        Tenants tenants = new Tenants();
        Tenant tenant = new Tenant();
        tenants.getTenant().add(tenant);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<Tenants> jaxbElement = new JAXBElement<Tenants>(QName.valueOf("foo"),Tenants.class,tenants);
        spy.writeTo(jaxbElement, VersionChoice.class, null, null, null, null, myOut);
        verify(spy).getTenantWithoutWrapper(tenant);
    }

    @Test
    public void writeTo_JAXBElementTypeTenants_writesToOutputStream() throws Exception {
        Tenants tenants = new Tenants();
        Tenant tenant = new Tenant();
        tenants.getTenant().add(tenant);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success","This test worked!");
        JAXBElement<Tenants> jaxbElement = new JAXBElement<Tenants>(QName.valueOf("foo"),Tenants.class,tenants);
        doReturn(jsonObject).when(spy).getTenantWithoutWrapper(tenant);
        spy.writeTo(jaxbElement, Tenants.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"tenants\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_JAXBElementTypeServiceList_callsGetServiceWithoutWrapper() throws Exception {
        ServiceList serviceList = new ServiceList();
        Service service = new Service();
        serviceList.getService().add(service);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<ServiceList> jaxbElement = new JAXBElement<ServiceList>(QName.valueOf("foo"),ServiceList.class,serviceList);
        spy.writeTo(jaxbElement, ServiceList.class, null, null, null, null, myOut);
        verify(spy).getServiceWithoutWrapper(service);
    }

    @Test
    public void writeTo_JAXBElementTypeServiceList_writesToOutputStream() throws Exception {
        ServiceList serviceList = new ServiceList();
        Service service = new Service();
        serviceList.getService().add(service);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success","This test worked!");
        JAXBElement<ServiceList> jaxbElement = new JAXBElement<ServiceList>(QName.valueOf("foo"),ServiceList.class,serviceList);
        doReturn(jsonObject).when(spy).getServiceWithoutWrapper(service);
        spy.writeTo(jaxbElement, ServiceList.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"OS-KSADM:services\":[{\"success\":\"This test worked!\"}]}"));
    }

    @Test
    public void writeTo_JAXBElementTypeEndPointList_callsGetEndpoint() throws Exception {
        EndpointList endpointList = new EndpointList();
        Endpoint endpoint = new Endpoint();
        endpointList.getEndpoint().add(endpoint);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<EndpointList> jaxbElement = new JAXBElement<EndpointList>(QName.valueOf("foo"),EndpointList.class,endpointList);
        spy.writeTo(jaxbElement, EndpointList.class, null, null, null, null, myOut);
        verify(spy).getEndpoint(endpoint);
    }

    @Test
    public void writeTo_JAXBElementTypeEndPointList_writesToOutputStream() throws Exception {
        EndpointList endpointList = new EndpointList();
        Endpoint endpoint = new Endpoint();
        endpointList.getEndpoint().add(endpoint);
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success","This test worked!");
        JAXBElement<EndpointList> jaxbElement = new JAXBElement<EndpointList>(QName.valueOf("foo"),EndpointList.class,endpointList);
        doReturn(jsonObject).when(spy).getEndpoint(endpoint);
        spy.writeTo(jaxbElement, EndpointList.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"endpoints\":[{\"success\":\"This test worked!\"}]}"));
    }


    @Test
    public void writeTo_JAXBElementTypeEndPointTemplateListFullyPopulated_writesToOutputStreamAllValues() throws Exception {
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
        JAXBElement<EndpointTemplateList> jaxbElement = new JAXBElement<EndpointTemplateList>(QName.valueOf("foo"),EndpointTemplateList.class,endpointTemplateList);
        writer.writeTo(jaxbElement, EndpointList.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"OS-KSCATALOG:endpointTemplates\":[{\"region\":\"USA\",\"id\":123,\"publicURL\":\"www.publicURL.com\"," +
            "\"enabled\":false,\"versionInfo\":null,\"versionList\":null,\"adminURL\":\"www.adminURL.com\",\"name\":\"John Smith\",\"global\":true," +
            "\"versionId\":\"456\",\"type\":\"CLOUD\",\"internalURL\":\"www.internalURL.com\"}]}"));
    }

    @Test
    public void writeTo_JAXBElementTypeEndPointTemplateListEmptyTemplate_writesToOutputStreamEmptyList() throws Exception {
        EndpointTemplateList endpointTemplateList = new EndpointTemplateList();
        EndpointTemplate endpointTemplate = new EndpointTemplate();
        endpointTemplate.setId(123);
        endpointTemplate.setEnabled(false);
        endpointTemplateList.getEndpointTemplate().add(endpointTemplate);
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<EndpointTemplateList> jaxbElement = new JAXBElement<EndpointTemplateList>(QName.valueOf("foo"),EndpointTemplateList.class,endpointTemplateList);
        writer.writeTo(jaxbElement, EndpointList.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"OS-KSCATALOG:endpointTemplates\":[{\"id\":123,\"enabled\":false}]}"));
    }

    @Test
    public void writeTo_JAXBElementTypeCredentialTypeSecretQA_callsGetSecretQA() throws Exception {
        SecretQA secretQA = new SecretQA();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<CredentialType> jaxbElement = new JAXBElement<CredentialType>(QName.valueOf("foo"),CredentialType.class,secretQA);
        spy.writeTo(jaxbElement, CredentialType.class, null, null, null, null, myOut);
        verify(spy).getSecretQA(secretQA);
    }



    @Test
    public void writeTo_JAXBElementTypeCredentialTypeSecretQA_writesToOutputStream() throws Exception {
        SecretQA secretQA = new SecretQA();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success","This test worked!");
        JAXBElement<CredentialType> jaxbElement = new JAXBElement<CredentialType>(QName.valueOf("foo"),CredentialType.class,secretQA);
        doReturn(jsonObject).when(spy).getSecretQA(secretQA);
        spy.writeTo(jaxbElement, CredentialType.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_JAXBElementTypeCredentialTypePasswordCredentialsRequiredUsername_callsGetPasswordCredentials() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<CredentialType> jaxbElement = new JAXBElement<CredentialType>(QName.valueOf("foo"),CredentialType.class,passwordCredentialsRequiredUsername);
        spy.writeTo(jaxbElement, CredentialType.class, null, null, null, null, myOut);
        verify(spy).getPasswordCredentials(passwordCredentialsRequiredUsername);
    }



    @Test
    public void writeTo_JAXBElementTypeCredentialTypePasswordCredentialsRequiredUsername_writesToOutputStream() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success","This test worked!");
        JAXBElement<CredentialType> jaxbElement = new JAXBElement<CredentialType>(QName.valueOf("foo"),CredentialType.class,passwordCredentialsRequiredUsername);
        doReturn(jsonObject).when(spy).getPasswordCredentials(passwordCredentialsRequiredUsername);
        spy.writeTo(jaxbElement, CredentialType.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Test
    public void writeTo_JAXBElementTypeCredentialTypePasswordCredentialsBase_callsGetPasswordCredentialsBase() throws Exception {
        PasswordCredentialsBase passwordCredentialsBase = new PasswordCredentialsBase();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement<CredentialType> jaxbElement = new JAXBElement<CredentialType>(QName.valueOf("foo"),CredentialType.class,passwordCredentialsBase);
        spy.writeTo(jaxbElement, CredentialType.class, null, null, null, null, myOut);
        verify(spy).getPasswordCredentialsBase(passwordCredentialsBase);
    }



    @Test
    public void writeTo_JAXBElementTypeCredentialTypePasswordCredentialsBase_writesToOutputStream() throws Exception {
        PasswordCredentialsBase passwordCredentialsBase = new PasswordCredentialsBase();
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        jsonObject.put("success","This test worked!");
        JAXBElement<CredentialType> jaxbElement = new JAXBElement<CredentialType>(QName.valueOf("foo"),CredentialType.class,passwordCredentialsBase);
        doReturn(jsonObject).when(spy).getPasswordCredentialsBase(passwordCredentialsBase);
        spy.writeTo(jaxbElement, CredentialType.class, null, null, null, null, myOut);
        assertThat("string",myOut.toString(),equalTo("{\"success\":\"This test worked!\"}"));
    }

    @Ignore
    @Test
    public void writeTo_JAXBElementTypeCredentialTypeEC2CredentialsType_() throws Exception {

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
        JAXBElement jaxbElement = new JAXBElement<Extension>(QName.valueOf("foo"), Extension.class, extension);
        writer.writeTo(jaxbElement, Extension.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"extension\":{\"updated\":\"2012-01-01\",\"alias\":\"alias\",\"description\":\"description\",\"name\":\"name\",\"namespace\":\"namespace\"}}", myOut.toString());
    }


    @Test
    public void getExtensions() throws Exception {
        Extensions extensions = new Extensions();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<Extensions>(QName.valueOf("foo"), Extensions.class, extensions);
        writer.writeTo(jaxbElement, Extensions.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"extensions\":[]}", myOut.toString());
    }

    @Test
    public void getTenants() throws Exception {
        Tenants tenants = new Tenants();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<Tenants>(QName.valueOf("foo"), Tenants.class, tenants);
        writer.writeTo(jaxbElement, Tenants.class, null, null, null, null, myOut);
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
        JAXBElement jaxbElement = new JAXBElement<Service>(QName.valueOf("foo"), Service.class, service);
        writer.writeTo(jaxbElement, Service.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"OS-KSADM:service\":{\"id\":\"id\",\"description\":\"description\",\"name\":\"name\",\"type\":\"type\"}}", myOut.toString());
    }

    @Test
    public void getServiceList() throws Exception {
        ServiceList serviceList = new ServiceList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<ServiceList>(QName.valueOf("foo"), ServiceList.class, serviceList);
        writer.writeTo(jaxbElement, ServiceList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"OS-KSADM:services\":[]}", myOut.toString());
    }

    @Test
    public void getPasswordCredentialsBase_returnsValidObject() throws Exception {
        PasswordCredentialsBase passwordCredentialsBase = new PasswordCredentialsBase();
        passwordCredentialsBase.setPassword("bananas");
        passwordCredentialsBase.setUsername("jqsmith");
        JSONObject jsonObject = writer.getPasswordCredentialsBase(passwordCredentialsBase);
        JSONObject jsonObject1 = (JSONObject) jsonObject.get("passwordCredentials");
        assertThat("string",jsonObject1.get("username").toString(),equalTo("jqsmith"));
        assertThat("string",jsonObject1.get("password").toString(),equalTo("bananas"));
    }

    @Test
    public void getSecretQA() throws Exception {
        SecretQA secretQA = new SecretQA();
        secretQA.setQuestion("why?");
        secretQA.setAnswer("because!");
        secretQA.setUsername("username");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<SecretQA>(QName.valueOf("RAX-KSQA:secretQA"), SecretQA.class, secretQA);
        writer.writeTo(jaxbElement, SecretQA.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"RAX-KSQA:secretQA\":{\"answer\":\"because!\",\"question\":\"why?\",\"username\":\"username\"}}", myOut.toString());
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
        JAXBElement jaxbElement = new JAXBElement<EndpointTemplate>(QName.valueOf("foo"), EndpointTemplate.class, endpointTemplate);
        writer.writeTo(jaxbElement, EndpointTemplate.class, null, null, null, null, myOut);
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
        JAXBElement jaxbElement = new JAXBElement<Endpoint>(QName.valueOf("foo"), Endpoint.class, endpoint);
        writer.writeTo(jaxbElement, Endpoint.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"endpoint\":{\"region\":\"region\",\"id\":1,\"publicURL\":\"publicurl\",\"versionInfo\":\"info\",\"versionList\":\"list\",\"adminURL\":\"adminurl\",\"name\":\"name\",\"versionId\":\"id\",\"type\":\"type\",\"internalURL\":\"internalurl\"}}", myOut.toString());
    }

    @Test
    public void getEndpointList() throws Exception {
        EndpointList endpointList = new EndpointList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<EndpointList>(QName.valueOf("foo"), EndpointList.class, endpointList);
        writer.writeTo(jaxbElement, EndpointList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"endpoints\":[]}", myOut.toString());
    }

    @Test
    public void getEndpointTemplateList() throws Exception {
        EndpointTemplateList endpointList = new EndpointTemplateList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<EndpointTemplateList>(QName.valueOf("foo"), EndpointTemplateList.class, endpointList);
        writer.writeTo(jaxbElement, EndpointTemplateList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"OS-KSCATALOG:endpointTemplates\":[]}", myOut.toString());
    }

    @Test
    public void getApiKeyCredentials() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("apiKey");
        creds.setUsername("username");

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<ApiKeyCredentials>(QName.valueOf("foo"), ApiKeyCredentials.class, creds);
        writer.writeTo(jaxbElement, ApiKeyCredentials.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"username\",\"apiKey\":\"apiKey\"}}", myOut.toString());
    }

    @Test
    public void getGroups() throws Exception {
        Groups groups = new Groups();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<Groups>(QName.valueOf("foo"), Groups.class, groups);
        writer.writeTo(jaxbElement, Groups.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"RAX-KSGRP:groups\":[]}", myOut.toString());
    }

    @Test
    public void getCredentialListType() throws Exception {
        CredentialListType creds = new CredentialListType();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<CredentialListType>(QName.valueOf("foo"), CredentialListType.class, creds);
        writer.writeTo(jaxbElement, CredentialListType.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"credentials\":[]}", myOut.toString());
    }

    @Test
    public void getRoleList() throws Exception {
        RoleList roles = new RoleList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<RoleList>(QName.valueOf("foo"), RoleList.class, roles);
        writer.writeTo(jaxbElement, RoleList.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"roles\":[]}", myOut.toString());
    }

    @Test
    public void getUserList() throws Exception {
        UserList users = new UserList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<UserList>(QName.valueOf("foo"), UserList.class, users);
        writer.writeTo(jaxbElement, UserList.class, null, null, null, null, myOut);
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
        JAXBElement jaxbElement = new JAXBElement<AuthenticateResponse>(QName.valueOf("foo"), AuthenticateResponse.class, authenticateResponse);
        writer.writeTo(jaxbElement, AuthenticateResponse.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"access\":{\"token\":{\"id\":\"id\",\"expires\":\"2012-01-01\",\"tenant\":\"name\"},\"serviceCatalog\":[],\"user\":{\"id\":\"id\",\"roles\":[],\"name\":\"name\"}}}", myOut.toString());
    }

    @Test
    public void getBaseUrlList() throws Exception {
        BaseURLList baseURLList = new BaseURLList();

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<BaseURLList>(QName.valueOf("foo"), BaseURLList.class, baseURLList);
        writer.writeTo(jaxbElement, BaseURLList.class, null, null, null, null, myOut);
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
        JAXBElement jaxbElement = new JAXBElement<com.rackspacecloud.docs.auth.api.v1.User>(QName.valueOf("foo"), com.rackspacecloud.docs.auth.api.v1.User.class, user);
        writer.writeTo(jaxbElement, com.rackspacecloud.docs.auth.api.v1.User.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"user\":{\"id\":\"id\",\"enabled\":true,\"nastId\":\"nast\",\"mossoId\":1,\"baseURLRefs\":[],\"key\":\"key\"}}", myOut.toString());
    }

    @Test
    public void getTokenUser() throws Exception {
        UserForAuthenticateResponse user = new UserForAuthenticateResponse();
        user.setId("id");
        user.setName("name");
        user.setRoles(null);

        final ByteArrayOutputStream myOut = new ByteArrayOutputStream();
        JAXBElement jaxbElement = new JAXBElement<UserForAuthenticateResponse>(QName.valueOf("foo"), UserForAuthenticateResponse.class, user);
        writer.writeTo(jaxbElement, UserForAuthenticateResponse.class, null, null, null, null, myOut);
        Assert.assertEquals("{\"foo\":{\"name\":\"name\",\"id\":\"id\"}}", myOut.toString());
    }
}
