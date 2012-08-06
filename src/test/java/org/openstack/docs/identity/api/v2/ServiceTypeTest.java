package org.openstack.docs.identity.api.v2;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/5/12
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceTypeTest {
    ServiceType serviceType;

    @Before
    public void setUp() throws Exception {
        serviceType = ServiceType.COMPUTE;
    }

    @Test
    public void value_returnsValue() throws Exception {
        String result = serviceType.value();
        assertThat("value", result, equalTo("compute"));
    }

    @Test
    public void fromValue_returnsValue() throws Exception {
        ServiceType result = ServiceType.fromValue("compute");
        assertThat("result", result.value(), equalTo("compute"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void fromValue_valueNotFound() throws Exception {
        ServiceType.fromValue("test");
    }
}
