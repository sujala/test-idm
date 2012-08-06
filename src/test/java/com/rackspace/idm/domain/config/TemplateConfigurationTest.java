package com.rackspace.idm.domain.config;


import freemarker.template.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 10:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class TemplateConfigurationTest {

    TemplateConfiguration templateConfiguration;

    @Before
    public void setUp() throws Exception {
        templateConfiguration = new TemplateConfiguration();
    }

    @Test
    public void freeMakerConfiguration_returnsConfiguration() throws Exception {
        assertThat("configuration",templateConfiguration.freeMakerConfiguration(),instanceOf(Configuration.class));
    }
}
