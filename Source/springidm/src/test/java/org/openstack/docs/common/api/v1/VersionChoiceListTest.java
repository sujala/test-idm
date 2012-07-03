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
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class VersionChoiceListTest {
    private VersionChoiceList versionChoiceList;

    @Before
    public void setUp() throws Exception {
        versionChoiceList = new VersionChoiceList();
        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setId("id");
        versionChoice.getOtherAttributes();
    }

    @Test
    public void getVersion_versionIsNull_returnsEmptyList() throws Exception {
        List<VersionChoice> result = versionChoiceList.getVersion();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getVersion_versionNotNull_returnsExistingList() throws Exception {
        List<VersionChoice> choiceList = versionChoiceList.getVersion();
        VersionChoice versionChoice = new VersionChoice();
        versionChoice.setId("id");
        choiceList.add(versionChoice);
        List<VersionChoice> result = versionChoiceList.getVersion();
        assertThat("list", result.get(0).getId(), equalTo("id"));
    }

    @Test
    public void getAny_anyIsNull_returnsEmptyList() throws Exception {
        List<Object> result = versionChoiceList.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAny_anyNotNull_returnsExistingList() throws Exception {
        List<Object> list = versionChoiceList.getAny();
        list.add("test");
        List<Object> result = versionChoiceList.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void getOhterAttribute_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = versionChoiceList.getOtherAttributes();
        assertThat("other attributes", result.isEmpty(), equalTo(true));
    }
}
