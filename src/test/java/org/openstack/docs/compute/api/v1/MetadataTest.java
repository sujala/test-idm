package org.openstack.docs.compute.api.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 12:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetadataTest {

    Metadata metadata;

    @Before
    public void setUp() throws Exception {
        metadata = new Metadata();
    }

    @Test
    public void getMeta_metaNull_returnsEmptyArrayList() throws Exception {
        List<MetadataItem> meta = metadata.getMeta();
        assertThat("list size", meta.size(),equalTo(0));
    }

    @Test
    public void getMeta_metaNotNull_returnsPopulatedArrayList() throws Exception {
        MetadataItem metadataItem = new MetadataItem();
        metadata.getMeta().add(metadataItem);
        List<MetadataItem> meta = metadata.getMeta();
        assertThat("list size", meta.size(),equalTo(1));
        assertThat("metadata item",meta.get(0),equalTo(metadataItem));
    }

    @Test
    public void getAny_anyNull_returnsEmptyArrayList() throws Exception {
        List<Object> any = metadata.getAny();
        assertThat("list size", any.size(),equalTo(0));
    }

    @Test
    public void getAny_anyNotNull_returnsPopulatedArrayList() throws Exception {
        MetadataItem metadataItem = new MetadataItem();
        metadata.getAny().add(metadataItem);
        List<Object> any = metadata.getAny();
        assertThat("list size", any.size(),equalTo(1));
        assertThat("metadata item",(MetadataItem) any.get(0),equalTo(metadataItem));
    }

    @Test
    public void getOtherAttributes_returnsEmptyMap() throws Exception {
        Map<QName,String> otherAttributes = metadata.getOtherAttributes();
        assertThat("other attributes size",otherAttributes.size(),equalTo(0));
    }
}
