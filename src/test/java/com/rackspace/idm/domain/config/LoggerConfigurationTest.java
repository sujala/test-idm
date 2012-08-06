package com.rackspace.idm.domain.config;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 10:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoggerConfigurationTest {

    LoggerConfiguration loggerConfiguration;

    @Before
    public void setUp() throws Exception {
        loggerConfiguration = new LoggerConfiguration();
    }

    @Test
    public void getLoggerFactoryWrapper_returnsLoggerFactoryWrapper() throws Exception {
        assertThat("logger factory wrapper", loggerConfiguration.getLoggerFactoryWrapper(),instanceOf(LoggerFactoryWrapper.class));
    }
}
