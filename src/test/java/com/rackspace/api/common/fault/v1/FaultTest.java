package com.rackspace.api.common.fault.v1;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/5/12
 * Time: 1:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class FaultTest {
    private Fault fault;

    @Before
    public void setUp() throws Exception {
        fault = new Fault();
        fault.setCode(0);
        fault.setDetail("detail");
        fault.setLink("link");
        fault.setMessage("message");
    }

    @Test
    public void getCode_returnsCode() throws Exception {
        int result = fault.getCode();
        assertThat("code", result, equalTo(0));
    }

    @Test
    public void getMessage_returnsMessage() throws Exception {
        String result = fault.getMessage();
        assertThat("message", result, equalTo("message"));
    }

    @Test
    public void getDetail_returnsDetail() throws Exception {
        String result = fault.getDetail();
        assertThat("detail", result, equalTo("detail"));
    }

    @Test
    public void getLink_returnsLink() throws Exception {
        String result = fault.getLink();
        assertThat("link", result, equalTo("link"));
    }
}
