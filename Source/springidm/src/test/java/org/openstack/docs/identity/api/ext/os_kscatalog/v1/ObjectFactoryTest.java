package org.openstack.docs.identity.api.ext.os_kscatalog.v1;

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
 * Date: 7/9/12
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;
    EndpointTemplate endpointTemplate;
    EndpointTemplateList endpointTemplateList;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();

        endpointTemplate = objectFactory.createEndpointTemplate();
        endpointTemplateList = objectFactory.createEndpointTemplateList();
    }
    
    @Test
    public void endpointTemplate_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = endpointTemplate.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpointTemplate_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = endpointTemplate.getAny();
        any.add("test");
        List<Object> result = endpointTemplate.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void endpointTemplate_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = endpointTemplate.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpointTemplateList_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = endpointTemplateList.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpointTemplateList_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = endpointTemplateList.getAny();
        any.add("test");
        List<Object> result = endpointTemplateList.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void endpointTemplateList_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = endpointTemplateList.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }
}
