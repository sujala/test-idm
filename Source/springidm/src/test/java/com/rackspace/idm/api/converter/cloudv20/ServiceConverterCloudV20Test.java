package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.Application;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/15/12
 * Time: 10:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceConverterCloudV20Test {
    private ServiceConverterCloudV20 serviceConverterCloudV20;
    private JAXBObjectFactories jaxbObjectFactories;

    @Before
    public void setUp() throws Exception {
        serviceConverterCloudV20 = new ServiceConverterCloudV20();
        jaxbObjectFactories = new JAXBObjectFactories();
        serviceConverterCloudV20.setOBJ_FACTORIES(jaxbObjectFactories);
    }

    @Test
    public void toService_createsService_returnsCorrectInfo() throws Exception {
        Application client = new Application();
        client.setClientId("clientId");
        client.setName("name");
        client.setOpenStackType("openStackType");
        client.setDescription("description");
        Service service = serviceConverterCloudV20.toService(client);
        assertThat("id", service.getId(), equalTo("clientId"));
        assertThat("name", service.getName(), equalTo("name"));
        assertThat("type", service.getType(), equalTo("openStackType"));
        assertThat("description", service.getDescription(), equalTo("description"));
    }

    @Test
    public void toServiceList_createsServiceList_returnsCorrectInfo() throws Exception {
        List<Application> clients = new ArrayList<Application>();
        Application client = new Application();
        client.setClientId("clientId");
        client.setName("name");
        client.setOpenStackType("openStackType");
        client.setDescription("description");
        clients.add(client);
        ServiceList list = serviceConverterCloudV20.toServiceList(clients);
        assertThat("id", list.getService().get(0).getId(), equalTo("clientId"));
        assertThat("name", list.getService().get(0).getName(), equalTo("name"));
        assertThat("type", list.getService().get(0).getType(), equalTo("openStackType"));
        assertThat("description", list.getService().get(0).getDescription(), equalTo("description"));
    }

}
