package org.openstack.docs.identity.api.ext.os_ksadm.v1;

import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 5:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceTest {

    Service service;

    @Before
    public void setUp() throws Exception {
        service = new Service();
    }

    @Test
    public void getOtherAttributes_returnsEmptyList() throws Exception {
        Map<QName,String> otherAttributes = service.getOtherAttributes();
        assertThat("size",otherAttributes.size(),equalTo(0));
    }
}
