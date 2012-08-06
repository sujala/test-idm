package com.rackspace.idm.domain.config;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/20/12
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class JAXBXMLContextResolverTest {

    JAXBXMLContextResolver jaxbxmlContextResolver;

    @Before
    public void setUp() throws Exception {
        jaxbxmlContextResolver = new JAXBXMLContextResolver();
    }

    @Test
    public void getContext_returnsContext() throws Exception {
        assertThat("returns context",jaxbxmlContextResolver.getContext(null),instanceOf(JAXBContext.class));
    }

    @Test
    public void get_returnsContext() throws Exception {
        assertThat("returns context",jaxbxmlContextResolver.get(),instanceOf(JAXBContext.class));
    }

    @Test
    public void get_withNullContext_setsNewContext() throws Exception {
        jaxbxmlContextResolver.setContext(null);
        JAXBContext result = jaxbxmlContextResolver.get();
        assertThat("jaxbcontext", result, notNullValue());
    }
}
