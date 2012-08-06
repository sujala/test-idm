package com.rackspace.idm.domain.config.providers.cloudv20;

import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;

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
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class OSKSCATALOG_XMLWriterTest {

    private OSKSCATALOG_XMLWriter oskscatalog_xmlWriter;

    @Before
    public void setUp() throws Exception {
        oskscatalog_xmlWriter = new OSKSCATALOG_XMLWriter();
        oskscatalog_xmlWriter = spy(oskscatalog_xmlWriter);
    }

    @Test
    public void isWriteable_with11Class_returnsTrue() throws Exception {
        boolean writeable = oskscatalog_xmlWriter.isWriteable(null, EndpointTemplate.class, null, null);
        assertThat("EndpointTemplate is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withParameterizedTypeOf11Class_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{EndpointTemplate.class});
        boolean writeable = oskscatalog_xmlWriter.isWriteable(null, type, null, null);
        assertThat("EndpointTemplate is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withTypeOfNon11Class_returnsFalse() throws Exception {
        boolean writeable = oskscatalog_xmlWriter.isWriteable(null, Object.class, null, null);
        assertThat("EndpointTemplate is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfMultipleClasses_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{EndpointTemplate.class, Object.class});
        boolean writeable = oskscatalog_xmlWriter.isWriteable(null, type, null, null);
        assertThat("EndpointTemplate is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfNon11Class_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Object.class});
        boolean writeable = oskscatalog_xmlWriter.isWriteable(null, type, null, null);
        assertThat("EndpointTemplate is not writeable", writeable, equalTo(false));
    }

    @Test
    public void getSize_returnsNegOne() throws Exception {
        long size = oskscatalog_xmlWriter.getSize(null, null, null, null, null);
        assertThat("size", size, equalTo(-1L));
    }

    @Test(expected = WebApplicationException.class)
    public void writeTo_withNon11Class_ThrowsException() throws Exception {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        oskscatalog_xmlWriter.writeTo(new Object(), null, null, null, null, null, entityStream);
    }

    @Test
    public void getPreferredPrefix_usesCorev20NsPrefixMap() throws Exception {
        HashMap<String, String> corev11NsPrefixMap = new HashMap<String, String>();
        corev11NsPrefixMap.put("http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0", "");
        oskscatalog_xmlWriter.setNsPrefixMap(corev11NsPrefixMap);
        String preferredPrefix = oskscatalog_xmlWriter.getPreferredPrefix("http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0", "", true);
        assertThat("preferred prefix", preferredPrefix, equalTo(""));
    }
}
