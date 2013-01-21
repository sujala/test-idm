package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.provider.EntityHolder;
import org.apache.commons.ssl.asn1.ASN1InputStream;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.User;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import java.io.InputStream;
import java.lang.reflect.Type;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/20/12
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLReaderTestOld {

    XMLReader xmlReader;
    XMLReader spy;

    @Before
    public void setUp() throws Exception {
        xmlReader = new XMLReader();
        spy = spy(xmlReader);
    }

    @Test
    public void getContext_returnsJAXBContext() throws Exception {
        assertThat("jaxb context", xmlReader.getContext(), instanceOf(JAXBContext.class));
    }

    @Test
    public void isReadable_typeIsEntityHolder_returnsFalse() throws Exception {
        assertThat("returns boolean", xmlReader.isReadable(EntityHolder.class, null, null, null), equalTo(false));
    }

    @Test
    public void isReadable_typeIsNotEntityHolderAndContainedInClasses_returnsTrue() throws Exception {
        assertThat("returns boolean",xmlReader.isReadable(User.class,User.class,null,null),equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotEntityHolderAndNotContainedInClasses_returnsFalse() throws Exception {
        assertThat("returns boolean",xmlReader.isReadable(Object.class,Object.class,null,null),equalTo(false));
    }

    @Test
    public void readFrom_callsGetContext() throws Exception {
        String xml = "<user xmlns=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "      enabled=\"true\" email=\"john.smith@example.org\"\n" +
                "      username=\"jqsmith\" id=\"123456\"/>";
        InputStream entityStream = new ASN1InputStream(xml.getBytes());
        spy.readFrom(null, User.class, null, null, null, entityStream);
        verify(spy).getContext();
    }

    @Test
    public void readFrom_createsUnMarshaller() throws Exception {
        JAXBContext jaxbContext = mock(JAXBContext.class);
        String xml = "<user xmlns=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "      enabled=\"true\" email=\"john.smith@example.org\"\n" +
                "      username=\"jqsmith\" id=\"123456\"/>";
        InputStream entityStream = new ASN1InputStream(xml.getBytes());

        doReturn(spy.getContext().createUnmarshaller()).when(jaxbContext).createUnmarshaller();
        doReturn(jaxbContext).when(spy).getContext();

        spy.readFrom(null, User.class, null, null, null, entityStream);
        verify(jaxbContext).createUnmarshaller();
    }

    @Test
    public void readFrom_validInputStream_returnsCorrectObject() throws Exception {
        String xml = "<user xmlns=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "      enabled=\"true\" email=\"john.smith@example.org\"\n" +
                "      username=\"jqsmith\" id=\"123456\"/>";
        InputStream entityStream = new ASN1InputStream(xml.getBytes());
        assertThat("returns user", xmlReader.readFrom(null, User.class, null, null, null, entityStream), instanceOf(User.class));
    }

    @Test (expected = BadRequestException.class)
    public void readFrom_invalidInputStream_throwsBadRequestException() throws Exception {
        InputStream entityStream = new ASN1InputStream("invalid".getBytes());
        assertThat("returns user", xmlReader.readFrom(null, User.class, null, null, null, entityStream), instanceOf(User.class));
    }

}
