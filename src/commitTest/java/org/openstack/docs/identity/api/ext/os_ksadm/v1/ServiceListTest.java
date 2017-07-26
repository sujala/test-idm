package org.openstack.docs.identity.api.ext.os_ksadm.v1;

import com.rackspace.idm.domain.entity.User;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceListTest {

    ServiceList serviceList;

    @Before
    public void setUp() throws Exception {
        serviceList = new ServiceList();
    }

    @Test
    public void getAny_anyNull_returnsEmptyList() throws Exception {
        List<Object> any = serviceList.getAny();
        assertThat("list size",any.size(),equalTo(0));
    }

    @Test
    public void getAny_anyNotNull_returnsPopulatedList() throws Exception {
        User user = new User();
        serviceList.getAny().add(user);
        List<Object> any = serviceList.getAny();
        assertThat("list size",any.size(),equalTo(1));
        assertThat("object",((User) any.get(0)),equalTo(user));
    }

    @Test
    public void getOtherAttributes_returnsEmptyMap() throws Exception {
        Map<QName, String> otherAttribute = serviceList.getOtherAttributes();
        assertThat("empty",otherAttribute.size(),equalTo(0));
    }
}
