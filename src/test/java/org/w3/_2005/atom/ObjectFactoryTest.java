package org.w3._2005.atom;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/9/12
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;
    Link link;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
        link = objectFactory.createLink();
    }

    @Test
    public void relation_fromValue_returnsValue() throws Exception {
        Relation result = Relation.fromValue("start");
        assertThat("start", result.value(), equalTo("start"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void relation_fromValue_withNullValue_throwsIllegalArgumentException() throws Exception {
        Relation.fromValue(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void relation_fromValue_withValueNotFound_throwsIllegalArgumentException() throws Exception {
        Relation.fromValue("notFound");
    }

    @Test
    public void link_getHreflang_returnsHreflang() throws Exception {
        link.setHreflang("test");
        String result = link.getHreflang();
        assertThat("hreflang", result, equalTo("test"));
    }

    @Test
    public void link_getTitle_returnsTitle() throws Exception {
        link.setTitle("test");
        String result = link.getTitle();
        assertThat("title", result, equalTo("test"));
    }

    @Test
    public void link_getBase_returnsBase() throws Exception {
        link.setBase("test");
        String result = link.getBase();
        assertThat("Base", result, equalTo("test"));
    }

    @Test
    public void link_getLang_returnsLang() throws Exception {
        link.setLang("test");
        String result = link.getLang();
        assertThat("Lang", result, equalTo("test"));
    }

    @Test
    public void createLink_returnsNewJAXBElement() throws Exception {
        JAXBElement<Link> result = objectFactory.createLink(link);
        assertThat("base", result.getValue().base, equalTo(null));
    }
}
