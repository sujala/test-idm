package com.rackspace.idm.exception;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/24/12
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExceptionHandlerTest {

    ExceptionHandler exceptionHandler;
    ExceptionHandler spy;
    JAXBObjectFactories jaxbObjectFactories = mock(JAXBObjectFactories.class);
    ObjectFactory objectFactory = mock(ObjectFactory.class);

    @Before
    public void setUp() throws Exception {
        exceptionHandler = new ExceptionHandler();
        exceptionHandler.setObjFactories(jaxbObjectFactories);
        spy = spy(exceptionHandler);
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
    }

    @Test
    public void badRequestExceptionResponse_getsOpenStackIdentityV2Factory() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.badRequestExceptionResponse("message");
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void badRequestExceptionResponse_createsBadRequestFault() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.badRequestExceptionResponse("message");
        verify(objectFactory).createBadRequestFault();
    }

    @Test
    public void badRequestExceptionResponse_setsCodeTo400() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.badRequestExceptionResponse("message");
        verify(badRequestFault).setCode(400);
    }

    @Test
    public void badRequestExceptionResponse_doesNotSetDetails() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.badRequestExceptionResponse("message");
        verify(badRequestFault, never()).setDetails(anyString());
    }

    @Test
    public void badRequestExceptionResponse_setsMessage() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.badRequestExceptionResponse("message");
        verify(badRequestFault).setMessage("message");
    }

    @Test
    public void badRequestExceptionResponse_responseStatusIs400() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        Response response = exceptionHandler.badRequestExceptionResponse("message").build();
        assertThat("response code",response.getStatus(),equalTo(400));
    }

    @Test
    public void badRequestExceptionResponse_returnsCorrectEntityInResponse() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        Response response = exceptionHandler.badRequestExceptionResponse("message").build();
        assertThat("response entity", (BadRequestFault) response.getEntity(),equalTo(badRequestFault));
    }

    @Test
    public void notAuthenticatedExceptionResponse_getsOpenStackIdentityV20Factory() throws Exception {
        UnauthorizedFault unauthorizedFault = mock(UnauthorizedFault.class);
        JAXBElement<UnauthorizedFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "unAuthorized"), UnauthorizedFault.class, null, unauthorizedFault);
        when(objectFactory.createUnauthorized(unauthorizedFault)).thenReturn(someFault);
        when(objectFactory.createUnauthorizedFault()).thenReturn(unauthorizedFault);
        exceptionHandler.notAuthenticatedExceptionResponse("not authenticated");
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void notAuthenticatedExceptionResponse_createsUnauthorizedFault() throws Exception {
        UnauthorizedFault unauthorizedFault = mock(UnauthorizedFault.class);
        JAXBElement<UnauthorizedFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "unAuthorized"), UnauthorizedFault.class, null, unauthorizedFault);
        when(objectFactory.createUnauthorized(unauthorizedFault)).thenReturn(someFault);
        when(objectFactory.createUnauthorizedFault()).thenReturn(unauthorizedFault);
        exceptionHandler.notAuthenticatedExceptionResponse("not authenticated");
        verify(objectFactory).createUnauthorizedFault();
    }

    @Test
    public void notAuthenticatedExceptionResponse_setsCodeTo401() throws Exception {
        UnauthorizedFault unauthorizedFault = mock(UnauthorizedFault.class);
        JAXBElement<UnauthorizedFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "unAuthorized"), UnauthorizedFault.class, null, unauthorizedFault);
        when(objectFactory.createUnauthorized(unauthorizedFault)).thenReturn(someFault);
        when(objectFactory.createUnauthorizedFault()).thenReturn(unauthorizedFault);
        exceptionHandler.notAuthenticatedExceptionResponse("not authenticated");
        verify(unauthorizedFault).setCode(401);
    }

    @Test
    public void notAuthenticatedExceptionResponse_setsMessage() throws Exception {
        UnauthorizedFault unauthorizedFault = mock(UnauthorizedFault.class);
        JAXBElement<UnauthorizedFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "unAuthorized"), UnauthorizedFault.class, null, unauthorizedFault);
        when(objectFactory.createUnauthorized(unauthorizedFault)).thenReturn(someFault);
        when(objectFactory.createUnauthorizedFault()).thenReturn(unauthorizedFault);
        exceptionHandler.notAuthenticatedExceptionResponse("not authenticated");
        verify(unauthorizedFault).setMessage("not authenticated");
    }

    @Test
    public void notAuthenticatedExceptionResponse_doesNotSetDetails() throws Exception {
        UnauthorizedFault unauthorizedFault = mock(UnauthorizedFault.class);
        JAXBElement<UnauthorizedFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "unAuthorized"), UnauthorizedFault.class, null, unauthorizedFault);
        when(objectFactory.createUnauthorized(unauthorizedFault)).thenReturn(someFault);
        when(objectFactory.createUnauthorizedFault()).thenReturn(unauthorizedFault);
        exceptionHandler.notAuthenticatedExceptionResponse("not authenticated");
        verify(unauthorizedFault,never()).setDetails(anyString());
    }

    @Test
    public void notAuthenticatedExceptionResponse_responseCodeIs401() throws Exception {
        UnauthorizedFault unauthorizedFault = mock(UnauthorizedFault.class);
        JAXBElement<UnauthorizedFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "unAuthorized"), UnauthorizedFault.class, null, unauthorizedFault);
        when(objectFactory.createUnauthorized(unauthorizedFault)).thenReturn(someFault);
        when(objectFactory.createUnauthorizedFault()).thenReturn(unauthorizedFault);
        Response response = exceptionHandler.notAuthenticatedExceptionResponse("not authenticated").build();
        assertThat("response code",response.getStatus(),equalTo(401));
    }

    @Test
    public void notAuthenticatedExceptionResponse_correctEntity() throws Exception {
        UnauthorizedFault unauthorizedFault = mock(UnauthorizedFault.class);
        JAXBElement<UnauthorizedFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "unAuthorized"), UnauthorizedFault.class, null, unauthorizedFault);
        when(objectFactory.createUnauthorized(unauthorizedFault)).thenReturn(someFault);
        when(objectFactory.createUnauthorizedFault()).thenReturn(unauthorizedFault);
        Response response = exceptionHandler.notAuthenticatedExceptionResponse("not authenticated").build();
        assertThat("response entity",(UnauthorizedFault) response.getEntity(),equalTo(unauthorizedFault));
    }

    @Test
    public void forbiddenExceptionResponse_getsOpenstackIdentityV2Factory() throws Exception {
        ForbiddenFault forbiddenFault = mock(ForbiddenFault.class);
        JAXBElement<ForbiddenFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "forbidden"), ForbiddenFault.class, null, forbiddenFault);
        when(objectFactory.createForbidden(forbiddenFault)).thenReturn(someFault);
        when(objectFactory.createForbiddenFault()).thenReturn(forbiddenFault);
        exceptionHandler.forbiddenExceptionResponse("forbidden fault");
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void forbiddenExceptionResponse_createsForbiddenFault() throws Exception {
        ForbiddenFault forbiddenFault = mock(ForbiddenFault.class);
        JAXBElement<ForbiddenFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "forbidden"), ForbiddenFault.class, null, forbiddenFault);
        when(objectFactory.createForbidden(forbiddenFault)).thenReturn(someFault);
        when(objectFactory.createForbiddenFault()).thenReturn(forbiddenFault);
        exceptionHandler.forbiddenExceptionResponse("forbidden fault");
        verify(objectFactory).createForbiddenFault();
    }

    @Test
    public void forbiddenExceptionResponse_returns403() throws Exception {
        ForbiddenFault forbiddenFault = mock(ForbiddenFault.class);
        JAXBElement<ForbiddenFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "forbidden"), ForbiddenFault.class, null, forbiddenFault);
        when(objectFactory.createForbidden(forbiddenFault)).thenReturn(someFault);
        when(objectFactory.createForbiddenFault()).thenReturn(forbiddenFault);
        exceptionHandler.forbiddenExceptionResponse("not allowed");
        verify(forbiddenFault).setCode(403);
    }

    @Test
    public void forbiddenExceptionResponse_setsMessage() throws Exception {
        ForbiddenFault forbiddenFault = mock(ForbiddenFault.class);
        JAXBElement<ForbiddenFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "forbidden"), ForbiddenFault.class, null, forbiddenFault);
        when(objectFactory.createForbidden(forbiddenFault)).thenReturn(someFault);
        when(objectFactory.createForbiddenFault()).thenReturn(forbiddenFault);
        exceptionHandler.forbiddenExceptionResponse("forbidden fault");
        verify(forbiddenFault).setMessage("forbidden fault");
    }

    @Test
    public void forbiddenExceptionResponse_doesNotSetDetails() throws Exception {
        ForbiddenFault forbiddenFault = mock(ForbiddenFault.class);
        JAXBElement<ForbiddenFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "forbidden"), ForbiddenFault.class, null, forbiddenFault);
        when(objectFactory.createForbidden(forbiddenFault)).thenReturn(someFault);
        when(objectFactory.createForbiddenFault()).thenReturn(forbiddenFault);
        exceptionHandler.forbiddenExceptionResponse("forbidden fault");
        verify(forbiddenFault,never()).setDetails(anyString());
    }

    @Test
    public void forbiddenExceptionResponse_responseCodeIs403() throws Exception {
        ForbiddenFault forbiddenFault = mock(ForbiddenFault.class);
        JAXBElement<ForbiddenFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "forbidden"), ForbiddenFault.class, null, forbiddenFault);
        when(objectFactory.createForbidden(forbiddenFault)).thenReturn(someFault);
        when(objectFactory.createForbiddenFault()).thenReturn(forbiddenFault);
        Response response = exceptionHandler.forbiddenExceptionResponse("forbidden fault").build();
        assertThat("response code",response.getStatus(),equalTo(403));
    }

    @Test
    public void forbiddenExceptionResponse_correctEntity() throws Exception {
        ForbiddenFault forbiddenFault = mock(ForbiddenFault.class);
        JAXBElement<ForbiddenFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "forbidden"), ForbiddenFault.class, null, forbiddenFault);
        when(objectFactory.createForbidden(forbiddenFault)).thenReturn(someFault);
        when(objectFactory.createForbiddenFault()).thenReturn(forbiddenFault);
        Response response = exceptionHandler.forbiddenExceptionResponse("forbidden fault").build();
        assertThat("response entity", (ForbiddenFault) response.getEntity(),equalTo(forbiddenFault));
    }

    @Test
    public void notFoundExceptionResponse_getsOpenstackIdentityV2Factory() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        exceptionHandler.notFoundExceptionResponse("item not found");
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void notFoundExceptionResponse_createsItemNotFoundFault() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        exceptionHandler.notFoundExceptionResponse("item not found");
        verify(objectFactory).createItemNotFoundFault();
    }

    @Test
    public void notFoundExceptionResponse_setsCodeTo404() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        exceptionHandler.notFoundExceptionResponse("item not found");
        verify(itemNotFoundFault).setCode(404);
    }

    @Test
    public void notFoundExceptionResponse_setsMessage() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        exceptionHandler.notFoundExceptionResponse("item not found");
        verify(itemNotFoundFault).setMessage("item not found");
    }

    @Test
    public void notFoundExceptionResponse_doesNotSetDetails() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        exceptionHandler.notFoundExceptionResponse("item not found");
        verify(itemNotFoundFault,never()).setDetails(anyString());
    }

    @Test
    public void notFoundExceptionResponse_setsResponseCode() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        Response response = exceptionHandler.notFoundExceptionResponse("item not found").build();
        assertThat("response code", response.getStatus(),equalTo(404));
    }

    @Test
    public void notFoundExceptionResponse_correctEntity() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        Response response = exceptionHandler.notFoundExceptionResponse("item not found").build();
        assertThat("response code", (ItemNotFoundFault) response.getEntity(),equalTo(itemNotFoundFault));
    }

    @Test
    public void tenantConflictExceptionResponse_getsOpenstackIdentityV2Factory() throws Exception {
        TenantConflictFault tenantConflictFault = mock(TenantConflictFault.class);
        JAXBElement<TenantConflictFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenantConflict"), TenantConflictFault.class, null, tenantConflictFault);
        when(objectFactory.createTenantConflict(tenantConflictFault)).thenReturn(someFault);
        when(objectFactory.createTenantConflictFault()).thenReturn(tenantConflictFault);
        exceptionHandler.tenantConflictExceptionResponse("tenant conflict");
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void tenantConflictExceptionResponse_createsTenantConflictFault() throws Exception {
        TenantConflictFault tenantConflictFault = mock(TenantConflictFault.class);
        JAXBElement<TenantConflictFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenantConflict"), TenantConflictFault.class, null, tenantConflictFault);
        when(objectFactory.createTenantConflict(tenantConflictFault)).thenReturn(someFault);
        when(objectFactory.createTenantConflictFault()).thenReturn(tenantConflictFault);
        exceptionHandler.tenantConflictExceptionResponse("tenant conflict");
        verify(objectFactory).createTenantConflictFault();
    }

    @Test
    public void tenantConflictExceptionResponse_setsCodeTo409() throws Exception {
        TenantConflictFault tenantConflictFault = mock(TenantConflictFault.class);
        JAXBElement<TenantConflictFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenantConflict"), TenantConflictFault.class, null, tenantConflictFault);
        when(objectFactory.createTenantConflict(tenantConflictFault)).thenReturn(someFault);
        when(objectFactory.createTenantConflictFault()).thenReturn(tenantConflictFault);
        exceptionHandler.tenantConflictExceptionResponse("tenant conflict");
        verify(tenantConflictFault).setCode(409);
    }

    @Test
    public void tenantConflictExceptionResponse_setsMessage() throws Exception {
        TenantConflictFault tenantConflictFault = mock(TenantConflictFault.class);
        JAXBElement<TenantConflictFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenantConflict"), TenantConflictFault.class, null, tenantConflictFault);
        when(objectFactory.createTenantConflict(tenantConflictFault)).thenReturn(someFault);
        when(objectFactory.createTenantConflictFault()).thenReturn(tenantConflictFault);
        exceptionHandler.tenantConflictExceptionResponse("responseMessage");
        verify(tenantConflictFault).setMessage("responseMessage");
    }

    @Test
    public void tenantConflictExceptionResponse_doesNotSetDetails() throws Exception {
        TenantConflictFault tenantConflictFault = mock(TenantConflictFault.class);
        JAXBElement<TenantConflictFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenantConflict"), TenantConflictFault.class, null, tenantConflictFault);
        when(objectFactory.createTenantConflict(tenantConflictFault)).thenReturn(someFault);
        when(objectFactory.createTenantConflictFault()).thenReturn(tenantConflictFault);
        exceptionHandler.tenantConflictExceptionResponse("responseMessage");
        verify(tenantConflictFault,never()).setDetails(anyString());
    }

    @Test
    public void tenantConflictExceptionResponse_responseCodeIs409() throws Exception {
        TenantConflictFault tenantConflictFault = mock(TenantConflictFault.class);
        JAXBElement<TenantConflictFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenantConflict"), TenantConflictFault.class, null, tenantConflictFault);
        when(objectFactory.createTenantConflict(tenantConflictFault)).thenReturn(someFault);
        when(objectFactory.createTenantConflictFault()).thenReturn(tenantConflictFault);
        Response response = exceptionHandler.tenantConflictExceptionResponse("responseMessage").build();
        assertThat("response code",response.getStatus(),equalTo(409));
    }

    @Test
    public void tenantConflictExceptionResponse_correctEntity() throws Exception {
        TenantConflictFault tenantConflictFault = mock(TenantConflictFault.class);
        JAXBElement<TenantConflictFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenantConflict"), TenantConflictFault.class, null, tenantConflictFault);
        when(objectFactory.createTenantConflict(tenantConflictFault)).thenReturn(someFault);
        when(objectFactory.createTenantConflictFault()).thenReturn(tenantConflictFault);
        Response response = exceptionHandler.tenantConflictExceptionResponse("responseMessage").build();
        assertThat("response code", (TenantConflictFault) response.getEntity(),equalTo(tenantConflictFault));
    }

    @Test
    public void userDisabledExceptionResponse_getsOpenstackIdentityV2Factory() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        exceptionHandler.userDisabledExceptionResponse("responseMessage");
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void userDisabledExceptionResponse_createsUserDisabledFault() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        exceptionHandler.userDisabledExceptionResponse("responseMessage");
        verify(objectFactory).createUserDisabledFault();
    }

    @Test
    public void userDisabledExceptionResponse_setsCodeTo403() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        exceptionHandler.userDisabledExceptionResponse("responseMessage");
        verify(userDisabledFault).setCode(403);
    }

    @Test
    public void userDisabledExceptionResponse_setsMessage() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        exceptionHandler.userDisabledExceptionResponse("userName");
        verify(userDisabledFault).setMessage("userName");
    }

    @Test
    public void userDisabledExceptionResponse_doesNotSetDetails() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        exceptionHandler.userDisabledExceptionResponse("responseMessage");
        verify(userDisabledFault,never()).setDetails(anyString());
    }

    @Test
    public void userDisabledExceptionResponse_responseCodeIs403() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        Response response = exceptionHandler.userDisabledExceptionResponse("responseMessage").build();
        assertThat("response code", response.getStatus(),equalTo(403));
    }

    @Test
    public void userDisabledExceptionResponse_correctEntity() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        Response response = exceptionHandler.userDisabledExceptionResponse("responseMessage").build();
        assertThat("response code", (UserDisabledFault) response.getEntity(),equalTo(userDisabledFault));
    }

    @Test
    public void conflictExceptionResponse_getsOpenstackIdentityV2Factory() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.conflictExceptionResponse("user conflict");
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void conflictExceptionResponse_createsBadRequestFault() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.conflictExceptionResponse("user conflict");
        verify(objectFactory).createBadRequestFault();
    }

    @Test
    public void conflictExceptionResponse_setsCodeTo409() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.conflictExceptionResponse("user conflict");
        verify(badRequestFault).setCode(409);
    }

    @Test
    public void conflictExceptionResponse_setsMessage() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.conflictExceptionResponse("responseMessage");
        verify(badRequestFault).setMessage("responseMessage");
    }

    @Test
    public void conflictExceptionResponse_doesNotSetDetails() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        exceptionHandler.conflictExceptionResponse("responseMessage");
        verify(badRequestFault,never()).setDetails("responseMessage");
    }

    @Test
    public void conflictExceptionResponse_responseIs409() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        Response response = exceptionHandler.conflictExceptionResponse("responseMessage").build();
        assertThat("response code",response.getStatus(),equalTo(409));
    }

    @Test
    public void conflictExceptionResponse_correctEntity() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        Response response = exceptionHandler.conflictExceptionResponse("responseMessage").build();
        assertThat("response entity", (BadRequestFault) response.getEntity(),equalTo(badRequestFault));
    }

    @Test
    public void serviceExceptionResponse_getsOpenstackIdentityV2Factory() throws Exception {
        IdentityFault identityFault = mock(IdentityFault.class);
        JAXBElement<IdentityFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "identity"), IdentityFault.class, null, identityFault);
        when(objectFactory.createIdentityFault(identityFault)).thenReturn(someFault);
        when(objectFactory.createIdentityFault()).thenReturn(identityFault);
        exceptionHandler.serviceExceptionResponse();
        verify(jaxbObjectFactories,times(2)).getOpenStackIdentityV2Factory();
    }

    @Test
    public void serviceExceptionResponse_createsIdentityFault() throws Exception {
        IdentityFault identityFault = mock(IdentityFault.class);
        JAXBElement<IdentityFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "identity"), IdentityFault.class, null, identityFault);
        when(objectFactory.createIdentityFault(identityFault)).thenReturn(someFault);
        when(objectFactory.createIdentityFault()).thenReturn(identityFault);
        exceptionHandler.serviceExceptionResponse();
        verify(objectFactory).createIdentityFault();
    }

    @Test
    public void serviceExceptionResponse_setsCodeTo500() throws Exception {
        IdentityFault identityFault = mock(IdentityFault.class);
        JAXBElement<IdentityFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "identity"), IdentityFault.class, null, identityFault);
        when(objectFactory.createIdentityFault(identityFault)).thenReturn(someFault);
        when(objectFactory.createIdentityFault()).thenReturn(identityFault);
        exceptionHandler.serviceExceptionResponse();
        verify(identityFault).setCode(500);
    }

    @Test
    public void serviceExceptionResponse_doesNotSetMessage() throws Exception {
        IdentityFault identityFault = mock(IdentityFault.class);
        JAXBElement<IdentityFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "identity"), IdentityFault.class, null, identityFault);
        when(objectFactory.createIdentityFault(identityFault)).thenReturn(someFault);
        when(objectFactory.createIdentityFault()).thenReturn(identityFault);
        exceptionHandler.serviceExceptionResponse();
        verify(identityFault,never()).setMessage(anyString());
    }

    @Test
    public void serviceExceptionResponse_doesNotSetDetails() throws Exception {
        IdentityFault identityFault = mock(IdentityFault.class);
        JAXBElement<IdentityFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "identity"), IdentityFault.class, null, identityFault);
        when(objectFactory.createIdentityFault(identityFault)).thenReturn(someFault);
        when(objectFactory.createIdentityFault()).thenReturn(identityFault);
        exceptionHandler.serviceExceptionResponse();
        verify(identityFault,never()).setDetails(anyString());
    }

    @Test
    public void exceptionResponse_whenBadRequestException_callsBadRequestExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).badRequestExceptionResponse("bad request");
        spy.exceptionResponse(new BadRequestException("bad request"));
        verify(spy).badRequestExceptionResponse("bad request");
    }

    @Test
    public void exceptionResponse_whenBadRequestException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).badRequestExceptionResponse("bad request");
        assertThat("response builder", spy.exceptionResponse(new BadRequestException("bad request")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenStalePasswordException_callsBadRequestExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).badRequestExceptionResponse("Wrong Password");
        spy.exceptionResponse(new StalePasswordException("Wrong Password"));
        verify(spy).badRequestExceptionResponse("Wrong Password");
    }

    @Test
    public void exceptionResponse_whenStalePasswordException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).badRequestExceptionResponse("Wrong Password");
        assertThat("response builder", spy.exceptionResponse(new StalePasswordException("Wrong Password")),instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenNotAuthorizedException_callsNotAuthenticatedExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).notAuthenticatedExceptionResponse("not authorized");
        spy.exceptionResponse(new NotAuthorizedException("not authorized"));
        verify(spy).notAuthenticatedExceptionResponse("not authorized");
    }

    @Test
    public void exceptionResponse_whenNotAuthorizedException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).notAuthenticatedExceptionResponse("not authorized");
        assertThat("response builder", spy.exceptionResponse(new NotAuthorizedException("not authorized")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenNotAuthenticatedException_callsNotAuthenticatedExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).notAuthenticatedExceptionResponse("not authenticated");
        spy.exceptionResponse(new NotAuthenticatedException("not authenticated"));
        verify(spy).notAuthenticatedExceptionResponse("not authenticated");
    }

    @Test
    public void exceptionResponse_whenNotAuthenticatedException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).notAuthenticatedExceptionResponse("not authenticated");
        assertThat("response builder", spy.exceptionResponse(new NotAuthenticatedException("not authenticated")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenForbiddenException_callsForbiddenExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).forbiddenExceptionResponse("forbidden");
        spy.exceptionResponse(new ForbiddenException("forbidden"));
        verify(spy).forbiddenExceptionResponse("forbidden");
    }

    @Test
    public void exceptionResponse_whenForbiddenException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).forbiddenExceptionResponse("forbidden");
        assertThat("response builder", spy.exceptionResponse(new ForbiddenException("forbidden")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenNotFoundException_callsNotFoundExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).notFoundExceptionResponse("not found");
        spy.exceptionResponse(new NotFoundException("not found"));
        verify(spy).notFoundExceptionResponse("not found");
    }

    @Test
    public void exceptionResponse_whenNotFoundException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).notFoundExceptionResponse("not found");
        assertThat("response builder", spy.exceptionResponse(new NotFoundException("not found")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenClientConflictException_callsTenantConflictExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).tenantConflictExceptionResponse("tenant conflict");
        spy.exceptionResponse(new ClientConflictException("tenant conflict"));
        verify(spy).tenantConflictExceptionResponse("tenant conflict");
    }

    @Test
    public void exceptionResponse_whenClientConflictException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).tenantConflictExceptionResponse("tenant conflict");
        assertThat("response builder", spy.exceptionResponse(new ClientConflictException("tenant conflict")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenUserDisabledException_callsUserDisabledExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).userDisabledExceptionResponse("user disabled");
        spy.exceptionResponse(new UserDisabledException("user disabled"));
        verify(spy).userDisabledExceptionResponse("user disabled");
    }

    @Test
    public void exceptionResponse_whenUserDisabledException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).userDisabledExceptionResponse("user disabled");
        assertThat("response builder", spy.exceptionResponse(new UserDisabledException("user disabled")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenDuplicateUsernameException_callsConflictExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).conflictExceptionResponse("duplicate username");
        spy.exceptionResponse(new DuplicateUsernameException("duplicate username"));
        verify(spy).conflictExceptionResponse("duplicate username");
    }

    @Test
    public void exceptionResponse_whenDuplicateUsernameException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).conflictExceptionResponse("duplicate username");
        assertThat("response builder", spy.exceptionResponse(new DuplicateUsernameException("duplicate username")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenBaseUrlConflictException_callsConflictExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).conflictExceptionResponse("base url conflict");
        spy.exceptionResponse(new BaseUrlConflictException("base url conflict"));
        verify(spy).conflictExceptionResponse("base url conflict");
    }

    @Test
    public void exceptionResponse_whenBaseUrlConflictException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).conflictExceptionResponse("base url conflict");
        assertThat("response builder", spy.exceptionResponse(new BaseUrlConflictException("base url conflict")), instanceOf(ResponseBuilderImpl.class));
    }

    @Test
    public void exceptionResponse_whenUnspecifiedException_callsServiceExceptionResponse() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).serviceExceptionResponse();
        spy.exceptionResponse(new NullPointerException());
        verify(spy).serviceExceptionResponse();
    }

    @Test
    public void exceptionResponse_whenUnspecifiedException_returnsResponseBuilder() throws Exception {
        doReturn(new ResponseBuilderImpl()).when(spy).serviceExceptionResponse();
        assertThat("response builder", spy.exceptionResponse(new NullPointerException()), instanceOf(ResponseBuilderImpl.class));
    }
}
