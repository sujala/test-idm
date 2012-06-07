package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Tenant;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/7/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForBaseURLRefListTest {

    JSONReaderForBaseURLRefList jsonReaderForBaseURLRefList;

    @Before
    public void setUp() throws Exception {
        jsonReaderForBaseURLRefList = new JSONReaderForBaseURLRefList();
    }

    @Test
    public void isReadable_typeIsBaseURLRefList_returnsTrue() throws Exception {
        assertThat("bool", jsonReaderForBaseURLRefList.isReadable(BaseURLRefList.class, null, null, null), equalTo(true));
    }

    @Test
    public void isReadable_typeIsNotBaseURLRefList_returnsFalse() throws Exception {
        assertThat("bool", jsonReaderForBaseURLRefList.isReadable(Tenant.class, null, null, null), equalTo(false));
    }

    @Test
    public void readFrom_returnsBaseURLRefList() throws Exception {
        String body = "{\n" +
                "  \"baseURLRefs\": [\n" +
                "       {\n" +
                "           \"id\": \"123\",\n" +
                "           \"href\": \"../samples/baseURLRefs.json\",\n" +
                "           \"v1Default\": true\n" +
                "       },\n" +
                "       {\n" +
                "           \"id\": \"456\",\n" +
                "           \"href\": \"../samples2/baseURLRefs.json\",\n" +
                "           \"v1Default\": true\n" +
                "       }\n" +
                "   ]\n" +
                "}";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(body.getBytes()));
        assertThat("basURLRefList",jsonReaderForBaseURLRefList.readFrom(BaseURLRefList.class, null, null, null, null, inputStream),instanceOf(BaseURLRefList.class));
    }

    @Test
    public void getBaseURLRefFromJSONString_validJsonBody_returnsBaseURLRefListPopulatedList() throws Exception {
        String body ="{\n" +
                "  \"baseURLRefs\": [\n" +
                "       {\n" +
                "           \"id\": \"123\",\n" +
                "           \"href\": \"../samples/baseURLRefs.json\",\n" +
                "           \"v1Default\": true\n" +
                "       },\n" +
                "       {\n" +
                "           \"id\": \"456\",\n" +
                "           \"href\": \"../samples2/baseURLRefs.json\",\n" +
                "           \"v1Default\": true\n" +
                "       }\n" +
                "   ]\n" +
                "}";
        List<BaseURLRef> list = JSONReaderForBaseURLRefList.getBaseURLRefFromJSONString(body).getBaseURLRef();
        assertThat("baseURLRef", list.size(), equalTo(2));
    }

    @Test
    public void getBaseURlRefFromJSONString_passwordCredentialInput_returnsBaseURLRefListEmptyList() throws Exception {
        String body = "{\n" +
                "    \"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        List<BaseURLRef> list = JSONReaderForBaseURLRefList.getBaseURLRefFromJSONString(body).getBaseURLRef();
        assertThat("baseURLRef", list.size(), equalTo(0));
    }

    @Test(expected = BadRequestException.class)
    public void getBaseURLRefFromJSONString_invalidInput_throwsBadRequestException() throws Exception {
        String body = "\"auth\":{\n" +
                "        \"passwordCredentials\":{\n" +
                "            \"username\":\"jsmith\",\n" +
                "            \"password\":\"theUsersPassword\"\n" +
                "        }\n" +
                "    }\n";
        JSONReaderForBaseURLRefList.getBaseURLRefFromJSONString(body);
    }

    @Test
    public void getBaseURLRefFromJSONString_emptyList_returnsBaseURLRefListEmptyList() throws Exception {
        String body = "{\n" +
                "  \"baseURLRefs\": [\n" +
                "   ]\n" +
                "}";
        List<BaseURLRef> list = JSONReaderForBaseURLRefList.getBaseURLRefFromJSONString(body).getBaseURLRef();
        assertThat("baseURLRef", list.size(), equalTo(0));
    }
}
