package com.rackspace.idm.domain.config.providers;

import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.common.api.v1.MediaType;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/18/12
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuthXMLCommonWriterTest {

    private AuthXMLCommonWriter authXMLCommonWriter;

    @Before
    public void setUp() throws Exception {
        authXMLCommonWriter = new AuthXMLCommonWriter();
        authXMLCommonWriter = spy(authXMLCommonWriter);
    }

    @Test
    public void isWriteable_with11Class_returnsTrue() throws Exception {
        boolean writeable = authXMLCommonWriter.isWriteable(null, MediaType.class, null, null);
        assertThat("MediaType is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withParameterizedTypeOf11Class_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{MediaType.class});
        boolean writeable = authXMLCommonWriter.isWriteable(null, type, null, null);
        assertThat("MediaType is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withTypeOfNon11Class_returnsFalse() throws Exception {
        boolean writeable = authXMLCommonWriter.isWriteable(null, Object.class, null, null);
        assertThat("MediaType is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfMultipleClasses_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{MediaType.class, Object.class});
        boolean writeable = authXMLCommonWriter.isWriteable(null, type, null, null);
        assertThat("MediaType is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfNon11Class_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Object.class});
        boolean writeable = authXMLCommonWriter.isWriteable(null, type, null, null);
        assertThat("MediaType is not writeable", writeable, equalTo(false));
    }

    @Test
    public void getSize_returnsNegOne() throws Exception {
        long size = authXMLCommonWriter.getSize(null, null, null, null, null);
        assertThat("size", size, equalTo(-1L));
    }

    @Test(expected = WebApplicationException.class)
    public void writeTo_withNon11Class_ThrowsException() throws Exception {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        authXMLCommonWriter.writeTo(new Object(), null, null, null, null, null, entityStream);
    }

    @Test
    public void getPreferredPrefix_usesCorev11NsPrefixMap() throws Exception {
        HashMap<String, String> corev11NsPrefixMap = new HashMap<String, String>();
        corev11NsPrefixMap.put("http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0", "");
        authXMLCommonWriter.setNsPrefixMap(corev11NsPrefixMap);
        String preferredPrefix = authXMLCommonWriter.getPreferredPrefix("http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0", "", true);
        assertThat("preferred prefix", preferredPrefix, equalTo(""));
    }
}
