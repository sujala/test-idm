package com.rackspace.idm.domain.config.providers.cloudv20;

import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Endpoint;

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
 * Time: 4:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class Core20XMLWriterTest {

    private Core20XMLWriter core20XMLWriter;

    @Before
    public void setUp() throws Exception {
        core20XMLWriter = new Core20XMLWriter();
        core20XMLWriter = spy(core20XMLWriter);
    }

    @Test
    public void isWriteable_with11Class_returnsTrue() throws Exception {
        boolean writeable = core20XMLWriter.isWriteable(null, Endpoint.class, null, null);
        assertThat("Endpoint is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withParameterizedTypeOf11Class_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Endpoint.class});
        boolean writeable = core20XMLWriter.isWriteable(null, type, null, null);
        assertThat("Endpoint is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withTypeOfNon11Class_returnsFalse() throws Exception {
        boolean writeable = core20XMLWriter.isWriteable(null, Object.class, null, null);
        assertThat("Endpoint is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfMultipleClasses_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Endpoint.class, Object.class});
        boolean writeable = core20XMLWriter.isWriteable(null, type, null, null);
        assertThat("Endpoint is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfNon11Class_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Object.class});
        boolean writeable = core20XMLWriter.isWriteable(null, type, null, null);
        assertThat("Endpoint is not writeable", writeable, equalTo(false));
    }

    @Test
    public void getSize_returnsNegOne() throws Exception {
        long size = core20XMLWriter.getSize(null, null, null, null, null);
        assertThat("size", size, equalTo(-1L));
    }

    @Test(expected = WebApplicationException.class)
    public void writeTo_withNon11Class_ThrowsException() throws Exception {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        core20XMLWriter.writeTo(new Object(), null, null, null, null, null, entityStream);
    }

    @Test
    public void getPreferredPrefix_usesCorev20NsPrefixMap() throws Exception {
        HashMap<String, String> corev11NsPrefixMap = new HashMap<String, String>();
        corev11NsPrefixMap.put("http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0", "os-ksec2");
        core20XMLWriter.setNsPrefixMap(corev11NsPrefixMap);
        String preferredPrefix = core20XMLWriter.getPreferredPrefix("http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0", "", true);
        assertThat("preferred prefix", preferredPrefix, equalTo("os-ksec2"));
    }
}
