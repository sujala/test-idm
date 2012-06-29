package com.rackspace.idm.api.config;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 8:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class IdmWadlGeneratorConfigTest {

    IdmWadlGeneratorConfig idmWadlGeneratorConfig;

    @Before
    public void setUp() throws Exception {
        idmWadlGeneratorConfig = new IdmWadlGeneratorConfig();
    }

    @Test
    public void configure_returnsList() throws Exception {
        assertThat("list",idmWadlGeneratorConfig.configure(),instanceOf(List.class));
    }
}
