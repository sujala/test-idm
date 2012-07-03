package com.rackspace.idm.api.resource.cloud;

import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/3/12
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class CredentialProviderTest {
    CredentialProvider credentialProvider;

    @Before
    public void setUp() throws Exception {
        credentialProvider = new CredentialProvider();
    }

    @Test
    public void isReadable_withNonXmlType_returnsFalse() throws Exception {
        boolean readable = credentialProvider.isReadable(JAXBElement.class, JAXBElement.class.getGenericSuperclass(), null, MediaType.APPLICATION_JSON_TYPE);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void isReadable_withParameterizedTypeAndNonXml_returnsFalse() throws Exception {
        Type type =  new JAXBElement<String>(new QName("first", "second"), String.class, "String").getClass().getGenericSuperclass();
        boolean readable = credentialProvider.isReadable(JAXBElement.class, type, null, MediaType.APPLICATION_JSON_TYPE);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void isReadable_withXmlAndNonParameterizedType_returnsFalse() throws Exception {
        boolean readable = credentialProvider.isReadable(JAXBElement.class, JAXBElement.class.getGenericSuperclass(), null, MediaType.APPLICATION_XML_TYPE);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void isReadable_withXmlAndParameterizedType_returnsTrue() throws Exception {
        ParameterizedType type = mock(ParameterizedType.class);
        when(type.getRawType()).thenReturn(JAXBElement.class);
        WildcardType wildcardType = mock(WildcardType.class);
        when(wildcardType.getUpperBounds()).thenReturn(new Type[]{Credentials.class});
        when(type.getActualTypeArguments()).thenReturn(new Type[]{wildcardType});
        boolean readable = credentialProvider.isReadable(JAXBElement.class, type, null, MediaType.APPLICATION_XML_TYPE);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void readFrom_withPasswordCredentials_withoutException() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<passwordCredentials\n" +
                "        xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                "        username=\"userTest\"\n" +
                "        password=\"password\"/>").getBytes()));
        JAXBElement<? extends Credentials> jaxbElement = credentialProvider.readFrom(null, null, null, null, null, inputStream);
        assertThat("Credentials from provider", jaxbElement.getValue() instanceof PasswordCredentials, equalTo(true));
    }

    @Test
    public void readFrom_withNastCredentials_withoutException() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<nastCredentials\n" +
                "        xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                "        nastId=\"RackCloudFS_bf571533-d113-4249-962d-178648338863\"\n" +
                "        key=\"a86850deb2742ec3cb41518e26aa2d89\"/>").getBytes()));
        JAXBElement<? extends Credentials> jaxbElement = credentialProvider.readFrom(null, null, null, null, null, inputStream);
        assertThat("Credentials from provider", jaxbElement.getValue() instanceof NastCredentials, equalTo(true));
    }

    @Test
    public void readFrom_withMossoCredentials_withoutException() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<mossoCredentials\n" +
                "    xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                "    mossoId=\"323676\"\n" +
                "    key=\"a86850deb2742ec3cb41518e26aa2d89\"/>").getBytes()));
        JAXBElement<? extends Credentials> jaxbElement = credentialProvider.readFrom(null, null, null, null, null, inputStream);
        assertThat("Credentials from provider", jaxbElement.getValue() instanceof MossoCredentials, equalTo(true));
    }

    @Test( expected = WebApplicationException.class)
    public void readFrom_UnexpectedCredentials_throws400Exception() throws Exception {
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<magicCredentials\n" +
                "    xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                "    magicId=\"323676\"\n" +
                "    magicKey=\"a86850deb2742ec3cb41518e26aa2d89\"/>").getBytes()));
        credentialProvider.readFrom(null, null, null, null, null, inputStream);
    }
}
