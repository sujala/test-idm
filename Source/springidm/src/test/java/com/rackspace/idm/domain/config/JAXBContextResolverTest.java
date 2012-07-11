package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.entity.User;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;


/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class JAXBContextResolverTest {

    JAXBContextResolver jaxbContextResolver;

    @Before
    public void setUp() throws Exception {
        jaxbContextResolver = new JAXBContextResolver();
    }

    @Test
    public void getContext_returnsJAXBContext() throws Exception {
        assertThat("jaxbContext",jaxbContextResolver.getContext(User.class),instanceOf(JAXBContext.class));
    }
}
