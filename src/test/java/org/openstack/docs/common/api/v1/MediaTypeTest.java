package org.openstack.docs.common.api.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/29/12
 * Time: 3:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class MediaTypeTest {
    private MediaType mediaType;

    @Before
    public void setUp() throws Exception {
        mediaType = new MediaType();
        mediaType.setBase("base");
        mediaType.setType("type");
    }

    @Test
    public void getAny_anyIsNull_returnsEmptyList() throws Exception {
        List<Object> result = mediaType.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAny_anyNotNull_returnsExistingList() throws Exception {
        List<Object> list = mediaType.getAny();
        list.add("test");
        List<Object> result = mediaType.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void getBase_baseNotNull_returnsBase() throws Exception {
        String result = mediaType.getBase();
        assertThat("base", result, equalTo("base"));
    }

    @Test
    public void getOhterAttribute_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = mediaType.getOtherAttributes();
        assertThat("other attributes", result.isEmpty(), equalTo(true));
    }
}
