package com.rackspace.idm.domain.config.providers.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
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
 * Date: 7/23/12
 * Time: 10:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class RaxAuthXmlWriterTest {
    private RaxAuthXmlWriter raxAuthXmlWriter;

    @Before
    public void setUp() throws Exception {
        raxAuthXmlWriter = new RaxAuthXmlWriter();
        raxAuthXmlWriter = spy(raxAuthXmlWriter);
    }

    @Test
    public void isWriteable_with11Class_returnsTrue() throws Exception {
        boolean writeable = raxAuthXmlWriter.isWriteable(null, ImpersonationRequest.class, null, null);
        assertThat("ImpersonationRequest is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfRAXAUTHClass_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{ImpersonationRequest.class});
        boolean writeable = raxAuthXmlWriter.isWriteable(null, type, null, null);
        assertThat("ImpersonationRequest is writeable", writeable, equalTo(true));
    }

    @Test
    public void isWriteable_withTypeOfNonRAXAUTHClass_returnsFalse() throws Exception {
        boolean writeable = raxAuthXmlWriter.isWriteable(null, Object.class, null, null);
        assertThat("ImpersonationRequest is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfMultipleClasses_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{ImpersonationRequest.class, Object.class});
        boolean writeable = raxAuthXmlWriter.isWriteable(null, type, null, null);
        assertThat("ImpersonationRequest is not writeable", writeable, equalTo(false));
    }

    @Test
    public void isWriteable_withParameterizedTypeOfNonRAXAUTHClass_returnsFalse() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getActualTypeArguments()).thenReturn(new Type[]{Object.class});
        boolean writeable = raxAuthXmlWriter.isWriteable(null, type, null, null);
        assertThat("ImpersonationRequest is not writeable", writeable, equalTo(false));
    }

    @Test
    public void getSize_returnsNegOne() throws Exception {
        long size = raxAuthXmlWriter.getSize(null, null, null, null, null);
        assertThat("size", size, equalTo(-1L));
    }

    @Test(expected = WebApplicationException.class)
    public void writeTo_withNon11Class_ThrowsException() throws Exception {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        raxAuthXmlWriter.writeTo(new Object(), null, null, null, null, null, entityStream);
    }

    @Test
    public void getPreferredPrefix_usesRaxAuthNsPrefixMap() throws Exception {
        HashMap<String, String> raxAuthNsPrefixMap = new HashMap<String, String>();
        raxAuthNsPrefixMap.put("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "");
        raxAuthXmlWriter.setNsPrefixMap(raxAuthNsPrefixMap);
        String preferredPrefix = raxAuthXmlWriter.getPreferredPrefix("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "", true);
        assertThat("preferred prefix", preferredPrefix, equalTo(""));
    }
}
