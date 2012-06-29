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
 * Time: 3:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensionsTest {
    private Extensions extensions;
    private Extension extension;

    @Before
    public void setUp() throws Exception {
        extensions = new Extensions();
        extension = new Extension();
        extension.getOtherAttributes();
    }

    @Test
    public void getAny_anyIsNull_returnsEmptyList() throws Exception {
        List<Object> result = extensions.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAny_anyNotNull_returnsExistingList() throws Exception {
        List<Object> list = extensions.getAny();
        list.add("test");
        List<Object> result = extensions.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void getOhterAttribute_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = extensions.getOtherAttributes();
        assertThat("other attributes", result.isEmpty(), equalTo(true));
    }
}
