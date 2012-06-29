package org.openstack.docs.compute.api.v1;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 1:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {

    ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createMetadataItem_returnsMetadataItem() throws Exception {
        assertThat("metadata item",objectFactory.createMetadataItem(),instanceOf(MetadataItem.class));
    }

    @Test
    public void createMetadata_returnsMetadata() throws Exception {
        assertThat("metadata",objectFactory.createMetadata(),instanceOf(Metadata.class));
    }
}
