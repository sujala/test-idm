package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.entity.User;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/28/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoggerFactoryWrapperTest {

    LoggerFactoryWrapper loggerFactoryWrapper;

    @Before
    public void setUp() throws Exception {
        loggerFactoryWrapper = new LoggerFactoryWrapper();
    }

    @Test
    public void getLogger_returnsLogger() throws Exception {
        assertThat("logger",loggerFactoryWrapper.getLogger(User.class),instanceOf(Logger.class));
    }
}
