package org.openstack.docs.common.api.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {

    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createExtension_returnsExtension() throws Exception {
        assertThat("extension",objectFactory.createExtension(),instanceOf(Extension.class));
    }

    @Test
    public void createVersionChoiceList_returnsVersionChoiceList() throws Exception {
        assertThat("version choice list",objectFactory.createVersionChoiceList(),instanceOf(VersionChoiceList.class));
    }

    @Test
    public void createExtensions_returnsExtensions() throws Exception {
        assertThat("extensions",objectFactory.createExtensions(),instanceOf(Extensions.class));
    }

    @Test
    public void createVersionChoice_returnsVersionChoice() throws Exception {
        assertThat("version choice",objectFactory.createVersionChoice(),instanceOf(VersionChoice.class));
    }

    @Test
    public void createMediaType_returnsMediaType() throws Exception {
        assertThat("media type",objectFactory.createMediaType(),instanceOf(MediaType.class));
    }

    @Test
    public void createMediaTypeList_returnsMediaTypeList() throws Exception {
        assertThat("media type lists", objectFactory.createMediaTypeList(),instanceOf(MediaTypeList.class));
    }

    @Test
    public void createExtensions_returnsJAXBElement() throws Exception {
        assertThat("jaxb element",objectFactory.createExtensions(new Extensions()),instanceOf(JAXBElement.class));
    }

    @Test
    public void createVersion_returnsJAXBElement() throws Exception {
        assertThat("jaxb element", objectFactory.createVersion(new VersionChoice()),instanceOf(JAXBElement.class));
    }

    @Test
    public void createChoices_returnsJAXBElement() throws Exception {
        assertThat("jaxb element",objectFactory.createChoices(new VersionChoiceList()),instanceOf(JAXBElement.class));
    }

    @Test
    public void createVersions_returnsJAXBElement() throws Exception {
        assertThat("jaxb element", objectFactory.createVersions(new VersionChoiceList()),instanceOf(JAXBElement.class));
    }
}
