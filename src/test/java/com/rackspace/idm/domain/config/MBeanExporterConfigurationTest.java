package com.rackspace.idm.domain.config;

import com.rackspace.idm.util.LdapRouterMBean;
import com.rackspace.idm.util.LoggerMBean;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/28/12
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class MBeanExporterConfigurationTest {

    Configuration config = mock(Configuration.class);
    MBeanExporterConfiguration serviceConfiguration = new MBeanExporterConfiguration();
    MBeanExporterConfiguration spy;

    @Before
    public void setUp() throws Exception {
        spy = spy(serviceConfiguration);
    }

    @Test
    public void loggerMonitoringBean_returnsLoggerMBean() throws Exception {
        assertThat("loggerMBean",serviceConfiguration.loggerMonitoringBean(),instanceOf(LoggerMBean.class));
    }

    @Test
    public void ldapRouterMonitoringBean_returnsLdapRouterMBean() throws Exception {
        assertThat("ldapRouterMBean",serviceConfiguration.ldapRouterMonitoringBean(),instanceOf(LdapRouterMBean.class));
    }

    @Test
    public void exporter_callsLoggerMonitoringBean() throws Exception {
        spy.exporter();
        verify(spy).loggerMonitoringBean();
    }

    @Test
    public void exporter_callsLdapRouterMonitoringBean() throws Exception {
        spy.exporter();
        verify(spy).ldapRouterMonitoringBean();
    }

    @Test
    public void exporter_returnsCorrectMBeanExporter() throws Exception {
        assertThat("mBeanExporter",serviceConfiguration.exporter(),instanceOf(MBeanExporter.class));
    }
}
