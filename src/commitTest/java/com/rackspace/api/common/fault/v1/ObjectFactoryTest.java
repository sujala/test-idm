package com.rackspace.api.common.fault.v1;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/5/12
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    private ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createMethodNotAllowedFault_returnsNewObject() throws Exception {
        MethodNotAllowedFault result = objectFactory.createMethodNotAllowedFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createServiceUnavailableFault_returnsNewObject() throws Exception {
        ServiceUnavailableFault result = objectFactory.createServiceUnavailableFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createItemNotFoundFault_returnsNewObject() throws Exception {
        ItemNotFoundFault result = objectFactory.createItemNotFoundFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createUnsupportedMediaTypeFault_returnsNewObject() throws Exception {
        UnsupportedMediaTypeFault result = objectFactory.createUnsupportedMediaTypeFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createUnsupportedBadRequestFault_returnsNewObject() throws Exception {
        BadRequestFault result = objectFactory.createBadRequestFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createUnauthorizedFault_returnsNewObject() throws Exception {
        UnauthorizedFault result = objectFactory.createUnauthorizedFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createForbiddenFault_returnsNewObject() throws Exception {
        ForbiddenFault result = objectFactory.createForbiddenFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createServiceFault_returnsNewObject() throws Exception {
        ServiceFault result = objectFactory.createServiceFault();
        assertThat("fault", result.detail, equalTo(null));
    }

    @Test
    public void createFault_returnsNewObject() throws Exception {
        Fault result = objectFactory.createFault();
        assertThat("fault", result.detail, equalTo(null));
    }
}
