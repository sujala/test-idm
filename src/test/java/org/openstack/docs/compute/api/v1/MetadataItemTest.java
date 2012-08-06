package org.openstack.docs.compute.api.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetadataItemTest {

    MetadataItem metadataItem;

    @Before
    public void setUp() throws Exception {
        metadataItem = new MetadataItem();
    }

    @Test
    public void getValue_setValue_behavesCorrectly() throws Exception {
        assertThat("null value",metadataItem.getValue(),nullValue());
        metadataItem.setValue("value");
        assertThat("string",metadataItem.getValue(),equalTo("value"));
    }

    @Test
    public void getKey_setKey_behavesCorrectly() throws Exception {
        assertThat("null value",metadataItem.getKey(),nullValue());
        metadataItem.setKey("value");
        assertThat("string",metadataItem.getKey(),equalTo("value"));
    }

    @Test
    public void getOtherAttributes_returnsMap() throws Exception {
        Map<QName,String> otherAttributes = metadataItem.getOtherAttributes();
        assertThat("size of other attributes",otherAttributes.size(),equalTo(0));
    }
}
