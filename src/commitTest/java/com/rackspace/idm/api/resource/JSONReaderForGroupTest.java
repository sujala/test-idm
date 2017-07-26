package com.rackspace.idm.api.resource;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.idm.exception.BadRequestException;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForGroupTest {

    String groupJSON = "{" +
            "   \"RAX-KSGRP:group\" : {" +
            "       \"id\" : \"groupId\"," +
            "       \"name\" : \"groupName\"," +
            "       \"description\" : \"groupDescription\"" +
            "   }" +
            "}";
    private String emptyGroupJSON = "{" +
            "   \"RAX-KSGRP:group\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnTrue() throws Exception {
        JSONReaderForGroup jsonReaderForGroup = new JSONReaderForGroup();
        boolean readable = jsonReaderForGroup.isReadable(Group.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnFalse() throws Exception {
        JSONReaderForGroup jsonReaderForGroup = new JSONReaderForGroup();
        boolean readable = jsonReaderForGroup.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnGroup() throws Exception {
        JSONReaderForGroup jsonReaderForGroup = new JSONReaderForGroup();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(groupJSON.getBytes()));
        Group group = jsonReaderForGroup.readFrom(Group.class, null, null, null, null, inputStream);
        assertThat("group", group, is(Group.class));
    }

    @Test
    public void getGroupFromJSONString_withValidJSON_setsGroupId() throws Exception {
        Group groupFromJSONString = JSONReaderForGroup.getGroupFromJSONString(groupJSON);
        assertThat("group id", groupFromJSONString.getId(), equalTo("groupId"));
    }

    @Test
    public void getGroupFromJSONString_withValidJSON_setsGroupName() throws Exception {
        Group groupFromJSONString = JSONReaderForGroup.getGroupFromJSONString(groupJSON);
        assertThat("group name", groupFromJSONString.getName(), equalTo("groupName"));
    }

    @Test
    public void getGroupFromJSONString_withValidJSON_setsGroupDescription() throws Exception {
        Group groupFromJSONString = JSONReaderForGroup.getGroupFromJSONString(groupJSON);
        assertThat("group description", groupFromJSONString.getDescription(), equalTo("groupDescription"));
    }

    @Test
    public void getGroupFromJSONString_withValidEmptyGroupJSON_setsNullGroupId() throws Exception {
        Group groupFromJSONString = JSONReaderForGroup.getGroupFromJSONString(emptyGroupJSON);
        assertThat("group id", groupFromJSONString.getId(), nullValue());
    }

    @Test
    public void getGroupFromJSONString_withValidEmptyGroupJSON_setsNullGroupName() throws Exception {
        Group groupFromJSONString = JSONReaderForGroup.getGroupFromJSONString(emptyGroupJSON);
        assertThat("group name", groupFromJSONString.getName(), nullValue());
    }

    @Test
    public void getGroupFromJSONString_withValidEmptyGroupJSON_setsNullGroupDescription() throws Exception {
        Group groupFromJSONString = JSONReaderForGroup.getGroupFromJSONString(emptyGroupJSON);
        assertThat("group description", groupFromJSONString.getDescription(), nullValue());
    }

    @Test
    public void getGroupFromJSONString_withEmptyJSON_returnsNewGroupObject() throws Exception {
        Group groupFromJSONString = JSONReaderForGroup.getGroupFromJSONString("{ }");
        assertThat("group", groupFromJSONString, is(Group.class));
        assertThat("group", groupFromJSONString.getId(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getGroupFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForGroup.getGroupFromJSONString("Invalid JSON");
    }
}
