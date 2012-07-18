package com.rackspace.idm.domain.config.providers.cloudv11;

import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/18/12
 * Time: 3:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Core11XMLWriterTest {

    private Core11XMLWriter core11XMLWriter;

    @Before
    public void setUp() throws Exception {
        core11XMLWriter = new Core11XMLWriter();
        core11XMLWriter = spy(core11XMLWriter);
    }

    @Test
    public void isWriteable_with11Class_returnsTrue() throws Exception {
        boolean writeable = core11XMLWriter.isWriteable(null, BaseURL.class, null, null);
        assertThat("BaseURL is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withParameterizedTypeOf11Class_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{BaseURL.class});
        boolean writeable = core11XMLWriter.isWriteable(null, type, null, null);
        assertThat("BaseURL is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withTypeOfNon11Class_returnsFalse() throws Exception {
        boolean writeable = core11XMLWriter.isWriteable(null, Object.class, null, null);
        assertThat("BaseURL is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfMultipleClasses_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{BaseURL.class, BaseURLRef.class});
        boolean writeable = core11XMLWriter.isWriteable(null, type, null, null);
        assertThat("BaseURL is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfNon11Class_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Object.class});
        boolean writeable = core11XMLWriter.isWriteable(null, type, null, null);
        assertThat("BaseURL is not writeable", writeable, equalTo(false));
    }

    @Test
    public void getSize_returnsNegOne() throws Exception {
        long size = core11XMLWriter.getSize(null, null, null, null, null);
        assertThat("size", size, equalTo(-1L));
    }

    @Test(expected = WebApplicationException.class)
    public void writeTo_withNon11Class_ThrowsException() throws Exception {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        core11XMLWriter.writeTo(new Object(), null, null, null, null, null, entityStream);
    }

    @Test
    public void getPreferredPrefix_usesCorev11NsPrefixMap() throws Exception {
        HashMap<String, String> corev11NsPrefixMap = new HashMap<String, String>();
        corev11NsPrefixMap.put("http://www.w3.org/2005/Atom", "atom");
        core11XMLWriter.setNsPrefixMap(corev11NsPrefixMap);
        String preferredPrefix = core11XMLWriter.getPreferredPrefix("http://www.w3.org/2005/Atom", "", true);
        assertThat("preferred prefix", preferredPrefix, equalTo("atom"));
    }
}
