package com.rackspace.idm.domain.config.providers.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import org.junit.Before;
import org.junit.Test;

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
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class RAXKSKEY_XMLWriterTest {

    private RAXKSKEY_XMLWriter raxkskey_xmlWriter;

    @Before
    public void setUp() throws Exception {
        raxkskey_xmlWriter = new RAXKSKEY_XMLWriter();
        raxkskey_xmlWriter = spy(raxkskey_xmlWriter);
    }

    @Test
    public void isWriteable_with11Class_returnsTrue() throws Exception {
        boolean writeable = raxkskey_xmlWriter.isWriteable(null, ApiKeyCredentials.class, null, null);
        assertThat("ApiKeyCredentials is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withParameterizedTypeOf11Class_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{ApiKeyCredentials.class});
        boolean writeable = raxkskey_xmlWriter.isWriteable(null, type, null, null);
        assertThat("ApiKeyCredentials is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withTypeOfNon11Class_returnsFalse() throws Exception {
        boolean writeable = raxkskey_xmlWriter.isWriteable(null, Object.class, null, null);
        assertThat("ApiKeyCredentials is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfMultipleClasses_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{ApiKeyCredentials.class, Object.class});
        boolean writeable = raxkskey_xmlWriter.isWriteable(null, type, null, null);
        assertThat("ApiKeyCredentials is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfNon11Class_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Object.class});
        boolean writeable = raxkskey_xmlWriter.isWriteable(null, type, null, null);
        assertThat("ApiKeyCredentials is not writeable", writeable, equalTo(false));
    }

    @Test
    public void getSize_returnsNegOne() throws Exception {
        long size = raxkskey_xmlWriter.getSize(null, null, null, null, null);
        assertThat("size", size, equalTo(-1L));
    }

    @Test(expected = WebApplicationException.class)
    public void writeTo_withNon11Class_ThrowsException() throws Exception {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        raxkskey_xmlWriter.writeTo(new Object(), null, null, null, null, null, entityStream);
    }

    @Test
    public void getPreferredPrefix_usesCorev11NsPrefixMap() throws Exception {
        HashMap<String, String> corev11NsPrefixMap = new HashMap<String, String>();
        corev11NsPrefixMap.put("http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0", "");
        raxkskey_xmlWriter.setNsPrefixMap(corev11NsPrefixMap);
        String preferredPrefix = raxkskey_xmlWriter.getPreferredPrefix("http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0", "", true);
        assertThat("preferred prefix", preferredPrefix, equalTo(""));
    }
}
