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

    @Before
    public void setUp() throws Exception {
        xmlReader = new XMLReader();
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

    @Test (expected = BadRequestException.class)
    public void readFrom_invalidInputStream_throwsBadRequestException() throws Exception {
        InputStream entityStream = new ASN1InputStream("invalid".getBytes());
        assertThat("returns user", xmlReader.readFrom(null, User.class, null, null, null, entityStream), instanceOf(User.class));
    }

}
