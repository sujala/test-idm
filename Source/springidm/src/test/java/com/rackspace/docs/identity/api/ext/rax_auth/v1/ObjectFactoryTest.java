package com.rackspace.docs.identity.api.ext.rax_auth.v1;

import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;

import javax.xml.bind.JAXBElement;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/9/12
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;
    Domain domain;
    DefaultRegionServices defaultRegionServices;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();

        domain = objectFactory.createDomain();
        defaultRegionServices = objectFactory.createDefaultRegionServices();
    }

    @Test
    public void createImpersonationRequest_returnsNewCreatedObject() throws Exception {
        ImpersonationRequest result = objectFactory.createImpersonationRequest();
        assertThat("user", result.user, equalTo(null));
    }

    @Test
    public void createImpersonator_returnsNewCreatedObject() throws Exception {
        JAXBElement<UserForAuthenticateResponse> result = objectFactory.createImpersonator(new UserForAuthenticateResponse());
        assertThat("id", result.getValue().getId(), equalTo(null));
    }

    @Test
    public void createImpersonation_returnsNewCreatedObject() throws Exception {
        JAXBElement<ImpersonationRequest> result = objectFactory.createImpersonation(new ImpersonationRequest());
        assertThat("user", result.getValue().user, equalTo(null));
    }

    @Test
    public void domain_setAndGetDescription_returnsDescription() throws Exception {
        domain.setDescription("test");
        String result = domain.getDescription();
        assertThat("description", result, equalTo("test"));
    }

    @Test
    public void domain_setAndGetId_returnsId() throws Exception {
        domain.setId("test");
        String result = domain.getId();
        assertThat("Id", result, equalTo("test"));
    }

    @Test
    public void domain_setAndGetName_returnsName() throws Exception {
        domain.setName("test");
        String result = domain.getName();
        assertThat("Name", result, equalTo("test"));
    }

    @Test
    public void domain_enabledIsNull_returnsTrue() throws Exception {
        boolean result = domain.isEnabled();
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void domain_enabledNotNull_returnsValue() throws Exception {
        domain.setEnabled(false);
        boolean result = domain.isEnabled();
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void createDomainWithDomainValue_returnsNewCreatedJAXBElementList() throws Exception {
        JAXBElement<Domain> result = objectFactory.createDomain(domain);
        assertThat("name", result.getValue().name, equalTo(null));
    }

    @Test
    public void defaultRegionServices_serviceNameIsNull_returnsEmptyList() throws Exception {
        List<String> result = defaultRegionServices.getServiceName();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void defaultRegionServices_serviceNameNotNull_returnsExistingList() throws Exception {
        List<String> serviceName = defaultRegionServices.getServiceName();
        serviceName.add("test");
        List<String> result = defaultRegionServices.getServiceName();
        assertThat("test", result.get(0), equalTo("test"));
        assertThat("list size", result.size(), equalTo(1));
    }

    @Test
    public void createDefaultRegionalServicesWithDefaultRegionServicesValue_returnsNewCreatedJAXBElementList() throws Exception {
        JAXBElement<DefaultRegionServices> result = objectFactory.createDefaultRegionServices(defaultRegionServices);
        assertThat("service name", result.getValue().serviceName, equalTo(null));
    }
}
