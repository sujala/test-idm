package com.rackspace.idm.domain.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.DataConfiguration;
import org.apache.commons.configuration.web.AppletConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.applet.Applet;
import java.util.ResourceBundle;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/28/12
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyFileConfigurationTest {

    PropertyFileConfiguration propertyFileConfiguration;
    PropertyFileConfiguration spy;
    Configuration configuration = mock(Configuration.class);

    @Before
    public void setUp() throws Exception {
        propertyFileConfiguration = new PropertyFileConfiguration();
        spy = spy(propertyFileConfiguration);
    }

    @Test
    public void getConfig_callsReadConfigFile() throws Exception {
        doReturn(configuration).when(spy).readConfigFile(anyString());
        spy.getConfig();
        verify(spy).readConfigFile(anyString());
    }

    @Test
    public void getConfig_returnsConfiguration() throws Exception {
        doReturn(configuration).when(spy).readConfigFile(anyString());
        assertThat("configuration", spy.getConfig(), instanceOf(Configuration.class));
    }

    @Test
    public void faultMessageBundle_returnsResourceBundle() throws Exception {
        assertThat("resource bundle",propertyFileConfiguration.faultMessageBundle(),instanceOf(ResourceBundle.class));
    }

    @Test
    public void getConfigurationFromClasspath_callsReadConfigFile() throws Exception {
        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(spy).readConfigFile("idm.properties");
        spy.getConfigFromClasspath();
        verify(spy).readConfigFile("idm.properties");
    }

    @Test
    public void getConfigurationFormClasspath_returnsConfiguration() throws Exception {
        doReturn(configuration).when(spy).readConfigFile("idm.properties");
        assertThat("configuration", spy.getConfigFromClasspath(), instanceOf(Configuration.class));
    }

    @Test
    public void readConfigFile_propertiesConfigurationObjectBuildFails_throwsIllegalStateException() throws Exception {
        try{
            propertyFileConfiguration.readConfigFile("foo");
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type", ex.getClass().getName(),equalTo("java.lang.IllegalStateException"));
            assertThat("exception message", ex.getMessage(),equalTo("Could not load configuration file foo"));
        }
    }
}
