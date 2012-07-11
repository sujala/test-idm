package com.rackspacecloud.docs.auth.api.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/9/12
 * Time: 1:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createNastCredentials_returnsNewCreatedObject() throws Exception {
        NastCredentials result = objectFactory.createNastCredentials();
        assertThat("nast id",result.getNastId(), equalTo(null));
    }

    @Test
    public void createServiceUnavailableFault_returnsNewCreatedObject() throws Exception {
        ServiceUnavailableFault result = objectFactory.createServiceUnavailableFault();
        assertThat("code", result.code, equalTo(0));
    }

    @Test
    public void createMossoCredentials_returnsNewCreatedObject() throws Exception {
        MossoCredentials result = objectFactory.createMossoCredentials();
        assertThat("mosso id", result.mossoId, equalTo(0));
    }

    @Test
    public void createAuthFault_setsDetails() throws Exception {
        AuthFault result = objectFactory.createAuthFault();
        result.setDetails("details");
        assertThat("details", result.getDetails(), equalTo("details"));
    }

    @Test
    public void createFullToken_getCreated() throws Exception {
        FullToken fullToken = objectFactory.createFullToken();
        XMLGregorianCalendar result = fullToken.getCreated();
        assertThat("calendar", result, equalTo(null));
    }

    @Test
    public void createTemporaryUnavailableFault_returnsNewCreatedObject() throws Exception {
        TemporaryUnavailableFault result = objectFactory.createTemporaryUnavailableFault();
        assertThat("code", result.code, equalTo(0));
    }

    @Test
    public void createPasswordCredentials_returnsNewCreatedObject() throws Exception {
        PasswordCredentials result = objectFactory.createPasswordCredentials();
        assertThat("code", result.password, equalTo(null));
    }

    @Test
    public void createUserCredentials_returnsNewCreatedObject() throws Exception {
        UserCredentials result = objectFactory.createUserCredentials();
        assertThat("code", result.username, equalTo(null));
    }

    @Test
    public void createGroupsList_returnsNewCreatedObject() throws Exception {
        GroupsList result = objectFactory.createGroupsList();
        assertThat("code", result.group, equalTo(null));
    }

    @Test
    public void createForbiddenFault_returnsNewCreatedObject() throws Exception {
        ForbiddenFault result = objectFactory.createForbiddenFault();
        assertThat("code", result.code, equalTo(0));
    }

    @Test
    public void createService_returnsNewCreatedObject() throws Exception {
        Service result = objectFactory.createService();
        assertThat("code", result.endpoint, equalTo(null));
    }

    @Test
    public void createGroup_returnsNewCreatedObject() throws Exception {
        Group result = objectFactory.createGroup();
        assertThat("code", result.description, equalTo(null));
    }

    @Test
    public void createKeyCredentials_returnsNewCreatedObject() throws Exception {
        KeyCredentials result = objectFactory.createKeyCredentials();
        assertThat("code", result.key, equalTo(null));
    }

    @Test
    public void createEndpoint_returnsNewCreatedObject() throws Exception {
        Endpoint result = objectFactory.createEndpoint();
        assertThat("code", result.adminURL, equalTo(null));
    }

    @Test
    public void createForbidden_returnsNewJAXBElement() throws Exception {
        JAXBElement<ForbiddenFault> result = objectFactory.createForbidden(new ForbiddenFault());
        assertThat("code",result.getValue().code, equalTo(0));
    }

    @Test
    public void createTemporaryUnavailable_returnsNewJAXBElement() throws Exception {
        JAXBElement<TemporaryUnavailableFault> result = objectFactory.createTemporaryUnavailable(new TemporaryUnavailableFault());
        assertThat("code",result.getValue().code, equalTo(0));
    }

    @Test
    public void createServiceUnavailable_returnsNewJAXBElement() throws Exception {
        JAXBElement<ServiceUnavailableFault> result = objectFactory.createServiceUnavailable(new ServiceUnavailableFault());
        assertThat("code",result.getValue().code, equalTo(0));
    }
}
