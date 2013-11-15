package com.rackspace.idm.domain.config;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

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
    Configuration configuration = mock(Configuration.class);

    @Before
    public void setUp() throws Exception {
        propertyFileConfiguration = new PropertyFileConfiguration();
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
