package com.rackspace.idm.api.resource.cloud;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/20/12
 * Time: 5:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebClientDevWrapperTest {

    WebClientDevWrapper webClientDevWrapper;

    @Before
    public void setUp() throws Exception {
        webClientDevWrapper = new WebClientDevWrapper();
    }

    @Test
    public void wrapClient_throwsException_returnsNull() throws Exception {
        assertThat("null",WebClientDevWrapper.wrapClient(null),equalTo(null));
    }
}
