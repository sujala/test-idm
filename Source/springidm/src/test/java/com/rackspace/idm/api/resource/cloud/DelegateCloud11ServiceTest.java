package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.v11.DefaultCloud11Service;
import com.rackspace.idm.api.resource.cloud.v11.DelegateCloud11Service;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/30/11
 * Time: 9:19 PM
 */
public class DelegateCloud11ServiceTest {
    DelegateCloud11Service delegateCloud11Service;
    DefaultCloud11Service defaultCloud11Service;
    com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY;
    Configuration config;
    CloudClient cloudClient;
    Marshaller marshaller;

    @Before
    public void setUp() throws JAXBException {
        delegateCloud11Service = new DelegateCloud11Service();
        defaultCloud11Service = mock(DefaultCloud11Service.class);
        delegateCloud11Service.setDefaultCloud11Service(defaultCloud11Service);
        OBJ_FACTORY = mock(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
        DelegateCloud11Service.setOBJ_FACTORY(OBJ_FACTORY);
        config = mock(Configuration.class);
        delegateCloud11Service.setConfig(config);
        cloudClient = mock(CloudClient.class);
        delegateCloud11Service.setCloudClient(cloudClient);
        marshaller = mock(Marshaller.class);
        delegateCloud11Service.setMarshaller(marshaller);
        when(config.getString("cloudAuth11url")).thenReturn("url");
    }

    @Test
    public void setUserEnabled_callsOBJ_FACTORY_createUser() throws Exception {
        UserWithOnlyEnabled user = new UserWithOnlyEnabled();
        user.setEnabled(true);
        when(defaultCloud11Service.setUserEnabled("testId",user,null)).thenReturn(new ResponseBuilderImpl().status(404));
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(QName.valueOf("name"),User.class,null));
        delegateCloud11Service.setUserEnabled("testId", user, null);
        verify(OBJ_FACTORY).createUser(Matchers.<User>any());
    }
}
