package com.rackspace.docs.identity.api.ext.rax_ksgrp.v1;

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
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class GroupTest {
    private Group group;

    @Before
    public void setUp() throws Exception {
        group = new Group();
    }

    @Test
    public void getAny_anyIsNull_createsNewList() throws Exception {
        List<Object> result = group.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void getAny_anyNotNull_returnsExistingList() throws Exception {
        List<Object> existingList = group.getAny();
        existingList.add("test");
        List<Object> result = group.getAny();
        assertThat("string", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void getOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> otherAttributes = group.getOtherAttributes();
        assertThat("other attributes", otherAttributes.isEmpty(), equalTo(true));
    }
}
