package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.api.idm.v1.ApplicationList;
import com.rackspace.api.idm.v1.ApplicationSecretCredentials;
import com.rackspace.idm.domain.entity.Applications;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/8/12
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationConverterTest {

    private ApplicationConverter applicationConverter;
    //TODO: give these sensical names. application and client are used interchangably in this class!
    com.rackspace.idm.domain.entity.Application clientDO;
    Application client;


    @Before
    public void setUp() throws Exception {
        applicationConverter = new ApplicationConverter(new RolesConverter());
        clientDO = new com.rackspace.idm.domain.entity.Application();
        client = new Application();
    }

    @Test
    public void toClientDo_withClient_setsClientId() throws Exception {
        client.setClientId("clientId");
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("client id", application.getClientId(), equalTo("clientId"));
    }

    @Test
    public void toClientDo_withClient_setsRCN() throws Exception {
        client.setCustomerId("customerId");
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("RCN", application.getRCN(), equalTo("customerId"));
    }

    @Test
    public void toClientDo_withClient_setsName() throws Exception {
        client.setName("name");
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("name", application.getName(), equalTo("name"));
    }

    @Test
    public void toClientDo_withClient_setsCallBackUrl() throws Exception {
        client.setCallBackUrl("callBackUrl");
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("callback Url", application.getCallBackUrl(), equalTo("callBackUrl"));
    }

    @Test
    public void toClientDo_withClient_setsTitle() throws Exception {
        client.setTitle("title");
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("title", application.getTitle(), equalTo("title"));
    }

    @Test
    public void toClientDo_withClient_setsDescription() throws Exception {
        client.setDescription("description");
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("description", application.getDescription(), equalTo("description"));
    }

    @Test
    public void toClientDo_withClient_setsScope() throws Exception {
        client.setScope("scope");
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("scope", application.getScope(), equalTo("scope"));
    }

    @Test
    public void toClientDo_withClient_setsEnabled() throws Exception {
        client.setEnabled(true);
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("enabled", application.isEnabled(), equalTo(true));
    }

    @Test
    public void toClientDo_withClient_withNullEnabled_DoesNotSetEnabled() throws Exception {
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("enabled", application.isEnabled(), nullValue());
    }

    @Test
    public void toClientDo_withClient_setClientSecret() throws Exception {
        ApplicationSecretCredentials applicationSecretCredentials = new ApplicationSecretCredentials();
        applicationSecretCredentials.setClientSecret("clientSecret");
        client.setSecretCredentials(applicationSecretCredentials);
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("client secret", application.getClientSecret(), equalTo("clientSecret"));
    }

    @Test
    public void toClientDo_withClient_withNullSecretCredentials_doesNotSetClientSecret() throws Exception {
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("client secret", application.getClientSecretObj(), nullValue());
    }

    @Test
    public void toClientDo_withClient_withNullClientSecret_doesNotSetClientSecret() throws Exception {
        com.rackspace.idm.domain.entity.Application application = applicationConverter.toClientDO(client);
        assertThat("client Secret", application.getClientSecretObj(), nullValue());
    }

    @Test
    public void toClientListJaxb_withEmptyClients() throws Exception {
        Applications applications = new Applications();
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toClientListJaxb(applications);
        assertThat("applicationList", applicationListJAXBElement, nullValue());
    }

    @Test
    public void toClientListJaxb_withNullApplication_returnsNull() throws Exception {
        Applications applications = new Applications();
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toClientListJaxb(applications);
        assertThat("applicationList", applicationListJAXBElement, nullValue());
    }

    @Test
    public void toClientListJaxb_withClients_returnsApplicationListJaxb() throws Exception {
        Applications applications = new Applications();
        List<com.rackspace.idm.domain.entity.Application> clients = new ArrayList<com.rackspace.idm.domain.entity.Application>();
        clients.add(new com.rackspace.idm.domain.entity.Application());
        applications.setClients(clients);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toClientListJaxb(applications);
        assertThat("applicationList", applicationListJAXBElement.getDeclaredType(), equalTo(ApplicationList.class));
    }

    @Test
    public void toClientListJaxb_withOneClient() throws Exception {
        Applications applications = new Applications();
        List<com.rackspace.idm.domain.entity.Application> clients = new ArrayList<com.rackspace.idm.domain.entity.Application>();
        clients.add(new com.rackspace.idm.domain.entity.Application());
        applications.setClients(clients);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toClientListJaxb(applications);
        assertThat("applicationList size", applicationListJAXBElement.getValue().getApplication().size(), equalTo(1));
    }

    @Test
    public void toClientListJaxb_withClients_setsLimit() throws Exception {
        Applications applications = new Applications();
        applications.setClients(new ArrayList<com.rackspace.idm.domain.entity.Application>());
        applications.setLimit(123);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toClientListJaxb(applications);
        assertThat("applicationList limit", applicationListJAXBElement.getValue().getLimit(), equalTo(123));
    }

    @Test
    public void toClientListJaxb_withClients_setsOffset() throws Exception {
        Applications applications = new Applications();
        applications.setClients(new ArrayList<com.rackspace.idm.domain.entity.Application>());
        applications.setOffset(123);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toClientListJaxb(applications);
        assertThat("applicationList offset", applicationListJAXBElement.getValue().getOffset(), equalTo(123));
    }

    @Test
    public void toClientListJaxb_withClients_setsTotalRecords() throws Exception {
        Applications applications = new Applications();
        applications.setClients(new ArrayList<com.rackspace.idm.domain.entity.Application>());
        applications.setTotalRecords(123);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toClientListJaxb(applications);
        assertThat("applicationList totalRecords", applicationListJAXBElement.getValue().getTotalRecords(), equalTo(123));
    }

    @Test
    public void toApplicationJaxbMin_withNull_returnsNull() throws Exception {
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toApplicationJaxbMin(null);
        assertThat("applications", applicationListJAXBElement, nullValue());
    }

    @Test
    public void toApplicationJaxbMin_withNullGetClients_returnsNull() throws Exception {
        Applications applications = new Applications();
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toApplicationJaxbMin(applications);
        assertThat("applications", applicationListJAXBElement, nullValue());
    }

    @Test
    public void toApplicationJaxbMin_withApplications_returnsJaxbApplicationList() throws Exception {
        Applications applications = new Applications();
        applications.setClients(new ArrayList<com.rackspace.idm.domain.entity.Application>());
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toApplicationJaxbMin(applications);
        assertThat("applications returned", applicationListJAXBElement.getDeclaredType(), equalTo(ApplicationList.class));
    }

    @Test
    public void toApplicationJaxbMin_withThreeApplications_returnsApplicationListSizeThree() throws Exception {
        Applications applications = new Applications();
        ArrayList<com.rackspace.idm.domain.entity.Application> clients = new ArrayList<com.rackspace.idm.domain.entity.Application>();
        clients.add(new com.rackspace.idm.domain.entity.Application());
        clients.add(new com.rackspace.idm.domain.entity.Application());
        clients.add(new com.rackspace.idm.domain.entity.Application());
        applications.setClients(clients);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toApplicationJaxbMin(applications);
        assertThat("applications size", applicationListJAXBElement.getValue().getApplication().size(), equalTo(3));
    }

    @Test
    public void toApplicationJaxbMin_withApplications_setsLimit() throws Exception {
        Applications applications = new Applications();
        applications.setClients(new ArrayList<com.rackspace.idm.domain.entity.Application>());
        applications.setLimit(10);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toApplicationJaxbMin(applications);
        assertThat("applications limit", applicationListJAXBElement.getValue().getLimit(), equalTo(10));
    }

    @Test
    public void toApplicationJaxbMin_withOffset_ApplicationsSetOffset() throws Exception {
        Applications applications = new Applications();
        applications.setClients(new ArrayList<com.rackspace.idm.domain.entity.Application>());
        applications.setOffset(27);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toApplicationJaxbMin(applications);
        assertThat("applications offset", applicationListJAXBElement.getValue().getOffset(), equalTo(27));
    }

    @Test
    public void toApplicationJaxbMin_withApplications_setTotalRecords() throws Exception {
        Applications applications = new Applications();
        applications.setClients(new ArrayList<com.rackspace.idm.domain.entity.Application>());
        applications.setTotalRecords(15);
        JAXBElement<ApplicationList> applicationListJAXBElement = applicationConverter.toApplicationJaxbMin(applications);
        assertThat("applications total records", applicationListJAXBElement.getValue().getTotalRecords(), equalTo(15));
    }

    @Test
    public void toCLientJaxbMin_withClient_setsClientId() throws Exception {
        clientDO.setClientId("clientId");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxbMin(clientDO);
        assertThat("application client id", applicationJAXBElement.getValue().getClientId(), equalTo("clientId"));
    }

    @Test
    public void toCLientJaxbMin_setsCustomerId() throws Exception {
        clientDO.setRCN("customerId");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxbMin(clientDO);
        assertThat("application customer id", applicationJAXBElement.getValue().getCustomerId(), equalTo("customerId"));
    }

    @Test
    public void toCLientJaxbMin_setsName() throws Exception {
        clientDO.setName("name");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxbMin(clientDO);
        assertThat("application name", applicationJAXBElement.getValue().getName(), equalTo("name"));
    }

    @Test
    public void toCLientJaxbMin_setsDescription() throws Exception {
        clientDO.setDescription("description");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxbMin(clientDO);
        assertThat("application description", applicationJAXBElement.getValue().getDescription(), equalTo("description"));
    }

    @Test
    public void toClientJaxb_withClient_setsClientId() throws Exception {
        clientDO.setClientId("clientId");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("client id", applicationJAXBElement.getValue().getClientId(), equalTo("clientId"));
    }

    @Test
    public void toClientJaxb_withClient_setsCustomerId() throws Exception {
        clientDO.setRCN("customerId");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("customer id", applicationJAXBElement.getValue().getCustomerId(), equalTo("customerId"));
    }

    @Test
    public void toClientJaxb_withClient_setsEnabled() throws Exception {
        clientDO.setEnabled(true);
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("enabled", applicationJAXBElement.getValue().isEnabled(), equalTo(true));
    }

    @Test
    public void toClientJaxb_withClient_setsName() throws Exception {
        clientDO.setName("name");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("name", applicationJAXBElement.getValue().getName(), equalTo("name"));
    }

    @Test
    public void toClientJaxb_withClient_setsCallBackUrl() throws Exception {
        clientDO.setCallBackUrl("callBackUrl");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("callback url", applicationJAXBElement.getValue().getCallBackUrl(), equalTo("callBackUrl"));
    }

    @Test
    public void toClientJaxb_withClient_setsTitle() throws Exception {
        clientDO.setTitle("title");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("title", applicationJAXBElement.getValue().getTitle(), equalTo("title"));
    }

    @Test
    public void toClientJaxb_withClient_setsDescription() throws Exception {
        clientDO.setDescription("description");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("description", applicationJAXBElement.getValue().getDescription(), equalTo("description"));
    }

    @Test
    public void toClientJaxb_withClient_setsScope() throws Exception {
        clientDO.setScope("scope");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("scope", applicationJAXBElement.getValue().getScope(), equalTo("scope"));
    }

    @Test
    public void toClientJaxb_withClientWithSecretCredAndIncludeCreds_setsSecretCredentials() throws Exception {
        clientDO.setClientSecret("clientSecret");
        JAXBElement<Application> applicationJAXBElement = applicationConverter.toClientJaxb(clientDO, false);
        assertThat("client secret", applicationJAXBElement.getValue().getSecretCredentials(), not(nullValue()));
        assertThat("client secret", applicationJAXBElement.getValue().getSecretCredentials().getClientSecret(), equalTo("clientSecret"));
    }

}
