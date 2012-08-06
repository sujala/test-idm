package com.rackspace.idm.util;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/23/12
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoggerMBeanTest {

    LoggerMBean loggerMBean;

    @Before
    public void setUp() throws Exception {
        loggerMBean = new LoggerMBean();
    }

    @Test
    public void getLogLevel_setLogLevel() throws Exception {
        assertThat("logger level",loggerMBean.getLogLevel(),equalTo("INFO"));
        loggerMBean.setLogLevel("WARN");
        assertThat("logger level",loggerMBean.getLogLevel(),equalTo("WARN"));
    }
}
